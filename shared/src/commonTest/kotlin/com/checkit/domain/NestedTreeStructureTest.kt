package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [buildNestedTree] never drops rows: orphans and parent cycles surface
 * as extra roots instead of vanishing (sync-merge safety).
 */
class NestedTreeStructureTest {

    private fun item(id: String, parentId: String?, position: Int) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = "item $id",
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    private fun flattenIds(roots: List<NestedItemNode>): List<String> {
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
    fun normalTreeBuildsNested() {
        val roots = buildNestedTree(
            listOf(
                item("1", null, 0),
                item("2", "1", 0),
                item("3", null, 1),
            )
        )
        assertEquals(listOf("1", "3"), roots.map { it.item.id })
        assertEquals(listOf("2"), roots.first().children.map { it.item.id })
    }

    @Test
    fun orphanAttachesAtRootSorted() {
        val roots = buildNestedTree(
            listOf(
                item("1", null, 1),
                item("orphan", "gone", 0),
            )
        )
        assertEquals(listOf("orphan", "1"), roots.map { it.item.id })
    }

    @Test
    fun orphanSubtreeStaysTogether() {
        val roots = buildNestedTree(
            listOf(
                item("1", null, 0),
                item("orphan", "gone", 0),
                item("child", "orphan", 0),
            )
        )
        assertEquals(listOf("1", "orphan"), roots.map { it.item.id })
        assertEquals(
            listOf("child"),
            roots.single { it.item.id == "orphan" }.children.map { it.item.id },
        )
    }

    @Test
    fun cycleTerminatesWithBothVisible() {
        val roots = buildNestedTree(
            listOf(
                item("1", null, 0),
                item("a", "b", 0),
                item("b", "a", 0),
            )
        )
        val ids = flattenIds(roots)
        assertEquals(listOf("1"), roots.map { it.item.id }.take(1))
        assertTrue(ids.containsAll(listOf("a", "b")), "cycle members must stay visible: $ids")
    }
}
