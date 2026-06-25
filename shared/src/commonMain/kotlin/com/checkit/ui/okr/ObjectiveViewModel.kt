package com.checkit.ui.okr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.CheckItRepository
import com.checkit.data.KeyResultWriteInput
import com.checkit.domain.KeyResultUnit
import com.checkit.ui.EditorMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ObjectiveUiState(
    val expandedNodeKeys: Set<String> = emptySet(),
    val selectedNodeKey: String? = null,
    val keyResultEditor: KeyResultEditorState? = null,
    val message: String? = null
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

class ObjectiveViewModel(
    private val repository: CheckItRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ObjectiveUiState())
    val uiState: StateFlow<ObjectiveUiState> = _uiState.asStateFlow()

    fun toggleExpanded(nodeKey: String) {
        _uiState.update { state ->
            val expanded = if (nodeKey in state.expandedNodeKeys) {
                state.expandedNodeKeys - nodeKey
            } else {
                state.expandedNodeKeys + nodeKey
            }
            state.copy(expandedNodeKeys = expanded)
        }
    }

    fun selectNode(nodeKey: String) {
        _uiState.update { it.copy(selectedNodeKey = nodeKey) }
    }

    fun openNewKeyResult(objectiveId: Long) {
        _uiState.update {
            it.copy(keyResultEditor = KeyResultEditorState(mode = EditorMode.Add, objectiveId = objectiveId))
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
            showMessage("Add a key result title")
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

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateKeyResultEditor(transform: (KeyResultEditorState) -> KeyResultEditorState) {
        _uiState.update { state ->
            val form = state.keyResultEditor ?: return@update state
            state.copy(keyResultEditor = transform(form))
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
