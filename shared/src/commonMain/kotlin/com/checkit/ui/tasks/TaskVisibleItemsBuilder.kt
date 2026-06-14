package com.checkit.ui.tasks

import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.TaskBoardSelection
import com.checkit.ui.TaskListEntry
import com.checkit.ui.TaskSelectionState
import com.checkit.ui.TaskSortOption
import com.checkit.ui.TaskViewOptionsState
import com.checkit.ui.TaskVisibleItemsState
import kotlinx.datetime.LocalDate

internal class TaskVisibleItemsBuilder(
    private val selectTaskBoardItems: SelectTaskBoardItemsUseCase
) {
    fun build(
        board: TaskBoard,
        selection: TaskSelectionState,
        options: TaskViewOptionsState,
        today: LocalDate
    ): TaskVisibleItemsState {
        val selectedFilter = board.filters.firstOrNull { it.id == selection.selectedFilterId }
        val selectedItems = when {
            selection.selectedTagId != null -> {
                val tagId = selection.selectedTagId
                SelectedTaskItems(
                    tasks = board.tasks.filter { task -> !task.isTrashed && task.tags.any { it.id == tagId } },
                    notes = board.notes.filter { note -> !note.isTrashed && note.tags.any { it.id == tagId } }
                )
            }
            selectedFilter != null -> {
                selectTaskBoardItems(board, TaskBoardSelection.FilterSelection(selectedFilter), today)
                    .let { SelectedTaskItems(tasks = it.tasks, notes = it.notes) }
            }
            selection.selectedListId != null -> {
                selectTaskBoardItems(board, TaskBoardSelection.ListSelection(selection.selectedListId), today)
                    .let { SelectedTaskItems(tasks = it.tasks, notes = it.notes) }
            }
            else -> SelectedTaskItems(
                tasks = board.tasks.filter { task -> !task.isTrashed },
                notes = board.notes.filter { note -> !note.isTrashed }
            )
        }

        val shouldHideCompleted = !options.showCompleted && selectedFilter?.status != TaskStatus.Completed
        val completionFilteredTasks = if (shouldHideCompleted) {
            selectedItems.tasks.filter { it.status != TaskStatus.Completed }
        } else {
            selectedItems.tasks
        }
        val completionFilteredNotes = if (shouldHideCompleted) {
            selectedItems.notes.filter { it.status != TaskStatus.Completed }
        } else {
            selectedItems.notes
        }

        val query = options.searchText.trim()
        val searchFilteredTasks = if (query.isEmpty()) {
            completionFilteredTasks
        } else {
            completionFilteredTasks.filter { it.matchesSearch(query) }
        }
        val searchFilteredNotes = if (query.isEmpty()) {
            completionFilteredNotes
        } else {
            completionFilteredNotes.filter { it.matchesSearch(query) }
        }

        val sortedVisibleItems = (searchFilteredTasks.map { TaskListEntry.Task(it) } +
            searchFilteredNotes.map { TaskListEntry.Note(it) })
            .sortedFor(options.sortOption)

        return TaskVisibleItemsState(
            tasks = sortedVisibleItems.mapNotNull { (it as? TaskListEntry.Task)?.item },
            notes = sortedVisibleItems.mapNotNull { (it as? TaskListEntry.Note)?.item },
            listItems = sortedVisibleItems
        )
    }
}

private data class SelectedTaskItems(
    val tasks: List<TaskItem>,
    val notes: List<NoteItem>
)

private fun TaskItem.matchesSearch(query: String): Boolean =
    name.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true)

private fun NoteItem.matchesSearch(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        content.contains(query, ignoreCase = true)

private fun List<TaskListEntry>.sortedFor(sortOption: TaskSortOption): List<TaskListEntry> =
    when (sortOption) {
        TaskSortOption.Custom -> sortedWith(
            compareBy<TaskListEntry> { it.sortOrder }
                .thenBy { it.typeRank }
                .thenBy { it.id }
        )
        TaskSortOption.Priority -> sortedWith(
            compareBy<TaskListEntry> { it.priorityRank }
                .thenBy { it.dateForSort ?: LocalDate.fromEpochDays(Int.MAX_VALUE) }
                .thenBy { it.startTimeForSort ?: Int.MAX_VALUE }
                .thenBy { it.sortOrder }
                .thenBy { it.typeRank }
                .thenBy { it.id }
        )
        TaskSortOption.Title -> sortedWith(
            compareBy<TaskListEntry> { it.titleForSort }
                .thenBy { it.sortOrder }
                .thenBy { it.typeRank }
                .thenBy { it.id }
        )
        TaskSortOption.Date -> sortedWith(
            compareBy<TaskListEntry> { it.dateForSort ?: LocalDate.fromEpochDays(Int.MAX_VALUE) }
                .thenBy { it.startTimeForSort ?: Int.MAX_VALUE }
                .thenBy { it.sortOrder }
                .thenBy { it.typeRank }
                .thenBy { it.id }
        )
    }

private val TaskListEntry.id: Long
    get() = when (this) {
        is TaskListEntry.Task -> item.id
        is TaskListEntry.Note -> item.id
    }

private val TaskListEntry.sortOrder: Int
    get() = when (this) {
        is TaskListEntry.Task -> item.sortOrder
        is TaskListEntry.Note -> item.sortOrder
    }

private val TaskListEntry.typeRank: Int
    get() = when (this) {
        is TaskListEntry.Task -> 0
        is TaskListEntry.Note -> 1
    }

private val TaskListEntry.priorityRank: Int
    get() = when (this) {
        is TaskListEntry.Task -> item.priority.rankForSort()
        is TaskListEntry.Note -> TaskPriority.None.rankForSort()
    }

private val TaskListEntry.dateForSort: LocalDate?
    get() = when (this) {
        is TaskListEntry.Task -> item.doDate
        is TaskListEntry.Note -> item.date
    }

private val TaskListEntry.startTimeForSort: Int?
    get() = when (this) {
        is TaskListEntry.Task -> item.startTimeMinutes
        is TaskListEntry.Note -> item.startTimeMinutes
    }

private val TaskListEntry.titleForSort: String
    get() = when (this) {
        is TaskListEntry.Task -> item.name
        is TaskListEntry.Note -> item.title.ifBlank { item.content }
    }.lowercase()

private fun TaskPriority.rankForSort(): Int =
    when (this) {
        TaskPriority.High -> 0
        TaskPriority.Medium -> 1
        TaskPriority.Low -> 2
        TaskPriority.None -> 3
    }
