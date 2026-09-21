package com.checkit.notifications

internal object NotificationIds {
    const val PlanReminder = 70_001
    const val ReviewReminder = 70_002
    const val CheckInReminder = 70_003
    const val SprintFinished = 1002
    const val CountdownOngoing = 1003
    const val CountdownFinished = 1004

    fun taskReminder(taskId: String): Int =
        taskId.hashCode()

    fun appReminder(type: String): Int = when (type) {
        DailyAppReminderWorker.TypePlan -> PlanReminder
        DailyAppReminderWorker.TypeReview -> ReviewReminder
        else -> 70_000
    }

    fun dailyPlanSchedule(itemId: String): Int =
        80_000 + (itemId.hashCode() and 0x3fff)

    fun quickNoteReminder(noteId: String): Int =
        90_000 + (noteId.hashCode() and 0x3fff)
}
