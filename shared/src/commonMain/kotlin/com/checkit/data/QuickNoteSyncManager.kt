package com.checkit.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class QuickNoteSyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR,
}

data class QuickNoteSyncState(
    val status: QuickNoteSyncStatus = QuickNoteSyncStatus.IDLE,
    val lastSyncedAt: Long? = null,
    val message: String? = null,
)

/**
 * Reconciliation with Firestore (`users/{userId}/quickNotes/{quickNoteId}`).
 *
 * All QuickNote mutations commit to Room first; implementations upload
 * pending state asynchronously and merge remote changes back into Room
 * with last-write-wins on [com.checkit.domain.QuickNote.updatedAt].
 * iOS uses the [NoOpQuickNoteSyncManager]; Android syncs via Firestore.
 *
 * Sync is incremental, never periodic: only rows marked dirty are pushed,
 * and pulls are limited to documents newer than the last pull. Triggers
 * are local edits (debounced), app resume, reconnect, and manual refresh.
 */
interface QuickNoteSyncManager {
    val syncState: StateFlow<QuickNoteSyncState>
    fun requestSync()
    suspend fun sync()
}

class NoOpQuickNoteSyncManager : QuickNoteSyncManager {
    private val state = MutableStateFlow(QuickNoteSyncState(QuickNoteSyncStatus.SYNCED))
    override val syncState: StateFlow<QuickNoteSyncState> = state.asStateFlow()
    override fun requestSync() = Unit
    override suspend fun sync() = Unit
}
