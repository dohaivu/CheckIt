package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.NoteWriteInput
import com.checkit.data.TagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DueDatePreset
import com.checkit.domain.NoteItem
import com.checkit.domain.TagItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ObserveTaskBoardUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(onlyOpen: Boolean = true): Flow<TaskBoard> = repository.observeTaskBoard(onlyOpen)
}

class GetTaskUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(taskId: Long): TaskItem? = repository.getTask(taskId)
}

class GetNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(noteId: Long): NoteItem? = repository.getNote(noteId)
}

class ObserveTasksForDateUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<TaskItem>> = repository.observeTasksForDate(date)
}

class ObserveNotesForDateUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<NoteItem>> = repository.observeNotesForDate(date)
}

class ObserveTasksInRangeUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<TaskItem>> =
        repository.observeTasksInRange(startDate, endDateInclusive)
}

class ObserveNotesInRangeUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<NoteItem>> =
        repository.observeNotesInRange(startDate, endDateInclusive)
}

// open or done today
class ObserveWorkingTasksUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<TaskItem>> = repository.observeWorkingTasks(date)
}

class ObserveTagsUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<List<TagItem>> = repository.observeTags()
}

class GetDailyPlanItemUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long): DailyPlanItem? = repository.getDailyPlanItem(itemId)
}

/**
 * Adds open tasks due today to My Day. Idempotent: tasks already on the plan
 * are skipped, so repeated invocations within a day are safe.
 */
class AutoAddTodayTasksToMyDayUseCase(
    private val repository: CheckItRepository,
    private val deleteDailyPlanItem: DeleteDailyPlanItemUseCase,
    private val smartScheduleDailyPlan: SmartScheduleDailyPlanUseCase
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): Int = mutex.withLock {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        removeIncompleteHabitsFromYesterday(today)
        val alreadyPlannedTaskIds = repository.dailyPlanForDate(today)
            ?.items
            ?.mapNotNull { it.taskId }
            ?.toSet()
            .orEmpty()
        val tasksToAdd = repository.observeTaskBoard()
            .first()
            .tasks
            .filter { task ->
                !task.isTrashed &&
                    task.status == TaskStatus.Open &&
                    task.qualifiesForAddToMyDay(today) &&
                    task.id !in alreadyPlannedTaskIds
            }

        tasksToAdd.forEach { task ->
            repository.addTaskToDailyPlan(today, task)
        }
        if (tasksToAdd.isNotEmpty()) {
            smartScheduleDailyPlan().getOrThrow()
        }
        tasksToAdd.size
    }

    private suspend fun removeIncompleteHabitsFromYesterday(today: LocalDate) {
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        repository.dailyPlanForDate(yesterday)
            ?.items
            ?.filter { it.isHabit && it.status != DailyPlanItemStatus.Done }
            ?.forEach { deleteDailyPlanItem(it.id) }
    }
}

private fun TaskItem.qualifiesForAddToMyDay(today: LocalDate): Boolean =
    when (type) {
        TaskType.Task -> doDate == today
        TaskType.Habit -> completedDate == null
    }

class ObserveTagUsageCountsUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<Map<Long, Int>> = repository.observeTagUsageCounts()
}

class AddTagUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(input: TagWriteInput): Long = repository.addTag(input)
}

class UpdateTagUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(tagId: Long, input: TagWriteInput) =
        repository.updateTag(tagId, input)
}

class UpdateTagSortOrderUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(tagId: Long, sortOrder: Int) =
        repository.updateTagSortOrder(tagId, sortOrder)
}

class DeleteTagUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(tagId: Long) = repository.deleteTag(tagId)
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

class RestoreTaskUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(taskId: Long) = repository.restoreTask(taskId)
}

class CompleteTaskUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(taskId: Long) = repository.completeTask(taskId)
}

class UpdateTaskStatusUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(taskId: Long, status: TaskStatus) = repository.updateTaskStatus(taskId, status)
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

class CompleteNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(noteId: Long) = repository.completeNote(noteId)
}

class UpdateNoteStatusUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(noteId: Long, status: TaskStatus) = repository.updateNoteStatus(noteId, status)
}

class DeleteNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(noteId: Long) = repository.trashNote(noteId)
}

class RestoreNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(noteId: Long) = repository.restoreNote(noteId)
}

class MoveTaskUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(taskId: Long, listId: Long, sectionId: Long?, sortOrder: Int, isPinned: Boolean) =
        repository.moveTask(taskId, listId, sectionId, sortOrder, isPinned)
}

class MoveNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(noteId: Long, listId: Long, sectionId: Long?, sortOrder: Int, isPinned: Boolean) =
        repository.moveNote(noteId, listId, sectionId, sortOrder, isPinned)
}


class SelectTaskBoardItemsUseCase {
    operator fun invoke(
        board: TaskBoard,
        selection: TaskBoardSelection,
        today: LocalDate
    ): TaskBoardItems {
        val listFilteredTasks = when (selection) {
            is TaskBoardSelection.ListSelection -> board.tasks.filter { it.list?.id == selection.listId && !it.isTrashed }
            is TaskBoardSelection.FilterSelection -> board.tasks.filter { it.matches(selection.filter, today) }
        }
        val listFilteredNotes = when (selection) {
            is TaskBoardSelection.ListSelection -> board.notes.filter { it.list?.id == selection.listId && !it.isTrashed }
            is TaskBoardSelection.FilterSelection -> board.notes.filter { it.matches(selection.filter, today) }
        }

        return TaskBoardItems(
            tasks = listFilteredTasks.sortedWith(compareBy<TaskItem> { it.sortOrder }.thenBy { it.doDate }),
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
        DueDatePreset.Today -> doDate == today
        DueDatePreset.Upcoming -> doDate != null && doDate >= today && doDate <= today.plus(7, DateTimeUnit.DAY)
        DueDatePreset.Overdue -> doDate != null && doDate < today && status != TaskStatus.Completed
        DueDatePreset.NoDate -> doDate == null
        DueDatePreset.Someday -> doDate == null && priority == TaskPriority.None
    }

private fun NoteItem.matches(filter: TaskFilter, today: LocalDate): Boolean {
    if (filter.includeTrashed) return isTrashed
    if (isTrashed) return false
    if (filter.tagId != null && tags.none { it.id == filter.tagId }) return false
    if (filter.status != null && status != filter.status) return false
    if (filter.priority != null) return false
    if (filter.dueDatePreset != null && !matchesNoteDate(filter.dueDatePreset, date, today)) return false
    return true
}

private fun matchesNoteDate(preset: DueDatePreset, date: LocalDate?, today: LocalDate): Boolean =
    when (preset) {
        DueDatePreset.Today -> date == today
        DueDatePreset.Upcoming -> date != null && date >= today && date <= today.plus(7, DateTimeUnit.DAY)
        DueDatePreset.Overdue -> date != null && date < today
        DueDatePreset.NoDate -> date == null
        DueDatePreset.Someday -> date == null
    }
