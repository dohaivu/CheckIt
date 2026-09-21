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
 * Manual Firestore sync for one open nested document on Android.
 *
 * Offline-first: every mutation is already in Room; [syncDocument] pushes
 * dirty rows of that document, pulls watermarked remote changes with
 * last-write-wins on `updatedAtMillis`, then purges uploaded tombstones
 * after the shared retention window.
 *
 * Unlike QuickNote there are no automatic triggers (no debounce, no
 * reconnect callback): the UI calls [syncDocument] explicitly from a sync
 * button. Failures are surfaced through [syncState]; the app stays fully
 * usable offline.
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
            if (!isOnline()) {
                _syncState.value = NestedSyncState(
                    NestedSyncStatus.OFFLINE,
                    documentId,
                    lastSyncedAt(documentId),
                )
                return
            }
            _syncState.value = NestedSyncState(
                NestedSyncStatus.SYNCING,
                documentId,
                lastSyncedAt(documentId),
            )
            try {
                val userId = ensureUserId()
                if (userId == null) {
                    recordFailure(documentId, "Sign-in failed. Try again.")
                    return
                }
                Log.d(TAG, "Nested sync started uid=$userId doc=$documentId")
                val firestore = FirebaseFirestore.getInstance(NestedSyncConfig.DATABASE_ID)
                val docRef = firestore
                    .collection(NestedSyncConfig.USERS_COLLECTION)
                    .document(userId)
                    .collection(NestedSyncConfig.DOCUMENTS_COLLECTION)
                    .document(documentId)
                val itemsRef = docRef.collection(NestedSyncConfig.ITEMS_COLLECTION)

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
                bridge.dirtyItemJsons(documentId)
                    .mapNotNull { json ->
                        NestedSyncDocument.mapFromJson(json)?.let { json to it }
                    }
                    .chunked(NestedSyncConfig.PUSH_BATCH_SIZE)
                    .let { chunks ->
                        chunks.forEach { chunk ->
                            val batch = firestore.batch()
                            chunk.forEach { (_, map) ->
                                val id = map[NestedSyncDocument.FIELD_ID] as? String ?: return@forEach
                                batch.set(itemsRef.document(id), map)
                            }
                            batch.commit().awaitTask()
                        }
                        val pushedMaps = chunks.flatten().map { it.second }
                        if (pushedMaps.isNotEmpty()) {
                            pushed += pushedMaps.size
                            bridge.markItemsClean(
                                pushedMaps.mapNotNull { it[NestedSyncDocument.FIELD_ID] as? String },
                                pushedMaps.maxOf { (it[NestedSyncDocument.FIELD_UPDATED_AT] as? Number)?.toLong() ?: 0L },
                            )
                        }
                    }

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
                val remoteItems = itemsRef
                    .whereGreaterThan(
                        NestedSyncDocument.FIELD_UPDATED_AT,
                        lastPull - NestedSyncConfig.PULL_OVERLAP_MILLIS,
                    )
                    .get().awaitTask().documents
                var maxRemoteUpdatedAt = lastPull
                remoteItems.forEach { doc ->
                    val data = doc.data.orEmpty() + (NestedSyncDocument.FIELD_ID to doc.id)
                    maxRemoteUpdatedAt = maxOf(
                        maxRemoteUpdatedAt,
                        (data[NestedSyncDocument.FIELD_UPDATED_AT] as? Number)?.toLong() ?: lastPull,
                    )
                    if (bridge.applyRemoteItemJson(NestedSyncDocument.toJson(data))) {
                        applied++
                    }
                }
                setLastPullMillis(documentId, maxOf(lastPull, pullStart, maxRemoteUpdatedAt))

                // Purge: uploaded item tombstones, then the document tombstone.
                val purgeCutoff = pullStart - QuickNoteRules.PURGE_AFTER_MILLIS
                val purged = purgeItems(itemsRef, documentId, purgeCutoff) +
                    purgeDocument(docRef, documentId, purgeCutoff)

                setLastSyncedAt(documentId, pullStart)
                _syncState.value = NestedSyncState(NestedSyncStatus.SYNCED, documentId, pullStart)
                Log.i(
                    TAG,
                    "Nested sync succeeded doc=$documentId pushed=$pushed " +
                        "pulled=${remoteItems.size} applied=$applied purged=$purged",
                )
            } catch (e: Exception) {
                val offline = !isOnline()
                recordFailure(
                    documentId,
                    if (offline) "You're offline. Changes are saved on this device."
                    else "Sync failed (${e.message ?: "unknown error"}). Try again.",
                )
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
            if (!isOnline()) {
                _syncState.value = NestedSyncState(
                    NestedSyncStatus.OFFLINE,
                    null,
                    lastSyncedDocsAt(),
                )
                return
            }
            _syncState.value = NestedSyncState(
                NestedSyncStatus.SYNCING,
                null,
                lastSyncedDocsAt(),
            )
            try {
                val userId = ensureUserId()
                if (userId == null) {
                    recordFailure(null, "Sign-in failed. Try again.")
                    return
                }
                Log.d(TAG, "Nested docs sync started uid=$userId")
                val firestore = FirebaseFirestore.getInstance(NestedSyncConfig.DATABASE_ID)
                val docsRef = firestore
                    .collection(NestedSyncConfig.USERS_COLLECTION)
                    .document(userId)
                    .collection(NestedSyncConfig.DOCUMENTS_COLLECTION)

                // Push: all dirty document rows (batched).
                var pushed = 0
                bridge.dirtyDocumentJsons()
                    .mapNotNull { NestedSyncDocument.mapFromJson(it) }
                    .chunked(NestedSyncConfig.PUSH_BATCH_SIZE)
                    .let { chunks ->
                        chunks.forEach { chunk ->
                            val batch = firestore.batch()
                            chunk.forEach { map ->
                                val id = map[NestedSyncDocument.FIELD_ID] as? String ?: return@forEach
                                batch.set(docsRef.document(id), map)
                            }
                            batch.commit().awaitTask()
                        }
                        val flat = chunks.flatten()
                        if (flat.isNotEmpty()) {
                            pushed = flat.size
                            bridge.markDocumentsClean(
                                flat.mapNotNull { it[NestedSyncDocument.FIELD_ID] as? String },
                                flat.maxOf { (it[NestedSyncDocument.FIELD_UPDATED_AT] as? Number)?.toLong() ?: 0L },
                            )
                        }
                    }

                // Pull: watermarked documents (overlap margin for clock skew).
                var applied = 0
                val lastPull = lastPullDocsMillis()
                val pullStart = Clock.System.now().toEpochMilliseconds()
                val remoteDocs = docsRef
                    .whereGreaterThan(
                        NestedSyncDocument.FIELD_UPDATED_AT,
                        lastPull - NestedSyncConfig.PULL_OVERLAP_MILLIS,
                    )
                    .get().awaitTask().documents
                var maxRemoteUpdatedAt = lastPull
                remoteDocs.forEach { doc ->
                    val data = doc.data.orEmpty() + (NestedSyncDocument.FIELD_ID to doc.id)
                    maxRemoteUpdatedAt = maxOf(
                        maxRemoteUpdatedAt,
                        (data[NestedSyncDocument.FIELD_UPDATED_AT] as? Number)?.toLong() ?: lastPull,
                    )
                    if (bridge.applyRemoteDocumentJson(NestedSyncDocument.toJson(data))) {
                        applied++
                    }
                }
                setLastPullDocsMillis(maxOf(lastPull, pullStart, maxRemoteUpdatedAt))

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
                        "pulled=${remoteDocs.size} applied=$applied purged=$purged",
                )
            } catch (e: Exception) {
                val offline = !isOnline()
                recordFailure(
                    null,
                    if (offline) "You're offline. Changes are saved on this device."
                    else "Sync failed (${e.message ?: "unknown error"}). Try again.",
                )
            }
        }
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
        if (bridge.dirtyItemJsons(documentId).isNotEmpty()) return 0
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
