package com.checkit.notifications

data class ScheduledTaskReminder(
    val taskId: String,
    val taskName: String,
    val remindAtMillis: Long,
    val label: String
)

interface TaskReminderNotificationScheduler {
    suspend fun scheduleTaskReminders(taskId: String, reminders: List<ScheduledTaskReminder>)
    suspend fun cancelTaskReminders(taskId: String)
}

class NoOpTaskReminderNotificationScheduler : TaskReminderNotificationScheduler {
    override suspend fun scheduleTaskReminders(taskId: String, reminders: List<ScheduledTaskReminder>) = Unit
    override suspend fun cancelTaskReminders(taskId: String) = Unit
}
