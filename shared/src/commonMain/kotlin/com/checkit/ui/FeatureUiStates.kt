package com.checkit.ui

import androidx.compose.ui.graphics.Color
import com.checkit.domain.ActiveTagToken
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
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
    val listDisplayType: TaskListDisplayType = TaskListDisplayType.Standard,
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

enum class TaskListDisplayType {
    Brief,
    Standard,
    Detail
}

data class MyDayUiState(
    val board: TaskBoard = TaskBoard(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val selectedView: MyDayView = MyDayView.Agenda,
    val checkIn: CheckInState? = null,
    val showSuggestions: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val today: LocalDate = com.checkit.ui.today()
    val plan: DailyPlan? = dailyPlans.firstOrNull { it.date == today }
    val items: List<DailyPlanItem> = plan?.items.orEmpty()
    val plannedItems: List<DailyPlanItem> = items.filter { it.status != DailyPlanItemStatus.Done }
    val doneItems: List<DailyPlanItem> = items.filter { it.status == DailyPlanItemStatus.Done }
    val itemTaskIds: Set<Long> = items.mapNotNull { it.taskId }.toSet()
    val suggestedTasks: List<TaskItem> = board.tasks
        .filter { task ->
            !task.isTrashed &&
                task.status != TaskStatus.Completed &&
                task.id !in itemTaskIds
        }
        .sortedWith(compareBy<TaskItem> { it.dueDate ?: LocalDate.fromEpochDays(Int.MAX_VALUE) }.thenBy { it.sortOrder })
}

enum class MyDayView {
    Agenda,
    Timeline,
    Board
}

data class CheckInState(
    val doneTitle: String = "",
    val doneNote: String = "",
    val statusNote: String = "",
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null
)

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
        val reminderOffsets: Set<Int> = emptySet(),
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

data class CalendarUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.Month,
    val selectedMonth: kotlinx.datetime.LocalDate = today().firstDayOfMonth(),
    val selectedDate: kotlinx.datetime.LocalDate = today(),
    val board: TaskBoard = TaskBoard(),
    val dailyPlans: List<DailyPlan> = emptyList()
) {
    val listsById: Map<Long, TaskList> = board.lists.associateBy { it.id }
    val dailyPlanByDate: Map<kotlinx.datetime.LocalDate, DailyPlan> = dailyPlans.associateBy { it.date }

    fun tasksForDate(date: kotlinx.datetime.LocalDate): List<TaskItem> =
        board.tasks.filter { !it.isTrashed && it.dueDate == date }
            .sortedWith(compareBy<TaskItem> { it.sortOrder }.thenBy { it.dueDate })

    fun notesForDate(date: kotlinx.datetime.LocalDate): List<NoteItem> =
        board.notes.filter { !it.isTrashed && it.date == date }.sortedBy { it.sortOrder }

    fun markerColorsForDate(date: kotlinx.datetime.LocalDate): List<Color> {
        if (date <= today()) {
            val dailyItems = dailyPlanByDate[date]?.items.orEmpty()
            if (dailyItems.isNotEmpty()) {
                return dailyItems.map { dailyItemColor(it) }.take(MarkerCap)
            }
        }
        val tasks = tasksForDate(date)
        val notes = notesForDate(date)
        val combined = tasks.map { listColorFor(it.listId) } + notes.map { listColorFor(it.listId) }
        return if (combined.size <= MarkerCap) combined else combined.take(MarkerCap)
    }

    fun dailyPlanForDate(date: kotlinx.datetime.LocalDate): DailyPlan? = dailyPlanByDate[date]

    private fun listColorFor(listId: Long): Color =
        listsById[listId]?.color?.parseHexColorOrNull()
            ?: ListEditorDefaults.Colors.first().parseHexColorOrNull()
            ?: Color(0xFF64748B)

    private fun dailyItemColor(item: DailyPlanItem): Color =
        when (item.status) {
            DailyPlanItemStatus.Done -> Color(0xFF059669)
            DailyPlanItemStatus.InProgress -> Color(0xFF2563EB)
            DailyPlanItemStatus.Skipped, DailyPlanItemStatus.Moved -> Color(0xFF64748B)
            DailyPlanItemStatus.Planned -> item.taskId
                ?.let { taskId -> board.tasks.firstOrNull { it.id == taskId }?.listId }
                ?.let { listColorFor(it) }
                ?: Color(0xFFCA8A04)
        }

    private companion object {
        const val MarkerCap: Int = 6
    }
}

private fun String.parseHexColorOrNull(): Color? {
    val hex = removePrefix("#")
    val rgb = hex.toIntOrNull(16) ?: return null
    return Color(
        red = ((rgb shr 16) and 0xFF) / 255f,
        green = ((rgb shr 8) and 0xFF) / 255f,
        blue = (rgb and 0xFF) / 255f
    )
}

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
