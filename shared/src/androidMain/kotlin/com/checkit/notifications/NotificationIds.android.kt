package com.checkit.notifications

internal object NotificationIds {
    const val PlanReminder = 70_001
    const val ReviewReminder = 70_002
    const val CheckInReminder = 70_003

    fun taskReminder(taskId: Long): Int =
        taskId.stableIntId()

    fun appReminder(type: String): Int = when (type) {
        DailyAppReminderWorker.TypePlan -> PlanReminder
        DailyAppReminderWorker.TypeReview -> ReviewReminder
        else -> 70_000
    }

    fun dailyPlanSchedule(itemId: Long): Int =
        80_000 + itemId.stableIntId().and(0x3fff)

    private fun Long.stableIntId(): Int =
        (this xor (this ushr 32)).toInt()
}
