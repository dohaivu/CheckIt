package com.checkit.ui.tasks

import com.checkit.domain.DueDatePreset
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddTaskListUseCase
import com.checkit.domain.usecase.AddTaskTagUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.CompleteNoteUseCase
import com.checkit.domain.usecase.OpenTaskUseCase
import com.checkit.domain.usecase.OpenNoteUseCase
import com.checkit.domain.usecase.RestoreNoteUseCase
import com.checkit.domain.usecase.RestoreTaskUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTaskListUseCase
import com.checkit.domain.usecase.DeleteTaskTagUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemStatusUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateTaskListUseCase
import com.checkit.domain.usecase.UpdateTaskTagUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
import com.checkit.ui.TaskListEntry
import com.checkit.ui.TaskSortOption
import com.checkit.ui.TaskWorkspaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
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
            settingsRepository = FakeSettingsRepository()
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

    @Test
    fun titleSortBuildsUnifiedTaskAndNoteListOrder() = runTest(dispatcher) {
        val inbox = TaskList(id = 1L, name = "Inbox", color = "#2563EB", icon = "Inbox", sortOrder = 0)
        viewModel = createViewModel(
            TaskBoard(
                lists = listOf(inbox),
                tasks = listOf(
                    task(id = 1L, list = inbox, name = "Bravo"),
                    task(id = 2L, list = inbox, name = "Delta")
                ),
                notes = listOf(
                    note(id = 3L, list = inbox, title = "Alpha"),
                    note(id = 4L, list = inbox, title = "Charlie")
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSortOption(TaskSortOption.Title)

        val labels = viewModel.uiState.value.visibleListItems.map { entry ->
            when (entry) {
                is TaskListEntry.Task -> "task:${entry.item.name}"
                is TaskListEntry.Note -> "note:${entry.item.title}"
            }
        }
        assertEquals(listOf("note:Alpha", "task:Bravo", "note:Charlie", "task:Delta"), labels)
    }

    @Test
    fun searchFiltersTasksAndNotesByTitleAndBody() = runTest(dispatcher) {
        val inbox = TaskList(id = 1L, name = "Inbox", color = "#2563EB", icon = "Inbox", sortOrder = 0)
        viewModel = createViewModel(
            TaskBoard(
                lists = listOf(inbox),
                tasks = listOf(
                    task(id = 1L, list = inbox, name = "Budget", description = "Quarterly planning"),
                    task(id = 2L, list = inbox, name = "Groceries", description = "Milk")
                ),
                notes = listOf(
                    note(id = 3L, list = inbox, title = "Ideas", content = "Quarterly roadmap"),
                    note(id = 4L, list = inbox, title = "Receipt", content = "Coffee")
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSearchText("quarter")

        val labels = viewModel.uiState.value.visibleListItems.map { entry ->
            when (entry) {
                is TaskListEntry.Task -> "task:${entry.item.name}"
                is TaskListEntry.Note -> "note:${entry.item.title}"
            }
        }
        assertEquals(listOf("task:Budget", "note:Ideas"), labels)
    }

    private fun createViewModel(board: TaskBoard): TaskViewModel {
        repository = FakeCheckItRepository(initialBoard = board)
        return TaskViewModel(
            observeTaskBoard = ObserveTaskBoardUseCase(repository),
            ensureDefaultTaskData = EnsureDefaultTaskDataUseCase(repository),
            selectTaskBoardItems = SelectTaskBoardItemsUseCase(),
            addTask = AddTaskUseCase(repository),
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
            settingsRepository = FakeSettingsRepository()
        )
    }

    private fun task(
        id: Long,
        list: TaskList,
        name: String,
        description: String = ""
    ) = TaskItem(
        id = id,
        list = list,
        name = name,
        description = description,
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun note(
        id: Long,
        list: TaskList,
        title: String,
        content: String = ""
    ) = NoteItem(
        id = id,
        list = list,
        title = title,
        content = content,
        date = LocalDate(2026, 6, 14),
        createdAtMillis = 0L,
        editedAtMillis = 0L,
        sortOrder = id.toInt()
    )
}
