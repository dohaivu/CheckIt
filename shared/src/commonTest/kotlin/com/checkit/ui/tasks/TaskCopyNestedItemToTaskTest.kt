package com.checkit.ui.tasks

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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCopyNestedItemToTaskTest {
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

    @Test
    fun openNewTaskFromNestedItemPrefillsTitleNoteAndSubtasks() {
        viewModel.openNewTaskFromNestedItem(
            title = "Prepare workshop",
            note = "Book the room first",
            subtaskTexts = listOf("Slides", "Handout", "Coffee")
        )
        dispatcher.scheduler.advanceUntilIdle()

        val form = viewModel.uiState.value.editor as? TaskEditorState.TaskForm
            ?: error("Expected a TaskForm editor")
        assertEquals(EditorMode.Add, form.mode)
        assertNull(form.taskId)
        assertEquals("Prepare workshop", form.name)
        assertEquals("Book the room first", form.description)
        assertEquals(listOf("Slides", "Handout", "Coffee"), form.subtasks.map { it.name })
        assertEquals(listOf(false, false, false), form.subtasks.map { it.isCompleted })
    }

    @Test
    fun savingCopiedTaskPersistsSubtaskNamesAndCreatesNoLink() {
        viewModel.openNewTaskFromNestedItem(
            title = "Prepare workshop",
            note = null,
            subtaskTexts = listOf("Slides", "Handout")
        )

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val added = repository.addedTasks.single()
        assertEquals("Prepare workshop", added.name)
        assertEquals(listOf("Slides", "Handout"), added.subtasks.map { it.name })
        assertEquals(emptyList(), repository.linkedDailyPlanItemTaskIds.toList())
        assertNull(viewModel.uiState.value.editor)
    }
}
