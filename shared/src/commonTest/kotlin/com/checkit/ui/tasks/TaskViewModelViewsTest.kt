package com.checkit.ui.tasks

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DueDatePreset
import com.checkit.domain.ListItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TagItem
import com.checkit.domain.TaskType
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.SyncKeyResultFromDailyPlanUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.CompleteNoteUseCase
import com.checkit.domain.usecase.OpenTaskUseCase
import com.checkit.domain.usecase.OpenNoteUseCase
import com.checkit.domain.usecase.RestoreNoteUseCase
import com.checkit.domain.usecase.RestoreTaskUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemStatusUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTagUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
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
            lists = listOf(
                ListItem(id = 1L, title = "Inbox", color = "#2563EB", icon = "Inbox", sortOrder = 0)
            ),
            filters = listOf(
                todayFilter(1L),
                highPriorityFilter(2L),
                allFilter(0L)
            )
        )
        repository = FakeCheckItRepository(initialBoard = board)
        viewModel = TaskViewModel(
            observeTaskBoard = ObserveTaskBoardUseCase(repository),
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
            updateDailyPlanItemTag = UpdateDailyPlanItemTagUseCase(repository),
            syncKeyResultFromDailyPlan = SyncKeyResultFromDailyPlanUseCase(repository),
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
    fun selectingActiveFilterClearsScope() = runTest(dispatcher) {
        viewModel.selectFilter(2L)
        viewModel.selectFilter(2L)

        val state = viewModel.uiState.value
        assertNull(state.selectedFilterId)
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
    fun filterPersistsWhenSelectingList() = runTest(dispatcher) {
        viewModel.selectFilter(1L)
        viewModel.selectView(TaskWorkspaceView.Timeline)
        viewModel.selectList(99L)

        val state = viewModel.uiState.value
        assertEquals(1, state.dayLimit)
        assertEquals(TaskWorkspaceView.Timeline, state.selectedView)
        assertEquals(99L, state.selectedListId)
    }

    @Test
    fun filterPersistsWhenSelectingTag() = runTest(dispatcher) {
        viewModel.selectFilter(1L)
        viewModel.selectView(TaskWorkspaceView.Timeline)
        viewModel.selectTag(7L)

        val state = viewModel.uiState.value
        assertEquals(1, state.dayLimit)
        assertEquals(TaskWorkspaceView.Timeline, state.selectedView)
        assertEquals(7L, state.selectedTagId)
    }

    @Test
    fun titleSortBuildsUnifiedTaskAndNoteListOrder() = runTest(dispatcher) {
        val inbox = ListItem(id = 1L, title = "Inbox", color = "#2563EB", icon = "Inbox", sortOrder = 0)
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
        val inbox = ListItem(id = 1L, title = "Inbox", color = "#2563EB", icon = "Inbox", sortOrder = 0)
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

    @Test
    fun habitsViewShowsOnlyHabitTasks() = runTest(dispatcher) {
        val inbox = ListItem(id = 1L, title = "Inbox", color = "#2563EB", icon = "Inbox", sortOrder = 0)
        viewModel = createViewModel(
            TaskBoard(
                lists = listOf(inbox),
                tasks = listOf(
                    task(id = 1L, list = inbox, name = "Read"),
                    task(id = 2L, list = inbox, name = "Meditate", type = TaskType.Habit),
                    task(id = 3L, list = inbox, name = "Write")
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectView(TaskWorkspaceView.Habits)

        val state = viewModel.uiState.value
        assertEquals(listOf("Meditate"), state.visibleTasks.map { it.name })
        assertTrue(state.visibleNotes.isEmpty())
    }

    @Test
    fun togglingTaskTagAlsoUpdatesDailyPlanItemTags() = runTest(dispatcher) {
        val inbox = ListItem(id = 1L, title = "Inbox", color = "#2563EB", icon = "Inbox", sortOrder = 0)
        val workTag = TagItem(id = 1L, name = "Work", color = "#DC2626", sortOrder = 0)
        val homeTag = TagItem(id = 2L, name = "Home", color = "#0891B2", sortOrder = 1)
        val item = task(id = 5L, list = inbox, name = "Gym", tags = listOf(workTag))
        viewModel = createViewModel(
            TaskBoard(
                lists = listOf(inbox),
                tags = listOf(workTag, homeTag),
                tasks = listOf(item)
            )
        )
        val today = LocalDate(2026, 6, 14)
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = today,
                    items = listOf(
                        DailyPlanItem(
                            id = 10L,
                            dateEpochDays = today.toEpochDays().toInt(),
                            taskId = item.id,
                            title = "Gym",
                            source = DailyPlanItemSource.ExistingTask,
                            status = DailyPlanItemStatus.Planned,
                            tags = listOf(workTag),
                            sortOrder = 0,
                            addedAtMillis = 0L
                        )
                    )
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openTask(item, repository.dailyPlanForDate(today)?.items?.first())
        viewModel.toggleTaskTag(homeTag.id)
        dispatcher.scheduler.advanceUntilIdle()

        val updatedItem = repository.dailyPlanForDate(today)?.items?.first()
        assertEquals(setOf(workTag.id, homeTag.id), updatedItem?.tags?.map { it.id }?.toSet())
    }


    @Test
    fun openTaskPreservesTwelveWeekGoalId() = runTest(dispatcher) {
        val goalId = 7L
        val tacticTask = TaskItem(
            id = 42L,
            name = "Tactic task",
            type = TaskType.Tactic,
            twelveWeekGoalId = goalId,
            sortOrder = 0,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )
        viewModel = createViewModel(TaskBoard(tasks = listOf(tacticTask)))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openTask(tacticTask)
        dispatcher.scheduler.advanceUntilIdle()

        val editor = viewModel.uiState.value.editor as TaskEditorState.TaskForm
        assertEquals(goalId, editor.twelveWeekGoalId)
    }


    private fun createViewModel(board: TaskBoard): TaskViewModel {
        repository = FakeCheckItRepository(initialBoard = board)
        return TaskViewModel(
            observeTaskBoard = ObserveTaskBoardUseCase(repository),
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
            updateDailyPlanItemTag = UpdateDailyPlanItemTagUseCase(repository),
            syncKeyResultFromDailyPlan = SyncKeyResultFromDailyPlanUseCase(repository),
            settingsRepository = FakeSettingsRepository()
        )
    }

    private fun task(
        id: Long,
        list: ListItem,
        name: String,
        description: String = "",
        type: TaskType = TaskType.Task,
        tags: List<TagItem> = emptyList()
    ) = TaskItem(
        id = id,
        list = list,
        name = name,
        description = description,
        type = type,
        tags = tags,
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun note(
        id: Long,
        list: ListItem,
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

    private fun todayFilter(id: Long = 1L) = TaskFilter(
        id = id,
        name = "Today",
        icon = "Today",
        color = "#2563EB",
        dueDatePreset = DueDatePreset.Today,
        sortOrder = 0
    )

    private fun allFilter(id: Long = 0L) = TaskFilter(
        id = id,
        name = "All",
        icon = "AllInclusive",
        color = "#475569",
        sortOrder = -1
    )

    private fun highPriorityFilter(id: Long = 2L) = TaskFilter(
        id = id,
        name = "High priority",
        icon = "PriorityHigh",
        color = "#DC2626",
        priority = TaskPriority.High,
        sortOrder = 2
    )
}
