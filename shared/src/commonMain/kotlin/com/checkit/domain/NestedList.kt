package com.checkit.domain

/**
 * A single nested-lists document. Holds one unlimited-depth item tree.
 */
data class NestedDocument(
    val id: Long = 0L,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

/**
 * One node of a [NestedDocument] tree. Uniform line; checkbox style is per-item.
 * [parentId] null means the item sits at the document root level.
 */
data class NestedListItem(
    val id: Long = 0L,
    val documentId: Long,
    val parentId: Long? = null,
    val position: Int,
    val text: String,
    val note: String? = null,
    val checkboxEnabled: Boolean = false,
    val checked: Boolean = false,
    val collapsed: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

/** UI/read model for one item node with its nested children. */
data class NestedItemNode(
    val item: NestedListItem,
    val children: List<NestedItemNode> = emptyList()
) {
    val hasChildren: Boolean get() = children.isNotEmpty()
}

/** Root of the read model for one document: the document plus its item tree. */
data class NestedDocumentTree(
    val document: NestedDocument,
    val rootNodes: List<NestedItemNode>
)

/**
 * A single re-parent/reorder instruction produced by [planNestedMoves].
 * Sibling gaps in [position] are acceptable; relative order is what matters.
 */
data class NestedItemMove(
    val itemId: Long,
    val parentId: Long?,
    val position: Int
)

/**
 * Builds the item tree for a document. Groups by parent, sorts siblings by
 * (position, id), recurses. Roots are items whose [NestedListItem.parentId] is
 * null. There is no depth limit.
 */
fun buildNestedTree(items: List<NestedListItem>): List<NestedItemNode> {
    if (items.isEmpty()) return emptyList()
    val childrenByParent = items.groupBy { it.parentId }
    fun nodeFor(item: NestedListItem): NestedItemNode {
        val children = childrenByParent[item.id].orEmpty()
            .sortedWith(compareBy<NestedListItem> { it.position }.thenBy { it.id })
            .map { nodeFor(it) }
        return NestedItemNode(item = item, children = children)
    }
    return childrenByParent[null].orEmpty()
        .sortedWith(compareBy<NestedListItem> { it.position }.thenBy { it.id })
        .map { nodeFor(it) }
}

/**
 * Computes the re-parent/reorder moves that apply [itemIds] under
 * [targetParentId] at [targetIndex].
 *
 * - Guard: moving an item under itself or one of its own descendants throws
 *   [IllegalArgumentException].
 * - Subtree-aware: if a selected parent is moved, its descendants are excluded
 *   from [itemIds] (they travel with it). The selected items are then inserted
 *   in their current sibling order at [targetIndex].
 */
fun planNestedMoves(
    items: List<NestedListItem>,
    itemIds: Set<Long>,
    targetParentId: Long?,
    targetIndex: Int
): List<NestedItemMove> {
    if (itemIds.isEmpty()) return emptyList()

    val itemsById = items.associateBy { it.id }
    val selected = itemIds.mapNotNull { itemsById[it] }

    fun descendantsOf(id: Long): Set<Long> {
        val childrenByParent = items.groupBy { it.parentId }
        val result = mutableSetOf<Long>()
        val queue = ArrayDeque<Long>()
        queue.add(id)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            childrenByParent[current].orEmpty().forEach { child ->
                if (result.add(child.id)) queue.add(child.id)
            }
        }
        return result
    }

    val selectedDescendants = selected.flatMap { descendantsOf(it.id) }.toSet()
    val effectiveIds = itemIds - selectedDescendants
    val effective = effectiveIds.mapNotNull { itemsById[it] }
        .sortedWith(compareBy<NestedListItem> { it.position }.thenBy { it.id })

    if (targetParentId != null) {
        if (targetParentId in effectiveIds) {
            throw IllegalArgumentException("Cannot move an item into itself")
        }
        if (targetParentId in selectedDescendants) {
            throw IllegalArgumentException("Cannot move an item into its own subtree")
        }
    }

    return effective.mapIndexed { index, item ->
        NestedItemMove(
            itemId = item.id,
            parentId = targetParentId,
            position = targetIndex + index
        )
    }
}

/**
 * Returns the ids of [item]'s descendants (not including [item] itself).
 */
fun nestedDescendantIds(items: List<NestedListItem>, itemId: Long): Set<Long> {
    val childrenByParent = items.groupBy { it.parentId }
    val result = mutableSetOf<Long>()
    val queue = ArrayDeque<Long>()
    queue.add(itemId)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        childrenByParent[current].orEmpty().forEach { child ->
            if (result.add(child.id)) queue.add(child.id)
        }
    }
    return result
}