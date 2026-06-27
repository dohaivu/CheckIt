package com.checkit.ui

import com.checkit.domain.ActiveTagToken
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DueDatePreset
import com.checkit.domain.NoteItem
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.theme.AppIconColorDefaults
import kotlinx.datetime.LocalDate

data class TaskUiState(
    val activeTagToken: ActiveTagToken? = null,
    val tagSuggestions: List<String> = emptyList(),
    val board: TaskBoard = TaskBoard(),
    val selection: TaskSelectionState = TaskSelectionState(),
    val options: TaskViewOptionsState = TaskViewOptionsState(),
    val visibleItems: TaskVisibleItemsState = TaskVisibleItemsState(),
    val editor: TaskEditorState? = null,
    val isLoading: Boolean = true
) {
    val selectedListId: Long? get() = selection.selectedListId
    val selectedGoalId: Long? get() = selection.selectedGoalId
    val selectedFilterId: Long? get() = options.selectedFilterId
    val selectedTagId: Long? get() = selection.selectedTagId
    val selectedView: TaskWorkspaceView get() = options.selectedView
    val listDisplayType: TaskListDisplayType get() = options.listDisplayType
    val showCompleted: Boolean get() = options.showCompleted
    val searchText: String get() = options.searchText
    val sortOption: TaskSortOption get() = options.sortOption
    val visibleTasks: List<TaskItem> get() = visibleItems.tasks
    val visibleNotes: List<NoteItem> get() = visibleItems.notes
    val visibleListItems: List<TaskListEntry> get() = visibleItems.listItems
    val selectedList: Objective? = board.objectives.firstOrNull { it.id == selectedListId }
    val selectedGoal = board.goals.firstOrNull { it.id == selectedGoalId }
    val selectedFilter: TaskFilter? = board.filters.firstOrNull { it.id == selectedFilterId }
    val selectedTag = board.tags.firstOrNull { it.id == selectedTagId }
    val title: String = selectedGoal?.title ?: selectedList?.name ?: selectedTag?.name ?: "All tasks"
    val dayLimit: Int? = if (selectedFilter?.dueDatePreset == DueDatePreset.Today) 1 else null
    val availableViews: List<TaskWorkspaceView> = TaskWorkspaceView.entries
        .filter { it != TaskWorkspaceView.Timeline || dayLimit == 1 }
        .filter { it != TaskWorkspaceView.Goal || selectedGoal != null }
}

data class TaskSelectionState(
    val selectedGoalId: Long? = null,
    val selectedListId: Long? = null,
    val selectedTagId: Long? = null
)

data class TaskViewOptionsState(
    val selectedView: TaskWorkspaceView = TaskWorkspaceView.List,
    val listDisplayType: TaskListDisplayType = TaskListDisplayType.Standard,
    val showCompleted: Boolean = false,
    val searchText: String = "",
    val sortOption: TaskSortOption = TaskSortOption.Custom,
    val selectedFilterId: Long? = null
)

data class TaskVisibleItemsState(
    val tasks: List<TaskItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val listItems: List<TaskListEntry> = emptyList()
)

sealed interface TaskListEntry {
    val key: String

    data class Task(val item: TaskItem) : TaskListEntry {
        override val key: String = "task-${item.id}"
    }

    data class Note(val item: NoteItem) : TaskListEntry {
        override val key: String = "note-${item.id}"
    }
}

enum class TaskWorkspaceView {
    List,
    Agenda,
    Goal,
    Timeline;

    companion object {
        fun fromCode(code: String): TaskWorkspaceView =
            entries.firstOrNull { it.name == code } ?: List
    }
}

enum class TaskListDisplayType {
    Brief,
    Standard,
    Detail;

    companion object {
        fun fromCode(code: String): TaskListDisplayType =
            entries.firstOrNull { it.name == code } ?: Standard
    }
}

enum class TaskSortOption {
    Custom,
    Priority,
    Title,
    Date;

    companion object {
        fun fromCode(code: String): TaskSortOption =
            entries.firstOrNull { it.name == code } ?: Custom
    }
}

enum class EditorMode {
    Add,
    View,
    Edit
}

sealed interface TaskEditorState {
    data class TaskForm(
        val mode: EditorMode,
        val taskId: Long? = null,
        val objectiveId: Long,
        val keyResultId: Long? = null,
        val name: String = "",
        val description: String = "",
        val doDate: LocalDate? = null,
        val startTimeMinutes: Int? = null,
        val endTimeMinutes: Int? = null,
        val repeatPreset: RepeatPreset = RepeatPreset.None,
        val subtasks: List<SubTaskEditorState> = emptyList(),
        val reminderOffsets: Set<Int> = emptySet(),
        val status: TaskStatus = TaskStatus.Open,
        val priority: TaskPriority = TaskPriority.None,
        val selectedTagIds: Set<Long> = emptySet(),
        val addToMyDayOnSave: Boolean = false,
        val dailyPlanItem: DailyPlanItem? = null,
        val trashedAtMillis: Long? = null
    ) : TaskEditorState {
        val durationMinutes: Int?
            get() = calculateDurationMinutes(startTimeMinutes, endTimeMinutes)
    }

    data class NoteForm(
        val mode: EditorMode,
        val noteId: Long? = null,
        val objectiveId: Long,
        val title: String = "",
        val content: String = "",
        val status: TaskStatus = TaskStatus.Open,
        val date: LocalDate,
        val startTimeMinutes: Int? = null,
        val selectedTagIds: Set<Long> = emptySet(),
        val trashedAtMillis: Long? = null
    ) : TaskEditorState
}

data class SubTaskEditorState(
    val id: Long? = null,
    val name: String,
    val isCompleted: Boolean = false,
    val editorKey: Long = nextSubTaskEditorKey()
)

fun SubTaskItem.toEditorState() = SubTaskEditorState(
    id = id,
    name = name,
    isCompleted = isCompleted
)

private var subTaskEditorKeySeed = 0L

private fun nextSubTaskEditorKey(): Long = --subTaskEditorKeySeed

data class ObjectiveEditorState(
    val mode: EditorMode,
    val objectiveId: Long? = null,
    val goalId: Long? = null,
    val name: String = "",
    val color: String = AppIconColorDefaults.ListColors.first(),
    val icon: String = AppIconColorDefaults.ListIcons.first(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

data class TagEditorState(
    val mode: EditorMode,
    val tagId: Long? = null,
    val name: String = "",
    val color: String = AppIconColorDefaults.ListColors.first()
)

enum class RepeatPreset(
    val label: String,
    val rrule: String?
) {
    None("Does not repeat", null),
    EveryDay("Everyday", "FREQ=DAILY;INTERVAL=1"),
    EveryWeek("Weekly", "FREQ=WEEKLY;INTERVAL=1"),
    EveryMonth("Monthly", "FREQ=MONTHLY;INTERVAL=1");

    companion object {
        fun fromRRule(rrule: String?): RepeatPreset =
            entries.firstOrNull { it.rrule == rrule } ?: None
    }
}

private fun calculateDurationMinutes(startTimeMinutes: Int?, endTimeMinutes: Int?): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
    return if (end >= start) {
        end - start
    } else {
        24 * 60 - start + end
    }
}
