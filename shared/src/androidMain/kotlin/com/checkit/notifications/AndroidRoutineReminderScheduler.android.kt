package com.checkit.notifications

import android.content.Context
import androidx.work.WorkManager

class AndroidRoutineReminderScheduler(
    context: Context
) : RoutineReminderScheduler {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    override suspend fun scheduleRoutineReminder(reminder: ScheduledRoutineReminder) {
        RoutineReminderWorker.scheduleAt(
            context = appContext,
            routineId = reminder.routineId,
            title = reminder.title,
            reminderMinutes = reminder.reminderMinutes,
            stepCount = reminder.stepCount,
            delayMillis = RoutineReminderWorker.delayUntilNext(reminder.reminderMinutes)
        )
    }

    override suspend fun cancelRoutineReminder(routineId: String) {
        workManager.cancelUniqueWork(RoutineReminderWorker.workName(routineId))
    }
}
