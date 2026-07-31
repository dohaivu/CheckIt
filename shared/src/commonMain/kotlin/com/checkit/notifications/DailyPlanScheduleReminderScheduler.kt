package com.checkit.notifications

interface DailyPlanScheduleReminderScheduler {
    suspend fun rescheduleNext(afterTimeMinutes: Int? = null, isRescheduling: Boolean = false)
    suspend fun cancel()
}

class NoOpDailyPlanScheduleReminderScheduler : DailyPlanScheduleReminderScheduler {
    override suspend fun rescheduleNext(afterTimeMinutes: Int?, isRescheduling: Boolean) = Unit
    override suspend fun cancel() = Unit
}
