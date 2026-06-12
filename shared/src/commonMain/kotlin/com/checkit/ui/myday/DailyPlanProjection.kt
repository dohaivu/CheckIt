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
    val plannedTasks: List<PlannedTaskProjection>,
    val notes: List<NoteItem>,
    val checkIns: List<DailyPlanItem>,
) {
    val tasks: List<TaskItem> = plannedTasks.map { it.task }
}

data class PlannedTaskProjection(
    val task: TaskItem,
    val dailyPlanItem: DailyPlanItem
)

fun List<DailyPlanItem>.toTaskViewProjection(
    board: TaskBoard,
    date: LocalDate
): MyDayTaskViewProjection {
    val realTasksById = board.tasksById
    val projectedTasks = mutableListOf<PlannedTaskProjection>()
    val projectedNotes = board.notes.filter { !it.isTrashed && it.date == date }
    val projectedCheckIns = mutableListOf<DailyPlanItem>()

    forEach { item ->
        val taskId = item.taskId
        if (taskId != null) {
            val realTask = realTasksById[taskId]
            if (realTask != null) {
                projectedTasks += PlannedTaskProjection(
                    task = realTask.copy(
                        status = item.status.toTaskStatus(),
                        startTimeMinutes = item.startTimeMinutes,
                        endTimeMinutes = item.endTimeMinutes,
                        sortOrder = item.sortOrder,
                        doDate = date
                    ),
                    dailyPlanItem = item
                )
            }
        } else {
            projectedCheckIns += item
        }
    }

    return MyDayTaskViewProjection(
        plannedTasks = projectedTasks,
        notes = projectedNotes,
        checkIns = projectedCheckIns
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
