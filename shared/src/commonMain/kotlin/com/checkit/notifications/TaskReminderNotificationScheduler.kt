package com.checkit.notifications

data class ScheduledTaskReminder(
    val taskId: Long,
    val taskName: String,
    val remindAtMillis: Long,
    val label: String
)

interface TaskReminderNotificationScheduler {
    suspend fun scheduleTaskReminders(taskId: Long, reminders: List<ScheduledTaskReminder>)
    suspend fun cancelTaskReminders(taskId: Long)
}

class NoOpTaskReminderNotificationScheduler : TaskReminderNotificationScheduler {
    override suspend fun scheduleTaskReminders(taskId: Long, reminders: List<ScheduledTaskReminder>) = Unit
    override suspend fun cancelTaskReminders(taskId: Long) = Unit
}
