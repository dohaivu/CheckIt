package com.checkit.domain

/**
 * Immutable, document-independent snapshot of a copied subtree.
 *
 * [NestedItemNode] and [NestedListItem] are immutable data classes, so keeping
 * the node references is a safe snapshot: later edits in the source document
 * produce new item instances and never mutate this clip.
 *
 * The clip carries no destination information. Pasting may target any document
 * and any parent; [planNestedSubtreePaste] remaps ids and parent links.
 */
data class NestedSubtreeClip(
    val roots: List<NestedItemNode>,
    val sourceDocumentId: String
) {
    /** Total number of rows in the clip, including every descendant. */
    val itemCount: Int by lazy {
        var count = 0
        val stack = ArrayDeque<NestedItemNode>()
        roots.asReversed().forEach(stack::addLast)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            count++
            node.children.asReversed().forEach(stack::addLast)
        }
        count
    }

    val isEmpty: Boolean get() = roots.isEmpty()
}

/**
 * Snapshots the subtree rooted at [itemId], including collapsed descendants
 * and rows hidden by the current filter/zoom. Returns null when the item is
 * not part of [tree].
 */
fun captureNestedSubtree(tree: NestedDocumentTree, itemId: String): NestedSubtreeClip? {
    val node = tree.nodeById[itemId] ?: return null
    return NestedSubtreeClip(
        roots = listOf(node),
        sourceDocumentId = tree.document.id
    )
}

/**
 * Plans a fresh copy of [clip] under [parentId] in [documentId].
 *
 * Every row gets a new id from [newId], a new [NestedListItem.createdAtMillis]/
 * [NestedListItem.updatedAtMillis] of [nowMillis], and a remapped parent id.
 * All other fields are preserved, including checked state, tags, styles, dates
 * and metrics. Pasted roots are expanded so the copy's descendants are visible;
 * descendants keep their original collapsed state.
 *
 * Root rows are placed at consecutive positions starting at [startPosition],
 * and children at consecutive positions within their new parent. The returned
 * list is pre-order (parents before children) so it can be inserted directly
 * with Room foreign keys enabled.
 */
fun planNestedSubtreePaste(
    clip: NestedSubtreeClip,
    documentId: String,
    parentId: String?,
    startPosition: Int,
    nowMillis: Long,
    newId: () -> String
): List<NestedListItem> {
    if (clip.isEmpty) return emptyList()

    data class Pending(
        val node: NestedItemNode,
        val newParentId: String?,
        val position: Int,
        val isRoot: Boolean
    )

    val result = ArrayList<NestedListItem>(clip.itemCount)
    val stack = ArrayDeque<Pending>()
    for (index in clip.roots.indices.reversed()) {
        stack.addLast(
            Pending(
                node = clip.roots[index],
                newParentId = parentId,
                position = startPosition + index,
                isRoot = true
            )
        )
    }

    while (stack.isNotEmpty()) {
        val pending = stack.removeLast()
        val source = pending.node.item
        val newItemId = newId()
        result.add(
            source.copy(
                id = newItemId,
                documentId = documentId,
                parentId = pending.newParentId,
                position = pending.position,
                collapsed = if (pending.isRoot) false else source.collapsed,
                createdAtMillis = nowMillis,
                updatedAtMillis = nowMillis
            )
        )
        for (index in pending.node.children.indices.reversed()) {
            stack.addLast(
                Pending(
                    node = pending.node.children[index],
                    newParentId = newItemId,
                    position = index,
                    isRoot = false
                )
            )
        }
    }
    return result
}
