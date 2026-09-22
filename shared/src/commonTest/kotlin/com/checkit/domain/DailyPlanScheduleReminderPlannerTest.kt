package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyPlanScheduleReminderPlannerTest {
    @Test
    fun nextReminderUsesEarliestPlannedTimedItem() {
        val reminder = DailyPlanScheduleReminderPlanner.nextReminder(
            items = listOf(
                item(id = "1", title = "Later", startTimeMinutes = 11 * 60),
                item(id = "2", title = "Soon", startTimeMinutes = 9 * 60),
                item(id = "3", title = "Past", startTimeMinutes = 8 * 60)
            ),
            earliestTimeMinutes = 8 * 60 + 30
        )

        assertEquals(
            DailyPlanScheduleReminder(itemId = "2", title = "Soon", startTimeMinutes = 9 * 60),
            reminder
        )
    }

    @Test
    fun nextReminderSkipsDoneAndUntimedItems() {
        val reminder = DailyPlanScheduleReminderPlanner.nextReminder(
            items = listOf(
                item(id = "1", title = "Done", status = DailyPlanItemStatus.Done, startTimeMinutes = 9 * 60),
                item(id = "2", title = "Untimed", startTimeMinutes = null),
                item(id = "3", title = "Planned", startTimeMinutes = 10 * 60)
            ),
            earliestTimeMinutes = 9 * 60
        )

        assertEquals(
            DailyPlanScheduleReminder(itemId = "3", title = "Planned", startTimeMinutes = 10 * 60),
            reminder
        )
    }

    @Test
    fun nextReminderSkipsItemsAtEarliestTime() {
        val reminder = DailyPlanScheduleReminderPlanner.nextReminder(
            items = listOf(
                item(id = "1", title = "Now", startTimeMinutes = 9 * 60),
                item(id = "2", title = "Next", startTimeMinutes = 9 * 60 + 1)
            ),
            earliestTimeMinutes = 9 * 60
        )

        assertEquals(
            DailyPlanScheduleReminder(itemId = "2", title = "Next", startTimeMinutes = 9 * 60 + 1),
            reminder
        )
    }

    @Test
    fun nextReminderReturnsNullAfterEndOfDay() {
        val reminder = DailyPlanScheduleReminderPlanner.nextReminder(
            items = listOf(item(id = "1", title = "Late", startTimeMinutes = 23 * 60 + 59)),
            earliestTimeMinutes = 24 * 60
        )

        assertNull(reminder)
    }

    private fun item(
        id: String,
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
