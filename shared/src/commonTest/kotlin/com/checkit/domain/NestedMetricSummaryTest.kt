package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NestedMetricSummaryTest {
    private fun item(
        id: String,
        parentId: String? = null,
        position: Int = 0,
        checked: Boolean = false,
        checkboxEnabled: Boolean = false,
        actualMinutes: Int = 0,
        policy: MetricRollupPolicy = MetricRollupPolicy.IncludeChildren
    ) = NestedListItem(
        id = id,
        documentId = "1",
        parentId = parentId,
        position = position,
        text = "Item $id",
        checkboxEnabled = checkboxEnabled,
        checked = checked,
        actualMinutes = actualMinutes,
        metricRollupPolicy = policy,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    @Test
    fun countsAlwaysIncludeTheWholeSubtree() {
        val tree = buildNestedTree(
            listOf(
                item("1", actualMinutes = 10),
                item("2", parentId = "1", checked = true, checkboxEnabled = true, actualMinutes = 20),
                item("3", parentId = "1", checkboxEnabled = true, policy = MetricRollupPolicy.ExcludeFromParent)
            )
        )

        assertEquals(
            NestedMetricSummary(doneTaskCount = 1, totalTaskCount = 2, trackedMinutes = 30),
            calculateNestedMetricSummaries(tree)["1"]
        )
    }

    @Test
    fun ownOnlyDoesNotIncludeChildTimeButStillCountsChildren() {
        val tree = buildNestedTree(
            listOf(
                item("1", actualMinutes = 10, policy = MetricRollupPolicy.OwnOnly),
                item("2", parentId = "1", actualMinutes = 20)
            )
        )

        assertEquals(
            NestedMetricSummary(doneTaskCount = 0, totalTaskCount = 0, trackedMinutes = 10),
            calculateNestedMetricSummaries(tree)["1"]
        )
    }

    @Test
    fun excludedChildStillHasOwnSummaryButDoesNotContributeToParentTime() {
        val tree = buildNestedTree(
            listOf(
                item("1", actualMinutes = 10),
                item("2", parentId = "1", actualMinutes = 20, policy = MetricRollupPolicy.ExcludeFromParent),
                item("3", parentId = "2", actualMinutes = 30)
            )
        )
        val summaries = calculateNestedMetricSummaries(tree)

        assertEquals(50, summaries["2"]?.trackedMinutes)
        assertEquals(10, summaries["1"]?.trackedMinutes)
        assertEquals(0, summaries["1"]?.doneTaskCount)
        assertEquals(0, summaries["1"]?.totalTaskCount)
    }

    @Test
    fun checkedWithoutCheckboxIsNeitherDoneNorTotal() {
        val tree = buildNestedTree(
            listOf(
                item("1"),
                item("2", parentId = "1", checked = true),
                item("3", parentId = "1", checkboxEnabled = true),
                item("4", parentId = "1", checkboxEnabled = true, checked = true)
            )
        )
        val summary = calculateNestedMetricSummaries(tree)["1"]

        assertEquals(1, summary?.doneTaskCount)
        assertEquals(2, summary?.totalTaskCount)
    }
}
