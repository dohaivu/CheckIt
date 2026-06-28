package com.checkit.ui.tasks

import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.Objective
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.AutoUpdateKeyResultCurrentValueUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.CompleteNoteUseCase
import com.checkit.domain.usecase.OpenTaskUseCase
import com.checkit.domain.usecase.OpenNoteUseCase
import com.checkit.domain.usecase.RestoreNoteUseCase
import com.checkit.domain.usecase.RestoreTaskUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemStatusUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
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
                objectives = listOf(inboxList()),
                tasks = listOf(timedTask())
            )
        )
        viewModel = TaskViewModel(
            observeTaskBoard = ObserveTaskBoardUseCase(repository),
            ensureDefaultTaskData = EnsureDefaultTaskDataUseCase(repository),
            selectTaskBoardItems = SelectTaskBoardItemsUseCase(),
            addTask = AddTaskUseCase(repository),
            addTaskToDailyPlan = AddTaskToDailyPlanUseCase(repository),
            updateTask = UpdateTaskUseCase(repository),
            deleteTask = DeleteTaskUseCase(repository),
            restoreTask = RestoreTaskUseCase(repository),
            completeTask = CompleteTaskUseCase(repository),
            completeNote = CompleteNoteUseCase(repository),
            openTask = OpenTaskUseCase(repository),
            openNote = OpenNoteUseCase(repository),
            addNote = AddNoteUseCase(repository),
            updateNote = UpdateNoteUseCase(repository),
            deleteNote = DeleteNoteUseCase(repository),
            restoreNote = RestoreNoteUseCase(repository),
            updateDailyPlanItemTime = UpdateDailyPlanItemTimeUseCase(repository),
            updateDailyPlanItemStatus = UpdateDailyPlanItemStatusUseCase(repository),
            autoUpdateKeyResultCurrentValue = AutoUpdateKeyResultCurrentValueUseCase(repository),
            settingsRepository = FakeSettingsRepository()
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

    private fun inboxList() = Objective(
        id = 1L,
        name = "Inbox",
        color = "#2563EB",
        icon = "Inbox",
        sortOrder = 0
    )

    private fun timedTask() = TaskItem(
        id = 7L,
        objective = Objective.None,
        name = "Focus",
        startTimeMinutes = 9 * 60,
        endTimeMinutes = 10 * 60,
        durationMinutes = 60,
        sortOrder = 0,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )
}
