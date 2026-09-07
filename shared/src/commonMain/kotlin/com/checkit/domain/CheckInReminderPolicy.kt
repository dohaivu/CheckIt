package com.checkit.domain

import com.checkit.data.CheckItDao
import com.checkit.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

data class CheckInReminderPlanItem(
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val isDone: Boolean = false,
    val completedAtMillis: Long? = null,
    val handledAtMillis: Long? = null,
    val title: String = "",
    /**
     * Planned end of a timed item as epoch millis. Preferred recency signal:
     * tap timestamps drift from real work (late or batch marking), while the
     * planned end says when the time block was over.
     */
    val scheduledEndMillis: Long? = null
)

data class CheckInDecision(
    val shouldShow: Boolean,
    val idleMinutes: Long?,
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
                startTimeMinutes = item.startTimeMinutes,
                endTimeMinutes = item.endTimeMinutes,
                isDone = item.status == DailyPlanItemStatus.Done.name,
                completedAtMillis = item.completedAtMillis,
                handledAtMillis = item.handledAtMillis,
                title = item.title,
                scheduledEndMillis = scheduledEndMillis(
                    dateEpochDays = dateEpochDays,
                    endTimeMinutes = item.endTimeMinutes,
                    startTimeMinutes = item.startTimeMinutes
                )
            )
        }
        return decide(items, nowMinutes, nowMillis, settings.idleCheckInThresholdMinutes)
    }

    suspend fun markReminderShown(shownAtMillis: Long) {
        settingsRepository.setCheckInReminderLastShownAtMillis(shownAtMillis)
    }

    companion object {
        const val MinimumRepeatIntervalMillis = 1L * 60L * 60L * 1000L
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
            val items = loadItems()
            return isIdle(items, nowMinutes, nowMillis, idleThresholdMinutes)
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
            return isIdle(items, nowMinutes, nowMillis, idleThresholdMinutes)
        }

        fun decide(
            items: List<CheckInReminderPlanItem>,
            nowMinutes: Int,
            nowMillis: Long,
            idleThresholdMinutes: Int = DefaultIdleThresholdMinutes
        ): CheckInDecision {
            val lastDone = lastDoneAtMillis(items)
            val idleMinutes = lastDone?.let { doneAt ->
                if (nowMillis < doneAt) 0L else (nowMillis - doneAt) / 60_000L
            }
            val idle = if (lastDone == null) {
                // No usable Done signal yet; avoid nagging in the early morning
                // when the plan reminder owns the nudge.
                nowMinutes >= MorningGuardMinutes
            } else {
                nowMillis - lastDone >= idleThresholdMinutes.coerceAtLeast(1) * 60_000L
            }
            if (!idle) return CheckInDecision(false, idleMinutes, null)
            return CheckInDecision(true, idleMinutes, currentTimedItem(items, nowMinutes))
        }

        private fun isInsideCooldown(nowMillis: Long, lastShownAtMillis: Long?): Boolean =
            lastShownAtMillis != null && nowMillis - lastShownAtMillis < MinimumRepeatIntervalMillis

        fun lastDoneAtMillis(items: List<CheckInReminderPlanItem>): Long? =
            items.filter { it.isDone }
                .mapNotNull { it.scheduledEndMillis ?: it.completedAtMillis ?: it.handledAtMillis }
                .maxOrNull()

        /**
         * Converts a timed item's planned end (or start for point items) to epoch
         * millis on [dateEpochDays]. Null for untimed items — those fall back to
         * tap timestamps in [lastDoneAtMillis]. Resolved via LocalDateTime so DST
         * transitions don't skew the result.
         */
        fun scheduledEndMillis(
            dateEpochDays: Int,
            endTimeMinutes: Int?,
            startTimeMinutes: Int?,
            timeZone: TimeZone = TimeZone.currentSystemDefault()
        ): Long? {
            val endMinutes = endTimeMinutes ?: startTimeMinutes ?: return null
            val dateTime = LocalDateTime(
                LocalDate.fromEpochDays(dateEpochDays),
                LocalTime((endMinutes / 60).coerceIn(0, 23), (endMinutes % 60).coerceIn(0, 59))
            )
            return dateTime.toInstant(timeZone).toEpochMilliseconds()
        }

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
        ): Boolean = decide(items, nowMinutes, nowMillis, idleThresholdMinutes).shouldShow

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
