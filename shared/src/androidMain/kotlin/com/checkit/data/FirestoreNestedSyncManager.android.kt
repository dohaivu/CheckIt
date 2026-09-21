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
                var purged = 0
                val purgeCutoff = pullStart - QuickNoteRules.PURGE_AFTER_MILLIS
                bridge.purgeableItemIds(documentId, purgeCutoff).forEach { id ->
                    try {
                        itemsRef.document(id).delete().awaitTask()
                        bridge.hardDeleteItems(listOf(id))
                        purged++
                    } catch (e: Exception) {
                        Log.w(TAG, "Nested purge failed for item $id; will retry next sync", e)
                    }
                }
                bridge.purgeableDocumentId(documentId, purgeCutoff)?.let { id ->
                    try {
                        docRef.delete().awaitTask()
                        bridge.hardDeleteDocument(id)
                        purged++
                    } catch (e: Exception) {
                        Log.w(TAG, "Nested purge failed for document $id; will retry next sync", e)
                    }
                }

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

    private suspend fun recordFailure(documentId: String, message: String) {
        _syncState.value = NestedSyncState(
            NestedSyncStatus.ERROR,
            documentId,
            lastSyncedAt(documentId),
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
    }
}
