package com.checkit.data

import com.checkit.domain.DueDatePreset
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskReminder
import com.checkit.domain.TaskReminderWriteInput
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskTag
import com.checkit.notifications.NoOpTaskReminderNotificationScheduler
import com.checkit.notifications.ScheduledTaskReminder
import com.checkit.notifications.TaskReminderNotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

interface CheckItRepository {
    fun observeTaskBoard(): Flow<TaskBoard>
    fun observeDailyPlans(): Flow<List<DailyPlan>>
    suspend fun ensureDefaultTaskData()
    suspend fun addList(input: TaskListWriteInput): Long
    suspend fun updateList(listId: Long, input: TaskListWriteInput)
    suspend fun addTag(input: TaskTagWriteInput): Long
    suspend fun updateTag(tagId: Long, input: TaskTagWriteInput)
    suspend fun isTagNameTaken(name: String, excludeTagId: Long? = null): Boolean
    suspend fun addTask(input: TaskWriteInput): Long
    suspend fun updateTask(taskId: Long, input: TaskWriteInput)
    suspend fun trashTask(taskId: Long)
    suspend fun restoreTask(taskId: Long)
    suspend fun completeTask(taskId: Long)
    suspend fun openTask(taskId: Long)
    suspend fun addTaskToDailyPlan(date: LocalDate, task: TaskItem): Long
    suspend fun addManualDoneToDailyPlan(
        date: LocalDate,
        title: String,
        note: String?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        source: DailyPlanItemSource = DailyPlanItemSource.CheckInManualDone,
        tagIds: List<Long> = emptyList()
    ): Long
    suspend fun updateDailyPlanItemTime(itemId: Long, startTimeMinutes: Int?, endTimeMinutes: Int?)
    suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput)
    suspend fun deleteDailyPlanItem(itemId: Long)
    suspend fun addNote(input: NoteWriteInput): Long
    suspend fun updateNote(noteId: Long, input: NoteWriteInput)
    suspend fun completeNote(noteId: Long)
    suspend fun openNote(noteId: Long)
    suspend fun trashNote(noteId: Long)
    suspend fun restoreNote(noteId: Long)
}

data class TaskListWriteInput(
    val name: String,
    val color: String,
    val icon: String
)

data class TaskTagWriteInput(
    val name: String,
    val color: String
)

data class TaskWriteInput(
    val listId: Long,
    val name: String,
    val description: String,
    val subtasks: List<SubTaskWriteInput>,
    val status: TaskStatus,
    val priority: TaskPriority,
    val doDate: LocalDate?,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val durationMinutes: Int?,
    val repeatRRule: String?,
    val reminders: List<TaskReminderWriteInput>,
    val tagIds: List<Long>
)

data class SubTaskWriteInput(
    val name: String,
    val isCompleted: Boolean
)

data class NoteWriteInput(
    val listId: Long,
    val title: String,
    val content: String,
    val status: TaskStatus,
    val date: LocalDate,
    val startTimeMinutes: Int?,
    val tagIds: List<Long>
)

data class DailyPlanItemWriteInput(
    val title: String,
    val note: String?,
    val source: DailyPlanItemSource,
    val status: DailyPlanItemStatus,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val tagIds: List<Long>
)

class RoomCheckItRepository(
    private val dao: CheckItDao,
    private val reminderNotificationScheduler: TaskReminderNotificationScheduler = NoOpTaskReminderNotificationScheduler()
) : CheckItRepository {
    private val seedMutex = Mutex()

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
            val listsById = rows.lists.associateBy { it.id }.mapValues { (_, entity) -> entity.toDomain() }

            TaskBoard(
                lists = listsById.values.toList(),
                filters = rows.filters.map { it.toDomain() },
                tasks = rows.tasks.map { task ->
                    task.toDomain(
                        list = listsById[task.listId] ?: TaskList.None,
                        subtasks = subTasksByTask[task.id].orEmpty().map { it.toDomain() },
                        reminders = remindersByTask[task.id].orEmpty().map { it.toDomain() },
                        tags = taskTagIds[task.id].orEmpty().mapNotNull { tagsById[it] }
                    )
                },
                notes = rows.notes.map { note ->
                    note.toDomain(
                        list = listsById[note.listId] ?: TaskList.None,
                        tags = noteTagIds[note.id].orEmpty().mapNotNull { tagsById[it] }
                    )
                },
                tags = domainTags
            )
        }

    override fun observeDailyPlans(): Flow<List<DailyPlan>> =
        combine(
            dao.observeDailyPlans(),
            dao.observeDailyPlanItems(),
            dao.observeDailyPlanItemTags(),
            dao.observeTags()
        ) { plans, items, itemTags, tags ->
            val domainTags = tags.map { it.toDomain() }
            val tagsById = domainTags.associateBy { it.id }
            val itemTagIds = itemTags.groupBy { it.itemId }.mapValues { it.value.map { it.tagId } }

            val itemsByPlan = items.groupBy { it.dailyPlanId }
            plans.map { plan ->
                plan.toDomain(
                    items = itemsByPlan[plan.id].orEmpty()
                        .map { item ->
                            item.toDomain(
                                tags = itemTagIds[item.id].orEmpty().mapNotNull { tagsById[it] }
                            )
                        }
                        .sortedWith(compareBy<DailyPlanItem> { it.startTimeMinutes }.thenBy { it.sortOrder })
                )
            }
        }

    override suspend fun ensureDefaultTaskData() = seedMutex.withLock {
        cleanupDuplicateSeedData()
        if (dao.listCount() > 0) return@withLock

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
        val workId = dao.insertTag(TagEntity(name = "Work", color = "#7C3AED"))
        val homeId = dao.insertTag(TagEntity(name = "Home", color = "#059669"))
        val todayTaskId = dao.insertTask(
            TaskEntity(
                listId = inboxId,
                name = "Plan the day",
                description = "Review agenda, timeline, and the next task to start.",
                status = TaskStatus.InProgress.name,
                priority = TaskPriority.High.name,
                doDateEpochDays = today.toEpochDays().toInt(),
                startTimeMinutes = 9 * 60,
                endTimeMinutes = 9 * 60 + 30,
                durationMinutes = 30,
                repeatRRule = "FREQ=DAILY;INTERVAL=1",
                sortOrder = 0,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        dao.insertTaskTagIfParentsExist(todayTaskId, workId)
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
                title = "Note basics",
                content = "Ideas, meeting notes, and loose thoughts live beside tasks in each list.",
                status = TaskStatus.Open.name,
                dateEpochDays = today.toEpochDays().toInt(),
                createdAtMillis = now,
                editedAtMillis = now,
                sortOrder = 1
            )
        )
        dao.insertNoteTagIfParentsExist(noteId, homeId)

        dao.insertFilter(TaskFilterEntity(name = "All", icon = "AllInclusive", color = "#475569", sortOrder = 0))
        dao.insertFilter(TaskFilterEntity(name = "Today", icon = "Today", color = "#2563EB", dueDatePreset = DueDatePreset.Today.name, sortOrder = 1))
        dao.insertFilter(TaskFilterEntity(name = "Completed", icon = "TaskAlt", color = "#059669", status = TaskStatus.Completed.name, sortOrder = 2))
        dao.insertFilter(TaskFilterEntity(name = "High priority", icon = "PriorityHigh", color = "#DC2626", priority = TaskPriority.High.name, sortOrder = 3))
        dao.insertFilter(TaskFilterEntity(name = "Trashed", icon = "Delete", color = "#6B7280", includeTrashed = true, sortOrder = 4))
    }

    override suspend fun addList(input: TaskListWriteInput): Long =
        dao.insertList(
            TaskListEntity(
                name = input.name,
                color = input.color,
                icon = input.icon,
                sortOrder = dao.nextListSortOrder()
            )
        )

    override suspend fun updateList(listId: Long, input: TaskListWriteInput) {
        dao.updateList(listId = listId, name = input.name, color = input.color, icon = input.icon)
    }

    override suspend fun addTag(input: TaskTagWriteInput): Long =
        dao.insertTag(
            TagEntity(
                name = input.name,
                color = input.color
            )
        )

    override suspend fun updateTag(tagId: Long, input: TaskTagWriteInput) {
        dao.updateTag(tagId = tagId, name = input.name, color = input.color)
    }

    override suspend fun isTagNameTaken(name: String, excludeTagId: Long?): Boolean =
        dao.tagNameInUseExcept(name = name, excludeId = excludeTagId ?: -1L) > 0

    override suspend fun addTask(input: TaskWriteInput): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val taskId = dao.insertTask(
            TaskEntity(
                listId = input.listId,
                name = input.name,
                description = input.description,
                status = input.status.name,
                priority = input.priority.name,
                doDateEpochDays = input.doDate?.toEpochDays()?.toInt(),
                startTimeMinutes = input.startTimeMinutes,
                endTimeMinutes = input.endTimeMinutes,
                durationMinutes = input.durationMinutes,
                repeatRRule = input.repeatRRule,
                sortOrder = dao.nextTaskSortOrder(input.listId),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        input.tagIds.forEach { tagId -> dao.insertTaskTagIfParentsExist(taskId, tagId) }
        dao.replaceTaskSubTasks(taskId, input.subtasks)
        dao.replaceTaskReminders(taskId, input.reminders)
        scheduleTaskReminders(taskId, input)
        return taskId
    }

    override suspend fun updateTask(taskId: Long, input: TaskWriteInput) {
        val existingTask = dao.taskById(taskId)
        val shouldRemoveOpenDailyPlanItems = existingTask?.hasDifferentScheduleThan(input) == true
        dao.updateTask(
            taskId = taskId,
            listId = input.listId,
            name = input.name,
            description = input.description,
            status = input.status.name,
            priority = input.priority.name,
            doDateEpochDays = input.doDate?.toEpochDays()?.toInt(),
            startTimeMinutes = input.startTimeMinutes,
            endTimeMinutes = input.endTimeMinutes,
            durationMinutes = input.durationMinutes,
            repeatRRule = input.repeatRRule,
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        dao.deleteTaskTags(taskId)
        input.tagIds.forEach { tagId -> dao.insertTaskTagIfParentsExist(taskId, tagId) }
        dao.replaceTaskSubTasks(taskId, input.subtasks)
        dao.replaceTaskReminders(taskId, input.reminders)
        if (shouldRemoveOpenDailyPlanItems) {
            dao.deleteOpenDailyPlanItemsForTask(taskId)
        }
        scheduleTaskReminders(taskId, input)
    }

    override suspend fun trashTask(taskId: Long) {
        dao.trashTask(taskId, Clock.System.now().toEpochMilliseconds())
        dao.deleteOpenDailyPlanItemsForTask(taskId)
        reminderNotificationScheduler.cancelTaskReminders(taskId)
    }

    override suspend fun restoreTask(taskId: Long) {
        dao.restoreTask(taskId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun completeTask(taskId: Long) {
        val instant = Clock.System.now()
        val today = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val completedAtMillis = instant.toEpochMilliseconds()
        dao.completeTask(
            taskId = taskId,
            status = TaskStatus.Completed.name,
            completedDateEpochDays = today.toEpochDays().toInt(),
            updatedAtMillis = completedAtMillis
        )
        dao.updateDailyPlanItemsForTaskStatus(
            taskId = taskId,
            status = DailyPlanItemStatus.Done.name,
            completedAtMillis = completedAtMillis
        )
        reminderNotificationScheduler.cancelTaskReminders(taskId)
    }

    override suspend fun openTask(taskId: Long) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.updateTaskStatusOpen(
            taskId = taskId,
            status = TaskStatus.Open.name,
            updatedAtMillis = now
        )
        dao.updateDailyPlanItemsForTaskStatus(
            taskId = taskId,
            status = DailyPlanItemStatus.Planned.name,
            completedAtMillis = null
        )
    }

    override suspend fun addTaskToDailyPlan(date: LocalDate, task: TaskItem): Long {
        val planId = ensureDailyPlan(date)
        val now = Clock.System.now().toEpochMilliseconds()
        val itemId = dao.insertDailyPlanItem(
            DailyPlanItemEntity(
                dailyPlanId = planId,
                taskId = task.id,
                title = task.name.ifBlank { "Untitled task" },
                source = DailyPlanItemSource.ExistingTask.name,
                status = if (task.status == TaskStatus.Completed) {
                    DailyPlanItemStatus.Done.name
                } else {
                    DailyPlanItemStatus.Planned.name
                },
                sortOrder = dao.nextDailyPlanItemSortOrder(planId),
                startTimeMinutes = task.startTimeMinutes,
                endTimeMinutes = task.endTimeMinutes,
                addedAtMillis = now,
                completedAtMillis = if (task.status == TaskStatus.Completed) now else null
            )
        )
        task.tags.forEach { tag -> dao.insertDailyPlanItemTagIfParentsExist(itemId, tag.id) }
        return itemId
    }

    override suspend fun addManualDoneToDailyPlan(
        date: LocalDate,
        title: String,
        note: String?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        source: DailyPlanItemSource,
        tagIds: List<Long>
    ): Long {
        val planId = ensureDailyPlan(date)
        val now = Clock.System.now().toEpochMilliseconds()
        val itemId = dao.insertDailyPlanItem(
            DailyPlanItemEntity(
                dailyPlanId = planId,
                title = title.trim(),
                note = note?.trim()?.takeIf { it.isNotBlank() },
                source = source.name,
                status = DailyPlanItemStatus.Done.name,
                sortOrder = dao.nextDailyPlanItemSortOrder(planId),
                startTimeMinutes = startTimeMinutes,
                endTimeMinutes = if (source == DailyPlanItemSource.CheckInNote) null else endTimeMinutes,
                addedAtMillis = now,
                completedAtMillis = now
            )
        )
        tagIds.forEach { tagId -> dao.insertDailyPlanItemTagIfParentsExist(itemId, tagId) }
        return itemId
    }

    override suspend fun updateDailyPlanItemTime(
        itemId: Long,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?
    ) {
        val item = dao.dailyPlanItemById(itemId)
        dao.updateDailyPlanItemTime(itemId, startTimeMinutes, endTimeMinutes)
        item?.taskId?.let { taskId ->
            dao.clearTaskTime(taskId, Clock.System.now().toEpochMilliseconds())
        }
    }

    override suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput) {
        dao.updateDailyPlanItem(
            itemId = itemId,
            title = input.title.trim(),
            note = input.note?.trim()?.takeIf { it.isNotBlank() },
            source = input.source.name,
            status = input.status.name,
            startTimeMinutes = input.startTimeMinutes,
            endTimeMinutes = if (input.source == DailyPlanItemSource.CheckInNote) null else input.endTimeMinutes,
            completedAtMillis = if (input.status == DailyPlanItemStatus.Done) {
                Clock.System.now().toEpochMilliseconds()
            } else {
                null
            }
        )
        dao.deleteDailyPlanItemTags(itemId)
        input.tagIds.forEach { tagId -> dao.insertDailyPlanItemTagIfParentsExist(itemId, tagId) }
    }

    override suspend fun deleteDailyPlanItem(itemId: Long) {
        dao.deleteDailyPlanItem(itemId)
    }

    override suspend fun addNote(input: NoteWriteInput): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val noteId = dao.insertNote(
            NoteEntity(
                listId = input.listId,
                title = input.title,
                content = input.content,
                status = input.status.name,
                dateEpochDays = input.date.toEpochDays().toInt(),
                startTimeMinutes = input.startTimeMinutes,
                createdAtMillis = now,
                editedAtMillis = now,
                sortOrder = dao.nextNoteSortOrder(input.listId)
            )
        )
        input.tagIds.forEach { tagId -> dao.insertNoteTagIfParentsExist(noteId, tagId) }
        return noteId
    }

    override suspend fun updateNote(noteId: Long, input: NoteWriteInput) {
        dao.updateNote(
            noteId = noteId,
            listId = input.listId,
            title = input.title,
            content = input.content,
            status = input.status.name,
            dateEpochDays = input.date.toEpochDays().toInt(),
            startTimeMinutes = input.startTimeMinutes,
            editedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        dao.deleteNoteTags(noteId)
        input.tagIds.forEach { tagId -> dao.insertNoteTagIfParentsExist(noteId, tagId) }
    }

    override suspend fun trashNote(noteId: Long) {
        dao.trashNote(noteId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun restoreNote(noteId: Long) {
        dao.restoreNote(noteId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun completeNote(noteId: Long) {
        dao.updateNoteStatus(
            noteId = noteId,
            status = TaskStatus.Completed.name,
            editedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun openNote(noteId: Long) {
        dao.updateNoteStatus(
            noteId = noteId,
            status = TaskStatus.Open.name,
            editedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    private suspend fun scheduleTaskReminders(taskId: Long, input: TaskWriteInput) {
        if (input.status == TaskStatus.Completed || input.status == TaskStatus.Cancelled) {
            reminderNotificationScheduler.cancelTaskReminders(taskId)
            return
        }
        reminderNotificationScheduler.scheduleTaskReminders(
            taskId = taskId,
            reminders = input.reminders.map { reminder ->
                ScheduledTaskReminder(
                    taskId = taskId,
                    taskName = input.name,
                    remindAtMillis = reminder.remindAtMillis,
                    label = reminder.label
                )
            }
        )
    }

    private suspend fun ensureDailyPlan(date: LocalDate): Long {
        val dateEpochDays = date.toEpochDays().toInt()
        val existing = dao.dailyPlanForDate(dateEpochDays)
        if (existing != null) return existing.id
        val now = Clock.System.now().toEpochMilliseconds()
        return dao.insertDailyPlan(
            DailyPlanEntity(
                dateEpochDays = dateEpochDays,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    private suspend fun cleanupDuplicateSeedData() {
        dao.deleteDuplicateSeedTasks()
        dao.deleteDuplicateSeedNotes()
        dao.deleteDuplicateSeedFilters()
        dao.deleteDuplicateSeedTags()
        dao.deleteDuplicateEmptySeedLists()
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

private fun TaskEntity.hasDifferentScheduleThan(input: TaskWriteInput): Boolean =
    doDateEpochDays != input.doDate?.toEpochDays()?.toInt() ||
        startTimeMinutes != input.startTimeMinutes ||
        endTimeMinutes != input.endTimeMinutes

private fun TaskEntity.toDomain(
    list: TaskList,
    subtasks: List<SubTaskItem>,
    reminders: List<TaskReminder>,
    tags: List<TaskTag>
) = TaskItem(
    id = id,
    list = list,
    name = name,
    description = description,
    subtasks = subtasks,
    status = enumValueOf(status),
    tags = tags,
    priority = enumValueOf(priority),
    doDate = doDateEpochDays?.let { LocalDate.fromEpochDays(it) },
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

private fun DailyPlanEntity.toDomain(items: List<DailyPlanItem>) = DailyPlan(
    id = id,
    date = LocalDate.fromEpochDays(dateEpochDays),
    items = items,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

private fun DailyPlanItemEntity.toDomain(tags: List<TaskTag> = emptyList()) = DailyPlanItem(
    id = id,
    dailyPlanId = dailyPlanId,
    taskId = taskId,
    title = title,
    note = note,
    source = enumValueOf(source),
    status = enumValueOf(status),
    tags = tags,
    sortOrder = sortOrder,
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = endTimeMinutes,
    addedAtMillis = addedAtMillis,
    completedAtMillis = completedAtMillis
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

private fun NoteEntity.toDomain(list: TaskList, tags: List<TaskTag>) = NoteItem(
    id = id,
    list = list,
    title = title,
    content = content,
    status = enumValueOf(status),
    tags = tags,
    date = LocalDate.fromEpochDays(dateEpochDays),
    startTimeMinutes = startTimeMinutes,
    createdAtMillis = createdAtMillis,
    editedAtMillis = editedAtMillis,
    sortOrder = sortOrder,
    trashedAtMillis = trashedAtMillis
)
