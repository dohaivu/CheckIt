package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemMove
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedItemNode
import kotlinx.coroutines.flow.Flow

class ObserveNestedDocumentsUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<List<NestedDocument>> =
        repository.observeNestedDocuments()
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
        val newPosition = (items.filter { it.parentId == newParent.id }
            .maxOfOrNull { it.position } ?: -1) + 1
        return listOf(NestedItemMove(item.id, newParent.id, newPosition))
    }

    /** Outdents [itemId] to sit right after its parent. No-op if it has none. */
    fun outdent(items: List<NestedListItem>, itemId: Long): List<NestedItemMove> {
        val item = items.firstOrNull { it.id == itemId } ?: return emptyList()
        val parent = items.firstOrNull { it.id == item.parentId } ?: return emptyList()
        val siblings = siblingsOf(items, parent.parentId)
        val parentIndex = siblings.indexOfFirst { it.id == parent.id }
        if (parentIndex < 0) return emptyList()
        val reordered = siblings.toMutableList()
        reordered.add(parentIndex + 1, item)
        return renormalizeGroup(reordered, parent.parentId)
    }

    /** Moves [itemId] one slot up within its siblings. No-op if already first. */
    fun moveUp(items: List<NestedListItem>, itemId: Long): List<NestedItemMove> {
        val item = items.firstOrNull { it.id == itemId } ?: return emptyList()
        val siblings = siblingsOf(items, item.parentId)
        val index = siblings.indexOfFirst { it.id == itemId }
        if (index <= 0) return emptyList()
        val above = siblings[index - 1]
        return listOf(
            NestedItemMove(item.id, item.parentId, above.position),
            NestedItemMove(above.id, above.parentId, item.position)
        )
    }

    /** Moves [itemId] one slot down within its siblings. No-op if already last. */
    fun moveDown(items: List<NestedListItem>, itemId: Long): List<NestedItemMove> {
        val item = items.firstOrNull { it.id == itemId } ?: return emptyList()
        val siblings = siblingsOf(items, item.parentId)
        val index = siblings.indexOfFirst { it.id == itemId }
        if (index == siblings.lastIndex) return emptyList()
        val below = siblings[index + 1]
        return listOf(
            NestedItemMove(item.id, item.parentId, below.position),
            NestedItemMove(below.id, below.parentId, item.position)
        )
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
    nodes.flatMap { node ->
        listOf(node.item) + flattenNestedItems(node.children)
    }