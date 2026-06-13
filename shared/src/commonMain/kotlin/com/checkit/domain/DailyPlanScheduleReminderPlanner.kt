package com.checkit.domain

data class DailyPlanScheduleReminderItem(
    val id: Long,
    val title: String,
    val status: DailyPlanItemStatus,
    val startTimeMinutes: Int?
)

data class DailyPlanScheduleReminder(
    val itemId: Long,
    val title: String,
    val startTimeMinutes: Int
)

object DailyPlanScheduleReminderPlanner {
    fun nextReminder(
        items: List<DailyPlanScheduleReminderItem>,
        earliestTimeMinutes: Int
    ): DailyPlanScheduleReminder? {
        if (earliestTimeMinutes >= MinutesPerDay) return null
        val earliest = earliestTimeMinutes.coerceAtLeast(0)
        return items
            .asSequence()
            .filter { it.status == DailyPlanItemStatus.Planned }
            .mapNotNull { item ->
                val start = item.startTimeMinutes ?: return@mapNotNull null
                if (start <= earliest) return@mapNotNull null
                DailyPlanScheduleReminder(
                    itemId = item.id,
                    title = item.title.ifBlank { "My Day item" },
                    startTimeMinutes = start
                )
            }
            .minWithOrNull(compareBy<DailyPlanScheduleReminder> { it.startTimeMinutes }.thenBy { it.itemId })
    }

    private const val MinutesPerDay = 24 * 60
}
