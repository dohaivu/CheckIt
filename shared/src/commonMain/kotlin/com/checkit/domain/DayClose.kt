package com.checkit.domain

import kotlinx.datetime.LocalDate

/**
 * Actions for unfinished (Planned) items during evening review.
 * [CarryOver] is shared with PR2 morning leftovers.
 */
enum class LeftoverAction {
    /** No decision yet; the item is left untouched on the review day. */
    None,

    /** Mark the plan item Done. Does not complete the linked task. */
    MarkDone,

    /** Copy the item onto the next day as Planned. */
    CarryOver,

    /** Leave Planned on the review day; do not copy. */
    Drop
}

/** Default action for a planned leftover item during evening review. */
fun DailyPlanItem.defaultLeftoverAction(): LeftoverAction = LeftoverAction.None

/**
 * Pre-selected action when the review reopens. Handled items from an earlier
 * review re-open as their prior choice: CarryOver if a copy exists on the next
 * day (via [carriedFromItemId]), otherwise Drop. Unhandled items stay None so
 * the user picks explicitly.
 */
fun DailyPlanItem.defaultReviewAction(dailyPlans: List<DailyPlan>): LeftoverAction {
    if (handledAtMillis == null) return LeftoverAction.None
    val tomorrowEpochDay = dateEpochDays + 1
    val isCarried = dailyPlans.any { plan ->
        plan.date.toEpochDays().toInt() == tomorrowEpochDay &&
            plan.items.any { it.carriedFromItemId == id }
    }
    return if (isCarried) LeftoverAction.CarryOver else LeftoverAction.Drop
}

enum class CarryOverTimePolicy {
    /** Clear start/end so the next day can be re-timed (review default). */
    ClearTimes,

    /** Keep original start/end on the target date. */
    KeepTimes
}

data class DayCloseTagMinutes(
    val tagId: Long,
    val name: String,
    val color: String,
    val totalMinutes: Int
)

data class DayCloseSummary(
    val date: LocalDate,
    val doneCount: Int,
    val plannedCount: Int,
    val doneMinutes: Int,
    val plannedItems: List<DailyPlanItem>,
    val doneItems: List<DailyPlanItem>,
    val topTags: List<DayCloseTagMinutes>,
    /** Items already resolved (e.g. carried to tomorrow) by an earlier review; still re-decidable. */
    val alreadyCarriedItems: List<DailyPlanItem> = emptyList()
)

data class DayCloseConfirmInput(
    val date: LocalDate,
    val leftoverActions: Map<Long, LeftoverAction>,
    val winNote: String? = null,
    val tomorrowGoal: String? = null,
    val rating: Float = 0f
)

data class DayCloseConfirmResult(
    val markedDoneCount: Int,
    val carriedCount: Int,
    val droppedCount: Int,
    val winNoteSaved: Boolean
)

/** Outcome of persisting a complete day review. */
data class DayCloseCommitResult(
    val carriedCount: Int,
    val skippedCount: Int
)

data class CarryOverResult(
    val carriedCount: Int,
    val skippedCount: Int,
    val newItemIds: List<Long>
)
