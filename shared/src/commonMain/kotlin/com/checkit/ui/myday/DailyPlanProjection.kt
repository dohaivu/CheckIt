package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.parseHexColorOrNull
import com.checkit.ui.tasks.toClockLabel
import com.checkit.ui.today
import kotlinx.datetime.LocalDate

val MyDayListId = -10_000L

data class MyDayTaskViewProjection(
    val tasks: List<TaskItem>,
    val notes: List<NoteItem>,
    val lists: List<TaskList>,
    val dailyItemBySyntheticTaskId: Map<Long, DailyPlanItem>,
    val dailyItemBySyntheticNoteId: Map<Long, DailyPlanItem>
) {
    fun dailyItemFor(task: TaskItem): DailyPlanItem? = dailyItemBySyntheticTaskId[task.id]
    fun dailyItemFor(note: NoteItem): DailyPlanItem? = dailyItemBySyntheticNoteId[note.id]
}

fun List<DailyPlanItem>.toTaskViewProjection(
    board: TaskBoard,
    date: LocalDate
): MyDayTaskViewProjection {
    val fallbackList = TaskList(
        id = MyDayListId,
        name = "My Day",
        color = "#64748B",
        icon = "Today",
        sortOrder = 0
    )
    val lists = board.lists + fallbackList
    val listId = fallbackList.id
    val realTasksById = board.tasks.associateBy { it.id }
    val dailyItemBySyntheticTaskId = mutableMapOf<Long, DailyPlanItem>()
    val dailyItemBySyntheticNoteId = mutableMapOf<Long, DailyPlanItem>()
    val projectedTasks = mutableListOf<TaskItem>()
    val projectedNotes = mutableListOf<NoteItem>()
    // For My Day projection, we include global notes for today too
    projectedNotes += board.notes.filter { !it.isTrashed && it.date == today() }

    forEach { item ->
        when (item.source) {
            DailyPlanItemSource.CheckInNote -> {
                projectedNotes += NoteItem(
                    id = item.id,
                    listId = listId,
                    content = item.note.orEmpty(),
                    status = item.status.toTaskStatus(),
                    date = date,
                    startTimeMinutes = item.startTimeMinutes,
                    createdAtMillis = item.addedAtMillis,
                    editedAtMillis = item.completedAtMillis ?: item.addedAtMillis,
                    sortOrder = item.sortOrder
                )
                dailyItemBySyntheticNoteId[item.id] = item
            }
            else -> {
                val realTask = item.taskId?.let { realTasksById[it] }
                val projectedTask = realTask?.copy(
                    id = item.id,
                    doDate = date,
                    startTimeMinutes = item.startTimeMinutes,
                    endTimeMinutes = item.endTimeMinutes,
                    status = item.status.toTaskStatus(),
                    sortOrder = item.sortOrder
                ) ?: TaskItem(
                    id = item.id,
                    listId = listId,
                    name = item.titleSnapshot,
                    description = item.note.orEmpty(),
                    status = item.status.toTaskStatus(),
                    priority = TaskPriority.None,
                    doDate = date,
                    completedDate = date.takeIf { item.status == DailyPlanItemStatus.Done },
                    startTimeMinutes = item.startTimeMinutes,
                    endTimeMinutes = item.endTimeMinutes,
                    durationMinutes = item.durationMinutes(),
                    sortOrder = item.sortOrder,
                    createdAtMillis = item.addedAtMillis,
                    updatedAtMillis = item.completedAtMillis ?: item.addedAtMillis
                )
                projectedTasks += projectedTask
                dailyItemBySyntheticTaskId[projectedTask.id] = item
            }
        }
    }

    return MyDayTaskViewProjection(
        tasks = projectedTasks,
        notes = projectedNotes,
        lists = lists,
        dailyItemBySyntheticTaskId = dailyItemBySyntheticTaskId,
        dailyItemBySyntheticNoteId = dailyItemBySyntheticNoteId
    )
}

fun DailyPlanItemStatus.toTaskStatus(): TaskStatus = when (this) {
    DailyPlanItemStatus.Planned -> TaskStatus.Open
    DailyPlanItemStatus.Done -> TaskStatus.Completed
}

fun DailyPlanItem.durationMinutes(): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
    return (end - start).takeIf { it >= 0 }
}

fun DailyPlanItem.timeLabel(): String? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes
    return if (end == null) start.toClockLabel() else "${start.toClockLabel()} - ${end.toClockLabel()}"
}

fun DailyPlanItem.displayTitle(): String =
    when (source) {
        DailyPlanItemSource.CheckInNote -> note.orEmpty().ifBlank { "Empty note" }
        else -> titleSnapshot.ifBlank { "Untitled item" }
    }

fun DailyPlanItem.displaySupportingText(): String =
    when {
        source == DailyPlanItemSource.CheckInNote -> source.label()
        !note.isNullOrBlank() -> note.orEmpty()
        else -> source.label()
    }

fun DailyPlanItemSource.label(): String = when (this) {
    DailyPlanItemSource.ExistingTask -> "Task"
    DailyPlanItemSource.CheckInManualDone -> "CheckIn done"
    DailyPlanItemSource.CheckInNote -> "CheckIn note"
}