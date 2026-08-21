package com.checkit.ui.tasks.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.ListSection
import com.checkit.domain.usecase.AddSectionUseCase
import com.checkit.domain.usecase.DeleteSectionUseCase
import com.checkit.domain.usecase.UpdateSectionUseCase
import com.checkit.ui.UiEvent
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.tasks.TaskViewModel
import com.checkit.ui.theme.AppIconColorDefaults
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListSectionEditorState(
    val mode: EditorMode,
    val sectionId: Long? = null,
    val listId: Long,
    val title: String = "",
    val color: String = AppIconColorDefaults.ListColors.first(),
    val sortOrder: Int = 0
)

data class ListSectionUiState(
    val listId: Long? = null,
    val sections: List<ListSection> = emptyList(),
    val editor: ListSectionEditorState? = null
)

class ListSectionViewModel(
    private val addSection: AddSectionUseCase,
    private val updateSection: UpdateSectionUseCase,
    private val deleteSection: DeleteSectionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ListSectionUiState())
    val uiState: StateFlow<ListSectionUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun loadList(listId: Long, sections: List<ListSection>) {
        _uiState.update { it.copy(listId = listId, sections = sections.sortedBy { it.sortOrder }) }
    }

    fun openNewSection() {
        val listId = _uiState.value.listId ?: return
        _uiState.update {
            it.copy(
                editor = ListSectionEditorState(
                    mode = EditorMode.Add,
                    listId = listId,
                    sortOrder = it.sections.size
                )
            )
        }
    }

    fun openEditSection(section: ListSection) {
        _uiState.update {
            it.copy(
                editor = ListSectionEditorState(
                    mode = EditorMode.Edit,
                    sectionId = section.id,
                    listId = section.listId,
                    title = section.title,
                    color = section.color,
                    sortOrder = section.sortOrder
                )
            )
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun updateTitle(title: String) = updateEditor { it.copy(title = title) }
    fun updateColor(color: String) = updateEditor { it.copy(color = color) }

    fun saveEditor() {
        val form = _uiState.value.editor ?: return
        if (form.title.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Add a title"))
            return
        }
        viewModelScope.launch {
            if (form.mode == EditorMode.Add) {
                addSection(form.listId, form.title.trim(), form.color)
            } else {
                val sectionId = form.sectionId ?: return@launch
                updateSection(sectionId, form.title.trim(), form.color, form.sortOrder)
            }
            _uiState.update { it.copy(editor = null) }
        }
    }

    fun deleteEditorSection() {
        val form = _uiState.value.editor ?: return
        val sectionId = form.sectionId ?: return
        viewModelScope.launch {
            deleteSection(sectionId)
            _uiState.update { it.copy(editor = null) }
            sendEvent(UiEvent.ShowSnackbar("Section deleted"))
        }
    }

    fun reorderSections(sections: List<ListSection>) {
        _uiState.update { it.copy(sections = sections) }
        viewModelScope.launch {
            sections.forEachIndexed { index, section ->
                if (section.sortOrder != index) {
                    updateSection(section.id, section.title, section.color, index)
                }
            }
        }
    }

    private fun updateEditor(transform: (ListSectionEditorState) -> ListSectionEditorState) {
        _uiState.update { state ->
            val form = state.editor ?: return@update state
            state.copy(editor = transform(form))
        }
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
