package com.checkit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TaskReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(InputTaskId, -1L).takeIf { it > 0L } ?: return Result.failure()
        val taskName = inputData.getString(InputTaskName).orEmpty()
        val label = inputData.getString(InputLabel).orEmpty()

        CheckItNotificationCenter(applicationContext).showTaskReminder(
            taskId = taskId,
            taskName = taskName,
            label = label
        )
        return Result.success()
    }

    companion object {
        const val InputTaskId = "task_id"
        const val InputTaskName = "task_name"
        const val InputLabel = "label"
    }
}
