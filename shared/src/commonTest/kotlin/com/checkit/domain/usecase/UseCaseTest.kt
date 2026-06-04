package com.checkit.domain.usecase

import com.checkit.domain.DueDatePreset
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskTag
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class UseCaseTest {
    private val selectItems = SelectTaskBoardItemsUseCase()
    private val today = LocalDate(2026, 6, 4)

    @Test
    fun todayFilterReturnsTasksDueTodayOnly() {
        val board = TaskBoard(
            tasks = listOf(
                task(id = 1, dueDate = today),
                task(id = 2, dueDate = LocalDate(2026, 6, 5)),
                task(id = 3, dueDate = null)
            )
        )
        val filter = TaskFilter(
            id = 1,
            name = "Today",
            icon = "Today",
            color = "#2563EB",
            dueDatePreset = DueDatePreset.Today,
            sortOrder = 0
        )

        val items = selectItems(board, TaskBoardSelection.FilterSelection(filter), today)

        assertEquals(listOf(1L), items.tasks.map { it.id })
    }

    @Test
    fun tagAndPriorityFilterCanBeCombined() {
        val tag = TaskTag(id = 7, name = "Work", icon = "Work", color = "#7C3AED")
        val board = TaskBoard(
            tasks = listOf(
                task(id = 1, tags = listOf(tag), priority = TaskPriority.High),
                task(id = 2, tags = listOf(tag), priority = TaskPriority.Low),
                task(id = 3, tags = emptyList(), priority = TaskPriority.High)
            )
        )
        val filter = TaskFilter(
            id = 2,
            name = "High Work",
            icon = "PriorityHigh",
            color = "#DC2626",
            tagId = tag.id,
            priority = TaskPriority.High,
            sortOrder = 0
        )

        val items = selectItems(board, TaskBoardSelection.FilterSelection(filter), today)

        assertEquals(listOf(1L), items.tasks.map { it.id })
    }

    @Test
    fun tagFilterReturnsMatchingTasksAndNotes() {
        val tag = TaskTag(id = 7, name = "Work", icon = "Work", color = "#7C3AED")
        val board = TaskBoard(
            tasks = listOf(
                task(id = 1, tags = listOf(tag)),
                task(id = 2)
            ),
            notes = listOf(
                note(id = 3, tags = listOf(tag)),
                note(id = 4)
            )
        )
        val filter = TaskFilter(
            id = 4,
            name = "Work",
            icon = "Work",
            color = "#7C3AED",
            tagId = tag.id,
            sortOrder = 0
        )

        val items = selectItems(board, TaskBoardSelection.FilterSelection(filter), today)

        assertEquals(listOf(1L), items.tasks.map { it.id })
        assertEquals(listOf(3L), items.notes.map { it.id })
    }

    @Test
    fun trashedFilterOnlyReturnsTrashedTasks() {
        val board = TaskBoard(
            tasks = listOf(
                task(id = 1),
                task(id = 2, trashedAtMillis = 1000L)
            )
        )
        val filter = TaskFilter(
            id = 3,
            name = "Trashed",
            icon = "Delete",
            color = "#6B7280",
            includeTrashed = true,
            sortOrder = 0
        )

        val items = selectItems(board, TaskBoardSelection.FilterSelection(filter), today)

        assertEquals(listOf(2L), items.tasks.map { it.id })
    }

    @Test
    fun allFilterReturnsAllNonTrashedTasks() {
        val board = TaskBoard(
            tasks = listOf(
                task(id = 1, dueDate = today),
                task(id = 2, dueDate = LocalDate(2026, 6, 5), status = TaskStatus.Completed),
                task(id = 3, trashedAtMillis = 1000L),
                task(id = 4, priority = TaskPriority.High)
            )
        )
        val filter = TaskFilter(
            id = 0,
            name = "All",
            icon = "AllInclusive",
            color = "#475569",
            sortOrder = -1
        )

        val items = selectItems(board, TaskBoardSelection.FilterSelection(filter), today)

        assertEquals(listOf(1L, 2L, 4L), items.tasks.map { it.id })
    }

    private fun task(
        id: Long,
        dueDate: LocalDate? = null,
        tags: List<TaskTag> = emptyList(),
        priority: TaskPriority = TaskPriority.None,
        status: TaskStatus = TaskStatus.Open,
        trashedAtMillis: Long? = null
    ) = TaskItem(
        id = id,
        listId = 1,
        name = "Task $id",
        tags = tags,
        priority = priority,
        status = status,
        dueDate = dueDate,
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
        trashedAtMillis = trashedAtMillis
    )

    private fun note(
        id: Long,
        tags: List<TaskTag> = emptyList()
    ) = NoteItem(
        id = id,
        listId = 1,
        content = "Note $id",
        tags = tags,
        date = today,
        createdAtMillis = 0L,
        editedAtMillis = 0L,
        sortOrder = id.toInt()
    )
}
