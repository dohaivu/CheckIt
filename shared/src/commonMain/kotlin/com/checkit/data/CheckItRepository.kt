package com.checkit.data

import com.checkit.domain.DueDatePreset
import com.checkit.domain.NoteItem
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskReminder
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

interface CheckItRepository {
    fun observeTaskBoard(): Flow<TaskBoard>
    suspend fun ensureDefaultTaskData()
}

class RoomCheckItRepository(
    private val dao: CheckItDao,
) : CheckItRepository {
    override fun observeTaskBoard(): Flow<TaskBoard> =
        combine(
            combine(
                dao.observeLists(),
                dao.observeFilters(),
                dao.observeTasks(),
                dao.observeNotes(),
                dao.observeTags()
            ) { lists, filters, tasks, notes, tags ->
                TaskBoardRows(lists, filters, tasks, notes, tags)
            },
            combine(
                dao.observeSubTasks(),
                dao.observeReminders(),
                dao.observeTaskTags(),
                dao.observeNoteTags()
            ) { subTasks, reminders, taskTags, noteTags ->
                TaskBoardJoins(subTasks, reminders, taskTags, noteTags)
            }
        ) { rows, joins ->
            val domainTags = rows.tags.map { it.toDomain() }
            val tagsById = domainTags.associateBy { it.id }
            val taskTagIds = joins.taskTags.groupBy { it.taskId }.mapValues { entry -> entry.value.map { it.tagId } }
            val noteTagIds = joins.noteTags.groupBy { it.noteId }.mapValues { entry -> entry.value.map { it.tagId } }
            val subTasksByTask = joins.subTasks.groupBy { it.taskId }
            val remindersByTask = joins.reminders.groupBy { it.taskId }

            TaskBoard(
                lists = rows.lists.map { it.toDomain() },
                filters = rows.filters.map { it.toDomain() },
                tasks = rows.tasks.map { task ->
                    task.toDomain(
                        subtasks = subTasksByTask[task.id].orEmpty().map { it.toDomain() },
                        reminders = remindersByTask[task.id].orEmpty().map { it.toDomain() },
                        tags = taskTagIds[task.id].orEmpty().mapNotNull { tagsById[it] }
                    )
                },
                notes = rows.notes.map { note ->
                    note.toDomain(tags = noteTagIds[note.id].orEmpty().mapNotNull { tagsById[it] })
                },
                tags = domainTags
            )
        }

    override suspend fun ensureDefaultTaskData() {
        if (dao.listCount() > 0) return

        val instant = Clock.System.now()
        val now = instant.toEpochMilliseconds()
        val today = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val inboxId = dao.insertList(
            TaskListEntity(
                name = "Inbox",
                color = "#2563EB",
                icon = "Inbox",
                sortOrder = 0
            )
        )
        val workId = dao.insertTag(TagEntity(name = "Work", icon = "Work", color = "#7C3AED"))
        val homeId = dao.insertTag(TagEntity(name = "Home", icon = "Home", color = "#059669"))
        val todayTaskId = dao.insertTask(
            TaskEntity(
                listId = inboxId,
                name = "Plan the day",
                description = "Review agenda, timeline, and the next task to start.",
                status = TaskStatus.InProgress.name,
                priority = TaskPriority.High.name,
                dueDateEpochDays = today.toEpochDays().toInt(),
                startTimeMinutes = 9 * 60,
                endTimeMinutes = 9 * 60 + 30,
                durationMinutes = 30,
                repeatRRule = "FREQ=DAILY;INTERVAL=1",
                sortOrder = 0,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        dao.insertTaskTag(TaskTagEntity(todayTaskId, workId))
        dao.insertSubTask(SubTaskEntity(taskId = todayTaskId, name = "Check calendar", sortOrder = 0))
        dao.insertSubTask(SubTaskEntity(taskId = todayTaskId, name = "Pick top priority", sortOrder = 1))
        dao.insertReminder(
            TaskReminderEntity(
                taskId = todayTaskId,
                remindAtMillis = now + 15 * 60 * 1000,
                label = "Before focus block"
            )
        )

        val noteId = dao.insertNote(
            NoteEntity(
                listId = inboxId,
                content = "Ideas, meeting notes, and loose thoughts live beside tasks in each list.",
                createdAtMillis = now,
                editedAtMillis = now,
                sortOrder = 1
            )
        )
        dao.insertNoteTag(NoteTagEntity(noteId, homeId))

        dao.insertFilter(TaskFilterEntity(name = "Today", icon = "Today", color = "#2563EB", dueDatePreset = DueDatePreset.Today.name, sortOrder = 0))
        dao.insertFilter(TaskFilterEntity(name = "Completed", icon = "TaskAlt", color = "#059669", status = TaskStatus.Completed.name, sortOrder = 1))
        dao.insertFilter(TaskFilterEntity(name = "High priority", icon = "PriorityHigh", color = "#DC2626", priority = TaskPriority.High.name, sortOrder = 2))
        dao.insertFilter(TaskFilterEntity(name = "Trashed", icon = "Delete", color = "#6B7280", includeTrashed = true, sortOrder = 3))
    }
}

private data class TaskBoardRows(
    val lists: List<TaskListEntity>,
    val filters: List<TaskFilterEntity>,
    val tasks: List<TaskEntity>,
    val notes: List<NoteEntity>,
    val tags: List<TagEntity>
)

private data class TaskBoardJoins(
    val subTasks: List<SubTaskEntity>,
    val reminders: List<TaskReminderEntity>,
    val taskTags: List<TaskTagEntity>,
    val noteTags: List<NoteTagEntity>
)

private fun TaskListEntity.toDomain() = TaskList(
    id = id,
    name = name,
    color = color,
    icon = icon,
    sortOrder = sortOrder,
    isArchived = isArchived
)

private fun TagEntity.toDomain() = TaskTag(
    id = id,
    name = name,
    icon = icon,
    color = color
)

private fun TaskFilterEntity.toDomain() = TaskFilter(
    id = id,
    name = name,
    icon = icon,
    color = color,
    tagId = tagId,
    dueDatePreset = dueDatePreset?.let { enumValueOf<DueDatePreset>(it) },
    status = status?.let { enumValueOf<TaskStatus>(it) },
    priority = priority?.let { enumValueOf<TaskPriority>(it) },
    includeTrashed = includeTrashed,
    sortOrder = sortOrder
)

private fun TaskEntity.toDomain(
    subtasks: List<SubTaskItem>,
    reminders: List<TaskReminder>,
    tags: List<TaskTag>
) = TaskItem(
    id = id,
    listId = listId,
    name = name,
    description = description,
    subtasks = subtasks,
    status = enumValueOf(status),
    tags = tags,
    priority = enumValueOf(priority),
    dueDate = dueDateEpochDays?.let { LocalDate.fromEpochDays(it) },
    completedDate = completedDateEpochDays?.let { LocalDate.fromEpochDays(it) },
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = endTimeMinutes,
    durationMinutes = durationMinutes,
    reminders = reminders,
    repeatRRule = repeatRRule,
    sortOrder = sortOrder,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    trashedAtMillis = trashedAtMillis
)

private fun SubTaskEntity.toDomain() = SubTaskItem(
    id = id,
    taskId = taskId,
    name = name,
    isCompleted = isCompleted,
    sortOrder = sortOrder
)

private fun TaskReminderEntity.toDomain() = TaskReminder(
    id = id,
    taskId = taskId,
    remindAtMillis = remindAtMillis,
    label = label
)

private fun NoteEntity.toDomain(tags: List<TaskTag>) = NoteItem(
    id = id,
    listId = listId,
    content = content,
    tags = tags,
    createdAtMillis = createdAtMillis,
    editedAtMillis = editedAtMillis,
    sortOrder = sortOrder,
    trashedAtMillis = trashedAtMillis
)
