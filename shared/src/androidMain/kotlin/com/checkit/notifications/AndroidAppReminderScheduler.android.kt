package com.checkit.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.checkit.data.UserSettings
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.max

class AndroidAppReminderScheduler(
    context: Context
) : AppReminderScheduler {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    override suspend fun applySettings(settings: UserSettings) {
        if (settings.planReminderEnabled) {
            scheduleDailyReminder(
                type = DailyAppReminderWorker.TypePlan,
                timeMinutes = settings.planReminderTimeMinutes,
                title = "Plan your day",
                body = "Open My Day and choose what matters for today."
            )
        } else {
            workManager.cancelUniqueWork(DailyAppReminderWorker.workName(DailyAppReminderWorker.TypePlan))
        }

        if (settings.reviewReminderEnabled) {
            scheduleDailyReminder(
                type = DailyAppReminderWorker.TypeReview,
                timeMinutes = settings.reviewReminderTimeMinutes,
                title = "Review your day",
                body = "Take a minute to close out today in My Day."
            )
        } else {
            workManager.cancelUniqueWork(DailyAppReminderWorker.workName(DailyAppReminderWorker.TypeReview))
        }

        if (settings.checkInReminderEnabled) {
            scheduleCheckInReminder()
        } else {
            workManager.cancelUniqueWork(CheckInReminderWorker.WorkName)
        }
    }

    private fun scheduleDailyReminder(type: String, timeMinutes: Int, title: String, body: String) {
        val request = OneTimeWorkRequestBuilder<DailyAppReminderWorker>()
            .setInitialDelay(delayUntilNext(timeMinutes), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    DailyAppReminderWorker.InputType to type,
                    DailyAppReminderWorker.InputTimeMinutes to timeMinutes,
                    DailyAppReminderWorker.InputTitle to title,
                    DailyAppReminderWorker.InputBody to body,
                )
            )
            .build()

        workManager.enqueueUniqueWork(
            DailyAppReminderWorker.workName(type),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleCheckInReminder() {
        val request = PeriodicWorkRequestBuilder<CheckInReminderWorker>(30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            CheckInReminderWorker.WorkName,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    private fun delayUntilNext(timeMinutes: Int): Long {
        val normalized = timeMinutes.coerceIn(0, MinutesPerDay - 1)
        val now = LocalDateTime.now()
        var target = now.with(LocalTime.of(normalized / 60, normalized % 60))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return max(0L, Duration.between(now, target).toMillis())
    }

    private companion object {
        const val MinutesPerDay = 24 * 60
    }
}
