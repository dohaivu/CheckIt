package com.checkit.notifications

interface DailyPlanScheduleReminderScheduler {
    suspend fun rescheduleNext(afterTimeMinutes: Int? = null)
    suspend fun cancel()
}

class NoOpDailyPlanScheduleReminderScheduler : DailyPlanScheduleReminderScheduler {
    override suspend fun rescheduleNext(afterTimeMinutes: Int?) = Unit
    override suspend fun cancel() = Unit
}
