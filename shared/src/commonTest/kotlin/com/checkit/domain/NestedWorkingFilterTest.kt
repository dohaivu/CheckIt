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
        metrics: List<MetricItem> = emptyList(),
        checkboxEnabled: Boolean = false,
        checked: Boolean = false,
        progressPercent: Int? = null,
        actualMinutes: Int = 0
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
        checkboxEnabled = checkboxEnabled,
        checked = checked,
        progressPercent = progressPercent,
        actualMinutes = actualMinutes,
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
    fun openCheckboxMarksWorking() {
        assertTrue(isWorkingItem(item("a", null, checkboxEnabled = true)))
    }

    @Test
    fun checkedCheckboxWithoutOtherSignalsIsNotWorking() {
        assertFalse(isWorkingItem(item("a", null, checkboxEnabled = true, checked = true)))
    }

    @Test
    fun progressMarksWorking() {
        assertTrue(isWorkingItem(item("a", null, progressPercent = 40)))
    }

    @Test
    fun trackedTimeMarksWorking() {
        assertTrue(isWorkingItem(item("a", null, actualMinutes = 25)))
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
    fun workingImpliesHideChecked() {
        val roots = buildNestedTree(
            listOf(
                item("done", null, 0, priority = TaskPriority.High, checked = true),
                item("child", "done", 0, priority = TaskPriority.Low),
                item("open", null, 1, checkboxEnabled = true),
            )
        )
        val filtered = filterNestedTree(roots, start = null, end = null, workingOnly = true)
        assertEquals(listOf("open"), flatIds(filtered))
    }

    @Test
    fun workingImpliesHideCheckedWithQuery() {
        val roots = buildNestedTree(
            listOf(
                item("task done", null, 0, priority = TaskPriority.High, checked = true),
                item("task open", null, 1, priority = TaskPriority.Low),
            )
        )
        val filtered = filterOutlineRoots(roots, query = "task", workingOnly = true)
        assertEquals(listOf("task open"), flatIds(filtered))
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

    @Test
    fun queryMatchKeepsWorkingDescendants() {
        val roots = buildNestedTree(
            listOf(
                item("project", null, 0),
                item("task", "project", 0, priority = TaskPriority.High),
            )
        )
        val filtered = filterOutlineRoots(roots, query = "project", workingOnly = true)
        assertEquals(listOf("project", "task"), flatIds(filtered))
    }

    @Test
    fun workingAncestorKeepsQueryMatchingChild() {
        val roots = buildNestedTree(
            listOf(
                item("buy milk", null, 0, priority = TaskPriority.Low),
                item("oat milk", "buy milk", 0),
            )
        )
        val filtered = filterOutlineRoots(roots, query = "oat", workingOnly = true)
        assertEquals(listOf("buy milk", "oat milk"), flatIds(filtered))
    }

    @Test
    fun unrelatedWorkingItemsStillPrunedByQuery() {
        val roots = buildNestedTree(
            listOf(
                item("project", null, 0),
                item("task", "project", 0, priority = TaskPriority.High),
                item("unrelated", null, 1, priority = TaskPriority.Medium),
            )
        )
        val filtered = filterOutlineRoots(roots, query = "project", workingOnly = true)
        assertEquals(listOf("project", "task"), flatIds(filtered))
    }
}
