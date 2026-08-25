package com.checkit.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.JournalEntry
import com.checkit.domain.NoteItem
import com.checkit.domain.PeriodReview
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveDailyReflectStatsUseCase
import com.checkit.domain.usecase.ObserveJournalEntriesUseCase
import com.checkit.domain.usecase.ObserveNotesForDateUseCase
import com.checkit.domain.usecase.ObserveNotesInRangeUseCase
import com.checkit.domain.usecase.ObservePeriodReviewsUseCase
import com.checkit.domain.usecase.ObserveTasksForDateUseCase
import com.checkit.domain.usecase.ObserveTasksInRangeUseCase
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val observeDailyPlans: ObserveDailyPlansUseCase,
    private val observePeriodReviews: ObservePeriodReviewsUseCase,
    private val observeJournalEntries: ObserveJournalEntriesUseCase,
    private val observeDailyReflectStats: ObserveDailyReflectStatsUseCase,
    private val observeTasksInRange: ObserveTasksInRangeUseCase,
    private val observeNotesInRange: ObserveNotesInRangeUseCase,
    private val observeTasksForDate: ObserveTasksForDateUseCase,
    private val observeNotesForDate: ObserveNotesForDateUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    /** Tasks/notes observed over the recent window, keyed by date. */
    private val recentTasksByDate = MutableStateFlow<Map<LocalDate, List<TaskItem>>>(emptyMap())
    private val recentNotesByDate = MutableStateFlow<Map<LocalDate, List<NoteItem>>>(emptyMap())

    init {
        viewModelScope.launch {
            _uiState.map { it.selectedMonth }
                .distinctUntilChanged()
                .flatMapLatest { month ->
                    // Recent days (last 7 + future through the displayed window)
                    // are observed proactively so browsing them is instant and
                    // stays reactive; older aggregates come from the stats table.
                    val statsStart = month.minus(1, DateTimeUnit.MONTH)
                    val end = month.plus(2, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                    val recentStart = today().minus(6, DateTimeUnit.DAY)
                    combine(
                        observeDailyPlans(startDate = recentStart, endDate = end),
                        observePeriodReviews(statsStart, end),
                        observeJournalEntries(statsStart, end),
                        observeDailyReflectStats(statsStart, end),
                        combine(
                            observeTasksInRange(recentStart, end),
                            observeNotesInRange(recentStart, end)
                        ) { tasks, notes -> tasks to notes }
                    ) { dailyPlans, periodReviews, journalEntries, dailyStats, (recentTasks, recentNotes) ->
                        CalendarCombined(
                            dailyPlans,
                            periodReviews,
                            journalEntries,
                            dailyStats,
                            recentTasks.groupBy { it.doDate!! },
                            recentNotes.groupBy { it.date!! }
                        )
                    }
                }
                .catch { _ ->
                    _uiState.update { it.copy() }
                }
                .collect { combined ->
                    recentTasksByDate.value = combined.recentTasksByDate
                    recentNotesByDate.value = combined.recentNotesByDate
                    _uiState.update { state ->
                        state.copy(
                            dailyPlans = combined.dailyPlans,
                            periodReviews = combined.dayReviews,
                            journalEntries = combined.journalEntries,
                            dailyStatsByDate = combined.dailyStats.associateBy { it.date }
                        )
                    }
                }
        }

        // Selected date details: served from the observed recent window when the
        // date falls inside it, otherwise a dedicated single-date observation.
        viewModelScope.launch {
            _uiState.map { it.selectedDate }
                .distinctUntilChanged()
                .flatMapLatest { date ->
                    if (date >= today().minus(6, DateTimeUnit.DAY)) {
                        combine(recentTasksByDate, recentNotesByDate) { tasks, notes ->
                            tasks[date].orEmpty() to notes[date].orEmpty()
                        }
                    } else {
                        combine(
                            observeTasksForDate(date),
                            observeNotesForDate(date)
                        ) { tasks, notes -> tasks to notes }
                    }
                }
                .collect { (tasks, notes) ->
                    _uiState.update {
                        it.copy(selectedDateTasks = tasks, selectedDateNotes = notes)
                    }
                }
        }

        // Past-day agenda: plans within the recent window arrive via the main
        // flow; older dates need a one-off single-day plan fetch.
        viewModelScope.launch {
            _uiState.map { it.selectedDate }
                .distinctUntilChanged()
                .collect { date ->
                    val pastPlan = if (date < today().minus(6, DateTimeUnit.DAY)) {
                        observeDailyPlans(startDate = date, endDate = date).first().firstOrNull()
                    } else {
                        null
                    }
                    _uiState.update { it.copy(selectedDayPlan = pastPlan) }
                }
        }
    }

    fun previousPeriod() {
        _uiState.update { state ->
            when (state.calendarDisplayMode) {
                CalendarDisplayMode.Month -> state.copy(selectedMonth = state.selectedMonth.minus(1, DateTimeUnit.MONTH))
                CalendarDisplayMode.Week -> {
                    val selectedDate = state.selectedDate.minus(7, DateTimeUnit.DAY)
                    state.copy(
                        selectedDate = selectedDate,
                        selectedMonth = selectedDate.firstDayOfMonth()
                    )
                }
            }
        }
    }

    fun nextPeriod() {
        _uiState.update { state ->
            when (state.calendarDisplayMode) {
                CalendarDisplayMode.Month -> state.copy(selectedMonth = state.selectedMonth.plus(1, DateTimeUnit.MONTH))
                CalendarDisplayMode.Week -> {
                    val selectedDate = state.selectedDate.plus(7, DateTimeUnit.DAY)
                    state.copy(
                        selectedDate = selectedDate,
                        selectedMonth = selectedDate.firstDayOfMonth()
                    )
                }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                selectedMonth = date.firstDayOfMonth()
            )
        }
    }

    fun toggleTagFilter(tagId: Long) {
        _uiState.update { state ->
            val selectedTagIds = if (tagId in state.selectedTagIds) {
                state.selectedTagIds - tagId
            } else {
                state.selectedTagIds + tagId
            }
            state.copy(selectedTagIds = selectedTagIds)
        }
    }

    fun toggleCalendarDisplayMode() {
        _uiState.update { state ->
            val mode = when (state.calendarDisplayMode) {
                CalendarDisplayMode.Month -> CalendarDisplayMode.Week
                CalendarDisplayMode.Week -> CalendarDisplayMode.Month
            }
            state.copy(
                calendarDisplayMode = mode,
                selectedMonth = state.selectedDate.firstDayOfMonth()
            )
        }
    }

    fun resetToToday() {
        val today = today()
        _uiState.update {
            it.copy(
                selectedMonth = today.firstDayOfMonth(),
                selectedDate = today
            )
        }
    }

    fun toggleMonthlyWinsExpanded() {
        _uiState.update { it.copy(isMonthlyWinsExpanded = !it.isMonthlyWinsExpanded) }
    }
}

private data class CalendarCombined(
    val dailyPlans: List<DailyPlan>,
    val dayReviews: List<PeriodReview>,
    val journalEntries: List<JournalEntry>,
    val dailyStats: List<DailyReflectStat>,
    val recentTasksByDate: Map<LocalDate, List<TaskItem>>,
    val recentNotesByDate: Map<LocalDate, List<NoteItem>>
)
