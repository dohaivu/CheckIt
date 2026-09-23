package com.checkit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.checkit.ui.MinutesPerDay
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.max

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

        Logger.d("RoutineReminderWorker starting: routineId=$routineId, time=$reminderMinutes")

        return try {
            CheckItNotificationCenter(applicationContext).showRoutineReminder(
                routineId = routineId,
                title = title,
                stepCount = stepCount
            )
            scheduleNext(applicationContext, routineId, title, reminderMinutes, stepCount)
            Result.success()
        } catch (e: Exception) {
            Logger.e("RoutineReminderWorker failed for routineId=$routineId", e)
            // Keep the daily chain alive even when showing fails.
            runCatching { scheduleNext(applicationContext, routineId, title, reminderMinutes, stepCount) }
            Result.retry()
        }
    }

    companion object {
        const val InputRoutineId = "routine_id"
        const val InputTitle = "title"
        const val InputReminderMinutes = "reminder_minutes"
        const val InputStepCount = "step_count"

        fun workName(routineId: String): String = "routine-reminder-$routineId"

        fun scheduleNext(
            context: Context,
            routineId: String,
            title: String,
            reminderMinutes: Int,
            stepCount: Int
        ) {
            scheduleAt(
                context = context,
                routineId = routineId,
                title = title,
                reminderMinutes = reminderMinutes,
                stepCount = stepCount,
                delayMillis = delayUntilTomorrow(reminderMinutes)
            )
        }

        fun scheduleAt(
            context: Context,
            routineId: String,
            title: String,
            reminderMinutes: Int,
            stepCount: Int,
            delayMillis: Long
        ) {
            val request = OneTimeWorkRequestBuilder<RoutineReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        InputRoutineId to routineId,
                        InputTitle to title,
                        InputReminderMinutes to reminderMinutes,
                        InputStepCount to stepCount
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

        /** Next occurrence today if still ahead, otherwise tomorrow. */
        fun delayUntilNext(reminderMinutes: Int): Long {
            val now = LocalDateTime.now()
            var target = now.with(LocalTime.of(reminderMinutes / 60, reminderMinutes % 60))
            if (!target.isAfter(now)) {
                target = target.plusDays(1)
            }
            return max(0L, Duration.between(now, target).toMillis())
        }

        private fun delayUntilTomorrow(reminderMinutes: Int): Long {
            val now = LocalDateTime.now()
            val target = now
                .plusDays(1)
                .with(LocalTime.of(reminderMinutes / 60, reminderMinutes % 60))
            return max(0L, Duration.between(now, target).toMillis())
        }

        private const val WorkTag = "routine-reminder"
    }
}
