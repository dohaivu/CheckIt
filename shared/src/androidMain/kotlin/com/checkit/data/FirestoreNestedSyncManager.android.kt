package com.checkit.data

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.checkit.domain.QuickNoteRules
import com.checkit.util.awaitTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

private val Context.nestedSyncDataStore by preferencesDataStore(name = "nested_sync")

/**
 * Manual Firestore sync for nested lists on Android: one open document
 * ([syncDocument]) plus the document list ([syncDocuments]).
 *
 * Offline-first: every mutation is already in Room; sync pushes dirty rows,
 * pulls watermarked remote changes with last-write-wins on
 * `updatedAtMillis`, then purges uploaded tombstones after the shared
 * retention window.
 *
 * Unlike QuickNote there are no automatic triggers (no debounce, no
 * reconnect callback): the UI calls sync explicitly from sync buttons.
 * Failures are surfaced through [syncState]; the app stays fully usable
 * offline.
 */
class FirestoreNestedSyncManager(
    context: Context,
    private val bridge: NestedSyncBridge,
) : NestedSyncManager {
    private val appContext = context.applicationContext
    private val dataStore = appContext.nestedSyncDataStore
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow(NestedSyncState())
    override val syncState: StateFlow<NestedSyncState> = _syncState.asStateFlow()

    override suspend fun syncDocument(documentId: String) {
        syncMutex.withLock {
            val prepared = prepareSync(documentId) ?: return
            val (userId, firestore) = prepared
            Log.d(TAG, "Nested sync started uid=$userId doc=$documentId")
            val docsRef = firestore
                .collection(NestedSyncConfig.USERS_COLLECTION)
                .document(userId)
                .collection(NestedSyncConfig.DOCUMENTS_COLLECTION)
            val docRef = docsRef.document(documentId)
            val itemsRef = docRef.collection(NestedSyncConfig.ITEMS_COLLECTION)
            try {
                // Push: the document row, then dirty items (batched).
                var pushed = 0
                bridge.dirtyDocumentJson(documentId)?.let { json ->
                    val map = NestedSyncDocument.mapFromJson(json) ?: return@let
                    docRef.set(map).awaitTask()
                    pushed++
                    bridge.markDocumentClean(
                        documentId,
                        (map[NestedSyncDocument.FIELD_UPDATED_AT] as? Number)?.toLong() ?: 0L,
                    )
                }
                pushed += pushMaps(
                    firestore,
                    itemsRef,
                    bridge.dirtyItemJsons(documentId).mapNotNull { NestedSyncDocument.mapFromJson(it) },
                ) { ids, max -> bridge.markItemsClean(ids, max) }

                // Pull: the document row by id, then watermarked items.
                var applied = 0
                docRef.get().awaitTask().let { snapshot ->
                    if (snapshot.exists()) {
                        val data = snapshot.data.orEmpty() + (NestedSyncDocument.FIELD_ID to snapshot.id)
                        if (bridge.applyRemoteDocumentJson(NestedSyncDocument.toJson(data))) {
                            applied++
                        }
                    }
                }
                val lastPull = lastPullMillis(documentId)
                val pullStart = Clock.System.now().toEpochMilliseconds()
                val pulled = pullNewer(itemsRef, lastPull)
                // Batch apply orders parents before children so the
                // self-FK can never fail on first pulls.
                applied += bridge.applyRemoteItemJsons(pulled.jsons)
                setLastPullMillis(documentId, maxOf(lastPull, pullStart, pulled.maxRemoteUpdatedAt))

                // Purge: uploaded item tombstones, then the document tombstone.
                val purgeCutoff = pullStart - QuickNoteRules.PURGE_AFTER_MILLIS
                val purged = purgeItems(itemsRef, documentId, purgeCutoff) +
                    purgeDocument(docRef, documentId, purgeCutoff)

                setLastSyncedAt(documentId, pullStart)
                _syncState.value = NestedSyncState(NestedSyncStatus.SYNCED, documentId, pullStart)
                Log.i(
                    TAG,
                    "Nested sync succeeded doc=$documentId pushed=$pushed " +
                        "pulled=${pulled.jsons.size} applied=$applied purged=$purged",
                )
            } catch (e: Exception) {
                recordSyncFailure(documentId, e)
            }
        }
    }

    /**
     * Manual sync of the document list (all documents, no items): pushes
     * dirty document rows, pulls watermarked remote documents, and purges
     * uploaded document tombstones. Open-document sync still owns items.
     */
    override suspend fun syncDocuments() {
        syncMutex.withLock {
            val prepared = prepareSync(null) ?: return
            val (userId, firestore) = prepared
            Log.d(TAG, "Nested docs sync started uid=$userId")
            val docsRef = firestore
                .collection(NestedSyncConfig.USERS_COLLECTION)
                .document(userId)
                .collection(NestedSyncConfig.DOCUMENTS_COLLECTION)
            try {
                // Push: all dirty document rows (batched).
                val pushed = pushMaps(
                    firestore,
                    docsRef,
                    bridge.dirtyDocumentJsons().mapNotNull { NestedSyncDocument.mapFromJson(it) },
                ) { ids, max -> bridge.markDocumentsClean(ids, max) }

                // Pull: watermarked documents (overlap margin for clock skew).
                var applied = 0
                val lastPull = lastPullDocsMillis()
                val pullStart = Clock.System.now().toEpochMilliseconds()
                val pulled = pullNewer(docsRef, lastPull)
                pulled.jsons.forEach { json ->
                    if (bridge.applyRemoteDocumentJson(json)) applied++
                }
                setLastPullDocsMillis(maxOf(lastPull, pullStart, pulled.maxRemoteUpdatedAt))

                // Purge uploaded document tombstones (items first, guarded).
                var purged = 0
                val purgeCutoff = pullStart - QuickNoteRules.PURGE_AFTER_MILLIS
                bridge.purgeableDocumentIds(purgeCutoff).forEach { id ->
                    purged += purgeItems(
                        docsRef.document(id).collection(NestedSyncConfig.ITEMS_COLLECTION),
                        id,
                        purgeCutoff,
                    )
                    purged += purgeDocument(docsRef.document(id), id, purgeCutoff)
                }

                setLastSyncedDocsAt(pullStart)
                _syncState.value = NestedSyncState(NestedSyncStatus.SYNCED, null, pullStart)
                Log.i(
                    TAG,
                    "Nested docs sync succeeded pushed=$pushed " +
                        "pulled=${pulled.jsons.size} applied=$applied purged=$purged",
                )
            } catch (e: Exception) {
                recordSyncFailure(null, e)
            }
        }
    }

    private data class PreparedSync(val userId: String, val firestore: FirebaseFirestore)

    /**
     * Shared online/config/sign-in prologue. Sets [syncState] and returns
     * null when a terminal state was already set (offline or sign-in fail).
     */
    private suspend fun prepareSync(documentId: String?): PreparedSync? {
        val last = if (documentId != null) lastSyncedAt(documentId) else lastSyncedDocsAt()
        if (!isOnline()) {
            _syncState.value = NestedSyncState(NestedSyncStatus.OFFLINE, documentId, last)
            return null
        }
        _syncState.value = NestedSyncState(NestedSyncStatus.SYNCING, documentId, last)
        val userId = ensureUserId()
        if (userId == null) {
            recordFailure(documentId, "Sign-in failed. Try again.")
            return null
        }
        return PreparedSync(userId, FirebaseFirestore.getInstance(NestedSyncConfig.DATABASE_ID))
    }

    /**
     * Pushes maps into [collection] in batches, clears dirty flags guarded
     * by the pushed watermark, and returns the pushed count. Rows without
     * an id are skipped before batching so counts stay accurate.
     */
    private suspend fun pushMaps(
        firestore: FirebaseFirestore,
        collection: CollectionReference,
        maps: List<Map<String, Any?>>,
        markClean: suspend (ids: List<String>, maxUpdatedAt: Long) -> Unit,
    ): Int {
        val rows = maps.mapNotNull { map ->
            val id = map[NestedSyncDocument.FIELD_ID] as? String ?: return@mapNotNull null
            val updated = (map[NestedSyncDocument.FIELD_UPDATED_AT] as? Number)?.toLong() ?: 0L
            Triple(id, updated, map)
        }
        rows.chunked(NestedSyncConfig.PUSH_BATCH_SIZE).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { (id, _, map) -> batch.set(collection.document(id), map) }
            batch.commit().awaitTask()
        }
        if (rows.isNotEmpty()) {
            markClean(rows.map { it.first }, rows.maxOf { it.second })
        }
        return rows.size
    }

    private data class Pulled(val jsons: List<String>, val maxRemoteUpdatedAt: Long)

    /**
     * Watermarked pull of one collection (overlap margin for clock skew).
     * Returns JSON rows plus the max remote clock for the watermark.
     */
    private suspend fun pullNewer(collection: CollectionReference, lastPull: Long): Pulled {
        val snapshots = collection
            .whereGreaterThan(
                NestedSyncDocument.FIELD_UPDATED_AT,
                lastPull - NestedSyncConfig.PULL_OVERLAP_MILLIS,
            )
            .get().awaitTask().documents
        var maxRemote = lastPull
        val jsons = snapshots.map { doc ->
            val data = doc.data.orEmpty() + (NestedSyncDocument.FIELD_ID to doc.id)
            maxRemote = maxOf(
                maxRemote,
                (data[NestedSyncDocument.FIELD_UPDATED_AT] as? Number)?.toLong() ?: lastPull,
            )
            NestedSyncDocument.toJson(data)
        }
        return Pulled(jsons, maxRemote)
    }

    /**
     * Purges uploaded item tombstones of one document. Returns the count.
     */
    private suspend fun purgeItems(
        itemsRef: CollectionReference,
        documentId: String,
        cutoff: Long,
    ): Int {
        var purged = 0
        bridge.purgeableItemIds(documentId, cutoff).forEach { id ->
            try {
                itemsRef.document(id).delete().awaitTask()
                bridge.hardDeleteItems(listOf(id))
                purged++
            } catch (e: Exception) {
                Log.w(TAG, "Nested purge failed for item $id; will retry next sync", e)
            }
        }
        return purged
    }

    /**
     * Purges one uploaded document tombstone. Skipped while the document
     * still has dirty items, so list sync can never orphan unsynced item
     * rows through the cascading hard delete. Returns 0 or 1.
     */
    private suspend fun purgeDocument(
        docRef: DocumentReference,
        documentId: String,
        cutoff: Long,
    ): Int {
        val id = bridge.purgeableDocumentId(documentId, cutoff) ?: return 0
        if (bridge.hasDirtyItems(documentId)) return 0
        try {
            docRef.delete().awaitTask()
            bridge.hardDeleteDocument(id)
            return 1
        } catch (e: Exception) {
            Log.w(TAG, "Nested purge failed for document $id; will retry next sync", e)
            return 0
        }
    }

    private suspend fun lastPullMillis(documentId: String): Long =
        dataStore.data.map { it[longPreferencesKey(pullKey(documentId))] ?: 0L }.first()

    private suspend fun setLastPullMillis(documentId: String, value: Long) {
        dataStore.edit { prefs -> prefs[longPreferencesKey(pullKey(documentId))] = value }
    }

    private suspend fun lastSyncedAt(documentId: String): Long? =
        dataStore.data.map { it[longPreferencesKey(syncedKey(documentId))] }.first()

    private suspend fun setLastSyncedAt(documentId: String, value: Long) {
        dataStore.edit { prefs -> prefs[longPreferencesKey(syncedKey(documentId))] = value }
    }

    private fun pullKey(documentId: String) = "last_pull_$documentId"

    private fun syncedKey(documentId: String) = "last_synced_$documentId"

    private suspend fun lastPullDocsMillis(): Long =
        dataStore.data.map { it[KEY_LAST_PULL_DOCS] ?: 0L }.first()

    private suspend fun setLastPullDocsMillis(value: Long) {
        dataStore.edit { prefs -> prefs[KEY_LAST_PULL_DOCS] = value }
    }

    private suspend fun lastSyncedDocsAt(): Long? =
        dataStore.data.map { it[KEY_LAST_SYNCED_DOCS] }.first()

    private suspend fun setLastSyncedDocsAt(value: Long) {
        dataStore.edit { prefs -> prefs[KEY_LAST_SYNCED_DOCS] = value }
    }

    private suspend fun recordFailure(documentId: String?, message: String) {
        _syncState.value = NestedSyncState(
            NestedSyncStatus.ERROR,
            documentId,
            documentId?.let { lastSyncedAt(it) } ?: lastSyncedDocsAt(),
            message,
        )
        Log.w(TAG, "Nested sync failed for $documentId: $message")
    }

    /** Shared catch-block mapping: offline vs failed, surfaced via state. */
    private suspend fun recordSyncFailure(documentId: String?, e: Exception) {
        val offline = !isOnline()
        recordFailure(
            documentId,
            if (offline) "You're offline. Changes are saved on this device."
            else "Sync failed (${e.message ?: "unknown error"}). Try again.",
        )
    }

    private fun isOnline(): Boolean =
        runCatching { connectivityManager.activeNetwork != null }.getOrDefault(true)

    private suspend fun ensureUserId(): String? = try {
        val auth = FirebaseAuth.getInstance()
        val existing = auth.currentUser?.uid
        if (existing != null) {
            existing
        } else {
            val uid = auth.signInAnonymously().awaitTask().user?.uid
            if (uid != null) {
                Log.d(TAG, "Anonymous sign-in succeeded (uid=${uid.take(6)}…)")
            }
            uid
        }
    } catch (e: Exception) {
        Log.w(TAG, "Nested anonymous sign-in failed", e)
        null
    }

    companion object {
        private const val TAG = "NestedSync"
        private val KEY_LAST_PULL_DOCS = longPreferencesKey("last_pull_docs")
        private val KEY_LAST_SYNCED_DOCS = longPreferencesKey("last_synced_docs")
    }
}
