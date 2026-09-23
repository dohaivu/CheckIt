package com.checkit.ui.myday

import com.checkit.domain.RoutineStepTemplate
import com.checkit.ui.UiEvent
import com.checkit.ui.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

/** Fixed trailing window for routine logs (heatmap + streaks), mirroring habits. */
private const val RoutineWindowDays = 180

/** Observes routines + today checks + logs and handles routine actions. */
internal class RoutineController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    fun start() {
        val today = today()
        val start = today.minus(RoutineWindowDays - 1, DateTimeUnit.DAY)
        scope.launch {
            combine(
                deps.observeRoutines(),
                deps.observeRoutineToday(),
                deps.observeRoutineLogs(start, today)
            ) { routines, routineToday, logs ->
                Triple(routines, routineToday, logs)
            }
                .catch { error ->
                    state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to load routines"))
                }
                .collect { (routines, routineToday, logs) ->
                    state.update {
                        it.copy(
                            routines = routines,
                            routineToday = routineToday,
                            routineLogs = logs
                        )
                    }
                }
        }
    }

    fun toggleStep(routineId: String, stepId: String) {
        scope.launch {
            try {
                deps.toggleRoutineStep(routineId, stepId)
            } catch (error: Exception) {
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to update routine"))
            }
        }
    }

    fun saveRoutine(
        id: String?,
        title: String,
        reminderMinutes: Int?,
        steps: List<RoutineStepTemplate>
    ) {
        scope.launch {
            try {
                deps.saveRoutine(id, title, reminderMinutes, steps)
            } catch (error: Exception) {
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save routine"))
            }
        }
    }

    fun deleteRoutine(id: String) {
        scope.launch {
            try {
                deps.deleteRoutine(id)
            } catch (error: Exception) {
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to delete routine"))
            }
        }
    }
}
