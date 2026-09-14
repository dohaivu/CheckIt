package com.checkit.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.os.Build
import com.checkit.domain.CountdownState
import com.checkit.shared.R

class AndroidCountdownScheduler(
    private val context: Context
) : CountdownScheduler {

    override fun startPersistentNotification(running: CountdownState.Running) {
        CountdownService.start(context, running.content, running.totalSeconds)
    }

    override fun updatePersistentNotification(running: CountdownState.Running) {
        // CountdownService observes the manager flow and re-posts itself.
    }

    override fun cancelNotification() {
        CountdownService.stop(context)
    }

    override suspend fun showFinishedNotification(finished: CountdownState.Finished) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                android.app.NotificationChannel(
                    CHANNEL_ID,
                    "Countdown Timer",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Countdown finished alerts" }
            )
        }
        val intent = Intent().setClassName(context.packageName, "com.checkit.MainActivity").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = finished.content.ifBlank { "Countdown finished" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.schedule_24px)
            .setContentTitle("Time's up")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(NotificationIds.CountdownFinished, notification)
    }

    private companion object {
        const val CHANNEL_ID = "countdown_timer_channel"
    }
}
