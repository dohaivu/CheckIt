package com.checkit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.checkit.domain.CheckInDecision
import com.checkit.domain.CheckInReminderPolicy
import com.checkit.domain.NotificationMessage
import com.checkit.domain.SprintManager
import com.checkit.domain.SprintState
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
    private val sprintManager: SprintManager by inject()

    override suspend fun doWork(): Result {
        return try {
            executeCheckInReminder(
                appContext = applicationContext,
                policy = checkInReminderPolicy,
                sprintActive = sprintManager.state.value is SprintState.Running,
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
    private val policy: CheckInReminderPolicy,
    private val sprintManager: SprintManager
) : CheckInReminderForceRunner {
    private val appContext = context.applicationContext

    override suspend fun forceRun(): String =
        executeCheckInReminder(
            appContext = appContext,
            policy = policy,
            sprintActive = sprintManager.state.value is SprintState.Running,
            force = true
        )
}

/**
 * Single code path for both the periodic [CheckInReminderWorker] and manual
 * force-runs. Evaluates the policy, shows the notification when due, and
 * returns a human-readable outcome for logging and dev UI.
 */
internal suspend fun executeCheckInReminder(
    appContext: Context,
    policy: CheckInReminderPolicy,
    sprintActive: Boolean,
    force: Boolean
): String {
    if (!force && sprintActive) {
        return "Suppressed: sprint in progress"
    }
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
    val currentItem = decision.currentItem?.takeIf { it.title.isNotBlank() }
    val message = if (currentItem != null) {
        NotificationMessage.currentItemCheckIn(currentItem.title, decision.idleMinutes)
    } else {
        NotificationMessage.idleCheckIn(decision.idleMinutes)
    }
    CheckItNotificationCenter(appContext).showCheckInReminder(
        title = message.title,
        body = message.body,
        dailyPlanItemId = currentItem?.id
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
