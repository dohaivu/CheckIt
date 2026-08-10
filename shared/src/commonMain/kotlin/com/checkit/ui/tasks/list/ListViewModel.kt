package com.checkit.ui.tasks.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.ListWriteInput
import com.checkit.domain.ListItem
import com.checkit.domain.usecase.AddListUseCase
import com.checkit.domain.usecase.DeleteListUseCase
import com.checkit.domain.usecase.UpdateListUseCase
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.tasks.TagEditorState
import com.checkit.ui.UiEvent
import com.checkit.ui.theme.AppIconColorDefaults
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListEditorState(
    val mode: EditorMode,
    val listId: Long? = null,
    val title: String = "",
    val color: String = AppIconColorDefaults.ListColors.first(),
    val icon: String = AppIconColorDefaults.ListIcons.first(),
    val isArchived: Boolean = false
)

data class ListUiState(
    val editor: ListEditorState? = null
)

class ListViewModel(
    private val addList: AddListUseCase,
    private val updateList: UpdateListUseCase,
    private val deleteList: DeleteListUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun openNewList() {
        _uiState.update { it.copy(editor = ListEditorState(mode = EditorMode.Add)) }
    }

    fun openEditList(list: ListItem) {
        _uiState.update {
            it.copy(
                editor = ListEditorState(
                    mode = EditorMode.Edit,
                    listId = list.id,
                    title = list.title,
                    color = list.color,
                    icon = list.icon,
                    isArchived = list.isArchived
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
            sendEvent(UiEvent.ShowSnackbar("Add a title"))
            return
        }
        val input = ListWriteInput(
            title = form.title.trim(),
            color = form.color,
            icon = form.icon
        )
        viewModelScope.launch {
            val savedId = if (form.mode == EditorMode.Add) {
                addList(input)
            } else {
                val listId = form.listId ?: return@launch
                updateList(listId, input)
                listId
            }
            _uiState.update { it.copy(editor = null) }
            onSaved(savedId)
        }
    }

    fun deleteEditorList(onDeleted: () -> Unit = {}) {
        val form = _uiState.value.editor ?: return
        val listId = form.listId ?: return
        if (form.title == "Inbox") {
            sendEvent(UiEvent.ShowSnackbar("Inbox can't be deleted"))
            return
        }
        viewModelScope.launch {
            deleteList(listId)
            _uiState.update { it.copy(editor = null) }
            sendEvent(UiEvent.ShowSnackbar("List deleted"))
            onDeleted()
        }
    }

    private fun updateEditor(transform: (ListEditorState) -> ListEditorState) {
        _uiState.update { state ->
            val form = state.editor ?: return@update state
            state.copy(editor = transform(form))
        }
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
