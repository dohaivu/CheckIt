package com.checkit.notifications

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.checkit.data.UserSettings
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.max

class AndroidAppReminderScheduler(
    context: Context,
    private val dailyPlanScheduleReminderScheduler: DailyPlanScheduleReminderScheduler
) : AppReminderScheduler {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    private var lastPlanTime: Int? = null
    private var lastReviewTime: Int? = null
    private var lastCheckInEnabled: Boolean? = null
    private var lastScheduleEnabled: Boolean? = null

    override suspend fun applySettings(settings: UserSettings) {
        println("AndroidAppReminderScheduler: Applying settings: plan=${settings.planReminderEnabled}, review=${settings.reviewReminderEnabled}, checkIn=${settings.checkInReminderEnabled}")
        val planWorkName = DailyAppReminderWorker.workName(DailyAppReminderWorker.TypePlan)
        if (settings.planReminderEnabled) {
            if (lastPlanTime != settings.planReminderTimeMinutes) {
                scheduleDailyReminder(
                    type = DailyAppReminderWorker.TypePlan,
                    timeMinutes = settings.planReminderTimeMinutes,
                    title = "Plan your day",
                    body = "Open My Day and choose what matters for today."
                )
                lastPlanTime = settings.planReminderTimeMinutes
            }
        } else {
            workManager.cancelUniqueWork(planWorkName)
            lastPlanTime = null
        }

        val reviewWorkName = DailyAppReminderWorker.workName(DailyAppReminderWorker.TypeReview)
        if (settings.reviewReminderEnabled) {
            if (lastReviewTime != settings.reviewReminderTimeMinutes) {
                scheduleDailyReminder(
                    type = DailyAppReminderWorker.TypeReview,
                    timeMinutes = settings.reviewReminderTimeMinutes,
                    title = "Review your day",
                    body = "Take a minute to close out today in My Day."
                )
                lastReviewTime = settings.reviewReminderTimeMinutes
            }
        } else {
            workManager.cancelUniqueWork(reviewWorkName)
            lastReviewTime = null
        }

        if (settings.checkInReminderEnabled) {
            if (lastCheckInEnabled != true) {
                scheduleCheckInReminder()
                lastCheckInEnabled = true
            }
        } else {
            workManager.cancelUniqueWork(CheckInReminderWorker.WorkName)
            lastCheckInEnabled = false
        }

        if (settings.scheduleReminderEnabled) {
            if (lastScheduleEnabled != true) {
                dailyPlanScheduleReminderScheduler.rescheduleNext()
                lastScheduleEnabled = true
            }
        } else {
            dailyPlanScheduleReminderScheduler.cancel()
            lastScheduleEnabled = false
        }
    }

    private fun scheduleDailyReminder(type: String, timeMinutes: Int, title: String, body: String) {
        val workName = DailyAppReminderWorker.workName(type)
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
            workName,
            ExistingWorkPolicy.REPLACE,
            request
        )
        Logger.d("Scheduled daily reminder for $type at $timeMinutes")
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
        var target = now.with(LocalTime.of(normalized / 60, normalized % 60, 0, 0))
        // If the target time is within the next 10 seconds, we treat it as "now" or "just missed" 
        // and schedule for tomorrow to avoid immediate or infinite loops.
        // Otherwise, if it's earlier today, schedule for tomorrow.
        if (target.isBefore(now.plusSeconds(10))) {
            target = target.plusDays(1)
        }
        return max(0L, Duration.between(now, target).toMillis())
    }

    private companion object {
        const val MinutesPerDay = 24 * 60
    }
}
