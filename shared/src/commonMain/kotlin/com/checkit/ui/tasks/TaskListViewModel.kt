package com.checkit.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.TaskListWriteInput
import com.checkit.domain.TaskList
import com.checkit.domain.usecase.AddTaskListUseCase
import com.checkit.domain.usecase.DeleteTaskListUseCase
import com.checkit.domain.usecase.UpdateTaskListUseCase
import com.checkit.ui.EditorMode
import com.checkit.ui.ListEditorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskListUiState(
    val editor: ListEditorState? = null,
    val message: String? = null
)

class TaskListViewModel(
    private val addTaskList: AddTaskListUseCase,
    private val updateTaskList: UpdateTaskListUseCase,
    private val deleteTaskList: DeleteTaskListUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    fun openNewList() {
        _uiState.update { it.copy(editor = ListEditorState(mode = EditorMode.Add)) }
    }

    fun openEditList(list: TaskList) {
        _uiState.update {
            it.copy(
                editor = ListEditorState(
                    mode = EditorMode.Edit,
                    listId = list.id,
                    name = list.name,
                    color = list.color,
                    icon = list.icon
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

    fun saveEditor(onSaved: (Long) -> Unit = {}) {
        val form = _uiState.value.editor ?: return
        if (form.name.isBlank()) {
            showMessage("Add a list name")
            return
        }
        val input = TaskListWriteInput(
            name = form.name.trim(),
            color = form.color,
            icon = form.icon
        )
        viewModelScope.launch {
            val savedId = if (form.mode == EditorMode.Add) {
                addTaskList(input)
            } else {
                val listId = form.listId ?: return@launch
                updateTaskList(listId, input)
                listId
            }
            _uiState.update { it.copy(editor = null) }
            onSaved(savedId)
        }
    }

    fun deleteEditorList(onDeleted: () -> Unit = {}) {
        val form = _uiState.value.editor ?: return
        val listId = form.listId ?: return
        if (form.name == InboxListName) {
            showMessage("Inbox can't be deleted")
            return
        }
        viewModelScope.launch {
            deleteTaskList(listId)
            _uiState.update { it.copy(editor = null, message = "List deleted; items moved to Inbox") }
            onDeleted()
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateEditor(transform: (ListEditorState) -> ListEditorState) {
        _uiState.update { state ->
            val form = state.editor ?: return@update state
            state.copy(editor = transform(form))
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}

internal const val InboxListName = "Inbox"
