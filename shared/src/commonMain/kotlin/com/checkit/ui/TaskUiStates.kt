package com.checkit.ui

import com.checkit.domain.ActiveTagToken
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DueDatePreset
import com.checkit.domain.NoteItem
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.theme.AppIconColorDefaults
import kotlinx.datetime.LocalDate

data class TaskUiState(
    val activeTagToken: ActiveTagToken? = null,
    val tagSuggestions: List<String> = emptyList(),
    val board: TaskBoard = TaskBoard(),
    val selectedListId: Long? = null,
    val selectedFilterId: Long? = null,
    val selectedTagId: Long? = null,
    val selectedView: TaskWorkspaceView = TaskWorkspaceView.List,
    val listDisplayType: TaskListDisplayType = TaskListDisplayType.Standard,
    val showCompleted: Boolean = false,
    val sortOption: TaskSortOption = TaskSortOption.Custom,
    val visibleTasks: List<TaskItem> = emptyList(),
    val visibleNotes: List<NoteItem> = emptyList(),
    val visibleListItems: List<TaskListEntry> = emptyList(),
    val editor: TaskEditorState? = null,
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val selectedList: TaskList? = board.lists.firstOrNull { it.id == selectedListId }
    val selectedFilter: TaskFilter? = board.filters.firstOrNull { it.id == selectedFilterId }
    val selectedTag = board.tags.firstOrNull { it.id == selectedTagId }
    val title: String = selectedList?.name ?: selectedFilter?.name ?: selectedTag?.name ?: "Tasks"
    val dayLimit: Int? = if (selectedFilter?.dueDatePreset == DueDatePreset.Today) 1 else null
    val availableViews: List<TaskWorkspaceView> = if (dayLimit == 1) {
        TaskWorkspaceView.entries
    } else {
        TaskWorkspaceView.entries.filter { it != TaskWorkspaceView.Timeline }
    }
}

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
        val listId: Long,
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
        val dailyPlanItem: DailyPlanItem? = null,
        val trashedAtMillis: Long? = null
    ) : TaskEditorState {
        val durationMinutes: Int?
            get() = calculateDurationMinutes(startTimeMinutes, endTimeMinutes)
    }

    data class NoteForm(
        val mode: EditorMode,
        val noteId: Long? = null,
        val listId: Long,
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

data class ListEditorState(
    val mode: EditorMode,
    val listId: Long? = null,
    val name: String = "",
    val color: String = AppIconColorDefaults.ListColors.first(),
    val icon: String = AppIconColorDefaults.ListIcons.first()
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
