package com.checkit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
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
        val timeMinutesValue = inputData.getInt(InputTimeMinutes, -1)
        val timeMinutes = timeMinutesValue.takeIf { it in 0 until MinutesPerDay }
        val title = inputData.getString(InputTitle).orEmpty()
        val body = inputData.getString(InputBody).orEmpty()

        if (timeMinutes == null) {
            Logger.e("DailyAppReminderWorker failed: Invalid timeMinutes=$timeMinutesValue for type=$type")
            return Result.failure()
        }

        Logger.d("DailyAppReminderWorker starting: type=$type, time=$timeMinutes, title='$title'")

        return try {
            CheckItNotificationCenter(applicationContext).showAppReminder(
                notificationId = NotificationIds.appReminder(type),
                title = title,
                body = body,
                type = AppReminderType.fromDailyWorkerType(type)
            )
            scheduleNext(applicationContext, type, timeMinutes, title, body)
            Result.success()
        } catch (e: Exception) {
            Logger.e("DailyAppReminderWorker failed to process type=$type", e)
            // Even if showing notification fails, try to schedule next so the chain doesn't break
            runCatching { scheduleNext(applicationContext, type, timeMinutes, title, body) }
            Result.retry()
        }
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
                ExistingWorkPolicy.APPEND_OR_REPLACE,
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

    }
}
