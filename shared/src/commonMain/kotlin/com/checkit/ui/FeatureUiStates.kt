package com.checkit.ui

import com.checkit.domain.ActiveTagToken
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.ui.components.ReportPeriod

data class TaskUiState(
    val activeTagToken: ActiveTagToken? = null,
    val tagSuggestions: List<String> = emptyList(),
    val board: TaskBoard = TaskBoard(),
    val selectedListId: Long? = null,
    val selectedFilterId: Long? = null,
    val selectedView: TaskWorkspaceView = TaskWorkspaceView.List,
    val visibleTasks: List<TaskItem> = emptyList(),
    val visibleNotes: List<NoteItem> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val selectedList: TaskList? = board.lists.firstOrNull { it.id == selectedListId }
    val selectedFilter: TaskFilter? = board.filters.firstOrNull { it.id == selectedFilterId }
    val title: String = selectedList?.name ?: selectedFilter?.name ?: "Tasks"
}

enum class TaskWorkspaceView {
    List,
    Agenda,
    Timeline
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
