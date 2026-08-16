package com.checkit.domain

import kotlinx.datetime.LocalDate

enum class NestedTextStyle {
    Body,
    Header,
    Subheader
}

enum class NestedColorToken {
    Default,
    Red,
    Orange,
    Yellow,
    Green,
    Blue,
    Purple,
    Pink
}

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
    val textStyle: NestedTextStyle = NestedTextStyle.Body,
    val textColor: NestedColorToken = NestedColorToken.Default,
    val backgroundColor: NestedColorToken = NestedColorToken.Default,
    val doDate: LocalDate? = null,
    val priority: TaskPriority = TaskPriority.None,
    val tags: List<TagItem> = emptyList(),
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
) {
    /** Lazily-built indexes keep repeated editor actions from rescanning the tree. */
    val nodeById: Map<Long, NestedItemNode> by lazy { indexNestedNodes(rootNodes) }
    val itemById: Map<Long, NestedListItem> by lazy { nodeById.mapValues { it.value.item } }
    val flatItems: List<NestedListItem> by lazy { flattenNestedNodes(rootNodes) }
}

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
    val sortedChildren = childrenByParent.mapValues { (_, children) ->
        children.sortedWith(compareBy<NestedListItem> { it.position }.thenBy { it.id })
    }
    val nodesById = HashMap<Long, NestedItemNode>(items.size)
    val roots = sortedChildren[null].orEmpty()
    val stack = ArrayDeque<Pair<NestedListItem, Boolean>>()
    roots.asReversed().forEach { stack.addLast(it to false) }
    while (stack.isNotEmpty()) {
        val (item, expanded) = stack.removeLast()
        if (!expanded) {
            stack.addLast(item to true)
            sortedChildren[item.id].orEmpty().asReversed().forEach { child ->
                stack.addLast(child to false)
            }
        } else {
            nodesById[item.id] = NestedItemNode(
                item = item,
                children = sortedChildren[item.id].orEmpty().mapNotNull { child -> nodesById[child.id] }
            )
        }
    }
    return roots.mapNotNull { root -> nodesById[root.id] }
}

private fun indexNestedNodes(roots: List<NestedItemNode>): Map<Long, NestedItemNode> {
    val result = HashMap<Long, NestedItemNode>()
    val stack = ArrayDeque<NestedItemNode>()
    roots.asReversed().forEach(stack::addLast)
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        result[node.item.id] = node
        node.children.asReversed().forEach(stack::addLast)
    }
    return result
}

private fun flattenNestedNodes(roots: List<NestedItemNode>): List<NestedListItem> {
    val result = ArrayList<NestedListItem>()
    val stack = ArrayDeque<NestedItemNode>()
    roots.asReversed().forEach(stack::addLast)
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        result += node.item
        node.children.asReversed().forEach(stack::addLast)
    }
    return result
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

    val childrenByParent = items.groupBy { it.parentId }
    fun descendantsOf(id: Long): Set<Long> {
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
