package com.checkit.ui.okr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.GoalWriteInput
import com.checkit.domain.Goal
import com.checkit.domain.usecase.AddGoalUseCase
import com.checkit.domain.usecase.DeleteGoalUseCase
import com.checkit.domain.usecase.UpdateGoalUseCase
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.UiEvent
import com.checkit.ui.theme.AppIconColorDefaults
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GoalItemType {
    data class Objective(val objectiveId: Long) : GoalItemType
    data class KeyResult(val keyResultId: Long) : GoalItemType
    data class Task(val taskId: Long) : GoalItemType
    data class Note(val noteId: Long) : GoalItemType
}

data class GoalUiState(
    val editor: GoalEditorState? = null,
    val collapsedNodeKeys: Set<String> = emptySet(),
    val selectedNodeKey: String? = null,
    val selectedItemType: GoalItemType? = null
)

data class GoalEditorState(
    val mode: EditorMode,
    val goalId: Long? = null,
    val title: String = "",
    val color: String = AppIconColorDefaults.ListColors.first(),
    val icon: String = AppIconColorDefaults.ListIcons.first()
)

class GoalViewModel(
    private val addGoal: AddGoalUseCase,
    private val updateGoal: UpdateGoalUseCase,
    private val deleteGoal: DeleteGoalUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun openNewGoal() {
        _uiState.update { it.copy(editor = GoalEditorState(mode = EditorMode.Add)) }
    }

    fun openEditGoal(goal: Goal) {
        _uiState.update {
            it.copy(
                editor = GoalEditorState(
                    mode = EditorMode.Edit,
                    goalId = goal.id,
                    title = goal.title,
                    color = goal.color,
                    icon = goal.icon
                )
            )
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun updateTitle(title: String) = updateEditor { it.copy(title = title) }
    fun updateColor(color: String) = updateEditor { it.copy(color = color) }
    fun updateIcon(icon: String) = updateEditor { it.copy(icon = icon) }

    fun saveEditor(onSaved: (Long) -> Unit = {}) {
        val form = _uiState.value.editor ?: return
        if (form.title.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Add a goal title"))
            return
        }
        val input = GoalWriteInput(
            title = form.title.trim(),
            color = form.color,
            icon = form.icon
        )
        viewModelScope.launch {
            val savedId = if (form.mode == EditorMode.Add) {
                addGoal(input)
            } else {
                val goalId = form.goalId ?: return@launch
                updateGoal(goalId, input)
                goalId
            }
            _uiState.update { it.copy(editor = null) }
            onSaved(savedId)
        }
    }

    fun deleteEditorGoal(onDeleted: () -> Unit = {}) {
        val goalId = _uiState.value.editor?.goalId ?: return
        viewModelScope.launch {
            deleteGoal(goalId)
            _uiState.update { it.copy(editor = null) }
            sendEvent(UiEvent.ShowSnackbar("Goal deleted"))
            onDeleted()
        }
    }

    fun toggleExpanded(nodeKey: String) {
        _uiState.update { state ->
            val collapsed = if (nodeKey in state.collapsedNodeKeys) {
                state.collapsedNodeKeys - nodeKey
            } else {
                state.collapsedNodeKeys + nodeKey
            }
            state.copy(collapsedNodeKeys = collapsed)
        }
    }

    fun selectNode(nodeKey: String?) {
        _uiState.update {
            val nextKey = if (it.selectedNodeKey == nodeKey) null else nodeKey
            it.copy(
                selectedNodeKey = nextKey,
                selectedItemType = nextKey?.let(::parseItemType)
            )
        }
    }

    private fun parseItemType(nodeKey: String): GoalItemType? {
        val id = nodeKey.substringAfterLast("-").toLongOrNull() ?: return null
        return when {
            nodeKey.startsWith("objective-") -> GoalItemType.Objective(id)
            nodeKey.startsWith("key-result-") -> GoalItemType.KeyResult(id)
            nodeKey.startsWith("task-") -> GoalItemType.Task(id)
            nodeKey.startsWith("note-") -> GoalItemType.Note(id)
            else -> null
        }
    }

    private fun updateEditor(transform: (GoalEditorState) -> GoalEditorState) {
        _uiState.update { state ->
            val form = state.editor ?: return@update state
            state.copy(editor = transform(form))
        }
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
