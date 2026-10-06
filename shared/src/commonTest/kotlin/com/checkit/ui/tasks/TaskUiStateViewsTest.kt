package com.checkit.ui.tasks

import com.checkit.domain.DueDatePreset
import com.checkit.domain.ListItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TaskUiStateViewsTest {

    private fun todayFilter(id: String = "1") = TaskFilter(
        id = id,
        name = "Today",
        icon = "Today",
        color = "#2563EB",
        dueDatePreset = DueDatePreset.Today,
        sortOrder = 0
    )

    private fun allFilter(id: String = "") = TaskFilter(
        id = id,
        name = "All",
        icon = "AllInclusive",
        color = "#475569",
        sortOrder = -1
    )

    private fun highPriorityFilter(id: String = "2") = TaskFilter(
        id = id,
        name = "High priority",
        icon = "PriorityHigh",
        color = "#DC2626",
        priority = TaskPriority.High,
        sortOrder = 2
    )

    @Test
    fun availableViewsExcludesTimelineWhenNoFilterSelected() {
        val state = TaskUiState()

        assertNull(state.dayLimit)
        assertEquals(
            listOf(TaskWorkspaceView.List, TaskWorkspaceView.Agenda, TaskWorkspaceView.Habits),
            state.availableViews
        )
    }

    @Test
    fun availableViewsExcludesTimelineForNonTodayFilter() {
        val board = TaskBoard(filters = listOf(highPriorityFilter()))
        val state = TaskUiState(board = board, options = TaskViewOptionsState(selectedFilterId = "2"))

        assertNull(state.dayLimit)
        assertFalse(TaskWorkspaceView.Timeline in state.availableViews)
    }

    @Test
    fun availableViewsIncludesTimelineForTodayFilter() {
        val board = TaskBoard(filters = listOf(todayFilter(), highPriorityFilter()))
        val state = TaskUiState(board = board, options = TaskViewOptionsState(selectedFilterId = "1"))

        assertEquals(1, state.dayLimit)
        assertEquals(
            listOf(TaskWorkspaceView.List, TaskWorkspaceView.Agenda, TaskWorkspaceView.Timeline, TaskWorkspaceView.Habits),
            state.availableViews
        )
    }

    @Test
    fun availableViewsExcludesTimelineForAllFilter() {
        val board = TaskBoard(filters = listOf(allFilter(), todayFilter(), highPriorityFilter()))
        val state = TaskUiState(board = board, options = TaskViewOptionsState(selectedFilterId = ""))

        assertNull(state.dayLimit)
        assertEquals(
            listOf(TaskWorkspaceView.List, TaskWorkspaceView.Agenda, TaskWorkspaceView.Habits),
            state.availableViews
        )
    }

    @Test
    fun availableViewsExcludesTimelineForListOrTagSelection() {
        val board = TaskBoard(filters = listOf(todayFilter(), highPriorityFilter()))
        val state = TaskUiState(board = board, selection = TaskSelectionState(selectedListId = "99"))

        assertNull(state.dayLimit)
        assertFalse(TaskWorkspaceView.Timeline in state.availableViews)
    }

    @Test
    fun openTaskCountsCalculatedCorrectly() {
        val list1 = ListItem(id = "list-1", title = "Work", icon = "Work", color = "#000", sortOrder = 0)
        val list2 = ListItem(id = "list-2", title = "Home", icon = "Home", color = "#000", sortOrder = 1)

        val task1 = TaskItem(id = "1", list = list1, name = "Task 1", status = TaskStatus.Open, createdAtMillis = 0, updatedAtMillis = 0)
        val task2 = TaskItem(id = "2", list = list1, name = "Task 2", status = TaskStatus.Completed, createdAtMillis = 0, updatedAtMillis = 0)
        val task3 = TaskItem(id = "3", list = list1, name = "Task 3", status = TaskStatus.Open, createdAtMillis = 0, updatedAtMillis = 0)
        val task4 = TaskItem(id = "4", list = list2, name = "Task 4", status = TaskStatus.Open, createdAtMillis = 0, updatedAtMillis = 0)
        val task5 = TaskItem(id = "5", list = list2, name = "Trashed Task", status = TaskStatus.Open, trashedAtMillis = 1000L, createdAtMillis = 0, updatedAtMillis = 0)

        val board = TaskBoard(
            lists = listOf(list1, list2),
            tasks = listOf(task1, task2, task3, task4, task5)
        )

        assertEquals(3, board.allOpenTasksCount)
        assertEquals(mapOf("list-1" to 2, "list-2" to 1), board.openTasksCountByListId)
    }
}
