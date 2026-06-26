package com.checkit.ui.okr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.ObjectiveWriteInput
import com.checkit.domain.Objective
import com.checkit.domain.usecase.AddObjectiveUseCase
import com.checkit.domain.usecase.DeleteObjectiveUseCase
import com.checkit.domain.usecase.UpdateObjectiveUseCase
import com.checkit.ui.EditorMode
import com.checkit.ui.ObjectiveEditorState
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

    fun openNewList() {
        _uiState.update { it.copy(editor = ObjectiveEditorState(mode = EditorMode.Add, goalId = null)) }
    }

    fun openNewObjective(goalId: Long) {
        _uiState.update { it.copy(editor = ObjectiveEditorState(mode = EditorMode.Add, goalId = goalId)) }
    }

    fun openEditObjective(list: Objective) {
        _uiState.update {
            it.copy(
                editor = ObjectiveEditorState(
                    mode = EditorMode.Edit,
                    objectiveId = list.id,
                    goalId = list.goalId,
                    name = list.name,
                    color = list.color,
                    icon = list.icon,
                    startDate = list.startDate,
                    endDate = list.endDate
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
        if (form.name.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Add a name"))
            return
        }
        if (form.goalId != null) {
            if (form.startDate == null || form.endDate == null) {
                sendEvent(UiEvent.ShowSnackbar("Set start and end dates"))
                return
            }
        }
        val input = ObjectiveWriteInput(
            name = form.name.trim(),
            color = form.color,
            icon = form.icon,
            goalId = form.goalId,
            startDate = form.startDate,
            endDate = form.endDate
        )
        viewModelScope.launch {
            val savedId = if (form.mode == EditorMode.Add) {
                addObjective(input)
            } else {
                val objectiveId = form.objectiveId ?: return@launch
                updateObjective(objectiveId, input)
                objectiveId
            }
            _uiState.update { it.copy(editor = null) }
            onSaved(if (form.goalId != null) form.goalId else savedId)
        }
    }

    fun deleteEditorList(onDeleted: () -> Unit = {}) {
        val form = _uiState.value.editor ?: return
        val objectiveId = form.objectiveId ?: return
        if (form.name == InboxListName) {
            sendEvent(UiEvent.ShowSnackbar("Inbox can't be deleted"))
            return
        }
        viewModelScope.launch {
            deleteObjective(objectiveId)
            _uiState.update { it.copy(editor = null) }
            sendEvent(UiEvent.ShowSnackbar("List deleted; items moved to Inbox"))
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
