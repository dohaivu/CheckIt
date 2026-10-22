package com.checkit.data

/**
 * Pure decision logic for the manual "force push" / "force pull" actions,
 * kept out of the Firestore orchestration so both platforms share it and it
 * stays unit testable.
 *
 * Force operations deliberately bypass last-write-wins. Force push makes the
 * local rows authoritative (clock bumped to `now` so other devices accept
 * them), while force pull takes the remote rows verbatim with `dirty = false`
 * so they are not re-uploaded.
 */
object NestedForceSync {

    /** Local document as it is staged for a force push, clock bumped to [nowMillis]. */
    fun documentMap(document: NestedDocumentEntity, nowMillis: Long): Map<String, Any?> =
        NestedSyncDocument.docToMap(
            id = document.id,
            title = document.title,
            sortOrder = document.sortOrder,
            createdAtMillis = document.createdAtMillis,
            updatedAtMillis = nowMillis,
            deleted = document.deleted,
        )

    /** Local item as it is staged for a force push, clock bumped to [nowMillis]. */
    fun itemMap(
        item: NestedListItemEntity,
        tagIds: List<String>,
        nowMillis: Long,
    ): Map<String, Any?> =
        NestedSyncDocument.itemToMap(
            id = item.id,
            documentId = item.documentId,
            parentId = item.parentId,
            position = item.position,
            text = item.text,
            note = item.note,
            checkboxEnabled = item.checkboxEnabled,
            checked = item.checked,
            completedAtMillis = item.completedAtMillis,
            collapsed = item.collapsed,
            textStyle = item.textStyle,
            textColor = item.textColor,
            backgroundColor = item.backgroundColor,
            startDateEpochDays = item.startDateEpochDays,
            endDateEpochDays = item.endDateEpochDays,
            priority = item.priority,
            actualMinutes = item.actualMinutes,
            metricRollupPolicy = item.metricRollupPolicy,
            showTrackedMinutes = item.showTrackedMinutes,
            progressPercent = item.progressPercent,
            manualMetricsJson = item.manualMetricsJson,
            tagIds = tagIds,
            createdAtMillis = item.createdAtMillis,
            updatedAtMillis = nowMillis,
            deleted = item.deleted,
        )

    /**
     * Remote item ids missing locally. A force push mirrors the device, so
     * these are deleted from Firestore (including remote tombstones).
     */
    fun remoteExtrasToDelete(localIds: List<String>, remoteIds: List<String>): List<String> {
        if (remoteIds.isEmpty()) return emptyList()
        val local = localIds.toSet()
        return remoteIds.filter { it !in local }
    }

    /**
     * Remote document row for a force pull, keeping its clocks verbatim and
     * marking it clean. Returns null for a missing/invalid or tombstoned row.
     */
    fun pullDocument(
        map: Map<String, Any?>,
        documentId: String,
    ): NestedDocumentEntity? {
        val remote = NestedSyncDocument.docFromMap(documentId, map) ?: return null
        if (remote.deleted) return null
        return NestedDocumentEntity(
            id = remote.id,
            title = remote.title,
            sortOrder = remote.sortOrder,
            createdAtMillis = remote.createdAtMillis,
            updatedAtMillis = remote.updatedAtMillis,
            dirty = false,
            deleted = false,
        )
    }

    /**
     * Remote item rows for a force pull: parents ordered before children,
     * orphans reparented to the root, unknown tag ids dropped (tags foreign
     * key), remote clocks kept and rows clean. Deleted remote rows are
     * skipped so the local copy holds only live cloud content.
     */
    fun pullItems(
        items: List<RemoteNestedItem>,
        documentId: String,
        knownTagIds: Set<String>,
    ): List<PulledNestedItem> {
        val live = items.filter { !it.deleted }.map { it.copy(documentId = documentId) }
        if (live.isEmpty()) return emptyList()
        return NestedSyncDocument.orderForApply(live, localIds = emptySet()).map { remote ->
            PulledNestedItem(
                item = NestedListItemEntity(
                    id = remote.id,
                    documentId = remote.documentId,
                    parentId = remote.parentId,
                    position = remote.position,
                    text = remote.text,
                    note = remote.note,
                    checkboxEnabled = remote.checkboxEnabled,
                    checked = remote.checked,
                    completedAtMillis = remote.completedAtMillis,
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
                    manualMetricsJson = remote.manualMetricsJson,
                    createdAtMillis = remote.createdAtMillis,
                    updatedAtMillis = remote.updatedAtMillis,
                    dirty = false,
                    deleted = false,
                ),
                tagIds = remote.tagIds.filter { it in knownTagIds }.distinct(),
            )
        }
    }
}

/** One remote row (plus its resolved tag links) ready for a force pull insert. */
data class PulledNestedItem(
    val item: NestedListItemEntity,
    val tagIds: List<String>,
)
