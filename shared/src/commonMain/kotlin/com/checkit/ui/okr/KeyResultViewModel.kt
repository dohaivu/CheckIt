package com.checkit.ui.okr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.CheckItRepository
import com.checkit.data.KeyResultWriteInput
import com.checkit.domain.KeyResult
import com.checkit.domain.KeyResultUnit
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KeyResultUiState(
    val keyResultEditor: KeyResultEditorState? = null
)

data class KeyResultEditorState(
    val mode: EditorMode,
    val objectiveId: Long,
    val keyResultId: Long? = null,
    val title: String = "",
    val targetValue: Double = 0.0,
    val currentValue: Double = 0.0,
    val unit: KeyResultUnit = KeyResultUnit.Number
)

class KeyResultViewModel(
    private val repository: CheckItRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeyResultUiState())
    val uiState: StateFlow<KeyResultUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun openNewKeyResult(objectiveId: Long) {
        _uiState.update {
            it.copy(keyResultEditor = KeyResultEditorState(mode = EditorMode.Add, objectiveId = objectiveId))
        }
    }

    fun openEditKeyResult(keyResult: KeyResult) {
        _uiState.update {
            it.copy(
                keyResultEditor = KeyResultEditorState(
                    mode = EditorMode.Edit,
                    objectiveId = keyResult.objectiveId,
                    keyResultId = keyResult.id,
                    title = keyResult.title,
                    targetValue = keyResult.targetValue,
                    currentValue = keyResult.currentValue,
                    unit = KeyResultUnit.fromString(keyResult.unit)
                )
            )
        }
    }

    fun dismissKeyResultEditor() {
        _uiState.update { it.copy(keyResultEditor = null) }
    }

    fun updateKeyResultTitle(title: String) = updateKeyResultEditor { it.copy(title = title) }
    fun updateKeyResultTargetValue(value: Double) = updateKeyResultEditor { it.copy(targetValue = value) }
    fun updateKeyResultCurrentValue(value: Double) = updateKeyResultEditor { it.copy(currentValue = value) }
    fun updateKeyResultUnit(unit: KeyResultUnit) = updateKeyResultEditor { it.copy(unit = unit) }

    fun saveKeyResultEditor(onSaved: () -> Unit = {}) {
        val form = _uiState.value.keyResultEditor ?: return
        if (form.title.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Add a title"))
            return
        }
        if (form.targetValue <= 0.0) {
            sendEvent(UiEvent.ShowSnackbar("Set a target value"))
            return
        }
        val input = KeyResultWriteInput(
            objectiveId = form.objectiveId,
            title = form.title.trim(),
            targetValue = form.targetValue,
            currentValue = form.currentValue,
            unit = form.unit.name
        )
        viewModelScope.launch {
            if (form.mode == EditorMode.Add) {
                repository.addKeyResult(input)
            } else {
                val keyResultId = form.keyResultId ?: return@launch
                repository.updateKeyResult(keyResultId, input)
            }
            _uiState.update { it.copy(keyResultEditor = null) }
            onSaved()
        }
    }

    fun deleteKeyResultEditor(onDeleted: () -> Unit = {}) {
        val form = _uiState.value.keyResultEditor ?: return
        val keyResultId = form.keyResultId ?: return
        viewModelScope.launch {
            repository.deleteKeyResult(keyResultId)
            _uiState.update { it.copy(keyResultEditor = null) }
            onDeleted()
        }
    }

    private fun updateKeyResultEditor(transform: (KeyResultEditorState) -> KeyResultEditorState) {
        _uiState.update { state ->
            val form = state.keyResultEditor ?: return@update state
            state.copy(keyResultEditor = transform(form))
        }
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
