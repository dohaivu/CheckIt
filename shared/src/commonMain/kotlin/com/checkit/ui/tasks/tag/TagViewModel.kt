package com.checkit.ui.tasks.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.TagWriteInput
import com.checkit.domain.TaskTag
import com.checkit.domain.usecase.AddTagUseCase
import com.checkit.domain.usecase.DeleteTagUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.UpdateTagUseCase
import com.checkit.domain.usecase.UpdateTagSortOrderUseCase
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.tasks.TagEditorState
import com.checkit.ui.UiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TagUiState(
    val editor: TagEditorState? = null,
    val tagUsageCounts: Map<Long, Int> = emptyMap()
)

class TagViewModel(
    private val observeTaskBoard: ObserveTaskBoardUseCase? = null,
    private val addTaskTag: AddTagUseCase,
    private val updateTaskTag: UpdateTagUseCase,
    private val deleteTaskTag: DeleteTagUseCase,
    private val isTagNameTaken: IsTagNameTakenUseCase,
    private val updateTagSortOrder: UpdateTagSortOrderUseCase? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(TagUiState())
    val uiState: StateFlow<TagUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeTaskBoard?.let { observe ->
            viewModelScope.launch {
                observe().collect { board ->
                val counts = withContext(Dispatchers.Default) {
                    val stats = mutableMapOf<Long, Int>()
                    board.tasks.filter { !it.isTrashed }.forEach { task ->
                        task.tags.forEach { tag ->
                            stats[tag.id] = (stats[tag.id] ?: 0) + 1
                        }
                    }
                    board.notes.filter { !it.isTrashed }.forEach { note ->
                        note.tags.forEach { tag ->
                            stats[tag.id] = (stats[tag.id] ?: 0) + 1
                        }
                    }
                    stats
                }
                _uiState.update { it.copy(tagUsageCounts = counts) }
                }
            }
        }
    }

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
            sendEvent(UiEvent.ShowSnackbar("Add a tag name"))
            return
        }
        viewModelScope.launch {
            if (isTagNameTaken(trimmedName, form.tagId)) {
                sendEvent(UiEvent.ShowSnackbar("Tag name already exists"))
                return@launch
            }
            val input = TagWriteInput(
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
            _uiState.update { it.copy(editor = null) }
            sendEvent(UiEvent.ShowSnackbar("Tag deleted"))
            onDeleted()
        }
    }

    fun updateTagSortOrders(tags: List<TaskTag>) {
        val updater = updateTagSortOrder ?: return
        viewModelScope.launch {
            tags.forEachIndexed { index, tag ->
                if (tag.sortOrder != index) updater(tag.id, index)
            }
        }
    }

    private fun updateEditor(transform: (TagEditorState) -> TagEditorState) {
        _uiState.update { state ->
            val form = state.editor ?: return@update state
            state.copy(editor = transform(form))
        }
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
