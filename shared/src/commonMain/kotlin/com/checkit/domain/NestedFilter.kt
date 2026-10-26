package com.checkit.domain

import kotlinx.datetime.LocalDate

/**
 * Filtered outline plus the context metadata views use for quiet rows:
 *
 * - [roots]: the pruned tree, same shape as [filterNestedTree] returns.
 * - [contextIds]: items kept only to hold matches under their ancestors.
 *   They are scaffolding, not work, and views render them muted. Empty when
 *   no positive criterion is active.
 */
class NestedOutlineProjection(
    val roots: List<NestedItemNode>,
    val contextIds: List<String>
)

/**
 * Prunes the tree, keeping only nodes that overlap with [start] to [end]
 * (inclusive) OR have descendants that do. Supports text [query] and [hideChecked] status.
 *
 * Thin wrapper over [filterNestedTreeWithContext] for callers that only need
 * the pruned roots.
 *
 * With [workingOnly], the finder-filtered regions are refined to working
 * items (see [isWorkingItem]) in a second pass — same ancestor/descendant
 * keep semantics as the other positive filters. Two passes (instead of one
 * AND pass) so a query-matching parent with working children, or vice
 * versa, isn't pruned to nothing. Working implies hide-checked: done work
 * is noise in a needs-attention view (All stays for review).
 */
fun filterNestedTree(
    roots: List<NestedItemNode>,
    start: LocalDate? = null,
    end: LocalDate? = null,
    query: String = "",
    hideChecked: Boolean = false,
    selectedTagIds: Set<String> = emptySet(),
    workingOnly: Boolean = false
): List<NestedItemNode> =
    filterNestedTreeWithContext(roots, start, end, query, hideChecked, selectedTagIds, workingOnly).roots

/**
 * [filterNestedTree] plus the ancestor-context metadata described on
 * [NestedOutlineProjection].
 *
 * @param includeNotesInQuery when true (default), [query] matches both item
 *   text and notes; set false to match item titles only.
 *
 * Iterative on purpose: nested lists have no depth limit, and this walk runs
 * on the UI filtering path. It also avoids copying the tree at all when no
 * criterion is active, so opening the filter bar stays free.
 */
fun filterNestedTreeWithContext(
    roots: List<NestedItemNode>,
    start: LocalDate? = null,
    end: LocalDate? = null,
    query: String = "",
    hideChecked: Boolean = false,
    selectedTagIds: Set<String> = emptySet(),
    workingOnly: Boolean = false,
    includeNotesInQuery: Boolean = true
): NestedOutlineProjection {
    val hasPositiveCriteria = (start != null && end != null) ||
        query.isNotBlank() ||
        selectedTagIds.isNotEmpty()
    val filtered: List<FilteredNestedNode> = when {
        // Working alone: the first pass would copy the whole tree unchanged,
        // and Working already implies hide-checked.
        workingOnly && !hasPositiveCriteria ->
            filterNestedTreePass(
                roots, start = null, end = null, query = "", hideChecked = true,
                selectedTagIds = emptySet(), workingOnly = true,
                includeNotesInQuery = includeNotesInQuery
            )
        // Nothing narrows the tree: preserve identity so an idle filter bar
        // doesn't deep-copy the outline or invalidate the caller's cache.
        !workingOnly && !hasPositiveCriteria && !hideChecked ->
            return NestedOutlineProjection(roots, emptyList())
        else -> {
            val firstPass = filterNestedTreePass(
                roots, start, end, query, hideChecked, selectedTagIds, workingOnly = false,
                includeNotesInQuery = includeNotesInQuery
            )
            if (workingOnly) {
                filterNestedTreePass(
                    roots = firstPass.map { it.node },
                    start = null,
                    end = null,
                    query = "",
                    hideChecked = true,
                    selectedTagIds = emptySet(),
                    workingOnly = true,
                    includeNotesInQuery = includeNotesInQuery
                )
            } else {
                firstPass
            }
        }
    }
    return NestedOutlineProjection(
        roots = filtered.map { it.node },
        contextIds = collectContextIds(filtered)
    )
}

/** A "working" item carries actionable state: priority, due date, an enabled
 * date-based metric, an open checkbox, visible progress, or tracked time. */
fun isWorkingItem(item: NestedListItem): Boolean =
    item.priority != TaskPriority.None ||
            item.startDate != null || item.endDate != null ||
            item.manualMetrics.any { it.enabled && (it.unit == MetricUnit.Countdown || it.unit == MetricUnit.DueDate) } ||
            (item.checkboxEnabled && !item.checked) ||
            item.progressPercent != null ||
            item.actualMinutes > 0

/** One kept node with the match bookkeeping the context metadata is built from. */
private class FilteredNestedNode(
    val node: NestedItemNode,
    val children: List<FilteredNestedNode>,
    /** Satisfies the filter directly. */
    val matchesSelf: Boolean,
    /** Kept because an ancestor matched, not for a match of its own. */
    val isForced: Boolean
)

/** Iterative post-order frame for [filterNestedTreePass]. */
private class FilterFrame(
    val node: NestedItemNode,
    val forceKeep: Boolean,
    val matchesSelf: Boolean,
    val shouldForceKeepDescendants: Boolean
) {
    var expanded = false
}

/**
 * One filter pass over [roots]. Keeps matches, everything below a match, and
 * the ancestors needed to reach them; returns the kept roots in sibling order.
 */
private fun filterNestedTreePass(
    roots: List<NestedItemNode>,
    start: LocalDate?,
    end: LocalDate?,
    query: String,
    hideChecked: Boolean,
    selectedTagIds: Set<String>,
    workingOnly: Boolean,
    includeNotesInQuery: Boolean
): List<FilteredNestedNode> {
    if (roots.isEmpty()) return emptyList()
    val results = HashMap<String, FilteredNestedNode>(roots.size * 2)
    val stack = ArrayDeque<FilterFrame>()

    fun push(node: NestedItemNode, forceKeep: Boolean) {
        // Hide Checked prunes the item and its whole subtree.
        if (hideChecked && node.item.checked) return
        val matchesSelf = matchesNestedFilter(
            node.item, start, end, query, selectedTagIds, workingOnly, includeNotesInQuery
        )
        stack.addLast(
            FilterFrame(
                node = node,
                forceKeep = forceKeep,
                matchesSelf = matchesSelf,
                shouldForceKeepDescendants = forceKeep || matchesSelf
            )
        )
    }

    roots.asReversed().forEach { push(it, forceKeep = false) }
    while (stack.isNotEmpty()) {
        val frame = stack.last()
        if (!frame.expanded) {
            frame.expanded = true
            frame.node.children.asReversed().forEach { child ->
                push(child, frame.shouldForceKeepDescendants)
            }
            continue
        }
        stack.removeLast()
        val keptChildren = frame.node.children.mapNotNull { results[it.item.id] }
        // Keep the node if an ancestor matched, it matched itself, or it has
        // a kept descendant (ancestor context).
        if (!frame.shouldForceKeepDescendants && keptChildren.isEmpty()) continue
        results[frame.node.item.id] = FilteredNestedNode(
            node = frame.node.copy(children = keptChildren.map { it.node }),
            children = keptChildren,
            matchesSelf = frame.matchesSelf,
            isForced = frame.forceKeep
        )
    }
    return roots.mapNotNull { results[it.item.id] }
}

/** True when [item] satisfies every active positive criterion. */
private fun matchesNestedFilter(
    item: NestedListItem,
    start: LocalDate?,
    end: LocalDate?,
    query: String,
    selectedTagIds: Set<String>,
    workingOnly: Boolean,
    includeNotesInQuery: Boolean
): Boolean {
    if (start != null && end != null) {
        val itemStart = item.startDate
        val itemEnd = item.endDate
        if (itemStart == null || itemEnd == null || itemStart > end || itemEnd < start) return false
    }
    if (query.isNotBlank()) {
        val titleMatches = item.text.contains(query, ignoreCase = true)
        val noteMatches = includeNotesInQuery &&
            item.note?.contains(query, ignoreCase = true) == true
        if (!titleMatches && !noteMatches) return false
    }
    if (selectedTagIds.isNotEmpty() && item.tags.none { it.id in selectedTagIds }) return false
    if (workingOnly && !isWorkingItem(item)) return false
    return true
}

/** Pre-order so ids read top-down, matching the outline itself. */
private fun collectContextIds(roots: List<FilteredNestedNode>): List<String> {
    val contextIds = ArrayList<String>()
    val stack = ArrayDeque<FilteredNestedNode>()
    roots.asReversed().forEach(stack::addLast)
    while (stack.isNotEmpty()) {
        val result = stack.removeLast()
        // Kept only because a descendant matched: ancestor scaffolding.
        if (!result.matchesSelf && !result.isForced) contextIds.add(result.node.item.id)
        result.children.asReversed().forEach(stack::addLast)
    }
    return contextIds
}
