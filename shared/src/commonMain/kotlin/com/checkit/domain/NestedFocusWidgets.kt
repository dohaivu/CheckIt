package com.checkit.domain

/**
 * Promotes topmost actionable high-priority items to roots for glanceable
 * widgets: unchecked P1 items with no matched ancestor. Descendants are kept
 * (any priority) down to [maxDepth] levels below each promoted root;
 * checked subtrees are pruned entirely and collapse is ignored. Pure.
 */
fun focusSubtrees(roots: List<NestedItemNode>, maxDepth: Int = 2): List<NestedItemNode> {
    val out = mutableListOf<NestedItemNode>()
    fun collect(node: NestedItemNode, ancestorMatched: Boolean, depthBelow: Int): NestedItemNode? {
        if (node.item.checked) return null
        val matches = !ancestorMatched && node.item.priority == TaskPriority.High
        if (!ancestorMatched && !matches) {
            node.children.forEach { collect(it, ancestorMatched = false, depthBelow = 0)?.let(out::add) }
            return null
        }
        if (ancestorMatched && depthBelow > maxDepth) return null
        val nextDepth = if (matches) 1 else depthBelow + 1
        val keptChildren = node.children.mapNotNull { collect(it, ancestorMatched = true, depthBelow = nextDepth) }
        return node.copy(children = keptChildren)
    }
    roots.forEach { collect(it, ancestorMatched = false, depthBelow = 0)?.let(out::add) }
    return out
}

/** One flattened focus row: the item, its depth below the promoted root, and whether it has children. */
data class FocusRow(
    val item: NestedListItem,
    val depth: Int,
    val hasChildren: Boolean
)

/** Flattens kept subtrees to rows with depth relative to each root. */
fun flattenFocusRows(roots: List<NestedItemNode>): List<FocusRow> =
    buildList {
        val stack = ArrayDeque<Pair<NestedItemNode, Int>>()
        roots.asReversed().forEach { stack.addLast(it to 0) }
        while (stack.isNotEmpty()) {
            val (node, depth) = stack.removeLast()
            add(FocusRow(node.item, depth, node.children.isNotEmpty()))
            node.children.asReversed().forEach { stack.addLast(it to depth + 1) }
        }
    }
