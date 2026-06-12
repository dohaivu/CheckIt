package com.checkit.ui.tasks

import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddTaskListUseCase
import com.checkit.domain.usecase.AddTaskTagUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.CompleteNoteUseCase
import com.checkit.domain.usecase.OpenTaskUseCase
import com.checkit.domain.usecase.OpenNoteUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class TaskTimelineViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: TaskViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                lists = listOf(inboxList()),
                tasks = listOf(timedTask())
            )
        )
        viewModel = TaskViewModel(
            observeTaskBoard = ObserveTaskBoardUseCase(repository),
            ensureDefaultTaskData = EnsureDefaultTaskDataUseCase(repository),
            selectTaskBoardItems = SelectTaskBoardItemsUseCase(),
            addTask = AddTaskUseCase(repository),
            updateTask = UpdateTaskUseCase(repository),
            deleteTask = DeleteTaskUseCase(repository),
            completeTask = CompleteTaskUseCase(repository),
            completeNote = CompleteNoteUseCase(repository),
            openTask = OpenTaskUseCase(repository),
            openNote = OpenNoteUseCase(repository),
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

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openNewTaskAtPrefillsTimelineSlot() = runTest(dispatcher) {
        viewModel.openNewTaskAt(10 * 60 + 15, 11 * 60 + 15)

        val editor = viewModel.uiState.value.editor as TaskEditorState.TaskForm
        assertEquals(EditorMode.Add, editor.mode)
        assertEquals(10 * 60 + 15, editor.startTimeMinutes)
        assertEquals(11 * 60 + 15, editor.endTimeMinutes)
    }

    @Test
    fun updateTaskTimePersistsTimelineMove() = runTest(dispatcher) {
        val task = timedTask()

        viewModel.updateTaskTime(task, 12 * 60, 13 * 60 + 15)
        dispatcher.scheduler.advanceUntilIdle()

        val (taskId, input) = repository.updatedTasks.single()
        assertEquals(task.id, taskId)
        assertEquals(12 * 60, input.startTimeMinutes)
        assertEquals(13 * 60 + 15, input.endTimeMinutes)
        assertEquals(75, input.durationMinutes)
    }

    private fun inboxList() = TaskList(
        id = 1L,
        name = "Inbox",
        color = "#2563EB",
        icon = "Inbox",
        sortOrder = 0
    )

    private fun timedTask() = TaskItem(
        id = 7L,
        list = TaskList.None,
        name = "Focus",
        startTimeMinutes = 9 * 60,
        endTimeMinutes = 10 * 60,
        durationMinutes = 60,
        sortOrder = 0,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )
}
