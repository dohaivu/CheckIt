package com.checkit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.checkit.domain.CheckInReminderPolicy
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Clock

class CheckInReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {
    private val checkInReminderPolicy: CheckInReminderPolicy by inject()

    override suspend fun doWork(): Result {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val time = LocalTime.now()
            val nowMinutes = time.hour * 60 + time.minute
            if (
                checkInReminderPolicy.shouldShowReminder(
                    dateEpochDays = LocalDate.now().toEpochDay().toInt(),
                    nowMinutes = nowMinutes,
                    nowMillis = now
                )
            ) {
                CheckItNotificationCenter(applicationContext).showAppReminder(
                    notificationId = NotificationIds.CheckInReminder,
                    title = "Check in",
                    body = "Nothing is in My Day around now. Add or adjust your plan?",
                    type = AppReminderType.CheckIn
                )
                checkInReminderPolicy.markReminderShown(now)
            }
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val WorkName = "check-in-reminder"
    }
}
