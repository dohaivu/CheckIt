package com.checkit.domain

/**
 * Rolls child metric summaries up into each parent.
 *
 * [NestedMetricSummary.doneTaskCount] counts directly checked children and
 * [NestedMetricSummary.totalTaskCount] counts direct children with a checkbox,
 * both without descending (not descendants), while tracked minutes follow each
 * item's [MetricRollupPolicy]. Iterative: nested lists have no depth limit, so
 * recursion here could overflow on a pathological document.
 */
fun calculateNestedMetricSummaries(roots: List<NestedItemNode>): Map<String, NestedMetricSummary> {
    val summaries = HashMap<String, NestedMetricSummary>()
    val stack = ArrayDeque<Pair<NestedItemNode, Boolean>>()
    roots.asReversed().forEach { stack.addLast(it to false) }
    while (stack.isNotEmpty()) {
        val (node, visited) = stack.removeLast()
        if (!visited) {
            stack.addLast(node to true)
            node.children.asReversed().forEach { stack.addLast(it to false) }
            continue
        }
        val totalTaskCount = node.children.count { it.item.checkboxEnabled }
        val doneTaskCount = node.children.count { it.item.checkboxEnabled && it.item.checked }
        var trackedMinutes = node.item.actualMinutes.coerceAtLeast(0)
        node.children.forEach { child ->
            // Post-order guarantees the child summary exists; surface a bug
            // instead of silently dropping the rollup.
            val childSummary = summaries.getValue(child.item.id)
            if (node.item.metricRollupPolicy != MetricRollupPolicy.OwnOnly &&
                child.item.metricRollupPolicy != MetricRollupPolicy.ExcludeFromParent
            ) {
                trackedMinutes += childSummary.trackedMinutes
            }
        }
        summaries[node.item.id] = NestedMetricSummary(
            doneTaskCount = doneTaskCount,
            totalTaskCount = totalTaskCount,
            trackedMinutes = trackedMinutes
        )
    }
    return summaries
}

/** Sort orders for a parent's children. Performance is O(g log g) on the sibling group only. */
enum class NestedSortOrder {
    NameAsc,
    AddedDesc,
    CompletedDesc,
    IncompleteFirst,
    PriorityDesc,
}

/**
 * Builds the item tree for a document. Groups by parent, sorts siblings by
 * (position, id), then builds bottom-up. Roots are items whose
 * [NestedListItem.parentId] is null. There is no depth limit.
 *
 * Nothing is ever dropped: items unreachable from the roots (orphaned
 * parent ids, parent cycles from concurrent cross-device moves) are
 * appended as extra roots in (position, id) order, so divergent rows stay
 * visible instead of vanishing. Cycles terminate via the expanded guard;
 * at most one edge of a cycle is omitted.
 */
fun buildNestedTree(items: List<NestedListItem>): List<NestedItemNode> {
    if (items.isEmpty()) return emptyList()
    val childrenByParent = items.groupBy { it.parentId }
    val sortedChildren = childrenByParent.mapValues { (_, children) ->
        children.sortedWith(compareBy<NestedListItem> { it.position }.thenBy { it.id })
    }
    val nodesById = HashMap<String, NestedItemNode>(items.size)
    val expanded = HashSet<String>(items.size)
    val roots = sortedChildren[null].orEmpty()
    val extraRoots = mutableListOf<NestedListItem>()
    val stack = ArrayDeque<Pair<NestedListItem, Boolean>>()
    fun seed(seeds: List<NestedListItem>) {
        seeds.asReversed().forEach { stack.addLast(it to false) }
    }
    fun drain() {
        while (stack.isNotEmpty()) {
            val (item, expandedMark) = stack.removeLast()
            if (!expandedMark) {
                if (!expanded.add(item.id)) continue
                stack.addLast(item to true)
                sortedChildren[item.id].orEmpty().asReversed().forEach { child ->
                    if (child.id !in expanded) stack.addLast(child to false)
                }
            } else {
                nodesById[item.id] = NestedItemNode(
                    item = item,
                    children = sortedChildren[item.id].orEmpty().mapNotNull { child -> nodesById[child.id] }
                )
            }
        }
    }
    seed(roots)
    drain()
    val rootOrder = compareBy<NestedListItem> { it.position }.thenBy { it.id }
    // Common case: every row is reachable. `roots` is already in sibling order.
    if (nodesById.size == items.size) {
        return roots.mapNotNull { nodesById[it.id] }
    }
    // Unreachable rows (orphaned parents, parent cycles from concurrent
    // cross-device moves) seed as extra roots instead of being dropped, so
    // divergent rows stay visible. One deterministic representative at a
    // time; its subtree builds via normal expansion. The expanded guard
    // guarantees termination and each round builds at least the seed.
    val allIds = items.map { it.id }.toSet()
    while (true) {
        val unbuilt = items.filter { it.id !in nodesById }
        if (unbuilt.isEmpty()) break
        // Topmost first (parent missing or already built) so descendants
        // attach via expansion instead of duplicating as roots; pure
        // cycles fall back to the deterministic minimum.
        val next = unbuilt
            .filter { it.parentId == null || it.parentId !in allIds || it.parentId in nodesById }
            .minWithOrNull(rootOrder)
            ?: unbuilt.minWithOrNull(rootOrder)!!
        extraRoots.add(next)
        seed(listOf(next))
        drain()
    }
    return (roots + extraRoots)
        .sortedWith(rootOrder)
        .mapNotNull { root -> nodesById[root.id] }
}

/** Node and item lookups built in one traversal of the read model. */
internal class NestedNodeIndex(
    val nodes: Map<String, NestedItemNode>,
    val items: Map<String, NestedListItem>
)

/**
 * Visits every node pre-order (parent before children, siblings in display
 * order) without recursion, so unlimited-depth documents stay safe.
 */
internal inline fun forEachNestedNode(
    roots: List<NestedItemNode>,
    action: (NestedItemNode) -> Unit
) {
    val stack = ArrayDeque<NestedItemNode>()
    roots.asReversed().forEach(stack::addLast)
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        action(node)
        node.children.asReversed().forEach(stack::addLast)
    }
}

internal fun indexNestedNodes(roots: List<NestedItemNode>): NestedNodeIndex {
    val nodes = HashMap<String, NestedItemNode>()
    val items = HashMap<String, NestedListItem>()
    forEachNestedNode(roots) { node ->
        nodes[node.item.id] = node
        items[node.item.id] = node.item
    }
    return NestedNodeIndex(nodes, items)
}

internal fun flattenNestedNodes(roots: List<NestedItemNode>): List<NestedListItem> =
    buildList {
        forEachNestedNode(roots) { node -> add(node.item) }
    }
