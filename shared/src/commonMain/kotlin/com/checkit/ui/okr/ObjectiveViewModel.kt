package com.checkit.ui.okr

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ObjectiveUiState(
    val expandedNodeKeys: Set<String> = emptySet(),
    val selectedNodeKey: String? = null
)

class ObjectiveViewModel : ViewModel() {
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
}
