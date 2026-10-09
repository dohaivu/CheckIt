package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FocusSubtreesTest {

    private fun item(
        id: String,
        parentId: String?,
        position: Int = 0,
        priority: TaskPriority = TaskPriority.None,
        checked: Boolean = false,
        collapsed: Boolean = false
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = "item $id",
        priority = priority,
        checked = checked,
        collapsed = collapsed,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    private fun flatIds(roots: List<NestedItemNode>): List<String> {
        val out = mutableListOf<String>()
        val stack = ArrayDeque(roots)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            out += node.item.id
            node.children.asReversed().forEach(stack::addLast)
        }
        return out
    }

    @Test
    fun promotesTopmostHighPriorityWithoutAncestors() {
        val roots = buildNestedTree(
            listOf(
                item("plain", null, 0),
                item("hot", "plain", 0, priority = TaskPriority.High),
                item("other", null, 1),
            )
        )
        assertEquals(listOf("hot"), flatIds(focusSubtrees(roots)))
    }

    @Test
    fun nestedMatchShowsOnceUnderParent() {
        val roots = buildNestedTree(
            listOf(
                item("p1", null, 0, priority = TaskPriority.High),
                item("p2", "p1", 0, priority = TaskPriority.High),
            )
        )
        assertEquals(listOf("p1", "p2"), flatIds(focusSubtrees(roots)))
    }

    @Test
    fun descendantsCappedAtMaxDepth() {
        val roots = buildNestedTree(
            listOf(
                item("hot", null, 0, priority = TaskPriority.High),
                item("c1", "hot", 0),
                item("c2", "c1", 0),
                item("c3", "c2", 0),
            )
        )
        assertEquals(listOf("hot", "c1", "c2"), flatIds(focusSubtrees(roots, maxDepth = 2)))
        assertEquals(listOf("hot", "c1"), flatIds(focusSubtrees(roots, maxDepth = 1)))
    }

    @Test
    fun checkedSubtreesPrunedEntirely() {
        val roots = buildNestedTree(
            listOf(
                item("done", null, 0, priority = TaskPriority.High, checked = true),
                item("child", "done", 0, priority = TaskPriority.High),
                item("open", null, 1, priority = TaskPriority.High),
            )
        )
        assertEquals(listOf("open"), flatIds(focusSubtrees(roots)))
    }

    @Test
    fun collapseIgnored() {
        val roots = buildNestedTree(
            listOf(
                item("hot", null, 0, priority = TaskPriority.High, collapsed = true),
                item("child", "hot", 0),
            )
        )
        assertEquals(listOf("hot", "child"), flatIds(focusSubtrees(roots)))
    }

    @Test
    fun plainTreeYieldsNothing() {
        val roots = buildNestedTree(listOf(item("a", null, 0)))
        assertTrue(focusSubtrees(roots).isEmpty())
    }

    @Test
    fun flattenFocusRowsUsesRelativeDepth() {
        val roots = buildNestedTree(
            listOf(
                item("plain", null, 0),
                item("hot", "plain", 0, priority = TaskPriority.High),
                item("kid", "hot", 0),
            )
        )
        val rows = flattenFocusRows(focusSubtrees(roots))
        assertEquals(listOf("hot" to 0, "kid" to 1), rows.map { it.item.id to it.depth })
        assertEquals(listOf(true, false), rows.map { it.hasChildren })
    }
}
