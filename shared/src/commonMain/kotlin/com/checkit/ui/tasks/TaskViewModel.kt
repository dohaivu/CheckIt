package com.checkit.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.NoteWriteInput
import com.checkit.data.TaskListWriteInput
import com.checkit.data.TaskTagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskTag
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddTaskListUseCase
import com.checkit.domain.usecase.AddTaskTagUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.TaskBoardSelection
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateTaskListUseCase
import com.checkit.domain.usecase.UpdateTaskTagUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
import com.checkit.ui.EditorMode
import com.checkit.ui.ListEditorState
import com.checkit.ui.RepeatPreset
import com.checkit.ui.TagEditorState
import com.checkit.ui.TaskEditorState
import com.checkit.ui.TaskUiState
import com.checkit.ui.TaskWorkspaceView
import com.checkit.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

class TaskViewModel(
    private val observeTaskBoard: ObserveTaskBoardUseCase,
    private val ensureDefaultTaskData: EnsureDefaultTaskDataUseCase,
    private val selectTaskBoardItems: SelectTaskBoardItemsUseCase,
    private val addTask: AddTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val completeTask: CompleteTaskUseCase,
    private val addNote: AddNoteUseCase,
    private val updateNote: UpdateNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val addTaskList: AddTaskListUseCase,
    private val updateTaskList: UpdateTaskListUseCase,
    private val addTaskTag: AddTaskTagUseCase,
    private val updateTaskTag: UpdateTaskTagUseCase,
    private val isTagNameTaken: IsTagNameTakenUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensureDefaultTaskData()
            observeTaskBoard()
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = error.message ?: "Unable to load tasks")
                    }
                }
                .collect { board ->
                    _uiState.update { current -> current.withBoard(board) }
                }
        }
    }

    fun selectList(listId: Long) {
        _uiState.update {
            it.copy(selectedListId = listId, selectedFilterId = null, selectedTagId = null).refreshVisibleItems()
        }
    }

    fun selectFilter(filterId: Long) {
        _uiState.update {
            it.copy(selectedListId = null, selectedFilterId = filterId, selectedTagId = null).refreshVisibleItems()
        }
    }

    fun selectTag(tagId: Long) {
        _uiState.update {
            it.copy(selectedListId = null, selectedFilterId = null, selectedTagId = tagId).refreshVisibleItems()
        }
    }

    fun selectView(view: TaskWorkspaceView) {
        _uiState.update { it.copy(selectedView = view) }
    }

    fun openNewTask() {
        val listId = editableListId() ?: return showMessage("Create a list before adding tasks")
        _uiState.update {
            it.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.Add,
                    listId = listId,
                    dueDate = today()
                )
            )
        }
    }

    fun openNewNote() {
        val listId = editableListId() ?: return showMessage("Create a list before adding notes")
        _uiState.update {
            it.copy(editor = TaskEditorState.NoteForm(mode = EditorMode.Add, listId = listId, date = today()))
        }
    }

    fun openTask(task: TaskItem) {
        _uiState.update {
            it.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.View,
                    taskId = task.id,
                    listId = task.listId,
                    name = task.name,
                    description = task.description,
                    dueDate = task.dueDate,
                    startTimeMinutes = task.startTimeMinutes,
                    endTimeMinutes = task.endTimeMinutes,
                    repeatPreset = RepeatPreset.fromRRule(task.repeatRRule),
                    status = task.status,
                    priority = task.priority,
                    selectedTagIds = task.tags.map { it.id }.toSet()
                )
            )
        }
    }

    fun openNote(note: NoteItem) {
        _uiState.update {
            it.copy(
                editor = TaskEditorState.NoteForm(
                    mode = EditorMode.View,
                    noteId = note.id,
                    listId = note.listId,
                    content = note.content,
                    date = note.date,
                    selectedTagIds = note.tags.map { it.id }.toSet()
                )
            )
        }
    }

    fun editCurrentItem() {
        _uiState.update { state ->
            val editor = state.editor ?: return@update state
            val editingEditor = when (editor) {
                is TaskEditorState.TaskForm -> editor.copy(mode = EditorMode.Edit)
                is TaskEditorState.NoteForm -> editor.copy(mode = EditorMode.Edit)
            }
            state.copy(editor = editingEditor)
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun openNewList() {
        _uiState.update { it.copy(listEditor = ListEditorState(mode = EditorMode.Add)) }
    }

    fun openEditList(list: TaskList) {
        _uiState.update {
            it.copy(
                listEditor = ListEditorState(
                    mode = EditorMode.Edit,
                    listId = list.id,
                    name = list.name,
                    color = list.color,
                    icon = list.icon
                )
            )
        }
    }

    fun dismissListEditor() {
        _uiState.update { it.copy(listEditor = null) }
    }

    fun updateListEditorName(name: String) = updateListEditor { it.copy(name = name) }
    fun updateListEditorColor(color: String) = updateListEditor { it.copy(color = color) }
    fun updateListEditorIcon(icon: String) = updateListEditor { it.copy(icon = icon) }

    fun saveListEditor() {
        val form = _uiState.value.listEditor ?: return
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
            _uiState.update {
                it.copy(
                    listEditor = null,
                    selectedListId = savedId,
                    selectedFilterId = null,
                    selectedTagId = null
                ).refreshVisibleItems()
            }
        }
    }

    fun openNewTag() {
        _uiState.update { it.copy(tagEditor = TagEditorState(mode = EditorMode.Add)) }
    }

    fun openEditTag(tag: TaskTag) {
        _uiState.update {
            it.copy(
                tagEditor = TagEditorState(
                    mode = EditorMode.Edit,
                    tagId = tag.id,
                    name = tag.name,
                    color = tag.color,
                    icon = tag.icon
                )
            )
        }
    }

    fun dismissTagEditor() {
        _uiState.update { it.copy(tagEditor = null) }
    }

    fun updateTagEditorName(name: String) = updateTagEditor { it.copy(name = name) }
    fun updateTagEditorColor(color: String) = updateTagEditor { it.copy(color = color) }
    fun updateTagEditorIcon(icon: String) = updateTagEditor { it.copy(icon = icon) }

    fun saveTagEditor() {
        val form = _uiState.value.tagEditor ?: return
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
                color = form.color,
                icon = form.icon
            )
            val savedId = if (form.mode == EditorMode.Add) {
                addTaskTag(input)
            } else {
                val tagId = form.tagId ?: return@launch
                updateTaskTag(tagId, input)
                tagId
            }
            _uiState.update {
                it.copy(
                    tagEditor = null,
                    selectedListId = null,
                    selectedFilterId = null,
                    selectedTagId = savedId
                ).refreshVisibleItems()
            }
        }
    }

    fun updateTaskName(name: String) = updateTaskForm { it.copy(name = name) }
    fun updateTaskDescription(description: String) = updateTaskForm { it.copy(description = description) }
    fun updateTaskDueDate(dueDate: LocalDate?) = updateTaskForm { it.copy(dueDate = dueDate) }
    fun updateTaskStartTime(startTimeMinutes: Int?) = updateTaskForm { it.copy(startTimeMinutes = startTimeMinutes) }
    fun updateTaskEndTime(endTimeMinutes: Int?) = updateTaskForm { it.copy(endTimeMinutes = endTimeMinutes) }
    fun updateTaskRepeat(repeatPreset: RepeatPreset) = updateTaskForm { it.copy(repeatPreset = repeatPreset) }
    fun updateTaskPriority(priority: TaskPriority) = updateTaskForm { it.copy(priority = priority) }
    fun toggleTaskTag(tagId: Long) = updateTaskForm { form ->
        form.copy(selectedTagIds = form.selectedTagIds.toggle(tagId))
    }
    fun updateNoteContent(content: String) = updateNoteForm { it.copy(content = content) }
    fun updateNoteDate(date: LocalDate) = updateNoteForm { it.copy(date = date) }
    fun toggleNoteTag(tagId: Long) = updateNoteForm { form ->
        form.copy(selectedTagIds = form.selectedTagIds.toggle(tagId))
    }

    fun saveEditor() {
        val editor = _uiState.value.editor ?: return
        when (editor) {
            is TaskEditorState.TaskForm -> if (editor.mode != EditorMode.View) saveTask(editor)
            is TaskEditorState.NoteForm -> if (editor.mode != EditorMode.View) saveNote(editor)
        }
    }

    fun deleteEditorItem() {
        val editor = _uiState.value.editor ?: return
        viewModelScope.launch {
            when (editor) {
                is TaskEditorState.TaskForm -> editor.taskId?.let { deleteTask(it) }
                is TaskEditorState.NoteForm -> editor.noteId?.let { deleteNote(it) }
            }
            _uiState.update { it.copy(editor = null, message = "Moved to trash") }
        }
    }

    fun completeCurrentTask() {
        val taskId = (_uiState.value.editor as? TaskEditorState.TaskForm)?.taskId ?: return
        viewModelScope.launch {
            completeTask(taskId)
            _uiState.update { it.copy(editor = null, message = "Task completed") }
        }
    }

    private fun saveTask(form: TaskEditorState.TaskForm) {
        val input = form.toWriteInput() ?: return
        viewModelScope.launch {
            if (form.mode == EditorMode.Add) {
                addTask(input)
            } else {
                updateTask(form.taskId ?: return@launch, input)
            }
            _uiState.update { it.copy(editor = null) }
        }
    }

    private fun saveNote(form: TaskEditorState.NoteForm) {
        if (form.content.isBlank()) {
            showMessage("Add note content")
            return
        }
        viewModelScope.launch {
            val input = NoteWriteInput(
                listId = form.listId,
                content = form.content.trim(),
                date = form.date,
                tagIds = form.selectedTagIds.toList()
            )
            if (form.mode == EditorMode.Add) {
                addNote(input)
            } else {
                updateNote(form.noteId ?: return@launch, input)
            }
            _uiState.update { it.copy(editor = null) }
        }
    }

    private fun TaskEditorState.TaskForm.toWriteInput(): TaskWriteInput? {
        if (name.isBlank()) {
            showMessage("Add a task title")
            return null
        }
        return TaskWriteInput(
            listId = listId,
            name = name.trim(),
            description = description.trim(),
            status = status,
            priority = priority,
            dueDate = dueDate,
            startTimeMinutes = startTimeMinutes,
            endTimeMinutes = endTimeMinutes,
            durationMinutes = durationMinutes,
            repeatRRule = repeatPreset.rrule,
            tagIds = selectedTagIds.toList()
        )
    }

    private fun updateTaskForm(transform: (TaskEditorState.TaskForm) -> TaskEditorState.TaskForm) {
        _uiState.update { state ->
            val form = state.editor as? TaskEditorState.TaskForm ?: return@update state
            state.copy(editor = transform(form))
        }
    }

    private fun updateNoteForm(transform: (TaskEditorState.NoteForm) -> TaskEditorState.NoteForm) {
        _uiState.update { state ->
            val form = state.editor as? TaskEditorState.NoteForm ?: return@update state
            state.copy(editor = transform(form))
        }
    }

    private fun updateListEditor(transform: (ListEditorState) -> ListEditorState) {
        _uiState.update { state ->
            val form = state.listEditor ?: return@update state
            state.copy(listEditor = transform(form))
        }
    }

    private fun updateTagEditor(transform: (TagEditorState) -> TagEditorState) {
        _uiState.update { state ->
            val form = state.tagEditor ?: return@update state
            state.copy(tagEditor = transform(form))
        }
    }

    private fun editableListId(): Long? =
        _uiState.value.selectedListId ?: _uiState.value.board.lists.firstOrNull()?.id

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun TaskUiState.withBoard(board: TaskBoard): TaskUiState {
        val nextListId = selectedListId?.takeIf { selectedId -> board.lists.any { it.id == selectedId } }
            ?: board.lists.firstOrNull()?.id
        val nextFilterId = selectedFilterId?.takeIf { selectedId -> board.filters.any { it.id == selectedId } }
        val nextTagId = selectedTagId?.takeIf { selectedId -> board.tags.any { it.id == selectedId } }
        return copy(
            board = board,
            selectedListId = if (nextFilterId == null && nextTagId == null) nextListId else null,
            selectedFilterId = nextFilterId,
            selectedTagId = nextTagId,
            isLoading = false
        ).refreshVisibleItems()
    }

    private fun TaskUiState.refreshVisibleItems(): TaskUiState {
        selectedTagId?.let { tagId ->
            return copy(
                visibleTasks = board.tasks.filter { task -> !task.isTrashed && task.tags.any { it.id == tagId } },
                visibleNotes = board.notes.filter { note -> !note.isTrashed && note.tags.any { it.id == tagId } }
            )
        }
        val selection = selectedFilter?.let { TaskBoardSelection.FilterSelection(it) }
            ?: selectedListId?.let { TaskBoardSelection.ListSelection(it) }
            ?: return copy(visibleTasks = emptyList(), visibleNotes = emptyList())
        val items = selectTaskBoardItems(board, selection, today())
        return copy(visibleTasks = items.tasks, visibleNotes = items.notes)
    }
}

private fun Set<Long>.toggle(value: Long): Set<Long> =
    if (contains(value)) this - value else this + value
