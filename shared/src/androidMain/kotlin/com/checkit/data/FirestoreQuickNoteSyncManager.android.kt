package com.checkit.data

import android.util.Log
import com.checkit.notifications.QuickNoteReminderScheduler
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resumeWithException
import kotlin.time.Clock

/**
 * Firestore sync for QuickNote on Android.
 *
 * Offline-first: every mutation is already in Room before [requestSync] is
 * called. Sync is debounced, idempotent, and safe to retry: local rows
 * (including tombstones) are uploaded, then remote documents are merged
 * back with last-write-wins on `updatedAt`. Failures (e.g. no network,
 * no signed-in user) are swallowed so the UI never waits on Firebase.
 *
 * Auth is anonymous; the UID is stable per install and scopes the
 * `users/{userId}/quickNotes` collection. It can later be linked to a
 * permanent provider without changing note IDs.
 */
class FirestoreQuickNoteSyncManager(
    private val dao: QuickNoteDao,
    private val reminderScheduler: QuickNoteReminderScheduler,
) : QuickNoteSyncManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private var pendingJob: Job? = null

    override fun requestSync() {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(SYNC_DEBOUNCE_MILLIS)
            sync()
        }
    }

    override suspend fun sync() {
        syncMutex.withLock {
            try {
                val userId = ensureUserId() ?: return
                Log.d(TAG, "Sync started (uid=${userId.take(6)}…)")
                val notesRef = FirebaseFirestore.getInstance(DATABASE_ID)
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(NOTES_COLLECTION)

                // Push: all local rows, including tombstones so deletes propagate.
                val local = dao.getAllForSync().map { it.toDomain() }
                local.forEach { note ->
                    notesRef.document(note.id).set(QuickNoteSyncDocument.toMap(note)).await()
                }
                Log.d(TAG, "Sync pushed ${local.size} note(s)")

                // Pull + merge with last-write-wins.
                val remote = notesRef.get().await().documents.mapNotNull { doc ->
                    QuickNoteSyncDocument.fromMap(doc.id, doc.data)
                }
                var applied = 0
                remote.forEach { remoteNote ->
                    val existing = dao.getById(remoteNote.id)?.toDomain()
                    val winner = QuickNoteSyncDocument.resolveLocal(existing, remoteNote)
                    if (winner != null) {
                        dao.upsert(winner.toEntity())
                        applied++
                        if (winner.deleted) {
                            reminderScheduler.cancel(winner.id)
                        }
                    }
                }
                if (applied > 0) {
                    reconcileAlarms()
                }
                Log.i(
                    TAG,
                    "Sync succeeded: pushed=${local.size} pulled=${remote.size} applied=$applied",
                )
            } catch (e: Exception) {
                Log.w(TAG, "QuickNote sync failed; will retry on next request", e)
            }
        }
    }

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
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it, null) }
    addOnFailureListener { cont.resumeWithException(it) }
}
