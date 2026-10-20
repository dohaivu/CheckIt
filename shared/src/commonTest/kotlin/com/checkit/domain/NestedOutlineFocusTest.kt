package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Context metadata on top of [filterNestedTreeWithFocus]: which rows are
 * ancestor scaffolding kept only to hold matches, and therefore render muted.
 */
class NestedOutlineFocusTest {

    private fun item(
        id: String,
        parentId: String?,
        position: Int = 0,
        priority: TaskPriority = TaskPriority.None,
        checkboxEnabled: Boolean = false,
        checked: Boolean = false
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = "item $id",
        priority = priority,
        checkboxEnabled = checkboxEnabled,
        checked = checked,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    @Test
    fun noCriteriaReportsNoContext() {
        val roots = buildNestedTree(
            listOf(
                item("root", null, 0),
                item("child", "root", 0, priority = TaskPriority.High),
            )
        )
        val focus = filterNestedTreeWithFocus(roots)
        assertTrue(focus.contextIds.isEmpty())
        assertEquals(listOf("root"), focus.roots.map { it.item.id })
        assertEquals(listOf("child"), focus.roots.first().children.map { it.item.id })
    }

    @Test
    fun hideCheckedAloneReportsNoContext() {
        val roots = buildNestedTree(
            listOf(
                item("root", null, 0),
                item("child", "root", 0, checkboxEnabled = true),
            )
        )
        val focus = filterNestedTreeWithFocus(roots, hideChecked = true)
        assertTrue(focus.contextIds.isEmpty())
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
        val focus = filterNestedTreeWithFocus(roots, workingOnly = true)
        assertEquals(listOf("root"), focus.contextIds)
        assertEquals(listOf("root", "busy"), focus.roots.flatMapIds())
    }

    @Test
    fun matchWithOnlyPlainChildrenIsNotContext() {
        val roots = buildNestedTree(
            listOf(
                item("busy", null, 0, priority = TaskPriority.Low),
                item("plain", "busy", 0),
            )
        )
        val focus = filterNestedTreeWithFocus(roots, workingOnly = true)
        assertTrue(focus.contextIds.isEmpty())
    }

    @Test
    fun workingMatchUnderMatchIsNotContext() {
        val roots = buildNestedTree(
            listOf(
                item("project", null, 0, priority = TaskPriority.High),
                item("task", "project", 0, priority = TaskPriority.High),
            )
        )
        val focus = filterNestedTreeWithFocus(roots, workingOnly = true)
        assertTrue(focus.contextIds.isEmpty())
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
        val focus = filterNestedTreeWithFocus(roots, workingOnly = true)
        assertEquals(listOf("root", "middle"), focus.contextIds)
        assertEquals(listOf("root", "middle", "task"), focus.roots.flatMapIds())
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
        val focus = filterNestedTreeWithFocus(roots, query = "milk")
        assertEquals(listOf("groceries"), focus.contextIds)
        assertEquals(listOf("groceries", "buy milk"), focus.roots.flatMapIds())
    }

    @Test
    fun descendantsOfMatchAreNotContext() {
        val roots = buildNestedTree(
            listOf(
                item("busy", null, 0, priority = TaskPriority.Low),
                item("child", "busy", 0),
            )
        )
        val focus = filterNestedTreeWithFocus(roots, workingOnly = true)
        assertTrue(focus.contextIds.isEmpty())
        assertEquals(listOf("busy", "child"), focus.roots.flatMapIds())
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
