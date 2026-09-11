package com.checkit.data

/**
 * Single source for Firestore sync topology shared by the Android
 * ([FirestoreQuickNoteSyncManager]) and macOS (Swift `QuickNoteFirestoreSync`)
 * implementations. Timing/backoff policies stay platform-local; everything
 * addressing remote resources lives here so both sides can never diverge.
 */
object QuickNoteSyncConfig {
    const val DATABASE_ID = "checkit"
    const val STORAGE_BUCKET = "gs://aimpact-studio-872e7.firebasestorage.app"
    const val USERS_COLLECTION = "users"
    const val NOTES_COLLECTION = "quickNotes"
    const val ATTACHMENTS_DIR = "quicknote_attachments"
    const val PUSH_BATCH_SIZE = 400
    const val PULL_OVERLAP_MILLIS = 60_000L
    const val BASE_BACKOFF_MILLIS = 30_000L
    const val MAX_BACKOFF_MILLIS = 300_000L
    const val MAX_DOWNLOAD_BYTES = 10L * 1024L * 1024L
}
