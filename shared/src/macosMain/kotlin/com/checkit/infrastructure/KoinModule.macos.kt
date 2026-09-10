package com.checkit.infrastructure

import com.checkit.data.NoOpQuickNoteSyncManager
import com.checkit.data.QuickNoteSyncManager
import com.checkit.notifications.NoOpQuickNoteReminderScheduler
import com.checkit.notifications.NoOpTaskReminderNotificationScheduler
import com.checkit.notifications.AppReminderScheduler
import com.checkit.notifications.CheckInReminderForceRunner
import com.checkit.notifications.DailyPlanScheduleReminderScheduler
import com.checkit.notifications.NoOpAppReminderScheduler
import com.checkit.notifications.NoOpCheckInReminderForceRunner
import com.checkit.notifications.NoOpDailyPlanScheduleReminderScheduler
import com.checkit.notifications.QuickNoteReminderScheduler
import com.checkit.notifications.TaskReminderNotificationScheduler
import org.koin.dsl.module

actual fun platformModule() = module {
    single<TaskReminderNotificationScheduler> { NoOpTaskReminderNotificationScheduler() }
    single<DailyPlanScheduleReminderScheduler> { NoOpDailyPlanScheduleReminderScheduler() }
    single<AppReminderScheduler> { NoOpAppReminderScheduler() }
    single<CheckInReminderForceRunner> { NoOpCheckInReminderForceRunner() }
    single<QuickNoteReminderScheduler> { NoOpQuickNoteReminderScheduler() }
    single<QuickNoteSyncManager> { NoOpQuickNoteSyncManager() }
}
