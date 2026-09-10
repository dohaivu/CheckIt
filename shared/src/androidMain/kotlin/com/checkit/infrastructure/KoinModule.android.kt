package com.checkit.infrastructure

import com.checkit.data.FirestoreQuickNoteSyncManager
import com.checkit.data.QuickNoteSyncManager
import com.checkit.notifications.AlarmManagerQuickNoteReminderScheduler
import com.checkit.notifications.AndroidCheckInReminderForceRunner
import com.checkit.notifications.AndroidDailyPlanScheduleReminderScheduler
import com.checkit.notifications.AndroidAppReminderScheduler
import com.checkit.notifications.AndroidSprintNotificationScheduler
import com.checkit.notifications.AndroidTaskReminderNotificationScheduler
import com.checkit.notifications.AppReminderScheduler
import com.checkit.notifications.CheckInReminderForceRunner
import com.checkit.notifications.DailyPlanScheduleReminderScheduler
import com.checkit.notifications.QuickNoteReminderScheduler
import com.checkit.notifications.SprintNotificationScheduler
import com.checkit.notifications.TaskReminderNotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    single<TaskReminderNotificationScheduler> { AndroidTaskReminderNotificationScheduler(androidContext()) }
    single<DailyPlanScheduleReminderScheduler> {
        AndroidDailyPlanScheduleReminderScheduler(androidContext(), get())
    }
    single<AppReminderScheduler> { AndroidAppReminderScheduler(androidContext(), get()) }
    single<SprintNotificationScheduler> { AndroidSprintNotificationScheduler(androidContext()) }
    single<CheckInReminderForceRunner> { AndroidCheckInReminderForceRunner(androidContext(), get(), get()) }
    single<QuickNoteReminderScheduler> { AlarmManagerQuickNoteReminderScheduler(androidContext()) }
    single<QuickNoteSyncManager> { FirestoreQuickNoteSyncManager(get(), get()) }
}
