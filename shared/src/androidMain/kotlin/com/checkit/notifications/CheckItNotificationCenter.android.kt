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
import com.checkit.widget.ExtraDailyPlanItemId
import java.time.LocalTime

class CheckItNotificationCenter(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun showTaskReminder(taskId: Long, taskName: String, label: String) {
        showReminder(
            notificationId = NotificationIds.taskReminder(taskId),
            requestCode = taskId.hashCode(),
            title = taskName.ifBlank { "Task reminder" },
            body = label,
            subText = null,
            bypassDnd = true // User specifically set this reminder, show it regardless of DND
        )
    }

    fun showAppReminder(notificationId: Int, title: String, body: String) {
        showReminder(
            notificationId = notificationId,
            requestCode = notificationId,
            title = title,
            body = body,
            subText = null,
            bypassDnd = false // App reminders respect DND
        )
    }

    fun showAppReminder(notificationId: Int, title: String, body: String, type: AppReminderType) {
        showReminder(
            notificationId = notificationId,
            requestCode = notificationId,
            title = title,
            body = body,
            subText = type.subText,
            bypassDnd = false // App reminders respect DND
        )
    }

    fun showDailyPlanScheduleReminder(itemId: Long, title: String) {
        showReminder(
            notificationId = NotificationIds.dailyPlanSchedule(itemId),
            requestCode = NotificationIds.dailyPlanSchedule(itemId),
            title = title.ifBlank { "My Day" },
            body = NotificationText.withActionQuote("Starting now"),
            subText = AppReminderType.Schedule.subText,
            dailyPlanItemId = itemId,
            bypassDnd = false
        )
    }

    private fun showReminder(
        notificationId: Int,
        requestCode: Int,
        title: String,
        body: String,
        subText: String?,
        bypassDnd: Boolean
    ) {
        showReminder(
            notificationId = notificationId,
            requestCode = requestCode,
            title = title,
            body = body,
            subText = subText,
            dailyPlanItemId = null,
            bypassDnd = bypassDnd
        )
    }

    private fun showReminder(
        notificationId: Int,
        requestCode: Int,
        title: String,
        body: String,
        subText: String?,
        dailyPlanItemId: Long?,
        bypassDnd: Boolean
    ) {
        if (!canPostNotifications()) return
        if (!bypassDnd && !canNotifyNow()) return

        ensureChannels()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            dailyPlanItemId?.let { putExtra(ExtraDailyPlanItemId, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val iconRes = context.applicationInfo.icon
        val notification = NotificationCompat.Builder(context, NotificationChannels.ReminderId)
            .setSmallIcon(if (iconRes != 0) iconRes else R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSubText(subText)
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
            NotificationChannels.ReminderId,
            NotificationChannels.ReminderName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = NotificationChannels.ReminderDescription
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
}
