package com.checkit.ui.myday

import com.checkit.domain.RoutineStepTemplate
import com.checkit.ui.UiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

/** Observes routines + today checks and handles routine actions. */
internal class RoutineController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    fun start() {
        scope.launch {
            combine(
                deps.observeRoutines(),
                deps.observeRoutineToday()
            ) { routines, routineToday ->
                routines to routineToday
            }
                .catch { error ->
                    state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to load routines"))
                }
                .collect { (routines, routineToday) ->
                    state.update {
                        it.copy(
                            routines = routines,
                            routineToday = routineToday
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
        description: String,
        reminderMinutes: Int?,
        activeWeekdays: Set<DayOfWeek>,
        steps: List<RoutineStepTemplate>
    ) {
        scope.launch {
            try {
                deps.saveRoutine(id, title, description, reminderMinutes, activeWeekdays, steps)
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
