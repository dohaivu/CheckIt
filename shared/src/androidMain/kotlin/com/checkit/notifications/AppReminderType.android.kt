package com.checkit.notifications

enum class AppReminderType(val subText: String) {
    Plan("Plan"),
    Review("Review");

    companion object {
        fun fromDailyWorkerType(type: String): AppReminderType = when (type) {
            DailyAppReminderWorker.TypePlan -> Plan
            DailyAppReminderWorker.TypeReview -> Review
            else -> Plan
        }
    }
}
