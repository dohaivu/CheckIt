package com.checkit.data

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayCloseCommitResult
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewPeriod
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import com.checkit.domain.DueDatePreset
import com.checkit.domain.Goal
import com.checkit.domain.JournalEntry
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
import com.checkit.domain.TagItem
import com.checkit.domain.TaskType
import com.checkit.domain.hasEndTime
import com.checkit.notifications.DailyPlanScheduleReminderScheduler
import com.checkit.notifications.NoOpDailyPlanScheduleReminderScheduler
import com.checkit.notifications.NoOpTaskReminderNotificationScheduler
import com.checkit.notifications.ScheduledTaskReminder
import com.checkit.notifications.TaskReminderNotificationScheduler
import com.checkit.ui.tasks.views.currentTimeMinutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

interface CheckItRepository {
    fun observeTaskBoard(): Flow<TaskBoard>
    fun observeDailyPlans(): Flow<List<DailyPlan>>
    fun observeJournalEntries(): Flow<List<JournalEntry>>
    suspend fun addJournalEntry(input: JournalEntryWriteInput): Long
    suspend fun updateJournalEntry(entryId: Long, input: JournalEntryWriteInput)
    suspend fun deleteJournalEntry(entryId: Long)
    suspend fun addGoal(input: GoalWriteInput): Long    suspend fun updateGoal(goalId: Long, input: GoalWriteInput)
    suspend fun deleteGoal(goalId: Long)
    suspend fun addObjective(input: ObjectiveWriteInput): Long
    suspend fun updateObjective(objectiveId: Long, input: ObjectiveWriteInput)
    suspend fun deleteObjective(objectiveId: Long)
    suspend fun addKeyResult(input: KeyResultWriteInput): Long
    suspend fun updateKeyResult(keyResultId: Long, input: KeyResultWriteInput)
    suspend fun deleteKeyResult(keyResultId: Long)
    suspend fun addTag(input: TagWriteInput): Long
    suspend fun updateTag(tagId: Long, input: TagWriteInput)
    suspend fun updateTagSortOrder(tagId: Long, sortOrder: Int)
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
    suspend fun updateDailyPlanItemTimes(updates: List<DailyPlanItemTimeUpdate>) {
        updates.forEach { update ->
            updateDailyPlanItemTime(update.itemId, update.startTimeMinutes, update.endTimeMinutes)
        }
    }
    suspend fun updateDailyPlanItemStatus(itemId: Long, status: DailyPlanItemStatus)
    suspend fun updateDailyPlanItemsStatus(itemIds: List<Long>, status: DailyPlanItemStatus)
    suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput)
    suspend fun updateDailyPlanItemTags(itemId: Long, tagIds: List<Long>)
    suspend fun deleteDailyPlanItem(itemId: Long)
    suspend fun getDailyPlanItem(itemId: Long): DailyPlanItem?
    suspend fun dailyPlanForDate(date: LocalDate): DailyPlan?
    fun observePeriodReviews(): Flow<List<PeriodReview>>
    suspend fun periodReviewFor(period: ReviewPeriod, date: LocalDate): PeriodReview?
    suspend fun savePeriodReview(review: PeriodReview)
    /**
     * Applies a complete evening review atomically.
     * @return result with carry/skip counts; goal note handling.
     */
    suspend fun completeDayClose(
        date: LocalDate,
        markDoneItemIds: List<Long>,
        carryItemIds: List<Long>,
        dropItemIds: List<Long>,
        winNote: String?,
        tomorrowGoal: String?,
        doneCount: Int,
        plannedCount: Int,
        doneMinutes: Int,
        targetDate: LocalDate,
        nowMillis: Long
    ): DayCloseCommitResult
    /**
     * Copies a plan item onto [targetDate] as Planned.
     * @return new item id, or null if skipped (same taskId already on that date,
     * or the item was already carried onto that date).
     */
    suspend fun copyDailyPlanItemToDate(
        source: DailyPlanItem,
        targetDate: LocalDate,
        clearTimes: Boolean
    ): Long?
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

data class DailyPlanItemTimeUpdate(
    val itemId: Long,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?
)

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
    val type: TaskType = TaskType.Task,
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
    val date: LocalDate?,
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

data class JournalEntryWriteInput(
    val date: LocalDate,
    val context: String?,
    val content: String,
    val moods: List<String> = emptyList(),
    val tagIds: List<Long> = emptyList(),
    val attachments: List<String> = emptyList()
)

class RoomCheckItRepository(
    private val dao: CheckItDao,
    private val reminderNotificationScheduler: TaskReminderNotificationScheduler = NoOpTaskReminderNotificationScheduler(),
    private val dailyPlanScheduleReminderScheduler: DailyPlanScheduleReminderScheduler =
        NoOpDailyPlanScheduleReminderScheduler()
) : CheckItRepository {
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
                combine(
                    dao.observeKeyResults(),
                    dao.observeTags(),
                    dao.observeTaskObjectives(),
                    dao.observeTaskKeyResults(),
                    dao.observeNoteObjectives()
                ) { keyResults, tags, taskObjectives, taskKeyResults, noteObjectives ->
                    TaskBoardMetadata(keyResults, tags, taskObjectives, taskKeyResults, noteObjectives)
                }
            ) { subTasks, reminders, taskTags, noteTags, metadata ->
                TaskBoardJoins(
                    subTasks,
                    reminders,
                    taskTags,
                    noteTags,
                    metadata.keyResults,
                    metadata.tags,
                    metadata.taskObjectives,
                    metadata.taskKeyResults,
                    metadata.noteObjectives
                )
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

            val taskObjectiveMap = joins.taskObjectives.associate { it.taskId to it.objectiveId }
            val taskKeyResultMap = joins.taskKeyResults.associate { it.taskId to it.keyResultId }
            val noteObjectiveMap = joins.noteObjectives.associate { it.noteId to it.objectiveId }

            TaskBoard(
                goals = domainGoals,
                objectives = objectivesById.values.toList(),
                keyResults = domainKeyResults,
                filters = rows.filters.map { it.toDomain() },
                tasks = rows.tasks.map { task ->
                    val objectiveId = taskObjectiveMap[task.id]
                    val keyResultId = taskKeyResultMap[task.id]
                    task.toDomain(
                        objective = objectivesById[objectiveId] ?: Objective.None,
                        keyResult = keyResultId?.let { keyResultsById[it] },
                        subtasks = subTasksByTask[task.id].orEmpty().map { it.toDomain() },
                        reminders = remindersByTask[task.id].orEmpty().map { it.toDomain() },
                        tags = taskTagIds[task.id].orEmpty().mapNotNull { tagsById[it] }
                    )
                },
                notes = rows.notes.map { note ->
                    val objectiveId = noteObjectiveMap[note.id]
                    note.toDomain(
                        objective = objectivesById[objectiveId] ?: Objective.None,
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

    override fun observeJournalEntries(): Flow<List<JournalEntry>> =
        combine(
            dao.observeJournalEntries(),
            dao.observeJournalEntryTags(),
            dao.observeTags()
        ) { entries, entryTags, tags ->
            val domainTags = tags.map { it.toDomain() }
            val tagsById = domainTags.associateBy { it.id }
            val entryTagIds = entryTags.groupBy { it.entryId }.mapValues { it.value.map { it.tagId } }

            entries.map { entry ->
                entry.toDomain(
                    tags = entryTagIds[entry.id].orEmpty().mapNotNull { tagsById[it] }
                )
            }
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
            targetObjectiveId = inboxId
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
                color = input.color,
                sortOrder = dao.nextTagSortOrder()
            )
        )

    override suspend fun updateTag(tagId: Long, input: TagWriteInput) {
        dao.updateTag(tagId = tagId, name = input.name, color = input.color)
    }

    override suspend fun updateTagSortOrder(tagId: Long, sortOrder: Int) {
        dao.updateTagSortOrder(tagId, sortOrder)
    }

    override suspend fun deleteTag(tagId: Long) {
        dao.deleteTag(tagId)
    }

    override suspend fun isTagNameTaken(name: String, excludeTagId: Long?): Boolean =
        dao.tagNameInUseExcept(name = name, excludeId = excludeTagId ?: -1L) > 0

    override suspend fun addTask(input: TaskWriteInput): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val keyResultId = input.keyResultIdForObjective()
        val isHabit = input.type == TaskType.Habit
        val taskId = dao.insertTask(
            TaskEntity(
                name = input.name,
                description = input.description,
                status = input.status.name,
                priority = input.priority.name,
                type = input.type.name,
                doDateEpochDays = if (isHabit) null else input.doDate?.toEpochDays()?.toInt(),
                startTimeMinutes = if (isHabit) null else input.startTimeMinutes,
                endTimeMinutes = if (isHabit) null else input.endTimeMinutes,
                repeatRRule = if (isHabit) null else input.repeatRRule,
                sortOrder = dao.nextTaskSortOrder(input.objectiveId),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        dao.insertTaskObjective(TaskObjectiveEntity(taskId, input.objectiveId))
        keyResultId?.let { dao.insertTaskKeyResult(TaskKeyResultEntity(taskId, it)) }
        input.tagIds.forEach { tagId -> addTaskTag(taskId, tagId) }
        dao.replaceTaskSubTasks(taskId, input.subtasks)
        dao.replaceTaskReminders(taskId, input.reminders)
        scheduleTaskReminders(taskId, input)
        return taskId
    }

    override suspend fun updateTask(taskId: Long, input: TaskWriteInput) {
        val existingTask = dao.taskById(taskId)
        val shouldRemoveOpenDailyPlanItems = existingTask?.hasDifferentScheduleThan(input) == true
        val keyResultId = input.keyResultIdForObjective()
        val isHabit = input.type == TaskType.Habit
        dao.updateTask(
            taskId = taskId,
            name = input.name,
            description = input.description,
            status = input.status.name,
            priority = input.priority.name,
            type = input.type.name,
            doDateEpochDays = if (isHabit) null else input.doDate?.toEpochDays()?.toInt(),
            startTimeMinutes = if (isHabit) null else input.startTimeMinutes,
            endTimeMinutes = if (isHabit) null else input.endTimeMinutes,
            repeatRRule = if (isHabit) null else input.repeatRRule,
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        dao.deleteTaskObjective(taskId)
        dao.deleteTaskKeyResult(taskId)
        dao.insertTaskObjective(TaskObjectiveEntity(taskId, input.objectiveId))
        keyResultId?.let { dao.insertTaskKeyResult(TaskKeyResultEntity(taskId, it)) }
        dao.deleteTaskTags(taskId)
        input.tagIds.forEach { tagId -> addTaskTag(taskId, tagId) }
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
                isHabit = task.type == TaskType.Habit,
                addedAtMillis = now,
                completedAtMillis = if (task.status == TaskStatus.Completed) now else null
            )
        )
        task.tags.forEach { tag -> addDailyPlanItemTag(itemId, tag.id) }
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
        tagIds.forEach { tagId -> addDailyPlanItemTag(itemId, tagId) }
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

    override suspend fun updateDailyPlanItemTimes(updates: List<DailyPlanItemTimeUpdate>) {
        if (updates.isEmpty()) return
        val items = updates.map { update -> dao.dailyPlanItemById(update.itemId) }
        dao.updateDailyPlanItemTimes(updates)
        items.mapNotNull { it?.taskId }.forEach { taskId ->
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

    override suspend fun updateDailyPlanItemsStatus(
        itemIds: List<Long>,
        status: DailyPlanItemStatus
    ) {
        if (itemIds.isEmpty()) return
        dao.updateDailyPlanItemsStatus(
            itemIds = itemIds,
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
        input.tagIds.forEach { tagId -> addDailyPlanItemTag(itemId, tagId) }
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun updateDailyPlanItemTags(itemId: Long, tagIds: List<Long>) {
        dao.deleteDailyPlanItemTags(itemId)
        tagIds.forEach { tagId -> addDailyPlanItemTag(itemId, tagId) }
    }

    override suspend fun deleteDailyPlanItem(itemId: Long) {
        dao.deleteDailyPlanItem(itemId)
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun addJournalEntry(input: JournalEntryWriteInput): Long {
        val entryId = dao.insertJournalEntry(
            JournalEntryEntity(
                dateEpochDays = input.date.toEpochDays().toInt(),
                context = input.context?.trim()?.takeIf { it.isNotBlank() },
                content = input.content.trim(),
                moods = input.moods.joinToString(","),
                createdTimeMinutes = currentTimeMinutes(),
                attachments = input.attachments.joinToString(",")
            )
        )
        input.tagIds.forEach { tagId -> addJournalEntryTag(entryId, tagId) }
        return entryId
    }

    override suspend fun updateJournalEntry(entryId: Long, input: JournalEntryWriteInput) {
        dao.updateJournalEntry(
            entryId = entryId,
            context = input.context?.trim()?.takeIf { it.isNotBlank() },
            content = input.content.trim(),
            moods = input.moods.joinToString(","),
            attachments = input.attachments.joinToString(",")
        )
        dao.deleteJournalEntryTags(entryId)
        input.tagIds.forEach { tagId -> addJournalEntryTag(entryId, tagId) }
    }

    override suspend fun deleteJournalEntry(entryId: Long) {
        dao.deleteJournalEntry(entryId)
    }

    override suspend fun getDailyPlanItem(itemId: Long): DailyPlanItem? {
        val item = dao.dailyPlanItemById(itemId) ?: return null
        val tagIds = dao.tagIdsForItem(itemId)
        val tags = if (tagIds.isNotEmpty()) dao.tagsByIds(tagIds).map { it.toDomain() } else emptyList()
        return item.toDomain(tags)
    }

    override suspend fun dailyPlanForDate(date: LocalDate): DailyPlan? {
        val items = dao.dailyPlanItemsForDate(date.toEpochDays().toInt())
            .map { item ->
                val tagIds = dao.tagIdsForItem(item.id)
                val tags = if (tagIds.isNotEmpty()) dao.tagsByIds(tagIds).map { it.toDomain() } else emptyList()
                item.toDomain(tags)
            }
            .sortedWith(compareBy<DailyPlanItem> { it.startTimeMinutes }.thenBy { it.sortOrder })
        return if (items.isEmpty()) null else DailyPlan(date = date, items = items)
    }

    override suspend fun copyDailyPlanItemToDate(
        source: DailyPlanItem,
        targetDate: LocalDate,
        clearTimes: Boolean
    ): Long? {
        val targetEpochDays = targetDate.toEpochDays().toInt()
        val targetItems = dao.dailyPlanItemsForDate(targetEpochDays)
        val alreadyPresent = targetItems.any { item ->
            (source.taskId != null && item.taskId == source.taskId) ||
                (item.carriedFromItemId != null && item.carriedFromItemId == source.id)
        }
        val now = Clock.System.now().toEpochMilliseconds()
        if (alreadyPresent) {
            dao.markDailyPlanItemsHandled(listOf(source.id), now)
            return null
        }
        val startTime = if (clearTimes) null else source.startTimeMinutes
        val endTime = when {
            clearTimes -> null
            source.source.hasEndTime() -> source.endTimeMinutes
            else -> null
        }
        val itemId = dao.insertDailyPlanItem(
            DailyPlanItemEntity(
                dateEpochDays = targetEpochDays,
                taskId = source.taskId,
                title = source.title.ifBlank { "Untitled" },
                note = source.note,
                source = source.source.name,
                status = DailyPlanItemStatus.Planned.name,
                sortOrder = dao.nextDailyPlanItemSortOrder(targetEpochDays),
                startTimeMinutes = startTime,
                endTimeMinutes = endTime,
                addedAtMillis = now,
                completedAtMillis = null,
                carriedFromItemId = source.id
            )
        )
        source.tags.forEach { tag -> addDailyPlanItemTag(itemId, tag.id) }
        dao.markDailyPlanItemsHandled(listOf(source.id), now)
        dailyPlanScheduleReminderScheduler.rescheduleNext()
        return itemId
    }

    override fun observePeriodReviews(): Flow<List<PeriodReview>> =
        dao.observePeriodReviews().map { entities -> entities.map { it.toDomain() } }

    override suspend fun periodReviewFor(period: ReviewPeriod, date: LocalDate): PeriodReview? =
        dao.periodReviewFor(period.name, date.toEpochDays().toInt())?.toDomain()

    override suspend fun savePeriodReview(review: PeriodReview) {
        dao.upsertPeriodReview(
            PeriodReviewEntity(
                id = review.id,
                periodType = review.period.name,
                periodStartEpochDays = review.periodStartEpochDays,
                periodEndEpochDays = review.periodEndEpochDays,
                content = review.content,
                highlightsJson = review.highlightsJson,
                intentNext = review.intentNext,
                source = review.source.name,
                status = review.status.name,
                completedAtMillis = review.completedAtMillis,
                generatedAtMillis = review.generatedAtMillis,
                editedAtMillis = review.editedAtMillis,
                statsJson = review.statsJson
            )
        )
    }

    override suspend fun completeDayClose(
        date: LocalDate,
        markDoneItemIds: List<Long>,
        carryItemIds: List<Long>,
        dropItemIds: List<Long>,
        winNote: String?,
        tomorrowGoal: String?,
        doneCount: Int,
        plannedCount: Int,
        doneMinutes: Int,
        targetDate: LocalDate,
        nowMillis: Long
    ): DayCloseCommitResult =
        dao.completeDayClose(
            dateEpochDays = date.toEpochDays().toInt(),
            markDoneItemIds = markDoneItemIds,
            carryItemIds = carryItemIds,
            dropItemIds = dropItemIds,
            winNote = winNote,
            tomorrowGoal = tomorrowGoal,
            doneCount = doneCount,
            plannedCount = plannedCount,
            doneMinutes = doneMinutes,
            targetDateEpochDays = targetDate.toEpochDays().toInt(),
            nowMillis = nowMillis
        )

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
                title = input.title,
                content = input.content,
                status = input.status.name,
                dateEpochDays = input.date?.toEpochDays()?.toInt(),
                startTimeMinutes = input.startTimeMinutes,
                createdAtMillis = now,
                editedAtMillis = now,
                sortOrder = dao.nextNoteSortOrder(input.objectiveId)
            )
        )
        dao.insertNoteObjective(NoteObjectiveEntity(noteId, input.objectiveId))
        input.tagIds.forEach { tagId -> addNoteTag(noteId, tagId) }
        return noteId
    }

    override suspend fun updateNote(noteId: Long, input: NoteWriteInput) {
        dao.updateNote(
            noteId = noteId,
            title = input.title,
            content = input.content,
            status = input.status.name,
            dateEpochDays = input.date?.toEpochDays()?.toInt(),
            startTimeMinutes = input.startTimeMinutes,
            editedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        dao.deleteNoteObjective(noteId)
        dao.insertNoteObjective(NoteObjectiveEntity(noteId, input.objectiveId))
        dao.deleteNoteTags(noteId)
        input.tagIds.forEach { tagId -> addNoteTag(noteId, tagId) }
    }

    override suspend fun trashNote(noteId: Long) {
        dao.trashNote(noteId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun restoreNote(noteId: Long) {
        dao.restoreNote(noteId, Clock.System.now().toEpochMilliseconds())
    }

    private suspend fun addTaskTag(taskId: Long, tagId: Long) {
        dao.insertTaskTagIfParentsExist(taskId, tagId)
        dao.updateTagLastUsedAtMillis(tagId, Clock.System.now().toEpochMilliseconds())
    }

    private suspend fun addNoteTag(noteId: Long, tagId: Long) {
        dao.insertNoteTagIfParentsExist(noteId, tagId)
        dao.updateTagLastUsedAtMillis(tagId, Clock.System.now().toEpochMilliseconds())
    }

    private suspend fun addDailyPlanItemTag(itemId: Long, tagId: Long) {
        dao.insertDailyPlanItemTagIfParentsExist(itemId, tagId)
        dao.updateTagLastUsedAtMillis(tagId, Clock.System.now().toEpochMilliseconds())
    }

    private suspend fun addJournalEntryTag(entryId: Long, tagId: Long) {
        dao.insertJournalEntryTagIfParentsExist(entryId, tagId)
        dao.updateTagLastUsedAtMillis(tagId, Clock.System.now().toEpochMilliseconds())
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
}

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
    val tags: List<TagEntity>,
    val taskObjectives: List<TaskObjectiveEntity>,
    val taskKeyResults: List<TaskKeyResultEntity>,
    val noteObjectives: List<NoteObjectiveEntity>
)

private data class TaskBoardMetadata(
    val keyResults: List<KeyResultEntity>,
    val tags: List<TagEntity>,
    val taskObjectives: List<TaskObjectiveEntity>,
    val taskKeyResults: List<TaskKeyResultEntity>,
    val noteObjectives: List<NoteObjectiveEntity>
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

private fun TagEntity.toDomain() = TagItem(
    id = id,
    name = name,
    color = color,
    sortOrder = sortOrder,
    lastUsedAtMillis = lastUsedAtMillis
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
    type != input.type.name ||
        doDateEpochDays != input.doDate?.toEpochDays()?.toInt() ||
        startTimeMinutes != input.startTimeMinutes ||
        endTimeMinutes != input.endTimeMinutes

private fun TaskEntity.toDomain(
    objective: Objective,
    keyResult: KeyResult?,
    subtasks: List<SubTaskItem>,
    reminders: List<TaskReminder>,
    tags: List<TagItem>
) = TaskItem(
    id = id,
    objective = objective,
    keyResult = keyResult,
    name = name,
    description = description,
    subtasks = subtasks,
    status = enumValueOf(status),
    type = enumValueOf(type),
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

private fun DailyPlanItemEntity.toDomain(tags: List<TagItem> = emptyList()) = DailyPlanItem(
    id = id,
    dateEpochDays = dateEpochDays,
    taskId = taskId,
    title = title,
    note = note,
    source = enumValueOf(source),
    status = enumValueOf(status),
    tags = tags,
    isHabit = isHabit,
    sortOrder = sortOrder,
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = endTimeMinutes,
    addedAtMillis = addedAtMillis,
    completedAtMillis = completedAtMillis,
    carriedFromItemId = carriedFromItemId,
    handledAtMillis = handledAtMillis
)

private fun PeriodReviewEntity.toDomain() = PeriodReview(
    id = id,
    period = ReviewPeriod.valueOf(periodType),
    periodStartEpochDays = periodStartEpochDays,
    periodEndEpochDays = periodEndEpochDays,
    content = content,
    highlightsJson = highlightsJson,
    intentNext = intentNext,
    source = ReviewSource.valueOf(source),
    status = ReviewStatus.valueOf(status),
    completedAtMillis = completedAtMillis,
    generatedAtMillis = generatedAtMillis,
    editedAtMillis = editedAtMillis,
    statsJson = statsJson
)

private fun JournalEntryEntity.toDomain(tags: List<TagItem> = emptyList()) = JournalEntry(
    id = id,
    dateEpochDays = dateEpochDays,
    context = context,
    content = content,
    moods = moods.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    tags = tags,
    createdTimeMinutes = createdTimeMinutes,
    attachments = attachments.split(",").map { it.trim() }.filter { it.isNotEmpty() }
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

private fun NoteEntity.toDomain(objective: Objective, tags: List<TagItem>) = NoteItem(
    id = id,
    objective = objective,
    title = title,
    content = content,
    status = enumValueOf(status),
    tags = tags,
    date = dateEpochDays?.let { LocalDate.fromEpochDays(it) },
    startTimeMinutes = startTimeMinutes,
    createdAtMillis = createdAtMillis,
    editedAtMillis = editedAtMillis,
    sortOrder = sortOrder,
    trashedAtMillis = trashedAtMillis
)
