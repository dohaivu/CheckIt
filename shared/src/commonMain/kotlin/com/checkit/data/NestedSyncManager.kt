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

/** What a running (or last finished) sync pass was for. */
enum class NestedSyncOperation {
    SYNC,
    LIST_SYNC,
    FORCE_PUSH,
    FORCE_PULL,
}

data class NestedSyncState(
    val status: NestedSyncStatus = NestedSyncStatus.IDLE,
    val documentId: String? = null,
    val lastSyncedAt: Long? = null,
    val message: String? = null,
    val operation: NestedSyncOperation = NestedSyncOperation.SYNC,
)

/**
 * Manual Firestore sync for one open nested document
 * (`users/{userId}/nestedDocuments/{documentId}` plus its `nestedItems`
 * subcollection).
 *
 * Unlike QuickNote there are no automatic triggers: the UI calls
 * [syncDocument] (open document) or [syncDocuments] (document list)
 * explicitly from sync buttons. Implementations push dirty rows, pull full
 * remote state with last-write-wins on `updatedAtMillis`, reconcile rows
 * deleted elsewhere, and purge uploaded tombstones after the shared
 * retention window.
 *
 * [forcePush] and [forcePull] are the explicit escape hatches: they ignore
 * last-write-wins and make one side authoritative for a single document.
 * Both are destructive to the other side, so the UI must confirm first.
 */
interface NestedSyncManager {
    val syncState: StateFlow<NestedSyncState>
    suspend fun syncDocument(documentId: String)
    suspend fun syncDocuments()

    /**
     * Overwrites the cloud copy with this device's document and items
     * (remote-only items are deleted). Returns false when the sync could
     * not complete; details are surfaced through [syncState].
     */
    suspend fun forcePush(documentId: String): Boolean

    /**
     * Replaces this device's document and items with the cloud copy.
     * Returns false when there is nothing to pull or the sync failed;
     * details are surfaced through [syncState].
     */
    suspend fun forcePull(documentId: String): Boolean
}

class NoOpNestedSyncManager : NestedSyncManager {
    private val state = MutableStateFlow(NestedSyncState(NestedSyncStatus.SYNCED))
    override val syncState: StateFlow<NestedSyncState> = state.asStateFlow()
    override suspend fun syncDocument(documentId: String) = Unit
    override suspend fun syncDocuments() = Unit
    override suspend fun forcePush(documentId: String): Boolean = false
    override suspend fun forcePull(documentId: String): Boolean = false
}
