package com.checkit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.checkit.domain.DailyPlanScheduleReminderPolicy
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DailyPlanScheduleReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {
    private val policy: DailyPlanScheduleReminderPolicy by inject()
    private val scheduler: DailyPlanScheduleReminderScheduler by inject()

    override suspend fun doWork(): Result {
        return try {
            val itemId = inputData.getLong(InputItemId, -1L).takeIf { it > 0L } ?: return Result.failure()
            val dateEpochDays = inputData.getInt(InputDateEpochDays, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
                ?: return Result.failure()
            val timeMinutes = inputData.getInt(InputTimeMinutes, -1).takeIf { it in 0 until MinutesPerDay }
                ?: return Result.failure()
            val title = inputData.getString(InputTitle).orEmpty().ifBlank { "My Day item" }

            if (policy.shouldShowReminder(dateEpochDays, itemId, timeMinutes)) {
                CheckItNotificationCenter(applicationContext).showDailyPlanScheduleReminder(
                    itemId = itemId,
                    title = title
                )
            }
            scheduler.rescheduleNext(afterTimeMinutes = timeMinutes + 1)
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val WorkName = "daily-plan-schedule-reminder"
        const val InputItemId = "item_id"
        const val InputTitle = "title"
        const val InputDateEpochDays = "date_epoch_days"
        const val InputTimeMinutes = "time_minutes"
        private const val MinutesPerDay = 24 * 60
    }
}
