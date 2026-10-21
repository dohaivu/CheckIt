package com.checkit.domain

/**
 * A single re-parent/reorder instruction produced by move planning.
 * Sibling gaps in [position] are acceptable; relative order is what matters.
 */
data class NestedItemMove(
    val itemId: String,
    val parentId: String?,
    val position: Int
)

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

/** Candidate parents for "move under": siblings and parent-siblings, in display order. */
data class MoveDestinations(
    val siblings: List<NestedListItem>,
    val parentSiblings: List<NestedListItem>
) {
    val isEmpty: Boolean get() = siblings.isEmpty() && parentSiblings.isEmpty()
}

/**
 * Lists where [itemId] may move under: its siblings (excluding itself) and
 * its parent's siblings (excluding its parent). Roots have no parent-siblings.
 * Siblings and uncles can never sit inside the moved subtree, so the set is
 * inherently cycle-safe. Pure for UI reuse on both platforms.
 */
fun moveDestinations(items: List<NestedListItem>, itemId: String): MoveDestinations {
    val item = items.firstOrNull { it.id == itemId }
        ?: return MoveDestinations(emptyList(), emptyList())
    val inGroupOrder = compareBy<NestedListItem> { it.position }.thenBy { it.id }
    val siblings = items
        .filter { it.parentId == item.parentId && it.id != itemId }
        .sortedWith(inGroupOrder)
    val parent = items.firstOrNull { it.id == item.parentId }
    val parentSiblings = if (parent == null) {
        emptyList()
    } else {
        items
            .filter { it.parentId == parent.parentId && it.id != parent.id }
            .sortedWith(inGroupOrder)
    }
    return MoveDestinations(siblings, parentSiblings)
}
