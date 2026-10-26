package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Context metadata from [filterNestedTreeWithContext]: which rows are ancestor
 * scaffolding kept only to hold matches, and therefore render muted.
 */
class NestedOutlineProjectionTest {

    private fun item(
        id: String,
        parentId: String?,
        position: Int = 0,
        priority: TaskPriority = TaskPriority.None,
        checkboxEnabled: Boolean = false,
        checked: Boolean = false,
        text: String? = null,
        note: String? = null
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = text ?: "item $id",
        note = note,
        priority = priority,
        checkboxEnabled = checkboxEnabled,
        checked = checked,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    @Test
    fun noCriteriaPreservesRootsAndReportsNoContext() {
        val roots = buildNestedTree(
            listOf(
                item("root", null, 0),
                item("child", "root", 0, priority = TaskPriority.High),
            )
        )
        val projection = filterNestedTreeWithContext(roots)
        // Identity matters: an idle filter must not deep-copy the outline.
        assertSame(roots, projection.roots)
        assertTrue(projection.contextIds.isEmpty())
    }

    @Test
    fun hideCheckedAloneReportsNoContext() {
        val roots = buildNestedTree(
            listOf(
                item("root", null, 0),
                item("child", "root", 0, checkboxEnabled = true),
            )
        )
        val projection = filterNestedTreeWithContext(roots, hideChecked = true)
        assertTrue(projection.contextIds.isEmpty())
    }

    @Test
    fun workingAncestorsAreContext() {
        val roots = buildNestedTree(
            listOf(
                item("root", null, 0),
                item("plain", "root", 0),
                item("busy", "root", 1, priority = TaskPriority.Medium),
                item("other", null, 1),
            )
        )
        val projection = filterNestedTreeWithContext(roots, workingOnly = true)
        assertEquals(listOf("root"), projection.contextIds)
        assertEquals(listOf("root", "busy"), projection.roots.flatMapIds())
    }

    @Test
    fun matchWithOnlyPlainChildrenIsNotContext() {
        val roots = buildNestedTree(
            listOf(
                item("busy", null, 0, priority = TaskPriority.Low),
                item("plain", "busy", 0),
            )
        )
        val projection = filterNestedTreeWithContext(roots, workingOnly = true)
        assertTrue(projection.contextIds.isEmpty())
    }

    @Test
    fun workingMatchUnderMatchIsNotContext() {
        val roots = buildNestedTree(
            listOf(
                item("project", null, 0, priority = TaskPriority.High),
                item("task", "project", 0, priority = TaskPriority.High),
            )
        )
        val projection = filterNestedTreeWithContext(roots, workingOnly = true)
        assertTrue(projection.contextIds.isEmpty())
    }

    @Test
    fun intermediateAncestorsAreAllContextInTopDownOrder() {
        val roots = buildNestedTree(
            listOf(
                item("root", null, 0),
                item("middle", "root", 0),
                item("task", "middle", 0, checkboxEnabled = true),
            )
        )
        val projection = filterNestedTreeWithContext(roots, workingOnly = true)
        assertEquals(listOf("root", "middle"), projection.contextIds)
        assertEquals(listOf("root", "middle", "task"), projection.roots.flatMapIds())
    }

    @Test
    fun queryMatchesArePrefixedByContextAncestors() {
        val roots = buildNestedTree(
            listOf(
                item("groceries", null, 0),
                item("buy milk", "groceries", 0),
                item("other", null, 1),
            )
        )
        val projection = filterNestedTreeWithContext(roots, query = "milk")
        assertEquals(listOf("groceries"), projection.contextIds)
        assertEquals(listOf("groceries", "buy milk"), projection.roots.flatMapIds())
    }

    @Test
    fun queryMatchesTitleWhenNotesAreExcluded() {
        val roots = buildNestedTree(
            listOf(
                item("groceries", null, 0),
                item("milk", "groceries", 0, text = "buy milk", note = "unrelated"),
                item("other", null, 1),
            )
        )
        val projection = filterNestedTreeWithContext(
            roots, query = "milk", includeNotesInQuery = false
        )
        assertEquals(listOf("groceries"), projection.contextIds)
        assertEquals(listOf("groceries", "milk"), projection.roots.flatMapIds())
    }

    @Test
    fun queryIgnoresNotesWhenNotesAreExcluded() {
        val roots = buildNestedTree(
            listOf(
                item("call", null, 0, text = "call mom", note = "buy milk"),
                item("other", null, 1),
            )
        )
        val projection = filterNestedTreeWithContext(
            roots, query = "milk", includeNotesInQuery = false
        )
        assertTrue(projection.roots.isEmpty())
        assertTrue(projection.contextIds.isEmpty())
    }

    @Test
    fun queryIgnoresNotesByDefault() {
        val roots = buildNestedTree(
            listOf(
                item("call", null, 0, text = "call mom", note = "buy milk"),
                item("other", null, 1),
            )
        )
        val projection = filterNestedTreeWithContext(roots, query = "milk")
        assertTrue(projection.roots.isEmpty())
        assertTrue(projection.contextIds.isEmpty())
    }

    @Test
    fun descendantsOfMatchAreNotContext() {
        val roots = buildNestedTree(
            listOf(
                item("busy", null, 0, priority = TaskPriority.Low),
                item("child", "busy", 0),
            )
        )
        val projection = filterNestedTreeWithContext(roots, workingOnly = true)
        assertTrue(projection.contextIds.isEmpty())
        assertEquals(listOf("busy", "child"), projection.roots.flatMapIds())
    }

    @Test
    fun deepChainsFilterWithoutRecursion() {
        val depth = 20_000
        val items = (0 until depth).map { index ->
            item(
                id = "n$index",
                parentId = if (index == 0) null else "n${index - 1}",
                position = 0,
                checkboxEnabled = index == depth - 1,
            )
        }
        val roots = buildNestedTree(items)
        val projection = filterNestedTreeWithContext(roots, workingOnly = true)
        assertEquals(depth - 1, projection.contextIds.size)
        assertEquals("n0", projection.contextIds.first())
        assertEquals("n${depth - 2}", projection.contextIds.last())
        assertEquals(listOf("n0"), projection.roots.map { it.item.id })
    }

    /** Pre-order ids across the pruned forest. */
    private fun List<NestedItemNode>.flatMapIds(): List<String> {
        val out = mutableListOf<String>()
        val stack = ArrayDeque<NestedItemNode>()
        asReversed().forEach(stack::addLast)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            out += node.item.id
            node.children.asReversed().forEach(stack::addLast)
        }
        return out
    }
}
