package com.checkit.ui.myday

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayReviewSummary
import com.checkit.domain.LeftoverAction
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import com.checkit.domain.YesterdayLeftovers
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.today
import kotlinx.datetime.LocalDate

sealed class SprintChoice {
    data class Task(val task: TaskItem) : SprintChoice()
    data class PlanItem(val item: DailyPlanItem, val task: TaskItem? = null) : SprintChoice()
}

data class MyDayUiState(
    val board: TaskBoard = TaskBoard(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val selectedView: MyDayView = MyDayView.Timeline,
    val itemEditor: DailyPlanItemEditorState? = null,
    val dayReview: DayReviewUiState? = null,
    val showDayReviewBanner: Boolean = false,
    val reviewReminderEnabled: Boolean = true,
    val reviewReminderTimeMinutes: Int = 21 * 60,
    val planReminderEnabled: Boolean = true,
    val planReminderTimeMinutes: Int = 7 * 60,
    val lastDayReviewEpochDay: Int? = null,
    val lastDayPlanDismissedEpochDay: Int? = null,
    val leftoversBannerDismissedEpochDay: Int? = null,
    val autoCarryOverLeftovers: Boolean = false,
    val yesterdayLeftovers: List<DailyPlanItem> = emptyList(),
    val pendingYesterdayLeftovers: List<DailyPlanItem> = emptyList(),
    val showLeftoversBanner: Boolean = false,
    val showLeftoversSheet: Boolean = false,
    val showPlanAssistBanner: Boolean = false,
    val showSuggestions: Boolean = false,
    val showQuickSprintSheet: Boolean = false,
    val showCelebration: Boolean = false,
    val suggestionStartTimeMinutes: Int? = null,
    val suggestionEndTimeMinutes: Int? = null,
    val isLoading: Boolean = true
) {
    val today: LocalDate = today()
    val plan: DailyPlan? = dailyPlans.firstOrNull { it.date == today }
    val items: List<DailyPlanItem> = plan?.items.orEmpty()
    val plannedItems: List<DailyPlanItem> = items.filter { it.status != DailyPlanItemStatus.Done }
    val doneItems: List<DailyPlanItem> = items.filter { it.status == DailyPlanItemStatus.Done }
    val suggestedTasks: List<TaskItem> = board.tasks
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

    val yesterdayDate: LocalDate get() = YesterdayLeftovers.sourceDate(today)

    val sprintSuggestedToday: List<SprintChoice> = plannedItems
        .take(3)
        .map { item -> SprintChoice.PlanItem(item, board.tasksById[item.taskId]) }

    val sprintSuggestedYesterday: List<SprintChoice> = pendingYesterdayLeftovers
        .take(3)
        .map { item -> SprintChoice.PlanItem(item, board.tasksById[item.taskId]) }

    val continueSprintItem: SprintChoice? = doneItems.lastOrNull()?.let { SprintChoice.PlanItem(it, board.tasksById[it.taskId]) }
        ?: sprintSuggestedToday.firstOrNull()
        ?: sprintSuggestedYesterday.firstOrNull()
}

data class DayReviewUiState(
    val summary: DayReviewSummary,
    val leftoverActions: Map<Long, LeftoverAction> = emptyMap(),
    val winNote: String = "",
    val winNoteItemId: Long? = null,
    val tomorrowGoal: String = "",
    val isSubmitting: Boolean = false
) {
    fun actionFor(itemId: Long): LeftoverAction =
        leftoverActions[itemId] ?: LeftoverAction.CarryOver
}

enum class MyDayView {
    Agenda,
    Timeline,
    Board
}

data class DailyPlanItemEditorState(
    val mode: EditorMode = EditorMode.Add,
    val itemId: Long? = null,
    val taskId: Long? = null,
    val date: LocalDate = today(),
    val source: DailyPlanItemSource = DailyPlanItemSource.MyDayTask,
    val title: String = "",
    val note: String = "",
    val status: DailyPlanItemStatus = DailyPlanItemStatus.Done,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val selectedTagIds: Set<Long> = emptySet()
) {
    val isAddMode: Boolean get() = mode == EditorMode.Add
    val isEditMode: Boolean get() = mode == EditorMode.Edit
    val canDelete: Boolean get() = itemId != null

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

internal fun DailyPlanItem.workMinutes(): Int {
    val start = startTimeMinutes ?: return 0
    val end = endTimeMinutes ?: return 0
    return (end - start).coerceAtLeast(0)
}

internal fun DailyPlan?.doneWorkMinutes(): Int =
    this
        ?.items
        .orEmpty()
        .filter { it.status == DailyPlanItemStatus.Done }
        .sumOf { it.workMinutes() }
