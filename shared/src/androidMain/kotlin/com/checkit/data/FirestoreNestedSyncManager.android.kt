package com.checkit.data

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.checkit.domain.QuickNoteRules
import com.checkit.util.awaitTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock

private val Context.nestedSyncDataStore by preferencesDataStore(name = "nested_sync")

/**
 * Manual Firestore sync for nested lists on Android: one open document
 * ([syncDocument]) plus the document list ([syncDocuments]).
 *
 * Offline-first: every mutation is already in Room; sync pushes dirty rows,
 * pulls full remote state with last-write-wins on `updatedAtMillis`,
 * reconciles rows deleted elsewhere, then purges uploaded tombstones
 * after the shared retention window. An account switch re-dirties live
 * rows so they upload to the new collection instead of stranding.
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
            writePreSyncSnapshot(documentId)
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
                pushed += pushAllDirtyItems(firestore, userId)

                // Pull: full document plus full items. Manual sync converges:
                // LWW applies remote wins, delete detection tombstones
                // clean local rows missing remotely.
                var applied = 0
                var reconciled = 0
                val pullStart = Clock.System.now().toEpochMilliseconds()
                docRef.get().awaitTask().let { snapshot ->
                    if (snapshot.exists()) {
                        val data = snapshot.data.orEmpty() + (NestedSyncDocument.FIELD_ID to snapshot.id)
                        if (bridge.applyRemoteDocumentJson(NestedSyncDocument.toJson(data))) {
                            applied++
                        }
                    } else if (bridge.reconcileDocumentMissing(documentId, pullStart)) {
                        reconciled++
                    }
                }
                val itemJsons = itemsRef.get().awaitTask().documents.map { doc ->
                    val data = doc.data.orEmpty() + (NestedSyncDocument.FIELD_ID to doc.id)
                    NestedSyncDocument.toJson(data)
                }
                // Batch apply orders parents before children so the
                // self-FK can never fail on first pulls.
                applied += bridge.applyRemoteItemJsons(itemJsons)
                reconciled += bridge.reconcileMissingItems(documentId, remoteIds(itemJsons), pullStart)

                // Purge: uploaded item tombstones, then the document tombstone.
                val purgeCutoff = pullStart - QuickNoteRules.PURGE_AFTER_MILLIS
                val purged = purgeItems(itemsRef, documentId, purgeCutoff) +
                    purgeDocument(docRef, documentId, purgeCutoff)

                setLastSyncedAt(documentId, pullStart)
                _syncState.value = NestedSyncState(NestedSyncStatus.SYNCED, documentId, pullStart)
                Log.i(
                    TAG,
                    "Nested sync succeeded doc=$documentId pushed=$pushed " +
                        "pulled=${itemJsons.size} applied=$applied reconciled=$reconciled purged=$purged",
                )
            } catch (e: Exception) {
                recordSyncFailure(documentId, e)
            }
        }
    }

    /**
     * Manual sync of the document list: pushes all dirty documents plus all
     * dirty items, pulls the full document set, and purges uploaded
     * document tombstones. Open-document sync additionally pulls items.
     */
    override suspend fun syncDocuments() {
        syncMutex.withLock {
            writePreSyncSnapshot(null)
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
                ) { ids, max -> bridge.markDocumentsClean(ids, max) } +
                    // Plus all dirty items anywhere: list sync never strands edits.
                    pushAllDirtyItems(firestore, userId)

                // Pull: full documents. Manual sync converges: LWW applies
                // remote wins, delete detection tombstones clean rows
                // missing remotely.
                var applied = 0
                val pullStart = Clock.System.now().toEpochMilliseconds()
                val docJsons = docsRef.get().awaitTask().documents.map { doc ->
                    val data = doc.data.orEmpty() + (NestedSyncDocument.FIELD_ID to doc.id)
                    NestedSyncDocument.toJson(data)
                }
                docJsons.forEach { json ->
                    if (bridge.applyRemoteDocumentJson(json)) applied++
                }
                val reconciled = bridge.reconcileMissingDocuments(remoteIds(docJsons), pullStart)

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
                        "pulled=${docJsons.size} applied=$applied reconciled=$reconciled purged=$purged",
                )
            } catch (e: Exception) {
                recordSyncFailure(null, e)
            }
        }
    }

    /**
     * Pushes every dirty item across all documents, grouped by parent
     * collection. Both buttons use this so no edit is ever stranded by
     * tapping the "wrong" one; pull scopes stay per-button.
     */
    private suspend fun pushAllDirtyItems(firestore: FirebaseFirestore, userId: String): Int {
        val maps = bridge.dirtyAllItemJsons().mapNotNull { NestedSyncDocument.mapFromJson(it) }
        var pushed = 0
        maps.groupBy { it[NestedSyncDocument.FIELD_DOCUMENT_ID] as? String ?: "" }
            .filterKeys { it.isNotEmpty() }
            .forEach { (docId, group) ->
                val itemsRef = firestore
                    .collection(NestedSyncConfig.USERS_COLLECTION)
                    .document(userId)
                    .collection(NestedSyncConfig.DOCUMENTS_COLLECTION)
                    .document(docId)
                    .collection(NestedSyncConfig.ITEMS_COLLECTION)
                pushed += pushMaps(firestore, itemsRef, group) { ids, max ->
                    bridge.markItemsClean(ids, max)
                }
            }
        return pushed
    }

    private data class PreparedSync(val userId: String, val firestore: FirebaseFirestore)

    /**
     * Pre-sync safety snapshot (full nested JSON, last 3 kept). Never
     * throws: a snapshot must never break a sync.
     */
    private suspend fun writePreSyncSnapshot(documentId: String?) {
        try {
            val json = bridge.exportNestedSnapshotJson()
            withContext(Dispatchers.IO) {
                val dir = File(appContext.filesDir, SNAPSHOT_DIR).apply { mkdirs() }
                val name = "nested_${documentId ?: "docs"}_${Clock.System.now().toEpochMilliseconds()}.json"
                File(dir, name).writeText(json)
                dir.listFiles()
                    ?.sortedBy { it.lastModified() }
                    ?.dropLast(SNAPSHOT_KEEP)
                    ?.forEach { runCatching { it.delete() } }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pre-sync snapshot failed; continuing sync", e)
        }
    }

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
        // Account switch (link, sign-in/out, reinstall-side): clean rows
        // would never upload to the new collection, stranding the device
        // split-brained with a green status. Re-dirty live rows instead.
        val lastUid = dataStore.data.map { it[KEY_LAST_UID] }.first()
        if (lastUid != userId) {
            val revived = bridge.markAllDirty()
            dataStore.edit { prefs -> prefs[KEY_LAST_UID] = userId }
            Log.i(TAG, "Account changed; marked $revived nested rows dirty for re-upload")
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

    /** Ids of bridge JSON rows for delete detection. */
    private fun remoteIds(jsons: List<String>): List<String> =
        jsons.mapNotNull {
            NestedSyncDocument.mapFromJson(it)?.get(NestedSyncDocument.FIELD_ID) as? String
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

    private suspend fun lastSyncedAt(documentId: String): Long? =
        dataStore.data.map { it[longPreferencesKey(syncedKey(documentId))] }.first()

    private suspend fun setLastSyncedAt(documentId: String, value: Long) {
        dataStore.edit { prefs -> prefs[longPreferencesKey(syncedKey(documentId))] = value }
    }

    private fun syncedKey(documentId: String) = "last_synced_$documentId"

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
        private const val SNAPSHOT_DIR = "nested_sync_snapshots"
        private const val SNAPSHOT_KEEP = 3
        private val KEY_LAST_SYNCED_DOCS = longPreferencesKey("last_synced_docs")
        private val KEY_LAST_UID = stringPreferencesKey("uid")
    }
}
