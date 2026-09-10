package com.checkit.data

/**
 * Reconciliation with Firestore (`users/{userId}/quickNotes/{quickNoteId}`).
 *
 * All QuickNote mutations commit to Room first; implementations upload
 * pending state asynchronously and merge remote changes back into Room
 * with last-write-wins on [com.checkit.domain.QuickNote.updatedAt].
 * iOS uses the [NoOpQuickNoteSyncManager]; Android syncs via Firestore.
 */
interface QuickNoteSyncManager {
    fun requestSync()
    suspend fun sync()
}

class NoOpQuickNoteSyncManager : QuickNoteSyncManager {
    override fun requestSync() = Unit
    override suspend fun sync() = Unit
}
