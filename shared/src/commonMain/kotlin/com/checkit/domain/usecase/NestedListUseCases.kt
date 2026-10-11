package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.MetricItem
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemMove
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedSortOrder
import com.checkit.domain.TagItem
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.NestedColorToken
import com.checkit.domain.MetricRollupPolicy

import com.checkit.domain.TaskPriority
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.Flow

class ObserveNestedDocumentsUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<List<NestedDocument>> =
        repository.observeNestedDocuments()
}

class ObserveNestedTagsUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<List<TagItem>> = repository.observeTags()
}

class ObserveNestedDocumentTreeUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(documentId: String): Flow<NestedDocumentTree> =
        repository.observeNestedDocumentTree(documentId)
}

class AddNestedDocumentUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(title: String): String {
        val trimmed = title.trim()
        require(trimmed.isNotBlank()) { "Document title must not be blank" }
        return repository.addNestedDocument(trimmed)
    }
}

class RenameNestedDocumentUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(documentId: String, title: String) {
        val trimmed = title.trim()
        require(trimmed.isNotBlank()) { "Document title must not be blank" }
        repository.renameNestedDocument(documentId, trimmed)
    }
}

class DeleteNestedDocumentUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(documentId: String) = repository.deleteNestedDocument(documentId)
}

class AddNestedItemUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(documentId: String, parentId: String?, text: String, position: Int? = null): String {
        val trimmed = text.trim()
        require(trimmed.isNotBlank()) { "Item text must not be blank" }
        return repository.addNestedItem(documentId, parentId, trimmed, position)
    }
}

class UpdateNestedItemTextUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: String, text: String) {
        val trimmed = text.trim()
        require(trimmed.isNotBlank()) { "Item text must not be blank" }
        repository.updateNestedItemText(itemId, trimmed)
    }
}

class UpdateNestedItemNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: String, note: String?) =
        repository.updateNestedItemNote(itemId, note)
}

class UpdateNestedItemFormattingUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        itemId: String,
        textStyle: NestedTextStyle,
        textColor: NestedColorToken,
        backgroundColor: NestedColorToken
    ) = repository.updateNestedItemFormatting(itemId, textStyle, textColor, backgroundColor)
}

class UpdateNestedItemPriorityUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: String, priority: TaskPriority) =
        repository.updateNestedItemPriority(itemId, priority)
}

class UpdateNestedItemDateRangeUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: String, startDate: LocalDate?, endDate: LocalDate?) =
        repository.updateNestedItemDateRange(itemId, startDate, endDate)
}

class UpdateNestedItemTagsUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: String, tagIds: List<String>) =
        repository.updateNestedItemTags(itemId, tagIds)
}

class UpdateNestedItemMetricSettingsUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        itemId: String,
        actualMinutes: Int,
        metricRollupPolicy: MetricRollupPolicy,
        showTrackedMinutes: Boolean
    ) = repository.updateNestedItemMetricSettings(
        itemId, actualMinutes, metricRollupPolicy, showTrackedMinutes
    )
}

class ReplaceNestedManualMetricsUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: String, metrics: List<MetricItem>) =
        repository.replaceNestedManualMetrics(itemId, metrics)
}

class UpdateNestedItemProgressUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: String, progressPercent: Int?) =
        repository.updateNestedItemProgress(itemId, progressPercent)
}

class SetNestedItemCheckboxEnabledUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: String, checkboxEnabled: Boolean) =
        repository.setNestedItemCheckboxEnabled(itemId, checkboxEnabled)
}

class SetNestedItemsCheckedUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemIds: List<String>, checked: Boolean) =
        repository.setNestedItemsChecked(itemIds, checked)
}

class ToggleNestedItemCollapsedUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: String) = repository.toggleNestedItemCollapsed(itemId)
}

/**
 * Resolves the scope roots for expand-all / collapse-all: batch-selected ids
 * win, then the single selected item (when it has children), then the zoomed
 * (focused) item, else empty meaning the whole document (current view).
 */
fun resolveCollapseScopeRoots(
    items: List<NestedListItem>,
    selectedIds: Set<String>,
    selectedItemId: String?,
    focusedItemId: String?
): List<String> {
    val byId = items.associateBy { it.id }
    val selectedRoots = selectedIds.mapNotNull { byId[it]?.id }
    if (selectedRoots.isNotEmpty()) return selectedRoots
    val selected = selectedItemId?.let { byId[it] }
    if (selected != null && items.any { it.parentId == selected.id }) return listOf(selected.id)
    val focused = focusedItemId?.let { byId[it] }
    if (focused != null) return listOf(focused.id)
    return emptyList()
}

/**
 * Collects ids of items with children inside the subtrees rooted at
 * [scopeRootIds] (roots included when they have children), keeping only rows
 * whose collapsed state differs from [collapsed]. Empty [scopeRootIds] means
 * the whole document.
 *
 * Collapse-all keeps the scope roots themselves expanded so the selection /
 * zoomed view stays visible; only their descendants collapse.
 */
fun collapsibleIdsInScopes(
    items: List<NestedListItem>,
    scopeRootIds: List<String>,
    collapsed: Boolean
): List<String> {
    if (items.isEmpty()) return emptyList()
    val childrenByParent = items.groupBy { it.parentId }
    if (scopeRootIds.isEmpty()) {
        return items.filter { item ->
            item.collapsed != collapsed && childrenByParent[item.id]?.isNotEmpty() == true
        }.map { it.id }
    }
    val byId = items.associateBy { it.id }
    val roots = scopeRootIds.mapNotNull { byId[it]?.id }.toSet()
    val out = LinkedHashSet<String>()
    val stack = ArrayDeque<String>()
    roots.forEach(stack::addLast)
    val visited = HashSet<String>()
    while (stack.isNotEmpty()) {
        val id = stack.removeLast()
        if (!visited.add(id)) continue
        val item = byId[id] ?: continue
        val children = childrenByParent[id].orEmpty()
        if (children.isNotEmpty()) {
            if (item.collapsed != collapsed && !(collapsed && id in roots)) out.add(id)
            children.forEach { stack.addLast(it.id) }
        }
    }
    return out.toList()
}

class SetNestedItemsCollapsedUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        items: List<NestedListItem>,
        scopeRootIds: List<String>,
        collapsed: Boolean
    ) {
        val ids = collapsibleIdsInScopes(items, scopeRootIds, collapsed)
        if (ids.isEmpty()) return
        repository.setNestedItemsCollapsed(ids, collapsed)
    }
}

class MoveNestedItemsUseCase(
    private val repository: CheckItRepository
) {
    /**
     * Applies absolute placements (parentId + position) produced by the move
     * helpers below. Works off the caller-supplied item list so indentation
     * and reorder stay pure and testable.
     */
    suspend operator fun invoke(moves: List<NestedItemMove>) =
        repository.moveNestedItems(moves)

    /** Indents [itemId] under its previous sibling. No-op if first in group. */
    fun indent(items: List<NestedListItem>, itemId: String): List<NestedItemMove> {
        val item = items.firstOrNull { it.id == itemId } ?: return emptyList()
        val siblings = siblingsOf(items, item.parentId)
        val index = siblings.indexOfFirst { it.id == itemId }
        if (index <= 0) return emptyList()
        val newParent = siblings[index - 1]
        val targetSiblings = siblingsOf(items, newParent.id) + item
        // Keep both groups contiguous. This matters after repeated indent/outdent
        // operations and makes subsequent moves deterministic.
        val sourceMoves = renormalizeGroup(siblings.filterNot { it.id == item.id }, item.parentId)
        return sourceMoves + renormalizeGroup(targetSiblings, newParent.id)
    }

    /** Outdents [itemId] to sit right after its parent. No-op if it has none. */
    fun outdent(items: List<NestedListItem>, itemId: String): List<NestedItemMove> {
        val item = items.firstOrNull { it.id == itemId } ?: return emptyList()
        val parent = items.firstOrNull { it.id == item.parentId } ?: return emptyList()
        val siblings = siblingsOf(items, parent.parentId)
        val parentIndex = siblings.indexOfFirst { it.id == parent.id }
        if (parentIndex < 0) return emptyList()
        val sourceSiblings = siblingsOf(items, item.parentId).filterNot { it.id == item.id }
        val targetSiblings = siblings.toMutableList().apply { add(parentIndex + 1, item) }
        return renormalizeGroup(sourceSiblings, item.parentId) +
            renormalizeGroup(targetSiblings, parent.parentId)
    }

    /** Moves [itemId] one slot up within its siblings. No-op if already first. */
    fun moveUp(items: List<NestedListItem>, itemId: String): List<NestedItemMove> {
        val item = items.firstOrNull { it.id == itemId } ?: return emptyList()
        val siblings = siblingsOf(items, item.parentId)
        val index = siblings.indexOfFirst { it.id == itemId }
        if (index <= 0) return emptyList()
        val reordered = siblings.toMutableList().apply {
            add(index - 1, removeAt(index))
        }
        return renormalizeGroup(reordered, item.parentId)
    }

    /** Moves [itemId] one slot down within its siblings. No-op if already last. */
    fun moveDown(items: List<NestedListItem>, itemId: String): List<NestedItemMove> {
        val item = items.firstOrNull { it.id == itemId } ?: return emptyList()
        val siblings = siblingsOf(items, item.parentId)
        val index = siblings.indexOfFirst { it.id == itemId }
        if (index < 0 || index >= siblings.lastIndex) return emptyList()
        val reordered = siblings.toMutableList().apply {
            add(index + 1, removeAt(index))
        }
        return renormalizeGroup(reordered, item.parentId)
    }

    /**
     * Sorts the children of [parentId] by [order] and renormalizes the group
     * to contiguous 0-based positions. Empty when the parent is unknown,
     * childless, or already sorted. Comparators always end with
     * (position, id) so ties keep a stable, deterministic order.
     */
    fun sortChildren(
        items: List<NestedListItem>,
        parentId: String?,
        order: NestedSortOrder
    ): List<NestedItemMove> {
        val siblings = siblingsOf(items, parentId)
        if (siblings.size < 2) return emptyList()
        val comparator = when (order) {
            NestedSortOrder.NameAsc -> compareBy<NestedListItem> { it.text.lowercase() }
            NestedSortOrder.AddedDesc -> compareByDescending<NestedListItem> { it.createdAtMillis }
            NestedSortOrder.CompletedDesc -> compareByDescending<NestedListItem> { it.completedAtMillis ?: Long.MIN_VALUE }
            NestedSortOrder.IncompleteFirst -> compareBy<NestedListItem> { it.checked }
            NestedSortOrder.PriorityDesc -> compareBy<NestedListItem> { it.priority.sortRank() }
        }.thenBy { it.position }.thenBy { it.id }
        return renormalizeGroup(siblings.sortedWith(comparator), parentId)
    }

    /**
     * Moves [itemId] to become the last child of [destinationId]. Empty when
     * either is unknown, the destination sits in the item's own subtree, or
     * the item is already its last child.
     */
    fun moveUnder(
        items: List<NestedListItem>,
        itemId: String,
        destinationId: String
    ): List<NestedItemMove> {
        if (items.none { it.id == destinationId }) return emptyList()
        return moveToPosition(items, itemId, destinationId, siblingsOf(items, destinationId).size)
    }

    /**
     * Places [itemId] as child of [newParentId] at [newIndex]. The index refers
     * to the target group *excluding* the dragged item (so same-parent reorders
     * behave like gap-based drops). Returns moves that renormalize both affected
     * groups; empty if the item does not exist or the drop targets its own subtree.
     */
    fun moveToPosition(
        items: List<NestedListItem>,
        itemId: String,
        newParentId: String?,
        newIndex: Int
    ): List<NestedItemMove> {
        val item = items.firstOrNull { it.id == itemId } ?: return emptyList()
        var cursor: String? = newParentId
        while (cursor != null) {
            if (cursor == itemId) return emptyList()
            cursor = items.firstOrNull { it.id == cursor }?.parentId
        }
        val sourceSiblings = siblingsOf(items, item.parentId).filterNot { it.id == itemId }
        return if (newParentId == item.parentId) {
            val reordered = sourceSiblings.toMutableList().apply {
                add(newIndex.coerceIn(0, size), item)
            }
            renormalizeGroup(reordered, item.parentId)
        } else {
            val targetSiblings = siblingsOf(items, newParentId).toMutableList().apply {
                add(newIndex.coerceIn(0, size), item)
            }
            renormalizeGroup(sourceSiblings, item.parentId) +
                renormalizeGroup(targetSiblings, newParentId)
        }
    }

    /**
     * Emits moves that pin [ordered] to contiguous 0-based positions under
     * [parentId], only for items whose parent or position actually changes.
     */
    private fun renormalizeGroup(ordered: List<NestedListItem>, parentId: String?): List<NestedItemMove> =
        ordered.mapIndexedNotNull { index, item ->
            if (item.parentId != parentId || item.position != index) {
                NestedItemMove(item.id, parentId, index)
            } else null
        }

    private fun siblingsOf(items: List<NestedListItem>, parentId: String?): List<NestedListItem> =
        items.filter { it.parentId == parentId }
            .sortedWith(compareBy<NestedListItem> { it.position }.thenBy { it.id })

    /** Rank for PriorityDesc: High first, None last (matches task sort convention). */
    private fun TaskPriority.sortRank(): Int = when (this) {
        TaskPriority.High -> 0
        TaskPriority.Medium -> 1
        TaskPriority.Low -> 2
        TaskPriority.None -> 3
    }
}

/** Deletes items and all of their descendants (cascade via [CheckItDao.deleteNestedItems]). */
class DeleteNestedItemsUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemIds: List<String>) = repository.deleteNestedItems(itemIds)
}

/** Flattens a node forest back into a list of items (depth-first, pre-order). */
fun flattenNestedItems(nodes: List<NestedItemNode>): List<NestedListItem> =
    buildList {
        val stack = ArrayDeque<NestedItemNode>()
        nodes.asReversed().forEach(stack::addLast)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            add(node.item)
            node.children.asReversed().forEach(stack::addLast)
        }
    }
