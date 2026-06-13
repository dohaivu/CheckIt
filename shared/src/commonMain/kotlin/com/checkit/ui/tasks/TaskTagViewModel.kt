package com.checkit.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.TaskTagWriteInput
import com.checkit.domain.TaskTag
import com.checkit.domain.usecase.AddTaskTagUseCase
import com.checkit.domain.usecase.DeleteTaskTagUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.UpdateTaskTagUseCase
import com.checkit.ui.EditorMode
import com.checkit.ui.TagEditorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskTagUiState(
    val editor: TagEditorState? = null,
    val message: String? = null
)

class TaskTagViewModel(
    private val addTaskTag: AddTaskTagUseCase,
    private val updateTaskTag: UpdateTaskTagUseCase,
    private val deleteTaskTag: DeleteTaskTagUseCase,
    private val isTagNameTaken: IsTagNameTakenUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskTagUiState())
    val uiState: StateFlow<TaskTagUiState> = _uiState.asStateFlow()

    fun openNewTag() {
        _uiState.update { it.copy(editor = TagEditorState(mode = EditorMode.Add)) }
    }

    fun openEditTag(tag: TaskTag) {
        _uiState.update {
            it.copy(
                editor = TagEditorState(
                    mode = EditorMode.Edit,
                    tagId = tag.id,
                    name = tag.name,
                    color = tag.color
                )
            )
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun updateName(name: String) = updateEditor { it.copy(name = name) }
    fun updateColor(color: String) = updateEditor { it.copy(color = color) }

    fun saveEditor(onSaved: (Long) -> Unit = {}) {
        val form = _uiState.value.editor ?: return
        val trimmedName = form.name.trim()
        if (trimmedName.isBlank()) {
            showMessage("Add a tag name")
            return
        }
        viewModelScope.launch {
            if (isTagNameTaken(trimmedName, form.tagId)) {
                showMessage("Tag name already exists")
                return@launch
            }
            val input = TaskTagWriteInput(
                name = trimmedName,
                color = form.color
            )
            val savedId = if (form.mode == EditorMode.Add) {
                addTaskTag(input)
            } else {
                val tagId = form.tagId ?: return@launch
                updateTaskTag(tagId, input)
                tagId
            }
            _uiState.update { it.copy(editor = null) }
            onSaved(savedId)
        }
    }

    fun deleteEditorTag(onDeleted: () -> Unit = {}) {
        val tagId = _uiState.value.editor?.tagId ?: return
        viewModelScope.launch {
            deleteTaskTag(tagId)
            _uiState.update { it.copy(editor = null, message = "Tag deleted") }
            onDeleted()
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateEditor(transform: (TagEditorState) -> TagEditorState) {
        _uiState.update { state ->
            val form = state.editor ?: return@update state
            state.copy(editor = transform(form))
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
