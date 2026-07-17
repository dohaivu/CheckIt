package com.checkit.domain

import kotlinx.datetime.LocalDate

/**
 * Actions for unfinished (Planned) items during evening review.
 * [CarryOver] is shared with PR2 morning leftovers.
 */
enum class LeftoverAction {
    /** Mark the plan item Done. Does not complete the linked task. */
    MarkDone,

    /** Copy the item onto the next day as Planned. */
    CarryOver,

    /** Leave Planned on the review day; do not copy. */
    Drop
}

enum class CarryOverTimePolicy {
    /** Clear start/end so the next day can be re-timed (review default). */
    ClearTimes,

    /** Keep original start/end on the target date. */
    KeepTimes
}

data class DayReviewTagMinutes(
    val tagId: Long,
    val name: String,
    val color: String,
    val totalMinutes: Int
)

data class DayReviewSummary(
    val date: LocalDate,
    val doneCount: Int,
    val plannedCount: Int,
    val doneMinutes: Int,
    val plannedItems: List<DailyPlanItem>,
    val doneItems: List<DailyPlanItem>,
    val topTags: List<DayReviewTagMinutes>,
    /** Existing win-of-day note on this plan, if any. */
    val winNoteItemId: Long? = null,
    val winNote: String = ""
)

data class DayReviewConfirmInput(
    val date: LocalDate,
    val leftoverActions: Map<Long, LeftoverAction>,
    val winNote: String? = null,
    val winNoteItemId: Long? = null,
    val tomorrowGoal: String? = null
)

data class DayReviewConfirmResult(
    val markedDoneCount: Int,
    val carriedCount: Int,
    val droppedCount: Int,
    val winNoteSaved: Boolean
)

/** Win-of-day note stored as a My Day note with a fixed title. */
object DayReviewWinNote {
    const val Title = "Win"

    fun findItem(plan: DailyPlan?): DailyPlanItem? =
        plan?.items
            .orEmpty()
            .asSequence()
            .filter { it.source == DailyPlanItemSource.MyDayNote && it.title == Title }
            .maxByOrNull { it.addedAtMillis }

    fun textOf(item: DailyPlanItem?): String =
        item?.note?.trim().orEmpty()
}

data class CarryOverResult(
    val carriedCount: Int,
    val skippedCount: Int,
    val newItemIds: List<Long>
)

/** Banner / auto-prompt rules for day review. */
object DayReviewBannerPolicy {
    fun shouldShow(
        hasPlanItems: Boolean,
        reviewReminderEnabled: Boolean,
        reviewReminderTimeMinutes: Int,
        lastDayReviewEpochDay: Int?,
        todayEpochDay: Int,
        nowMinutes: Int
    ): Boolean {
        if (!hasPlanItems) return false
        if (!reviewReminderEnabled) return false
        if (lastDayReviewEpochDay == todayEpochDay) return false
        val threshold = reviewReminderTimeMinutes.coerceIn(0, MinutesPerDay - 1)
        return nowMinutes >= threshold
    }

    private const val MinutesPerDay = 24 * 60
}

fun DailyPlanItem.planWorkMinutes(): Int {
    val start = startTimeMinutes ?: return 0
    val end = endTimeMinutes ?: return 0
    return (end - start).coerceAtLeast(0)
}
