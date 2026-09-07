package com.checkit.domain

import com.checkit.data.CheckItDao
import com.checkit.data.SettingsRepository
import kotlinx.coroutines.flow.first

data class CheckInReminderPlanItem(
    val id: Long? = null,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val isDone: Boolean = false,
    val title: String = ""
)

data class CheckInDecision(
    val shouldShow: Boolean,
    val idleMinutes: Int?,
    val currentItem: CheckInReminderPlanItem?
)

class CheckInReminderPolicy(
    private val dao: CheckItDao,
    private val settingsRepository: SettingsRepository
) {
    suspend fun evaluate(
        dateEpochDays: Int,
        nowMinutes: Int,
        nowMillis: Long,
        force: Boolean = false
    ): CheckInDecision {
        val settings = settingsRepository.settings.first()
        if (!settings.checkInReminderEnabled) return CheckInDecision(false, null, null)
        if (!force) {
            if (!NotificationDoNotDisturbPolicy.canNotifyAt(nowMinutes)) return CheckInDecision(false, null, null)
            if (isInsideCooldown(nowMillis, settings.checkInReminderLastShownAtMillis)) {
                return CheckInDecision(false, null, null)
            }
        }
        val items = dao.dailyPlanItemsForDate(dateEpochDays).map { item ->
            CheckInReminderPlanItem(
                id = item.id,
                startTimeMinutes = item.startTimeMinutes,
                endTimeMinutes = item.endTimeMinutes,
                isDone = item.status == DailyPlanItemStatus.Done.name,
                title = item.title
            )
        }
        return decide(items, nowMinutes, settings.idleCheckInThresholdMinutes)
    }

    suspend fun markReminderShown(shownAtMillis: Long) {
        settingsRepository.setCheckInReminderLastShownAtMillis(shownAtMillis)
    }

    companion object {
        const val MinimumRepeatIntervalMillis = 60L * 60L * 1000L // 1 hour
        const val DefaultIdleThresholdMinutes = 60
        const val MinIdleThresholdMinutes = 15
        const val MaxIdleThresholdMinutes = 240
        const val MorningGuardMinutes = 9 * 60

        suspend fun shouldShowReminder(
            nowMinutes: Int,
            nowMillis: Long,
            lastShownAtMillis: Long?,
            idleThresholdMinutes: Int = DefaultIdleThresholdMinutes,
            force: Boolean = false,
            loadItems: suspend () -> List<CheckInReminderPlanItem>
        ): Boolean {
            if (!force) {
                if (!NotificationDoNotDisturbPolicy.canNotifyAt(nowMinutes)) return false
                if (isInsideCooldown(nowMillis, lastShownAtMillis)) return false
            }
            return isIdle(loadItems(), nowMinutes, idleThresholdMinutes)
        }

        fun shouldShowReminder(
            items: List<CheckInReminderPlanItem>,
            nowMinutes: Int,
            nowMillis: Long,
            lastShownAtMillis: Long?,
            idleThresholdMinutes: Int = DefaultIdleThresholdMinutes,
            force: Boolean = false
        ): Boolean {
            if (!force) {
                if (!NotificationDoNotDisturbPolicy.canNotifyAt(nowMinutes)) return false
                if (isInsideCooldown(nowMillis, lastShownAtMillis)) return false
            }
            return isIdle(items, nowMinutes, idleThresholdMinutes)
        }

        fun decide(
            items: List<CheckInReminderPlanItem>,
            nowMinutes: Int,
            idleThresholdMinutes: Int = DefaultIdleThresholdMinutes
        ): CheckInDecision {
            val lastDone = lastDoneEndMinutes(items)
            val idleMinutes = lastDone?.let { (nowMinutes - it).coerceAtLeast(0) }
            val idle = if (lastDone == null) {
                // No finished block yet; avoid nagging in the early morning
                // when the plan reminder owns the nudge.
                nowMinutes >= MorningGuardMinutes
            } else {
                nowMinutes - lastDone >= idleThresholdMinutes.coerceAtLeast(1)
            }
            if (!idle) return CheckInDecision(false, idleMinutes, null)
            return CheckInDecision(true, idleMinutes, currentTimedItem(items, nowMinutes))
        }

        private fun isInsideCooldown(nowMillis: Long, lastShownAtMillis: Long?): Boolean =
            lastShownAtMillis != null && nowMillis - lastShownAtMillis < MinimumRepeatIntervalMillis

        /**
         * End of the most recently finished time block, in minutes of day.
         * Point items (start only) count by their start. Untimed Done items
         * carry no time signal and are ignored.
         */
        fun lastDoneEndMinutes(items: List<CheckInReminderPlanItem>): Int? =
            items.filter { it.isDone }
                .mapNotNull { it.endTimeMinutes ?: it.startTimeMinutes }
                .maxOrNull()

        fun idleMinutesSinceLastDone(
            items: List<CheckInReminderPlanItem>,
            nowMinutes: Int
        ): Int? {
            val lastDone = lastDoneEndMinutes(items) ?: return null
            return (nowMinutes - lastDone).coerceAtLeast(0)
        }

        fun isIdle(
            items: List<CheckInReminderPlanItem>,
            nowMinutes: Int,
            idleThresholdMinutes: Int = DefaultIdleThresholdMinutes
        ): Boolean = decide(items, nowMinutes, idleThresholdMinutes).shouldShow

        /**
         * The unfinished timed item happening right now, if any.
         * Bounds are inclusive: an item [start, end] matches start <= now <= end.
         * Done items are excluded — no point asking about finished work.
         * When several overlap, the most recently started one wins.
         */
        fun currentTimedItem(
            items: List<CheckInReminderPlanItem>,
            nowMinutes: Int
        ): CheckInReminderPlanItem? =
            items.filter { item ->
                if (item.isDone) return@filter false
                val start = item.startTimeMinutes ?: return@filter false
                val end = item.endTimeMinutes ?: start
                nowMinutes in start..end
            }.maxByOrNull { it.startTimeMinutes ?: Int.MIN_VALUE }
    }
}
