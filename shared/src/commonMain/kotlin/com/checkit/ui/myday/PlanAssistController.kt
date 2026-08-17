package com.checkit.ui.myday

import com.checkit.domain.DefaultTaskDurationMinutes
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TaskItem
import com.checkit.domain.nextAvailableTimeRange
import com.checkit.ui.UiEvent
import com.checkit.ui.currentMyDayTimeMinutes
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Handles plan assist banner, suggestions sheet, and quick add actions. */
internal class PlanAssistController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    fun openPlanAssist() {
        state.update {
            it.copy(
                showPlanAssistBanner = false,
                showSuggestions = true,
                showLeftoversSheet = false,
                suggestionStartTimeMinutes = null,
                suggestionEndTimeMinutes = null,
                itemEditor = null,
                dayClose = null
            )
        }
    }

    fun dismissPlanAssist() {
        val todayEpoch = today().toEpochDays().toInt()
        scope.launch {
            deps.settingsRepository.setLastDayPlanDismissedEpochDay(todayEpoch)
        }
        state.update {
            it.copy(
                showPlanAssistBanner = false,
                lastDayPlanDismissedEpochDay = todayEpoch
            )
        }
    }

    fun openSuggestions(
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null
    ) {
        state.update {
            it.copy(
                showSuggestions = true,
                showPlanAssistBanner = false,
                suggestionStartTimeMinutes = startTimeMinutes,
                suggestionEndTimeMinutes = endTimeMinutes
            )
        }
    }

    fun dismissSuggestions() {
        state.update {
            it.copy(
                showSuggestions = false,
                suggestionStartTimeMinutes = null,
                suggestionEndTimeMinutes = null
            )
        }
    }

    fun addTaskFromSuggestion(task: TaskItem) {
        addTaskToMyDay(task, clearSuggestions = true)
    }

    fun addTaskToMyDay(task: TaskItem) {
        addTaskToMyDay(task, clearSuggestions = false)
    }

    fun addDailyPlanItem(title: String, tagIds: List<Long>, nestedListItemId: Long? = null) {
        if (title.isBlank()) return
        val current = state.uiState.value

        val (startTime, endTime) = if (current.suggestionStartTimeMinutes == null) {
            nextAvailableTimeRange(currentMyDayTimeMinutes(), DefaultTaskDurationMinutes, current.items)
        } else {
            current.suggestionStartTimeMinutes to current.suggestionEndTimeMinutes
        }

        scope.launch {
            val editor = DailyPlanItemEditorState(
                mode = EditorMode.Add,
                date = current.today,
                source = DailyPlanItemSource.MyDayTask,
                title = title,
                status = DailyPlanItemStatus.Planned,
                startTimeMinutes = startTime,
                endTimeMinutes = endTime,
                selectedTagIds = tagIds.toSet(),
                nestedListItemId = nestedListItemId
            )
            // Note: UpsertDailyPlanItemUseCase might need update to handle nestedListItemId
            // but we can also call addDailyPlanItem from repository directly or update use case.
            // Let's check UpsertDailyPlanItemUseCase first.
            deps.upsertDailyPlanItem(editor).onSuccess {
                state.sendEvent(UiEvent.ShowSnackbar("Added to My Day"))
            }.onFailure { error ->
                state.sendEvent(UiEvent.ShowSnackbar("Failed: ${error.message ?: "Unknown error"}"))
            }
        }
    }

    private fun addTaskToMyDay(
        task: TaskItem,
        clearSuggestions: Boolean
    ) {
        scope.launch {
            val current = state.uiState.value
            deps.addSuggestedTaskToMyDay(
                task = task,
                suggestionStart = current.suggestionStartTimeMinutes,
                suggestionEnd = current.suggestionEndTimeMinutes
            ).onSuccess {
                state.update { stateValue ->
                    if (clearSuggestions) {
                        stateValue.copy(
                            showSuggestions = false,
                            suggestionStartTimeMinutes = null,
                            suggestionEndTimeMinutes = null
                        )
                    } else {
                        stateValue
                    }
                }
                state.sendEvent(UiEvent.ShowSnackbar("Added to My Day"))
            }.onFailure { error ->
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to add task"))
            }
        }
    }
}
