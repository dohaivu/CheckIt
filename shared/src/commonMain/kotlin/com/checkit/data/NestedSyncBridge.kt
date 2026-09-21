package com.checkit.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Swift-friendly Room access for Firestore sync of one nested document,
 * shared by Android (`FirestoreNestedSyncManager`) and macOS (Swift
 * `NestedFirestoreSync`). Document mapping stays single-sourced in
 * [NestedSyncDocument]; platforms exchange JSON strings and ids/booleans.
 *
 * Sync is manual and scoped to the open document: the UI calls
 * `NestedSyncManager.syncDocument(documentId)`; nothing here triggers
 * automatically.
 */
class NestedSyncBridge(
    private val dao: CheckItDao,
) {
    /** JSON-encoded push document for the row, or null when not dirty/missing. */
    suspend fun dirtyDocumentJson(documentId: String): String? {
        val entity = dao.nestedDocumentById(documentId)?.takeIf { it.dirty } ?: return null
        return NestedSyncDocument.toJson(
            NestedSyncDocument.docToMap(
                id = entity.id,
                title = entity.title,
                createdAtMillis = entity.createdAtMillis,
                updatedAtMillis = entity.updatedAtMillis,
                deleted = entity.deleted,
            )
        )
    }

    /** JSON-encoded push documents for all dirty documents (list sync). */
    suspend fun dirtyDocumentJsons(): List<String> {
        return dao.getDirtyNestedDocuments().map { entity ->
            NestedSyncDocument.toJson(
                NestedSyncDocument.docToMap(
                    id = entity.id,
                    title = entity.title,
                    createdAtMillis = entity.createdAtMillis,
                    updatedAtMillis = entity.updatedAtMillis,
                    deleted = entity.deleted,
                )
            )
        }
    }

    /** JSON-encoded push documents for dirty items of one document. */
    suspend fun dirtyItemJsons(documentId: String): List<String> {
        val items = dao.getDirtyNestedItemsForDocument(documentId)
        if (items.isEmpty()) return emptyList()
        val tagsByItem = dao.nestedItemTagsForItems(items.map { it.id })
            .groupBy { it.itemId }.mapValues { entry -> entry.value.map { it.tagId } }
        return items.map { entity ->
            NestedSyncDocument.toJson(
                NestedSyncDocument.itemToMap(
                    id = entity.id,
                    documentId = entity.documentId,
                    parentId = entity.parentId,
                    position = entity.position,
                    text = entity.text,
                    note = entity.note,
                    checkboxEnabled = entity.checkboxEnabled,
                    checked = entity.checked,
                    collapsed = entity.collapsed,
                    textStyle = entity.textStyle,
                    textColor = entity.textColor,
                    backgroundColor = entity.backgroundColor,
                    startDateEpochDays = entity.startDateEpochDays,
                    endDateEpochDays = entity.endDateEpochDays,
                    priority = entity.priority,
                    actualMinutes = entity.actualMinutes,
                    metricRollupPolicy = entity.metricRollupPolicy,
                    showTrackedMinutes = entity.showTrackedMinutes,
                    progressPercent = entity.progressPercent,
                    manualMetricsJson = entity.manualMetricsJson,
                    tagIds = tagsByItem[entity.id].orEmpty(),
                    createdAtMillis = entity.createdAtMillis,
                    updatedAtMillis = entity.updatedAtMillis,
                    deleted = entity.deleted,
                )
            )
        }
    }

    suspend fun markDocumentClean(id: String, maxUpdatedAt: Long) {
        dao.markNestedDocumentsClean(listOf(id), maxUpdatedAt)
    }

    suspend fun markDocumentsClean(ids: List<String>, maxUpdatedAt: Long) {
        if (ids.isNotEmpty()) dao.markNestedDocumentsClean(ids, maxUpdatedAt)
    }

    suspend fun markItemsClean(ids: List<String>, maxUpdatedAt: Long) {
        if (ids.isNotEmpty()) dao.markNestedItemsClean(ids, maxUpdatedAt)
    }

    /**
     * Last-write-wins merge of one remote document into Room. Returns true
     * when the remote won and was applied with dirty = false.
     */
    suspend fun applyRemoteDocumentJson(json: String): Boolean {
        val map = NestedSyncDocument.mapFromJson(json) ?: return false
        val remote = NestedSyncDocument.docFromMap(
            documentId = map[NestedSyncDocument.FIELD_ID] as? String ?: "",
            map = map,
        ) ?: return false
        val existing = dao.nestedDocumentById(remote.id)
        if (!NestedSyncDocument.shouldApplyRemote(existing?.updatedAtMillis, remote.updatedAtMillis)) {
            return false
        }
        dao.insertNestedDocument(
            NestedDocumentEntity(
                id = remote.id,
                title = remote.title,
                createdAtMillis = remote.createdAtMillis,
                updatedAtMillis = remote.updatedAtMillis,
                dirty = false,
                deleted = remote.deleted,
            )
        )
        return true
    }

    /**
     * Last-write-wins merge of one remote item into Room. Tag membership is
     * rebuilt from the embedded ids (unknown tags dropped for the FK).
     * Returns true when the remote won and was applied with dirty = false.
     */
    suspend fun applyRemoteItemJson(json: String): Boolean {
        val map = NestedSyncDocument.mapFromJson(json) ?: return false
        val remote = NestedSyncDocument.itemFromMap(
            documentId = map[NestedSyncDocument.FIELD_DOCUMENT_ID] as? String ?: "",
            map = map,
        ) ?: return false
        if (remote.documentId.isBlank()) return false
        val existing = dao.nestedItemById(remote.id)
        if (!NestedSyncDocument.shouldApplyRemote(existing?.updatedAtMillis, remote.updatedAtMillis)) {
            return false
        }
        dao.insertNestedListItem(
            NestedListItemEntity(
                id = remote.id,
                documentId = remote.documentId,
                parentId = remote.parentId,
                position = remote.position,
                text = remote.text,
                note = remote.note,
                checkboxEnabled = remote.checkboxEnabled,
                checked = remote.checked,
                collapsed = remote.collapsed,
                textStyle = remote.textStyle,
                textColor = remote.textColor,
                backgroundColor = remote.backgroundColor,
                startDateEpochDays = remote.startDateEpochDays,
                endDateEpochDays = remote.endDateEpochDays,
                priority = remote.priority,
                actualMinutes = remote.actualMinutes,
                metricRollupPolicy = remote.metricRollupPolicy,
                showTrackedMinutes = remote.showTrackedMinutes,
                progressPercent = remote.progressPercent,
                createdAtMillis = remote.createdAtMillis,
                updatedAtMillis = remote.updatedAtMillis,
                dirty = false,
                deleted = remote.deleted,
                manualMetricsJson = remote.manualMetricsJson,
            )
        )
        val knownTagIds = if (remote.tagIds.isNotEmpty()) {
            dao.tagsByIds(remote.tagIds).map { it.id }.toSet()
        } else emptySet()
        dao.replaceNestedItemTags(remote.id, remote.tagIds.filter { it in knownTagIds })
        return true
    }

    /** Ids of uploaded tombstones of one document old enough to purge. */
    suspend fun purgeableItemIds(documentId: String, cutoffMillis: Long): List<String> =
        dao.getPurgeableNestedItemIdsForDocument(documentId, cutoffMillis)

    /** The document id when its tombstone is uploaded and old enough to purge. */
    suspend fun purgeableDocumentId(documentId: String, cutoffMillis: Long): String? {
        val entity = dao.nestedDocumentById(documentId) ?: return null
        return entity.id.takeIf {
            entity.deleted && !entity.dirty && entity.updatedAtMillis <= cutoffMillis
        }
    }

    /** Ids of uploaded document tombstones old enough to purge. */
    suspend fun purgeableDocumentIds(cutoffMillis: Long): List<String> =
        dao.getPurgeableNestedDocumentTombstones(cutoffMillis).map { it.id }

    suspend fun hardDeleteItems(ids: List<String>) {
        if (ids.isNotEmpty()) dao.hardDeleteNestedItems(ids)
    }

    suspend fun hardDeleteDocument(id: String) {
        dao.hardDeleteNestedDocuments(listOf(id))
    }
}
