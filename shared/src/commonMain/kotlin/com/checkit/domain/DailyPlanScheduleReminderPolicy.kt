package com.checkit.domain

import com.checkit.data.CheckItDao
import com.checkit.data.SettingsRepository
import kotlinx.coroutines.flow.first

class DailyPlanScheduleReminderPolicy(
    private val dao: CheckItDao,
    private val settingsRepository: SettingsRepository
) {
    suspend fun nextReminderForDate(
        dateEpochDays: Int,
        earliestTimeMinutes: Int
    ): DailyPlanScheduleReminder? {
        val settings = settingsRepository.settings.first()
        if (!settings.scheduleReminderEnabled) return null

        return DailyPlanScheduleReminderPlanner.nextReminder(
            items = dao.dailyPlanItemsForDate(dateEpochDays).map { item ->
                DailyPlanScheduleReminderItem(
                    id = item.id,
                    title = item.title,
                    status = enumValueOf(item.status),
                    startTimeMinutes = item.startTimeMinutes
                )
            },
            earliestTimeMinutes = earliestTimeMinutes
        )
    }

    suspend fun shouldShowReminder(
        dateEpochDays: Int,
        itemId: Long,
        scheduledTimeMinutes: Int
    ): Boolean {
        val settings = settingsRepository.settings.first()
        if (!settings.scheduleReminderEnabled) return false

        val item = dao.dailyPlanItemsForDate(dateEpochDays).firstOrNull { it.id == itemId } ?: return false
        return enumValueOf<DailyPlanItemStatus>(item.status) == DailyPlanItemStatus.Planned &&
            item.startTimeMinutes == scheduledTimeMinutes
    }
}
