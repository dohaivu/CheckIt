package com.checkit.ui

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import kotlinx.datetime.LocalDate

data class MyDayUiState(
    val board: TaskBoard = TaskBoard(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val selectedView: MyDayView = MyDayView.Timeline,
    val itemEditor: DailyPlanItemEditorState? = null,
    val showSuggestions: Boolean = false,
    val suggestionStartTimeMinutes: Int? = null,
    val suggestionEndTimeMinutes: Int? = null,
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val today: LocalDate = com.checkit.ui.today()
    val plan: DailyPlan? = dailyPlans.firstOrNull { it.date == today }
    val items: List<DailyPlanItem> = plan?.items.orEmpty()
    val plannedItems: List<DailyPlanItem> = items.filter { it.status != DailyPlanItemStatus.Done }
    val doneItems: List<DailyPlanItem> = items.filter { it.status == DailyPlanItemStatus.Done }
    val suggestedTasks: List<TaskItem> = board.tasks
        .filter { task ->
            !task.isTrashed &&
                task.status != TaskStatus.Completed
        }
        .sortedWith(compareBy<TaskItem> { it.doDate ?: LocalDate.fromEpochDays(Int.MAX_VALUE) }.thenBy { it.sortOrder })
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
    val source: DailyPlanItemSource = DailyPlanItemSource.CheckInManualDone,
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
}
