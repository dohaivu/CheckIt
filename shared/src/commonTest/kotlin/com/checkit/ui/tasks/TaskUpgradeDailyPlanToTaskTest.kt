package com.checkit.ui.tasks

import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.CompleteNoteUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.GetNoteUseCase
import com.checkit.domain.usecase.GetTaskUseCase
import com.checkit.domain.usecase.LinkDailyPlanItemToTaskUseCase
import com.checkit.domain.usecase.MoveNoteUseCase
import com.checkit.domain.usecase.MoveTaskUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.RestoreNoteUseCase
import com.checkit.domain.usecase.RestoreTaskUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemStatusUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTagUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateNoteStatusUseCase
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateTaskStatusUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TaskUpgradeDailyPlanToTaskTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: TaskViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        viewModel = TaskViewModel(
            observeTaskBoard = ObserveTaskBoardUseCase(repository),
            selectTaskBoardItems = SelectTaskBoardItemsUseCase(),
            getTask = GetTaskUseCase(repository),
            getNote = GetNoteUseCase(repository),
            addTask = AddTaskUseCase(repository),
            addTaskToDailyPlan = AddTaskToDailyPlanUseCase(repository),
            updateTask = UpdateTaskUseCase(repository),
            deleteTask = DeleteTaskUseCase(repository),
            restoreTask = RestoreTaskUseCase(repository),
            completeTask = CompleteTaskUseCase(repository),
            completeNote = CompleteNoteUseCase(repository),
            updateTaskStatus = UpdateTaskStatusUseCase(repository),
            updateNoteStatus = UpdateNoteStatusUseCase(repository),
            addNote = AddNoteUseCase(repository),
            updateNote = UpdateNoteUseCase(repository),
            deleteNote = DeleteNoteUseCase(repository),
            restoreNote = RestoreNoteUseCase(repository),
            moveTask = MoveTaskUseCase(repository),
            moveNote = MoveNoteUseCase(repository),
            updateDailyPlanItemTime = UpdateDailyPlanItemTimeUseCase(repository),
            updateDailyPlanItemStatus = UpdateDailyPlanItemStatusUseCase(repository),
            updateDailyPlanItemTag = UpdateDailyPlanItemTagUseCase(repository),
            linkDailyPlanItemToTask = LinkDailyPlanItemToTaskUseCase(repository),
            settingsRepository = FakeSettingsRepository()
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun seedPlanItem(): Long = repository.addDailyPlanItem(
        date = LocalDate(2026, 8, 24),
        title = "Draft quarterly report",
        note = "Still missing the metrics section",
        startTimeMinutes = null,
        endTimeMinutes = null,
        source = DailyPlanItemSource.MyDayTask,
        status = DailyPlanItemStatus.Planned,
        tagIds = emptyList(),
        label = null,
        taskId = null,
        nestedListItemId = null,
        carriedFromItemId = null
    )

    @Test
    fun openNewTaskFromDailyPlanPrefillsForm() = runTestWithDispatcher {
        val planItemId = seedPlanItem()

        viewModel.openNewTaskFromDailyPlan(
            planItemId = planItemId,
            title = "Draft quarterly report",
            note = "Still missing the metrics section",
            label = "work",
            tagIds = setOf(7L, 9L)
        )
        dispatcher.scheduler.advanceUntilIdle()

        val form = viewModel.uiState.value.editor as? TaskEditorState.TaskForm
            ?: error("Expected a TaskForm editor")
        assertEquals(EditorMode.Add, form.mode)
        assertNull(form.taskId)
        assertEquals("Draft quarterly report", form.name)
        assertEquals("Still missing the metrics section", form.description)
        assertEquals("work", form.label)
        assertEquals(setOf(7L, 9L), form.selectedTagIds)
        assertEquals(planItemId, form.upgradeDailyPlanItemId)
    }

    @Test
    fun savingUpgradedTaskLinksNewTaskBackToPlanItem() = runTestWithDispatcher {
        val planItemId = seedPlanItem()
        viewModel.openNewTaskFromDailyPlan(
            planItemId = planItemId,
            title = "Draft quarterly report",
            note = "",
            label = null,
            tagIds = emptySet()
        )

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val linked = repository.linkedDailyPlanItemTaskIds.single()
        assertEquals(planItemId, linked.first)
        assertEquals(repository.lastAssignedTaskId, linked.second)
        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun savingRegularNewTaskDoesNotLinkAnyPlanItem() = runTestWithDispatcher {
        viewModel.openNewTask()
        viewModel.updateTaskName("Plain task")

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList(), repository.linkedDailyPlanItemTaskIds.toList())
    }

    private fun runTestWithDispatcher(block: suspend () -> Unit) =
        kotlinx.coroutines.test.runTest(dispatcher) { block() }
}
