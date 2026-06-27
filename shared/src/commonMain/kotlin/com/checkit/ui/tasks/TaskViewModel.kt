package com.checkit.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.NoteWriteInput
import com.checkit.data.SettingsRepository
import com.checkit.data.SubTaskWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.KeyResult
import com.checkit.domain.NoteItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskReminderPlanner
import com.checkit.domain.TaskReminderPreset
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.AutoUpdateKeyResultCurrentValueUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.CompleteNoteUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.OpenNoteUseCase
import com.checkit.domain.usecase.OpenTaskUseCase
import com.checkit.domain.usecase.RestoreNoteUseCase
import com.checkit.domain.usecase.RestoreTaskUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemStatusUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
import com.checkit.ui.EditorMode
import com.checkit.ui.RepeatPreset
import com.checkit.ui.SubTaskEditorState
import com.checkit.ui.TaskListDisplayType
import com.checkit.ui.TaskEditorState
import com.checkit.ui.TaskSelectionState
import com.checkit.ui.TaskSortOption
import com.checkit.ui.TaskUiState
import com.checkit.ui.TaskViewOptionsState
import com.checkit.ui.TaskWorkspaceView
import com.checkit.ui.UiEvent
import com.checkit.ui.components.MinutesPerDay
import com.checkit.ui.toEditorState
import com.checkit.ui.today
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class TaskViewModel(
    private val observeTaskBoard: ObserveTaskBoardUseCase,
    private val ensureDefaultTaskData: EnsureDefaultTaskDataUseCase,
    private val selectTaskBoardItems: SelectTaskBoardItemsUseCase,
    private val addTask: AddTaskUseCase,
    private val addTaskToDailyPlan: AddTaskToDailyPlanUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val restoreTask: RestoreTaskUseCase,
    private val completeTask: CompleteTaskUseCase,
    private val completeNote: CompleteNoteUseCase,
    private val openTask: OpenTaskUseCase,
    private val openNote: OpenNoteUseCase,
    private val addNote: AddNoteUseCase,
    private val updateNote: UpdateNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val restoreNote: RestoreNoteUseCase,
    private val updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
    private val updateDailyPlanItemStatus: UpdateDailyPlanItemStatusUseCase,
    private val autoUpdateKeyResultCurrentValue: AutoUpdateKeyResultCurrentValueUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()
    private val visibleItemsBuilder = TaskVisibleItemsBuilder(selectTaskBoardItems)
    private var pendingTaskTextSaveJob: Job? = null

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            ensureDefaultTaskData()
            observeTaskBoard()
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                    sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to load tasks"))
                }
                .collect { board ->
                    _uiState.update { current -> current.withBoard(board) }
                }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { state ->
                    val persistedView = TaskWorkspaceView.fromCode(settings.taskWorkspaceViewCode)
                    val effectiveView = if (state.selectedView == TaskWorkspaceView.Goal) {
                        state.selectedView
                    } else {
                        persistedView
                    }
                    val nextOptions = state.options.copy(
                        selectedView = effectiveView,
                        listDisplayType = TaskListDisplayType.fromCode(settings.taskListDisplayTypeCode),
                        showCompleted = settings.taskShowCompleted,
                        sortOption = TaskSortOption.fromCode(settings.taskSortOptionCode)
                    )
                    if (nextOptions == state.options) {
                        state
                    } else if (nextOptions.hasSameVisibleItemsAs(state.options)) {
                        state.copy(options = nextOptions).coerceViewToAvailable()
                    } else {
                        state.copy(options = nextOptions)
                            .refreshVisibleItems()
                            .coerceViewToAvailable()
                    }
                }
            }
        }
    }

    fun selectBoard() {
        _uiState.update {
            it.copy(selection = TaskSelectionState())
                .refreshVisibleItems()
                .coerceViewToAvailable()
        }
    }

    fun selectList(objectiveId: Long) {
        _uiState.update {
            it.copy(selection = TaskSelectionState(selectedListId = objectiveId))
                .refreshVisibleItems()
                .coerceViewToAvailable()
        }
    }

    fun selectGoal(goalId: Long) {
        _uiState.update {
            it.copy(selection = TaskSelectionState(selectedGoalId = goalId), options = it.options.copy(selectedView = TaskWorkspaceView.Goal))
                .refreshVisibleItems()
                .coerceViewToAvailable()
        }
    }

    fun selectFilter(filterId: Long) {
        _uiState.update { state ->
            val nextFilterId = filterId.takeUnless { state.options.selectedFilterId == filterId }
            state.copy(options = state.options.copy(selectedFilterId = nextFilterId))
                .refreshVisibleItems()
                .coerceViewToAvailable()
        }
    }

    fun selectTag(tagId: Long) {
        _uiState.update {
            it.copy(selection = TaskSelectionState(selectedTagId = tagId))
                .refreshVisibleItems()
                .coerceViewToAvailable()
        }
    }

    fun selectView(view: TaskWorkspaceView) {
        var shouldPersist = false
        _uiState.update {
            if (view == it.selectedView || view !in it.availableViews) {
                it
            } else {
                shouldPersist = true
                it.copy(options = it.options.copy(selectedView = view))
            }
        }
        if (shouldPersist && view != TaskWorkspaceView.Goal) {
            viewModelScope.launch {
                settingsRepository.setTaskWorkspaceViewCode(view.name)
            }
        }
    }

    fun selectListDisplayType(displayType: TaskListDisplayType) {
        var shouldPersist = false
        _uiState.update {
            if (displayType == it.listDisplayType) {
                it
            } else {
                shouldPersist = true
                it.copy(options = it.options.copy(listDisplayType = displayType))
            }
        }
        if (shouldPersist) {
            viewModelScope.launch {
                settingsRepository.setTaskListDisplayTypeCode(displayType.name)
            }
        }
    }

    fun setShowCompleted(showCompleted: Boolean) {
        var shouldPersist = false
        _uiState.update {
            if (showCompleted == it.showCompleted) {
                it
            } else {
                shouldPersist = true
                it.copy(options = it.options.copy(showCompleted = showCompleted)).refreshVisibleItems()
            }
        }
        if (shouldPersist) {
            viewModelScope.launch {
                settingsRepository.setTaskShowCompleted(showCompleted)
            }
        }
    }

    fun updateSearchText(searchText: String) {
        _uiState.update {
            if (searchText == it.searchText) {
                it
            } else {
                it.copy(options = it.options.copy(searchText = searchText)).refreshVisibleItems()
            }
        }
    }

    fun selectSortOption(sortOption: TaskSortOption) {
        var shouldPersist = false
        _uiState.update {
            if (sortOption == it.sortOption) {
                it
            } else {
                shouldPersist = true
                it.copy(options = it.options.copy(sortOption = sortOption)).refreshVisibleItems()
            }
        }
        if (shouldPersist) {
            viewModelScope.launch {
                settingsRepository.setTaskSortOptionCode(sortOption.name)
            }
        }
    }

    fun openNewTask(addToMyDayOnSave: Boolean = false) {
        openNewTaskOnDate(today(), addToMyDayOnSave)
    }

    fun openNewTaskOnKeyResult(keyResult: KeyResult) {
        cancelPendingTaskTextSave()
        _uiState.update {
            it.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.Add,
                    objectiveId = keyResult.objectiveId,
                    keyResultId = keyResult.id,
                    doDate = null,
                )
            )
        }
    }

    fun openNewTaskOnDate(date: LocalDate, addToMyDayOnSave: Boolean = false) {
        val objectiveId = editableListId() ?: return sendEvent(UiEvent.ShowSnackbar("Create a list before adding tasks"))
        cancelPendingTaskTextSave()
        _uiState.update {
            it.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.Add,
                    objectiveId = objectiveId,
                    doDate = date,
                    addToMyDayOnSave = addToMyDayOnSave
                )
            )
        }
    }

    fun openNewTaskAt(startTimeMinutes: Int, endTimeMinutes: Int) {
        val objectiveId = editableListId() ?: return sendEvent(UiEvent.ShowSnackbar("Create a list before adding tasks"))
        cancelPendingTaskTextSave()
        _uiState.update {
            it.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.Add,
                    objectiveId = objectiveId,
                    doDate = today(),
                    startTimeMinutes = startTimeMinutes.coerceIn(0, LastTimelineStartMinute),
                    endTimeMinutes = endTimeMinutes.coerceIn(MinimumTimelineDurationMinutes, MinutesPerDay)
                )
            )
        }
    }

    fun openNewNote(objectiveId: Long) {
        cancelPendingTaskTextSave()
        _uiState.update {
            it.copy(editor = TaskEditorState.NoteForm(mode = EditorMode.Add, objectiveId = objectiveId, date = today()))
        }
    }

    fun openNewNote() {
        val objectiveId = editableListId() ?: return sendEvent(UiEvent.ShowSnackbar("Create a list before adding notes"))
        cancelPendingTaskTextSave()
        _uiState.update {
            it.copy(editor = TaskEditorState.NoteForm(mode = EditorMode.Add, objectiveId = objectiveId, date = today()))
        }
    }

    fun switchAddEditorToTask() {
        _uiState.update { state ->
            val note = state.editor as? TaskEditorState.NoteForm ?: return@update state
            if (note.mode != EditorMode.Add) return@update state
            state.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.Add,
                    objectiveId = note.objectiveId,
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
                    objectiveId = task.objectiveId,
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
        cancelPendingTaskTextSave()
        _uiState.update {
            it.copy(
                editor = TaskEditorState.TaskForm(
                    mode = EditorMode.Edit,
                    taskId = task.id,
                    objectiveId = task.objective.id,
                    keyResultId = task.keyResult?.id,
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
                    dailyPlanItem = dailyPlan,
                    trashedAtMillis = task.trashedAtMillis
                )
            )
        }
    }

    fun openNote(note: NoteItem) {
        cancelPendingTaskTextSave()
        _uiState.update {
            it.copy(
                editor = TaskEditorState.NoteForm(
                    mode = EditorMode.Edit,
                    noteId = note.id,
                    objectiveId = note.objective.id,
                    title = note.title,
                    content = note.content,
                    status = note.status,
                    date = note.date,
                    startTimeMinutes = note.startTimeMinutes,
                    selectedTagIds = note.tags.map { it.id }.toSet(),
                    trashedAtMillis = note.trashedAtMillis
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
        flushPendingTaskTextSave()
        _uiState.update { it.copy(editor = null) }
    }

    fun updateTaskName(name: String) = updateTaskForm(saveImmediately = false) { it.copy(name = name) }
    fun updateTaskListId(listId: Long) = updateTaskForm { it.copy(objectiveId = listId) }
    fun updateTaskDescription(description: String) = updateTaskForm(saveImmediately = false) { it.copy(description = description) }
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
    fun updateSubTaskName(index: Int, name: String) = updateTaskForm(saveImmediately = false) { form ->
        form.copy(
            subtasks = form.subtasks.mapIndexed { subtaskIndex, subtask ->
                if (subtaskIndex == index) subtask.copy(name = name) else subtask
            }
        )
    }
    fun removeSubTask(index: Int) = updateTaskForm { form ->
        form.copy(subtasks = form.subtasks.filterIndexed { subtaskIndex, _ -> subtaskIndex != index })
    }
    fun moveSubTask(fromIndex: Int, toIndex: Int) = updateTaskForm { form ->
        if (fromIndex == toIndex ||
            fromIndex !in form.subtasks.indices ||
            toIndex !in form.subtasks.indices
        ) {
            form
        } else {
            form.copy(subtasks = form.subtasks.move(fromIndex, toIndex))
        }
    }
    fun toggleSubTask(index: Int) {
        val current = _uiState.value.editor as? TaskEditorState.TaskForm ?: return
        val nextSubtasks = current.subtasks.mapIndexed { subtaskIndex, subtask ->
            if (subtaskIndex == index) subtask.copy(isCompleted = !subtask.isCompleted) else subtask
        }
        val nextForm = current.copy(subtasks = nextSubtasks)
        _uiState.update { it.copy(editor = nextForm) }
        if (current.mode == EditorMode.View || current.mode == EditorMode.Edit) {
            cancelPendingTaskTextSave()
            persistTaskInPlace(nextForm)
        }
    }
    fun toggleTaskTag(tagId: Long) = updateTaskForm { form ->
        form.copy(selectedTagIds = form.selectedTagIds.toggle(tagId))
    }
    fun updateNoteTitle(title: String) = updateNoteForm { it.copy(title = title) }
    fun updateNoteContent(content: String) = updateNoteForm { it.copy(content = content) }
    fun updateNoteListId(listId: Long) = updateNoteForm { it.copy(objectiveId = listId) }
    fun updateNoteDate(date: LocalDate) = updateNoteForm { it.copy(date = date) }
    fun updateNoteStartTime(timeMinutes: Int?) = updateNoteForm { it.copy(startTimeMinutes = timeMinutes) }
    fun toggleNoteTag(tagId: Long) = updateNoteForm { form ->
        form.copy(selectedTagIds = form.selectedTagIds.toggle(tagId))
    }

    fun saveEditor() {
        flushPendingTaskTextSave()
        val editor = _uiState.value.editor ?: return
        when (editor) {
            is TaskEditorState.TaskForm -> if (editor.mode != EditorMode.View) saveTask(editor)
            is TaskEditorState.NoteForm -> if (editor.mode != EditorMode.View) saveNote(editor)
        }

        _uiState.update { it.copy(editor = null) }
    }

    fun deleteEditorItem() {
        cancelPendingTaskTextSave()
        val editor = _uiState.value.editor ?: return
        viewModelScope.launch {
            when (editor) {
                is TaskEditorState.TaskForm -> editor.taskId?.let { deleteTask(it) }
                is TaskEditorState.NoteForm -> editor.noteId?.let { deleteNote(it) }
            }
            _uiState.update { it.copy(editor = null) }
            sendEvent(UiEvent.ShowSnackbar("Moved to trash"))
        }
    }

    fun completeCurrentItem() {
        flushPendingTaskTextSave()
        val editor = _uiState.value.editor ?: return
        viewModelScope.launch {
            when (editor) {
                is TaskEditorState.TaskForm -> completeTask(editor.taskId ?: return@launch)
                is TaskEditorState.NoteForm -> completeNote(editor.noteId ?: return@launch)
            }
            _uiState.update { it.copy(editor = null) }
            sendEvent(UiEvent.ShowSnackbar("Completed"))
        }
    }

    fun openCurrentItem() {
        flushPendingTaskTextSave()
        val editor = _uiState.value.editor ?: return
        viewModelScope.launch {
            when (editor) {
                is TaskEditorState.TaskForm -> openTask(editor.taskId ?: return@launch)
                is TaskEditorState.NoteForm -> openNote(editor.noteId ?: return@launch)
            }
            _uiState.update { it.copy(editor = null) }
            sendEvent(UiEvent.ShowSnackbar("Opened"))
        }
    }

    fun restoreCurrentItem() {
        cancelPendingTaskTextSave()
        val editor = _uiState.value.editor ?: return
        viewModelScope.launch {
            when (editor) {
                is TaskEditorState.TaskForm -> restoreTask(editor.taskId ?: return@launch)
                is TaskEditorState.NoteForm -> restoreNote(editor.noteId ?: return@launch)
            }
            _uiState.update { state ->
                when (val current = state.editor) {
                    is TaskEditorState.TaskForm -> state.copy(editor = current.copy(trashedAtMillis = null))
                    is TaskEditorState.NoteForm -> state.copy(editor = current.copy(trashedAtMillis = null))
                    null -> state
                }.copy(editor = null)
            }
            sendEvent(UiEvent.ShowSnackbar("Restored"))
        }
    }

    fun updateDailyPlanStatus() {
        val form = _uiState.value.editor as? TaskEditorState.TaskForm ?: return
        val item = form.dailyPlanItem ?: return
        val previousStatus = item.status
        val nextStatus = when (previousStatus) {
            DailyPlanItemStatus.Planned -> DailyPlanItemStatus.Done
            DailyPlanItemStatus.Done -> DailyPlanItemStatus.Planned
        }
        val updatedItem = item.copy(status = nextStatus)
        _uiState.update { state ->
            val currentForm = state.editor as? TaskEditorState.TaskForm ?: return@update state
            state.copy(editor = currentForm.copy(dailyPlanItem = updatedItem))
        }
        viewModelScope.launch {
            updateDailyPlanItemStatus(item.id, nextStatus)
            autoUpdateKeyResultCurrentValue(item, previousStatus, nextStatus, _uiState.value.board)
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
                    objectiveId = note.objective.id,
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
                val taskId = addTask(input)
                if (form.addToMyDayOnSave) {
                    val task = _uiState.value.board.tasksById[taskId]
                        ?: observeTaskBoard()
                            .mapNotNull { board -> board.tasksById[taskId] }
                            .first()
                    addTaskToDailyPlan(today(), task)
                }
            } else {
                updateTask(form.taskId ?: return@launch, input)
            }
        }
    }

    private fun saveNote(form: TaskEditorState.NoteForm) {
        if (form.title.isBlank() && form.content.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Add a note title or content"))
            return
        }
        viewModelScope.launch {
            val input = NoteWriteInput(
                objectiveId = form.objectiveId,
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
            sendEvent(UiEvent.ShowSnackbar("Add a task title"))
            return null
        }
        return TaskWriteInput(
            objectiveId = objectiveId,
            keyResultId = keyResultId,
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
            objectiveId = objective.id,
            keyResultId = keyResult?.id,
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

    private fun updateTaskForm(
        saveImmediately: Boolean = true,
        transform: (TaskEditorState.TaskForm) -> TaskEditorState.TaskForm
    ) {
        var updatedForm: TaskEditorState.TaskForm? = null
        _uiState.update { state ->
            val form = state.editor as? TaskEditorState.TaskForm ?: return@update state
            updatedForm = transform(form)
            state.copy(editor = updatedForm)
        }
        val form = updatedForm ?: return
        if (form.mode != EditorMode.Edit) return
        if (saveImmediately) {
            cancelPendingTaskTextSave()
            saveTask(form)
        } else {
            scheduleTaskTextSave()
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

    private fun scheduleTaskTextSave() {
        pendingTaskTextSaveJob?.cancel()
        pendingTaskTextSaveJob = viewModelScope.launch {
            delay(TaskTextSaveDebounceMillis)
            pendingTaskTextSaveJob = null
            saveCurrentTaskForm()
        }
    }

    private fun flushPendingTaskTextSave() {
        val pendingSave = pendingTaskTextSaveJob ?: return
        pendingSave.cancel()
        pendingTaskTextSaveJob = null
        saveCurrentTaskForm()
    }

    private fun cancelPendingTaskTextSave() {
        pendingTaskTextSaveJob?.cancel()
        pendingTaskTextSaveJob = null
    }

    private fun saveCurrentTaskForm() {
        val form = _uiState.value.editor as? TaskEditorState.TaskForm ?: return
        if (form.mode == EditorMode.Edit) saveTask(form)
    }

    private fun editableListId(): Long? =
        _uiState.value.selectedListId ?: _uiState.value.board.objectives.firstOrNull()?.id

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun TaskUiState.withBoard(board: TaskBoard): TaskUiState {
        val nextListId = selectedListId?.takeIf { selectedId -> board.objectives.any { it.id == selectedId } }
        val nextFilterId = options.selectedFilterId?.takeIf { selectedId -> board.filters.any { it.id == selectedId } }
        val nextTagId = selectedTagId?.takeIf { selectedId -> board.tags.any { it.id == selectedId } }
        val nextGoalId = selectedGoalId?.takeIf { selectedId -> board.goals.any { it.id == selectedId } }
        return copy(
            board = board,
            selection = TaskSelectionState(
                selectedListId = nextListId,
                selectedTagId = nextTagId,
                selectedGoalId = nextGoalId
            ),
            options = options.copy(selectedFilterId = nextFilterId),
            isLoading = false
        ).refreshVisibleItems().coerceViewToAvailable()
    }

    private fun TaskUiState.coerceViewToAvailable(): TaskUiState =
        if (selectedView in availableViews) {
            this
        } else {
            copy(options = options.copy(selectedView = TaskWorkspaceView.List))
        }

    private fun TaskUiState.refreshVisibleItems(): TaskUiState {
        return copy(
            visibleItems = visibleItemsBuilder.build(
                board = board,
                selection = selection,
                options = options,
                today = today()
            )
        )
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (contains(value)) this - value else this + value

private fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T> =
    toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }

private fun TaskViewOptionsState.hasSameVisibleItemsAs(other: TaskViewOptionsState): Boolean =
    showCompleted == other.showCompleted &&
        searchText == other.searchText &&
        sortOption == other.sortOption

private const val MinimumTimelineDurationMinutes = 15
private const val LastTimelineStartMinute = MinutesPerDay - MinimumTimelineDurationMinutes
private const val TaskTextSaveDebounceMillis = 600L

private fun calculateDurationMinutes(startTimeMinutes: Int?, endTimeMinutes: Int?): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
    return if (end >= start) {
        end - start
    } else {
        MinutesPerDay - start + end
    }
}
