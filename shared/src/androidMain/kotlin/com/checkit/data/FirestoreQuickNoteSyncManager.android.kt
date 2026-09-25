package com.checkit.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteRules
import com.checkit.domain.QuickNoteType
import com.checkit.notifications.QuickNoteReminderScheduler
import com.checkit.util.awaitTask
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.math.min
import kotlin.time.Clock

private val Context.quickNoteSyncDataStore by preferencesDataStore(name = "quick_note_sync")

/**
 * Firestore sync for QuickNote on Android.
 *
 * Offline-first: every mutation is already in Room before [requestSync] is
 * called. Sync is incremental and never periodic:
 * - pull queries documents newer than the last pull watermark (or all documents
 *   if initial/manual refresh) and merges with LWW;
 * - push uploads remaining dirty rows (batched), then clears the dirty flag;
 * - purge permanently deletes old uploaded tombstones (blob, doc, row);
 * - triggers are local edits (debounced), app resume (via the maintenance
 *   use case), network reconnect, and manual refresh.
 *
 * Failures back off exponentially and are surfaced through [syncState] so
 * the UI can show a banner; the app stays fully usable offline.
 *
 * Media attachments (image/video/audio) are stored in Firebase Storage at
 * `quicknote_attachments/{userId}/{noteId}.webp`; only the download URL is
 * kept in Firestore. Local files never leave the device except via upload.
 *
 * Auth is anonymous; the UID is stable per install and scopes the
 * `users/{userId}/quickNotes` collection. It can later be linked to a
 * permanent provider without changing note IDs.
 */
class FirestoreQuickNoteSyncManager(
    context: Context,
    private val dao: QuickNoteDao,
    private val reminderScheduler: QuickNoteReminderScheduler,
) : QuickNoteSyncManager {
    private val appContext = context.applicationContext
    private val dataStore = appContext.quickNoteSyncDataStore
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow(QuickNoteSyncState())
    override val syncState: StateFlow<QuickNoteSyncState> = _syncState.asStateFlow()
    private var pendingJob: Job? = null
    private var consecutiveFailures = 0
    private var nextRetryAtMillis = 0L

    init {
        scope.launch {
            val lastSyncedAt = dataStore.data.map { it[KEY_LAST_SYNCED_AT] }.first()
            if (lastSyncedAt != null) {
                _syncState.value = QuickNoteSyncState(QuickNoteSyncStatus.SYNCED, lastSyncedAt)
            }
        }
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        requestSync()
                    }
                }
            )
        }.onFailure { e ->
            Log.w(TAG, "Could not register connectivity callback; reconnect sync disabled", e)
        }
    }

    override fun requestSync() {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(SYNC_DEBOUNCE_MILLIS)
            sync()
        }
    }

    /**
     * Immediate sync for explicit user intent (manual refresh, sign-in
     * catch-up). Bypasses the backoff gate and performs a full pull (resetting
     * watermark filter) so existing remote notes are guaranteed to sync.
     */
    override suspend fun syncNow() {
        consecutiveFailures = 0
        nextRetryAtMillis = 0L
        syncInternal(isFullPull = true)
    }

    override suspend fun sync() {
        syncInternal(isFullPull = false)
    }

    private suspend fun syncInternal(isFullPull: Boolean) {
        syncMutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            if (now < nextRetryAtMillis) return
            if (!isOnline()) {
                _syncState.value = QuickNoteSyncState(
                    QuickNoteSyncStatus.OFFLINE,
                    _syncState.value.lastSyncedAt,
                )
                return
            }
            _syncState.value = QuickNoteSyncState(
                QuickNoteSyncStatus.SYNCING,
                _syncState.value.lastSyncedAt,
            )
            try {
                val userId = ensureUserId()
                if (userId == null) {
                    recordFailure("Sign-in failed. Sync will retry automatically.")
                    return
                }
                Log.d(TAG, "Sync started uid=$userId (isFullPull=$isFullPull)")
                // Account switch (link, sign-in/out): clean rows would never
                // upload to the new collection. Re-dirty live rows instead.
                // The pull watermark belongs to the old collection, so reset
                // it: otherwise pre-existing remote rows (older than the
                // watermark) are filtered out forever and never pulled.
                val lastUid = dataStore.data.map { it[KEY_LAST_UID] }.first()
                val isAccountChanged = lastUid != userId
                if (isAccountChanged) {
                    val revived = dao.markAllDirty()
                    dataStore.edit { prefs ->
                        prefs[KEY_LAST_UID] = userId
                        prefs[KEY_LAST_PULL_MILLIS] = 0L
                    }
                    Log.i(TAG, "Account changed; marked $revived notes dirty for re-upload")
                }
                val firestore = FirebaseFirestore.getInstance(QuickNoteSyncConfig.DATABASE_ID)
                val storage = FirebaseStorage.getInstance(QuickNoteSyncConfig.STORAGE_BUCKET)
                val notesRef = firestore
                    .collection(QuickNoteSyncConfig.USERS_COLLECTION)
                    .document(userId)
                    .collection(QuickNoteSyncConfig.NOTES_COLLECTION)

                // PULL & MERGE FIRST:
                // Safely merges remote documents with local state before pushing.
                // If a remote document is newer, local state is overwritten and dirty is cleared,
                // preventing an outdated local client from clobbering remote data.
                val storedLastPull = if (isFullPull || isAccountChanged) 0L else dataStore.data.map { it[KEY_LAST_PULL_MILLIS] ?: 0L }.first()
                val pullStart = Clock.System.now().toEpochMilliseconds()
                val queryTask = if (storedLastPull == 0L) {
                    notesRef.get()
                } else {
                    notesRef.whereGreaterThan(
                        QuickNoteSyncDocument.FIELD_UPDATED_AT,
                        storedLastPull - QuickNoteSyncConfig.PULL_OVERLAP_MILLIS,
                    ).get()
                }
                val remoteDocs = queryTask.awaitTask().documents
                val remote = remoteDocs.mapNotNull { doc ->
                    QuickNoteSyncDocument.fromMap(doc.id, sanitizeFirestoreMap(doc.data.orEmpty()))
                }
                var applied = 0
                var maxRemoteUpdatedAt = storedLastPull
                remote.forEach { remoteNote ->
                    maxRemoteUpdatedAt = maxOf(maxRemoteUpdatedAt, remoteNote.updatedAt)
                    val existing = dao.getById(remoteNote.id)?.toDomain()
                    val winner = QuickNoteSyncDocument.resolveLocal(existing, remoteNote)
                    if (winner != null) {
                        val withAttachment = downloadAttachmentIfNeeded(storage, winner, existing)
                        dao.upsert(withAttachment.toEntity(dirty = false))
                        applied++
                        if (withAttachment.deleted) {
                            reminderScheduler.cancel(withAttachment.id)
                        }
                    }
                }
                dataStore.edit { prefs ->
                    prefs[KEY_LAST_PULL_MILLIS] = maxOf(storedLastPull, pullStart, maxRemoteUpdatedAt)
                    prefs[KEY_LAST_SYNCED_AT] = pullStart
                }
                if (applied > 0) {
                    reconcileAlarms()
                }

                // PUSH REMAINING DIRTY SECOND:
                // Only local notes that are still dirty (local is newer or newly created) are pushed.
                // Upload pending attachments first so their URLs are in the doc push.
                dao.getDirty().map { it.toDomain() }
                    .filter { it.type != QuickNoteType.TEXT && it.attachmentUrl == null }
                    .forEach { note ->
                        val url = uploadAttachment(storage, userId, note)
                        if (url != null) {
                            dao.setAttachmentUrl(
                                note.id,
                                url,
                                Clock.System.now().toEpochMilliseconds(),
                            )
                        }
                    }

                val toPush = dao.getDirty().map { it.toDomain() }
                    .filterNot { needsAttachmentUpload(it) }

                toPush.chunked(QuickNoteSyncConfig.PUSH_BATCH_SIZE).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { note ->
                        batch.set(notesRef.document(note.id), QuickNoteSyncDocument.toMap(note))
                    }
                    batch.commit().awaitTask()
                    dao.markClean(chunk.map { it.id }, chunk.maxOf { it.updatedAt })
                }

                // PURGE TOMBSTONES LAST:
                val purged = purgeTombstones(storage, notesRef, pullStart)
                consecutiveFailures = 0
                nextRetryAtMillis = 0L
                _syncState.value = QuickNoteSyncState(QuickNoteSyncStatus.SYNCED, pullStart)
                Log.i(
                    TAG,
                    "Sync succeeded: pulled=${remote.size} applied=$applied " +
                        "pushed=${toPush.size} purged=$purged",
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val offline = !isOnline()
                recordFailure(
                    if (offline) "You're offline. Changes are saved on this device."
                    else "Sync failed (${e.message ?: "unknown error"}). Will retry automatically."
                )
            }
        }
    }

    private suspend fun recordFailure(message: String) {
        consecutiveFailures++
        val backoff = min(
            QuickNoteSyncConfig.BASE_BACKOFF_MILLIS * (1L shl min(consecutiveFailures - 1, 4)),
            QuickNoteSyncConfig.MAX_BACKOFF_MILLIS,
        )
        nextRetryAtMillis = Clock.System.now().toEpochMilliseconds() + backoff
        _syncState.value = QuickNoteSyncState(
            QuickNoteSyncStatus.ERROR,
            _syncState.value.lastSyncedAt,
            message,
        )
        Log.w(TAG, "QuickNote sync failed; retry in ${backoff}ms")
    }

    /**
     * Permanent deletion for old tombstones: Storage blob, Firestore
     * document, then the local row — in that order, so a failure leaves the
     * tombstone intact for the next run instead of resurrecting the note.
     * Only tombstones confirmed uploaded (dirty = 0) are eligible, so every
     * device had a chance to sync the deletion first.
     */
    private suspend fun purgeTombstones(
        storage: FirebaseStorage,
        notesRef: CollectionReference,
        now: Long,
    ): Int {
        val purgeable = dao.getPurgeableTombstones(now - QuickNoteRules.PURGE_AFTER_MILLIS)
        var purged = 0
        purgeable.forEach { entity ->
            val note = entity.toDomain()
            try {
                note.attachmentUrl?.let { url ->
                    try {
                        storage.getReferenceFromUrl(url).delete().awaitTask()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.w(TAG, "Blob delete failed for ${note.id}, continuing purge", e)
                    }
                }
                notesRef.document(note.id).delete().awaitTask()
                dao.hardDelete(listOf(note.id))
                purged++
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Purge failed for ${note.id}; will retry next sync", e)
            }
        }
        if (purged > 0) {
            Log.i(TAG, "Purged $purged tombstone(s)")
        }
        return purged
    }

    private fun isOnline(): Boolean =
        runCatching { connectivityManager.activeNetwork != null }.getOrDefault(true)

    private fun needsAttachmentUpload(note: QuickNote): Boolean {
        if (note.type == QuickNoteType.TEXT || note.attachmentUrl != null) return false
        val path = note.attachmentLocalPath ?: return false
        return File(path).exists()
    }

    private suspend fun uploadAttachment(
        storage: FirebaseStorage,
        userId: String,
        note: QuickNote,
    ): String? {
        val path = note.attachmentLocalPath ?: return null
        return try {
            val file = File(path)
            if (!file.exists()) {
                Log.w(TAG, "Attachment missing for ${note.id}, skipping upload")
                return null
            }
            val ref = storage.reference.child("${QuickNoteSyncConfig.ATTACHMENTS_DIR}/$userId/${note.id}.webp")
            ref.putFile(Uri.fromFile(file)).awaitTask()
            ref.downloadUrl.awaitTask().toString()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Attachment upload failed for ${note.id}", e)
            null
        }
    }

    /**
     * Fetches the remote attachment when we don't already have it locally.
     * The local path is preserved when the URL is unchanged (or when the
     * download fails, as a display fallback) and never synced back.
     */
    private suspend fun downloadAttachmentIfNeeded(
        storage: FirebaseStorage,
        winner: QuickNote,
        existing: QuickNote?,
    ): QuickNote {
        val url = winner.attachmentUrl
        if (winner.type == QuickNoteType.TEXT || url == null) {
            return winner.copy(attachmentLocalPath = existing?.attachmentLocalPath)
        }
        val cached = existing?.attachmentLocalPath
            ?.takeIf { it.isNotBlank() }
            ?.let { path -> if (File(path).exists() && existing.attachmentUrl == url) path else null }
        if (cached != null) return winner.copy(attachmentLocalPath = cached)
        return try {
            val bytes = storage.getReferenceFromUrl(url).getBytes(QuickNoteSyncConfig.MAX_DOWNLOAD_BYTES).awaitTask()
            val file = File(attachmentDir(), "${winner.id}.webp")
            file.writeBytes(bytes)
            Log.d(TAG, "Downloaded attachment for ${winner.id} (${bytes.size} bytes)")
            winner.copy(attachmentLocalPath = file.absolutePath)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Attachment download failed for ${winner.id}", e)
            winner.copy(attachmentLocalPath = existing?.attachmentLocalPath)
        }
    }

    private fun attachmentDir(): File =
        File(appContext.filesDir, ATTACHMENTS_SUBDIR).apply { mkdirs() }

    private suspend fun reconcileAlarms() {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.getScheduledReminders()
            .map { it.toDomain() }
            .filter { it.remindAt != null && it.remindAt > now }
            .forEach { reminderScheduler.schedule(it) }
    }

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
        if (e is CancellationException) throw e
        if (e.message?.contains("CONFIGURATION_NOT_FOUND") == true) {
            Log.w(
                TAG,
                "Anonymous sign-in failed with CONFIGURATION_NOT_FOUND: " +
                    "enable Authentication > Sign-in method > Anonymous in the Firebase console, " +
                    "and check that google-services.json matches that project. " +
                    "Sync is paused until then; the app keeps working offline.",
                e,
            )
        } else {
            Log.w(TAG, "QuickNote anonymous sign-in failed", e)
        }
        null
    }

    /** Converts Firestore timestamps in a document map to millisecond longs. */
    private fun sanitizeFirestoreMap(map: Map<String, Any?>): Map<String, Any?> =
        map.mapValues { (_, value) ->
            if (value is Timestamp) {
                value.seconds * 1000 + value.nanoseconds / 1000000
            } else {
                value
            }
        }

    companion object {
        private const val TAG = "QuickNoteSync"
        private const val ATTACHMENTS_SUBDIR = "quicknote_images"
        private const val SYNC_DEBOUNCE_MILLIS = 30_000L
        private val KEY_LAST_PULL_MILLIS = longPreferencesKey("last_pull_millis")
        private val KEY_LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
        private val KEY_LAST_UID = stringPreferencesKey("uid")
    }
}
