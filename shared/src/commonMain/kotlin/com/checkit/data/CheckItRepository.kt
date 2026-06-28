package com.checkit.data

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DueDatePreset
import com.checkit.domain.Goal
import com.checkit.domain.KeyResult
import com.checkit.domain.NoteItem
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskReminder
import com.checkit.domain.TaskReminderWriteInput
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskTag
import com.checkit.domain.hasEndTime
import com.checkit.notifications.DailyPlanScheduleReminderScheduler
import com.checkit.notifications.NoOpDailyPlanScheduleReminderScheduler
import com.checkit.notifications.NoOpTaskReminderNotificationScheduler
import com.checkit.notifications.ScheduledTaskReminder
import com.checkit.notifications.TaskReminderNotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    suspend fun addGoal(input: GoalWriteInput): Long
    suspend fun updateGoal(goalId: Long, input: GoalWriteInput)
    suspend fun deleteGoal(goalId: Long)
    suspend fun addObjective(input: ObjectiveWriteInput): Long
    suspend fun updateObjective(objectiveId: Long, input: ObjectiveWriteInput)
    suspend fun deleteObjective(objectiveId: Long)
    suspend fun addKeyResult(input: KeyResultWriteInput): Long
    suspend fun updateKeyResult(keyResultId: Long, input: KeyResultWriteInput)
    suspend fun deleteKeyResult(keyResultId: Long)
    suspend fun addTag(input: TagWriteInput): Long
    suspend fun updateTag(tagId: Long, input: TagWriteInput)
    suspend fun deleteTag(tagId: Long)
    suspend fun isTagNameTaken(name: String, excludeTagId: Long? = null): Boolean
    suspend fun addTask(input: TaskWriteInput): Long
    suspend fun updateTask(taskId: Long, input: TaskWriteInput)
    suspend fun trashTask(taskId: Long)
    suspend fun restoreTask(taskId: Long)
    suspend fun completeTask(taskId: Long)
    suspend fun openTask(taskId: Long)
    suspend fun addTaskToDailyPlan(date: LocalDate, task: TaskItem): Long
    suspend fun addDailyPlanItem(
        date: LocalDate,
        title: String,
        note: String?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        source: DailyPlanItemSource = DailyPlanItemSource.MyDayTask,
        status: DailyPlanItemStatus = DailyPlanItemStatus.Done,
        tagIds: List<Long> = emptyList()
    ): Long
    suspend fun updateDailyPlanItemTime(itemId: Long, startTimeMinutes: Int?, endTimeMinutes: Int?)
    suspend fun updateDailyPlanItemStatus(itemId: Long, status: DailyPlanItemStatus)
    suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput)
    suspend fun deleteDailyPlanItem(itemId: Long)
    suspend fun getDailyPlanItem(itemId: Long): DailyPlanItem?
    suspend fun countDoneDailyPlanItemsForTaskOnDate(taskId: Long, dateEpochDays: Int, excludeItemId: Long): Int
    suspend fun adjustKeyResultValue(keyResultId: Long, delta: Double)
    suspend fun getKeyResultForTask(taskId: Long): KeyResult?
    suspend fun addNote(input: NoteWriteInput): Long
    suspend fun updateNote(noteId: Long, input: NoteWriteInput)
    suspend fun completeNote(noteId: Long)
    suspend fun openNote(noteId: Long)
    suspend fun trashNote(noteId: Long)
    suspend fun restoreNote(noteId: Long)
}

data class GoalWriteInput(
    val title: String,
    val color: String,
    val icon: String
)

data class ObjectiveWriteInput(
    val name: String,
    val color: String,
    val icon: String,
    val goalId: Long? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

data class KeyResultWriteInput(
    val objectiveId: Long,
    val title: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String
)

data class TagWriteInput(
    val name: String,
    val color: String
)

data class TaskWriteInput(
    val objectiveId: Long,
    val keyResultId: Long? = null,
    val name: String,
    val description: String,
    val subtasks: List<SubTaskWriteInput>,
    val status: TaskStatus,
    val priority: TaskPriority,
    val doDate: LocalDate?,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val repeatRRule: String?,
    val reminders: List<TaskReminderWriteInput>,
    val tagIds: List<Long>
)

data class SubTaskWriteInput(
    val name: String,
    val isCompleted: Boolean
)

data class NoteWriteInput(
    val objectiveId: Long,
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
    private val reminderNotificationScheduler: TaskReminderNotificationScheduler = NoOpTaskReminderNotificationScheduler(),
    private val dailyPlanScheduleReminderScheduler: DailyPlanScheduleReminderScheduler =
        NoOpDailyPlanScheduleReminderScheduler()
) : CheckItRepository {
    private val seedMutex = Mutex()

    override fun observeTaskBoard(): Flow<TaskBoard> =
        combine(
            combine(
                dao.observeGoals(),
                dao.observeObjectives(),
                dao.observeFilters(),
                dao.observeTasks(),
                dao.observeNotes()
            ) { goals, objectives, filters, tasks, notes ->
                TaskBoardRows(goals, objectives, filters, tasks, notes)
            },
            combine(
                dao.observeSubTasks(),
                dao.observeReminders(),
                dao.observeTaskTags(),
                dao.observeNoteTags(),
                combine(dao.observeKeyResults(), dao.observeTags()) { keyResults, tags ->
                    TaskBoardMetadata(keyResults, tags)
                }
            ) { subTasks, reminders, taskTags, noteTags, metadata ->
                TaskBoardJoins(subTasks, reminders, taskTags, noteTags, metadata.keyResults, metadata.tags)
            }
        ) { rows, joins ->
            val domainGoals = rows.goals.map { it.toDomain() }
            val domainTags = joins.tags.map { it.toDomain() }
            val domainKeyResults = joins.keyResults.map { it.toDomain() }
            val tagsById = domainTags.associateBy { it.id }
            val keyResultsById = domainKeyResults.associateBy { it.id }
            val taskTagIds = joins.taskTags.groupBy { it.taskId }.mapValues { entry -> entry.value.map { it.tagId } }
            val noteTagIds = joins.noteTags.groupBy { it.noteId }.mapValues { entry -> entry.value.map { it.tagId } }
            val subTasksByTask = joins.subTasks.groupBy { it.taskId }
            val remindersByTask = joins.reminders.groupBy { it.taskId }
            val objectivesById = rows.objectives.associateBy { it.id }.mapValues { (_, entity) -> entity.toDomain() }

            TaskBoard(
                goals = domainGoals,
                objectives = objectivesById.values.toList(),
                keyResults = domainKeyResults,
                filters = rows.filters.map { it.toDomain() },
                tasks = rows.tasks.map { task ->
                    task.toDomain(
                        objective = objectivesById[task.objectiveId] ?: Objective.None,
                        keyResult = task.keyResultId?.let { keyResultsById[it] },
                        subtasks = subTasksByTask[task.id].orEmpty().map { it.toDomain() },
                        reminders = remindersByTask[task.id].orEmpty().map { it.toDomain() },
                        tags = taskTagIds[task.id].orEmpty().mapNotNull { tagsById[it] }
                    )
                },
                notes = rows.notes.map { note ->
                    note.toDomain(
                        objective = objectivesById[note.objectiveId] ?: Objective.None,
                        tags = noteTagIds[note.id].orEmpty().mapNotNull { tagsById[it] }
                    )
                },
                tags = domainTags
            )
        }

    override fun observeDailyPlans(): Flow<List<DailyPlan>> =
        combine(
            dao.observeDailyPlanItems(),
            dao.observeDailyPlanItemTags(),
            dao.observeTags()
        ) { items, itemTags, tags ->
            val domainTags = tags.map { it.toDomain() }
            val tagsById = domainTags.associateBy { it.id }
            val itemTagIds = itemTags.groupBy { it.itemId }.mapValues { it.value.map { it.tagId } }

            items.groupBy { it.dateEpochDays }
                .map { (dateEpochDays, itemEntities) ->
                    DailyPlan(
                        date = LocalDate.fromEpochDays(dateEpochDays),
                        items = itemEntities.map { item ->
                            item.toDomain(
                                tags = itemTagIds[item.id].orEmpty().mapNotNull { tagsById[it] }
                            )
                        }.sortedWith(compareBy<DailyPlanItem> { it.startTimeMinutes }.thenBy { it.sortOrder })
                    )
                }
                .sortedByDescending { it.date }
        }

    override suspend fun ensureDefaultTaskData() = seedMutex.withLock {
        cleanupDuplicateSeedData()
        ensureDefaultFilters()
        if (dao.objectiveCount() > 0) return@withLock

        val instant = Clock.System.now()
        val now = instant.toEpochMilliseconds()
        val today = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val inboxId = dao.insertObjective(
            ObjectiveEntity(
                title = "Inbox",
                color = "#2563EB",
                icon = "Inbox",
                sortOrder = 0
            )
        )
        val workId = dao.insertTag(TagEntity(name = "work", color = "#7C3AED"))
        val homeId = dao.insertTag(TagEntity(name = "home", color = "#059669"))
        val todayTaskId = dao.insertTask(
            TaskEntity(
                objectiveId = inboxId,
                name = "Plan the day",
                description = "Review agenda, timeline, and the next task to start.",
                status = TaskStatus.Open.name,
                priority = TaskPriority.High.name,
                doDateEpochDays = today.toEpochDays().toInt(),
                startTimeMinutes = 9 * 60,
                endTimeMinutes = 9 * 60 + 30,
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
                objectiveId = inboxId,
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
    }

    override suspend fun addGoal(input: GoalWriteInput): Long =
        dao.insertGoal(
            GoalEntity(
                title = input.title,
                color = input.color,
                icon = input.icon,
                sortOrder = dao.nextGoalSortOrder()
            )
        )

    override suspend fun updateGoal(goalId: Long, input: GoalWriteInput) {
        dao.updateGoal(goalId = goalId, title = input.title, color = input.color, icon = input.icon)
    }

    override suspend fun deleteGoal(goalId: Long) {
        dao.deleteGoal(goalId)
    }

    override suspend fun addObjective(input: ObjectiveWriteInput): Long =
        dao.insertObjective(
            ObjectiveEntity(
                title = input.name,
                goalId = input.goalId,
                startDateEpochDays = input.startDate?.toEpochDays()?.toInt(),
                endDateEpochDays = input.endDate?.toEpochDays()?.toInt(),
                color = input.color,
                icon = input.icon,
                sortOrder = dao.nextObjectiveSortOrder()
            )
        )

    override suspend fun updateObjective(objectiveId: Long, input: ObjectiveWriteInput) {
        dao.updateObjective(
            objectiveId = objectiveId,
            name = input.name,
            goalId = input.goalId,
            startDateEpochDays = input.startDate?.toEpochDays()?.toInt(),
            endDateEpochDays = input.endDate?.toEpochDays()?.toInt(),
            color = input.color,
            icon = input.icon
        )
    }

    override suspend fun deleteObjective(objectiveId: Long) {
        val inboxId = dao.inboxObjectiveId() ?: return
        if (objectiveId == inboxId) return
        dao.deleteObjectiveMovingContents(
            objectiveId = objectiveId,
            targetObjectiveId = inboxId,
            timestampMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun addKeyResult(input: KeyResultWriteInput): Long =
        dao.insertKeyResult(
            KeyResultEntity(
                objectiveId = input.objectiveId,
                title = input.title,
                targetValue = input.targetValue,
                currentValue = input.currentValue,
                unit = input.unit,
                sortOrder = dao.nextKeyResultSortOrder(input.objectiveId)
            )
        )

    override suspend fun updateKeyResult(keyResultId: Long, input: KeyResultWriteInput) {
        dao.updateKeyResult(
            keyResultId = keyResultId,
            objectiveId = input.objectiveId,
            title = input.title,
            targetValue = input.targetValue,
            currentValue = input.currentValue,
            unit = input.unit
        )
    }

    override suspend fun deleteKeyResult(keyResultId: Long) {
        dao.deleteKeyResult(keyResultId)
    }

    override suspend fun addTag(input: TagWriteInput): Long =
        dao.insertTag(
            TagEntity(
                name = input.name,
                color = input.color
            )
        )

    override suspend fun updateTag(tagId: Long, input: TagWriteInput) {
        dao.updateTag(tagId = tagId, name = input.name, color = input.color)
    }

    override suspend fun deleteTag(tagId: Long) {
        dao.deleteTag(tagId)
    }

    override suspend fun isTagNameTaken(name: String, excludeTagId: Long?): Boolean =
        dao.tagNameInUseExcept(name = name, excludeId = excludeTagId ?: -1L) > 0

    override suspend fun addTask(input: TaskWriteInput): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val keyResultId = input.keyResultIdForObjective()
        val taskId = dao.insertTask(
            TaskEntity(
                objectiveId = input.objectiveId,
                keyResultId = keyResultId,
                name = input.name,
                description = input.description,
                status = input.status.name,
                priority = input.priority.name,
                doDateEpochDays = input.doDate?.toEpochDays()?.toInt(),
                startTimeMinutes = input.startTimeMinutes,
                endTimeMinutes = input.endTimeMinutes,
                repeatRRule = input.repeatRRule,
                sortOrder = dao.nextTaskSortOrder(input.objectiveId),
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
        val keyResultId = input.keyResultIdForObjective()
        dao.updateTask(
            taskId = taskId,
            objectiveId = input.objectiveId,
            keyResultId = keyResultId,
            name = input.name,
            description = input.description,
            status = input.status.name,
            priority = input.priority.name,
            doDateEpochDays = input.doDate?.toEpochDays()?.toInt(),
            startTimeMinutes = input.startTimeMinutes,
            endTimeMinutes = input.endTimeMinutes,
            repeatRRule = input.repeatRRule,
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        dao.deleteTaskTags(taskId)
        input.tagIds.forEach { tagId -> dao.insertTaskTagIfParentsExist(taskId, tagId) }
        dao.replaceTaskSubTasks(taskId, input.subtasks)
        dao.replaceTaskReminders(taskId, input.reminders)
        if (shouldRemoveOpenDailyPlanItems) {
            dao.deletePlannedDailyPlanItemsForTask(taskId)
            dailyPlanScheduleReminderScheduler.rescheduleNext()
        }
        scheduleTaskReminders(taskId, input)
    }

    override suspend fun trashTask(taskId: Long) {
        dao.trashTask(taskId, Clock.System.now().toEpochMilliseconds())
        dao.deletePlannedDailyPlanItemsForTask(taskId)
        reminderNotificationScheduler.cancelTaskReminders(taskId)
        dailyPlanScheduleReminderScheduler.rescheduleNext()
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
        dao.completePlannedDailyPlanItemsForTask(
            taskId = taskId,
            completedAtMillis = completedAtMillis
        )
        reminderNotificationScheduler.cancelTaskReminders(taskId)
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun openTask(taskId: Long) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.updateTaskStatusOpen(
            taskId = taskId,
            status = TaskStatus.Open.name,
            updatedAtMillis = now
        )
    }

    override suspend fun addTaskToDailyPlan(date: LocalDate, task: TaskItem): Long {
        val dateEpochDays = date.toEpochDays().toInt()
        val now = Clock.System.now().toEpochMilliseconds()
        val itemId = dao.insertDailyPlanItem(
            DailyPlanItemEntity(
                dateEpochDays = dateEpochDays,
                taskId = task.id,
                title = task.name.ifBlank { "Untitled task" },
                source = DailyPlanItemSource.ExistingTask.name,
                status = if (task.status == TaskStatus.Completed) {
                    DailyPlanItemStatus.Done.name
                } else {
                    DailyPlanItemStatus.Planned.name
                },
                sortOrder = dao.nextDailyPlanItemSortOrder(dateEpochDays),
                startTimeMinutes = task.startTimeMinutes,
                endTimeMinutes = task.endTimeMinutes,
                addedAtMillis = now,
                completedAtMillis = if (task.status == TaskStatus.Completed) now else null
            )
        )
        task.tags.forEach { tag -> dao.insertDailyPlanItemTagIfParentsExist(itemId, tag.id) }
        dailyPlanScheduleReminderScheduler.rescheduleNext()
        return itemId
    }

    override suspend fun addDailyPlanItem(
        date: LocalDate,
        title: String,
        note: String?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        source: DailyPlanItemSource,
        status: DailyPlanItemStatus,
        tagIds: List<Long>
    ): Long {
        val dateEpochDays = date.toEpochDays().toInt()
        val now = Clock.System.now().toEpochMilliseconds()
        val itemId = dao.insertDailyPlanItem(
            DailyPlanItemEntity(
                dateEpochDays = dateEpochDays,
                title = title.trim(),
                note = note?.trim()?.takeIf { it.isNotBlank() },
                source = source.name,
                status = status.name,
                sortOrder = dao.nextDailyPlanItemSortOrder(dateEpochDays),
                startTimeMinutes = startTimeMinutes,
                endTimeMinutes = if (source.hasEndTime()) endTimeMinutes else null,
                addedAtMillis = now,
                completedAtMillis = if (status == DailyPlanItemStatus.Done) now else null
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
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun updateDailyPlanItemStatus(itemId: Long, status: DailyPlanItemStatus) {
        dao.updateDailyPlanItemStatus(
            itemId = itemId,
            status = status.name,
            completedAtMillis = if (status == DailyPlanItemStatus.Done) {
                Clock.System.now().toEpochMilliseconds()
            } else {
                null
            }
        )
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput) {
        dao.updateDailyPlanItem(
            itemId = itemId,
            title = input.title.trim(),
            note = input.note?.trim()?.takeIf { it.isNotBlank() },
            source = input.source.name,
            status = input.status.name,
            startTimeMinutes = input.startTimeMinutes,
            endTimeMinutes = if (input.source.hasEndTime()) input.endTimeMinutes else null,
            completedAtMillis = if (input.status == DailyPlanItemStatus.Done) {
                Clock.System.now().toEpochMilliseconds()
            } else {
                null
            }
        )
        dao.deleteDailyPlanItemTags(itemId)
        input.tagIds.forEach { tagId -> dao.insertDailyPlanItemTagIfParentsExist(itemId, tagId) }
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun deleteDailyPlanItem(itemId: Long) {
        dao.deleteDailyPlanItem(itemId)
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun getDailyPlanItem(itemId: Long): DailyPlanItem? {
        val item = dao.dailyPlanItemById(itemId) ?: return null
        val tagIds = dao.tagIdsForItem(itemId)
        val tags = if (tagIds.isNotEmpty()) dao.tagsByIds(tagIds).map { it.toDomain() } else emptyList()
        return item.toDomain(tags)
    }

    override suspend fun countDoneDailyPlanItemsForTaskOnDate(
        taskId: Long,
        dateEpochDays: Int,
        excludeItemId: Long
    ): Int = dao.countDoneDailyPlanItemsForTaskOnDate(taskId, dateEpochDays, excludeItemId)

    override suspend fun adjustKeyResultValue(keyResultId: Long, delta: Double) {
        dao.adjustKeyResultValue(keyResultId, delta)
    }

    override suspend fun getKeyResultForTask(taskId: Long): KeyResult? {
        return dao.keyResultByTaskId(taskId)?.toDomain()
    }

    override suspend fun addNote(input: NoteWriteInput): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val noteId = dao.insertNote(
            NoteEntity(
                objectiveId = input.objectiveId,
                title = input.title,
                content = input.content,
                status = input.status.name,
                dateEpochDays = input.date.toEpochDays().toInt(),
                startTimeMinutes = input.startTimeMinutes,
                createdAtMillis = now,
                editedAtMillis = now,
                sortOrder = dao.nextNoteSortOrder(input.objectiveId)
            )
        )
        input.tagIds.forEach { tagId -> dao.insertNoteTagIfParentsExist(noteId, tagId) }
        return noteId
    }

    override suspend fun updateNote(noteId: Long, input: NoteWriteInput) {
        dao.updateNote(
            noteId = noteId,
            objectiveId = input.objectiveId,
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
        if (input.status == TaskStatus.Completed) {
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

    private suspend fun TaskWriteInput.keyResultIdForObjective(): Long? {
        val keyResultId = keyResultId ?: return null
        val keyResult = dao.keyResultById(keyResultId) ?: return null
        return keyResultId.takeIf { keyResult.objectiveId == objectiveId }
    }

    private suspend fun cleanupDuplicateSeedData() {
        dao.deleteDuplicateSeedTasks()
        dao.deleteDuplicateSeedNotes()
        dao.deleteDuplicateSeedFilters()
        dao.deleteDuplicateSeedTags()
        dao.deleteDuplicateEmptySeedObjectives()
    }

    private suspend fun ensureDefaultFilters() {
        DefaultTaskFilters.forEach { filter ->
            dao.insertFilterIfNameMissing(
                name = filter.name,
                icon = filter.icon,
                color = filter.color,
                dueDatePreset = filter.dueDatePreset?.name,
                status = filter.status?.name,
                priority = filter.priority?.name,
                includeTrashed = filter.includeTrashed,
                sortOrder = filter.sortOrder
            )
        }
    }
}

private val DefaultTaskFilters = listOf(
    TaskFilterSeed(name = "All", icon = "AllInclusive", color = "#475569", sortOrder = 0),
    TaskFilterSeed(
        name = "Today",
        icon = "Today",
        color = "#2563EB",
        dueDatePreset = DueDatePreset.Today,
        sortOrder = 1
    ),
    TaskFilterSeed(
        name = "Upcoming",
        icon = "Schedule",
        color = "#0891B2",
        dueDatePreset = DueDatePreset.Upcoming,
        sortOrder = 2
    ),
    TaskFilterSeed(
        name = "Overdue",
        icon = "Flag",
        color = "#EA580C",
        dueDatePreset = DueDatePreset.Overdue,
        sortOrder = 3
    ),
    TaskFilterSeed(
        name = "No date",
        icon = "Schedule",
        color = "#7C3AED",
        dueDatePreset = DueDatePreset.NoDate,
        sortOrder = 4
    ),
    TaskFilterSeed(
        name = "Completed",
        icon = "TaskAlt",
        color = "#059669",
        status = TaskStatus.Completed,
        sortOrder = 5
    ),
    TaskFilterSeed(
        name = "High priority",
        icon = "PriorityHigh",
        color = "#DC2626",
        priority = TaskPriority.High,
        sortOrder = 6
    ),
    TaskFilterSeed(name = "Trashed", icon = "Delete", color = "#6B7280", includeTrashed = true, sortOrder = 7)
)

private data class TaskFilterSeed(
    val name: String,
    val icon: String,
    val color: String,
    val dueDatePreset: DueDatePreset? = null,
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val includeTrashed: Boolean = false,
    val sortOrder: Int
)

private data class TaskBoardRows(
    val goals: List<GoalEntity>,
    val objectives: List<ObjectiveEntity>,
    val filters: List<TaskFilterEntity>,
    val tasks: List<TaskEntity>,
    val notes: List<NoteEntity>
)

private data class TaskBoardJoins(
    val subTasks: List<SubTaskEntity>,
    val reminders: List<TaskReminderEntity>,
    val taskTags: List<TaskTagEntity>,
    val noteTags: List<NoteTagEntity>,
    val keyResults: List<KeyResultEntity>,
    val tags: List<TagEntity>
)

private data class TaskBoardMetadata(
    val keyResults: List<KeyResultEntity>,
    val tags: List<TagEntity>
)

private fun GoalEntity.toDomain() = Goal(
    id = id,
    title = title,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
    isArchived = isArchived
)

private fun ObjectiveEntity.toDomain() = Objective(
    id = id,
    goalId = goalId,
    name = title,
    startDate = startDateEpochDays?.let { LocalDate.fromEpochDays(it) },
    endDate = endDateEpochDays?.let { LocalDate.fromEpochDays(it) },
    color = color ?: "#2563EB",
    icon = icon ?: "List",
    sortOrder = sortOrder,
    isArchived = isArchived
)

private fun KeyResultEntity.toDomain() = KeyResult(
    id = id,
    objectiveId = objectiveId,
    title = title,
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    sortOrder = sortOrder
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
    objective: Objective,
    keyResult: KeyResult?,
    subtasks: List<SubTaskItem>,
    reminders: List<TaskReminder>,
    tags: List<TaskTag>
) = TaskItem(
    id = id,
    objective = objective,
    keyResult = keyResult,
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
    reminders = reminders,
    repeatRRule = repeatRRule,
    sortOrder = sortOrder,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    trashedAtMillis = trashedAtMillis
)

private fun DailyPlanItemEntity.toDomain(tags: List<TaskTag> = emptyList()) = DailyPlanItem(
    id = id,
    dateEpochDays = dateEpochDays,
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

private fun NoteEntity.toDomain(objective: Objective, tags: List<TaskTag>) = NoteItem(
    id = id,
    objective = objective,
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
