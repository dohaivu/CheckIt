package com.checkit.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NestedSyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR,
}

data class NestedSyncState(
    val status: NestedSyncStatus = NestedSyncStatus.IDLE,
    val documentId: String? = null,
    val lastSyncedAt: Long? = null,
    val message: String? = null,
)

/**
 * Manual Firestore sync for one open nested document
 * (`users/{userId}/nestedDocuments/{documentId}` plus its `nestedItems`
 * subcollection).
 *
 * Unlike QuickNote there are no automatic triggers: the UI calls
 * [syncDocument] (open document) or [syncDocuments] (document list)
 * explicitly from sync buttons. Implementations push dirty rows, pull
 * watermarked remote changes with last-write-wins on `updatedAtMillis`,
 * and purge uploaded tombstones after the shared retention window.
 */
interface NestedSyncManager {
    val syncState: StateFlow<NestedSyncState>
    suspend fun syncDocument(documentId: String)
    suspend fun syncDocuments()
}

class NoOpNestedSyncManager : NestedSyncManager {
    private val state = MutableStateFlow(NestedSyncState(NestedSyncStatus.SYNCED))
    override val syncState: StateFlow<NestedSyncState> = state.asStateFlow()
    override suspend fun syncDocument(documentId: String) = Unit
    override suspend fun syncDocuments() = Unit
}
