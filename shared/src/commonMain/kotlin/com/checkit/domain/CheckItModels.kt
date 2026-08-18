package com.checkit.domain

import kotlinx.datetime.LocalDate

data class AppConfig(val versionName: String)

data class TaskBoard(
    val lists: List<ListItem> = emptyList(),
    val filters: List<TaskFilter> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val tags: List<TagItem> = emptyList()
) {
    val tasksById: Map<Long, TaskItem> by lazy { tasks.associateBy { it.id } }
    val notesById: Map<Long, NoteItem> by lazy { notes.associateBy { it.id } }
    val tasksByDate: Map<LocalDate, List<TaskItem>> by lazy {
        val map = mutableMapOf<LocalDate, MutableList<TaskItem>>()
        for (task in tasks) {
            if (!task.isTrashed && task.status != TaskStatus.Completed) {
                task.doDate?.let { date -> map.getOrPut(date) { mutableListOf() }.add(task) }
            }
        }
        map
    }
    val notesByDate: Map<LocalDate, List<NoteItem>> by lazy {
        val map = mutableMapOf<LocalDate, MutableList<NoteItem>>()
        for (note in notes) {
            if (!note.isTrashed && note.status != TaskStatus.Completed) {
                note.date?.let { date -> map.getOrPut(date) { mutableListOf() }.add(note) }
            }
        }
        map
    }
}

data class ListItem(
    val id: Long,
    val title: String,
    val icon: String,
    val color: String,
    val sortOrder: Int,
    val isArchived: Boolean = false
)

data class TaskItem(
    val id: Long,
    val list: ListItem? = null,
    val name: String,
    val description: String = "",
    val subtasks: List<SubTaskItem> = emptyList(),
    val status: TaskStatus = TaskStatus.Open,
    val type: TaskType = TaskType.Task,
    val tags: List<TagItem> = emptyList(),
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

data class SubTaskItem(
    val id: Long,
    val taskId: Long,
    val name: String,
    val isCompleted: Boolean,
    val sortOrder: Int
)

data class NoteItem(
    val id: Long,
    val list: ListItem? = null,
    val title: String = "",
    val content: String,
    val tags: List<TagItem> = emptyList(),
    val status: TaskStatus = TaskStatus.Open,
    val date: LocalDate? = null,
    val startTimeMinutes: Int? = null,
    val createdAtMillis: Long,
    val editedAtMillis: Long,
    val sortOrder: Int,
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
    val nestedListItemId: Long? = null,
    val title: String,
    val note: String? = null,
    val source: DailyPlanItemSource,
    val status: DailyPlanItemStatus,
    val tags: List<TagItem> = emptyList(),
    val isHabit: Boolean = false,
    val sortOrder: Int,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val addedAtMillis: Long,
    val completedAtMillis: Long? = null,
    /** Id of the source item this was copied from via carry-over, if any. */
    val carriedFromItemId: Long? = null,
    /** Timestamp (epoch millis) when this item was resolved by a review or carry-over. */
    val handledAtMillis: Long? = null
) {
    fun workMinutes(): Int {
        val start = startTimeMinutes ?: return 0
        val end = endTimeMinutes ?: return 0
        return (end - start).coerceAtLeast(0)
    }

    /** Efficient check to see if this domain object matches a database entity and tag set. */
    fun isSameAs(entity: com.checkit.data.DailyPlanItemEntity, resolvedTags: List<TagItem>): Boolean {
        return id == entity.id &&
            dateEpochDays == entity.dateEpochDays &&
            taskId == entity.taskId &&
            nestedListItemId == entity.nestedListItemId &&
            title == entity.title &&
            note == entity.note &&
            source.name == entity.source &&
            status.name == entity.status &&
            sortOrder == entity.sortOrder &&
            startTimeMinutes == entity.startTimeMinutes &&
            endTimeMinutes == entity.endTimeMinutes &&
            isHabit == entity.isHabit &&
            addedAtMillis == entity.addedAtMillis &&
            completedAtMillis == entity.completedAtMillis &&
            carriedFromItemId == entity.carriedFromItemId &&
            handledAtMillis == entity.handledAtMillis &&
            tags == resolvedTags
    }
}

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

data class JournalEntry(
    val id: Long,
    val dateEpochDays: Int,
    /** Activity, location, or any specific thing this entry is about, e.g. "Biking", "Cafe". */
    val context: String? = null,
    /** Freeform status text. */
    val content: String,
    val moods: List<String> = emptyList(),
    val tags: List<TagItem> = emptyList(),
    val createdTimeMinutes: Int,
    val attachments: List<String> = emptyList()
)

data class TagItem(
    val id: Long,
    val name: String,
    val color: String,
    val sortOrder: Int = 0,
    val lastUsedAtMillis: Long = 0L
) {
    companion object {
        val None = TagItem(id = -1, name = "None", color = "#FFFFFF")
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

enum class TaskType {
    Task,
    Habit
}

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
