package com.checkit.ui.tasks

import com.checkit.domain.DueDatePreset
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskPriority
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
import com.checkit.ui.TaskWorkspaceView
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
class TaskViewModelViewsTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: TaskViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val board = TaskBoard(
            filters = listOf(
                TaskFilter(
                    id = 0L,
                    name = "All",
                    icon = "AllInclusive",
                    color = "#475569",
                    sortOrder = -1
                ),
                TaskFilter(
                    id = 1L,
                    name = "Today",
                    icon = "Today",
                    color = "#2563EB",
                    dueDatePreset = DueDatePreset.Today,
                    sortOrder = 0
                ),
                TaskFilter(
                    id = 2L,
                    name = "High priority",
                    icon = "PriorityHigh",
                    color = "#DC2626",
                    priority = TaskPriority.High,
                    sortOrder = 2
                )
            )
        )
        repository = FakeCheckItRepository(initialBoard = board)
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
    fun timelineViewIsAvailableForTodayFilter() = runTest(dispatcher) {
        viewModel.selectFilter(1L)
        viewModel.selectView(TaskWorkspaceView.Timeline)

        val state = viewModel.uiState.value
        assertEquals(TaskWorkspaceView.Timeline, state.selectedView)
        assertEquals(1, state.dayLimit)
        assertTrue(TaskWorkspaceView.Timeline in state.availableViews)
    }

    @Test
    fun selectingNonTodayFilterCoercesTimelineBackToList() = runTest(dispatcher) {
        viewModel.selectFilter(1L)
        viewModel.selectView(TaskWorkspaceView.Timeline)
        viewModel.selectFilter(2L)

        val state = viewModel.uiState.value
        assertNull(state.dayLimit)
        assertEquals(TaskWorkspaceView.List, state.selectedView)
        assertFalse(TaskWorkspaceView.Timeline in state.availableViews)
    }

    @Test
    fun selectViewTimelineIsIgnoredWhenFilterIsNotToday() = runTest(dispatcher) {
        viewModel.selectFilter(2L)
        viewModel.selectView(TaskWorkspaceView.Timeline)

        val state = viewModel.uiState.value
        assertEquals(TaskWorkspaceView.List, state.selectedView)
    }

    @Test
    fun allFilterExcludesTimelineView() = runTest(dispatcher) {
        viewModel.selectFilter(0L)
        viewModel.selectView(TaskWorkspaceView.Timeline)

        val state = viewModel.uiState.value
        assertNull(state.dayLimit)
        assertEquals(TaskWorkspaceView.List, state.selectedView)
        assertFalse(TaskWorkspaceView.Timeline in state.availableViews)
    }

    @Test
    fun selectingListAlsoCoercesTimelineAway() = runTest(dispatcher) {
        viewModel.selectFilter(1L)
        viewModel.selectView(TaskWorkspaceView.Timeline)
        viewModel.selectList(99L)

        val state = viewModel.uiState.value
        assertNull(state.dayLimit)
        assertEquals(TaskWorkspaceView.List, state.selectedView)
    }

    @Test
    fun selectingTagCoercesTimelineAway() = runTest(dispatcher) {
        viewModel.selectFilter(1L)
        viewModel.selectView(TaskWorkspaceView.Timeline)
        viewModel.selectTag(7L)

        val state = viewModel.uiState.value
        assertNull(state.dayLimit)
        assertEquals(TaskWorkspaceView.List, state.selectedView)
    }
}
