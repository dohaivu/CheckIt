package com.checkit.domain

import com.checkit.data.CheckItDao
import com.checkit.data.SettingsRepository
import com.checkit.ui.MinutesPerDay
import kotlinx.coroutines.flow.first

data class CheckInReminderPlanItem(
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val isDone: Boolean = false,
    val completedAtMillis: Long? = null,
    val handledAtMillis: Long? = null
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
            idleThresholdMinutes = settings.idleCheckInThresholdMinutes,
            loadItems = {
                dao.dailyPlanItemsForDate(dateEpochDays).map { item ->
                    CheckInReminderPlanItem(
                        startTimeMinutes = item.startTimeMinutes,
                        endTimeMinutes = item.endTimeMinutes,
                        isDone = item.status == DailyPlanItemStatus.Done.name,
                        completedAtMillis = item.completedAtMillis,
                        handledAtMillis = item.handledAtMillis
                    )
                }
            }
        )
    }

    suspend fun markReminderShown(shownAtMillis: Long) {
        settingsRepository.setCheckInReminderLastShownAtMillis(shownAtMillis)
    }

    suspend fun idleMinutesForDate(dateEpochDays: Int, nowMillis: Long): Long? {
        val items = dao.dailyPlanItemsForDate(dateEpochDays).map { item ->
            CheckInReminderPlanItem(
                startTimeMinutes = item.startTimeMinutes,
                endTimeMinutes = item.endTimeMinutes,
                isDone = item.status == DailyPlanItemStatus.Done.name,
                completedAtMillis = item.completedAtMillis,
                handledAtMillis = item.handledAtMillis
            )
        }
        return idleMinutesSinceLastDone(items, nowMillis)
    }

    companion object {
        const val NearbyWindowMinutes = 15
        const val MinimumRepeatIntervalMillis = 1L * 60L * 60L * 1000L
        const val DefaultIdleThresholdMinutes = 60
        const val MorningGuardMinutes = 9 * 60

        suspend fun shouldShowReminder(
            nowMinutes: Int,
            nowMillis: Long,
            lastShownAtMillis: Long?,
            idleThresholdMinutes: Int = DefaultIdleThresholdMinutes,
            loadItems: suspend () -> List<CheckInReminderPlanItem>
        ): Boolean {
            if (!NotificationDoNotDisturbPolicy.canNotifyAt(nowMinutes)) return false
            if (isInsideCooldown(nowMillis, lastShownAtMillis)) return false
            val items = loadItems()
            if (!hasNoNearbyItem(items, nowMinutes)) return false
            return isIdle(items, nowMinutes, nowMillis, idleThresholdMinutes)
        }

        fun shouldShowReminder(
            items: List<CheckInReminderPlanItem>,
            nowMinutes: Int,
            nowMillis: Long,
            lastShownAtMillis: Long?,
            idleThresholdMinutes: Int = DefaultIdleThresholdMinutes
        ): Boolean {
            if (!NotificationDoNotDisturbPolicy.canNotifyAt(nowMinutes)) return false
            if (isInsideCooldown(nowMillis, lastShownAtMillis)) return false
            if (!hasNoNearbyItem(items, nowMinutes)) return false
            return isIdle(items, nowMinutes, nowMillis, idleThresholdMinutes)
        }

        private fun isInsideCooldown(nowMillis: Long, lastShownAtMillis: Long?): Boolean =
            lastShownAtMillis != null && nowMillis - lastShownAtMillis < MinimumRepeatIntervalMillis

        fun lastDoneAtMillis(items: List<CheckInReminderPlanItem>): Long? =
            items.filter { it.isDone }
                .mapNotNull { it.completedAtMillis ?: it.handledAtMillis }
                .maxOrNull()

        fun idleMinutesSinceLastDone(
            items: List<CheckInReminderPlanItem>,
            nowMillis: Long
        ): Long? {
            val lastDone = lastDoneAtMillis(items) ?: return null
            if (nowMillis < lastDone) return 0L
            return (nowMillis - lastDone) / 60_000L
        }

        fun isIdle(
            items: List<CheckInReminderPlanItem>,
            nowMinutes: Int,
            nowMillis: Long,
            idleThresholdMinutes: Int = DefaultIdleThresholdMinutes
        ): Boolean {
            val lastDone = lastDoneAtMillis(items)
            if (lastDone == null) {
                // Nothing completed today yet; avoid nagging in the early morning
                // when the plan reminder owns the nudge.
                return nowMinutes >= MorningGuardMinutes
            }
            return nowMillis - lastDone >= idleThresholdMinutes.coerceAtLeast(1) * 60_000L
        }

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
