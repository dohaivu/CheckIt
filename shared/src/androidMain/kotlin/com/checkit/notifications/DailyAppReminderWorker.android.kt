package com.checkit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.max

class DailyAppReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val type = inputData.getString(InputType).orEmpty()
        val timeMinutes = inputData.getInt(InputTimeMinutes, -1).takeIf { it in 0 until MinutesPerDay } ?: return Result.failure()
        val title = inputData.getString(InputTitle).orEmpty()
        val body = inputData.getString(InputBody).orEmpty()

        println("DailyAppReminderWorker: Running for type=$type at time=$timeMinutes")

        CheckItNotificationCenter(applicationContext).showAppReminder(
            notificationId = notificationId(type),
            title = title,
            body = body
        )
        scheduleNext(applicationContext, type, timeMinutes, title, body)
        return Result.success()
    }

    companion object {
        const val TypePlan = "plan"
        const val TypeReview = "review"
        const val InputType = "type"
        const val InputTimeMinutes = "time_minutes"
        const val InputTitle = "title"
        const val InputBody = "body"
        private const val MinutesPerDay = 24 * 60

        fun workName(type: String): String = "app-reminder-$type"

        fun scheduleNext(context: Context, type: String, timeMinutes: Int, title: String, body: String) {
            val request = OneTimeWorkRequestBuilder<DailyAppReminderWorker>()
                .setInitialDelay(delayUntilTomorrow(timeMinutes), TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        InputType to type,
                        InputTimeMinutes to timeMinutes,
                        InputTitle to title,
                        InputBody to body,
                    )
                )
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                workName(type),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun delayUntilTomorrow(timeMinutes: Int): Long {
            val now = LocalDateTime.now()
            val target = now
                .plusDays(1)
                .with(LocalTime.of(timeMinutes / 60, timeMinutes % 60))
            return max(0L, Duration.between(now, target).toMillis())
        }

        private fun notificationId(type: String): Int = when (type) {
            TypePlan -> 70_001
            TypeReview -> 70_002
            else -> 70_000
        }
    }
}
