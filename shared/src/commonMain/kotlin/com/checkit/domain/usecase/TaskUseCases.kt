package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.NoteWriteInput
import com.checkit.data.SettingsRepository
import com.checkit.data.TagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DueDatePreset
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

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

class AutoAddTodayTasksToMyDayUseCase(
    private val repository: CheckItRepository,
    private val settingsRepository: SettingsRepository
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): Int = mutex.withLock {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayEpochDay = today.toEpochDays().toInt()
        if (settingsRepository.settings.first().autoMyDayLastRunEpochDay == todayEpochDay) {
            return@withLock 0
        }

        repository.ensureDefaultTaskData()
        val tasksToAdd = repository.observeTaskBoard()
            .first()
            .tasks
            .filter { task ->
                !task.isTrashed &&
                    task.status == TaskStatus.Open &&
                    task.doDate == today
            }

        tasksToAdd.forEach { task ->
            repository.addTaskToDailyPlan(today, task)
        }
        settingsRepository.setAutoMyDayLastRunEpochDay(todayEpochDay)
        tasksToAdd.size
    }
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

class OpenTaskUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(taskId: Long) = repository.openTask(taskId)
}

class AddTaskToDailyPlanUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(date: LocalDate, task: TaskItem): Long =
        repository.addTaskToDailyPlan(date, task)
}

class AddManualDoneToDailyPlanUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        date: LocalDate,
        title: String,
        note: String?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        source: DailyPlanItemSource = DailyPlanItemSource.MyDayTask,
        status: DailyPlanItemStatus = DailyPlanItemStatus.Done,
        tagIds: List<Long> = emptyList()
    ): Long =
        repository.addManualDoneToDailyPlan(date, title, note, startTimeMinutes, endTimeMinutes, source, status, tagIds)
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

class OpenNoteUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(noteId: Long) = repository.openNote(noteId)
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

class SelectTaskBoardItemsUseCase {
    operator fun invoke(
        board: TaskBoard,
        selection: TaskBoardSelection,
        today: LocalDate
    ): TaskBoardItems {
        val listFilteredTasks = when (selection) {
            is TaskBoardSelection.ListSelection -> board.tasks.filter { it.objective.id == selection.listId && !it.isTrashed }
            is TaskBoardSelection.FilterSelection -> board.tasks.filter { it.matches(selection.filter, today) }
        }
        val listFilteredNotes = when (selection) {
            is TaskBoardSelection.ListSelection -> board.notes.filter { it.objective.id == selection.listId && !it.isTrashed }
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

private fun matchesNoteDate(preset: DueDatePreset, date: LocalDate, today: LocalDate): Boolean =
    when (preset) {
        DueDatePreset.Today -> date == today
        DueDatePreset.Upcoming -> date >= today && date <= today.plus(7, DateTimeUnit.DAY)
        DueDatePreset.Overdue -> date < today
        DueDatePreset.NoDate -> false
        DueDatePreset.Someday -> false
    }
