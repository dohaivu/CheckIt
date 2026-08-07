package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TagItem
import kotlin.test.Test
import kotlin.test.assertEquals

class DayLinearTimelineTest {
    private val workTag = TagItem(id = 1L, name = "Work", color = "#000000", sortOrder = 0)
    private val deepTag = TagItem(id = 2L, name = "Deep", color = "#000000", sortOrder = 1)

    private fun item(
        id: Long,
        tags: List<TagItem>,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = 0,
        title = "Item $id",
        source = DailyPlanItemSource.MyDayTask,
        status = DailyPlanItemStatus.Planned,
        tags = tags,
        sortOrder = 0,
        startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes,
        addedAtMillis = 0L
    )

    @Test
    fun tagTimeTotalsSumsMinutesPerTagAcrossItems() {
        val items = listOf(
            item(1L, listOf(workTag), 60, 120),
            item(2L, listOf(workTag, deepTag), 120, 180),
            item(3L, listOf(deepTag), 180, 210)
        )

        val totals = items.tagTimeTotals()

        assertEquals(2, totals.size)
        assertEquals(workTag.name to 120, totals.first().tag.name to totals.first().minutes)
        assertEquals(deepTag.name to 90, totals.last().tag.name to totals.last().minutes)
    }

    @Test
    fun tagTimeTotalsExcludesItemsWithoutTimeRange() {
        val items = listOf(
            item(1L, listOf(workTag), 60, 120),
            item(2L, listOf(workTag), null, null),
            item(3L, listOf(workTag), 100, 100)
        )

        assertEquals(listOf(60), items.tagTimeTotals().map { it.minutes })
    }

    @Test
    fun tagTimeTotalsSortsByMinutesDescending() {
        val lowTag = TagItem(id = 3L, name = "Low", color = "#000000", sortOrder = 2)
        val items = listOf(
            item(1L, listOf(lowTag), 60, 90),
            item(2L, listOf(workTag), 60, 150),
            item(3L, listOf(deepTag), 120, 180)
        )

        val totals = items.tagTimeTotals()

        assertEquals(listOf(workTag.id, deepTag.id, lowTag.id), totals.map { it.tag.id })
    }
}
