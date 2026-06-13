package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.data.NoteWriteInput
import com.checkit.data.TaskListWriteInput
import com.checkit.data.TaskTagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class ObserveTaskBoardUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<TaskBoard> = repository.observeTaskBoard()
}

class ObserveDailyPlansUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<List<DailyPlan>> = repository.observeDailyPlans()
}

class BuildDailyPlanMarkdownSummaryUseCase {
    operator fun invoke(
        date: LocalDate,
        plan: DailyPlan?,
        board: TaskBoard
    ): String {
        val tasksById = board.tasksById
        val doneItems = plan
            ?.items
            .orEmpty()
            .filter { it.status == DailyPlanItemStatus.Done }
            .sortedBy { it.startTimeMinutes ?: Int.MAX_VALUE }

        return buildString {
            appendLine("# ${date.toSummaryDateLabel()}")
            appendLine()
            if (doneItems.isEmpty()) {
                appendLine("No completed daily-plan items.")
                return@buildString
            }

            doneItems.forEachIndexed { index, item ->
                if (index > 0) appendLine()
                appendLine("- **${item.timeLabel()}**")
                item.titleLine()?.let { title ->
                    appendLine(title)
                }
                val task = item.taskId?.let { tasksById[it] }
                item.summaryDetail(task)?.let { detail ->
                    appendLine("_${detail}_")
                }
                task?.subtasks.orEmpty().forEach { subtask ->
                    appendLine("- [${if (subtask.isCompleted) "x" else " "}] ${subtask.name.cleanMarkdownLine()}")
                }
            }
        }.trimEnd()
    }
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

class DeleteTaskListUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(listId: Long) = repository.deleteList(listId)
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

class DeleteTaskTagUseCase(
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
        source: DailyPlanItemSource = DailyPlanItemSource.CheckInManualDone,
        tagIds: List<Long> = emptyList()
    ): Long =
        repository.addManualDoneToDailyPlan(date, title, note, startTimeMinutes, endTimeMinutes, source, tagIds)
}

class UpdateDailyPlanItemTimeUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, startTimeMinutes: Int?, endTimeMinutes: Int?) =
        repository.updateDailyPlanItemTime(itemId, startTimeMinutes, endTimeMinutes)
}

class UpdateDailyPlanItemStatusUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, status: DailyPlanItemStatus) =
        repository.updateDailyPlanItemStatus(itemId, status)
}

class UpdateDailyPlanItemUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, input: DailyPlanItemWriteInput) =
        repository.updateDailyPlanItem(itemId, input)
}

class DeleteDailyPlanItemUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long) = repository.deleteDailyPlanItem(itemId)
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
            is TaskBoardSelection.ListSelection -> board.tasks.filter { it.list.id == selection.listId && !it.isTrashed }
            is TaskBoardSelection.FilterSelection -> board.tasks.filter { it.matches(selection.filter, today) }
        }
        val listFilteredNotes = when (selection) {
            is TaskBoardSelection.ListSelection -> board.notes.filter { it.list.id == selection.listId && !it.isTrashed }
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

private fun DailyPlanItem.titleLine(): String? {
    val title = title.cleanMarkdownLine().takeIf { it.isNotBlank() } ?: return null
    val tagLabel = tags
        .mapNotNull { it.name.toMarkdownTag() }
        .joinToString(separator = " ")
        .takeIf { it.isNotBlank() }
    return listOfNotNull(title, tagLabel).joinToString(separator = " ")
}

private fun DailyPlanItem.summaryDetail(task: TaskItem?): String? {
    val taskDescription = if (source == DailyPlanItemSource.ExistingTask) {
        task?.description
    } else {
        null
    }
    return taskDescription
        ?.cleanMarkdownLine()
        ?.takeIf { it.isNotBlank() }
        ?: note?.cleanMarkdownLine()?.takeIf { it.isNotBlank() }
}

private fun DailyPlanItem.timeLabel(): String {
    val start = startTimeMinutes ?: return "All-Day"
    val end = endTimeMinutes
    return if (end == null) start.toClockLabel() else "${start.toClockLabel()} - ${end.toClockLabel()}"
}

private fun String.toMarkdownTag(): String? {
    val name = trim().removePrefix("#").replace(WhitespaceRegex, "")
    return name.takeIf { it.isNotBlank() }?.let { "#$it" }
}

private fun String.cleanMarkdownLine(): String =
    trim().replace(WhitespaceRegex, " ")

private val WhitespaceRegex = Regex("\\s+")

private fun Int.toClockLabel(): String {
    val hour = this / 60
    val minute = this % 60
    val suffix = if (hour >= 12) "PM" else "AM"
    val displayHour = when (val normalized = hour % 12) {
        0 -> 12
        else -> normalized
    }
    return "$displayHour:${minute.toString().padStart(2, '0')} $suffix"
}

private fun LocalDate.toSummaryDateLabel(): String =
    "${month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} $day, $year"

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
        DueDatePreset.Someday -> false
    }
