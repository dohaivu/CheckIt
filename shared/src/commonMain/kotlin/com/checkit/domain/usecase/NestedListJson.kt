package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.MetricItem
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.TaskPriority
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NestedDocumentExport(
    val version: Int = CURRENT_VERSION,
    val exportedAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val document: NestedDocumentExportData,
    val items: List<NestedItemExportData> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 1
        private val jsonFormat = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromJson(json: String): NestedDocumentExport =
            jsonFormat.decodeFromString(serializer(), json)
    }

    fun toJson(): String = jsonFormat.encodeToString(serializer(), this)
}

@Serializable
data class NestedDocumentExportData(
    val id: String = "",
    val title: String,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

@Serializable
data class NestedItemExportData(
    val id: String = "",
    val parentId: String? = null,
    val position: Int = 0,
    val text: String,
    val note: String? = null,
    val label: String? = null,
    val checkboxEnabled: Boolean = false,
    val checked: Boolean = false,
    val completedAtMillis: Long? = null,
    val collapsed: Boolean = false,
    val textStyle: String = "Body",
    val textColor: String = "Default",
    val backgroundColor: String = "Default",
    val borderShape: String = "Rounded",
    val borderStyle: String = "None",
    val borderColor: String = "Default",
    val startDate: String? = null,
    val endDate: String? = null,
    val priority: String = "None",
    val tags: List<String> = emptyList(),
    val actualMinutes: Int = 0,
    val metricRollupPolicy: String = "IncludeChildren",
    val showTrackedMinutes: Boolean = false,
    val progressPercent: Int? = null,
    val manualMetrics: List<MetricItem> = emptyList(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val children: List<NestedItemExportData> = emptyList()
)

fun NestedDocumentTree.toExport(): NestedDocumentExport =
    NestedDocumentExport(
        version = NestedDocumentExport.CURRENT_VERSION,
        exportedAtMillis = Clock.System.now().toEpochMilliseconds(),
        document = NestedDocumentExportData(
            id = document.id,
            title = document.title,
            createdAtMillis = document.createdAtMillis,
            updatedAtMillis = document.updatedAtMillis
        ),
        items = rootNodes.map { it.toExportData() }
    )

fun NestedItemNode.toExportData(): NestedItemExportData =
    NestedItemExportData(
        id = item.id,
        parentId = item.parentId,
        position = item.position,
        text = item.text,
        note = item.note,
        label = item.label,
        checkboxEnabled = item.checkboxEnabled,
        checked = item.checked,
        completedAtMillis = item.completedAtMillis,
        collapsed = item.collapsed,
        textStyle = item.textStyle.name,
        textColor = item.textColor.name,
        backgroundColor = item.backgroundColor.name,
        borderShape = item.borderShape.name,
        borderStyle = item.borderStyle.name,
        borderColor = item.borderColor.name,
        startDate = item.startDate?.toString(),
        endDate = item.endDate?.toString(),
        priority = item.priority.name,
        tags = item.tags.map { it.name },
        actualMinutes = item.actualMinutes,
        metricRollupPolicy = item.metricRollupPolicy.name,
        showTrackedMinutes = item.showTrackedMinutes,
        progressPercent = item.progressPercent,
        manualMetrics = item.manualMetrics,
        createdAtMillis = item.createdAtMillis,
        updatedAtMillis = item.updatedAtMillis,
        children = children.map { it.toExportData() }
    )

/**
 * Normalizes an item list that may be either hierarchical (with nested children)
 * or flat (with parentId references), into a flat list with valid parentIds and positions.
 */
fun flattenExportItems(items: List<NestedItemExportData>): List<NestedItemExportData> {
    val hasChildren = items.any { it.children.isNotEmpty() }
    if (!hasChildren) {
        return normalizeFlatPositions(items)
    }

    val flatList = mutableListOf<NestedItemExportData>()
    fun traverse(node: NestedItemExportData, parentId: String?, position: Int) {
        val resolvedId = node.id.ifBlank { Uuid.random().toString() }
        flatList.add(
            node.copy(
                id = resolvedId,
                parentId = parentId,
                position = position,
                children = emptyList()
            )
        )
        node.children.forEachIndexed { childIndex, child ->
            traverse(child, resolvedId, childIndex)
        }
    }

    items.forEachIndexed { index, root ->
        traverse(root, null, index)
    }
    return flatList
}

private fun normalizeFlatPositions(items: List<NestedItemExportData>): List<NestedItemExportData> {
    val grouped = items.groupBy { it.parentId }
    val result = mutableListOf<NestedItemExportData>()
    for ((_, group) in grouped) {
        val sorted = group.sortedBy { it.position }
        sorted.forEachIndexed { idx, item ->
            result.add(
                item.copy(
                    id = item.id.ifBlank { Uuid.random().toString() },
                    position = idx,
                    children = emptyList()
                )
            )
        }
    }
    return result
}

/**
 * Computes a unique document title if a document with the same title already exists.
 * E.g., "Work" -> "Work (1)" -> "Work (2)".
 */
fun resolveUniqueTitle(candidate: String, existingTitles: Set<String>): String {
    val trimmed = candidate.trim().ifBlank { "Untitled Document" }
    val lowerExisting = existingTitles.map { it.lowercase().trim() }.toSet()
    if (trimmed.lowercase() !in lowerExisting) return trimmed

    val regex = Regex("^(.*?)(?:\\s*\\((\\d+)\\))?$")
    val match = regex.matchEntire(trimmed)
    val base = match?.groupValues?.get(1)?.trim().takeIf { !it.isNullOrBlank() } ?: trimmed

    var counter = 1
    val existingNum = match?.groupValues?.getOrNull(2)?.toIntOrNull()
    if (existingNum != null) {
        counter = existingNum + 1
    }

    while (true) {
        val candidateTitle = "$base ($counter)"
        if (candidateTitle.lowercase() !in lowerExisting) {
            return candidateTitle
        }
        counter++
    }
}

/**
 * Re-maps all item IDs and parentIds to fresh UUIDs when reassignIds is true,
 * ensuring no primary key or foreign key collisions in SQLite.
 */
fun remapItemsForImport(
    items: List<NestedItemExportData>,
    targetDocumentId: String,
    reassignIds: Boolean
): List<NestedItemExportData> {
    if (!reassignIds) {
        return items
    }
    val idMap = mutableMapOf<String, String>()
    items.forEach { item ->
        val oldId = item.id.ifBlank { Uuid.random().toString() }
        idMap[oldId] = Uuid.random().toString()
    }
    return items.map { item ->
        val oldId = item.id.ifBlank { Uuid.random().toString() }
        val newId = idMap[oldId] ?: Uuid.random().toString()
        val newParentId = item.parentId?.let { idMap[it] }
        item.copy(
            id = newId,
            parentId = newParentId
        )
    }
}

class ExportNestedDocumentJsonUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(documentId: String): String =
        repository.exportNestedDocumentJson(documentId)
}

class ImportNestedDocumentJsonUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(json: String): String =
        repository.importNestedDocumentJson(json)
}
