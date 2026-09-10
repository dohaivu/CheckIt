package com.checkit.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.checkit.notifications.QuickNoteReminderScheduler
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import kotlin.time.Clock

private val Context.quickNoteSyncDataStore by preferencesDataStore(name = "quick_note_sync")

/**
 * Firestore sync for QuickNote on Android.
 *
 * Offline-first: every mutation is already in Room before [requestSync] is
 * called. Sync is incremental and never periodic:
 * - push uploads only rows flagged dirty (batched), then clears the flag;
 * - pull queries only documents newer than the last pull watermark;
 * - triggers are local edits (debounced), app resume (via the maintenance
 *   use case), network reconnect, and manual refresh.
 *
 * Failures back off exponentially and are surfaced through [syncState] so
 * the UI can show a banner; the app stays fully usable offline.
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

    override suspend fun sync() {
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
                Log.d(TAG, "Sync started (uid=${userId.take(6)}…)")
                val notesRef = FirebaseFirestore.getInstance(DATABASE_ID)
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(NOTES_COLLECTION)

                // Push: only locally changed rows, including tombstones.
                val dirty = dao.getDirty().map { it.toDomain() }
                dirty.chunked(PUSH_BATCH_SIZE).forEach { chunk ->
                    val batch = FirebaseFirestore.getInstance(DATABASE_ID).batch()
                    chunk.forEach { note ->
                        batch.set(notesRef.document(note.id), QuickNoteSyncDocument.toMap(note))
                    }
                    batch.commit().await()
                }
                if (dirty.isNotEmpty()) {
                    dao.markClean(dirty.map { it.id }, dirty.maxOf { it.updatedAt })
                }

                // Pull: only documents newer than the last pull (with overlap
                // margin for clock skew; LWW merge keeps re-pulls idempotent).
                val lastPull = dataStore.data.map { it[KEY_LAST_PULL_MILLIS] ?: 0L }.first()
                val pullStart = Clock.System.now().toEpochMilliseconds()
                val remote = notesRef
                    .whereGreaterThan(
                        QuickNoteSyncDocument.FIELD_UPDATED_AT,
                        lastPull - PULL_OVERLAP_MILLIS,
                    )
                    .get().await().documents.mapNotNull { doc ->
                        QuickNoteSyncDocument.fromMap(doc.id, doc.data)
                    }
                var applied = 0
                var maxRemoteUpdatedAt = lastPull
                remote.forEach { remoteNote ->
                    maxRemoteUpdatedAt = maxOf(maxRemoteUpdatedAt, remoteNote.updatedAt)
                    val existing = dao.getById(remoteNote.id)?.toDomain()
                    val winner = QuickNoteSyncDocument.resolveLocal(existing, remoteNote)
                    if (winner != null) {
                        dao.upsert(winner.toEntity(dirty = false))
                        applied++
                        if (winner.deleted) {
                            reminderScheduler.cancel(winner.id)
                        }
                    }
                }
                dataStore.edit { prefs ->
                    prefs[KEY_LAST_PULL_MILLIS] = maxOf(lastPull, pullStart, maxRemoteUpdatedAt)
                    prefs[KEY_LAST_SYNCED_AT] = pullStart
                }
                if (applied > 0) {
                    reconcileAlarms()
                }
                consecutiveFailures = 0
                nextRetryAtMillis = 0L
                _syncState.value = QuickNoteSyncState(QuickNoteSyncStatus.SYNCED, pullStart)
                Log.i(
                    TAG,
                    "Sync succeeded: pushed=${dirty.size} pulled=${remote.size} applied=$applied",
                )
            } catch (e: Exception) {
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
            BASE_BACKOFF_MILLIS * (1L shl min(consecutiveFailures - 1, 4)),
            MAX_BACKOFF_MILLIS,
        )
        nextRetryAtMillis = Clock.System.now().toEpochMilliseconds() + backoff
        _syncState.value = QuickNoteSyncState(
            QuickNoteSyncStatus.ERROR,
            _syncState.value.lastSyncedAt,
            message,
        )
        Log.w(TAG, "QuickNote sync failed; retry in ${backoff}ms")
    }

    private fun isOnline(): Boolean =
        runCatching { connectivityManager.activeNetwork != null }.getOrDefault(true)

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
            val uid = auth.signInAnonymously().await().user?.uid
            if (uid != null) {
                Log.d(TAG, "Anonymous sign-in succeeded (uid=${uid.take(6)}…)")
            }
            uid
        }
    } catch (e: Exception) {
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

    companion object {
        private const val TAG = "QuickNoteSync"
        private const val DATABASE_ID = "checkit"
        private const val USERS_COLLECTION = "users"
        private const val NOTES_COLLECTION = "quickNotes"
        private const val SYNC_DEBOUNCE_MILLIS = 1_500L
        private const val PUSH_BATCH_SIZE = 400
        private const val PULL_OVERLAP_MILLIS = 60_000L
        private const val BASE_BACKOFF_MILLIS = 30_000L
        private const val MAX_BACKOFF_MILLIS = 300_000L
        private val KEY_LAST_PULL_MILLIS = longPreferencesKey("last_pull_millis")
        private val KEY_LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it, null) }
    addOnFailureListener { cont.resumeWithException(it) }
}
