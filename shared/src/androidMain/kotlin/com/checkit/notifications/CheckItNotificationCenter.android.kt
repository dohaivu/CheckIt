package com.checkit.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.checkit.MainActivity
import com.checkit.domain.NotificationDoNotDisturbPolicy
import com.checkit.shared.R
import java.time.LocalTime

class CheckItNotificationCenter(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun showTaskReminder(taskId: Long, taskName: String, label: String) {
        showReminder(
            notificationId = notificationId(taskId),
            requestCode = taskId.hashCode(),
            title = taskName.ifBlank { "Task reminder" },
            body = label,
            bypassDnd = true // User specifically set this reminder, show it regardless of DND
        )
    }

    fun showAppReminder(notificationId: Int, title: String, body: String) {
        showReminder(
            notificationId = notificationId,
            requestCode = notificationId,
            title = title,
            body = body,
            bypassDnd = false // App reminders respect DND
        )
    }

    private fun showReminder(notificationId: Int, requestCode: Int, title: String, body: String, bypassDnd: Boolean) {
        if (!canPostNotifications()) return
        if (!bypassDnd && !canNotifyNow()) return

        ensureChannels()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val iconRes = context.applicationInfo.icon
        val notification = NotificationCompat.Builder(context, ReminderChannelId)
            .setSmallIcon(if (iconRes != 0) iconRes else R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ReminderChannelId,
            "Task reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for scheduled task reminders"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun canNotifyNow(): Boolean {
        val now = LocalTime.now()
        return NotificationDoNotDisturbPolicy.canNotifyAt(now.hour * 60 + now.minute)
    }

    private fun notificationId(taskId: Long): Int =
        (taskId xor (taskId ushr 32)).toInt()

    companion object {
        const val ReminderChannelId = "task_reminders"
    }
}
