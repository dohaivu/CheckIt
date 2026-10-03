package com.checkit.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.Period
import com.checkit.domain.usecase.ObserveDailyReflectStatsUseCase
import com.checkit.domain.usecase.ObservePeriodGoalsUseCase
import com.checkit.ui.today
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

/**
 * Loads the whole displayed year in a single observation: day ratings from
 * period goals plus done minutes from the precomputed daily stats table.
 * Only two flows are combined (vs. five in [CalendarViewModel]) and the
 * per-date maps are built once per emission, so all 12 month grids render
 * from cheap map lookups.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class YearViewModel(
    private val observePeriodGoals: ObservePeriodGoalsUseCase,
    private val observeDailyReflectStats: ObserveDailyReflectStatsUseCase
) : ViewModel() {
    private val year = MutableStateFlow(today().year)

    private val _uiState = MutableStateFlow(YearUiState())
    val uiState: StateFlow<YearUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            year
                .flatMapLatest { year ->
                    val start = LocalDate(year, Month.JANUARY, 1)
                    val end = LocalDate(year, Month.DECEMBER, 31)
                    combine(
                        observePeriodGoals(start, end),
                        observeDailyReflectStats(start, end)
                    ) { goals, stats ->
                        YearUiState(
                            year = year,
                            ratingByDate = goals
                                .asSequence()
                                .filter { it.period == Period.Day }
                                .associate { it.startDate to it.rating },
                            doneMinutesByDate = stats.associate { it.date to it.doneMinutes },
                            isLoading = false
                        )
                    }.catch { emit(YearUiState(year = year, isLoading = false)) }
                }
                .collect { _uiState.value = it }
        }
    }

    fun setYear(year: Int) {
        this.year.value = year
    }
}
