package com.checkit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.checkit.domain.CheckInDecision
import com.checkit.domain.CheckInReminderPolicy
import com.checkit.domain.NotificationMessage
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
            executeCheckInReminder(
                appContext = applicationContext,
                policy = checkInReminderPolicy,
                force = inputData.getBoolean(InputForceRun, false)
            )
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val WorkName = "check-in-reminder"
        const val InputForceRun = "force_run"
    }
}

class AndroidCheckInReminderForceRunner(
    context: Context,
    private val policy: CheckInReminderPolicy
) : CheckInReminderForceRunner {
    private val appContext = context.applicationContext

    override suspend fun forceRun(): String =
        executeCheckInReminder(appContext, policy, force = true)
}

/**
 * Single code path for both the periodic [CheckInReminderWorker] and manual
 * force-runs. Evaluates the policy, shows the notification when due, and
 * returns a human-readable outcome for logging and dev UI.
 */
internal suspend fun executeCheckInReminder(
    appContext: Context,
    policy: CheckInReminderPolicy,
    force: Boolean
): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val time = LocalTime.now()
    val nowMinutes = time.hour * 60 + time.minute
    val dateEpochDays = LocalDate.now().toEpochDay().toInt()
    val decision = policy.evaluate(
        dateEpochDays = dateEpochDays,
        nowMinutes = nowMinutes,
        nowMillis = now,
        force = force
    )
    if (!decision.shouldShow) {
        return describeSuppressed(decision)
    }
    val currentTitle = decision.currentItem?.title?.takeIf { it.isNotBlank() }
    val message = if (currentTitle != null) {
        NotificationMessage.currentItemCheckIn(currentTitle, decision.idleMinutes)
    } else {
        NotificationMessage.idleCheckIn(decision.idleMinutes)
    }
    CheckItNotificationCenter(appContext).showAppReminder(
        notificationId = NotificationIds.CheckInReminder,
        title = message.title,
        body = message.body,
        type = AppReminderType.CheckIn
    )
    policy.markReminderShown(now)
    return "Shown: ${message.title}"
}

private fun describeSuppressed(decision: CheckInDecision): String {
    val idleMinutes = decision.idleMinutes
    return if (idleMinutes != null) {
        "Suppressed: last Done ${idleMinutes}m ago (below threshold)"
    } else {
        "Suppressed: reminder disabled or morning guard (nothing Done before 09:00)"
    }
}
