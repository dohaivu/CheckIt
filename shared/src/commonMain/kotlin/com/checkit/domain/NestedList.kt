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

enum class MetricRollupPolicy {
    IncludeChildren,
    OwnOnly,
    ExcludeFromParent
}

data class NestedMetricSummary(
    val doneItemCount: Int = 0,
    val trackedMinutes: Int = 0
)

/**
 * A single nested-lists document. Holds one unlimited-depth item tree.
 */
data class NestedDocument(
    val id: String = "",
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

/**
 * One node of a [NestedDocument] tree. Uniform line; checkbox style is per-item.
 * [parentId] null means the item sits at the document root level.
 */
data class NestedListItem(
    val id: String = "",
    val documentId: String,
    val parentId: String? = null,
    val position: Int,
    val text: String,
    val note: String? = null,
    val checkboxEnabled: Boolean = false,
    val checked: Boolean = false,
    /** When the item was last checked; null when never checked or unchecked. */
    val completedAtMillis: Long? = null,
    val collapsed: Boolean = false,
    val textStyle: NestedTextStyle = NestedTextStyle.Body,
    val textColor: NestedColorToken = NestedColorToken.Default,
    val backgroundColor: NestedColorToken = NestedColorToken.Default,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val priority: TaskPriority = TaskPriority.None,
    val tags: List<TagItem> = emptyList(),
    val actualMinutes: Int = 0,
    val metricRollupPolicy: MetricRollupPolicy = MetricRollupPolicy.IncludeChildren,
    val showTrackedMinutes: Boolean = false,
    /** Manual progress 0..100; null means progress UI is hidden. */
    val progressPercent: Int? = null,
    val manualMetrics: List<MetricItem> = emptyList(),
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
    val nodeById: Map<String, NestedItemNode> by lazy { indexNestedNodes(rootNodes) }
    val itemById: Map<String, NestedListItem> by lazy { nodeById.mapValues { it.value.item } }
    val flatItems: List<NestedListItem> by lazy { flattenNestedNodes(rootNodes) }
    val metricSummaryById: Map<String, NestedMetricSummary> by lazy { calculateNestedMetricSummaries(rootNodes) }
}

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
        val doneItemCount = node.children.count { it.item.checked }
        var trackedMinutes = node.item.actualMinutes.coerceAtLeast(0)
        node.children.forEach { child ->
            val childSummary = summaries[child.item.id] ?: return@forEach
            if (node.item.metricRollupPolicy != MetricRollupPolicy.OwnOnly &&
                child.item.metricRollupPolicy != MetricRollupPolicy.ExcludeFromParent
            ) {
                trackedMinutes += childSummary.trackedMinutes
            }
        }
        summaries[node.item.id] = NestedMetricSummary(doneItemCount, trackedMinutes)
    }
    return summaries
}

/**
 * Swift-friendly outline projection for the macOS Working/search UI: text
 * [query] plus the Working filter, with the same ancestor-context keep
 * semantics as [filterNestedTree]. Lets Apple clients reuse the shared
 * logic without date/tag interop friction.
 */
fun filterOutlineRoots(
    roots: List<NestedItemNode>,
    query: String = "",
    workingOnly: Boolean = false
): List<NestedItemNode> =
    filterNestedTree(roots, start = null, end = null, query = query, workingOnly = workingOnly)

/**
 * Prunes the tree, keeping only nodes that overlap with [start] to [end]
 * (inclusive) OR have descendants that do. Supports text [query] and [hideChecked] status.
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
): List<NestedItemNode> {
    val found = roots.mapNotNull { filterNestedNode(it, start, end, query, hideChecked, selectedTagIds) }
    if (!workingOnly) return found
    // Refine (see KDoc): checked subtrees were already pruned above when
    // hideChecked is set; Working additionally implies hide-checked, since
    // done work is noise in a needs-attention view (All stays for review).
    return found.mapNotNull {
        filterNestedNode(it, null, null, "", hideChecked = true, workingOnly = true)
    }
}

private fun filterNestedNode(
    node: NestedItemNode,
    start: LocalDate?,
    end: LocalDate?,
    query: String,
    hideChecked: Boolean,
    selectedTagIds: Set<String> = emptySet(),
    forceKeep: Boolean = false,
    workingOnly: Boolean = false
): NestedItemNode? {
    val item = node.item

    // 1. Hide Checked: if enabled and item is checked, prune it and its subtree entirely
    if (hideChecked && item.checked) return null

    // 2. Determine if this node matches the POSITIVE criteria (Search + Date + Tags)
    
    // Date match: overlap with start/end if filter is provided
    val matchesDate = if (start != null && end != null) {
        if (item.startDate != null && item.endDate != null) {
            item.startDate <= end && item.endDate >= start
        } else {
            false
        }
    } else {
        true // No date filter active
    }
    val matchesQuery = if (query.isNotBlank()) {
        item.text.contains(query, ignoreCase = true) || item.note?.contains(query, ignoreCase = true) == true
    } else {
        true
    }
    val matchesTags = if (selectedTagIds.isNotEmpty()) {
        item.tags.any { it.id in selectedTagIds }
    } else {
        true
    }
    val matchesWorking = if (workingOnly) {
        isWorkingItem(item)
    } else {
        true
    }

    // Direct match means it satisfies search, date, tag and working constraints
    val matchesSelf = matchesDate && matchesQuery && matchesTags && matchesWorking

    // If an ancestor matched OR this node matches, we "force keep" descendants
    val shouldForceKeepDescendants = forceKeep || matchesSelf

    // 3. Recurse children
    val filteredChildren = node.children.mapNotNull {
        filterNestedNode(it, start, end, query, hideChecked, selectedTagIds, shouldForceKeepDescendants, workingOnly)
    }

    // Keep node if:
    // a) It's forced kept (because an ancestor matched positive filters)
    // b) It matched positive filters directly
    // c) It has descendants that matched (bottom-up context)
    return if (shouldForceKeepDescendants || filteredChildren.isNotEmpty()) {
        node.copy(children = filteredChildren)
    } else {
        null
    }
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

/**
 * Computes the (parentId, position) for inserting a new item, mirroring the
 * editor's draft-anchor semantics: a draft anchored to its own parent (or to
 * nothing) appends as a child; otherwise it inserts as the next sibling after
 * the anchor. Pure and shared by the Compose ViewModel and the Apple bridge.
 */
fun computeNestedInsertPosition(
    items: List<NestedListItem>,
    anchorId: String?,
    parentId: String?
): Pair<String?, Int?> {
    val anchor = items.firstOrNull { it.id == anchorId }
    val isAddingChild = parentId != null &&
        (anchor?.id == parentId || anchor?.parentId != parentId)
    val position = if (isAddingChild) {
        items.filter { it.parentId == parentId }.maxOfOrNull { it.position }?.plus(1) ?: 0
    } else {
        anchor?.position?.plus(1)
    }
    return parentId to position
}

/**
 * A single re-parent/reorder instruction produced by [planNestedMoves].
 * Sibling gaps in [position] are acceptable; relative order is what matters.
 */
data class NestedItemMove(
    val itemId: String,
    val parentId: String?,
    val position: Int
)

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
 * (position, id), recurses. Roots are items whose [NestedListItem.parentId] is
 * null. There is no depth limit.
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
    seed(roots)
    fun drain() {        while (stack.isNotEmpty()) {
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
    // Unreachable rows (orphaned parents, parent cycles from concurrent
    // cross-device moves) seed as extra roots instead of being dropped, so
    // divergent rows stay visible. One deterministic representative at a
    // time; its subtree builds via normal expansion. The expanded guard
    // guarantees termination and each round builds at least the seed.
    drain()
    val rootOrder = compareBy<NestedListItem> { it.position }.thenBy { it.id }
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

private fun indexNestedNodes(roots: List<NestedItemNode>): Map<String, NestedItemNode> {
    val result = HashMap<String, NestedItemNode>()
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
    itemIds: Set<String>,
    targetParentId: String?,
    targetIndex: Int
): List<NestedItemMove> {
    if (itemIds.isEmpty()) return emptyList()

    val itemsById = items.associateBy { it.id }
    val selected = itemIds.mapNotNull { itemsById[it] }

    val childrenByParent = items.groupBy { it.parentId }
    fun descendantsOf(id: String): Set<String> {
        val result = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
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
fun nestedDescendantIds(items: List<NestedListItem>, itemId: String): Set<String> {
    val childrenByParent = items.groupBy { it.parentId }
    val result = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    queue.add(itemId)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        childrenByParent[current].orEmpty().forEach { child ->
            if (result.add(child.id)) queue.add(child.id)
        }
    }
    return result
}
