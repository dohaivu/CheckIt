package com.checkit.ui.tasks

import com.checkit.domain.DueDatePreset
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskPriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TaskUiStateViewsTest {

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

    @Test
    fun availableViewsExcludesTimelineWhenNoFilterSelected() {
        val state = TaskUiState()

        assertNull(state.dayLimit)
        assertEquals(
            listOf(TaskWorkspaceView.List, TaskWorkspaceView.Agenda),
            state.availableViews
        )
    }

    @Test
    fun availableViewsExcludesTimelineForNonTodayFilter() {
        val board = TaskBoard(filters = listOf(highPriorityFilter()))
        val state = TaskUiState(board = board, options = TaskViewOptionsState(selectedFilterId = 2L))

        assertNull(state.dayLimit)
        assertFalse(TaskWorkspaceView.Timeline in state.availableViews)
    }

    @Test
    fun availableViewsIncludesTimelineForTodayFilter() {
        val board = TaskBoard(filters = listOf(todayFilter(), highPriorityFilter()))
        val state = TaskUiState(board = board, options = TaskViewOptionsState(selectedFilterId = 1L))

        assertEquals(1, state.dayLimit)
        assertEquals(
            listOf(TaskWorkspaceView.List, TaskWorkspaceView.Agenda, TaskWorkspaceView.Timeline),
            state.availableViews
        )
    }

    @Test
    fun availableViewsExcludesTimelineForAllFilter() {
        val board = TaskBoard(filters = listOf(allFilter(), todayFilter(), highPriorityFilter()))
        val state = TaskUiState(board = board, options = TaskViewOptionsState(selectedFilterId = 0L))

        assertNull(state.dayLimit)
        assertEquals(
            listOf(TaskWorkspaceView.List, TaskWorkspaceView.Agenda),
            state.availableViews
        )
    }

    @Test
    fun availableViewsExcludesTimelineForListOrTagSelection() {
        val board = TaskBoard(filters = listOf(todayFilter(), highPriorityFilter()))
        val state = TaskUiState(board = board, selection = TaskSelectionState(selectedListId = 99L))

        assertNull(state.dayLimit)
        assertFalse(TaskWorkspaceView.Timeline in state.availableViews)
    }
}
