package com.checkit.ui.myday

import com.checkit.ui.UiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Handles the Smart Scheduler action from the My Day top bar. */
internal class SmartSchedulerController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    fun scheduleAll() {
        scope.launch {
            deps.smartSchedule().onSuccess { result ->
                val message = when {
                    result.candidateCount == 0 -> "Nothing to schedule"
                    result.scheduledCount == 0 -> "No history found to schedule"
                    else -> "Scheduled ${result.scheduledCount} item" +
                        (if (result.scheduledCount == 1) "" else "s")
                }
                state.sendEvent(UiEvent.ShowSnackbar(message))
            }.onFailure { error ->
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Smart schedule failed"))
            }
        }
    }
}
