package com.checkit.ui.tasks

import com.checkit.data.CheckItRepository
import com.checkit.data.DailyPlanItemTimeUpdate
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.data.JournalEntryWriteInput
import com.checkit.data.ListWriteInput
import com.checkit.data.NoteWriteInput
import com.checkit.data.TagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.DailyTagRollup
import com.checkit.domain.DayCloseCommitResult
import com.checkit.domain.DoneItemSummary
import com.checkit.domain.HabitDailyRollup
import com.checkit.domain.JournalEntry
import com.checkit.domain.ListItem
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemMove
import com.checkit.domain.NestedManualMetric
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.NoteItem
import com.checkit.domain.PeriodReview
import com.checkit.domain.Period
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TagItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class FakeCheckItRepository(initialBoard: TaskBoard = TaskBoard()) : CheckItRepository {
    private val boardFlow = MutableStateFlow(initialBoard)
    val addedLists = mutableListOf<ListWriteInput>()
    val updatedLists = mutableListOf<Pair<Long, ListWriteInput>>()
    val deletedLists = mutableListOf<Long>()
    val addedTags = mutableListOf<TagWriteInput>()
    val updatedTags = mutableListOf<Pair<Long, TagWriteInput>>()
    val updatedTagSortOrders = mutableListOf<Pair<Long, Int>>()
    val deletedTags = mutableListOf<Long>()
    val addedTasks = mutableListOf<TaskWriteInput>()
    val updatedTasks = mutableListOf<Pair<Long, TaskWriteInput>>()
    val deletedTasks = mutableListOf<Long>()
    val trashedTasks = mutableListOf<Long>()
    val addedDailyPlanTasks = mutableListOf<Pair<LocalDate, TaskItem>>()
    val addedManualDailyPlanItems = mutableListOf<DailyPlanItemWriteInput>()
    val updatedDailyPlanItems = mutableListOf<Pair<Long, DailyPlanItemWriteInput>>()
    val updatedDailyPlanItemTimes = mutableListOf<Triple<Long, Int?, Int?>>()
    val deletedDailyPlanItemIds = mutableListOf<Long>()
    
    val addedSections = mutableListOf<Triple<Long, String, String>>()
    val updatedSections = mutableListOf<com.checkit.domain.ListSection>()
    val deletedSections = mutableListOf<Long>()
    
    val currentBoard: TaskBoard get() = boardFlow.value
    
    private var nextListId: Long = 100L
    private var nextTagId: Long = 200L
    private var nextTaskId: Long = 300L
    private var nextDailyPlanItemId: Long = 400L
    private var nextSectionId: Long = 600L

    val lastAssignedTagId: Long get() = nextTagId - 1
    val lastAssignedTaskId: Long get() = nextTaskId - 1
    val lastAssignedDailyPlanItemId: Long get() = nextDailyPlanItemId - 1

    private val dailyPlansFlow = MutableStateFlow<List<DailyPlan>>(emptyList())
    val copiedDailyPlanItems = mutableListOf<DailyPlanItem>()
    val statusUpdates = mutableListOf<Pair<Long, DailyPlanItemStatus>>()
    val markedHandledItemIds = mutableListOf<Long>()
    private val periodReviewsFlow = MutableStateFlow<List<PeriodReview>>(emptyList())

    private val journalEntriesFlow = MutableStateFlow<List<JournalEntry>>(emptyList())
    val addedJournalEntries = mutableListOf<JournalEntryWriteInput>()
    val updatedJournalEntries = mutableListOf<Pair<Long, JournalEntryWriteInput>>()
    val deletedJournalEntryIds = mutableListOf<Long>()
    private var nextJournalEntryId: Long = 500L

    override fun observeTaskBoard(onlyOpen: Boolean): Flow<TaskBoard> = boardFlow.map { board ->
        if (onlyOpen) {
            board.copy(
                tasks = board.tasks.filter { it.status == TaskStatus.Open && !it.isTrashed },
                notes = board.notes.filter { it.status == TaskStatus.Open && !it.isTrashed }
            )
        } else {
            board
        }
    }

    override fun observeTagUsageCounts(): Flow<Map<Long, Int>> = boardFlow.map { board ->
        val counts = mutableMapOf<Long, Int>()
        board.tasks.filter { !it.isTrashed }.forEach { task ->
            task.tags.forEach { tag -> counts[tag.id] = (counts[tag.id] ?: 0) + 1 }
        }
        board.notes.filter { !it.isTrashed }.forEach { note ->
            note.tags.forEach { tag -> counts[tag.id] = (counts[tag.id] ?: 0) + 1 }
        }
        dailyPlansFlow.value.forEach { plan ->
            plan.items.forEach { item ->
                item.tags.forEach { tag -> counts[tag.id] = (counts[tag.id] ?: 0) + 1 }
            }
        }
        journalEntriesFlow.value.forEach { entry ->
            entry.tags.forEach { tag -> counts[tag.id] = (counts[tag.id] ?: 0) + 1 }
        }
        counts
    }

    override fun observeTasksForDate(date: LocalDate): Flow<List<TaskItem>> = boardFlow.map { board ->
        board.tasks.filter { it.doDate == date && !it.isTrashed && it.status != TaskStatus.Completed }
    }

    override fun observeWorkingTasks(date: LocalDate): Flow<List<TaskItem>> = boardFlow.map { board ->
        board.tasks.filter { 
            !it.isTrashed && (it.doDate == date || it.status == TaskStatus.Open || it.completedDate == date)
        }
    }

    override fun observeNotesForDate(date: LocalDate): Flow<List<NoteItem>> = boardFlow.map { board ->
        board.notes.filter { it.date == date && !it.isTrashed && it.status != TaskStatus.Completed }
    }

    override fun observeTasksInRange(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<TaskItem>> =
        boardFlow.map { board ->
            board.tasks.filter { task ->
                !task.isTrashed && task.status != TaskStatus.Completed &&
                    task.doDate?.let { it >= startDate && it <= endDateInclusive } == true
            }
        }

    override fun observeNotesInRange(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<NoteItem>> =
        boardFlow.map { board ->
            board.notes.filter { note ->
                !note.isTrashed && note.status != TaskStatus.Completed &&
                    note.date?.let { it >= startDate && it <= endDateInclusive } == true
            }
        }

    override suspend fun getTask(taskId: Long): TaskItem? = 
        boardFlow.value.tasks.find { it.id == taskId }

    override suspend fun getNote(noteId: Long): NoteItem? = 
        boardFlow.value.notes.find { it.id == noteId }

    override fun observeDailyPlans(startDate: LocalDate?, endDate: LocalDate?): Flow<List<DailyPlan>> =
        dailyPlansFlow.map { plans ->
            if (startDate != null && endDate != null) {
                plans.filter { it.date in startDate..endDate }
            } else {
                plans
            }
        }

    override fun observeJournalEntries(): Flow<List<JournalEntry>> = journalEntriesFlow

    fun setJournalEntries(entries: List<JournalEntry>) {
        journalEntriesFlow.value = entries
    }

    override suspend fun addJournalEntry(input: JournalEntryWriteInput): Long {
        val id = nextJournalEntryId++
        addedJournalEntries.add(input)
        val entry = JournalEntry(
            id = id,
            dateEpochDays = input.date.toEpochDays().toInt(),
            label = input.label,
            content = input.content,
            moods = input.moods,
            tags = input.tagIds.mapNotNull { tagId -> currentBoard.tags.find { it.id == tagId } },
            createdTimeMinutes = 0
        )
        journalEntriesFlow.update { it + entry }
        return id
    }

    fun currentJournalEntry(id: Long) = journalEntriesFlow.value.find { it.id == id }

    override suspend fun updateJournalEntry(entryId: Long, input: JournalEntryWriteInput) {
        updatedJournalEntries.add(entryId to input)
        journalEntriesFlow.update { list ->
            list.map { entry ->
                if (entry.id == entryId) {
                    entry.copy(
                        label = input.label,
                        content = input.content,
                        moods = input.moods,
                        tags = input.tagIds.mapNotNull { tagId -> currentBoard.tags.find { it.id == tagId } }
                    )
                } else entry
            }
        }
    }

    override suspend fun deleteJournalEntry(entryId: Long) {
        deletedJournalEntryIds.add(entryId)
        journalEntriesFlow.update { it.filter { entry -> entry.id != entryId } }
    }

    fun setDailyPlans(plans: List<DailyPlan>) {
        dailyPlansFlow.value = plans
    }

    fun setDayReviews(reviews: List<PeriodReview>) {
        periodReviewsFlow.value = reviews
    }

    override suspend fun addList(input: ListWriteInput): Long {
        val id = nextListId++
        addedLists.add(input)
        val newList = ListItem(id, input.title, input.icon, input.color, 0)
        boardFlow.update { it.copy(lists = it.lists + newList) }
        return id
    }

    override suspend fun updateList(listId: Long, input: ListWriteInput) {
        updatedLists.add(listId to input)
        boardFlow.update { board ->
            board.copy(lists = board.lists.map { if (it.id == listId) it.copy(title = input.title, icon = input.icon, color = input.color) else it })
        }
    }

    override suspend fun deleteList(listId: Long) {
        deletedLists.add(listId)
        val inbox = currentBoard.lists.find { it.title == "Inbox" } ?: currentBoard.lists.firstOrNull { it.id != listId }
        boardFlow.update { board ->
            board.copy(
                lists = board.lists.filter { it.id != listId },
                tasks = board.tasks.map { if (it.list?.id == listId) it.copy(list = inbox) else it },
                notes = board.notes.map { if (it.list?.id == listId) it.copy(list = inbox) else it }
            )
        }
    }

    override suspend fun addTag(input: TagWriteInput): Long {
        val id = nextTagId++
        addedTags.add(input)
        val newTag = TagItem(id, input.name, input.color, 0)
        boardFlow.update { it.copy(tags = it.tags + newTag) }
        return id
    }

    override suspend fun updateTag(tagId: Long, input: TagWriteInput) {
        updatedTags.add(tagId to input)
        boardFlow.update { board ->
            board.copy(tags = board.tags.map { if (it.id == tagId) it.copy(name = input.name, color = input.color) else it })
        }
    }

    override suspend fun updateTagSortOrder(tagId: Long, sortOrder: Int) {
        updatedTagSortOrders.add(tagId to sortOrder)
        boardFlow.update { board ->
            board.copy(tags = board.tags.map { if (it.id == tagId) it.copy(sortOrder = sortOrder) else it })
        }
    }

    override suspend fun deleteTag(tagId: Long) {
        deletedTags.add(tagId)
        boardFlow.update { board ->
            board.copy(
                tags = board.tags.filter { it.id != tagId },
                tasks = board.tasks.map { it.copy(tags = it.tags.filter { t -> t.id != tagId }) },
                notes = board.notes.map { it.copy(tags = it.tags.filter { t -> t.id != tagId }) }
            )
        }
    }

    override suspend fun isTagNameTaken(name: String, excludeTagId: Long?): Boolean =
        currentBoard.tags.any { it.name == name && it.id != excludeTagId }

    override suspend fun addTask(input: TaskWriteInput): Long {
        val id = nextTaskId++
        addedTasks.add(input)
        val newTask = TaskItem(
            id = id,
            list = input.listId?.let { lid -> currentBoard.lists.find { it.id == lid } },
            name = input.name,
            description = input.description,
            subtasks = input.subtasks.mapIndexed { i, s -> SubTaskItem(i.toLong(), id, s.name, s.isCompleted, i) },
            status = input.status,
            priority = input.priority,
            type = input.type,
            tags = input.tagIds.mapNotNull { tid -> currentBoard.tags.find { it.id == tid } },
            doDate = input.doDate,
            startTimeMinutes = input.startTimeMinutes,
            endTimeMinutes = input.endTimeMinutes,
            repeatRRule = input.repeatRRule,
            sortOrder = 0,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )
        boardFlow.update { it.copy(tasks = it.tasks + newTask) }
        return id
    }

    override suspend fun updateTask(taskId: Long, input: TaskWriteInput) {
        updatedTasks.add(taskId to input)
        boardFlow.update { board ->
            board.copy(tasks = board.tasks.map {
                if (it.id == taskId) {
                    it.copy(
                        list = input.listId?.let { lid -> board.lists.find { it.id == lid } },
                        name = input.name,
                        description = input.description,
                        subtasks = input.subtasks.mapIndexed { i, s -> SubTaskItem(i.toLong(), taskId, s.name, s.isCompleted, i) },
                        status = input.status,
                        priority = input.priority,
                        type = input.type,
                        tags = input.tagIds.mapNotNull { tid -> board.tags.find { it.id == tid } },
                        doDate = input.doDate,
                        startTimeMinutes = input.startTimeMinutes,
                        endTimeMinutes = input.endTimeMinutes,
                        repeatRRule = input.repeatRRule
                    )
                } else it
            })
        }
    }

    override suspend fun trashTask(taskId: Long) {
        trashedTasks.add(taskId)
        boardFlow.update { board ->
            board.copy(tasks = board.tasks.map { if (it.id == taskId) it.copy(trashedAtMillis = 1L) else it })
        }
    }

    override suspend fun restoreTask(taskId: Long) {
        boardFlow.update { board ->
            board.copy(tasks = board.tasks.map { if (it.id == taskId) it.copy(trashedAtMillis = null) else it })
        }
    }

    override suspend fun completeTask(taskId: Long) {
        boardFlow.update { board ->
            board.copy(tasks = board.tasks.map { if (it.id == taskId) it.copy(status = TaskStatus.Completed) else it })
        }
    }

    override suspend fun updateTaskStatus(taskId: Long, status: TaskStatus) {
        boardFlow.update { board ->
            board.copy(tasks = board.tasks.map { if (it.id == taskId) it.copy(status = status) else it })
        }
    }

    override suspend fun addTaskToDailyPlan(date: LocalDate, task: TaskItem): Long {
        val id = nextDailyPlanItemId++
        addedDailyPlanTasks.add(date to task)
        val newItem = DailyPlanItem(
            id = id,
            dateEpochDays = date.toEpochDays().toInt(),
            taskId = task.id,
            title = task.name,
            source = DailyPlanItemSource.ExistingTask,
            status = DailyPlanItemStatus.Planned,
            sortOrder = 0,
            addedAtMillis = 0L,
            tags = task.tags,
            isHabit = task.type == TaskType.Habit
        )
        dailyPlansFlow.update { list ->
            val existing = list.find { it.date == date }
            if (existing != null) {
                list.map { if (it.date == date) it.copy(items = it.items + newItem) else it }
            } else {
                list + DailyPlan(date, listOf(newItem))
            }
        }
        return id
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
        val id = nextDailyPlanItemId++
        val input = DailyPlanItemWriteInput(title, note, source, status, startTimeMinutes, endTimeMinutes, tagIds, label, nestedListItemId)
        addedManualDailyPlanItems.add(input)
        val newItem = DailyPlanItem(
            id = id,
            dateEpochDays = date.toEpochDays().toInt(),
            taskId = taskId,
            nestedListItemId = nestedListItemId,
            title = title,
            note = note,
            source = source,
            status = status,
            sortOrder = 0,
            label = label,
            startTimeMinutes = startTimeMinutes,
            endTimeMinutes = endTimeMinutes,
            addedAtMillis = 0L,
            tags = tagIds.mapNotNull { tid -> currentBoard.tags.find { it.id == tid } },
            carriedFromItemId = carriedFromItemId
        )
        dailyPlansFlow.update { list ->
            val existingPlan = list.find { it.date == date }
            if (existingPlan != null) {
                list.map { if (it.date == date) it.copy(items = it.items + newItem) else it }
            } else {
                list + DailyPlan(date, listOf(newItem))
            }
        }
        return id
    }

    override suspend fun updateDailyPlanItemTime(itemId: Long, startTimeMinutes: Int?, endTimeMinutes: Int?) {
        updatedDailyPlanItemTimes.add(Triple(itemId, startTimeMinutes, endTimeMinutes))
        dailyPlansFlow.update { list ->
            list.map { plan ->
                plan.copy(items = plan.items.map { if (it.id == itemId) it.copy(startTimeMinutes = startTimeMinutes, endTimeMinutes = endTimeMinutes) else it })
            }
        }
    }

    override suspend fun updateDailyPlanItemTimes(updates: List<DailyPlanItemTimeUpdate>) {
        updates.forEach { update -> updateDailyPlanItemTime(update.itemId, update.startTimeMinutes, update.endTimeMinutes) }
    }

    override suspend fun updateDailyPlanItemStatus(itemId: Long, status: DailyPlanItemStatus) {
        statusUpdates.add(itemId to status)
        dailyPlansFlow.update { list ->
            list.map { plan ->
                plan.copy(items = plan.items.map { if (it.id == itemId) it.copy(status = status, completedAtMillis = if (status == DailyPlanItemStatus.Done) 1L else null) else it })
            }
        }
    }

    override suspend fun updateDailyPlanItemsStatus(itemIds: List<Long>, status: DailyPlanItemStatus) {
        itemIds.forEach { updateDailyPlanItemStatus(it, status) }
    }

    override suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput) {
        updatedDailyPlanItems.add(itemId to input)
        dailyPlansFlow.update { list ->
            list.map { plan ->
                plan.copy(items = plan.items.map { if (it.id == itemId) it.copy(
                    title = input.title,
                    note = input.note,
                    source = input.source,
                    status = input.status,
                    startTimeMinutes = input.startTimeMinutes,
                    endTimeMinutes = input.endTimeMinutes,
                    tags = input.tagIds.mapNotNull { tid -> boardFlow.value.tags.find { it.id == tid } }
                ) else it })
            }
        }
    }

    override suspend fun updateDailyPlanItemTags(itemId: Long, tagIds: List<Long>) {
        dailyPlansFlow.update { list ->
            list.map { plan ->
                plan.copy(items = plan.items.map { if (it.id == itemId) it.copy(
                    tags = tagIds.mapNotNull { tid -> boardFlow.value.tags.find { it.id == tid } }
                ) else it })
            }
        }
    }

    override suspend fun deleteDailyPlanItem(itemId: Long) {
        deletedDailyPlanItemIds.add(itemId)
        dailyPlansFlow.update { list ->
            list.map { plan -> plan.copy(items = plan.items.filter { it.id != itemId }) }
        }
    }

    override suspend fun getDailyPlanItem(itemId: Long): DailyPlanItem? =
        dailyPlansFlow.value.asSequence().flatMap { it.items.asSequence() }.find { it.id == itemId }

    override suspend fun dailyPlanForDate(date: LocalDate): DailyPlan? =
        dailyPlansFlow.value.find { it.date == date }

    override fun observePeriodReviews(): Flow<List<PeriodReview>> = periodReviewsFlow

    override fun observePeriodReviewsInRange(
        startDate: LocalDate?,
        endDateInclusive: LocalDate?
    ): Flow<List<PeriodReview>> =
        periodReviewsFlow.map { reviews ->
            reviews.filter { review ->
                val start = startDate?.toEpochDays()?.toInt()
                val end = endDateInclusive?.toEpochDays()?.toInt()
                (start == null || review.periodStartEpochDays >= start) &&
                    (end == null || review.periodStartEpochDays <= end)
            }
        }

    override suspend fun periodReviewFor(period: Period, date: LocalDate): PeriodReview? =
        periodReviewsFlow.value.find { it.period == period && it.periodStartEpochDays == date.toEpochDays().toInt() }

    override suspend fun savePeriodReview(review: PeriodReview) {
        periodReviewsFlow.update { list ->
            val existingIndex = list.indexOfFirst { 
                (it.id != 0L && it.id == review.id) || 
                (it.period == review.period && it.periodStartEpochDays == review.periodStartEpochDays)
            }
            if (existingIndex >= 0) {
                list.toMutableList().apply { 
                    val old = get(existingIndex)
                    set(existingIndex, review.copy(id = if (review.id == 0L) old.id else review.id)) 
                }
            } else {
                val newId = if (review.id == 0L) (list.maxOfOrNull { it.id } ?: 0L) + 1 else review.id
                list + review.copy(id = newId)
            }
        }
    }

    override fun observeDailyReflectStats(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): Flow<List<DailyReflectStat>> =
        combine(dailyPlansFlow, journalEntriesFlow) { plans, journals ->
            val itemsByDate = plans.groupBy({ it.date }) { plan -> plan.items }.mapValues { it.value.flatten() }
            (itemsByDate.keys + journals.map { LocalDate.fromEpochDays(it.dateEpochDays) })
                .distinct()
                .filter { it in startDate..endDateInclusive }
                .sorted()
                .map { date ->
                    val epoch = date.toEpochDays().toInt()
                    val items = itemsByDate[date].orEmpty()
                    DailyReflectStat(
                        dateEpochDays = epoch,
                        plannedItemCount = items.count { it.status == DailyPlanItemStatus.Planned && it.source.isActionableDigestSource() },
                        doneItemCount = items.count { it.status == DailyPlanItemStatus.Done && it.source.isActionableDigestSource() },
                        doneMinutes = items.filter { it.status == DailyPlanItemStatus.Done }.sumOf { it.workMinutes() },
                        journalCount = journals.count { it.dateEpochDays == epoch }
                    )
                }
        }

    override fun observeDailyTagRollups(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): Flow<List<DailyTagRollup>> =
        dailyPlansFlow.map { plans ->
            plans.asSequence()
                .filter { it.date in startDate..endDateInclusive }
                .flatMap { plan ->
                    plan.items.asSequence().flatMap { item ->
                        item.tags.asSequence().map { Triple(plan.date, item, it) }
                    }
                }
                .filter { (_, item, _) -> item.status == DailyPlanItemStatus.Done }
                .groupBy({ (date, _, tag) -> date.toEpochDays().toInt() to tag.id })
                .map { (_, rows) ->
                    val tag = rows.first().third
                    val items = rows.map { it.second }
                    DailyTagRollup(
                        dateEpochDays = rows.first().first.toEpochDays().toInt(),
                        tagId = tag.id,
                        tagName = tag.name,
                        tagColor = tag.color,
                        doneCount = items.size,
                        doneMinutes = items.sumOf { it.workMinutes() }
                    )
                }
                .toList()
        }

    override fun observeHabitDailyRollups(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): Flow<List<HabitDailyRollup>> =
        dailyPlansFlow.map { plans ->
            plans.asSequence()
                .filter { it.date in startDate..endDateInclusive }
                .flatMap { plan ->
                    plan.items.asSequence()
                        .filter { it.isHabit && it.status == DailyPlanItemStatus.Done }
                        .map { HabitDailyRollup(
                            dateEpochDays = plan.date.toEpochDays().toInt(),
                            habitKey = habitKeyFor(it),
                            title = it.title,
                            doneMinutes = it.workMinutes()
                        ) }
                }
                .toList()
        }

    override fun observeDoneItemSummaries(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): Flow<List<DoneItemSummary>> =
        dailyPlansFlow.map { plans ->
            plans.asSequence()
                .filter { it.date in startDate..endDateInclusive }
                .flatMap { it.items.asSequence() }
                .filter { it.status == DailyPlanItemStatus.Done }
                .sortedByDescending { it.completedAtMillis }
                .map {
                    DoneItemSummary(
                        id = it.id,
                        dateEpochDays = it.dateEpochDays,
                        title = it.title,
                        note = it.note,
                        sourceName = it.source.name,
                        startTimeMinutes = it.startTimeMinutes,
                        endTimeMinutes = it.endTimeMinutes,
                        completedAtMillis = it.completedAtMillis
                    )
                }
                .toList()
        }

    override fun observeJournalEntriesInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): Flow<List<JournalEntry>> =
        journalEntriesFlow.map { entries ->
            entries.filter { LocalDate.fromEpochDays(it.dateEpochDays) in startDate..endDateInclusive }
        }

    override fun observeJournalEntriesFiltered(
        moodEmojis: List<String>,
        searchText: String?,
        tagId: Long?
    ): Flow<List<JournalEntry>> =
        journalEntriesFlow.map { entries ->
            entries.filter { entry ->
                val moodMatches = moodEmojis.isEmpty() || entry.moods.any { it in moodEmojis }
                val searchMatches = searchText == null ||
                    entry.content.contains(searchText, ignoreCase = true) ||
                    (entry.label?.contains(searchText, ignoreCase = true) == true)
                val tagMatches = tagId == null || entry.tags.any { it.id == tagId }
                moodMatches && searchMatches && tagMatches
            }.sortedWith(
                compareByDescending<JournalEntry> { it.dateEpochDays }.thenBy { it.createdTimeMinutes }
            )
        }

    override suspend fun rebuildReflectStats() = Unit

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
    ): DayCloseCommitResult {
        val handledIds = markDoneItemIds + carryItemIds + dropItemIds
        
        dailyPlansFlow.update { plans ->
            plans.map { plan ->
                plan.copy(items = plan.items.map { item ->
                    if (item.id in handledIds) {
                        item.copy(
                            handledAtMillis = nowMillis,
                            status = if (item.id in markDoneItemIds) DailyPlanItemStatus.Done else item.status,
                            completedAtMillis = if (item.id in markDoneItemIds) nowMillis else item.completedAtMillis
                        )
                    } else item
                })
            }
        }

        markDoneItemIds.forEach { statusUpdates.add(it to DailyPlanItemStatus.Done) }
        markedHandledItemIds.addAll(handledIds)
        
        var carriedCount = 0
        var skippedCount = 0
        carryItemIds.forEach { itemId ->
            val source = getDailyPlanItem(itemId) ?: return@forEach
            val newId = copyDailyPlanItemToDate(source, targetDate, true)
            if (newId != null) carriedCount++ else skippedCount++
        }

        savePeriodReview(
            PeriodReview(
                period = Period.Day,
                periodStartEpochDays = date.toEpochDays().toInt(),
                periodEndEpochDays = date.toEpochDays().toInt() + 1,
                content = winNote?.trim().orEmpty(),
                intentNext = tomorrowGoal?.trim()?.takeIf { it.isNotEmpty() },
                source = ReviewSource.Manual,
                status = ReviewStatus.Complete,
                completedAtMillis = nowMillis,
                editedAtMillis = nowMillis
            )
        )
        
        return DayCloseCommitResult(carriedCount = carriedCount, skippedCount = skippedCount)
    }

    private fun markDailyPlanItemsHandled(ids: List<Long>) {
        ids.forEach { id ->
            if (!markedHandledItemIds.contains(id)) {
                markedHandledItemIds.add(id)
            }
        }
        val now = 1L
        dailyPlansFlow.update { plans ->
            plans.map { plan ->
                plan.copy(items = plan.items.map { item ->
                    if (item.id in ids) {
                        item.copy(handledAtMillis = item.handledAtMillis ?: now)
                    } else item
                })
            }
        }
    }

    override suspend fun copyDailyPlanItemToDate(
        source: DailyPlanItem,
        targetDate: LocalDate,
        clearTimes: Boolean
    ): Long? {
        val alreadyPresent = dailyPlansFlow.value.find { it.date == targetDate }?.items?.any { item ->
            (source.taskId != null && item.taskId == source.taskId) ||
                (item.carriedFromItemId != null && item.carriedFromItemId == source.id)
        } ?: false
        if (alreadyPresent) return null

        val newItemId = addDailyPlanItem(
            date = targetDate,
            title = source.title,
            note = source.note,
            startTimeMinutes = if (clearTimes) null else source.startTimeMinutes,
            endTimeMinutes = if (clearTimes) null else source.endTimeMinutes,
            source = source.source,
            status = DailyPlanItemStatus.Planned,
            tagIds = source.tags.map { it.id },
            label = source.label,
            taskId = source.taskId,
            nestedListItemId = source.nestedListItemId,
            carriedFromItemId = source.id
        )
        val newItem = getDailyPlanItem(newItemId)!!
        copiedDailyPlanItems.add(newItem)
        markDailyPlanItemsHandled(listOf(source.id))
        return newItemId
    }

    override suspend fun countDoneDailyPlanItemsForTaskOnDate(taskId: Long, dateEpochDays: Int, excludeItemId: Long): Int = 0

    override suspend fun addNote(input: NoteWriteInput): Long {
        val id = nextTaskId++ // sharing ID space
        val newNote = NoteItem(
            id = id,
            list = input.listId?.let { lid -> currentBoard.lists.find { it.id == lid } },
            title = input.title,
            content = input.content,
            status = input.status,
            tags = input.tagIds.mapNotNull { tid -> currentBoard.tags.find { it.id == tid } },
            date = input.date,
            startTimeMinutes = input.startTimeMinutes,
            createdAtMillis = 0L,
            editedAtMillis = 0L,
            sortOrder = 0
        )
        boardFlow.update { it.copy(notes = it.notes + newNote) }
        return id
    }

    override suspend fun updateNote(noteId: Long, input: NoteWriteInput) {
        boardFlow.update { board ->
            board.copy(notes = board.notes.map { if (it.id == noteId) it.copy(
                list = input.listId?.let { lid -> board.lists.find { it.id == lid } },
                title = input.title,
                content = input.content,
                status = input.status,
                tags = input.tagIds.mapNotNull { tid -> board.tags.find { it.id == tid } },
                date = input.date,
                startTimeMinutes = input.startTimeMinutes
            ) else it })
        }
    }

    override suspend fun completeNote(noteId: Long) {
        boardFlow.update { board ->
            board.copy(notes = board.notes.map { if (it.id == noteId) it.copy(status = TaskStatus.Completed) else it })
        }
    }

    override suspend fun updateNoteStatus(noteId: Long, status: TaskStatus) {
        boardFlow.update { board ->
            board.copy(notes = board.notes.map { if (it.id == noteId) it.copy(status = status) else it })
        }
    }

    override suspend fun trashNote(noteId: Long) {
        boardFlow.update { board ->
            board.copy(notes = board.notes.map { if (it.id == noteId) it.copy(trashedAtMillis = 1L) else it })
        }
    }

    override suspend fun restoreNote(noteId: Long) {
        boardFlow.update { board ->
            board.copy(notes = board.notes.map { if (it.id == noteId) it.copy(trashedAtMillis = null) else it })
        }
    }

    override suspend fun moveTask(taskId: Long, listId: Long, sectionId: Long?, sortOrder: Int, isPinned: Boolean) {
        boardFlow.update { board ->
            board.copy(
                tasks = board.tasks.map {
                    if (it.id == taskId) it.copy(sectionId = sectionId, sortOrder = sortOrder, isPinned = isPinned) else it
                }
            )
        }
    }

    override suspend fun moveNote(noteId: Long, listId: Long, sectionId: Long?, sortOrder: Int, isPinned: Boolean) {
        boardFlow.update { board ->
            board.copy(
                notes = board.notes.map {
                    if (it.id == noteId) it.copy(sectionId = sectionId, sortOrder = sortOrder, isPinned = isPinned) else it
                }
            )
        }
    }


    override suspend fun addSection(listId: Long, title: String, color: String): Long {
        val id = nextSectionId++
        addedSections.add(Triple(listId, title, color))
        boardFlow.update { board ->
            board.copy(
                lists = board.lists.map { list ->
                    if (list.id == listId) {
                        list.copy(
                            sections = list.sections + com.checkit.domain.ListSection(
                                id = id,
                                listId = listId,
                                title = title,
                                color = color,
                                sortOrder = list.sections.size
                            )
                        )
                    } else list
                }
            )
        }
        return id
    }

    override suspend fun updateSection(sectionId: Long, title: String, color: String, sortOrder: Int) {
        val section = com.checkit.domain.ListSection(sectionId, 0L, title, color, sortOrder)
        updatedSections.add(section)
        boardFlow.update { board ->
            board.copy(
                lists = board.lists.map { list ->
                    list.copy(
                        sections = list.sections.map { s ->
                            if (s.id == sectionId) section.copy(listId = s.listId) else s
                        }
                    )
                }
            )
        }
    }

    override suspend fun deleteSection(sectionId: Long) {
        deletedSections.add(sectionId)
        boardFlow.update { board ->
            board.copy(
                lists = board.lists.map { list ->
                    list.copy(sections = list.sections.filter { it.id != sectionId })
                }
            )
        }
    }

    override fun observeNestedDocuments(): Flow<List<NestedDocument>> = MutableStateFlow(emptyList())
    override fun observeTags(): Flow<List<TagItem>> = boardFlow.map { it.tags }
    override fun observeNestedDocumentTree(documentId: Long): Flow<NestedDocumentTree> = MutableStateFlow(NestedDocumentTree(NestedDocument(0, "", 0, 0), emptyList()))
    override suspend fun addNestedDocument(title: String): Long = 0
    override suspend fun renameNestedDocument(documentId: Long, title: String) {}
    override suspend fun deleteNestedDocument(documentId: Long) {}
    override suspend fun addNestedItem(documentId: Long, parentId: Long?, text: String, position: Int?): Long = 0
    override suspend fun updateNestedItemText(itemId: Long, text: String) {}
    override suspend fun updateNestedItemNote(itemId: Long, note: String?) {}
    override suspend fun updateNestedItemFormatting(itemId: Long, textStyle: NestedTextStyle, textColor: NestedColorToken, backgroundColor: NestedColorToken) {}
    override suspend fun updateNestedItemPriority(itemId: Long, priority: TaskPriority) {}
    override suspend fun updateNestedItemDateRange(itemId: Long, startDate: LocalDate?, endDate: LocalDate?) {}
    override suspend fun updateNestedItemTags(itemId: Long, tagIds: List<Long>) {}
    override suspend fun updateNestedItemMetricSettings(itemId: Long, actualMinutes: Int, metricRollupPolicy: MetricRollupPolicy, showTrackedMinutes: Boolean) {}
    override suspend fun replaceNestedManualMetrics(itemId: Long, metrics: List<NestedManualMetric>) {}
    override suspend fun setNestedItemCheckboxEnabled(itemId: Long, checkboxEnabled: Boolean) {}
    override suspend fun setNestedItemsChecked(itemIds: List<Long>, checked: Boolean) {}
    override suspend fun toggleNestedItemCollapsed(itemId: Long) {}
    override suspend fun moveNestedItems(moves: List<NestedItemMove>) {}
    override suspend fun deleteNestedItems(itemIds: List<Long>) {}
}

private fun DailyPlanItemSource.isActionableDigestSource(): Boolean =
    this == DailyPlanItemSource.MyDayTask ||
        this == DailyPlanItemSource.MyDayReminder ||
        this == DailyPlanItemSource.ExistingTask

private fun habitKeyFor(item: DailyPlanItem): String =
    item.taskId?.let { "task:$it" } ?: "title:${item.title.trim().lowercase()}"
