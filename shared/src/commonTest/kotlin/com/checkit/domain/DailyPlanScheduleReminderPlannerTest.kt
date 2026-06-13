package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyPlanScheduleReminderPlannerTest {
    @Test
    fun nextReminderUsesEarliestPlannedTimedItem() {
        val reminder = DailyPlanScheduleReminderPlanner.nextReminder(
            items = listOf(
                item(id = 1L, title = "Later", startTimeMinutes = 11 * 60),
                item(id = 2L, title = "Soon", startTimeMinutes = 9 * 60),
                item(id = 3L, title = "Past", startTimeMinutes = 8 * 60)
            ),
            earliestTimeMinutes = 8 * 60 + 30
        )

        assertEquals(
            DailyPlanScheduleReminder(itemId = 2L, title = "Soon", startTimeMinutes = 9 * 60),
            reminder
        )
    }

    @Test
    fun nextReminderSkipsDoneAndUntimedItems() {
        val reminder = DailyPlanScheduleReminderPlanner.nextReminder(
            items = listOf(
                item(id = 1L, title = "Done", status = DailyPlanItemStatus.Done, startTimeMinutes = 9 * 60),
                item(id = 2L, title = "Untimed", startTimeMinutes = null),
                item(id = 3L, title = "Planned", startTimeMinutes = 10 * 60)
            ),
            earliestTimeMinutes = 9 * 60
        )

        assertEquals(
            DailyPlanScheduleReminder(itemId = 3L, title = "Planned", startTimeMinutes = 10 * 60),
            reminder
        )
    }

    @Test
    fun nextReminderReturnsNullAfterEndOfDay() {
        val reminder = DailyPlanScheduleReminderPlanner.nextReminder(
            items = listOf(item(id = 1L, title = "Late", startTimeMinutes = 23 * 60 + 59)),
            earliestTimeMinutes = 24 * 60
        )

        assertNull(reminder)
    }

    private fun item(
        id: Long,
        title: String,
        status: DailyPlanItemStatus = DailyPlanItemStatus.Planned,
        startTimeMinutes: Int?
    ) = DailyPlanScheduleReminderItem(
        id = id,
        title = title,
        status = status,
        startTimeMinutes = startTimeMinutes
    )
}
