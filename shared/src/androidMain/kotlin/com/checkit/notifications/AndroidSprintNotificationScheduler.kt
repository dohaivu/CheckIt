package com.checkit.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.checkit.domain.SprintState
import com.checkit.platform.Platform
import com.checkit.shared.R

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

    override fun showFinishedNotification(description: String, isPomodoro: Boolean, isBreak: Boolean) {
        if (Platform.isAppInForeground()) return

        val title = when {
            isBreak -> "Break Finished!"
            isPomodoro -> "Deep Focus Finished!"
            else -> "Sprint Finished!"
        }
        val body = if (isBreak) "Ready to dive back in?" else "You did it: $description. Great job starting."
        
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
        val notification = NotificationCompat.Builder(context, NotificationChannels.ReminderId)
            .setSmallIcon(if (iconRes != 0) iconRes else R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }
}
