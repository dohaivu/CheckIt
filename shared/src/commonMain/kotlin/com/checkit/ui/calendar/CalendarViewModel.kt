package com.checkit.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.JournalEntry
import com.checkit.domain.NoteItem
import com.checkit.domain.PeriodReview
import com.checkit.domain.Period
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.usecase.GetNotesForDateUseCase
import com.checkit.domain.usecase.GetTasksForDateUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveDailyReflectStatsUseCase
import com.checkit.domain.usecase.ObserveJournalEntriesUseCase
import com.checkit.domain.usecase.ObservePeriodReviewsUseCase
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val getTasksForDate: GetTasksForDateUseCase,
    private val getNotesForDate: GetNotesForDateUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.map { it.selectedMonth }
                .distinctUntilChanged()
                .flatMapLatest { month ->
                    val start = month.minus(1, DateTimeUnit.MONTH)
                    val end = month.plus(2, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                    combine(
                        observeDailyPlans(startDate = start, endDate = end),
                        observePeriodReviews(start, end),
                        observeJournalEntries(start, end),
                        observeDailyReflectStats(start, end)
                    ) { dailyPlans, periodReviews, journalEntries, dailyStats ->
                        CalendarCombined(dailyPlans, periodReviews, journalEntries, dailyStats)
                    }
                }
                .catch { _ ->
                    _uiState.update { it.copy() }
                }
                .collect { combined ->
                    _uiState.update { state ->
                        state.copy(
                            dailyPlans = combined.dailyPlans,
                            dayReviews = combined.dayReviews.filter { it.period == Period.Day },
                            journalEntries = combined.journalEntries,
                            dailyStatsByDate = combined.dailyStats.associateBy { it.date }
                        )
                    }
                }
        }

        // Dedicated flow for selected date details (minimal data fetch)
        viewModelScope.launch {
            _uiState.map { it.selectedDate }
                .distinctUntilChanged()
                .collect { date ->
                    val tasks = getTasksForDate(date)
                    val notes = getNotesForDate(date)
                    _uiState.update { it.copy(selectedDateTasks = tasks, selectedDateNotes = notes) }
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

    fun toggleDailyPlanSummary() {
        _uiState.update { it.copy(showDailyPlanSummary = !it.showDailyPlanSummary) }
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
    val dailyStats: List<DailyReflectStat>
)
