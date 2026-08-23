package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FloatingQuickAddBarTest {

    private fun item(
        id: Long,
        startTimeMinutes: Int?,
        endTimeMinutes: Int? = null
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = 0,
        title = "Item $id",
        source = DailyPlanItemSource.MyDayTask,
        status = DailyPlanItemStatus.Planned,
        tags = emptyList(),
        sortOrder = 0,
        startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes,
        addedAtMillis = 0L
    )

    @Test
    fun noItemsMeansNothingNearby() {
        assertFalse(hasDailyPlanItemNearby(emptyList(), nowMinutes = 600))
    }

    @Test
    fun itemOverlappingNowIsNearby() {
        val items = listOf(item(1L, startTimeMinutes = 590, endTimeMinutes = 650))
        assertTrue(hasDailyPlanItemNearby(items, nowMinutes = 600))
    }

    @Test
    fun itemStartingWithinWindowIsNearby() {
        // Starts 20 minutes from now, within the default ±30 window.
        val items = listOf(item(1L, startTimeMinutes = 620))
        assertTrue(hasDailyPlanItemNearby(items, nowMinutes = 600))
    }

    @Test
    fun itemEndingJustOutsideWindowIsNotNearby() {
        val items = listOf(item(1L, startTimeMinutes = 500, endTimeMinutes = 560))
        assertFalse(hasDailyPlanItemNearby(items, nowMinutes = 600))
    }

    @Test
    fun itemStartingJustOutsideWindowIsNotNearby() {
        val items = listOf(item(1L, startTimeMinutes = 650, endTimeMinutes = 700))
        assertFalse(hasDailyPlanItemNearby(items, nowMinutes = 600))
    }

    @Test
    fun itemWithoutStartTimeNeverCountsAsNearby() {
        val items = listOf(item(1L, startTimeMinutes = null))
        assertFalse(hasDailyPlanItemNearby(items, nowMinutes = 600))
    }

    @Test
    fun itemWithoutEndTimeTreatedAsInstantAtStart() {
        assertTrue(hasDailyPlanItemNearby(listOf(item(1L, startTimeMinutes = 610)), nowMinutes = 600))
        assertFalse(hasDailyPlanItemNearby(listOf(item(2L, startTimeMinutes = 700)), nowMinutes = 600))
    }

    @Test
    fun customWindowRespected() {
        val items = listOf(item(1L, startTimeMinutes = 700, endTimeMinutes = 750))
        assertFalse(hasDailyPlanItemNearby(items, nowMinutes = 600, windowMinutes = 30))
        assertTrue(hasDailyPlanItemNearby(items, nowMinutes = 600, windowMinutes = 120))
    }
}
