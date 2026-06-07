package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskReminderPlannerTest {
    private val utc = TimeZone.UTC

    @Test
    fun buildReminderInputsUsesTaskStartTimeAndOffsets() {
        val reminders = TaskReminderPlanner.buildReminderInputs(
            doDate = LocalDate(2026, 6, 5),
            startTimeMinutes = 8 * 60 + 30,
            selectedOffsets = setOf(60, 0, 10),
            timeZone = utc
        )

        assertEquals(listOf(0, 10, 60), reminders.map { it.offsetMinutes })
        assertEquals(
            listOf(
                "At time of event",
                "10 mins before",
                "1 hour before"
            ),
            reminders.map { it.label }
        )
        assertEquals(
            listOf(
                1_780_648_200_000L,
                1_780_647_600_000L,
                1_780_644_600_000L
            ),
            reminders.map { it.remindAtMillis }
        )
    }

    @Test
    fun selectedOffsetsForTaskInfersPersistedReminderOffsets() {
        val task = TaskItem(
            id = 1L,
            listId = 1L,
            name = "Review",
            doDate = LocalDate(2026, 6, 5),
            startTimeMinutes = 8 * 60 + 30,
            reminders = listOf(
                TaskReminder(id = 1L, taskId = 1L, remindAtMillis = 1_780_647_600_000L),
                TaskReminder(id = 2L, taskId = 1L, remindAtMillis = 1_780_644_600_000L)
            ),
            sortOrder = 0,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )

        assertEquals(setOf(10, 60), TaskReminderPlanner.selectedOffsetsFor(task, utc))
    }

    @Test
    fun buildReminderInputsUsesAllDayLabelsWhenTaskHasNoStartTime() {
        val reminders = TaskReminderPlanner.buildReminderInputs(
            doDate = LocalDate(2026, 6, 5),
            startTimeMinutes = null,
            selectedOffsets = setOf(0, 24 * 60, 2 * 24 * 60, 7 * 24 * 60),
            timeZone = utc
        )

        assertEquals(
            listOf(
                "On the day at 9 AM",
                "The day before at 9 AM",
                "2 days before at 9 AM",
                "1 week before at 9 AM"
            ),
            reminders.map { it.label }
        )
        assertEquals(1_780_650_000_000L, reminders.first().remindAtMillis)
    }
}
