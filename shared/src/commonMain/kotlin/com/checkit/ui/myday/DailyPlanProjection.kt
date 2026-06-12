package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
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
                    task = realTask,
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
