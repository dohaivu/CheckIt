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
 *
 * @param today when set, [workingOnly] also drops overdue items (an end date
 *   before [today]); overdue work belongs to the Overdue criterion instead.
 */
fun filterNestedTree(
    roots: List<NestedItemNode>,
    start: LocalDate? = null,
    end: LocalDate? = null,
    query: String = "",
    hideChecked: Boolean = false,
    selectedTagIds: Set<String> = emptySet(),
    workingOnly: Boolean = false,
    overdueBefore: LocalDate? = null,
    today: LocalDate? = null
): List<NestedItemNode> =
    filterNestedTreeWithContext(
        roots = roots,
        start = start,
        end = end,
        query = query,
        hideChecked = hideChecked,
        selectedTagIds = selectedTagIds,
        workingOnly = workingOnly,
        overdueBefore = overdueBefore,
        today = today
    ).roots

/**
 * [filterNestedTree] plus the ancestor-context metadata described on
 * [NestedOutlineProjection].
 *
 * @param includeNotesInQuery when true, [query] also matches notes; defaults
 *   to false so search narrows by item titles only.
 * @param overdueBefore when set, keeps items whose inclusive end date is
 *   strictly before it. Used by [NestedRollingPreset.Overdue], which cannot be
 *   expressed as an overlapping date window.
 * @param today when set, [workingOnly] also excludes overdue items, so the
 *   Working view only shows work that is still current.
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
    includeNotesInQuery: Boolean = false,
    overdueBefore: LocalDate? = null,
    today: LocalDate? = null
): NestedOutlineProjection {
    val hasPositiveCriteria = (start != null && end != null) ||
        query.isNotBlank() ||
        selectedTagIds.isNotEmpty() ||
        overdueBefore != null
    val filtered: List<FilteredNestedNode> = when {
        // Working alone: the first pass would copy the whole tree unchanged,
        // and Working already implies hide-checked.
        workingOnly && !hasPositiveCriteria ->
            filterNestedTreePass(
                roots, start = null, end = null, query = "", hideChecked = true,
                selectedTagIds = emptySet(), workingOnly = true,
                includeNotesInQuery = includeNotesInQuery,
                overdueBefore = null,
                today = today
            )
        // Nothing narrows the tree: preserve identity so an idle filter bar
        // doesn't deep-copy the outline or invalidate the caller's cache.
        !workingOnly && !hasPositiveCriteria && !hideChecked ->
            return NestedOutlineProjection(roots, emptyList())
        else -> {
            val firstPass = filterNestedTreePass(
                roots, start, end, query, hideChecked, selectedTagIds, workingOnly = false,
                includeNotesInQuery = includeNotesInQuery,
                overdueBefore = overdueBefore,
                today = null
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
                    includeNotesInQuery = includeNotesInQuery,
                    overdueBefore = null,
                    today = today
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
 * date-based metric, an open checkbox, visible progress, or tracked time.
 * When [today] is given, overdue items (end date before [today]) are not
 * working: they belong to the Overdue criterion instead. */
fun isWorkingItem(item: NestedListItem, today: LocalDate? = null): Boolean {
    if (today != null && item.endDate != null && item.endDate < today) return false
    return item.priority != TaskPriority.None ||
            item.startDate != null || item.endDate != null ||
            item.manualMetrics.any { it.enabled && (it.unit == MetricUnit.Countdown || it.unit == MetricUnit.DueDate) } ||
            (item.checkboxEnabled && !item.checked) ||
            item.progressPercent != null ||
            item.actualMinutes > 0
}

/**
 * True when this row is still an open deadline: an unchecked checklist task or
 * an unfinished date-based metric. Finished work and plain dated notes are not
 * deadlines, so a past range on them must not be painted as a miss.
 */
fun NestedListItem.isOpenDeadlineItem(): Boolean =
    !checked && (
        checkboxEnabled ||
            manualMetrics.any {
                it.enabled && !it.isCompleted &&
                    (it.unit == MetricUnit.Countdown || it.unit == MetricUnit.DueDate)
            }
        )

/** True when an open deadline's inclusive end date has passed. */
fun NestedListItem.hasMissedDeadline(today: LocalDate): Boolean =
    isOpenDeadlineItem() && endDate != null && endDate < today

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
    includeNotesInQuery: Boolean,
    overdueBefore: LocalDate?,
    today: LocalDate?
): List<FilteredNestedNode> {
    if (roots.isEmpty()) return emptyList()
    val results = HashMap<String, FilteredNestedNode>(roots.size * 2)
    val stack = ArrayDeque<FilterFrame>()

    fun push(node: NestedItemNode, forceKeep: Boolean) {
        // Hide Checked prunes the item and its whole subtree.
        if (hideChecked && node.item.checked) return
        val matchesSelf = matchesNestedFilter(
            node.item, start, end, query, selectedTagIds, workingOnly, includeNotesInQuery,
            overdueBefore, today
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
    includeNotesInQuery: Boolean,
    overdueBefore: LocalDate?,
    today: LocalDate?
): Boolean {
    if (start != null && end != null) {
        if (!matchesNestedDateWindow(item.startDate, item.endDate, start, end)) return false
    }
    // Overdue looks at the deadline only: an inclusive end date before
    // [overdueBefore]. Ongoing and future work must not match.
    if (overdueBefore != null) {
        val endDate = item.endDate ?: return false
        if (endDate >= overdueBefore) return false
    }
    if (query.isNotBlank()) {
        val titleMatches = item.text.contains(query, ignoreCase = true)
        val noteMatches = includeNotesInQuery &&
            item.note?.contains(query, ignoreCase = true) == true
        if (!titleMatches && !noteMatches) return false
    }
    if (selectedTagIds.isNotEmpty() && item.tags.none { it.id in selectedTagIds }) return false
    if (workingOnly && !isWorkingItem(item, today)) return false
    return true
}

/**
 * True when a node's optional date range intersects the closed filter window.
 *
 * An item bound missing means that side is open-ended: a start-only node lives
 * from that day onward and an end-only node lives up to that day, so both now
 * participate in date filters instead of being excluded. A node with no dates
 * at all never matches a date window. Boundaries are inclusive because the
 * window is a calendar period (`FocusPeriod.endInclusive`).
 *
 * Descendants are not evaluated here: [filterNestedTreePass] force-keeps the
 * whole subtree of a match, which is what keeps undated children visible.
 */
internal fun matchesNestedDateWindow(
    itemStart: LocalDate?,
    itemEnd: LocalDate?,
    windowStart: LocalDate,
    windowEnd: LocalDate
): Boolean = when {
    itemStart == null && itemEnd == null -> false
    itemStart == null -> itemEnd!! >= windowStart
    itemEnd == null -> itemStart <= windowEnd
    else -> itemStart <= windowEnd && itemEnd >= windowStart
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
