package com.checkit.data

/**
 * Reconciliation hook for Firestore sync (stub for V1).
 *
 * All QuickNote mutations commit to Room first; implementations upload
 * pending state asynchronously and merge remote changes back into Room
 * with last-write-wins on [com.checkit.domain.QuickNote.updatedAt].
 */
interface QuickNoteSyncManager {
    fun requestSync()
    suspend fun sync()
}

class NoOpQuickNoteSyncManager : QuickNoteSyncManager {
    override fun requestSync() = Unit
    override suspend fun sync() = Unit
}
