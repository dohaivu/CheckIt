package com.checkit.infrastructure

import com.checkit.data.NoOpNestedSyncManager
import com.checkit.data.NoOpQuickNoteSyncManager
import com.checkit.data.NestedSyncManager
import com.checkit.auth.GoogleAccountManager
import com.checkit.auth.NoOpGoogleAccountManager
import com.checkit.checklist.ChecklistStorage
import com.checkit.checklist.NoOpChecklistStorage
import com.checkit.data.QuickNoteSyncManager
import com.checkit.notifications.NoOpQuickNoteReminderScheduler
import com.checkit.notifications.NoOpRoutineReminderScheduler
import com.checkit.notifications.NoOpTaskReminderNotificationScheduler
import com.checkit.notifications.AppReminderScheduler
import com.checkit.notifications.CheckInReminderForceRunner
import com.checkit.notifications.CountdownScheduler
import com.checkit.notifications.DailyPlanScheduleReminderScheduler
import com.checkit.notifications.NoOpAppReminderScheduler
import com.checkit.notifications.NoOpCheckInReminderForceRunner
import com.checkit.notifications.NoOpCountdownScheduler
import com.checkit.notifications.NoOpDailyPlanScheduleReminderScheduler
import com.checkit.notifications.QuickNoteReminderScheduler
import com.checkit.notifications.RoutineReminderScheduler
import com.checkit.notifications.TaskReminderNotificationScheduler
import com.checkit.platform.BackupScheduler
import com.checkit.platform.NoOpBackupScheduler
import com.checkit.ui.quicknote.NoOpQuickNoteCameraCapture
import com.checkit.ui.quicknote.QuickNoteCameraCapture
import org.koin.dsl.module

actual fun platformModule() = module {
    single<TaskReminderNotificationScheduler> { NoOpTaskReminderNotificationScheduler() }
    single<DailyPlanScheduleReminderScheduler> { NoOpDailyPlanScheduleReminderScheduler() }
    single<AppReminderScheduler> { NoOpAppReminderScheduler() }
    single<CheckInReminderForceRunner> { NoOpCheckInReminderForceRunner() }
    single<CountdownScheduler> { NoOpCountdownScheduler() }
    single<QuickNoteReminderScheduler> { NoOpQuickNoteReminderScheduler() }
    single<RoutineReminderScheduler> { NoOpRoutineReminderScheduler() }
    single<QuickNoteSyncManager> { NoOpQuickNoteSyncManager() }
    single<GoogleAccountManager> { NoOpGoogleAccountManager() }
    single<NestedSyncManager> { NoOpNestedSyncManager() }
    single<QuickNoteCameraCapture> { NoOpQuickNoteCameraCapture() }
    single<BackupScheduler> { NoOpBackupScheduler() }
    single<ChecklistStorage> { NoOpChecklistStorage() }
}
