package com.checkit.infrastructure

import com.checkit.notifications.AndroidAppReminderScheduler
import com.checkit.notifications.AndroidTaskReminderNotificationScheduler
import com.checkit.notifications.AppReminderScheduler
import com.checkit.notifications.TaskReminderNotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    single<TaskReminderNotificationScheduler> { AndroidTaskReminderNotificationScheduler(androidContext()) }
    single<AppReminderScheduler> { AndroidAppReminderScheduler(androidContext()) }
}
