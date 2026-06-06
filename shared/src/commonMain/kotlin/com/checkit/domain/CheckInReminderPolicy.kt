package com.checkit.domain

import com.checkit.data.CheckItDao
import com.checkit.data.SettingsRepository
import kotlinx.coroutines.flow.first

data class CheckInReminderPlanItem(
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?
)

class CheckInReminderPolicy(
    private val dao: CheckItDao,
    private val settingsRepository: SettingsRepository
) {
    suspend fun shouldShowReminder(
        dateEpochDays: Int,
        nowMinutes: Int,
        nowMillis: Long
    ): Boolean {
        val settings = settingsRepository.settings.first()
        if (!settings.checkInReminderEnabled) return false

        return shouldShowReminder(
            nowMinutes = nowMinutes,
            nowMillis = nowMillis,
            lastShownAtMillis = settings.checkInReminderLastShownAtMillis,
            loadItems = {
                dao.dailyPlanItemsForDate(dateEpochDays).map { item ->
                    CheckInReminderPlanItem(
                        startTimeMinutes = item.startTimeMinutes,
                        endTimeMinutes = item.endTimeMinutes
                    )
                }
            }
        )
    }

    suspend fun markReminderShown(shownAtMillis: Long) {
        settingsRepository.setCheckInReminderLastShownAtMillis(shownAtMillis)
    }

    companion object {
        const val NearbyWindowMinutes = 20
        const val MinimumRepeatIntervalMillis = 2L * 60L * 60L * 1000L
        private const val MinutesPerDay = 24 * 60

        suspend fun shouldShowReminder(
            nowMinutes: Int,
            nowMillis: Long,
            lastShownAtMillis: Long?,
            loadItems: suspend () -> List<CheckInReminderPlanItem>
        ): Boolean {
            if (!NotificationDoNotDisturbPolicy.canNotifyAt(nowMinutes)) return false
            if (isInsideCooldown(nowMillis, lastShownAtMillis)) return false
            return hasNoNearbyItem(loadItems(), nowMinutes)
        }

        fun shouldShowReminder(
            items: List<CheckInReminderPlanItem>,
            nowMinutes: Int,
            nowMillis: Long,
            lastShownAtMillis: Long?
        ): Boolean {
            if (!NotificationDoNotDisturbPolicy.canNotifyAt(nowMinutes)) return false
            if (isInsideCooldown(nowMillis, lastShownAtMillis)) return false
            return hasNoNearbyItem(items, nowMinutes)
        }

        private fun isInsideCooldown(nowMillis: Long, lastShownAtMillis: Long?): Boolean =
            lastShownAtMillis != null && nowMillis - lastShownAtMillis < MinimumRepeatIntervalMillis

        private fun hasNoNearbyItem(items: List<CheckInReminderPlanItem>, nowMinutes: Int): Boolean =
            items.none { item ->
                val start = item.startTimeMinutes ?: return@none false
                val end = item.endTimeMinutes ?: start
                overlapsWindow(
                    start = start,
                    end = end,
                    windowStart = nowMinutes - NearbyWindowMinutes,
                    windowEnd = nowMinutes + NearbyWindowMinutes
                )
            }

        private fun overlapsWindow(start: Int, end: Int, windowStart: Int, windowEnd: Int): Boolean {
            val itemStart = start.coerceIn(0, MinutesPerDay - 1)
            val itemEnd = end.coerceIn(itemStart, MinutesPerDay - 1)
            val clampedWindowStart = windowStart.coerceAtLeast(0)
            val clampedWindowEnd = windowEnd.coerceAtMost(MinutesPerDay - 1)
            return itemStart <= clampedWindowEnd && itemEnd >= clampedWindowStart
        }
    }
}
