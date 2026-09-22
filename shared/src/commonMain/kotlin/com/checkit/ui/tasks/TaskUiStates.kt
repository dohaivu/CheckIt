package com.checkit.ui.tasks

import com.checkit.domain.ActiveTagToken
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DueDatePreset
import com.checkit.domain.ListItem
import com.checkit.domain.ListSection
import com.checkit.domain.NoteItem
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskType
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
    val recentLabels: List<String> = emptyList(),
    val isLoading: Boolean = true
) {
    val selectedListId: String? get() = selection.selectedListId
    val selectedFilterId: String? get() = options.selectedFilterId
    val selectedTagId: String? get() = selection.selectedTagId
    val selectedView: TaskWorkspaceView get() = options.selectedView
    val listDisplayType: TaskListDisplayType get() = options.listDisplayType
    val showCompleted: Boolean get() = options.showCompleted
    val searchText: String get() = options.searchText
    val sortOption: TaskSortOption get() = options.sortOption
    val visibleTasks: List<TaskItem> get() = visibleItems.tasks
    val visibleNotes: List<NoteItem> get() = visibleItems.notes
    val visibleListItems: List<TaskListEntry> get() = visibleItems.listItems
    val selectedList: ListItem? = board.lists.firstOrNull { it.id == selectedListId }
    val selectedFilter: TaskFilter? = board.filters.firstOrNull { it.id == selectedFilterId }
    val selectedTag = board.tags.firstOrNull { it.id == selectedTagId }
    val dayLimit: Int? = if (selectedFilter?.dueDatePreset == DueDatePreset.Today) 1 else null
    val availableViews: List<TaskWorkspaceView> = TaskWorkspaceView.entries
        .filter { it != TaskWorkspaceView.Timeline || dayLimit == 1 }
}

data class TaskSelectionState(
    val selectedListId: String? = null,
    val selectedTagId: String? = null
)

data class TaskViewOptionsState(
    val selectedView: TaskWorkspaceView = TaskWorkspaceView.List,
    val listDisplayType: TaskListDisplayType = TaskListDisplayType.Standard,
    val showCompleted: Boolean = false,
    val searchText: String = "",
    val sortOption: TaskSortOption = TaskSortOption.Custom,
    val selectedFilterId: String? = null,
    val selectedTagIds: Set<String> = emptySet()
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

    data class SectionHeader(val section: ListSection?) : TaskListEntry {
        override val key: String = "section-${section?.id ?: "none"}"
    }

    data object PinnedHeader : TaskListEntry {
        override val key: String = "pinned-header"
    }
}

enum class TaskWorkspaceView {
    List,
    Agenda,
    Timeline,
    Habits;

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
        val taskId: String? = null,
        val listId: String? = null,
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
        val type: TaskType = TaskType.Task,
        val label: String? = null,
        val isPinned: Boolean = false,
        val selectedTagIds: Set<String> = emptySet(),
        val addToMyDayOnSave: Boolean = false,
        val dailyPlanItem: DailyPlanItem? = null,
        val upgradeDailyPlanItemId: String? = null,
        val trashedAtMillis: Long? = null,
        val error: String? = null
    ) : TaskEditorState

    data class NoteForm(
        val mode: EditorMode,
        val noteId: String? = null,
        val listId: String? = null,
        val title: String = "",
        val content: String = "",
        val status: TaskStatus = TaskStatus.Open,
        val date: LocalDate? = null,
        val startTimeMinutes: Int? = null,
        val label: String? = null,
        val isPinned: Boolean = false,
        val selectedTagIds: Set<String> = emptySet(),
        val trashedAtMillis: Long? = null,
        val error: String? = null
    ) : TaskEditorState
}

data class SubTaskEditorState(
    val id: String? = null,
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

data class TagEditorState(
    val mode: EditorMode,
    val tagId: String? = null,
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
