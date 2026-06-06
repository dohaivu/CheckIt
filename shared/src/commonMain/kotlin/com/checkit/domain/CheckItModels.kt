package com.checkit.domain

import kotlinx.datetime.LocalDate

data class AppConfig(val versionName: String)

data class TaskBoard(
    val lists: List<TaskList> = emptyList(),
    val filters: List<TaskFilter> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val tags: List<TaskTag> = emptyList()
)

data class TaskList(
    val id: Long,
    val name: String,
    val color: String,
    val icon: String,
    val sortOrder: Int,
    val isArchived: Boolean = false
)

data class TaskItem(
    val id: Long,
    val listId: Long,
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
    val durationMinutes: Int? = null,
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
    val id: Long,
    val date: LocalDate,
    val items: List<DailyPlanItem> = emptyList(),
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class DailyPlanItem(
    val id: Long,
    val dailyPlanId: Long,
    val taskId: Long? = null,
    val titleSnapshot: String,
    val note: String? = null,
    val source: DailyPlanItemSource,
    val status: DailyPlanItemStatus,
    val sortOrder: Int,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val addedAtMillis: Long,
    val completedAtMillis: Long? = null
)

enum class DailyPlanItemSource {
    ExistingTask,
    QuickTask,
    CheckInManualDone,
    CheckInNote
}

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
    val listId: Long,
    val content: String,
    val tags: List<TaskTag> = emptyList(),
    val status: TaskStatus = TaskStatus.Open,
    val date: LocalDate,
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
)

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
    InProgress,
    Blocked,
    Completed,
    Cancelled
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
    Someday
}
