package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NestedMetricSummaryTest {
    private fun item(
        id: Long,
        parentId: Long? = null,
        position: Int = 0,
        checked: Boolean = false,
        actualMinutes: Int = 0,
        policy: MetricRollupPolicy = MetricRollupPolicy.IncludeChildren
    ) = NestedListItem(
        id = id,
        documentId = 1L,
        parentId = parentId,
        position = position,
        text = "Item $id",
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
                item(1, actualMinutes = 10),
                item(2, parentId = 1, checked = true, actualMinutes = 20),
                item(3, parentId = 1, policy = MetricRollupPolicy.ExcludeFromParent)
            )
        )

        assertEquals(
            NestedMetricSummary(doneItemCount = 1, trackedMinutes = 30),
            calculateNestedMetricSummaries(tree)[1L]
        )
    }

    @Test
    fun ownOnlyDoesNotIncludeChildTimeButStillCountsChildren() {
        val tree = buildNestedTree(
            listOf(
                item(1, actualMinutes = 10, policy = MetricRollupPolicy.OwnOnly),
                item(2, parentId = 1, actualMinutes = 20)
            )
        )

        assertEquals(
            NestedMetricSummary(doneItemCount = 0, trackedMinutes = 10),
            calculateNestedMetricSummaries(tree)[1L]
        )
    }

    @Test
    fun excludedChildStillHasOwnSummaryButDoesNotContributeToParentTime() {
        val tree = buildNestedTree(
            listOf(
                item(1, actualMinutes = 10),
                item(2, parentId = 1, actualMinutes = 20, policy = MetricRollupPolicy.ExcludeFromParent),
                item(3, parentId = 2, actualMinutes = 30)
            )
        )
        val summaries = calculateNestedMetricSummaries(tree)

        assertEquals(50, summaries[2L]?.trackedMinutes)
        assertEquals(10, summaries[1L]?.trackedMinutes)
        assertEquals(0, summaries[1L]?.doneItemCount)
    }
}
