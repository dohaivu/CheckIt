package com.checkit.ui.myday

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayCloseSummary
import com.checkit.domain.JournalEntry
import com.checkit.domain.LeftoverAction
import com.checkit.domain.NoteItem
import com.checkit.domain.Period
import com.checkit.domain.PeriodGoal
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import com.checkit.domain.TagItem
import com.checkit.domain.defaultLeftoverAction
import com.checkit.domain.endDateInclusive
import com.checkit.domain.startOf
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.isOverdue
import com.checkit.ui.today
import com.checkit.ui.currentMyDayTimeMinutes
import kotlinx.datetime.LocalDate

sealed class SprintChoice {
    data class Task(val task: TaskItem) : SprintChoice()
    data class PlanItem(val item: DailyPlanItem, val task: TaskItem? = null) : SprintChoice()
}

sealed interface FabAction {
    data object QuickSprint : FabAction
    data class TagSprint(val tag: TagItem) : FabAction
}

data class MyDayUiState(
    val tasks: List<TaskItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val tags: List<TagItem> = emptyList(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val selectedView: MyDayView = MyDayView.Timeline,
    val itemEditor: DailyPlanItemEditorState? = null,
    val dayClose: DayCloseUiState? = null,
    val pendingYesterdayLeftovers: List<DailyPlanItem> = emptyList(),
    val showSuggestions: Boolean = false,
    val showQuickSprintSheet: Boolean = false,
    val showCelebration: Boolean = false,
    val suggestionStartTimeMinutes: Int? = null,
    val suggestionEndTimeMinutes: Int? = null,
    val recentTags: List<TagItem> = emptyList(),
    val lastFabAction: FabAction = FabAction.QuickSprint,
    val periodGoals: List<PeriodGoal> = emptyList(),
    /** Journal entries for today. */
    val journalEntries: List<JournalEntry> = emptyList(),
    val journalEditor: JournalEntryEditorState? = null,
    val showJournalList: Boolean = false,
    val recentLabels: List<String> = emptyList(),
    val nowMinutes: Int = 0,
    val isLoading: Boolean = true
) {
    val today: LocalDate = today()
    val plan: DailyPlan? = dailyPlans.firstOrNull { it.date == today }
    val items: List<DailyPlanItem> = plan?.items.orEmpty()
    val plannedItems: List<DailyPlanItem> = items.filter { it.status != DailyPlanItemStatus.Done }
    val doneItems: List<DailyPlanItem> = items.filter { it.status == DailyPlanItemStatus.Done }

    /** True when nothing on today's plan sits within ±30 minutes of now (floating quick-add bar). */
    val showFloatingQuickAdd: Boolean =
        !hasDailyPlanItemNearby(items, currentMyDayTimeMinutes())

    val suggestedTasks: List<TaskItem> = tasks
        .filter { task ->
            !task.isTrashed &&
                task.status != TaskStatus.Completed
        }
        .sortedWith(
            compareBy<TaskItem> { task ->
                // Prefer tasks that appear as yesterday leftovers (linked).
                val leftoverTaskIds = pendingYesterdayLeftovers.mapNotNull { it.taskId }.toSet()
                if (task.id in leftoverTaskIds) 0 else 1
            }
                .thenBy { it.doDate ?: LocalDate.fromEpochDays(Int.MAX_VALUE) }
                .thenBy { it.sortOrder }
        )

    val sprintSuggestedToday: List<SprintChoice> = plannedItems
        .take(5)
        .map { item -> SprintChoice.PlanItem(item, tasks.find { it.id == item.taskId }) }

    val sprintSuggestedYesterday: List<SprintChoice> = pendingYesterdayLeftovers
        .take(3)
        .map { item -> SprintChoice.PlanItem(item, tasks.find { it.id == item.taskId }) }

    val sprintSuggestedTasks: List<TaskItem> = suggestedTasks
        .filter { task ->
            val excludedTaskIds = (sprintSuggestedToday + sprintSuggestedYesterday).mapNotNull { choice ->
                when (choice) {
                    is SprintChoice.Task -> choice.task.id
                    is SprintChoice.PlanItem -> choice.task?.id
                }
            }.toSet()
            task.id !in excludedTaskIds
        }
        .take(5)

    val continueSprintItem: SprintChoice? = doneItems.lastOrNull()?.let { item -> SprintChoice.PlanItem(item, tasks.find { it.id == item.taskId }) }
        ?: sprintSuggestedToday.firstOrNull()
        ?: sprintSuggestedYesterday.firstOrNull()
        ?: sprintSuggestedTasks.firstOrNull()?.let { SprintChoice.Task(it) }

    private val periodGoalIndex: Map<Pair<Period, Int>, PeriodGoal> by lazy {
        periodGoals.associateBy { it.period to it.startEpochDays }
    }

    fun goalFor(period: Period, date: LocalDate): PeriodGoal? {
        val startEpoch = period.startOf(date).toEpochDays().toInt()
        return periodGoalIndex[period to startEpoch]
    }

    val dayGoal: PeriodGoal? by lazy { goalFor(Period.Day, today) }
    val weekGoal: PeriodGoal? by lazy { goalFor(Period.Week, today) }
    val monthGoal: PeriodGoal? by lazy { goalFor(Period.Month, today) }

    fun goalFor(period: Period): PeriodGoal? = when (period) {
        Period.Day -> dayGoal
        Period.Week -> weekGoal
        Period.Month -> monthGoal
        else -> goalFor(period, today)
    }

    private fun bannerTypeForPeriod(period: Period, goal: PeriodGoal?): PeriodBannerType {
        val isReviewMissing = goal == null || goal.review.isBlank()
        val isGoalMissing = goal == null || (goal.goal.isNullOrBlank() && goal.metrics.isEmpty())

        val isReviewTime = when (period) {
            Period.Day -> nowMinutes >= 19 * 60 // 7 PM
            Period.Week,
            Period.Month -> today == period.endDateInclusive(today)
            else -> false
        }

        return when {
            isReviewTime && isReviewMissing -> PeriodBannerType.ReviewPending
            isGoalMissing -> PeriodBannerType.MissingGoal
            else -> PeriodBannerType.ActiveGoal
        }
    }

    fun bannerTypeFor(period: Period): PeriodBannerType =
        bannerTypeForPeriod(period, goalFor(period))

    val dayBannerType: PeriodBannerType by lazy { bannerTypeForPeriod(Period.Day, dayGoal) }
    val weekBannerType: PeriodBannerType by lazy { bannerTypeForPeriod(Period.Week, weekGoal) }
    val monthBannerType: PeriodBannerType by lazy { bannerTypeForPeriod(Period.Month, monthGoal) }

    /** Lines from today's goal, trimmed and non-blank, for quick-add suggestions. */
    val goalSuggestions: List<String> by lazy {
        dayGoal?.goal
            ?.lines()
            ?.map { it.trim().removePrefix("- ") }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }
}

enum class PeriodBannerType {
    MissingGoal,
    ActiveGoal,
    ReviewPending
}

data class DayCloseUiState(
    val summary: DayCloseSummary,
    val leftoverActions: Map<Long, LeftoverAction> = emptyMap(),
    val winNote: String = "",
    val tomorrowGoal: String = "",
    val isSubmitting: Boolean = false
) {
    fun actionFor(item: DailyPlanItem): LeftoverAction =
        leftoverActions[item.id] ?: item.defaultLeftoverAction()
}

enum class MyDayView {
    Agenda,
    Timeline,
    Board
}

/** Bottom-sheet editor state for a single journal entry. */
data class JournalEntryEditorState(
    val entryId: Long? = null,
    val date: LocalDate = today(),
    val label: String = "",
    val content: String = "",
    val prompt: String = "",
    val moods: List<String> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet()
) {
    val isEditMode: Boolean get() = entryId != null
}

data class DailyPlanItemEditorState(
    val mode: EditorMode = EditorMode.Add,
    val itemId: Long? = null,
    val taskId: Long? = null,
    val nestedListItemId: Long? = null,
    val date: LocalDate = today(),
    val source: DailyPlanItemSource = DailyPlanItemSource.MyDayTask,
    val title: String = "",
    val note: String = "",
    val status: DailyPlanItemStatus = DailyPlanItemStatus.Done,
    val label: String? = null,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val selectedTagIds: Set<Long> = emptySet(),
    val error: String? = null
) {
    val isAddMode: Boolean get() = mode == EditorMode.Add
    val isEditMode: Boolean get() = mode == EditorMode.Edit
    val canDelete: Boolean get() = itemId != null
    val isOverdue: Boolean
        get() = date.isOverdue(
            today = today(),
            deadline = endTimeMinutes ?: startTimeMinutes,
            isCompleted = status == DailyPlanItemStatus.Done
        )

    fun saveSource(): DailyPlanItemSource = source

    fun saveStatus(): DailyPlanItemStatus =
        if (isAddMode) {
            if (source == DailyPlanItemSource.MyDayNote) DailyPlanItemStatus.Done
            else source.inferredAddStatus(startTimeMinutes)
        } else status
}

fun DailyPlanItemSource.inferredAddStatus(startTimeMinutes: Int?): DailyPlanItemStatus =
    if (infersAddStatusFromStartTime() && startTimeMinutes != null && startTimeMinutes < com.checkit.ui.currentMyDayTimeMinutes()) {
        DailyPlanItemStatus.Done
    } else {
        DailyPlanItemStatus.Planned
    }

fun DailyPlanItemSource.infersAddStatusFromStartTime(): Boolean =
    this == DailyPlanItemSource.MyDayTask || this == DailyPlanItemSource.MyDayReminder

fun DailyPlanItemSource.defaultStatus(): DailyPlanItemStatus = when (this) {
    DailyPlanItemSource.MyDayNote,
    DailyPlanItemSource.MyDayReminder -> DailyPlanItemStatus.Planned
    else -> DailyPlanItemStatus.Done
}

fun DailyPlan?.doneWorkMinutes(): Int =
    this
        ?.items
        .orEmpty()
        .filter { it.status == DailyPlanItemStatus.Done }
        .sumOf { it.workMinutes() }

/**
 * True when any timed plan item overlaps the [nowMinutes] ± [windowMinutes] window.
 * Items without a start time (all-day/unplanned) never count as "nearby".
 */
fun hasDailyPlanItemNearby(
    items: List<DailyPlanItem>,
    nowMinutes: Int,
    windowMinutes: Int = 15
): Boolean {
    val windowStart = nowMinutes - windowMinutes
    val windowEnd = nowMinutes + windowMinutes
    return items.any { item ->
        val start = item.startTimeMinutes ?: return@any false
        val end = item.endTimeMinutes ?: start
        start <= windowEnd && end >= windowStart
    }
}
