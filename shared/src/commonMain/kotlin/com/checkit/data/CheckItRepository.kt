package com.checkit.data

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayCloseCommitResult
import com.checkit.domain.PeriodReview
import com.checkit.domain.Period
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import com.checkit.domain.DueDatePreset
import com.checkit.domain.JournalEntry
import com.checkit.domain.ListItem
import com.checkit.domain.ListSection
import com.checkit.domain.NoteItem
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemMove
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.NestedColorToken
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.NestedManualMetric
import com.checkit.domain.NestedMetricUnit
import com.checkit.domain.buildNestedTree
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
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
    fun observeTaskBoard(onlyOpen: Boolean = true): Flow<TaskBoard>
    fun observeDailyPlans(startDate: LocalDate? = null, endDate: LocalDate? = null): Flow<List<DailyPlan>>
    fun observeJournalEntries(): Flow<List<JournalEntry>>
    suspend fun addJournalEntry(input: JournalEntryWriteInput): Long
    suspend fun updateJournalEntry(entryId: Long, input: JournalEntryWriteInput)
    suspend fun deleteJournalEntry(entryId: Long)
    suspend fun addList(input: ListWriteInput): Long
    suspend fun updateList(listId: Long, input: ListWriteInput)
    suspend fun deleteList(listId: Long)
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
        status: DailyPlanItemStatus = DailyPlanItemStatus.Planned,
        tagIds: List<Long> = emptyList(),
        label: String? = null,
        taskId: Long? = null,
        nestedListItemId: Long? = null,
        carriedFromItemId: Long? = null
    ): Long
    suspend fun updateDailyPlanItemTime(itemId: Long, startTimeMinutes: Int?, endTimeMinutes: Int?)
    suspend fun updateDailyPlanItemTimes(updates: List<DailyPlanItemTimeUpdate>)
    suspend fun updateDailyPlanItemStatus(itemId: Long, status: DailyPlanItemStatus)
    suspend fun updateDailyPlanItemsStatus(itemIds: List<Long>, status: DailyPlanItemStatus)
    suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput)
    suspend fun updateDailyPlanItemTags(itemId: Long, tagIds: List<Long>)
    suspend fun deleteDailyPlanItem(itemId: Long)
    suspend fun getDailyPlanItem(itemId: Long): DailyPlanItem?
    suspend fun dailyPlanForDate(date: LocalDate): DailyPlan?
    fun observePeriodReviews(): Flow<List<PeriodReview>>
    suspend fun periodReviewFor(period: Period, date: LocalDate): PeriodReview?
    suspend fun savePeriodReview(review: PeriodReview)
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
    suspend fun copyDailyPlanItemToDate(source: DailyPlanItem, targetDate: LocalDate, clearTimes: Boolean): Long?
    suspend fun countDoneDailyPlanItemsForTaskOnDate(taskId: Long, dateEpochDays: Int, excludeItemId: Long): Int
    suspend fun addNote(input: NoteWriteInput): Long
    suspend fun updateNote(noteId: Long, input: NoteWriteInput)
    suspend fun completeNote(noteId: Long)
    suspend fun openNote(noteId: Long)
    suspend fun trashNote(noteId: Long)
    suspend fun restoreNote(noteId: Long)
    suspend fun addSection(listId: Long, title: String, color: String): Long
    suspend fun updateSection(sectionId: Long, title: String, color: String, sortOrder: Int)
    suspend fun deleteSection(sectionId: Long)
    fun observeNestedDocuments(): Flow<List<NestedDocument>>
    fun observeTags(): Flow<List<TagItem>>
    fun observeNestedDocumentTree(documentId: Long): Flow<NestedDocumentTree>
    suspend fun addNestedDocument(title: String): Long
    suspend fun renameNestedDocument(documentId: Long, title: String)
    suspend fun deleteNestedDocument(documentId: Long)
    suspend fun addNestedItem(documentId: Long, parentId: Long?, text: String, position: Int?): Long
    suspend fun updateNestedItemText(itemId: Long, text: String)
    suspend fun updateNestedItemNote(itemId: Long, note: String?)
    suspend fun updateNestedItemFormatting(itemId: Long, textStyle: NestedTextStyle, textColor: NestedColorToken, backgroundColor: NestedColorToken)
    suspend fun updateNestedItemPriority(itemId: Long, priority: TaskPriority)
    suspend fun updateNestedItemDateRange(itemId: Long, startDate: LocalDate?, endDate: LocalDate?)
    suspend fun updateNestedItemTags(itemId: Long, tagIds: List<Long>)
    suspend fun updateNestedItemMetricSettings(itemId: Long, actualMinutes: Int, metricRollupPolicy: MetricRollupPolicy, showTrackedMinutes: Boolean)
    suspend fun replaceNestedManualMetrics(itemId: Long, metrics: List<NestedManualMetric>)
    suspend fun setNestedItemCheckboxEnabled(itemId: Long, checkboxEnabled: Boolean)
    suspend fun setNestedItemsChecked(itemIds: List<Long>, checked: Boolean)
    suspend fun toggleNestedItemCollapsed(itemId: Long)
    suspend fun moveNestedItems(moves: List<NestedItemMove>)
    suspend fun deleteNestedItems(itemIds: List<Long>)
}

data class DailyPlanItemTimeUpdate(
    val itemId: Long,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?
)

data class ListWriteInput(
    val title: String,
    val color: String,
    val icon: String
)

data class TagWriteInput(
    val name: String,
    val color: String
)

data class TaskWriteInput(
    val listId: Long? = null,
    val sectionId: Long? = null,
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
    val label: String? = null,
    val isPinned: Boolean = false,
    val reminders: List<TaskReminderWriteInput>,
    val tagIds: List<Long>
)

data class SubTaskWriteInput(
    val name: String,
    val isCompleted: Boolean
)

data class NoteWriteInput(
    val listId: Long? = null,
    val sectionId: Long? = null,
    val title: String,
    val content: String,
    val status: TaskStatus,
    val date: LocalDate?,
    val startTimeMinutes: Int?,
    val label: String? = null,
    val isPinned: Boolean = false,
    val tagIds: List<Long>
)

data class DailyPlanItemWriteInput(
    val title: String,
    val note: String?,
    val source: DailyPlanItemSource,
    val status: DailyPlanItemStatus,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val tagIds: List<Long>,
    val label: String? = null,
    val nestedListItemId: Long? = null
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

    private val dailyPlanItemCache = mutableMapOf<Long, DailyPlanItem>()
    private val dailyPlanCache = mutableMapOf<LocalDate, DailyPlan>()
    private val taskItemCache = mutableMapOf<Long, TaskItem>()
    private val noteItemCache = mutableMapOf<Long, NoteItem>()

    override fun observeTaskBoard(onlyOpen: Boolean): Flow<TaskBoard> {
        val tasksFlow = if (onlyOpen) dao.observeTasksOpen() else dao.observeTasksAll()
        val notesFlow = if (onlyOpen) dao.observeNotesOpen() else dao.observeNotesAll()

        val rowsFlow = combine(
            dao.observeFilters(),
            tasksFlow,
            notesFlow
        ) { filters, tasks, notes ->
            TaskBoardRows(filters, tasks, notes)
        }

        val joinsFlow = combine(
            dao.observeSubTasks(),
            dao.observeReminders(),
            dao.observeTaskTags(),
            dao.observeNoteTags()
        ) { subTasks, reminders, taskTags, noteTags ->
            TaskBoardJoins(subTasks, reminders, taskTags, noteTags)
        }

        val metadataFlow = combine(
            dao.observeTags(),
            dao.observeLists(),
            dao.observeListSections(),
            dao.observeTaskLists(),
            dao.observeNoteLists()
        ) { array ->
            @Suppress("UNCHECKED_CAST")
            TaskBoardMetadata(
                tags = array[0] as List<TagEntity>,
                lists = array[1] as List<ListEntity>,
                sections = array[2] as List<ListSectionEntity>,
                taskLists = array[3] as List<TaskListEntity>,
                noteLists = array[4] as List<NoteListEntity>
            )
        }

        return combine(rowsFlow, joinsFlow, metadataFlow) { rows, joins, metadata ->
            val domainTags = metadata.tags.map { it.toDomain() }
            val domainSections = metadata.sections.map { it.toDomain() }
            val sectionsByList = domainSections.groupBy { it.listId }
            val domainLists = metadata.lists.map { it.toDomain(sectionsByList[it.id].orEmpty()) }
            val tagsById = domainTags.associateBy { it.id }
            val listsById = domainLists.associateBy { it.id }
            val taskTagIds = joins.taskTags.groupBy { it.taskId }.mapValues { entry -> entry.value.map { it.tagId } }
            val noteTagIds = joins.noteTags.groupBy { it.noteId }.mapValues { entry -> entry.value.map { it.tagId } }
            val subTasksByTask = joins.subTasks.groupBy { it.taskId }
            val remindersByTask = joins.reminders.groupBy { it.taskId }

            val taskListMap = metadata.taskLists.associateBy { it.taskId }
            val noteListMap = metadata.noteLists.associateBy { it.noteId }

            TaskBoard(
                lists = domainLists,
                filters = rows.filters.map { it.toDomain() },
                tasks = rows.tasks.map { entity ->
                    val listJoin = taskListMap[entity.id]
                    val list = listJoin?.listId?.let { listsById[it] }
                    val subtasks = subTasksByTask[entity.id].orEmpty().map { it.toDomain() }
                    val reminders = remindersByTask[entity.id].orEmpty().map { it.toDomain() }
                    val itemTags = taskTagIds[entity.id].orEmpty().mapNotNull { tagsById[it] }
                    val listSortOrder = listJoin?.sortOrder ?: 0
                    val isPinned = listJoin?.isPinned ?: false
                    val sectionId = listJoin?.sectionId

                    val cached = taskItemCache[entity.id]
                    if (cached != null && cached.isSameAs(
                            name = entity.name,
                            description = entity.description,
                            statusName = entity.status,
                            priorityName = entity.priority,
                            typeName = entity.type,
                            doDateEpochDays = entity.doDateEpochDays,
                            completedDateEpochDays = entity.completedDateEpochDays,
                            startTimeMinutes = entity.startTimeMinutes,
                            endTimeMinutes = entity.endTimeMinutes,
                            repeatRRule = entity.repeatRRule,
                            label = entity.label,
                            createdAtMillis = entity.createdAtMillis,
                            updatedAtMillis = entity.updatedAtMillis,
                            trashedAtMillis = entity.trashedAtMillis,
                            resolvedListId = list?.id,
                            resolvedSubtasks = subtasks,
                            resolvedReminders = reminders,
                            resolvedTags = itemTags,
                            resolvedSortOrder = listSortOrder,
                            resolvedIsPinned = isPinned,
                            resolvedSectionId = sectionId
                        )
                    ) {
                        cached
                    } else {
                        val newItem = entity.toDomain(
                            list = list,
                            subtasks = subtasks,
                            reminders = reminders,
                            tags = itemTags,
                            listSortOrder = listSortOrder,
                            isPinned = isPinned,
                            sectionId = sectionId
                        )
                        taskItemCache[entity.id] = newItem
                        newItem
                    }
                },
                notes = rows.notes.map { entity ->
                    val listJoin = noteListMap[entity.id]
                    val list = listJoin?.listId?.let { listsById[it] }
                    val itemTags = noteTagIds[entity.id].orEmpty().mapNotNull { tagsById[it] }
                    val listSortOrder = listJoin?.sortOrder ?: 0
                    val isPinned = listJoin?.isPinned ?: false
                    val sectionId = listJoin?.sectionId

                    val cached = noteItemCache[entity.id]
                    if (cached != null && cached.isSameAs(
                            title = entity.title,
                            content = entity.content,
                            statusName = entity.status,
                            dateEpochDays = entity.dateEpochDays,
                            startTimeMinutes = entity.startTimeMinutes,
                            createdAtMillis = entity.createdAtMillis,
                            editedAtMillis = entity.editedAtMillis,
                            label = entity.label,
                            trashedAtMillis = entity.trashedAtMillis,
                            resolvedListId = list?.id,
                            resolvedTags = itemTags,
                            resolvedSortOrder = listSortOrder,
                            resolvedIsPinned = isPinned,
                            resolvedSectionId = sectionId
                        )
                    ) {
                        cached
                    } else {
                        val newItem = entity.toDomain(
                            list = list,
                            tags = itemTags,
                            listSortOrder = listSortOrder,
                            isPinned = isPinned,
                            sectionId = sectionId
                        )
                        noteItemCache[entity.id] = newItem
                        newItem
                    }
                },
                tags = domainTags
            )
        }
    }

    override fun observeDailyPlans(startDate: LocalDate?, endDate: LocalDate?): Flow<List<DailyPlan>> {
        val itemsFlow = if (startDate != null && endDate != null) {
            dao.observeDailyPlanItemsInRange(startDate.toEpochDays().toInt(), endDate.toEpochDays().toInt())
        } else {
            dao.observeDailyPlanItems()
        }
        return combine(
            itemsFlow,
            dao.observeDailyPlanItemTags(),
            dao.observeTags()
        ) { items, itemTags, tags ->
            val domainTags = tags.map { it.toDomain() }
            val tagsById = domainTags.associateBy { it.id }
            val itemTagIds = itemTags.groupBy { it.itemId }.mapValues { it.value.map { it.tagId } }

            items.groupBy { it.dateEpochDays }
                .map { (dateEpochDays, itemEntities) ->
                    val date = LocalDate.fromEpochDays(dateEpochDays)
                    val domainItems = itemEntities.map { entity ->
                        val itemTagsList = itemTagIds[entity.id].orEmpty().mapNotNull { tagsById[it] }
                        val cached = dailyPlanItemCache[entity.id]
                        if (cached != null && cached.isSameAs(
                                dateEpochDays = entity.dateEpochDays,
                                taskId = entity.taskId,
                                nestedListItemId = entity.nestedListItemId,
                                title = entity.title,
                                note = entity.note,
                                sourceName = entity.source,
                                statusName = entity.status,
                                label = entity.label,
                                sortOrder = entity.sortOrder,
                                startTimeMinutes = entity.startTimeMinutes,
                                endTimeMinutes = entity.endTimeMinutes,
                                isHabit = entity.isHabit,
                                addedAtMillis = entity.addedAtMillis,
                                completedAtMillis = entity.completedAtMillis,
                                carriedFromItemId = entity.carriedFromItemId,
                                handledAtMillis = entity.handledAtMillis,
                                resolvedTags = itemTagsList
                            )
                        ) {
                            cached
                        } else {
                            val newItem = entity.toDomain(itemTagsList)
                            dailyPlanItemCache[entity.id] = newItem
                            newItem
                        }
                    }.sortedWith(compareBy<DailyPlanItem> { it.startTimeMinutes }.thenBy { it.sortOrder })

                    val cachedPlan = dailyPlanCache[date]
                    if (cachedPlan != null && cachedPlan.items == domainItems) {
                        cachedPlan
                    } else {
                        val newPlan = DailyPlan(date = date, items = domainItems)
                        dailyPlanCache[date] = newPlan
                        newPlan
                    }
                }
                .sortedByDescending { it.date }
        }
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

    override suspend fun addList(input: ListWriteInput): Long =
        dao.insertList(
            ListEntity(
                title = input.title,
                color = input.color,
                icon = input.icon,
                sortOrder = dao.nextListSortOrder()
            )
        )

    override suspend fun updateList(listId: Long, input: ListWriteInput) {
        dao.updateList(listId = listId, title = input.title, icon = input.icon, color = input.color)
    }

    override suspend fun deleteList(listId: Long) {
        val inboxId = dao.inboxListId() ?: return
        if (listId == inboxId) return
        dao.deleteListMovingContents(
            listId = listId,
            targetListId = inboxId
        )
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
        val isTask = input.type == TaskType.Task
        val taskId = dao.insertTask(
            TaskEntity(
                name = input.name,
                description = input.description,
                status = input.status.name,
                priority = input.priority.name,
                type = input.type.name,
                doDateEpochDays = if (isTask) input.doDate?.toEpochDays()?.toInt() else null,
                startTimeMinutes = if (isTask) input.startTimeMinutes else null,
                endTimeMinutes = if (isTask) input.endTimeMinutes else null,
                repeatRRule = if (isTask) input.repeatRRule else null,
                label = input.label,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        input.listId?.let { listId ->
            dao.insertTaskList(
                TaskListEntity(
                    taskId = taskId,
                    listId = listId,
                    isPinned = input.isPinned,
                    sortOrder = dao.nextTaskSortOrder(listId),
                    sectionId = input.sectionId
                )
            )
        }
        input.tagIds.forEach { tagId -> addTaskTag(taskId, tagId) }
        dao.replaceTaskSubTasks(taskId, input.subtasks)
        dao.replaceTaskReminders(taskId, input.reminders)
        scheduleTaskReminders(taskId, input)
        return taskId
    }

    override suspend fun updateTask(taskId: Long, input: TaskWriteInput) {
        val existingTask = dao.taskById(taskId)
        val shouldRemoveOpenDailyPlanItems = existingTask?.hasDifferentScheduleThan(input) == true
        val isTask = input.type == TaskType.Task
        dao.updateTask(
            taskId = taskId,
            name = input.name,
            description = input.description,
            status = input.status.name,
            priority = input.priority.name,
            type = input.type.name,
            doDateEpochDays = if (isTask) input.doDate?.toEpochDays()?.toInt() else null,
            startTimeMinutes = if (isTask) input.startTimeMinutes else null,
            endTimeMinutes = if (isTask) input.endTimeMinutes else null,
            repeatRRule = if (isTask) input.repeatRRule else null,
            label = input.label,
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        dao.deleteTaskList(taskId)
        input.listId?.let { listId ->
            dao.insertTaskList(
                TaskListEntity(
                    taskId = taskId,
                    listId = listId,
                    isPinned = input.isPinned,
                    sortOrder = dao.nextTaskSortOrder(listId),
                    sectionId = input.sectionId
                )
            )
        }
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
                label = task.label,
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
        tagIds: List<Long>,
        label: String?,
        taskId: Long?,
        nestedListItemId: Long?,
        carriedFromItemId: Long?
    ): Long {
        val dateEpochDays = date.toEpochDays().toInt()
        val now = Clock.System.now().toEpochMilliseconds()
        val itemId = dao.insertDailyPlanItem(
            DailyPlanItemEntity(
                dateEpochDays = dateEpochDays,
                taskId = taskId,
                nestedListItemId = nestedListItemId,
                title = title.trim(),
                note = note?.trim()?.takeIf { it.isNotBlank() },
                source = source.name,
                status = status.name,
                sortOrder = dao.nextDailyPlanItemSortOrder(dateEpochDays),
                label = label,
                startTimeMinutes = startTimeMinutes,
                endTimeMinutes = if (source.hasEndTime()) endTimeMinutes else null,
                addedAtMillis = now,
                completedAtMillis = if (status == DailyPlanItemStatus.Done) now else null,
                carriedFromItemId = carriedFromItemId
            )
        )
        tagIds.forEach { tagId -> addDailyPlanItemTag(itemId, tagId) }
        dailyPlanScheduleReminderScheduler.rescheduleNext()
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
        val now = Clock.System.now().toEpochMilliseconds()
        dao.updateDailyPlanItemStatusWithMinutes(
            itemId = itemId,
            status = status.name,
            completedAtMillis = if (status == DailyPlanItemStatus.Done) now else null,
            nowMillis = now
        )
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun updateDailyPlanItemsStatus(
        itemIds: List<Long>,
        status: DailyPlanItemStatus
    ) {
        if (itemIds.isEmpty()) return
        val now = Clock.System.now().toEpochMilliseconds()
        
        itemIds.forEach { itemId ->
            dao.updateDailyPlanItemStatusWithMinutes(
                itemId = itemId,
                status = status.name,
                completedAtMillis = if (status == DailyPlanItemStatus.Done) now else null,
                nowMillis = now
            )
        }
        
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.updateDailyPlanItemWithTags(
            itemId = itemId,
            title = input.title,
            note = input.note,
            source = input.source.name,
            status = input.status.name,
            label = input.label,
            startTimeMinutes = input.startTimeMinutes,
            endTimeMinutes = if (input.source.hasEndTime()) input.endTimeMinutes else null,
            completedAtMillis = if (input.status == DailyPlanItemStatus.Done) now else null,
            nestedListItemId = input.nestedListItemId,
            tagIds = input.tagIds,
            nowMillis = now
        )
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun updateDailyPlanItemTags(itemId: Long, tagIds: List<Long>) {
        dao.deleteDailyPlanItemTags(itemId)
        tagIds.forEach { tagId -> addDailyPlanItemTag(itemId, tagId) }
    }

    override suspend fun deleteDailyPlanItem(itemId: Long) {
        val oldEntity = dao.dailyPlanItemById(itemId)
        if (oldEntity?.nestedListItemId != null && oldEntity.status == DailyPlanItemStatus.Done.name) {
            val minutes = oldEntity.toDomain().workMinutes()
            if (minutes > 0) {
                dao.updateNestedItemActualMinutesDelta(
                    itemId = oldEntity.nestedListItemId,
                    delta = -minutes,
                    updatedAtMillis = Clock.System.now().toEpochMilliseconds()
                )
            }
        }
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
                nestedListItemId = source.nestedListItemId,
                title = source.title.ifBlank { "Untitled" },
                note = source.note,
                source = source.source.name,
                status = DailyPlanItemStatus.Planned.name,
                sortOrder = dao.nextDailyPlanItemSortOrder(targetEpochDays),
                label = source.label,
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

    override suspend fun periodReviewFor(period: Period, date: LocalDate): PeriodReview? =
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
                label = input.label
            )
        )
        input.listId?.let { listId ->
            dao.insertNoteList(
                NoteListEntity(
                    noteId = noteId,
                    listId = listId,
                    isPinned = input.isPinned,
                    sortOrder = dao.nextNoteSortOrder(listId),
                    sectionId = input.sectionId
                )
            )
        }
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
            label = input.label,
            editedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        dao.deleteNoteList(noteId)
        input.listId?.let { listId ->
            dao.insertNoteList(
                NoteListEntity(
                    noteId = noteId,
                    listId = listId,
                    isPinned = input.isPinned,
                    sortOrder = dao.nextNoteSortOrder(listId),
                    sectionId = input.sectionId
                )
            )
        }
        dao.deleteNoteTags(noteId)
        input.tagIds.forEach { tagId -> addNoteTag(noteId, tagId) }
    }

    override suspend fun trashNote(noteId: Long) {
        dao.trashNote(noteId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun restoreNote(noteId: Long) {
        dao.restoreNote(noteId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun addSection(listId: Long, title: String, color: String): Long {
        return dao.insertListSection(
            ListSectionEntity(
                listId = listId,
                title = title.trim(),
                color = color,
                sortOrder = dao.nextSectionSortOrder(listId)
            )
        )
    }

    override suspend fun updateSection(sectionId: Long, title: String, color: String, sortOrder: Int) {
        dao.updateSection(sectionId, title.trim(), color, sortOrder)
    }

    override suspend fun deleteSection(sectionId: Long) {
        dao.deleteSection(sectionId)
    }

    // ---------------- Nested Documents ----------------

    override fun observeNestedDocuments(): Flow<List<NestedDocument>> =
        dao.observeNestedDocuments().map { entities ->
            entities.map { entity ->
                NestedDocument(
                    id = entity.id,
                    title = entity.title,
                    createdAtMillis = entity.createdAtMillis,
                    updatedAtMillis = entity.updatedAtMillis
                )
            }
        }

    override fun observeTags(): Flow<List<TagItem>> = dao.observeTags().map { tags -> tags.map { it.toDomain() } }

    override fun observeNestedDocumentTree(documentId: Long): Flow<NestedDocumentTree> =
        combine(
            dao.observeNestedDocuments(),
            dao.observeNestedItems(documentId),
            dao.observeNestedItemTags(documentId),
            dao.observeNestedManualMetrics(documentId)
        ) { documents, items, links, metricEntities ->
            val tagsById = links.map { it.tagId }.distinct()
                .takeIf { it.isNotEmpty() }
                ?.let { dao.tagsByIds(it).associateBy(TagEntity::id) }
                .orEmpty()
            val tagsByItemId = links.groupBy(NestedItemTagEntity::itemId)
            val metricsByItemId = metricEntities.groupBy(NestedManualMetricEntity::itemId)
            NestedDocumentTree(
                document = documents.firstOrNull { it.id == documentId }
                    ?.let { NestedDocument(it.id, it.title, it.createdAtMillis, it.updatedAtMillis) }
                    ?: NestedDocument(id = documentId, title = "", createdAtMillis = 0L, updatedAtMillis = 0L),
                rootNodes = buildNestedTree(items.map { item ->
                    item.toNestedListItem(
                        tags = tagsByItemId[item.id].orEmpty().mapNotNull { tagsById[it.tagId]?.toDomain() },
                        manualMetrics = metricsByItemId[item.id].orEmpty().map { it.toDomain() }
                    )
                })
            )
        }

    override suspend fun addNestedDocument(title: String): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val trimmed = title.trim()
        return dao.insertNestedDocument(
            NestedDocumentEntity(
                title = trimmed,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    override suspend fun renameNestedDocument(documentId: Long, title: String) {
        dao.updateNestedDocumentTitle(documentId, title.trim(), Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun deleteNestedDocument(documentId: Long) {
        dao.deleteNestedDocument(documentId)
    }

    override suspend fun addNestedItem(documentId: Long, parentId: Long?, text: String, position: Int?): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return dao.insertNestedListItem(
            NestedListItemEntity(
                documentId = documentId,
                parentId = parentId,
                position = position ?: dao.nextNestedItemPosition(documentId, parentId),
                text = text.trim(),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    override suspend fun updateNestedItemText(itemId: Long, text: String) {
        dao.updateNestedItemText(itemId, text.trim(), Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun updateNestedItemNote(itemId: Long, note: String?) {
        dao.updateNestedItemNote(
            itemId,
            note?.trim()?.takeIf { it.isNotBlank() },
            Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun updateNestedItemFormatting(
        itemId: Long,
        textStyle: NestedTextStyle,
        textColor: NestedColorToken,
        backgroundColor: NestedColorToken
    ) {
        dao.updateNestedItemFormatting(itemId, textStyle.name, textColor.name, backgroundColor.name, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun updateNestedItemPriority(itemId: Long, priority: TaskPriority) {
        dao.updateNestedItemPriority(itemId, priority.name, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun updateNestedItemDateRange(itemId: Long, startDate: LocalDate?, endDate: LocalDate?) {
        dao.updateNestedItemDateRange(
            itemId,
            startDate?.toEpochDays()?.toInt(),
            endDate?.toEpochDays()?.toInt(),
            Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun updateNestedItemMetricSettings(
        itemId: Long,
        actualMinutes: Int,
        metricRollupPolicy: MetricRollupPolicy,
        showTrackedMinutes: Boolean
    ) {
        dao.updateNestedItemActualMinutes(itemId, actualMinutes.coerceAtLeast(0), Clock.System.now().toEpochMilliseconds())
        dao.updateNestedItemMetricSettings(
            itemId,
            metricRollupPolicy.name,
            showTrackedMinutes,
            Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun replaceNestedManualMetrics(itemId: Long, metrics: List<NestedManualMetric>) {
        dao.replaceNestedManualMetrics(itemId, metrics.map { it.toEntity(itemId) })
    }

    override suspend fun updateNestedItemTags(itemId: Long, tagIds: List<Long>) {
        dao.replaceNestedItemTags(itemId, tagIds)
        val now = Clock.System.now().toEpochMilliseconds()
        tagIds.distinct().forEach { dao.updateTagLastUsedAtMillis(it, now) }
    }

    override suspend fun setNestedItemCheckboxEnabled(itemId: Long, checkboxEnabled: Boolean) {
        dao.setNestedItemCheckboxEnabled(itemId, checkboxEnabled)
    }

    override suspend fun setNestedItemsChecked(itemIds: List<Long>, checked: Boolean) {
        if (itemIds.isEmpty()) return
        dao.setNestedItemsChecked(itemIds, checked)
    }

    override suspend fun toggleNestedItemCollapsed(itemId: Long) {
        dao.toggleNestedItemCollapsed(itemId)
    }

    override suspend fun moveNestedItems(moves: List<NestedItemMove>) {
        if (moves.isEmpty()) return
        val now = Clock.System.now().toEpochMilliseconds()
        dao.applyNestedMoves(moves.map { move ->
            NestedMoveRow(
                itemId = move.itemId,
                parentId = move.parentId,
                position = move.position,
                updatedAtMillis = now
            )
        })
    }

    override suspend fun deleteNestedItems(itemIds: List<Long>) {
        if (itemIds.isEmpty()) return
        dao.deleteNestedItems(itemIds)
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
}

private data class TaskBoardRows(
    val filters: List<TaskFilterEntity>,
    val tasks: List<TaskEntity>,
    val notes: List<NoteEntity>
)

private data class TaskBoardJoins(
    val subTasks: List<SubTaskEntity>,
    val reminders: List<TaskReminderEntity>,
    val taskTags: List<TaskTagEntity>,
    val noteTags: List<NoteTagEntity>
)

private data class TaskBoardMetadata(
    val tags: List<TagEntity>,
    val lists: List<ListEntity>,
    val sections: List<ListSectionEntity>,
    val taskLists: List<TaskListEntity>,
    val noteLists: List<NoteListEntity>
)

private fun ListEntity.toDomain(sections: List<ListSection> = emptyList()) = ListItem(
    id = id,
    title = title,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
    isArchived = isArchived,
    sections = sections
)

private fun ListSectionEntity.toDomain() = ListSection(
    id = id,
    listId = listId,
    title = title,
    color = color,
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
    list: ListItem?,
    subtasks: List<SubTaskItem>,
    reminders: List<TaskReminder>,
    tags: List<TagItem>,
    listSortOrder: Int,
    isPinned: Boolean,
    sectionId: Long?
) = TaskItem(
    id = id,
    list = list,
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
    label = label,
    sortOrder = listSortOrder,
    isPinned = isPinned,
    sectionId = sectionId,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    trashedAtMillis = trashedAtMillis
)

private fun DailyPlanItemEntity.toDomain(tags: List<TagItem> = emptyList()) = DailyPlanItem(
    id = id,
    dateEpochDays = dateEpochDays,
    taskId = taskId,
    nestedListItemId = nestedListItemId,
    title = title,
    note = note,
    source = enumValueOf(source),
    status = enumValueOf(status),
    tags = tags,
    label = label,
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
    period = Period.valueOf(periodType),
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

private fun NoteEntity.toDomain(
    list: ListItem?,
    tags: List<TagItem>,
    listSortOrder: Int,
    isPinned: Boolean,
    sectionId: Long?
) = NoteItem(
    id = id,
    list = list,
    title = title,
    content = content,
    status = enumValueOf(status),
    tags = tags,
    date = dateEpochDays?.let { LocalDate.fromEpochDays(it) },
    startTimeMinutes = startTimeMinutes,
    label = label,
    createdAtMillis = createdAtMillis,
    editedAtMillis = editedAtMillis,
    sortOrder = listSortOrder,
    isPinned = isPinned,
    sectionId = sectionId,
    trashedAtMillis = trashedAtMillis
)

private fun NestedListItemEntity.toNestedListItem(
    tags: List<TagItem> = emptyList(),
    manualMetrics: List<NestedManualMetric> = emptyList()
) = NestedListItem(
    id = id,
    documentId = documentId,
    parentId = parentId,
    position = position,
    text = text,
    note = note,
    checkboxEnabled = checkboxEnabled,
    checked = checked,
    collapsed = collapsed,
    textStyle = runCatching { com.checkit.domain.NestedTextStyle.valueOf(textStyle) }.getOrDefault(com.checkit.domain.NestedTextStyle.Body),
    textColor = runCatching { com.checkit.domain.NestedColorToken.valueOf(textColor) }.getOrDefault(com.checkit.domain.NestedColorToken.Default),
    backgroundColor = runCatching { com.checkit.domain.NestedColorToken.valueOf(backgroundColor) }.getOrDefault(com.checkit.domain.NestedColorToken.Default),
    startDate = startDateEpochDays?.let { LocalDate.fromEpochDays(it) },
    endDate = endDateEpochDays?.let { LocalDate.fromEpochDays(it) },
    priority = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.None),
    tags = tags,
    actualMinutes = actualMinutes,
    metricRollupPolicy = runCatching { MetricRollupPolicy.valueOf(metricRollupPolicy) }
        .getOrDefault(MetricRollupPolicy.IncludeChildren),
    showTrackedMinutes = showTrackedMinutes,
    manualMetrics = manualMetrics,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

private fun NestedManualMetricEntity.toDomain() = NestedManualMetric(
    id = id,
    itemId = itemId,
    name = name,
    value = value,
    targetValue = targetValue,
    unit = runCatching { NestedMetricUnit.valueOf(unit) }.getOrDefault(NestedMetricUnit.None),
    customUnit = customUnit,
    sortOrder = sortOrder,
    enabled = enabled
)

private fun NestedManualMetric.toEntity(itemId: Long) = NestedManualMetricEntity(
    id = id,
    itemId = itemId,
    name = name.trim(),
    value = value.trim(),
    targetValue = targetValue?.trim()?.takeIf { it.isNotEmpty() },
    unit = unit.name,
    customUnit = customUnit?.trim()?.takeIf { it.isNotEmpty() },
    sortOrder = sortOrder,
    enabled = enabled
)
