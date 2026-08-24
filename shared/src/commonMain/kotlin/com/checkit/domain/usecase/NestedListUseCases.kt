package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemMove
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedItemNode
import com.checkit.domain.TagItem
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.NestedColorToken
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.NestedManualMetric
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
    operator fun invoke(documentId: Long): Flow<NestedDocumentTree> =
        repository.observeNestedDocumentTree(documentId)
}

class AddNestedDocumentUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(title: String): Long {
        val trimmed = title.trim()
        require(trimmed.isNotBlank()) { "Document title must not be blank" }
        return repository.addNestedDocument(trimmed)
    }
}

class RenameNestedDocumentUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(documentId: Long, title: String) {
        val trimmed = title.trim()
        require(trimmed.isNotBlank()) { "Document title must not be blank" }
        repository.renameNestedDocument(documentId, trimmed)
    }
}

class DeleteNestedDocumentUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(documentId: Long) = repository.deleteNestedDocument(documentId)
}

class AddNestedItemUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(documentId: Long, parentId: Long?, text: String, position: Int? = null): Long {
        val trimmed = text.trim()
        require(trimmed.isNotBlank()) { "Item text must not be blank" }
        return repository.addNestedItem(documentId, parentId, trimmed, position)
    }
}

class UpdateNestedItemTextUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, text: String) {
        val trimmed = text.trim()
        require(trimmed.isNotBlank()) { "Item text must not be blank" }
        repository.updateNestedItemText(itemId, trimmed)
    }
}

class UpdateNestedItemNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, note: String?) =
        repository.updateNestedItemNote(itemId, note)
}

class UpdateNestedItemFormattingUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        itemId: Long,
        textStyle: NestedTextStyle,
        textColor: NestedColorToken,
        backgroundColor: NestedColorToken
    ) = repository.updateNestedItemFormatting(itemId, textStyle, textColor, backgroundColor)
}

class UpdateNestedItemPriorityUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, priority: TaskPriority) =
        repository.updateNestedItemPriority(itemId, priority)
}

class UpdateNestedItemDateRangeUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, startDate: LocalDate?, endDate: LocalDate?) =
        repository.updateNestedItemDateRange(itemId, startDate, endDate)
}

class UpdateNestedItemTagsUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, tagIds: List<Long>) =
        repository.updateNestedItemTags(itemId, tagIds)
}

class UpdateNestedItemMetricSettingsUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        itemId: Long,
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
    suspend operator fun invoke(itemId: Long, metrics: List<NestedManualMetric>) =
        repository.replaceNestedManualMetrics(itemId, metrics)
}

class SetNestedItemCheckboxEnabledUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, checkboxEnabled: Boolean) =
        repository.setNestedItemCheckboxEnabled(itemId, checkboxEnabled)
}

class SetNestedItemsCheckedUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemIds: List<Long>, checked: Boolean) =
        repository.setNestedItemsChecked(itemIds, checked)
}

class ToggleNestedItemCollapsedUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long) = repository.toggleNestedItemCollapsed(itemId)
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
    fun indent(items: List<NestedListItem>, itemId: Long): List<NestedItemMove> {
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
    fun outdent(items: List<NestedListItem>, itemId: Long): List<NestedItemMove> {
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
    fun moveUp(items: List<NestedListItem>, itemId: Long): List<NestedItemMove> {
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
    fun moveDown(items: List<NestedListItem>, itemId: Long): List<NestedItemMove> {
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
     * Places [itemId] as child of [newParentId] at [newIndex]. The index refers
     * to the target group *excluding* the dragged item (so same-parent reorders
     * behave like gap-based drops). Returns moves that renormalize both affected
     * groups; empty if the item does not exist or the drop targets its own subtree.
     */
    fun moveToPosition(
        items: List<NestedListItem>,
        itemId: Long,
        newParentId: Long?,
        newIndex: Int
    ): List<NestedItemMove> {
        val item = items.firstOrNull { it.id == itemId } ?: return emptyList()
        var cursor: Long? = newParentId
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
    private fun renormalizeGroup(ordered: List<NestedListItem>, parentId: Long?): List<NestedItemMove> =
        ordered.mapIndexedNotNull { index, item ->
            if (item.parentId != parentId || item.position != index) {
                NestedItemMove(item.id, parentId, index)
            } else null
        }

    private fun siblingsOf(items: List<NestedListItem>, parentId: Long?): List<NestedListItem> =
        items.filter { it.parentId == parentId }
            .sortedWith(compareBy<NestedListItem> { it.position }.thenBy { it.id })
}

class DeleteNestedItemsUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemIds: List<Long>) = repository.deleteNestedItems(itemIds)
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
