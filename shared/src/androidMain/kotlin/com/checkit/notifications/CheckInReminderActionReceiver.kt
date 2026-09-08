package com.checkit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.checkit.data.CheckItRepository
import com.checkit.domain.CheckInReminderPolicy
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.widget.ExtraDailyPlanItemId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock

class CheckInReminderActionReceiver : BroadcastReceiver(), KoinComponent {
    private val repository: CheckItRepository by inject()
    private val policy: CheckInReminderPolicy by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_MARK_DONE = "com.checkit.ACTION_CHECK_IN_MARK_DONE"
        const val ACTION_SNOOZE = "com.checkit.ACTION_CHECK_IN_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_MARK_DONE -> {
                        val itemId = intent.getLongExtra(ExtraDailyPlanItemId, -1L)
                        if (itemId != -1L) {
                            repository.updateDailyPlanItemStatus(itemId, DailyPlanItemStatus.Done)
                        } else {
                            Log.w("CheckInAction", "Mark done without item id")
                        }
                    }
                    ACTION_SNOOZE -> {
                        // Cooldown is 1h, so this buys roughly an hour of quiet.
                        policy.markReminderShown(Clock.System.now().toEpochMilliseconds())
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckInAction", "Failed to handle ${intent.action}", e)
            } finally {
                NotificationManagerCompat.from(context).cancel(NotificationIds.CheckInReminder)
                pendingResult.finish()
            }
        }
    }
}
