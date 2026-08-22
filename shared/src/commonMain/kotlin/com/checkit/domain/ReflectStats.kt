package com.checkit.domain

import kotlinx.datetime.LocalDate

/** Precomputed per-day aggregates backing the Reflect tab. One row per day. */
data class DailyReflectStat(
    val dateEpochDays: Int,
    /** Actionable items (MyDayTask/MyDayReminder/ExistingTask) still planned. */
    val plannedItemCount: Int,
    /** Actionable items completed. */
    val doneItemCount: Int,
    /** Sum of scheduled minutes across all done items. */
    val doneMinutes: Int,
    val journalCount: Int
) {
    val date: LocalDate get() = LocalDate.fromEpochDays(dateEpochDays)
}

/**
 * Precomputed done count/minutes for one day and tag, joined with tag
 * name/color so the UI always reflects the current tag metadata.
 */
data class DailyTagRollup(
    val dateEpochDays: Int,
    val tagId: Long,
    val tagName: String,
    val tagColor: String?,
    val doneCount: Int,
    val doneMinutes: Int
)

/**
 * Precomputed habit check-in for one day. [habitKey] is stable across
 * carry-overs: `task:<taskId>` when task-backed, otherwise derived from title.
 */
data class HabitDailyRollup(
    val dateEpochDays: Int,
    val habitKey: String,
    val title: String,
    val doneMinutes: Int
)

/** Slim view of a done item used to build Reflect highlights. */
data class DoneItemSummary(
    val id: Long,
    val dateEpochDays: Int,
    val title: String,
    val note: String?,
    val sourceName: String,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val completedAtMillis: Long?
) {
    val date: LocalDate get() = LocalDate.fromEpochDays(dateEpochDays)
    val minutes: Int
        get() {
            val start = startTimeMinutes ?: return 0
            val end = endTimeMinutes ?: return 0
            return (end - start).coerceAtLeast(0)
        }
}
