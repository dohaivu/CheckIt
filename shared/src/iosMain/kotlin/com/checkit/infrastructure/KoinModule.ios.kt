package com.checkit.infrastructure

import com.checkit.notifications.NoOpTaskReminderNotificationScheduler
import com.checkit.notifications.AppReminderScheduler
import com.checkit.notifications.DailyPlanScheduleReminderScheduler
import com.checkit.notifications.NoOpAppReminderScheduler
import com.checkit.notifications.NoOpDailyPlanScheduleReminderScheduler
import com.checkit.notifications.TaskReminderNotificationScheduler
import org.koin.dsl.module

actual fun platformModule() = module {
    single<TaskReminderNotificationScheduler> { NoOpTaskReminderNotificationScheduler() }
    single<DailyPlanScheduleReminderScheduler> { NoOpDailyPlanScheduleReminderScheduler() }
    single<AppReminderScheduler> { NoOpAppReminderScheduler() }
}
