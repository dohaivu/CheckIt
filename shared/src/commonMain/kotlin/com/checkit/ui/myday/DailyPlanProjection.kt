package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskStatus
import com.checkit.ui.tasks.toClockLabel
import kotlinx.datetime.LocalDate

data class MyDayTaskViewProjection(
    val tasks: List<TaskItem>,
    val notes: List<NoteItem>,
    val checkIns: List<DailyPlanItem>,
    val lists: List<TaskList>
)

fun List<DailyPlanItem>.toTaskViewProjection(
    board: TaskBoard,
    date: LocalDate
): MyDayTaskViewProjection {
    val lists = board.lists
    val realTasksById = board.tasksById
    val projectedTasks = mutableListOf<TaskItem>()
    val projectedNotes = board.notes.filter { !it.isTrashed && it.date == date }
    val projectedCheckIns = mutableListOf<DailyPlanItem>()

    forEach { item ->
        val taskId = item.taskId
        if (taskId != null) {
            val realTask = realTasksById[taskId]
            if (realTask != null) {
                projectedTasks += realTask.copy(
                    status = item.status.toTaskStatus(),
                    startTimeMinutes = item.startTimeMinutes,
                    endTimeMinutes = item.endTimeMinutes,
                    sortOrder = item.sortOrder,
                    doDate = date
                )
            }
        } else {
            projectedCheckIns += item
        }
    }

    return MyDayTaskViewProjection(
        tasks = projectedTasks,
        notes = projectedNotes,
        checkIns = projectedCheckIns,
        lists = lists
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