package com.checkit.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.checkit.domain.SprintState
import com.checkit.platform.Platform
import com.checkit.shared.R
import org.jetbrains.compose.resources.getString
import checkit.shared.generated.resources.*

class AndroidSprintNotificationScheduler(
    private val context: Context
) : SprintNotificationScheduler {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun startPersistentNotification(running: SprintState.Running) {
        SprintForegroundService.start(context)
    }

    override fun updatePersistentNotification(running: SprintState.Running, isPaused: Boolean) {
        // Foreground service handles its own updates by observing the SprintManager flow
    }

    override fun cancelNotification() {
        SprintForegroundService.stop(context)
    }

    override suspend fun showFinishedNotification(finished: SprintState.Finished) {
        if (Platform.isAppInForeground()) return

        val title = when {
            finished.isBreak -> getString(Res.string.sprint_break_finish_title)
            finished.isPomodoro -> getString(Res.string.sprint_pomodoro_finish_title)
            else -> getString(Res.string.sprint_finish_title)
        }
        val body = when {
            finished.isBreak -> getString(Res.string.sprint_break_finish_subtitle)
            finished.isPomodoro -> getString(Res.string.sprint_pomodoro_finish_subtitle)
            else -> getString(Res.string.sprint_finished_body, finished.description)
        }
        
        val intent = Intent().setClassName(context.packageName, "com.checkit.MainActivity").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = context.applicationInfo.icon
        val builder = NotificationCompat.Builder(context, NotificationChannels.ReminderId)
            .setSmallIcon(if (iconRes != 0) iconRes else R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (finished.isBreak) {
            builder.addAction(createAction(context, getString(Res.string.sprint_action_next_pomodoro), SprintActionReceiver.ACTION_START_NEXT))
            builder.addAction(createAction(context, getString(Res.string.cancel), SprintActionReceiver.ACTION_SAVE_WIN)) // SAVE_WIN for break just dismisses/saves idle
        } else if (finished.isPomodoro) {
            builder.addAction(createAction(context, getString(Res.string.sprint_action_save_and_break), SprintActionReceiver.ACTION_SAVE_BREAK))
            builder.addAction(createAction(context, getString(Res.string.sprint_action_continue_pomodoro), SprintActionReceiver.ACTION_SAVE_CONTINUE))
            builder.addAction(createAction(context, getString(Res.string.sprint_action_save), SprintActionReceiver.ACTION_SAVE_WIN))
        } else {
            builder.addAction(createAction(context, getString(Res.string.sprint_action_pomodoro), SprintActionReceiver.ACTION_UPGRADE))
            builder.addAction(createAction(context, getString(Res.string.sprint_action_save), SprintActionReceiver.ACTION_SAVE_WIN))
        }

        notificationManager.notify(1002, builder.build())
    }

    private fun createAction(context: Context, label: String, action: String): NotificationCompat.Action {
        val intent = Intent(context, SprintActionReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, label, pendingIntent).build()
    }
}
