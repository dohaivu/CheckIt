package com.checkit.ui

import com.checkit.domain.ActiveTagToken
import com.checkit.domain.DueDatePreset
import com.checkit.domain.NoteItem
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.components.ReportPeriod
import kotlinx.datetime.LocalDate

data class TaskUiState(
    val activeTagToken: ActiveTagToken? = null,
    val tagSuggestions: List<String> = emptyList(),
    val board: TaskBoard = TaskBoard(),
    val selectedListId: Long? = null,
    val selectedFilterId: Long? = null,
    val selectedTagId: Long? = null,
    val selectedView: TaskWorkspaceView = TaskWorkspaceView.List,
    val visibleTasks: List<TaskItem> = emptyList(),
    val visibleNotes: List<NoteItem> = emptyList(),
    val editor: TaskEditorState? = null,
    val listEditor: ListEditorState? = null,
    val tagEditor: TagEditorState? = null,
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val selectedList: TaskList? = board.lists.firstOrNull { it.id == selectedListId }
    val selectedFilter: TaskFilter? = board.filters.firstOrNull { it.id == selectedFilterId }
    val selectedTag = board.tags.firstOrNull { it.id == selectedTagId }
    val title: String = selectedList?.name ?: selectedFilter?.name ?: selectedTag?.name ?: "Tasks"
    val isTodayFilterSelected: Boolean = selectedFilter?.dueDatePreset == DueDatePreset.Today
    val availableViews: List<TaskWorkspaceView> = if (isTodayFilterSelected) {
        TaskWorkspaceView.entries
    } else {
        TaskWorkspaceView.entries.filter { it != TaskWorkspaceView.Timeline }
    }
}

enum class TaskWorkspaceView {
    List,
    Agenda,
    Timeline
}

sealed interface TaskEditorState {
    data class TaskForm(
        val mode: EditorMode,
        val taskId: Long? = null,
        val listId: Long,
        val name: String = "",
        val description: String = "",
        val dueDate: LocalDate? = null,
        val startTimeMinutes: Int? = null,
        val endTimeMinutes: Int? = null,
        val repeatPreset: RepeatPreset = RepeatPreset.None,
        val subtasks: List<SubTaskEditorState> = emptyList(),
        val status: TaskStatus = TaskStatus.Open,
        val priority: TaskPriority = TaskPriority.None,
        val selectedTagIds: Set<Long> = emptySet()
    ) : TaskEditorState {
        val durationMinutes: Int?
            get() = calculateDurationMinutes(startTimeMinutes, endTimeMinutes)
    }

    data class NoteForm(
        val mode: EditorMode,
        val noteId: Long? = null,
        val listId: Long,
        val content: String = "",
        val date: LocalDate,
        val selectedTagIds: Set<Long> = emptySet()
    ) : TaskEditorState
}

enum class EditorMode {
    Add,
    View,
    Edit
}

data class SubTaskEditorState(
    val id: Long? = null,
    val name: String,
    val isCompleted: Boolean = false
)

fun SubTaskItem.toEditorState() = SubTaskEditorState(
    id = id,
    name = name,
    isCompleted = isCompleted
)

data class ListEditorState(
    val mode: EditorMode,
    val listId: Long? = null,
    val name: String = "",
    val color: String = ListEditorDefaults.Colors.first(),
    val icon: String = ListEditorDefaults.Icons.first()
)

data class TagEditorState(
    val mode: EditorMode,
    val tagId: Long? = null,
    val name: String = "",
    val color: String = TagEditorDefaults.Colors.first()
)

object ListEditorDefaults {
    val Colors: List<String> = listOf(
        "#2563EB",
        "#7C3AED",
        "#059669",
        "#DC2626",
        "#CA8A04",
        "#0891B2",
        "#DB2777",
        "#64748B"
    )
    val Icons: List<String> = listOf(
        "Inbox",
        "Home",
        "Work",
        "Folder",
        "TaskAlt",
        "Notes",
        "Today",
        "Schedule",
        "ShoppingCart",
        "Flight",
        "School",
        "Star"
    )
}

object TagEditorDefaults {
    val Colors: List<String> = ListEditorDefaults.Colors
}

enum class RepeatPreset(
    val label: String,
    val rrule: String?
) {
    None("Does not repeat", null),
    EveryDay("Every day", "FREQ=DAILY;INTERVAL=1"),
    EveryWeek("Every week", "FREQ=WEEKLY;INTERVAL=1"),
    EveryMonth("Every month", "FREQ=MONTHLY;INTERVAL=1");

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

data class CalendarUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.Month,
    val selectedMonth: kotlinx.datetime.LocalDate = today().firstDayOfMonth(),
    val selectedDate: kotlinx.datetime.LocalDate = today(),
    val calendarData: CalendarData = CalendarData()
)


data class CalendarData(
    val monthTransactionCount: Int = 0,
    val headerIndexes: Map<kotlinx.datetime.LocalDate, Int> = emptyMap(),
    val filteredMonthTotal: Long = 0L
)

data class ReportUiState(
    val selectedMonth: kotlinx.datetime.LocalDate = today().firstDayOfMonth(),
)

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.English,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val colorSchemeMode: AppColorSchemeMode = AppColorSchemeMode.Sunset,
    val tagUsageSort: TagUsageSort = TagUsageSort.MostUsed,
    val message: String? = null
)

enum class TagUsageSort {
    MostUsed,
    HighestSpending,
    RecentlyUsed,
    Alphabetical
}
