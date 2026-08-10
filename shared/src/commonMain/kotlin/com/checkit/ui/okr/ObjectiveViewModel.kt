package com.checkit.ui.okr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.ObjectiveWriteInput
import com.checkit.domain.Objective
import com.checkit.domain.usecase.AddObjectiveUseCase
import com.checkit.domain.usecase.DeleteObjectiveUseCase
import com.checkit.domain.usecase.UpdateObjectiveUseCase
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.tasks.ObjectiveEditorState
import com.checkit.ui.UiEvent
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ObjectiveUiState(
    val editor: ObjectiveEditorState? = null
)

class ObjectiveViewModel(
    private val addObjective: AddObjectiveUseCase,
    private val updateObjective: UpdateObjectiveUseCase,
    private val deleteObjective: DeleteObjectiveUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ObjectiveUiState())
    val uiState: StateFlow<ObjectiveUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun openNewObjective(goalId: Long) {
        _uiState.update { it.copy(editor = ObjectiveEditorState(mode = EditorMode.Add, goalId = goalId)) }
    }

    fun openEditObjective(objective: Objective) {
        _uiState.update {
            it.copy(
                editor = ObjectiveEditorState(
                    mode = EditorMode.Edit,
                    objectiveId = objective.id,
                    goalId = objective.goalId,
                    name = objective.name,
                    color = objective.color,
                    icon = objective.icon,
                    startDate = objective.startDate,
                    endDate = objective.endDate
                )
            )
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun updateName(name: String) = updateEditor { it.copy(name = name) }
    fun updateColor(color: String) = updateEditor { it.copy(color = color) }
    fun updateIcon(icon: String) = updateEditor { it.copy(icon = icon) }
    fun updateDateRange(start: LocalDate?, end: LocalDate?) = updateEditor { it.copy(startDate = start, endDate = end) }

    fun saveEditor(onSaved: (Long) -> Unit = {}) {
        val form = _uiState.value.editor ?: return
        val goalId = form.goalId ?: return
        if (form.name.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Add a name"))
            return
        }
        if (form.startDate == null || form.endDate == null) {
            sendEvent(UiEvent.ShowSnackbar("Set start and end dates"))
            return
        }

        val input = ObjectiveWriteInput(
            name = form.name.trim(),
            color = form.color,
            icon = form.icon,
            goalId = goalId,
            startDate = form.startDate,
            endDate = form.endDate
        )
        viewModelScope.launch {
            if (form.mode == EditorMode.Add) {
                addObjective(input)
            } else {
                val objectiveId = form.objectiveId ?: return@launch
                updateObjective(objectiveId, input)
            }
            _uiState.update { it.copy(editor = null) }
            onSaved(goalId)
        }
    }

    fun deleteEditorObjective(onDeleted: () -> Unit = {}) {
        val form = _uiState.value.editor ?: return
        val objectiveId = form.objectiveId ?: return
        viewModelScope.launch {
            deleteObjective(objectiveId)
            _uiState.update { it.copy(editor = null) }
            sendEvent(UiEvent.ShowSnackbar("Objective deleted"))
            onDeleted()
        }
    }

    private fun updateEditor(transform: (ObjectiveEditorState) -> ObjectiveEditorState) {
        _uiState.update { state ->
            val form = state.editor ?: return@update state
            state.copy(editor = transform(form))
        }
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

internal const val InboxListName = "Inbox"
