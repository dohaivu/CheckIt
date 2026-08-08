package com.checkit.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.DailyPlan
import com.checkit.domain.JournalEntry
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewPeriod
import com.checkit.domain.TaskBoard
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveJournalEntriesUseCase
import com.checkit.domain.usecase.ObservePeriodReviewsUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class CalendarViewModel(
    private val observeTaskBoard: ObserveTaskBoardUseCase,
    private val observeDailyPlans: ObserveDailyPlansUseCase,
    private val observeDayReviews: ObservePeriodReviewsUseCase,
    private val observeJournalEntries: ObserveJournalEntriesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeTaskBoard(),
                observeDailyPlans(),
                observeDayReviews(),
                observeJournalEntries()
            ) { board, dailyPlans, periodReviews, journalEntries ->
                CalendarCombined(board, dailyPlans, periodReviews, journalEntries)
            }
                .catch { _ ->
                    _uiState.update { it.copy() }
                }
                .collect { (board, dailyPlans, periodReviews, journalEntries) ->
                    _uiState.update { state ->
                        val availableTagIds = board.tags.map { it.id }.toSet()
                        state.copy(
                            board = board,
                            dailyPlans = dailyPlans,
                            dayReviews = periodReviews.filter { it.period == ReviewPeriod.Day },
                            journalEntries = journalEntries,
                            selectedTagIds = state.selectedTagIds.intersect(availableTagIds)
                        )
                    }
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
    val board: TaskBoard,
    val dailyPlans: List<DailyPlan>,
    val dayReviews: List<PeriodReview>,
    val journalEntries: List<JournalEntry>
)
