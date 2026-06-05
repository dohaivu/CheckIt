package com.checkit.infrastructure

import com.checkit.notifications.NoOpTaskReminderNotificationScheduler
import com.checkit.notifications.TaskReminderNotificationScheduler
import org.koin.dsl.module

actual fun platformModule() = module {
    single<TaskReminderNotificationScheduler> { NoOpTaskReminderNotificationScheduler() }
}
