package com.checkit.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.checkit.domain.DailyPlanScheduleReminderPolicy
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.max

class AndroidDailyPlanScheduleReminderScheduler(
    context: Context,
    private val policy: DailyPlanScheduleReminderPolicy
) : DailyPlanScheduleReminderScheduler {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    override suspend fun rescheduleNext(afterTimeMinutes: Int?) {
        val today = LocalDate.now()
        val earliestTimeMinutes = afterTimeMinutes ?: LocalTime.now().let { it.hour * 60 + it.minute }
        val reminder = policy.nextReminderForDate(
            dateEpochDays = today.toEpochDay().toInt(),
            earliestTimeMinutes = earliestTimeMinutes
        )

        if (reminder == null) {
            cancel()
            return
        }

        val request = OneTimeWorkRequestBuilder<DailyPlanScheduleReminderWorker>()
            .setInitialDelay(delayUntilToday(reminder.startTimeMinutes), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    DailyPlanScheduleReminderWorker.InputItemId to reminder.itemId,
                    DailyPlanScheduleReminderWorker.InputTitle to reminder.title,
                    DailyPlanScheduleReminderWorker.InputDateEpochDays to today.toEpochDay().toInt(),
                    DailyPlanScheduleReminderWorker.InputTimeMinutes to reminder.startTimeMinutes
                )
            )
            .build()

        workManager.enqueueUniqueWork(
            DailyPlanScheduleReminderWorker.WorkName,
            ExistingWorkPolicy.REPLACE,
            request
        )
        Logger.d("Scheduled My Day item reminder for ${reminder.title} at ${reminder.startTimeMinutes/60}")
    }

    override suspend fun cancel() {
        workManager.cancelUniqueWork(DailyPlanScheduleReminderWorker.WorkName)
    }

    private fun delayUntilToday(timeMinutes: Int): Long {
        val now = LocalDateTime.now()
        val target = now.with(LocalTime.of(timeMinutes / 60, timeMinutes % 60, 0, 0))
        return max(0L, Duration.between(now, target).toMillis())
    }
}
