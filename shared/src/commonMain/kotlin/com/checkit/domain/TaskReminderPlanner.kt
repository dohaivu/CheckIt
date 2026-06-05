package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

enum class TaskReminderPreset(
    val offsetMinutes: Int,
    val label: String
) {
    AtTime(0, "At time of event"),
    TenMinutesBefore(10, "10 mins before"),
    OneHourBefore(60, "1 hour before"),
    OneDayBefore(24 * 60, "1 day before");

    companion object {
        val default: TaskReminderPreset = TenMinutesBefore
        val offsets: Set<Int> = entries.map { it.offsetMinutes }.toSet()

        fun labelFor(offsetMinutes: Int): String =
            entries.firstOrNull { it.offsetMinutes == offsetMinutes }?.label ?: "${offsetMinutes} mins before"
    }
}

data class TaskReminderWriteInput(
    val offsetMinutes: Int,
    val remindAtMillis: Long,
    val label: String
)

object TaskReminderPlanner {
    private const val DefaultReminderHour = 9

    fun eventTimeMillis(
        dueDate: LocalDate?,
        startTimeMinutes: Int?,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Long? {
        val date = dueDate ?: return null
        val minutes = startTimeMinutes ?: DefaultReminderHour * 60
        return LocalDateTime(
            date = date,
            time = LocalTime(hour = minutes / 60, minute = minutes % 60)
        ).toInstant(timeZone).toEpochMilliseconds()
    }

    fun buildReminderInputs(
        dueDate: LocalDate?,
        startTimeMinutes: Int?,
        selectedOffsets: Set<Int>,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<TaskReminderWriteInput> {
        val eventMillis = eventTimeMillis(dueDate, startTimeMinutes, timeZone) ?: return emptyList()
        return selectedOffsets
            .filter { it >= 0 }
            .distinct()
            .sorted()
            .map { offsetMinutes ->
                TaskReminderWriteInput(
                    offsetMinutes = offsetMinutes,
                    remindAtMillis = eventMillis - offsetMinutes * 60_000L,
                    label = TaskReminderPreset.labelFor(offsetMinutes)
                )
            }
    }

    fun selectedOffsetsFor(task: TaskItem, timeZone: TimeZone = TimeZone.currentSystemDefault()): Set<Int> {
        val eventMillis = eventTimeMillis(task.dueDate, task.startTimeMinutes, timeZone) ?: return emptySet()
        return task.reminders.mapNotNull { reminder ->
            val offsetMillis = eventMillis - reminder.remindAtMillis
            if (offsetMillis < 0 || offsetMillis % 60_000L != 0L) {
                null
            } else {
                val offsetMinutes = (offsetMillis / 60_000L).toInt()
                offsetMinutes.takeIf { it in TaskReminderPreset.offsets }
            }
        }.toSet()
    }
}
