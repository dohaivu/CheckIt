package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

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
    val isArchived: Boolean = false,
    val sections: List<ListSection> = emptyList()
)

data class ListSection(
    val id: Long,
    val listId: Long,
    val title: String,
    val color: String,
    val sortOrder: Int
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
    val label: String? = null,
    val sortOrder: Int = 0,
    val isPinned: Boolean = false,
    val sectionId: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val trashedAtMillis: Long? = null
) {
    val isTrashed: Boolean get() = trashedAtMillis != null

    fun isSameAs(
        name: String,
        description: String,
        statusName: String,
        priorityName: String,
        typeName: String,
        doDateEpochDays: Int?,
        completedDateEpochDays: Int?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        repeatRRule: String?,
        label: String?,
        createdAtMillis: Long,
        updatedAtMillis: Long,
        trashedAtMillis: Long?,
        resolvedListId: Long?,
        resolvedSubtasks: List<SubTaskItem>,
        resolvedReminders: List<TaskReminder>,
        resolvedTags: List<TagItem>,
        resolvedSortOrder: Int,
        resolvedIsPinned: Boolean,
        resolvedSectionId: Long?
    ): Boolean {
        return this.id == id &&
            this.name == name &&
            this.description == description &&
            this.status.name == statusName &&
            this.priority.name == priorityName &&
            this.type.name == typeName &&
            this.doDate?.toEpochDays()?.toInt() == doDateEpochDays &&
            this.completedDate?.toEpochDays()?.toInt() == completedDateEpochDays &&
            this.startTimeMinutes == startTimeMinutes &&
            this.endTimeMinutes == endTimeMinutes &&
            this.repeatRRule == repeatRRule &&
            this.label == label &&
            this.createdAtMillis == createdAtMillis &&
            this.updatedAtMillis == updatedAtMillis &&
            this.trashedAtMillis == trashedAtMillis &&
            this.list?.id == resolvedListId &&
            this.subtasks == resolvedSubtasks &&
            this.reminders == resolvedReminders &&
            this.tags == resolvedTags &&
            this.sortOrder == resolvedSortOrder &&
            this.isPinned == resolvedIsPinned &&
            this.sectionId == resolvedSectionId
    }
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
    val label: String? = null,
    val createdAtMillis: Long,
    val editedAtMillis: Long,
    val sortOrder: Int = 0,
    val isPinned: Boolean = false,
    val sectionId: Long? = null,
    val trashedAtMillis: Long? = null
) {
    val isTrashed: Boolean get() = trashedAtMillis != null

    fun isSameAs(
        title: String,
        content: String,
        statusName: String,
        dateEpochDays: Int?,
        startTimeMinutes: Int?,
        createdAtMillis: Long,
        editedAtMillis: Long,
        label: String?,
        trashedAtMillis: Long?,
        resolvedListId: Long?,
        resolvedTags: List<TagItem>,
        resolvedSortOrder: Int,
        resolvedIsPinned: Boolean,
        resolvedSectionId: Long?
    ): Boolean {
        return this.id == id &&
            this.title == title &&
            this.content == content &&
            this.status.name == statusName &&
            this.date?.toEpochDays()?.toInt() == dateEpochDays &&
            this.startTimeMinutes == startTimeMinutes &&
            this.createdAtMillis == createdAtMillis &&
            this.editedAtMillis == editedAtMillis &&
            this.label == label &&
            this.trashedAtMillis == trashedAtMillis &&
            this.list?.id == resolvedListId &&
            this.tags == resolvedTags &&
            this.sortOrder == resolvedSortOrder &&
            this.isPinned == resolvedIsPinned &&
            this.sectionId == resolvedSectionId
    }
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
    val label: String? = null,
    val isHabit: Boolean = false,
    val sortOrder: Int,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val addedAtMillis: Long,
    val completedAtMillis: Long? = null,
    val carriedFromItemId: Long? = null,
    val handledAtMillis: Long? = null
) {
    fun workMinutes(): Int {
        val start = startTimeMinutes ?: return 0
        val end = endTimeMinutes ?: return 0
        return (end - start).coerceAtLeast(0)
    }

    fun isSameAs(
        dateEpochDays: Int,
        taskId: Long?,
        nestedListItemId: Long?,
        title: String,
        note: String?,
        sourceName: String,
        statusName: String,
        label: String?,
        sortOrder: Int,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        isHabit: Boolean,
        addedAtMillis: Long,
        completedAtMillis: Long?,
        carriedFromItemId: Long?,
        handledAtMillis: Long?,
        resolvedTags: List<TagItem>
    ): Boolean {
        return this.id == id &&
            this.dateEpochDays == dateEpochDays &&
            this.taskId == taskId &&
            this.nestedListItemId == nestedListItemId &&
            this.title == title &&
            this.note == note &&
            this.source.name == sourceName &&
            this.status.name == statusName &&
            this.label == label &&
            this.sortOrder == sortOrder &&
            this.startTimeMinutes == startTimeMinutes &&
            this.endTimeMinutes == endTimeMinutes &&
            this.isHabit == isHabit &&
            this.addedAtMillis == addedAtMillis &&
            this.completedAtMillis == completedAtMillis &&
            this.carriedFromItemId == carriedFromItemId &&
            this.handledAtMillis == handledAtMillis &&
            this.tags == resolvedTags
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
    val label: String? = null,
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

enum class MetricUnit {
    None,
    Percentage,
    Points,
    Items,
    Hours,
    Days,
    Rating,
    VND,
    Lan,
    Km,
    Custom
}

/**
 * A manually tracked metric attached to a [PeriodGoal] or nested list item:
 * free-form name/value pair with an optional unit. Stored inline as JSON.
 */
@Serializable
data class MetricItem(
    val name: String,
    val value: String,
    val targetValue: String? = null,
    val unit: MetricUnit = MetricUnit.None,
    val customUnit: String? = null,
    val sortOrder: Int = 0,
    val enabled: Boolean = true,
    val isCompleted: Boolean = false
)