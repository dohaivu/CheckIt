package com.checkit.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.checkit.domain.QuickNote
import kotlin.time.Clock

class AlarmManagerQuickNoteReminderScheduler(
    context: Context,
) : QuickNoteReminderScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun schedule(note: QuickNote) {
        val remindAt = note.remindAt ?: return
        if (note.deleted) return
        if (remindAt <= Clock.System.now().toEpochMilliseconds()) return
        val intent = Intent(appContext, QuickNoteReminderReceiver::class.java).apply {
            action = QuickNoteReminderReceiver.ACTION_REMIND
            putExtra(QuickNoteReminderReceiver.EXTRA_NOTE_ID, note.id)
            putExtra(QuickNoteReminderReceiver.EXTRA_CONTENT, note.content)
        }
        val pending = PendingIntent.getBroadcast(
            appContext,
            note.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pending)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pending)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, remindAt, pending)
            }
        } catch (_: SecurityException) {
            // Exact alarms denied: fall back to an inexact alarm so the reminder still fires.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pending)
        }
    }

    override suspend fun cancel(noteId: String) {
        val intent = Intent(appContext, QuickNoteReminderReceiver::class.java).apply {
            action = QuickNoteReminderReceiver.ACTION_REMIND
        }
        val pending = PendingIntent.getBroadcast(
            appContext,
            noteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
        pending.cancel()
    }
}
