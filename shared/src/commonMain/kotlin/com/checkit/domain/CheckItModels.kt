package com.checkit.domain

import kotlinx.datetime.LocalDate

data class AppConfig(val versionName: String)

data class TaskBoard(
    val goals: List<Goal> = emptyList(),
    val objectives: List<Objective> = emptyList(),
    val keyResults: List<KeyResult> = emptyList(),
    val filters: List<TaskFilter> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val tags: List<TaskTag> = emptyList()
) {
    val tasksById: Map<Long, TaskItem> by lazy { tasks.associateBy { it.id } }
    val notesById: Map<Long, NoteItem> by lazy { notes.associateBy { it.id } }
    val tasksByDate: Map<kotlinx.datetime.LocalDate, List<TaskItem>> by lazy {
        val map = mutableMapOf<kotlinx.datetime.LocalDate, MutableList<TaskItem>>()
        for (task in tasks) {
            if (!task.isTrashed && task.status != TaskStatus.Completed) {
                task.doDate?.let { date -> map.getOrPut(date) { mutableListOf() }.add(task) }
            }
        }
        map
    }
    val notesByDate: Map<kotlinx.datetime.LocalDate, List<NoteItem>> by lazy {
        val map = mutableMapOf<kotlinx.datetime.LocalDate, MutableList<NoteItem>>()
        for (note in notes) {
            if (!note.isTrashed && note.status != TaskStatus.Completed) {
                map.getOrPut(note.date) { mutableListOf() }.add(note)
            }
        }
        map
    }
}

data class Goal(
    val id: Long,
    val title: String,
    val icon: String,
    val color: String,
    val sortOrder: Int,
    val isArchived: Boolean = false
)

data class Objective(
    val id: Long,
    val goalId: Long? = null,
    val name: String,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val color: String,
    val icon: String,
    val sortOrder: Int,
    val isArchived: Boolean = false
) {
    val title: String get() = name

    companion object {
        val None = Objective(id = -1L, name = "", color = "", icon = "", sortOrder = -1)
        val MyDay = Objective(id = -2L, name = "MyDay", color = "0xFF64748B", icon = "Today", sortOrder = -2)
    }
}

data class KeyResult(
    val id: Long,
    val objectiveId: Long,
    val title: String,
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val unit: String,
    val sortOrder: Int
) {
    val progress: Double
        get() = if (targetValue == 0.0) 0.0 else (currentValue / targetValue).coerceIn(0.0, 1.0)
}

enum class KeyResultUnit(val label: String) {
    Percentage("%"),
    Number("#"), // quantity, count
    Currency("$"),
    Hours("h"),
    Days("d"),
    Points("pts"),
    Binary("completed"); // yes-1, no-0

    companion object {
        fun fromString(value: String): KeyResultUnit =
            entries.firstOrNull { it.name == value || it.label == value } ?: Number
    }
}

data class TaskItem(
    val id: Long,
    val objective: Objective,
    val keyResult: KeyResult? = null,
    val name: String,
    val description: String = "",
    val subtasks: List<SubTaskItem> = emptyList(),
    val status: TaskStatus = TaskStatus.Open,
    val tags: List<TaskTag> = emptyList(),
    val priority: TaskPriority = TaskPriority.None,
    val doDate: LocalDate? = null,
    val completedDate: LocalDate? = null,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val reminders: List<TaskReminder> = emptyList(),
    val repeatRRule: String? = null,
    val sortOrder: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val trashedAtMillis: Long? = null
) {
    val isTrashed: Boolean get() = trashedAtMillis != null
}

data class DailyPlan(
    val date: LocalDate,
    val items: List<DailyPlanItem> = emptyList()
)

data class DailyPlanItem(
    val id: Long,
    val dateEpochDays: Int,
    val taskId: Long? = null,
    val title: String,
    val note: String? = null,
    val source: DailyPlanItemSource,
    val status: DailyPlanItemStatus,
    val tags: List<TaskTag> = emptyList(),
    val sortOrder: Int,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val addedAtMillis: Long,
    val completedAtMillis: Long? = null
)

enum class DailyPlanItemSource {
    ExistingTask,
    MyDayTask,
    MyDayNote,
    MyDayReminder
}

fun DailyPlanItemSource.hasEndTime(): Boolean =
    this == DailyPlanItemSource.ExistingTask || this == DailyPlanItemSource.MyDayTask

enum class DailyPlanItemStatus {
    Planned,
    Done
}

data class SubTaskItem(
    val id: Long,
    val taskId: Long,
    val name: String,
    val isCompleted: Boolean,
    val sortOrder: Int
)

data class NoteItem(
    val id: Long,
    val objective: Objective,
    val title: String = "",
    val content: String,
    val tags: List<TaskTag> = emptyList(),
    val status: TaskStatus = TaskStatus.Open,
    val date: LocalDate,
    val startTimeMinutes: Int? = null,
    val createdAtMillis: Long,
    val editedAtMillis: Long,
    val sortOrder: Int,
    val trashedAtMillis: Long? = null
) {
    val isTrashed: Boolean get() = trashedAtMillis != null
}

data class TaskTag(
    val id: Long,
    val name: String,
    val color: String
) {
    companion object {
        val None = TaskTag(id = -1, name = "None", color = "#FFFFFF")
    }
}

data class TaskReminder(
    val id: Long,
    val taskId: Long,
    val remindAtMillis: Long,
    val label: String = ""
)

data class TaskFilter(
    val id: Long,
    val name: String,
    val icon: String,
    val color: String,
    val tagId: Long? = null,
    val dueDatePreset: DueDatePreset? = null,
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val includeTrashed: Boolean = false,
    val sortOrder: Int
)

enum class TaskStatus {
    Open,
    Completed
}

enum class TaskPriority {
    None,
    Low,
    Medium,
    High
}

enum class DueDatePreset {
    Today,
    Upcoming,
    Overdue,
    NoDate,
    Someday
}
