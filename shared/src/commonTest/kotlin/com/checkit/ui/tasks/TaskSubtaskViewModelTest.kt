package com.checkit.ui.tasks

import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
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
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateTaskListUseCase
import com.checkit.domain.usecase.UpdateTaskTagUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
import com.checkit.ui.EditorMode
import com.checkit.ui.TaskEditorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TaskSubtaskViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: TaskViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveNewTaskPersistsNonBlankSubtasksInOrder() = runTest(dispatcher) {
        createViewModel(TaskBoard(lists = listOf(inboxList())))
        viewModel.openNewTask()
        viewModel.updateTaskName(" Launch checklist ")
        viewModel.addSubTask()
        viewModel.updateSubTaskName(0, "  Draft notes  ")
        viewModel.addSubTask()
        viewModel.updateSubTaskName(1, "   ")
        viewModel.addSubTask()
        viewModel.updateSubTaskName(2, "Send update")
        viewModel.toggleSubTask(2)

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val input = repository.addedTasks.single()
        assertEquals(listOf("Draft notes", "Send update"), input.subtasks.map { it.name })
        assertEquals(listOf(false, true), input.subtasks.map { it.isCompleted })
        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun toggleSubtaskInViewModePersistsWithoutLeavingEditor() = runTest(dispatcher) {
        createViewModel(TaskBoard(lists = listOf(inboxList()), tasks = listOf(taskWithSubtasks())))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.openTask(taskWithSubtasks())

        viewModel.toggleSubTask(0)
        dispatcher.scheduler.advanceUntilIdle()

        val (taskId, input) = repository.updatedTasks.single()
        assertEquals(42L, taskId)
        assertEquals(listOf(true, true), input.subtasks.map { it.isCompleted })
        val editor = viewModel.uiState.value.editor as TaskEditorState.TaskForm
        assertEquals(EditorMode.View, editor.mode)
        assertTrue(editor.subtasks[0].isCompleted)
    }

    @Test
    fun editModeCanRenameRemoveAndSaveSubtasks() = runTest(dispatcher) {
        createViewModel(TaskBoard(lists = listOf(inboxList()), tasks = listOf(taskWithSubtasks())))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.openTask(taskWithSubtasks())
        viewModel.editCurrentItem()
        viewModel.updateSubTaskName(0, "Revised")
        viewModel.removeSubTask(1)

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val input = repository.updatedTasks.single().second
        assertEquals(listOf("Revised"), input.subtasks.map { it.name })
        assertFalse(input.subtasks.single().isCompleted)
        assertNull(viewModel.uiState.value.editor)
    }

    private fun createViewModel(board: TaskBoard) {
        repository = FakeCheckItRepository(initialBoard = board)
        viewModel = TaskViewModel(
            observeTaskBoard = ObserveTaskBoardUseCase(repository),
            ensureDefaultTaskData = EnsureDefaultTaskDataUseCase(repository),
            selectTaskBoardItems = SelectTaskBoardItemsUseCase(),
            addTask = AddTaskUseCase(repository),
            updateTask = UpdateTaskUseCase(repository),
            deleteTask = DeleteTaskUseCase(repository),
            completeTask = CompleteTaskUseCase(repository),
            addNote = AddNoteUseCase(repository),
            updateNote = UpdateNoteUseCase(repository),
            deleteNote = DeleteNoteUseCase(repository),
            addTaskList = AddTaskListUseCase(repository),
            updateTaskList = UpdateTaskListUseCase(repository),
            addTaskTag = AddTaskTagUseCase(repository),
            updateTaskTag = UpdateTaskTagUseCase(repository),
            isTagNameTaken = IsTagNameTakenUseCase(repository)
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    private fun inboxList() = TaskList(
        id = 1L,
        name = "Inbox",
        color = "#2563EB",
        icon = "Inbox",
        sortOrder = 0
    )

    private fun taskWithSubtasks() = TaskItem(
        id = 42L,
        listId = 1L,
        name = "Ship",
        subtasks = listOf(
            SubTaskItem(id = 10L, taskId = 42L, name = "Draft", isCompleted = false, sortOrder = 0),
            SubTaskItem(id = 11L, taskId = 42L, name = "Send", isCompleted = true, sortOrder = 1)
        ),
        sortOrder = 0,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )
}
