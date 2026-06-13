package com.checkit.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.ui.CalendarDisplayMode
import com.checkit.ui.CalendarUiState
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
    private val ensureDefaultTaskData: EnsureDefaultTaskDataUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensureDefaultTaskData()
            combine(observeTaskBoard(), observeDailyPlans()) { board, dailyPlans ->
                board to dailyPlans
            }
                .catch { _ ->
                    _uiState.update { it.copy() }
                }
                .collect { (board, dailyPlans) ->
                    _uiState.update { it.copy(board = board, dailyPlans = dailyPlans) }
                }
        }
    }

    fun previousMonth() {
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

    fun nextMonth() {
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
}
