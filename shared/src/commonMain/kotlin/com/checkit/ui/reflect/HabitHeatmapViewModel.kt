package com.checkit.ui.reflect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.HabitDailyRollup
import com.checkit.domain.usecase.ObserveHabitDailyRollupsForHabitUseCase
import com.checkit.ui.today
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Full-history state for one habit's heatmap. Months run from the earliest
 * recorded check-in to the current month, so the screen can show every row
 * persisted in the database rather than the fixed recent window used by the
 * Reflect summary card.
 */
data class HabitHeatmapUiState(
    val title: String = "",
    val months: List<HeatmapMonth> = emptyList(),
    val intensityByDate: Map<LocalDate, Float> = emptyMap(),
    val streak: Int = 0,
    val totalDone: Int = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class HabitHeatmapViewModel(
    private val observeHabitRollups: ObserveHabitDailyRollupsForHabitUseCase
) : ViewModel() {
    private val selectedHabitKey = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(HabitHeatmapUiState())
    val uiState: StateFlow<HabitHeatmapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedHabitKey
                .filterNotNull()
                .flatMapLatest { habitKey ->
                    observeHabitRollups(habitKey)
                        .catch { emit(emptyList()) }
                        .map { rollups -> buildUiState(rollups) }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun selectHabit(habitKey: String, title: String) {
        _uiState.update { it.copy(title = title.ifBlank { "Habit" }) }
        if (selectedHabitKey.value != habitKey) {
            _uiState.update { it.copy(isLoading = true) }
            selectedHabitKey.value = habitKey
        }
    }

    private fun buildUiState(rollups: List<HabitDailyRollup>): HabitHeatmapUiState {
        val today = today()
        val checkin = buildHabitCheckins(rollups, today).firstOrNull()
            ?: return _uiState.value.copy(
                months = emptyList(),
                intensityByDate = emptyMap(),
                streak = 0,
                totalDone = 0,
                isLoading = false
            )
        val firstDate = rollups.minOf { LocalDate.fromEpochDays(it.dateEpochDays) }
        return _uiState.value.copy(
            title = checkin.title,
            months = buildHeatmapMonthsBetween(
                firstMonthStart = LocalDate(firstDate.year, firstDate.month, 1),
                lastMonthStart = LocalDate(today.year, today.month, 1)
            ),
            intensityByDate = checkin.doneMinutesByDate.mapValues { (_, minutes) ->
                heatmapIntensity(minutes)
            },
            streak = checkin.streak,
            totalDone = checkin.totalDone,
            isLoading = false
        )
    }
}
