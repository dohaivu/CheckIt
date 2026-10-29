package com.checkit.ui.nested

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem

internal data class VisibleNestedRow(
    val node: NestedItemNode,
    val depth: Int,
    val isVisible: Boolean,
    val isInCheckedBranch: Boolean,
    val continuingLevels: Set<Int> = emptySet(),
    /** Item ids that own the continuing guide spine at each depth. */
    val continuingGuideOwners: Map<Int, String> = emptyMap(),
    /** Item ids of ancestors at each depth 0..<depth. */
    val ancestorGuideOwners: Map<Int, String> = emptyMap(),
    /** Kept only as ancestor scaffolding for a filtered match. */
    val isContext: Boolean = false
)

/** How far a row sits from the focused subtree. */
internal enum class NestedFocusTier { None, Focused, Path, Outside }

internal fun focusTier(
    index: Int,
    itemId: String,
    range: IntRange?,
    ancestorIds: Set<String>
): NestedFocusTier = when {
    range == null -> NestedFocusTier.None
    index in range -> NestedFocusTier.Focused
    itemId in ancestorIds -> NestedFocusTier.Path
    else -> NestedFocusTier.Outside
}

/**
 * Effective branch root for a selection. A selected parent is its own root;
 * a leaf promotes to its visible parent so focus and the selection spine keep
 * the surrounding sibling branch visible. If the parent is filtered out or
 * hidden, the leaf itself remains the root.
 */
internal fun selectionRootIdOf(
    rows: List<VisibleNestedRow>,
    tree: NestedDocumentTree,
    selectedId: String?
): String? {
    val id = selectedId ?: return null
    val node = tree.nodeById[id] ?: return null
    if (node.hasChildren) return id
    val parentId = node.item.parentId?.takeIf { it.isNotEmpty() } ?: return id
    return if (rows.any { it.node.item.id == parentId && it.isVisible }) parentId else id
}

/**
 * Pre-order rows make a subtree a contiguous index range: everything after
 * the root row whose depth is still deeper is a descendant. Returns null when
 * the root is missing or hidden in the current projection (filtered out,
 * collapsed under an ancestor, or outside the zoom root), so focus suspends
 * without touching filters.
 */
internal fun focusRowRange(rows: List<VisibleNestedRow>, rootId: String?): IntRange? {
    if (rootId == null) return null
    val start = rows.indexOfFirst { it.node.item.id == rootId }
    if (start < 0 || !rows[start].isVisible) return null
    val depth = rows[start].depth
    var end = start
    while (end + 1 < rows.size && rows[end + 1].depth > depth) end++
    return start..end
}

/**
 * Effective selection branch: the root row, its contiguous pre-order range,
 * and the guide column that carries its spine. Resolving this once lets rows
 * check membership in O(1) instead of walking ancestors one by one.
 */
internal data class NestedSelectionBranch(
    val rootId: String,
    val range: IntRange,
    val depth: Int
)

/** Resolve the selection branch for the current rows and selection. */
internal fun selectionBranchOf(
    rows: List<VisibleNestedRow>,
    tree: NestedDocumentTree,
    selectedId: String?
): NestedSelectionBranch? {
    val rootId = selectionRootIdOf(rows, tree, selectedId) ?: return null
    val range = focusRowRange(rows, rootId) ?: return null
    return NestedSelectionBranch(
        rootId = rootId,
        range = range,
        depth = rows[range.first].depth
    )
}

/** Ancestors of the selection root, used for the medium-emphasis path tier. */
internal fun focusAncestorIdsOf(tree: NestedDocumentTree, rootId: String?): Set<String> {
    if (rootId == null) return emptySet()
    val ancestors = LinkedHashSet<String>()
    var cursor: String? = tree.itemById[rootId]?.parentId
    while (true) {
        val id = cursor?.takeIf { it.isNotEmpty() } ?: break
        if (!ancestors.add(id)) break
        cursor = tree.itemById[id]?.parentId
    }
    return ancestors
}

private data class NestedTraversalEntry(
    val node: NestedItemNode,
    val depth: Int,
    val isVisible: Boolean,
    val ancestorChecked: Boolean,
    val continuingLevels: Set<Int>,
    val continuingGuideOwners: Map<Int, String>,
    val ancestorGuideOwners: Map<Int, String>
)

internal fun flattenVisibleNodes(
    roots: List<NestedItemNode>,
    contextIds: Set<String> = emptySet()
): List<VisibleNestedRow> {
    if (roots.isEmpty()) return emptyList()
    val result = ArrayList<VisibleNestedRow>()
    val stack = ArrayDeque<NestedTraversalEntry>()

    roots.asReversed().forEach { node ->
        stack.addLast(
            NestedTraversalEntry(
                node = node,
                depth = 0,
                isVisible = true,
                ancestorChecked = false,
                continuingLevels = emptySet(),
                continuingGuideOwners = emptyMap(),
                ancestorGuideOwners = emptyMap()
            )
        )
    }

    while (stack.isNotEmpty()) {
        val entry = stack.removeLast()
        val node = entry.node
        val isInCheckedBranch = entry.ancestorChecked || node.item.checked
        result += VisibleNestedRow(
            node = node,
            depth = entry.depth,
            isVisible = entry.isVisible,
            isInCheckedBranch = isInCheckedBranch,
            continuingLevels = entry.continuingLevels,
            continuingGuideOwners = entry.continuingGuideOwners,
            ancestorGuideOwners = entry.ancestorGuideOwners,
            isContext = node.item.id in contextIds
        )

        if (node.item.collapsed) continue

        val children = node.children
        children.asReversed().forEachIndexed { index, child ->
            val isLast = (index == 0) // asReversed: index 0 is the last child
            val nextContinuing = entry.continuingLevels.toMutableSet()
            val nextGuideOwners = entry.continuingGuideOwners.toMutableMap()
            if (!isLast) {
                nextContinuing.add(entry.depth)
                nextGuideOwners[entry.depth] = node.item.id
            } else {
                nextContinuing.remove(entry.depth)
                nextGuideOwners.remove(entry.depth)
            }
            val nextAncestorGuideOwners = entry.ancestorGuideOwners + (entry.depth to node.item.id)

            stack.addLast(
                NestedTraversalEntry(
                    node = child,
                    depth = entry.depth + 1,
                    isVisible = true,
                    ancestorChecked = isInCheckedBranch,
                    continuingLevels = nextContinuing,
                    continuingGuideOwners = nextGuideOwners,
                    ancestorGuideOwners = nextAncestorGuideOwners
                )
            )
        }
    }
    return result
}

// ---------------- drag & drop ----------------

/** Mutable state for an in-progress row drag; lives across gesture callbacks. */
internal class DragDropState(
    val itemId: String,
    val initialDepth: Int
) {
    // Observable so graphicsLayer/composition react to gesture mutations.
    var pointerViewportY by mutableStateOf(0)
    var totalDragY by mutableStateOf(0f)
    var totalDragX by mutableStateOf(0f)
    var targetParentId by mutableStateOf<String?>(null)
    var targetIndex by mutableStateOf(0)
    var indicatorRowId by mutableStateOf<String?>(null)
    var indicatorDepth by mutableStateOf(0)
    var below by mutableStateOf(false)
    var hasResolved by mutableStateOf(false)
}

internal data class ResolvedDropTarget(
    val parentId: String?,
    val index: Int,
    val rowId: String,
    val below: Boolean,
    val depth: Int
)

/**
 * Resolves the current drop placement from the dragged item's pointer position.
 * Uses the vertical gap between visible rows to determine allowed nesting depth,
 * and horizontal drag delta to choose the exact indent/outdent level.
 */
internal fun resolveDropTarget(
    tree: NestedDocumentTree,
    rows: List<VisibleNestedRow>,
    layoutInfo: LazyListLayoutInfo,
    drag: DragDropState,
    density: Density
): ResolvedDropTarget? {
    if (rows.isEmpty()) return null
    val entries = layoutInfo.visibleItemsInfo.filter { it.key is String }
    if (entries.isEmpty()) return null

    val pointerY = drag.pointerViewportY
    val hovered = entries.firstOrNull { pointerY in it.offset until (it.offset + it.size) }
        ?: if (pointerY < entries.first().offset) entries.first()
        else if (pointerY >= entries.last().offset + entries.last().size) entries.last()
        else entries.minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - pointerY) }
        ?: return null

    val hoveredKey = hovered.key as? String ?: return null
    val hoveredRowIdx = rows.indexOfFirst { it.node.item.id == hoveredKey }
    if (hoveredRowIdx < 0) return null
    val hoveredRow = rows[hoveredRowIdx]

    val rowCenterY = hovered.offset + hovered.size / 2
    val isBelow = pointerY > rowCenterY

    val aboveRow: VisibleNestedRow?
    val belowRow: VisibleNestedRow?
    if (!isBelow) {
        aboveRow = if (hoveredRowIdx > 0) rows[hoveredRowIdx - 1] else null
        belowRow = hoveredRow
    } else {
        aboveRow = hoveredRow
        belowRow = if (hoveredRowIdx < rows.lastIndex) rows[hoveredRowIdx + 1] else null
    }

    val minDepth = belowRow?.depth ?: 0
    val maxDepth = if (aboveRow != null) aboveRow.depth + 1 else 0
    val effectiveMinDepth = minDepth.coerceAtMost(maxDepth)

    val baseDepth = if (isBelow) aboveRow?.depth ?: 0 else belowRow?.depth ?: 0

    val stepPx = with(density) { 32.dp.toPx() }
    val depthDelta = (drag.totalDragX / stepPx).toInt()
    val targetDepth = (baseDepth + depthDelta).coerceIn(effectiveMinDepth, maxDepth)

    fun siblingsOf(parentId: String?): List<NestedListItem> {
        val nodes = if (parentId == null) tree.rootNodes else tree.nodeById[parentId]?.children.orEmpty()
        return nodes.mapNotNull { if (it.item.id != drag.itemId) it.item else null }
    }

    val targetParentId: String?
    val targetIndex: Int

    if (aboveRow == null) {
        targetParentId = belowRow?.node?.item?.parentId
        targetIndex = 0
    } else if (targetDepth == aboveRow.depth + 1) {
        targetParentId = aboveRow.node.item.id
        targetIndex = siblingsOf(aboveRow.node.item.id).size
    } else if (targetDepth == aboveRow.depth) {
        targetParentId = aboveRow.node.item.parentId
        val siblings = siblingsOf(targetParentId)
        val aboveIdx = siblings.indexOfFirst { it.id == aboveRow.node.item.id }
        targetIndex = if (aboveIdx >= 0) (aboveIdx + 1).coerceAtMost(siblings.size) else siblings.size
    } else {
        val levelsUp = aboveRow.depth - targetDepth
        var ancestorItem: NestedListItem? = aboveRow.node.item
        repeat(levelsUp) {
            ancestorItem = ancestorItem?.parentId?.let { tree.itemById[it] }
        }
        if (ancestorItem != null) {
            targetParentId = ancestorItem.parentId
            val siblings = siblingsOf(targetParentId)
            val ancIdx = siblings.indexOfFirst { it.id == ancestorItem.id }
            targetIndex = if (ancIdx >= 0) (ancIdx + 1).coerceAtMost(siblings.size) else siblings.size
        } else {
            targetParentId = null
            targetIndex = siblingsOf(null).size
        }
    }

    val displayRowId: String
    val displayBelow: Boolean
    if (belowRow != null) {
        displayRowId = belowRow.node.item.id
        displayBelow = false
    } else if (aboveRow != null) {
        displayRowId = aboveRow.node.item.id
        displayBelow = true
    } else {
        displayRowId = hoveredRow.node.item.id
        displayBelow = isBelow
    }

    return ResolvedDropTarget(
        parentId = targetParentId,
        index = targetIndex,
        rowId = displayRowId,
        below = displayBelow,
        depth = targetDepth
    )
}
