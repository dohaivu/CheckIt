package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NestedWorkingFilterTest {

    private fun item(
        id: String,
        parentId: String?,
        position: Int = 0,
        priority: TaskPriority = TaskPriority.None,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        metrics: List<MetricItem> = emptyList()
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = "item $id",
        priority = priority,
        startDate = startDate,
        endDate = endDate,
        manualMetrics = metrics,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    private fun metric(unit: MetricUnit, enabled: Boolean = true) = MetricItem(
        name = "m",
        value = "1",
        unit = unit,
        enabled = enabled
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
    fun priorityMarksWorking() {
        assertTrue(isWorkingItem(item("a", null, priority = TaskPriority.High)))
    }

    @Test
    fun dueDatesMarkWorking() {
        val day = LocalDate(2026, 10, 5)
        assertTrue(isWorkingItem(item("a", null, startDate = day)))
        assertTrue(isWorkingItem(item("b", null, endDate = day)))
    }

    @Test
    fun enabledDateMetricsMarkWorking() {
        assertTrue(isWorkingItem(item("a", null, metrics = listOf(metric(MetricUnit.Countdown)))))
        assertTrue(isWorkingItem(item("b", null, metrics = listOf(metric(MetricUnit.DueDate)))))
    }

    @Test
    fun disabledOrPlainMetricsAreNotWorking() {
        assertFalse(isWorkingItem(item("a", null, metrics = listOf(metric(MetricUnit.Countdown, enabled = false)))))
        assertFalse(isWorkingItem(item("b", null, metrics = listOf(metric(MetricUnit.Points)))))
        assertFalse(isWorkingItem(item("c", null)))
    }

    @Test
    fun workingOnlyKeepsMatchesAndAncestors() {
        val roots = buildNestedTree(
            listOf(
                item("root", null, 0),
                item("plain", "root", 0),
                item("busy", "root", 1, priority = TaskPriority.Medium),
                item("other", null, 1),
            )
        )
        val filtered = filterNestedTree(roots, start = null, end = null, workingOnly = true)
        assertEquals(listOf("root", "busy"), flatIds(filtered))
    }

    @Test
    fun workingOnlyForceKeepsDescendantsOfMatch() {
        val roots = buildNestedTree(
            listOf(
                item("busy", null, 0, priority = TaskPriority.Low),
                item("child", "busy", 0),
            )
        )
        val filtered = filterNestedTree(roots, start = null, end = null, workingOnly = true)
        assertEquals(listOf("busy", "child"), flatIds(filtered))
    }

    @Test
    fun workingOnlyPrunesEverythingWithoutSignal() {
        val roots = buildNestedTree(
            listOf(
                item("a", null, 0),
                item("b", "a", 0),
            )
        )
        assertTrue(filterNestedTree(roots, start = null, end = null, workingOnly = true).isEmpty())
    }

    @Test
    fun workingOnlyComposesWithHideChecked() {
        val roots = buildNestedTree(
            listOf(
                item("busy", null, 0, priority = TaskPriority.High),
            )
        )
        val tree = roots.map {
            it.copy(item = it.item.copy(checked = true))
        }
        val filtered = filterNestedTree(tree, start = null, end = null, hideChecked = true, workingOnly = true)
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun outlineRootsAppliesQuery() {
        val roots = buildNestedTree(
            listOf(
                item("groceries", null, 0),
                item("buy milk", "groceries", 0),
                item("other", null, 1),
            )
        )
        val filtered = filterOutlineRoots(roots, query = "milk")
        assertEquals(listOf("groceries", "buy milk"), flatIds(filtered))
    }

    @Test
    fun outlineRootsCombinesQueryAndWorking() {
        val roots = buildNestedTree(
            listOf(
                item("groceries", null, 0),
                item("buy milk", "groceries", 0, priority = TaskPriority.Low),
                item("buy eggs", "groceries", 1),
            )
        )
        val filtered = filterOutlineRoots(roots, query = "buy", workingOnly = true)
        assertEquals(listOf("groceries", "buy milk"), flatIds(filtered))
    }
}
