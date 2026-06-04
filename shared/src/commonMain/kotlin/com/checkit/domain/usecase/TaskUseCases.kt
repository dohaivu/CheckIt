package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.NoteWriteInput
import com.checkit.data.TaskListWriteInput
import com.checkit.data.TaskTagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.DueDatePreset
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class ObserveTaskBoardUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<TaskBoard> = repository.observeTaskBoard()
}

class EnsureDefaultTaskDataUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke() = repository.ensureDefaultTaskData()
}

class AddTaskListUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(input: TaskListWriteInput): Long = repository.addList(input)
}

class UpdateTaskListUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(listId: Long, input: TaskListWriteInput) =
        repository.updateList(listId, input)
}

class AddTaskTagUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(input: TaskTagWriteInput): Long = repository.addTag(input)
}

class UpdateTaskTagUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(tagId: Long, input: TaskTagWriteInput) =
        repository.updateTag(tagId, input)
}

class IsTagNameTakenUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(name: String, excludeTagId: Long? = null): Boolean =
        repository.isTagNameTaken(name, excludeTagId)
}

class AddTaskUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(input: TaskWriteInput): Long = repository.addTask(input)
}

class UpdateTaskUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(taskId: Long, input: TaskWriteInput) = repository.updateTask(taskId, input)
}

class DeleteTaskUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(taskId: Long) = repository.trashTask(taskId)
}

class CompleteTaskUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(taskId: Long) = repository.completeTask(taskId)
}

class AddNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(input: NoteWriteInput): Long = repository.addNote(input)
}

class UpdateNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(noteId: Long, input: NoteWriteInput) = repository.updateNote(noteId, input)
}

class DeleteNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(noteId: Long) = repository.trashNote(noteId)
}

class SelectTaskBoardItemsUseCase {
    operator fun invoke(
        board: TaskBoard,
        selection: TaskBoardSelection,
        today: LocalDate
    ): TaskBoardItems {
        val listFilteredTasks = when (selection) {
            is TaskBoardSelection.ListSelection -> board.tasks.filter { it.listId == selection.listId && !it.isTrashed }
            is TaskBoardSelection.FilterSelection -> board.tasks.filter { it.matches(selection.filter, today) }
        }
        val listFilteredNotes = when (selection) {
            is TaskBoardSelection.ListSelection -> board.notes.filter { it.listId == selection.listId && !it.isTrashed }
            is TaskBoardSelection.FilterSelection -> board.notes.filter { it.matches(selection.filter, today) }
        }

        return TaskBoardItems(
            tasks = listFilteredTasks.sortedWith(compareBy<TaskItem> { it.sortOrder }.thenBy { it.dueDate }),
            notes = listFilteredNotes.sortedBy { it.sortOrder }
        )
    }
}

sealed interface TaskBoardSelection {
    data class ListSelection(val listId: Long) : TaskBoardSelection
    data class FilterSelection(val filter: TaskFilter) : TaskBoardSelection
}

data class TaskBoardItems(
    val tasks: List<TaskItem>,
    val notes: List<NoteItem>
)

private fun TaskItem.matches(filter: TaskFilter, today: LocalDate): Boolean {
    if (filter.includeTrashed) return isTrashed
    if (isTrashed) return false
    if (filter.tagId != null && tags.none { it.id == filter.tagId }) return false
    if (filter.status != null && status != filter.status) return false
    if (filter.priority != null && priority != filter.priority) return false
    if (filter.dueDatePreset != null && !matchesDueDate(filter.dueDatePreset, today)) return false
    return true
}

private fun TaskItem.matchesDueDate(preset: DueDatePreset, today: LocalDate): Boolean =
    when (preset) {
        DueDatePreset.Today -> dueDate == today
        DueDatePreset.Upcoming -> dueDate != null && dueDate >= today && dueDate <= today.plus(7, DateTimeUnit.DAY)
        DueDatePreset.Overdue -> dueDate != null && dueDate < today && status != TaskStatus.Completed
        DueDatePreset.Someday -> dueDate == null && priority == TaskPriority.None
    }

private fun NoteItem.matches(filter: TaskFilter, today: LocalDate): Boolean {
    if (filter.includeTrashed) return isTrashed
    if (isTrashed) return false
    if (filter.tagId != null && tags.none { it.id == filter.tagId }) return false
    if (filter.status != null || filter.priority != null) return false
    if (filter.dueDatePreset != null && !matchesNoteDate(filter.dueDatePreset, date, today)) return false
    return true
}

private fun matchesNoteDate(preset: DueDatePreset, date: LocalDate, today: LocalDate): Boolean =
    when (preset) {
        DueDatePreset.Today -> date == today
        DueDatePreset.Upcoming -> date >= today && date <= today.plus(7, DateTimeUnit.DAY)
        DueDatePreset.Overdue -> date < today
        DueDatePreset.Someday -> false
    }
