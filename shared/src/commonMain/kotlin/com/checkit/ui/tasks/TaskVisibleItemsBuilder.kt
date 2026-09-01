package com.checkit.ui.tasks

import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskType
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.TaskBoardSelection
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
        val selectedFilter = board.filters.firstOrNull { it.id == options.selectedFilterId }
        val isTrashedFilter = selectedFilter?.includeTrashed == true
        val baseItems = when {
            selection.selectedTagId != null -> {
                val tagId = selection.selectedTagId
                if (isTrashedFilter) {
                    SelectedTaskItems(
                        tasks = board.tasks.filter { task -> task.isTrashed && task.tags.any { it.id == tagId } },
                        notes = board.notes.filter { note -> note.isTrashed && note.tags.any { it.id == tagId } }
                    )
                } else {
                    SelectedTaskItems(
                        tasks = board.tasks.filter { task -> !task.isTrashed && task.tags.any { it.id == tagId } },
                        notes = board.notes.filter { note -> !note.isTrashed && note.tags.any { it.id == tagId } }
                    )
                }
            }
            selection.selectedListId != null -> {
                if (isTrashedFilter) {
                    SelectedTaskItems(
                        tasks = board.tasks.filter { task -> task.isTrashed && task.list?.id == selection.selectedListId },
                        notes = board.notes.filter { note -> note.isTrashed && note.list?.id == selection.selectedListId }
                    )
                } else {
                    selectTaskBoardItems(board, TaskBoardSelection.ListSelection(selection.selectedListId), today)
                        .let { SelectedTaskItems(tasks = it.tasks, notes = it.notes) }
                }
            }
            else -> if (isTrashedFilter) {
                SelectedTaskItems(
                    tasks = board.tasks.filter { task -> task.isTrashed },
                    notes = board.notes.filter { note -> note.isTrashed }
                )
            } else {
                SelectedTaskItems(
                    tasks = board.tasks.filter { task -> !task.isTrashed },
                    notes = board.notes.filter { note -> !note.isTrashed }
                )
            }
        }
        val tagFilteredItems = if (options.selectedTagIds.isNotEmpty()) {
            SelectedTaskItems(
                tasks = baseItems.tasks.filter { task ->
                    task.tags.any { it.id in options.selectedTagIds }
                },
                notes = baseItems.notes.filter { note ->
                    note.tags.any { it.id in options.selectedTagIds }
                }
            )
        } else {
            baseItems
        }

        val selectedItems = if (selectedFilter != null) {
            val filterResult = selectTaskBoardItems(board, TaskBoardSelection.FilterSelection(selectedFilter), today)
            val filterTaskIds = filterResult.tasks.map { it.id }.toSet()
            val filterNoteIds = filterResult.notes.map { it.id }.toSet()
            SelectedTaskItems(
                tasks = tagFilteredItems.tasks.filter { it.id in filterTaskIds },
                notes = tagFilteredItems.notes.filter { it.id in filterNoteIds }
            )
        } else {
            tagFilteredItems
        }

        val viewItems = if (options.selectedView == TaskWorkspaceView.Habits) {
            SelectedTaskItems(
                tasks = selectedItems.tasks.filter { it.type == TaskType.Habit },
                notes = emptyList()
            )
        } else {
            selectedItems
        }

        val visibleEntries = mutableListOf<TaskListEntry>()
        val query = options.searchText.trim()
        val shouldHideCompleted = !options.showCompleted && selectedFilter?.status != TaskStatus.Completed
        viewItems.tasks.forEach { task ->
            if (task.isVisible(shouldHideCompleted, query)) {
                visibleEntries += TaskListEntry.Task(task)
            }
        }
        viewItems.notes.forEach { note ->
            if (note.isVisible(shouldHideCompleted, query)) {
                visibleEntries += TaskListEntry.Note(note)
            }
        }
        val sortedVisibleItems = visibleEntries.sortedFor(options.sortOption)

        val listItemsWithHeaders = if (selection.selectedListId != null && options.sortOption == TaskSortOption.Custom) {
            val list = board.lists.find { it.id == selection.selectedListId }
            val sections = list?.sections.orEmpty().sortedBy { it.sortOrder }
            
            val result = mutableListOf<TaskListEntry>()
            
            val pinned = sortedVisibleItems.filter { entry ->
                when (entry) {
                    is TaskListEntry.Task -> entry.item.isPinned
                    is TaskListEntry.Note -> entry.item.isPinned
                    else -> false
                }
            }
            
            // Always show Pinned header if we want to allow dragging into it, 
            // or at least if it's not empty.
            if (pinned.isNotEmpty()) {
                result += TaskListEntry.PinnedHeader
                result += pinned
            }

            val unpinned = sortedVisibleItems.filter { it !in pinned }

            // Items without section
            val unsectioned = unpinned.filter { entry ->
                when (entry) {
                    is TaskListEntry.Task -> entry.item.sectionId == null
                    is TaskListEntry.Note -> entry.item.sectionId == null
                    else -> false
                }
            }
            
            if (sections.isNotEmpty()) {
                // If there are sections, show unsectioned header if there are unsectioned items
                if (unsectioned.isNotEmpty()) {
                    result += TaskListEntry.SectionHeader(null)
                    result += unsectioned
                }
            } else {
                result += unsectioned
            }

            sections.forEach { section ->
                val sectionItems = unpinned.filter { entry ->
                    when (entry) {
                        is TaskListEntry.Task -> entry.item.sectionId == section.id
                        is TaskListEntry.Note -> entry.item.sectionId == section.id
                        else -> false
                    }
                }
                result += TaskListEntry.SectionHeader(section)
                result += sectionItems
            }
            result
        } else {
            sortedVisibleItems
        }

        val visibleTasks = mutableListOf<TaskItem>()
        val visibleNotes = mutableListOf<NoteItem>()
        listItemsWithHeaders.forEach { entry ->
            when (entry) {
                is TaskListEntry.Task -> visibleTasks += entry.item
                is TaskListEntry.Note -> visibleNotes += entry.item
                else -> {}
            }
        }
        return TaskVisibleItemsState(
            tasks = visibleTasks,
            notes = visibleNotes,
            listItems = listItemsWithHeaders
        )
    }
}

private data class SelectedTaskItems(
    val tasks: List<TaskItem>,
    val notes: List<NoteItem>
)

private fun TaskItem.isVisible(shouldHideCompleted: Boolean, query: String): Boolean {
    if (shouldHideCompleted && status == TaskStatus.Completed) return false
    return query.isEmpty() || matchesSearch(query)
}

private fun NoteItem.isVisible(shouldHideCompleted: Boolean, query: String): Boolean {
    if (shouldHideCompleted && status == TaskStatus.Completed) return false
    return query.isEmpty() || matchesSearch(query)
}

private fun TaskItem.matchesSearch(query: String): Boolean =
    name.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true)

private fun NoteItem.matchesSearch(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        content.contains(query, ignoreCase = true)

private fun List<TaskListEntry>.sortedFor(sortOption: TaskSortOption): List<TaskListEntry> =
    when (sortOption) {
        TaskSortOption.Custom -> sortedWith(TaskListEntryCustomComparator)
        TaskSortOption.Priority -> sortedWith(TaskListEntryPriorityComparator)
        TaskSortOption.Title -> sortedWith(TaskListEntryTitleComparator)
        TaskSortOption.Date -> sortedWith(TaskListEntryDateComparator)
    }

private val TaskListEntryCustomComparator: Comparator<TaskListEntry> =
    compareBy<TaskListEntry> { it.sortOrder }
        .thenBy { it.typeRank }
        .thenBy { it.id }

private val TaskListEntryPriorityComparator: Comparator<TaskListEntry> =
    compareBy<TaskListEntry> { it.priorityRank }
        .thenBy { it.dateForSort ?: LocalDate.fromEpochDays(Int.MAX_VALUE) }
        .thenBy { it.startTimeForSort ?: Int.MAX_VALUE }
        .thenBy { it.sortOrder }
        .thenBy { it.typeRank }
        .thenBy { it.id }

private val TaskListEntryTitleComparator: Comparator<TaskListEntry> =
    compareBy<TaskListEntry> { it.titleForSort }
        .thenBy { it.sortOrder }
        .thenBy { it.typeRank }
        .thenBy { it.id }

private val TaskListEntryDateComparator: Comparator<TaskListEntry> =
    compareBy<TaskListEntry> { it.dateForSort ?: LocalDate.fromEpochDays(Int.MAX_VALUE) }
        .thenBy { it.startTimeForSort ?: Int.MAX_VALUE }
        .thenBy { it.sortOrder }
        .thenBy { it.typeRank }
        .thenBy { it.id }

private val TaskListEntry.id: Long
    get() = when (this) {
        is TaskListEntry.Task -> item.id
        is TaskListEntry.Note -> item.id
        is TaskListEntry.SectionHeader -> section?.id ?: -1L
        is TaskListEntry.PinnedHeader -> -2L
    }

private val TaskListEntry.sortOrder: Int
    get() = when (this) {
        is TaskListEntry.Task -> item.sortOrder
        is TaskListEntry.Note -> item.sortOrder
        is TaskListEntry.SectionHeader -> section?.sortOrder ?: -1
        is TaskListEntry.PinnedHeader -> -2
    }

private val TaskListEntry.typeRank: Int
    get() = when (this) {
        is TaskListEntry.Task -> 0
        is TaskListEntry.Note -> 1
        is TaskListEntry.SectionHeader -> -1
        is TaskListEntry.PinnedHeader -> -2
    }

private val TaskListEntry.priorityRank: Int
    get() = when (this) {
        is TaskListEntry.Task -> item.priority.rankForSort()
        is TaskListEntry.Note -> TaskPriority.None.rankForSort()
        is TaskListEntry.SectionHeader -> -1
        is TaskListEntry.PinnedHeader -> -2
    }

private val TaskListEntry.dateForSort: LocalDate?
    get() = when (this) {
        is TaskListEntry.Task -> item.doDate
        is TaskListEntry.Note -> item.date
        is TaskListEntry.SectionHeader -> null
        is TaskListEntry.PinnedHeader -> null
    }

private val TaskListEntry.startTimeForSort: Int?
    get() = when (this) {
        is TaskListEntry.Task -> item.startTimeMinutes
        is TaskListEntry.Note -> item.startTimeMinutes
        is TaskListEntry.SectionHeader -> null
        is TaskListEntry.PinnedHeader -> null
    }

private val TaskListEntry.titleForSort: String
    get() = when (this) {
        is TaskListEntry.Task -> item.name
        is TaskListEntry.Note -> item.title.ifBlank { item.content }
        is TaskListEntry.SectionHeader -> section?.title.orEmpty()
        is TaskListEntry.PinnedHeader -> ""
    }.lowercase()

private fun TaskPriority.rankForSort(): Int =
    when (this) {
        TaskPriority.High -> 0
        TaskPriority.Medium -> 1
        TaskPriority.Low -> 2
        TaskPriority.None -> 3
    }
