package com.checkit.notifications

data class ScheduledRoutineReminder(
    val routineId: String,
    val title: String,
    val reminderMinutes: Int,
    val stepCount: Int
)

interface RoutineReminderScheduler {
    suspend fun scheduleRoutineReminder(reminder: ScheduledRoutineReminder)
    suspend fun cancelRoutineReminder(routineId: String)
}

class NoOpRoutineReminderScheduler : RoutineReminderScheduler {
    override suspend fun scheduleRoutineReminder(reminder: ScheduledRoutineReminder) = Unit
    override suspend fun cancelRoutineReminder(routineId: String) = Unit
}
