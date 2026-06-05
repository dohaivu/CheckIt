package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

enum class TaskReminderPreset(
    val offsetMinutes: Int,
    val timedLabel: String,
    val allDayLabel: String? = null
) {
    AtTime(0, "At time of event", "On the day at 9 AM"),
    TenMinutesBefore(10, "10 mins before"),
    OneHourBefore(60, "1 hour before"),
    OneDayBefore(24 * 60, "1 day before", "The day before at 9 AM"),
    TwoDaysBefore(2 * 24 * 60, "2 days before", "2 days before at 9 AM"),
    OneWeekBefore(7 * 24 * 60, "1 week before", "1 week before at 9 AM");

    val label: String get() = timedLabel

    companion object {
        val timedDefault: TaskReminderPreset = TenMinutesBefore
        val allDayDefault: TaskReminderPreset = AtTime
        val offsets: Set<Int> = entries.map { it.offsetMinutes }.toSet()

        fun availableFor(startTimeMinutes: Int?): List<TaskReminderPreset> =
            if (startTimeMinutes == null) {
                entries.filter { it.allDayLabel != null }
            } else {
                listOf(AtTime, TenMinutesBefore, OneHourBefore, OneDayBefore)
            }

        fun defaultFor(startTimeMinutes: Int?): TaskReminderPreset =
            if (startTimeMinutes == null) allDayDefault else timedDefault

        fun normalizeOffsets(startTimeMinutes: Int?, offsets: Set<Int>): Set<Int> {
            if (offsets.isEmpty()) return emptySet()
            val availableOffsets = availableFor(startTimeMinutes).map { it.offsetMinutes }.toSet()
            val kept = offsets.intersect(availableOffsets)
            return kept.ifEmpty { setOf(defaultFor(startTimeMinutes).offsetMinutes) }
        }

        fun labelFor(offsetMinutes: Int, startTimeMinutes: Int?): String =
            entries.firstOrNull { it.offsetMinutes == offsetMinutes }?.let { preset ->
                if (startTimeMinutes == null) preset.allDayLabel ?: preset.timedLabel else preset.timedLabel
            } ?: "${offsetMinutes} mins before"
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
                    label = TaskReminderPreset.labelFor(offsetMinutes, startTimeMinutes)
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
                offsetMinutes.takeIf { it in TaskReminderPreset.availableFor(task.startTimeMinutes).map { preset -> preset.offsetMinutes } }
            }
        }.toSet()
    }
}
