package com.checkit.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlin.math.max
import kotlin.time.Clock
import java.util.concurrent.TimeUnit

class AndroidTaskReminderNotificationScheduler(
    context: Context
) : TaskReminderNotificationScheduler {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    override suspend fun scheduleTaskReminders(taskId: Long, reminders: List<ScheduledTaskReminder>) {
        cancelTaskReminders(taskId)
        reminders
            .filter { it.remindAtMillis > Clock.System.now().toEpochMilliseconds() }
            .forEach { reminder ->
                val delayMillis = max(0L, reminder.remindAtMillis - Clock.System.now().toEpochMilliseconds())
                val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            TaskReminderWorker.InputTaskId to reminder.taskId,
                            TaskReminderWorker.InputTaskName to reminder.taskName,
                            TaskReminderWorker.InputLabel to reminder.label
                        )
                    )
                    .addTag(taskTag(taskId))
                    .build()

                workManager.enqueueUniqueWork(
                    workName(taskId, reminder.remindAtMillis),
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
    }

    override suspend fun cancelTaskReminders(taskId: Long) {
        workManager.cancelAllWorkByTag(taskTag(taskId))
    }

    private fun taskTag(taskId: Long): String = "task-reminder-task-$taskId"
    private fun workName(taskId: Long, remindAtMillis: Long): String = "task-reminder-$taskId-$remindAtMillis"
}
