package com.checkit.ui.okr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.GoalWriteInput
import com.checkit.domain.Goal
import com.checkit.domain.usecase.AddGoalUseCase
import com.checkit.domain.usecase.DeleteGoalUseCase
import com.checkit.domain.usecase.UpdateGoalUseCase
import com.checkit.ui.EditorMode
import com.checkit.ui.theme.AppIconColorDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalUiState(
    val editor: GoalEditorState? = null,
    val message: String? = null
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
            showMessage("Add a goal title")
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
            _uiState.update { it.copy(editor = null, message = "Goal deleted") }
            onDeleted()
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateEditor(transform: (GoalEditorState) -> GoalEditorState) {
        _uiState.update { state ->
            val form = state.editor ?: return@update state
            state.copy(editor = transform(form))
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
