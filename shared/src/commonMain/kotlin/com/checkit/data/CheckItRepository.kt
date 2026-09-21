package com.checkit.data

import androidx.room3.RoomRawQuery
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayCloseCommitResult
import com.checkit.domain.PeriodGoal
import com.checkit.domain.PeriodGoalHistoryItem
import com.checkit.domain.Period
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.DailyTagRollup
import com.checkit.domain.DoneItemSummary
import com.checkit.domain.HabitDailyRollup
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
import com.checkit.domain.MetricItem

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid

interface CheckItRepository {
    fun observeTaskBoard(onlyOpen: Boolean = true): Flow<TaskBoard>

    /** Live per-tag usage counts (tasks, notes, daily plan items, journal entries), computed in the database. */
    fun observeTagUsageCounts(): Flow<Map<String, Int>>
    fun observeTasksForDate(date: LocalDate): Flow<List<TaskItem>>
    fun observeTasksInRange(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<TaskItem>>
    fun observeWorkingTasks(date: LocalDate): Flow<List<TaskItem>>
    fun observeNotesForDate(date: LocalDate): Flow<List<NoteItem>>
    fun observeNotesInRange(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<NoteItem>>
    fun observeDailyPlans(startDate: LocalDate? = null, endDate: LocalDate? = null): Flow<List<DailyPlan>>
    fun observeJournalEntries(): Flow<List<JournalEntry>>
    suspend fun addJournalEntry(input: JournalEntryWriteInput): String
    suspend fun updateJournalEntry(entryId: String, input: JournalEntryWriteInput)
    suspend fun deleteJournalEntry(entryId: String)
    suspend fun addList(input: ListWriteInput): String
    suspend fun updateList(listId: String, input: ListWriteInput)
    suspend fun deleteList(listId: String)
    suspend fun addTag(input: TagWriteInput): String
    suspend fun updateTag(tagId: String, input: TagWriteInput)
    suspend fun updateTagSortOrder(tagId: String, sortOrder: Int)
    suspend fun deleteTag(tagId: String)
    suspend fun isTagNameTaken(name: String, excludeTagId: String? = null): Boolean
    suspend fun addTask(input: TaskWriteInput): String
    suspend fun updateTask(taskId: String, input: TaskWriteInput)
    suspend fun trashTask(taskId: String)
    suspend fun restoreTask(taskId: String)
    suspend fun completeTask(taskId: String)
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus)
    suspend fun addTaskToDailyPlan(date: LocalDate, task: TaskItem): String
    suspend fun addDailyPlanItem(
        date: LocalDate,
        title: String,
        note: String?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        source: DailyPlanItemSource = DailyPlanItemSource.MyDayTask,
        status: DailyPlanItemStatus = DailyPlanItemStatus.Planned,
        tagIds: List<String> = emptyList(),
        label: String? = null,
        taskId: String? = null,
        nestedListItemId: String? = null,
        carriedFromItemId: String? = null
    ): String
    suspend fun updateDailyPlanItemTime(itemId: String, startTimeMinutes: Int?, endTimeMinutes: Int?)
    suspend fun updateDailyPlanItemTimes(updates: List<DailyPlanItemTimeUpdate>)
    suspend fun updateDailyPlanItemStatus(itemId: String, status: DailyPlanItemStatus)
    suspend fun updateDailyPlanItemsStatus(itemIds: List<String>, status: DailyPlanItemStatus)
    suspend fun updateDailyPlanItem(itemId: String, input: DailyPlanItemWriteInput)
    suspend fun updateDailyPlanItemTags(itemId: String, tagIds: List<String>)
    suspend fun linkDailyPlanItemToTask(itemId: String, taskId: String)
    suspend fun deleteDailyPlanItem(itemId: String)
    suspend fun getDailyPlanItem(itemId: String): DailyPlanItem?
    suspend fun dailyPlanForDate(date: LocalDate): DailyPlan?
    suspend fun getTask(taskId: String): TaskItem?
    suspend fun getNote(noteId: String): NoteItem?
    fun observePeriodGoals(): Flow<List<PeriodGoal>>
    fun observePeriodGoalsInRange(startDate: LocalDate?, endDateInclusive: LocalDate?): Flow<List<PeriodGoal>>
    /** Unlimited newest-first history of [period] before [beforeEpochDays] (epoch days, exclusive). */
    fun pagingGoalHistory(period: Period, beforeEpochDays: Int): PagingSource<Int, PeriodGoalHistoryItem>
    suspend fun periodGoalFor(period: Period, date: LocalDate): PeriodGoal?
    suspend fun savePeriodGoal(goal: PeriodGoal)
    fun observeDailyReflectStats(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<DailyReflectStat>>
    fun observeDailyTagRollups(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<DailyTagRollup>>
    fun observeHabitDailyRollups(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<HabitDailyRollup>>
    fun observeDoneItemSummaries(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<DoneItemSummary>>
    fun observeJournalEntriesInRange(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<JournalEntry>>
    fun observeJournalEntriesFiltered(
        moodEmojis: List<String>,
        searchText: String?,
        tagId: String?,
        startDate: LocalDate? = null,
        endDateInclusive: LocalDate? = null
    ): Flow<List<JournalEntry>>

    /** Whether any journal entry or day review exists before [beforeDate] (browse-mode "load more" gate). */
    fun observeOlderJournalHistoryExists(beforeDate: LocalDate): Flow<Boolean>
    suspend fun rebuildReflectStats()
    suspend fun completeDayClose(
        date: LocalDate,
        markDoneItemIds: List<String>,
        carryItemIds: List<String>,
        dropItemIds: List<String>,
        winNote: String?,
        tomorrowGoal: String?,
        rating: Float = 0f,
        doneCount: Int,
        plannedCount: Int,
        doneMinutes: Int,
        targetDate: LocalDate,
        nowMillis: Long
    ): DayCloseCommitResult
    suspend fun copyDailyPlanItemToDate(source: DailyPlanItem, targetDate: LocalDate, clearTimes: Boolean): String?
    suspend fun countDoneDailyPlanItemsForTaskOnDate(taskId: String, dateEpochDays: Int, excludeItemId: String): Int
    suspend fun addNote(input: NoteWriteInput): String
    suspend fun updateNote(noteId: String, input: NoteWriteInput)
    suspend fun completeNote(noteId: String)
    suspend fun updateNoteStatus(noteId: String, status: TaskStatus)
    suspend fun trashNote(noteId: String)
    suspend fun restoreNote(noteId: String)
    suspend fun moveTask(taskId: String, listId: String, sectionId: String?, sortOrder: Int, isPinned: Boolean)
    suspend fun moveNote(noteId: String, listId: String, sectionId: String?, sortOrder: Int, isPinned: Boolean)
    suspend fun addSection(listId: String, title: String, color: String): String
    suspend fun updateSection(sectionId: String, title: String, color: String, sortOrder: Int)
    suspend fun deleteSection(sectionId: String)
    fun observeNestedDocuments(): Flow<List<NestedDocument>>
    fun observeTags(): Flow<List<TagItem>>
    fun observeNestedDocumentTree(documentId: String): Flow<NestedDocumentTree>
    suspend fun addNestedDocument(title: String): String
    suspend fun renameNestedDocument(documentId: String, title: String)
    suspend fun deleteNestedDocument(documentId: String)
    suspend fun addNestedItem(documentId: String, parentId: String?, text: String, position: Int?): String
    suspend fun updateNestedItemText(itemId: String, text: String)
    suspend fun updateNestedItemNote(itemId: String, note: String?)
    suspend fun updateNestedItemFormatting(itemId: String, textStyle: NestedTextStyle, textColor: NestedColorToken, backgroundColor: NestedColorToken)
    suspend fun updateNestedItemPriority(itemId: String, priority: TaskPriority)
    suspend fun updateNestedItemDateRange(itemId: String, startDate: LocalDate?, endDate: LocalDate?)
    suspend fun updateNestedItemTags(itemId: String, tagIds: List<String>)
    suspend fun updateNestedItemMetricSettings(itemId: String, actualMinutes: Int, metricRollupPolicy: MetricRollupPolicy, showTrackedMinutes: Boolean)
    suspend fun updateNestedItemProgress(itemId: String, progressPercent: Int?)
    suspend fun replaceNestedManualMetrics(itemId: String, metrics: List<MetricItem>)
    suspend fun setNestedItemCheckboxEnabled(itemId: String, checkboxEnabled: Boolean)
    suspend fun setNestedItemsChecked(itemIds: List<String>, checked: Boolean)
    suspend fun toggleNestedItemCollapsed(itemId: String)
    suspend fun moveNestedItems(moves: List<NestedItemMove>)
    suspend fun deleteNestedItems(itemIds: List<String>)
    suspend fun exportBackupJson(): String
    suspend fun importBackupJson(json: String)
}

data class DailyPlanItemTimeUpdate(
    val itemId: String,
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
    val listId: String? = null,
    val sectionId: String? = null,
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
    val tagIds: List<String>
)

data class SubTaskWriteInput(
    val name: String,
    val isCompleted: Boolean
)

data class NoteWriteInput(
    val listId: String? = null,
    val sectionId: String? = null,
    val title: String,
    val content: String,
    val status: TaskStatus,
    val date: LocalDate?,
    val startTimeMinutes: Int?,
    val label: String? = null,
    val isPinned: Boolean = false,
    val tagIds: List<String>
)

data class DailyPlanItemWriteInput(
    val date: LocalDate,
    val title: String,
    val note: String?,
    val source: DailyPlanItemSource,
    val status: DailyPlanItemStatus,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val tagIds: List<String>,
    val label: String? = null,
    val nestedListItemId: String? = null
)

data class JournalEntryWriteInput(
    val date: LocalDate,
    val label: String?,
    val content: String,
    val moods: List<String> = emptyList(),
    val tagIds: List<String> = emptyList(),
    val attachments: List<String> = emptyList()
)

class RoomCheckItRepository(
    private val dao: CheckItDao,
    private val reminderNotificationScheduler: TaskReminderNotificationScheduler = NoOpTaskReminderNotificationScheduler(),
    private val dailyPlanScheduleReminderScheduler: DailyPlanScheduleReminderScheduler =
        NoOpDailyPlanScheduleReminderScheduler(),
    private val quickNoteDao: QuickNoteDao? = null,
    private val settingsRepository: SettingsRepository? = null,
) : CheckItRepository {

    private val dailyPlanItemCache = mutableMapOf<String, DailyPlanItem>()
    private val dailyPlanCache = mutableMapOf<LocalDate, DailyPlan>()
    private val taskItemCache = mutableMapOf<String, TaskItem>()
    private val noteItemCache = mutableMapOf<String, NoteItem>()

    override fun observeTagUsageCounts(): Flow<Map<String, Int>> =
        dao.observeTagUsageCounts().map { rows -> rows.associate { it.tagId to it.usageCount } }

    override fun observeTaskBoard(onlyOpen: Boolean): Flow<TaskBoard> {
        val tasksFlow = if (onlyOpen) dao.observeTasksOpen() else dao.observeTasksAll()
        val notesFlow = if (onlyOpen) dao.observeNotesOpen() else dao.observeNotesAll()
        return observeTaskBoardInternal(tasksFlow, notesFlow)
    }

    override fun observeTasksForDate(date: LocalDate): Flow<List<TaskItem>> =
        observeTaskBoardInternal(
            tasksFlow = dao.observeTasksForDate(date.toEpochDays().toInt()),
            notesFlow = kotlinx.coroutines.flow.flowOf(emptyList())
        ).map { it.tasks }

    override fun observeWorkingTasks(date: LocalDate): Flow<List<TaskItem>> =
        observeTaskBoardInternal(
            tasksFlow = dao.observeWorkingTasks(date.toEpochDays().toInt()),
            notesFlow = kotlinx.coroutines.flow.flowOf(emptyList())
        ).map { it.tasks }

    override fun observeNotesForDate(date: LocalDate): Flow<List<NoteItem>> =
        observeTaskBoardInternal(
            tasksFlow = kotlinx.coroutines.flow.flowOf(emptyList()),
            notesFlow = dao.observeNotesForDate(date.toEpochDays().toInt())
        ).map { it.notes }

    override fun observeTasksInRange(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<TaskItem>> =
        observeTaskBoardInternal(
            tasksFlow = dao.observeTasksForDateRange(
                startDate.toEpochDays().toInt(),
                endDateInclusive.toEpochDays().toInt()
            ),
            notesFlow = kotlinx.coroutines.flow.flowOf(emptyList())
        ).map { it.tasks }

    override fun observeNotesInRange(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<NoteItem>> =
        observeTaskBoardInternal(
            tasksFlow = kotlinx.coroutines.flow.flowOf(emptyList()),
            notesFlow = dao.observeNotesForDateRange(
                startDate.toEpochDays().toInt(),
                endDateInclusive.toEpochDays().toInt()
            )
        ).map { it.notes }

    private fun observeTaskBoardInternal(
        tasksFlow: Flow<List<TaskEntity>>,
        notesFlow: Flow<List<NoteEntity>>
    ): Flow<TaskBoard> {
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

    override suspend fun addList(input: ListWriteInput): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = Uuid.random().toString()
        dao.insertList(
            ListEntity(
                id = id,
                title = input.title,
                color = input.color,
                icon = input.icon,
                sortOrder = dao.nextListSortOrder(),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        return id
    }

    override suspend fun updateList(listId: String, input: ListWriteInput) {
        dao.updateList(
            listId = listId,
            title = input.title,
            icon = input.icon,
            color = input.color,
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun deleteList(listId: String) {
        val inboxId = dao.inboxListId() ?: return
        if (listId == inboxId) return
        dao.deleteListMovingContents(
            listId = listId,
            targetListId = inboxId,
            nowMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun addTag(input: TagWriteInput): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = Uuid.random().toString()
        dao.insertTag(
            TagEntity(
                id = id,
                name = input.name,
                color = input.color,
                sortOrder = dao.nextTagSortOrder(),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        return id
    }

    override suspend fun updateTag(tagId: String, input: TagWriteInput) {
        dao.updateTag(
            tagId = tagId,
            name = input.name,
            color = input.color,
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun updateTagSortOrder(tagId: String, sortOrder: Int) {
        dao.updateTagSortOrder(tagId, sortOrder, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun deleteTag(tagId: String) {
        dao.deleteTag(tagId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun isTagNameTaken(name: String, excludeTagId: String?): Boolean =
        dao.tagNameInUseExcept(name = name, excludeId = excludeTagId ?: "") > 0

    override suspend fun addTask(input: TaskWriteInput): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val isTask = input.type == TaskType.Task
        val taskId = Uuid.random().toString()
        dao.insertTask(
            TaskEntity(
                id = taskId,
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

    override suspend fun updateTask(taskId: String, input: TaskWriteInput) {
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
        val existingTaskListJoin = dao.taskListByTaskId(taskId)
        if (existingTaskListJoin?.listId != input.listId) {
            // List membership changed: re-insert at the end of the target list.
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
        } else if (existingTaskListJoin != null) {
            // Same list: keep the task's position, only sync pin/section.
            val updated = existingTaskListJoin.copy(isPinned = input.isPinned, sectionId = input.sectionId)
            if (updated != existingTaskListJoin) {
                dao.insertTaskList(updated)
            }
        }
        dao.deleteTaskTags(taskId)
        input.tagIds.forEach { tagId -> addTaskTag(taskId, tagId) }
        dao.replaceTaskSubTasks(taskId, input.subtasks)
        dao.replaceTaskReminders(taskId, input.reminders)
        scheduleTaskReminders(taskId, input)
    }

    override suspend fun trashTask(taskId: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.trashTask(taskId, now)
        dao.deletePlannedDailyPlanItemsForTask(taskId, now)
        reminderNotificationScheduler.cancelTaskReminders(taskId)
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun restoreTask(taskId: String) {
        dao.restoreTask(taskId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun completeTask(taskId: String) {
        val instant = Clock.System.now()
        val today = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val completedAtMillis = instant.toEpochMilliseconds()
        dao.completeTask(
            taskId = taskId,
            status = TaskStatus.Completed.name,
            completedDateEpochDays = today.toEpochDays().toInt(),
            updatedAtMillis = completedAtMillis
        )
        reminderNotificationScheduler.cancelTaskReminders(taskId)
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.updateTaskStatus(
            taskId = taskId,
            status = status.name,
            updatedAtMillis = now
        )
    }

    override suspend fun addTaskToDailyPlan(date: LocalDate, task: TaskItem): String {
        val dateEpochDays = date.toEpochDays().toInt()
        val now = Clock.System.now().toEpochMilliseconds()
        val itemId = Uuid.random().toString()
        dao.insertDailyPlanItem(
            DailyPlanItemEntity(
                id = itemId,
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
                completedAtMillis = if (task.status == TaskStatus.Completed) now else null,
                updatedAtMillis = now
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
        tagIds: List<String>,
        label: String?,
        taskId: String?,
        nestedListItemId: String?,
        carriedFromItemId: String?
    ): String {
        val dateEpochDays = date.toEpochDays().toInt()
        val now = Clock.System.now().toEpochMilliseconds()
        val itemId = Uuid.random().toString()
        dao.insertDailyPlanItem(
            DailyPlanItemEntity(
                id = itemId,
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
                carriedFromItemId = carriedFromItemId,
                updatedAtMillis = now
            )
        )
        tagIds.forEach { tagId -> addDailyPlanItemTag(itemId, tagId) }
        dailyPlanScheduleReminderScheduler.rescheduleNext()
        return itemId
    }

    override suspend fun updateDailyPlanItemTime(
        itemId: String,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?
    ) {
        val item = dao.dailyPlanItemById(itemId)
        val now = Clock.System.now().toEpochMilliseconds()
        dao.updateDailyPlanItemTime(itemId, startTimeMinutes, endTimeMinutes, now)
        item?.taskId?.let { taskId ->
            dao.clearTaskTime(taskId, now)
        }
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun updateDailyPlanItemTimes(updates: List<DailyPlanItemTimeUpdate>) {
        if (updates.isEmpty()) return
        val now = Clock.System.now().toEpochMilliseconds()
        val items = updates.map { update -> dao.dailyPlanItemById(update.itemId) }
        dao.updateDailyPlanItemTimes(updates, now)
        items.mapNotNull { it?.taskId }.forEach { taskId ->
            dao.clearTaskTime(taskId, now)
        }
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun updateDailyPlanItemStatus(itemId: String, status: DailyPlanItemStatus) {
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
        itemIds: List<String>,
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

    override suspend fun updateDailyPlanItem(itemId: String, input: DailyPlanItemWriteInput) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.updateDailyPlanItemWithTags(
            itemId = itemId,
            dateEpochDays = input.date.toEpochDays().toInt(),
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

    override suspend fun updateDailyPlanItemTags(itemId: String, tagIds: List<String>) {
        dao.deleteDailyPlanItemTags(itemId)
        tagIds.forEach { tagId -> addDailyPlanItemTag(itemId, tagId) }
        // Membership syncs embedded in the item document.
        dao.markDailyPlanItemDirty(itemId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun linkDailyPlanItemToTask(itemId: String, taskId: String) {
        dao.linkDailyPlanItemToTask(
            itemId,
            taskId,
            DailyPlanItemSource.ExistingTask.name,
            Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun deleteDailyPlanItem(itemId: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val oldEntity = dao.dailyPlanItemById(itemId)
        if (oldEntity?.nestedListItemId != null && oldEntity.status == DailyPlanItemStatus.Done.name) {
            val minutes = oldEntity.toDomain().workMinutes()
            if (minutes > 0) {
                dao.updateNestedItemActualMinutesDelta(
                    itemId = oldEntity.nestedListItemId,
                    delta = -minutes,
                    updatedAtMillis = now
                )
            }
        }
        dao.deleteDailyPlanItem(itemId, now)
        dailyPlanScheduleReminderScheduler.rescheduleNext()
    }

    override suspend fun addJournalEntry(input: JournalEntryWriteInput): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val entryId = Uuid.random().toString()
        dao.insertJournalEntry(
            JournalEntryEntity(
                id = entryId,
                dateEpochDays = input.date.toEpochDays().toInt(),
                label = input.label?.trim()?.takeIf { it.isNotBlank() },
                content = input.content.trim(),
                moods = input.moods.joinToString(","),
                createdTimeMinutes = currentTimeMinutes(),
                attachments = input.attachments.joinToString(","),
                updatedAtMillis = now
            )
        )
        input.tagIds.forEach { tagId -> addJournalEntryTag(entryId, tagId) }
        return entryId
    }

    override suspend fun updateJournalEntry(entryId: String, input: JournalEntryWriteInput) {
        dao.updateJournalEntry(
            entryId = entryId,
            label = input.label?.trim()?.takeIf { it.isNotBlank() },
            content = input.content.trim(),
            moods = input.moods.joinToString(","),
            attachments = input.attachments.joinToString(","),
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        dao.deleteJournalEntryTags(entryId)
        input.tagIds.forEach { tagId -> addJournalEntryTag(entryId, tagId) }
    }

    override suspend fun deleteJournalEntry(entryId: String) {
        dao.deleteJournalEntry(entryId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun getDailyPlanItem(itemId: String): DailyPlanItem? {
        val item = dao.dailyPlanItemById(itemId) ?: return null
        val tagIds = dao.tagIdsForItem(itemId)
        val tags = if (tagIds.isNotEmpty()) dao.tagsByIds(tagIds).map { it.toDomain() } else emptyList()
        return item.toDomain(tags)
    }

    override suspend fun getTask(taskId: String): TaskItem? {
        val entity = dao.taskById(taskId) ?: return null
        val listJoin = dao.taskListByTaskId(taskId)
        val list = listJoin?.listId?.let { listId ->
            val listEntity = dao.listById(listId) ?: return@let null
            val sections = dao.observeSectionsForList(listId).first()
            listEntity.toDomain(sections.map { it.toDomain() })
        }
        val subtasks = dao.subTasksForTask(taskId).map { it.toDomain() }
        val reminders = dao.remindersForTask(taskId).map { it.toDomain() }
        val tagIds = dao.tagIdsForTask(taskId)
        val tags = if (tagIds.isNotEmpty()) dao.tagsByIds(tagIds).map { it.toDomain() } else emptyList()

        return entity.toDomain(
            list = list,
            subtasks = subtasks,
            reminders = reminders,
            tags = tags,
            listSortOrder = listJoin?.sortOrder ?: 0,
            isPinned = listJoin?.isPinned ?: false,
            sectionId = listJoin?.sectionId
        )
    }

    override suspend fun getNote(noteId: String): NoteItem? {
        val entity = dao.noteById(noteId) ?: return null
        val listJoin = dao.noteListByNoteId(noteId)
        val list = listJoin?.listId?.let { listId ->
            val listEntity = dao.listById(listId) ?: return@let null
            val sections = dao.observeSectionsForList(listId).first()
            listEntity.toDomain(sections.map { it.toDomain() })
        }
        val tagIds = dao.tagIdsForNote(noteId)
        val tags = if (tagIds.isNotEmpty()) dao.tagsByIds(tagIds).map { it.toDomain() } else emptyList()

        return entity.toDomain(
            list = list,
            tags = tags,
            listSortOrder = listJoin?.sortOrder ?: 0,
            isPinned = listJoin?.isPinned ?: false,
            sectionId = listJoin?.sectionId
        )
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
    ): String? {
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
        val itemId = Uuid.random().toString()
        dao.insertDailyPlanItem(
            DailyPlanItemEntity(
                id = itemId,
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
                carriedFromItemId = source.id,
                updatedAtMillis = now
            )
        )
        source.tags.forEach { tag -> addDailyPlanItemTag(itemId, tag.id) }
        dao.markDailyPlanItemsHandled(listOf(source.id), now)
        dailyPlanScheduleReminderScheduler.rescheduleNext()
        return itemId
    }

    override fun observePeriodGoals(): Flow<List<PeriodGoal>> =
        dao.observePeriodGoals().map { entities -> entities.map { it.toDomain() } }

    override fun observePeriodGoalsInRange(
        startDate: LocalDate?,
        endDateInclusive: LocalDate?
    ): Flow<List<PeriodGoal>> =
        dao.observePeriodGoalsBetween(
            startDate?.toEpochDays()?.toInt(),
            endDateInclusive?.toEpochDays()?.toInt()
        ).map { entities -> entities.map { it.toDomain() } }

    override fun pagingGoalHistory(period: Period, beforeEpochDays: Int): PagingSource<Int, PeriodGoalHistoryItem> =
        GoalHistoryPagingSource(dao, period, beforeEpochDays)

    override suspend fun periodGoalFor(period: Period, date: LocalDate): PeriodGoal? =
        dao.periodGoalFor(period.name, date.toEpochDays().toInt())?.toDomain()

    override suspend fun savePeriodGoal(goal: PeriodGoal) {
        // Resolve the persisted id up front: a row may already exist for this
        // (periodType, startEpochDays) even when [goal] carries no id, and
        // Room's @Upsert only falls back to "update WHERE id" (which cannot
        // match an unset id). With the id resolved, the upsert either inserts
        // a genuinely new row or updates the existing one by primary key.
        // New rows get a random UUID primary key (stable across devices for sync).
        val existingId = goal.id.takeIf { it.isNotEmpty() }
            ?: dao.periodGoalFor(goal.period.name, goal.startEpochDays)?.id
        val now = Clock.System.now().toEpochMilliseconds()
        dao.upsertPeriodGoal(
            PeriodGoalEntity(
                id = existingId ?: Uuid.random().toString(),
                periodType = goal.period.name,
                startEpochDays = goal.startEpochDays,
                endEpochDays = goal.endEpochDays,
                review = goal.review,
                goal = goal.goal,
                rating = goal.rating,
                completedAtMillis = goal.completedAtMillis,
                editedAtMillis = goal.editedAtMillis,
                updatedAtMillis = now,
                dirty = true,
                deleted = false,
                metricsJson = Json.encodeToString(goal.metrics.map { it.normalized() })
            )
        )
    }

    override fun observeDailyReflectStats(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): Flow<List<DailyReflectStat>> {
        val startEpochDays = startDate.toEpochDays().toInt()
        val endEpochDays = endDateInclusive.toEpochDays().toInt()
        return combine(
            dao.observeDailyReflectStats(startEpochDays, endEpochDays),
            dao.observeDailyTagRollups(startEpochDays, endEpochDays)
        ) { stats, rollups ->
            val rollupsByDate = rollups.groupBy { it.dateEpochDays }
            stats.map { entity ->
                entity.toDomain(
                    tagRollups = rollupsByDate[entity.dateEpochDays]
                        .orEmpty()
                        .map { it.toDomain() }
                )
            }
        }
    }

    override fun observeDailyTagRollups(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): Flow<List<DailyTagRollup>> =
        dao.observeDailyTagRollups(startDate.toEpochDays().toInt(), endDateInclusive.toEpochDays().toInt())
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeHabitDailyRollups(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): Flow<List<HabitDailyRollup>> =
        dao.observeHabitDailyRollups(startDate.toEpochDays().toInt(), endDateInclusive.toEpochDays().toInt())
            .map { entities ->
                entities.map {
                    HabitDailyRollup(
                        dateEpochDays = it.dateEpochDays,
                        habitKey = it.habitKey,
                        title = it.title,
                        doneMinutes = it.doneMinutes
                    )
                }
            }

    override fun observeDoneItemSummaries(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): Flow<List<DoneItemSummary>> =
        dao.observeDoneItemSummaries(startDate.toEpochDays().toInt(), endDateInclusive.toEpochDays().toInt())
            .map { entities ->
                entities.map {
                    DoneItemSummary(
                        id = it.id,
                        dateEpochDays = it.dateEpochDays,
                        title = it.title,
                        note = it.note,
                        sourceName = it.source,
                        startTimeMinutes = it.startTimeMinutes,
                        endTimeMinutes = it.endTimeMinutes,
                        completedAtMillis = it.completedAtMillis
                    )
                }
            }

    override fun observeJournalEntriesInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): Flow<List<JournalEntry>> =
        combine(
            dao.observeJournalEntriesInRange(startDate.toEpochDays().toInt(), endDateInclusive.toEpochDays().toInt()),
            dao.observeJournalEntryTags(),
            dao.observeTags()
        ) { entries, entryTags, tags ->
            val domainTags = tags.map { it.toDomain() }
            val tagsById = domainTags.associateBy { it.id }
            val entryTagIds = entryTags.groupBy { it.entryId }.mapValues { it.value.map { t -> t.tagId } }
            entries.map { entry ->
                entry.toDomain(entryTagIds[entry.id].orEmpty().mapNotNull { tagsById[it] })
            }
        }

    override fun observeJournalEntriesFiltered(
        moodEmojis: List<String>,
        searchText: String?,
        tagId: String?,
        startDate: LocalDate?,
        endDateInclusive: LocalDate?
    ): Flow<List<JournalEntry>> {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any>()

        if (moodEmojis.isNotEmpty()) {
            conditions.add(
                moodEmojis.joinToString(separator = " OR ", prefix = "(", postfix = ")") {
                    args.add("%$it%")
                    "moods LIKE ?"
                }
            )
        }
        if (!searchText.isNullOrBlank()) {
            args.add("%$searchText%")
            args.add("%$searchText%")
            conditions.add("(content LIKE ? OR label LIKE ?)")
        }
        if (tagId != null) {
            args.add(tagId)
            conditions.add(
                "EXISTS(SELECT 1 FROM journal_entry_tags AS jt WHERE jt.entryId = journal_entries.id AND jt.tagId = ?)"
            )
        }
        if (startDate != null) {
            args.add(startDate.toEpochDays().toInt())
            conditions.add("dateEpochDays >= ?")
        }
        if (endDateInclusive != null) {
            args.add(endDateInclusive.toEpochDays().toInt())
            conditions.add("dateEpochDays <= ?")
        }

        val sql = buildString {
            append("SELECT * FROM journal_entries WHERE deleted = 0")
            if (conditions.isNotEmpty()) {
                append(" AND ")
                append(conditions.joinToString(separator = " AND "))
            }
            append(" ORDER BY dateEpochDays DESC, createdTimeMinutes ASC")
        }
        val query = RoomRawQuery(sql) { statement ->
            args.forEachIndexed { index, arg ->
                when (arg) {
                    is String -> statement.bindText(index + 1, arg)
                    is Long -> statement.bindLong(index + 1, arg)
                    is Int -> statement.bindLong(index + 1, arg.toLong())
                }
            }
        }

        return combine(
            dao.observeJournalEntriesFiltered(query),
            dao.observeJournalEntryTags(),
            dao.observeTags()
        ) { entries, entryTags, tags ->
            val domainTags = tags.map { it.toDomain() }
            val tagsById = domainTags.associateBy { it.id }
            val entryTagIds = entryTags.groupBy { it.entryId }.mapValues { it.value.map { t -> t.tagId } }
            entries.map { entry ->
                entry.toDomain(entryTagIds[entry.id].orEmpty().mapNotNull { tagsById[it] })
            }
        }
    }

    override fun observeOlderJournalHistoryExists(beforeDate: LocalDate): Flow<Boolean> {
        val epochDays = beforeDate.toEpochDays().toInt()
        return combine(
            dao.observeJournalEntryExistsBefore(epochDays),
            dao.observeDayGoalExistsBefore(epochDays)
        ) { hasEntries, hasGoals -> hasEntries || hasGoals }
    }

    override suspend fun rebuildReflectStats() {
        dao.rebuildReflectStats(computedAtMillis = Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun completeDayClose(
        date: LocalDate,
        markDoneItemIds: List<String>,
        carryItemIds: List<String>,
        dropItemIds: List<String>,
        winNote: String?,
        tomorrowGoal: String?,
        rating: Float,
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
            rating = rating,
            doneCount = doneCount,
            plannedCount = plannedCount,
            doneMinutes = doneMinutes,
            targetDateEpochDays = targetDate.toEpochDays().toInt(),
            nowMillis = nowMillis
        )

    override suspend fun countDoneDailyPlanItemsForTaskOnDate(
        taskId: String,
        dateEpochDays: Int,
        excludeItemId: String
    ): Int = dao.countDoneDailyPlanItemsForTaskOnDate(taskId, dateEpochDays, excludeItemId)

    override suspend fun addNote(input: NoteWriteInput): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val noteId = Uuid.random().toString()
        dao.insertNote(
            NoteEntity(
                id = noteId,
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

    override suspend fun updateNote(noteId: String, input: NoteWriteInput) {
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
        val existingNoteListJoin = dao.noteListByNoteId(noteId)
        if (existingNoteListJoin?.listId != input.listId) {
            // List membership changed: re-insert at the end of the target list.
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
        } else if (existingNoteListJoin != null) {
            // Same list: keep the note's position, only sync pin/section.
            val updated = existingNoteListJoin.copy(isPinned = input.isPinned, sectionId = input.sectionId)
            if (updated != existingNoteListJoin) {
                dao.insertNoteList(updated)
            }
        }
        dao.deleteNoteTags(noteId)
        input.tagIds.forEach { tagId -> addNoteTag(noteId, tagId) }
    }

    override suspend fun trashNote(noteId: String) {
        dao.trashNote(noteId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun restoreNote(noteId: String) {
        dao.restoreNote(noteId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun moveTask(taskId: String, listId: String, sectionId: String?, sortOrder: Int, isPinned: Boolean) {
        dao.insertTaskList(
            TaskListEntity(
                taskId = taskId,
                listId = listId,
                sectionId = sectionId,
                sortOrder = sortOrder,
                isPinned = isPinned
            )
        )
        // Membership syncs embedded in the task document.
        dao.markTaskDirty(taskId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun moveNote(noteId: String, listId: String, sectionId: String?, sortOrder: Int, isPinned: Boolean) {
        dao.insertNoteList(
            NoteListEntity(
                noteId = noteId,
                listId = listId,
                sectionId = sectionId,
                sortOrder = sortOrder,
                isPinned = isPinned
            )
        )
        // Membership syncs embedded in the note document.
        dao.markNoteDirty(noteId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun addSection(listId: String, title: String, color: String): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = Uuid.random().toString()
        dao.insertListSection(
            ListSectionEntity(
                id = id,
                listId = listId,
                title = title.trim(),
                color = color,
                sortOrder = dao.nextSectionSortOrder(listId),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        return id
    }

    override suspend fun updateSection(sectionId: String, title: String, color: String, sortOrder: Int) {
        dao.updateSection(sectionId, title.trim(), color, sortOrder, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun deleteSection(sectionId: String) {
        dao.deleteSection(sectionId, Clock.System.now().toEpochMilliseconds())
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

    override fun observeNestedDocumentTree(documentId: String): Flow<NestedDocumentTree> =
        combine(
            dao.observeNestedDocuments(),
            dao.observeNestedItems(documentId),
            dao.observeNestedItemTags(documentId)
        ) { documents, items, links ->
            val tagsById = links.map { it.tagId }.distinct()
                .takeIf { it.isNotEmpty() }
                ?.let { dao.tagsByIds(it).associateBy(TagEntity::id) }
                .orEmpty()
            val tagsByItemId = links.groupBy(NestedItemTagEntity::itemId)
            NestedDocumentTree(
                document = documents.firstOrNull { it.id == documentId }
                    ?.let { NestedDocument(it.id, it.title, it.createdAtMillis, it.updatedAtMillis) }
                    ?: NestedDocument(id = documentId, title = "", createdAtMillis = 0L, updatedAtMillis = 0L),
                rootNodes = buildNestedTree(items.map { item ->
                    item.toNestedListItem(
                        tags = tagsByItemId[item.id].orEmpty().mapNotNull { tagsById[it.tagId]?.toDomain() }
                    )
                })
            )
        }

    override suspend fun addNestedDocument(title: String): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val trimmed = title.trim()
        val id = Uuid.random().toString()
        dao.insertNestedDocument(
            NestedDocumentEntity(
                id = id,
                title = trimmed,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        return id
    }

    override suspend fun renameNestedDocument(documentId: String, title: String) {
        dao.updateNestedDocumentTitle(documentId, title.trim(), Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun deleteNestedDocument(documentId: String) {
        dao.deleteNestedDocument(documentId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun addNestedItem(documentId: String, parentId: String?, text: String, position: Int?): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = Uuid.random().toString()
        dao.insertNestedListItem(
            NestedListItemEntity(
                id = id,
                documentId = documentId,
                parentId = parentId,
                position = position ?: dao.nextNestedItemPosition(documentId, parentId),
                text = text.trim(),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        return id
    }

    override suspend fun updateNestedItemText(itemId: String, text: String) {
        dao.updateNestedItemText(itemId, text.trim(), Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun updateNestedItemNote(itemId: String, note: String?) {
        dao.updateNestedItemNote(
            itemId,
            note?.trim()?.takeIf { it.isNotBlank() },
            Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun updateNestedItemFormatting(
        itemId: String,
        textStyle: NestedTextStyle,
        textColor: NestedColorToken,
        backgroundColor: NestedColorToken
    ) {
        dao.updateNestedItemFormatting(itemId, textStyle.name, textColor.name, backgroundColor.name, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun updateNestedItemPriority(itemId: String, priority: TaskPriority) {
        dao.updateNestedItemPriority(itemId, priority.name, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun updateNestedItemDateRange(itemId: String, startDate: LocalDate?, endDate: LocalDate?) {
        dao.updateNestedItemDateRange(
            itemId,
            startDate?.toEpochDays()?.toInt(),
            endDate?.toEpochDays()?.toInt(),
            Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun updateNestedItemMetricSettings(
        itemId: String,
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

    override suspend fun updateNestedItemProgress(itemId: String, progressPercent: Int?) {
        dao.updateNestedItemProgress(
            itemId,
            progressPercent?.coerceIn(0, 100),
            Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun replaceNestedManualMetrics(itemId: String, metrics: List<MetricItem>) {
        dao.updateNestedItemManualMetrics(
            itemId = itemId,
            metricsJson = Json.encodeToString(metrics.map { it.normalized() }),
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun updateNestedItemTags(itemId: String, tagIds: List<String>) {
        dao.replaceNestedItemTags(itemId, tagIds)
        val now = Clock.System.now().toEpochMilliseconds()
        // Membership syncs embedded in the item document.
        dao.markNestedItemDirty(itemId, now)
        tagIds.distinct().forEach { dao.updateTagLastUsedAtMillis(it, now) }
    }

    override suspend fun setNestedItemCheckboxEnabled(itemId: String, checkboxEnabled: Boolean) {
        dao.setNestedItemCheckboxEnabled(itemId, checkboxEnabled, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun setNestedItemsChecked(itemIds: List<String>, checked: Boolean) {
        if (itemIds.isEmpty()) return
        dao.setNestedItemsChecked(itemIds, checked, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun toggleNestedItemCollapsed(itemId: String) {
        dao.toggleNestedItemCollapsed(itemId, Clock.System.now().toEpochMilliseconds())
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

    override suspend fun deleteNestedItems(itemIds: List<String>) {
        if (itemIds.isEmpty()) return
        dao.deleteNestedItems(itemIds, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun exportBackupJson(): String {
        val backup = CheckItBackup(
            exportedAtMillis = Clock.System.now().toEpochMilliseconds(),
            settings = settingsRepository?.settings?.first() ?: UserSettings(),
            lists = dao.getAllListsOnce(),
            listSections = dao.getAllListSectionsOnce(),
            tags = dao.getAllTagsOnce(),
            tasks = dao.getAllTasksOnce(),
            subTasks = dao.getAllSubTasksOnce(),
            taskReminders = dao.getAllTaskRemindersOnce(),
            taskTags = dao.getAllTaskTagsOnce(),
            taskLists = dao.getAllTaskListsOnce(),
            notes = dao.getAllNotesOnce(),
            noteTags = dao.getAllNoteTagsOnce(),
            noteLists = dao.getAllNoteListsOnce(),
            dailyPlanItems = dao.getAllDailyPlanItemsOnce(),
            dailyPlanItemTags = dao.getAllDailyPlanItemTagsOnce(),
            journalEntries = dao.getAllJournalEntriesOnce(),
            journalEntryTags = dao.getAllJournalEntryTagsOnce(),
            taskFilters = dao.getAllTaskFiltersOnce(),
            periodGoals = dao.getAllPeriodGoalsOnce(),
            nestedDocuments = dao.getAllNestedDocumentsOnce(),
            nestedListItems = dao.getAllNestedListItemsOnce(),
            nestedItemTags = dao.getAllNestedItemTagsOnce(),
            quickNotes = quickNoteDao?.getAllOnce().orEmpty(),
        )
        return backupJson.encodeToString(CheckItBackup.serializer(), backup)
    }

    override suspend fun importBackupJson(json: String) {
        val backup = backupJson.decodeFromString(CheckItBackup.serializer(), json)
        require(backup.version == CheckItBackup.BACKUP_VERSION) {
            "Unsupported backup version ${backup.version}"
        }
        dao.restoreBackup(backup)
        quickNoteDao?.clearAll()
        if (backup.quickNotes.isNotEmpty()) {
            quickNoteDao?.insertAll(backup.quickNotes)
        }
        settingsRepository?.let { repository ->
            val settings = backup.settings
            repository.setLanguageCode(settings.languageCode)
            repository.setThemeModeCode(settings.themeModeCode)
            repository.setColorSchemeModeCode(settings.colorSchemeModeCode)
            repository.setTaskWorkspaceViewCode(settings.taskWorkspaceViewCode)
            repository.setTaskListDisplayTypeCode(settings.taskListDisplayTypeCode)
            repository.setTaskShowCompleted(settings.taskShowCompleted)
            repository.setTaskSortOptionCode(settings.taskSortOptionCode)
            repository.setPlanReminderEnabled(settings.planReminderEnabled)
            repository.setPlanReminderTimeMinutes(settings.planReminderTimeMinutes)
            repository.setReviewReminderEnabled(settings.reviewReminderEnabled)
            repository.setReviewReminderTimeMinutes(settings.reviewReminderTimeMinutes)
            repository.setCheckInReminderEnabled(settings.checkInReminderEnabled)
            repository.setIdleCheckInThresholdMinutes(settings.idleCheckInThresholdMinutes)
            repository.setScheduleReminderEnabled(settings.scheduleReminderEnabled)
        }
        dailyPlanItemCache.clear()
        dailyPlanCache.clear()
        taskItemCache.clear()
        noteItemCache.clear()
        rebuildReflectStats()
    }

    private suspend fun addTaskTag(taskId: String, tagId: String) {
        dao.insertTaskTagIfParentsExist(taskId, tagId)
        dao.updateTagLastUsedAtMillis(tagId, Clock.System.now().toEpochMilliseconds())
    }

    private suspend fun addNoteTag(noteId: String, tagId: String) {
        dao.insertNoteTagIfParentsExist(noteId, tagId)
        dao.updateTagLastUsedAtMillis(tagId, Clock.System.now().toEpochMilliseconds())
    }

    private suspend fun addDailyPlanItemTag(itemId: String, tagId: String) {
        dao.insertDailyPlanItemTagIfParentsExist(itemId, tagId)
        dao.updateTagLastUsedAtMillis(tagId, Clock.System.now().toEpochMilliseconds())
    }

    private suspend fun addJournalEntryTag(entryId: String, tagId: String) {
        dao.insertJournalEntryTagIfParentsExist(entryId, tagId)
        dao.updateTagLastUsedAtMillis(tagId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun completeNote(noteId: String) {
        dao.updateNoteStatus(
            noteId = noteId,
            status = TaskStatus.Completed.name,
            editedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun updateNoteStatus(noteId: String, status: TaskStatus) {
        dao.updateNoteStatus(
            noteId = noteId,
            status = status.name,
            editedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    private suspend fun scheduleTaskReminders(taskId: String, input: TaskWriteInput) {
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

private val backupJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
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

private fun TaskEntity.toDomain(
    list: ListItem?,
    subtasks: List<SubTaskItem>,
    reminders: List<TaskReminder>,
    tags: List<TagItem>,
    listSortOrder: Int,
    isPinned: Boolean,
    sectionId: String?
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

private val metricsJsonFormat = Json { ignoreUnknownKeys = true }

/** Trims free-text fields and drops blank optional values before persisting. */
private fun MetricItem.normalized() = copy(
    name = name.trim(),
    value = value.trim(),
    targetValue = targetValue?.trim()?.takeIf { it.isNotEmpty() },
    customUnit = customUnit?.trim()?.takeIf { it.isNotEmpty() }
)

internal fun PeriodGoalEntity.toDomain() = PeriodGoal(
    id = id,
    period = Period.valueOf(periodType),
    startEpochDays = startEpochDays,
    endEpochDays = endEpochDays,
    review = review,
    goal = goal,
    rating = rating,
    completedAtMillis = completedAtMillis,
    editedAtMillis = editedAtMillis,
    metrics = runCatching {
        metricsJsonFormat.decodeFromString<List<MetricItem>>(metricsJson)
    }.getOrDefault(emptyList())
)

/**
 * Append-only Paging 3 source over [CheckItDao.goalHistoryPage]: pages past
 * goals newest-first and enriches each with tracked minutes from the daily
 * rollups. History rows are immutable while the sheet is open, so refresh
 * restarts from the top.
 */
private class GoalHistoryPagingSource(
    private val dao: CheckItDao,
    private val period: Period,
    private val beforeEpochDays: Int
) : PagingSource<Int, PeriodGoalHistoryItem>() {
    override fun getRefreshKey(state: PagingState<Int, PeriodGoalHistoryItem>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PeriodGoalHistoryItem> =
        runCatching {
            val offset = params.key ?: 0
            val entities = dao.goalHistoryPage(period.name, beforeEpochDays, params.loadSize, offset)
            val items = entities.map { entity ->
                PeriodGoalHistoryItem(
                    goal = entity.toDomain(),
                    trackedMinutes = dao.sumDoneMinutesBetween(entity.startEpochDays, entity.endEpochDays)
                )
            }
            LoadResult.Page(
                data = items,
                prevKey = null,
                nextKey = if (entities.size < params.loadSize) null else offset + entities.size
            )
        }.getOrElse { LoadResult.Error(it) }
}

private fun DailyReflectStatsEntity.toDomain(tagRollups: List<DailyTagRollup> = emptyList()) = DailyReflectStat(
    dateEpochDays = dateEpochDays,
    plannedItemCount = plannedItemCount,
    doneItemCount = doneItemCount,
    doneMinutes = doneMinutes,
    journalCount = journalCount,
    tagRollups = tagRollups
)

private fun DailyTagRollupWithMeta.toDomain() = DailyTagRollup(
    dateEpochDays = dateEpochDays,
    tagId = tagId,
    tagName = tagName,
    tagColor = tagColor,
    doneCount = doneCount,
    doneMinutes = doneMinutes
)

private fun JournalEntryEntity.toDomain(tags: List<TagItem> = emptyList()) = JournalEntry(
    id = id,
    dateEpochDays = dateEpochDays,
    label = label,
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
    sectionId: String?
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
    tags: List<TagItem> = emptyList()
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
    progressPercent = progressPercent,
    manualMetrics = runCatching {
        metricsJsonFormat.decodeFromString<List<MetricItem>>(manualMetricsJson)
    }.getOrDefault(emptyList()),
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)
