package com.checkit.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.CheckItRepository
import com.checkit.ui.ReportUiState
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.today
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class ReportViewModel(
    private val repository: CheckItRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeDailyPlans()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = error.message ?: "Unable to load reports"
                        )
                    }
                }
                .collect { dailyPlans ->
                    _uiState.update {
                        it.copy(
                            dailyPlans = dailyPlans,
                            isLoading = false,
                            message = null
                        )
                    }
                }
        }
    }

    fun selectPeriod(period: ReportPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun previousPeriod() {
        _uiState.update { state ->
            state.copy(selectedDate = state.selectedPeriod.move(state.selectedDate, -1))
        }
    }

    fun nextPeriod() {
        _uiState.update { state ->
            state.copy(selectedDate = state.selectedPeriod.move(state.selectedDate, 1))
        }
    }

    fun resetToCurrentPeriod() {
        _uiState.update { it.copy(selectedDate = today()) }
    }

    fun previousWeek() {
        _uiState.update { state ->
            state.copy(selectedDate = ReportPeriod.Week.move(state.selectedDate, -1))
        }
    }

    fun nextWeek() {
        _uiState.update { state ->
            state.copy(selectedDate = ReportPeriod.Week.move(state.selectedDate, 1))
        }
    }
}

private fun ReportPeriod.move(date: LocalDate, amount: Int): LocalDate = when (this) {
    ReportPeriod.Week -> date.plus(amount * 7, DateTimeUnit.DAY)
    ReportPeriod.Month -> date.plus(amount, DateTimeUnit.MONTH).firstDayOfMonth()
    ReportPeriod.Annual -> date.plus(amount, DateTimeUnit.YEAR)
}
