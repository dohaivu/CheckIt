package com.checkit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.checkit.domain.decodeActiveWeekdays
import com.checkit.domain.encodeActiveWeekdays
import com.checkit.ui.MinutesPerDay
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlinx.datetime.DayOfWeek

/**
 * Daily repeating worker for one routine. Fires at [InputReminderMinutes],
 * shows the notification, then chains the next occurrence for tomorrow.
 * The chain lives in a unique-work slot per routine, so re-saving the
 * routine (or a stale chain) simply replaces it — never duplicates.
 */
class RoutineReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val routineId = inputData.getString(InputRoutineId)?.takeIf { it.isNotEmpty() }
            ?: return Result.failure()
        val title = inputData.getString(InputTitle).orEmpty()
        val reminderMinutesValue = inputData.getInt(InputReminderMinutes, -1)
        val reminderMinutes = reminderMinutesValue.takeIf { it in 0 until MinutesPerDay }
            ?: return Result.failure()
        val stepCount = inputData.getInt(InputStepCount, 0).coerceAtLeast(0)
        val activeWeekdays = decodeActiveWeekdays(inputData.getString(InputActiveWeekdays))

        Logger.d("RoutineReminderWorker starting: routineId=$routineId, time=$reminderMinutes")

        return try {
            if (isScheduledToday(activeWeekdays)) {
                CheckItNotificationCenter(applicationContext).showRoutineReminder(
                    routineId = routineId,
                    title = title,
                    stepCount = stepCount
                )
            }
            scheduleNext(applicationContext, routineId, title, reminderMinutes, stepCount, activeWeekdays)
            Result.success()
        } catch (e: Exception) {
            Logger.e("RoutineReminderWorker failed for routineId=$routineId", e)
            // Keep the daily chain alive even when showing fails.
            runCatching {
                scheduleNext(applicationContext, routineId, title, reminderMinutes, stepCount, activeWeekdays)
            }
            Result.retry()
        }
    }

    companion object {
        const val InputRoutineId = "routine_id"
        const val InputTitle = "title"
        const val InputReminderMinutes = "reminder_minutes"
        const val InputStepCount = "step_count"
        const val InputActiveWeekdays = "active_weekdays"

        fun workName(routineId: String): String = "routine-reminder-$routineId"

        fun scheduleNext(
            context: Context,
            routineId: String,
            title: String,
            reminderMinutes: Int,
            stepCount: Int,
            activeWeekdays: Set<DayOfWeek>
        ) {
            scheduleAt(
                context = context,
                routineId = routineId,
                title = title,
                reminderMinutes = reminderMinutes,
                stepCount = stepCount,
                activeWeekdays = activeWeekdays,
                delayMillis = delayUntilNext(reminderMinutes, activeWeekdays)
            )
        }

        fun scheduleAt(
            context: Context,
            routineId: String,
            title: String,
            reminderMinutes: Int,
            stepCount: Int,
            activeWeekdays: Set<DayOfWeek>,
            delayMillis: Long
        ) {
            val request = OneTimeWorkRequestBuilder<RoutineReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        InputRoutineId to routineId,
                        InputTitle to title,
                        InputReminderMinutes to reminderMinutes,
                        InputStepCount to stepCount,
                        InputActiveWeekdays to encodeActiveWeekdays(activeWeekdays)
                    )
                )
                .addTag(WorkTag)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                workName(routineId),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun isScheduledToday(activeWeekdays: Set<DayOfWeek>): Boolean {
            // java.time and kotlinx.datetime DayOfWeek share the same constant names.
            val todayName = LocalDateTime.now().dayOfWeek.name
            return activeWeekdays.any { it.name == todayName }
        }

        /**
         * Next scheduled occurrence strictly after now: today if its time is
         * still ahead and scheduled, otherwise the next scheduled day.
         */
        fun delayUntilNext(
            reminderMinutes: Int,
            activeWeekdays: Set<DayOfWeek>
        ): Long {
            val now = LocalDateTime.now()
            val time = LocalTime.of(reminderMinutes / 60, reminderMinutes % 60)
            var date = now.toLocalDate()
            repeat(8) {
                // java.time and kotlinx.datetime DayOfWeek share the same constant names.
                val scheduled = activeWeekdays.any { it.name == date.dayOfWeek.name }
                val target = LocalDateTime.of(date, time)
                if (scheduled && target.isAfter(now)) {
                    return max(0L, Duration.between(now, target).toMillis())
                }
                date = date.plusDays(1)
            }
            // Unreachable with a non-empty schedule; fall back to +24h.
            return Duration.ofDays(1).toMillis()
        }

        private const val WorkTag = "routine-reminder"
    }
}
