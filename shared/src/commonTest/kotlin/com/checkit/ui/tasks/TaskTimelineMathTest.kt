package com.checkit.ui.tasks

import com.checkit.domain.TaskItem
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskTimelineMathTest {
    @Test
    fun snapToQuarterHourRoundsToNearestFifteenMinuteBlock() {
        assertEquals(0, 7.snapToQuarterHour())
        assertEquals(15, 8.snapToQuarterHour())
        assertEquals(15, 22.snapToQuarterHour())
        assertEquals(30, 23.snapToQuarterHour())
        assertEquals(-15, (-8).snapToQuarterHour())
    }

    @Test
    fun buildTaskLayoutsPlacesOverlappingTasksInSideBySideLanes() {
        val first = task(id = 1L, start = 9 * 60, end = 10 * 60)
        val second = task(id = 2L, start = 9 * 60 + 30, end = 10 * 60 + 30)
        val third = task(id = 3L, start = 10 * 60 + 30, end = 11 * 60)

        val layouts = buildTaskLayouts(listOf(first, second, third))

        assertEquals(listOf(0, 1, 0), layouts.map { it.lane })
        assertEquals(listOf(2, 2, 1), layouts.map { it.laneCount })
    }

    @Test
    fun buildTaskLayoutsReusesLaneWhenEarlierTaskEnds() {
        val first = task(id = 1L, start = 9 * 60, end = 10 * 60)
        val second = task(id = 2L, start = 10 * 60, end = 11 * 60)

        val layouts = buildTaskLayouts(listOf(first, second))

        assertEquals(listOf(0, 0), layouts.map { it.lane })
        assertEquals(listOf(1, 1), layouts.map { it.laneCount })
    }

    @Test
    fun moveTimelineRangeKeepsDurationAndClampsToDayBounds() {
        assertEquals(
            10 * 60 + 15 to 11 * 60 + 15,
            moveTimelineRange(
                startTimeMinutes = 10 * 60,
                endTimeMinutes = 11 * 60,
                deltaMinutes = 15
            )
        )
        assertEquals(
            0 to 60,
            moveTimelineRange(
                startTimeMinutes = 15,
                endTimeMinutes = 75,
                deltaMinutes = -60
            )
        )
        assertEquals(
            23 * 60 to 24 * 60,
            moveTimelineRange(
                startTimeMinutes = 23 * 60,
                endTimeMinutes = 24 * 60,
                deltaMinutes = 60
            )
        )
    }

    @Test
    fun resizeTimelineStartKeepsEndAndMinimumDuration() {
        assertEquals(
            9 * 60 + 15 to 10 * 60,
            resizeTimelineStart(
                startTimeMinutes = 9 * 60,
                endTimeMinutes = 10 * 60,
                deltaMinutes = 15
            )
        )
        assertEquals(
            9 * 60 + 45 to 10 * 60,
            resizeTimelineStart(
                startTimeMinutes = 9 * 60,
                endTimeMinutes = 10 * 60,
                deltaMinutes = 90
            )
        )
    }

    @Test
    fun resizeTimelineEndKeepsStartAndMinimumDuration() {
        assertEquals(
            9 * 60 to 10 * 60 + 15,
            resizeTimelineEnd(
                startTimeMinutes = 9 * 60,
                endTimeMinutes = 10 * 60,
                deltaMinutes = 15
            )
        )
        assertEquals(
            9 * 60 to 9 * 60 + 15,
            resizeTimelineEnd(
                startTimeMinutes = 9 * 60,
                endTimeMinutes = 10 * 60,
                deltaMinutes = -90
            )
        )
    }

    private fun task(id: Long, start: Int, end: Int) = TaskItem(
        id = id,
        listId = 1L,
        name = "Task $id",
        startTimeMinutes = start,
        endTimeMinutes = end,
        durationMinutes = end - start,
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )
}
