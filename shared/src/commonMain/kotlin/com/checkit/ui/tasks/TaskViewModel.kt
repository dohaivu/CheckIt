package com.checkit.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.data.NoteWriteInput
import com.checkit.data.SettingsRepository
import com.checkit.data.SubTaskWriteInput
import com.checkit.data.TaskListWriteInput
import com.checkit.data.TaskTagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskReminderPlanner
import com.checkit.domain.TaskReminderPreset
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskTag
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddTaskListUseCase
import com.checkit.domain.usecase.AddTaskTagUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.CompleteNoteUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.OpenNoteUseCase
import com.checkit.domain.usecase.OpenTaskUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.TaskBoardSelection
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateTaskListUseCase
import com.checkit.domain.usecase.UpdateTaskTagUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
import com.checkit.ui.EditorMode
import com.checkit.ui.ListEditorState
import com.checkit.ui.RepeatPreset
import com.checkit.ui.SubTaskEditorState
import com.checkit.ui.TagEditorState
import com.checkit.ui.TaskListDisplayType
import com.checkit.ui.TaskEditorState
import com.checkit.ui.TaskSortOption
import com.checkit.ui.TaskUiState
import com.checkit.ui.TaskWorkspaceView
import com.checkit.ui.components.MinutesPerDay
import com.checkit.ui.toEditorState
import com.checkit.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class TaskViewModel(
    private val observeTaskBoard: ObserveTaskBoardUseCase,
    private val ensureDefaultTaskData: EnsureDefaultTaskDataUseCase,
    private val selectTaskBoardItems: SelectTaskBoardItemsUseCase,
    private val addTask: AddTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val completeTask: CompleteTaskUseCase,
    private val completeNote: CompleteNoteUseCase,
    private val openTask: OpenTaskUseCase,
    private val openNote: OpenNoteUseCase,
    private val addNote: AddNoteUseCase,
    private val updateNote: UpdateNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
    private val updateDailyPlanItem: UpdateDailyPlanItemUseCase,
    private val addTaskList: AddTaskListUseCase,
    private val updateTaskList: UpdateTaskListUseCase,
    private val addTaskTag: AddTaskTagUseCase,
    private val updateTaskTag: UpdateTaskTagUseCase,
    private val isTagNameTaken: IsTagNameTakenUseCase,
    private val settingsRepository: SettingsRepository
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
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        selectedView = TaskWorkspaceView.fromCode(settings.taskWorkspaceViewCode),
                        listDisplayType = TaskListDisplayType.fromCode(settings.taskListDisplayTypeCode),
                        showCompleted = settings.taskShowCompleted,
                        sortOption = TaskSortOption.fromCode(settings.taskSortOptionCode)
                    ).refreshVisibleItems().coerceViewToAvailable()
                }
            }
        }
    }

    fun selectList(listId: Long) {
        _uiState.update {
            it.copy(selectedListId = listId, selectedFilterId = null, selectedTagId = null)
                .refreshVisibleItems()
                .coerceViewToAvailable()
        }
    }

    fun selectFilter(filterId: Long) {
        _uiState.update {
            it.copy(selectedListId = null, selectedFilterId = filterId, selectedTagId = null)
                .refreshVisibleItems()
                .coerceViewToAvailable()
        }
    }

    fun selectTag(tagId: Long) {
        _uiState.update {
            it.copy(selectedListId = null, selectedFilterId = null, selectedTagId = tagId)
                .refreshVisibleItems()
                .coerceViewToAvailable()
        }
    }

    fun selectView(view: TaskWorkspaceView) {
        _uiState.update {
            if (view in it.availableViews) it.copy(selectedView = view) else it
        }
        viewModelScope.launch {
            settingsRepository.setTaskWorkspaceViewCode(view.name)
        }
    }

    fun selectListDisplayType(displayType: TaskListDisplayType) {
        _uiState.update { it.copy(listDisplayType = displayType) }
        viewModelScope.launch {
            settingsRepository.setTaskListDisplayTypeCode(displayType.name)
        }
    }

    fun setShowCompleted(showCompleted: Boolean) {
        _uiState.update { it.copy(showCompleted = showCompleted).refreshVisibleItems() }
        viewModelScope.launch {
            settingsRepository.setTaskShowCompleted(showCompleted)
        }
    }

    fun selectSortOption(sortOption: TaskSortOption) {
        _uiState.update { it.copy(sortOption = sortOption).refreshVisibleItems() }
        viewModelScope.launch {
            settingsRepository.setTaskSortOptionCode(sortOption.name)
        }
    }

    fun openNewTask() {
        openNewTaskOnDate(today())
    }

    fun openNewTaskOnDate(date: LocalDate) {
        val listId = editableListId() ?: return showMessage("Create a list before adding tasks")
        _uiState.update {
            it.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.Add,
                    listId = listId,
                    doDate = date
                )
            )
        }
    }

    fun openNewTaskAt(startTimeMinutes: Int, endTimeMinutes: Int) {
        val listId = editableListId() ?: return showMessage("Create a list before adding tasks")
        _uiState.update {
            it.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.Add,
                    listId = listId,
                    doDate = today(),
                    startTimeMinutes = startTimeMinutes.coerceIn(0, LastTimelineStartMinute),
                    endTimeMinutes = endTimeMinutes.coerceIn(MinimumTimelineDurationMinutes, MinutesPerDay)
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

    fun switchAddEditorToTask() {
        _uiState.update { state ->
            val note = state.editor as? TaskEditorState.NoteForm ?: return@update state
            if (note.mode != EditorMode.Add) return@update state
            state.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.Add,
                    listId = note.listId,
                    name = note.title,
                    description = note.content,
                    doDate = note.date,
                    selectedTagIds = note.selectedTagIds
                )
            )
        }
    }

    fun switchAddEditorToNote() {
        _uiState.update { state ->
            val task = state.editor as? TaskEditorState.TaskForm ?: return@update state
            if (task.mode != EditorMode.Add) return@update state
            state.copy(
                editor = TaskEditorState.NoteForm(
                    mode = EditorMode.Add,
                    listId = task.listId,
                    title = task.name,
                    content = task.description,
                    date = task.doDate ?: today(),
                    startTimeMinutes = task.startTimeMinutes,
                    selectedTagIds = task.selectedTagIds
                )
            )
        }
    }

    fun openTask(task: TaskItem, dailyPlan: DailyPlanItem? = null) {
        _uiState.update {
            it.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.Edit,
                    taskId = task.id,
                    listId = task.list.id,
                    name = task.name,
                    description = task.description,
                    doDate = task.doDate,
                    startTimeMinutes = task.startTimeMinutes,
                    endTimeMinutes = task.endTimeMinutes,
                    repeatPreset = RepeatPreset.fromRRule(task.repeatRRule),
                    subtasks = task.subtasks.map { subtask -> subtask.toEditorState() },
                    reminderOffsets = TaskReminderPlanner.selectedOffsetsFor(task),
                    status = task.status,
                    priority = task.priority,
                    selectedTagIds = task.tags.map { it.id }.toSet(),
                    dailyPlanItem = dailyPlan
                )
            )
        }
    }

    fun openNote(note: NoteItem) {
        _uiState.update {
            it.copy(
                editor = TaskEditorState.NoteForm(
                    mode = EditorMode.Edit,
                    noteId = note.id,
                    listId = note.list.id,
                    title = note.title,
                    content = note.content,
                    status = note.status,
                    date = note.date,
                    startTimeMinutes = note.startTimeMinutes,
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
                    color = tag.color
                )
            )
        }
    }

    fun dismissTagEditor() {
        _uiState.update { it.copy(tagEditor = null) }
    }

    fun updateTagEditorName(name: String) = updateTagEditor { it.copy(name = name) }
    fun updateTagEditorColor(color: String) = updateTagEditor { it.copy(color = color) }

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
                color = form.color
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
    fun updateTaskListId(listId: Long) = updateTaskForm { it.copy(listId = listId) }
    fun updateTaskDescription(description: String) = updateTaskForm { it.copy(description = description) }
    fun updateTaskDoDate(doDate: LocalDate?) = updateTaskForm {
        it.copy(
            doDate = doDate,
            reminderOffsets = if (doDate == null) emptySet() else it.reminderOffsets
        )
    }
    fun updateTaskStartTime(startTimeMinutes: Int?) = updateTaskForm {
        it.copy(
            startTimeMinutes = startTimeMinutes,
            reminderOffsets = TaskReminderPreset.normalizeOffsets(startTimeMinutes, it.reminderOffsets)
        )
    }
    fun updateTaskEndTime(endTimeMinutes: Int?) = updateTaskForm { it.copy(endTimeMinutes = endTimeMinutes) }
    fun updateDailyPlanStartTime(startTimeMinutes: Int?) = updateTaskDailyPlanItem { item ->
        item.copy(
            startTimeMinutes = startTimeMinutes,
            endTimeMinutes = if (startTimeMinutes == null) null else item.endTimeMinutes
        )
    }
    fun updateDailyPlanEndTime(endTimeMinutes: Int?) = updateTaskDailyPlanItem { it.copy(endTimeMinutes = endTimeMinutes) }
    fun updateTaskRepeat(repeatPreset: RepeatPreset) = updateTaskForm { it.copy(repeatPreset = repeatPreset) }
    fun updateTaskPriority(priority: TaskPriority) = updateTaskForm { it.copy(priority = priority) }
    fun toggleTaskReminder(offsetMinutes: Int) = updateTaskForm { form ->
        form.copy(reminderOffsets = form.reminderOffsets.toggle(offsetMinutes))
    }
    fun addSubTask() = updateTaskForm { form ->
        form.copy(subtasks = form.subtasks + SubTaskEditorState(name = ""))
    }
    fun updateSubTaskName(index: Int, name: String) = updateTaskForm { form ->
        form.copy(
            subtasks = form.subtasks.mapIndexed { subtaskIndex, subtask ->
                if (subtaskIndex == index) subtask.copy(name = name) else subtask
            }
        )
    }
    fun removeSubTask(index: Int) = updateTaskForm { form ->
        form.copy(subtasks = form.subtasks.filterIndexed { subtaskIndex, _ -> subtaskIndex != index })
    }
    fun toggleSubTask(index: Int) {
        val current = _uiState.value.editor as? TaskEditorState.TaskForm ?: return
        val nextSubtasks = current.subtasks.mapIndexed { subtaskIndex, subtask ->
            if (subtaskIndex == index) subtask.copy(isCompleted = !subtask.isCompleted) else subtask
        }
        val nextForm = current.copy(subtasks = nextSubtasks)
        _uiState.update { it.copy(editor = nextForm) }
        if (current.mode == EditorMode.View || current.mode == EditorMode.Edit) {
            persistTaskInPlace(nextForm)
        }
    }
    fun toggleTaskTag(tagId: Long) = updateTaskForm { form ->
        form.copy(selectedTagIds = form.selectedTagIds.toggle(tagId))
    }
    fun updateNoteTitle(title: String) = updateNoteForm { it.copy(title = title) }
    fun updateNoteContent(content: String) = updateNoteForm { it.copy(content = content) }
    fun updateNoteListId(listId: Long) = updateNoteForm { it.copy(listId = listId) }
    fun updateNoteDate(date: LocalDate) = updateNoteForm { it.copy(date = date) }
    fun updateNoteStartTime(timeMinutes: Int?) = updateNoteForm { it.copy(startTimeMinutes = timeMinutes) }
    fun toggleNoteTag(tagId: Long) = updateNoteForm { form ->
        form.copy(selectedTagIds = form.selectedTagIds.toggle(tagId))
    }

    fun saveEditor() {
        val editor = _uiState.value.editor ?: return
        when (editor) {
            is TaskEditorState.TaskForm -> if (editor.mode != EditorMode.View) saveTask(editor)
            is TaskEditorState.NoteForm -> if (editor.mode != EditorMode.View) saveNote(editor)
        }

        _uiState.update { it.copy(editor = null) }
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

    fun completeCurrentItem() {
        val editor = _uiState.value.editor ?: return
        viewModelScope.launch {
            when (editor) {
                is TaskEditorState.TaskForm -> completeTask(editor.taskId ?: return@launch)
                is TaskEditorState.NoteForm -> completeNote(editor.noteId ?: return@launch)
            }
            _uiState.update { it.copy(editor = null, message = "Completed") }
        }
    }

    fun openCurrentItem() {
        val editor = _uiState.value.editor ?: return
        viewModelScope.launch {
            when (editor) {
                is TaskEditorState.TaskForm -> openTask(editor.taskId ?: return@launch)
                is TaskEditorState.NoteForm -> openNote(editor.noteId ?: return@launch)
            }
            _uiState.update { it.copy(editor = null, message = "Opened") }
        }
    }

    fun updateDailyPlanStatus() {
        val form = _uiState.value.editor as? TaskEditorState.TaskForm ?: return
        val item = form.dailyPlanItem ?: return
        val nextStatus = when (item.status) {
            DailyPlanItemStatus.Planned -> DailyPlanItemStatus.Done
            DailyPlanItemStatus.Done -> DailyPlanItemStatus.Planned
        }
        val updatedItem = item.copy(status = nextStatus)
        _uiState.update { state ->
            val currentForm = state.editor as? TaskEditorState.TaskForm ?: return@update state
            state.copy(editor = currentForm.copy(dailyPlanItem = updatedItem))
        }
        viewModelScope.launch {
            updateDailyPlanItem(item.id, updatedItem.toWriteInput())
        }
    }

    fun removeDailyPlanItemFromEditor(itemId: Long) {
        _uiState.update { state ->
            val form = state.editor as? TaskEditorState.TaskForm ?: return@update state
            if (form.dailyPlanItem?.id != itemId) return@update state
            state.copy(editor = form.copy(dailyPlanItem = null))
        }
    }

    fun updateTaskTime(task: TaskItem, startTimeMinutes: Int, endTimeMinutes: Int) {
        val normalizedStart = startTimeMinutes.coerceIn(0, LastTimelineStartMinute)
        val normalizedEnd = endTimeMinutes.coerceIn(
            normalizedStart + MinimumTimelineDurationMinutes,
            MinutesPerDay
        )
        val reminderOffsets = TaskReminderPlanner.selectedOffsetsFor(task)
        viewModelScope.launch {
            updateTask(
                task.id,
                task.toWriteInput(
                    startTimeMinutes = normalizedStart,
                    endTimeMinutes = normalizedEnd,
                    reminderOffsets = reminderOffsets
                )
            )
        }
    }

    fun updateNoteTime(note: NoteItem, startTimeMinutes: Int) {
        val normalizedStart = startTimeMinutes.coerceIn(0, MinutesPerDay - 30)
        viewModelScope.launch {
            updateNote(
                note.id,
                NoteWriteInput(
                    listId = note.list.id,
                    title = note.title,
                    content = note.content,
                    status = note.status,
                    date = note.date,
                    startTimeMinutes = normalizedStart,
                    tagIds = note.tags.map { it.id }
                )
            )
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
        }
    }

    private fun saveNote(form: TaskEditorState.NoteForm) {
        if (form.title.isBlank() && form.content.isBlank()) {
            showMessage("Add a note title or content")
            return
        }
        viewModelScope.launch {
            val input = NoteWriteInput(
                listId = form.listId,
                title = form.title.trim(),
                content = form.content.trim(),
                status = form.status,
                date = form.date,
                startTimeMinutes = form.startTimeMinutes,
                tagIds = form.selectedTagIds.toList()
            )
            if (form.mode == EditorMode.Add) {
                addNote(input)
            } else {
                updateNote(form.noteId ?: return@launch, input)
            }
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
            doDate = doDate,
            startTimeMinutes = startTimeMinutes,
            endTimeMinutes = endTimeMinutes,
            durationMinutes = durationMinutes,
            repeatRRule = repeatPreset.rrule,
            subtasks = subtasks
                .map { it.copy(name = it.name.trim()) }
                .filter { it.name.isNotBlank() }
                .map { SubTaskWriteInput(name = it.name, isCompleted = it.isCompleted) },
            reminders = TaskReminderPlanner.buildReminderInputs(
                doDate = doDate,
                startTimeMinutes = startTimeMinutes,
                selectedOffsets = reminderOffsets
            ),
            tagIds = selectedTagIds.toList()
        )
    }

    private fun TaskItem.toWriteInput(
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        reminderOffsets: Set<Int>
    ): TaskWriteInput {
        val duration = calculateDurationMinutes(startTimeMinutes, endTimeMinutes)
        return TaskWriteInput(
            listId = list.id,
            name = name,
            description = description,
            status = status,
            priority = priority,
            doDate = doDate,
            startTimeMinutes = startTimeMinutes,
            endTimeMinutes = endTimeMinutes,
            durationMinutes = duration,
            repeatRRule = repeatRRule,
            subtasks = subtasks.map { SubTaskWriteInput(name = it.name, isCompleted = it.isCompleted) },
            reminders = TaskReminderPlanner.buildReminderInputs(
                doDate = doDate,
                startTimeMinutes = startTimeMinutes,
                selectedOffsets = reminderOffsets
            ),
            tagIds = tags.map { it.id }
        )
    }

    private fun persistTaskInPlace(form: TaskEditorState.TaskForm) {
        val input = form.toWriteInput() ?: return
        val taskId = form.taskId ?: return
        viewModelScope.launch {
            updateTask(taskId, input)
        }
    }

    private fun updateTaskForm(transform: (TaskEditorState.TaskForm) -> TaskEditorState.TaskForm) {
        _uiState.update { state ->
            val form = state.editor as? TaskEditorState.TaskForm ?: return@update state
            val updatedForm = transform(form)
            if (form.mode == EditorMode.Edit) saveTask(updatedForm)
            state.copy(editor = updatedForm)
        }
    }

    private fun updateNoteForm(transform: (TaskEditorState.NoteForm) -> TaskEditorState.NoteForm) {
        _uiState.update { state ->
            val form = state.editor as? TaskEditorState.NoteForm ?: return@update state
            val updatedForm = transform(form)
            if (form.mode == EditorMode.Edit) saveNote(updatedForm)
            state.copy(editor = updatedForm)
        }
    }

    private fun updateTaskDailyPlanItem(transform: (DailyPlanItem) -> DailyPlanItem) {
        val updatedItem = (_uiState.value.editor as? TaskEditorState.TaskForm)
            ?.dailyPlanItem
            ?.let(transform)
            ?: return
        _uiState.update { state ->
            val form = state.editor as? TaskEditorState.TaskForm ?: return@update state
            state.copy(editor = form.copy(dailyPlanItem = updatedItem))
        }
        viewModelScope.launch {
            updateDailyPlanItemTime(updatedItem.id, updatedItem.startTimeMinutes, updatedItem.endTimeMinutes)
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
        ).refreshVisibleItems().coerceViewToAvailable()
    }

    private fun TaskUiState.coerceViewToAvailable(): TaskUiState =
        if (selectedView in availableViews) this else copy(selectedView = TaskWorkspaceView.List)

    private fun TaskUiState.refreshVisibleItems(): TaskUiState {
        selectedTagId?.let { tagId ->
            return withVisibleItems(
                tasks = board.tasks.filter { task -> !task.isTrashed && task.tags.any { it.id == tagId } },
                notes = board.notes.filter { note -> !note.isTrashed && note.tags.any { it.id == tagId } }
            )
        }
        val selection = selectedFilter?.let { TaskBoardSelection.FilterSelection(it) }
            ?: selectedListId?.let { TaskBoardSelection.ListSelection(it) }
            ?: return copy(visibleTasks = emptyList(), visibleNotes = emptyList())
        val items = selectTaskBoardItems(board, selection, today())
        return withVisibleItems(items.tasks, items.notes)
    }

    private fun TaskUiState.withVisibleItems(
        tasks: List<TaskItem>,
        notes: List<NoteItem>
    ): TaskUiState {
        val shouldHideCompleted = !showCompleted && selectedFilter?.status != TaskStatus.Completed
        val completionFilteredTasks = if (shouldHideCompleted) {
            tasks.filter { it.status != TaskStatus.Completed }
        } else {
            tasks
        }
        val completionFilteredNotes = if (shouldHideCompleted) {
            notes.filter { it.status != TaskStatus.Completed }
        } else {
            notes
        }
        return copy(
            visibleTasks = completionFilteredTasks.sortedTasksFor(sortOption),
            visibleNotes = completionFilteredNotes.sortedNotesFor(sortOption)
        )
    }
}

private fun List<TaskItem>.sortedTasksFor(sortOption: TaskSortOption): List<TaskItem> =
    when (sortOption) {
        TaskSortOption.Custom -> sortedWith(compareBy<TaskItem> { it.sortOrder }.thenBy { it.doDate })
        TaskSortOption.Priority -> sortedWith(
            compareBy<TaskItem> { it.priority.rankForSort() }
                .thenBy { it.doDate ?: LocalDate.fromEpochDays(Int.MAX_VALUE) }
                .thenBy { it.sortOrder }
        )
        TaskSortOption.Title -> sortedWith(compareBy<TaskItem> { it.name.lowercase() }.thenBy { it.sortOrder })
        TaskSortOption.Date -> sortedWith(
            compareBy<TaskItem> { it.doDate ?: LocalDate.fromEpochDays(Int.MAX_VALUE) }
                .thenBy { it.startTimeMinutes ?: Int.MAX_VALUE }
                .thenBy { it.sortOrder }
        )
    }

private fun List<NoteItem>.sortedNotesFor(sortOption: TaskSortOption): List<NoteItem> =
    when (sortOption) {
        TaskSortOption.Custom,
        TaskSortOption.Priority -> sortedBy { it.sortOrder }
        TaskSortOption.Title -> sortedWith(
            compareBy<NoteItem> { it.title.ifBlank { it.content }.lowercase() }
                .thenBy { it.sortOrder }
        )
        TaskSortOption.Date -> sortedWith(compareBy<NoteItem> { it.date }.thenBy { it.sortOrder })
    }

private fun TaskPriority.rankForSort(): Int =
    when (this) {
        TaskPriority.High -> 0
        TaskPriority.Medium -> 1
        TaskPriority.Low -> 2
        TaskPriority.None -> 3
    }

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (contains(value)) this - value else this + value

private fun DailyPlanItem.toWriteInput() = DailyPlanItemWriteInput(
    title = titleSnapshot,
    note = note,
    source = source,
    status = status,
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = endTimeMinutes,
    tagIds = tags.map { it.id }
)

private const val MinimumTimelineDurationMinutes = 15
private const val LastTimelineStartMinute = MinutesPerDay - MinimumTimelineDurationMinutes

private fun calculateDurationMinutes(startTimeMinutes: Int?, endTimeMinutes: Int?): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
    return if (end >= start) {
        end - start
    } else {
        MinutesPerDay - start + end
    }
}
