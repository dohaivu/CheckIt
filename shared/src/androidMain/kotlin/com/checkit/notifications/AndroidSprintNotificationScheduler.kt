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
import android.util.Log

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
        Log.d("SprintNotification", "showFinishedNotification finished=$finished")
        CheckItNotificationCenter(context).ensureChannels()
        if (Platform.isAppInForeground()) {
            Log.d("SprintNotification", "App is in foreground, skipping tray notification (dialog should show)")
            return
        }

        try {
            val title = when {
                finished.isBreak -> getString(Res.string.sprint_break_finish_title)
                finished.isPomodoro -> getString(Res.string.sprint_pomodoro_finish_title)
                else -> getString(Res.string.sprint_finish_title)
            }
            
            val bodyTemplate = when {
                finished.isBreak -> getString(Res.string.sprint_break_finish_subtitle)
                finished.isPomodoro -> getString(Res.string.sprint_pomodoro_finish_subtitle)
                else -> getString(Res.string.sprint_finished_body)
            }
            // Manual formatting if needed or if getString(Res, arg) isn't working as expected
            val body = if (finished.isBreak || finished.isPomodoro) bodyTemplate else bodyTemplate.replace("%1\$s", finished.description)
            
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
                val nextLabel = getString(Res.string.sprint_action_next_pomodoro)
                val cancelLabel = getString(Res.string.cancel)
                builder.addAction(createAction(context, nextLabel, SprintActionReceiver.ACTION_START_NEXT))
                builder.addAction(createAction(context, cancelLabel, SprintActionReceiver.ACTION_SAVE_WIN))
            } else if (finished.isPomodoro) {
                val saveBreakLabel = getString(Res.string.sprint_action_save_and_break)
                val continueLabel = getString(Res.string.sprint_action_continue_pomodoro)
                val saveLabel = getString(Res.string.sprint_action_save)
                builder.addAction(createAction(context, saveBreakLabel, SprintActionReceiver.ACTION_SAVE_BREAK))
                builder.addAction(createAction(context, continueLabel, SprintActionReceiver.ACTION_SAVE_CONTINUE))
                builder.addAction(createAction(context, saveLabel, SprintActionReceiver.ACTION_SAVE_WIN))
            } else {
                val pomodoroLabel = getString(Res.string.sprint_action_pomodoro)
                val saveLabel = getString(Res.string.sprint_action_save)
                builder.addAction(createAction(context, pomodoroLabel, SprintActionReceiver.ACTION_UPGRADE))
                builder.addAction(createAction(context, saveLabel, SprintActionReceiver.ACTION_SAVE_WIN))
            }

            Log.d("SprintNotification", "Sending notification 1002")
            notificationManager.notify(1002, builder.build())
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("SprintNotification", "CRITICAL Error showing notification", e)
        }
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
