package com.checkit.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.ui.CalendarUiState
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.isSameMonth
import com.checkit.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class CalendarViewModel(
    private val observeTaskBoard: ObserveTaskBoardUseCase,
    private val ensureDefaultTaskData: EnsureDefaultTaskDataUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensureDefaultTaskData()
            observeTaskBoard()
                .catch { error ->
                    _uiState.update { it.copy() }
                }
                .collect { board ->
                    _uiState.update { it.copy(board = board) }
                }
        }
    }

    fun selectMonth(month: LocalDate) {
        _uiState.update {  it.copy(selectedMonth = month.firstDayOfMonth()) }
    }

    fun previousMonth() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.minus(1, DateTimeUnit.MONTH)) }
    }

    fun nextMonth() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.plus(1, DateTimeUnit.MONTH)) }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
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

private fun LocalDate.matchesPeriod(period: ReportPeriod, month: LocalDate): Boolean =
    when (period) {
        ReportPeriod.Month -> isSameMonth(month)
        ReportPeriod.Annual -> year == month.year
    }
