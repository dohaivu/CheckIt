package com.checkit.ui.tasks

import com.checkit.data.CheckItRepository
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.data.GoalWriteInput
import com.checkit.data.JournalEntryWriteInput
import com.checkit.data.KeyResultWriteInput
import com.checkit.data.NoteWriteInput
import com.checkit.data.SettingsRepository
import com.checkit.data.ListWriteInput
import com.checkit.data.ObjectiveWriteInput
import com.checkit.data.PlanPriorityWriteInput
import com.checkit.data.PlanPriorityTaskLink
import com.checkit.data.PlanPriorityDailyPlanItemLink
import com.checkit.data.TagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.data.UserSettings
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayCloseCommitResult
import com.checkit.domain.Goal
import com.checkit.domain.JournalEntry
import com.checkit.domain.KeyResult
import com.checkit.domain.ListItem
import com.checkit.domain.PeriodPlan
import com.checkit.domain.PeriodReview
import com.checkit.domain.PlanPeriod
import com.checkit.domain.PlanPriority
import com.checkit.domain.ReviewPeriod
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskReminder
import com.checkit.domain.TagItem
import com.checkit.domain.hasEndTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.update

internal class FakeCheckItRepository(
    initialBoard: TaskBoard = TaskBoard()
) : CheckItRepository {
    private val boardFlow = MutableStateFlow(initialBoard)
    val addedObjectives = mutableListOf<ObjectiveWriteInput>()
    val addedGoals = mutableListOf<GoalWriteInput>()
    val updatedGoals = mutableListOf<Pair<Long, GoalWriteInput>>()
    val deletedGoals = mutableListOf<Long>()
    val addedKeyResults = mutableListOf<KeyResultWriteInput>()
    val updatedKeyResults = mutableListOf<Pair<Long, KeyResultWriteInput>>()
    val deletedKeyResults = mutableListOf<Long>()
    val updatedObjectives = mutableListOf<Pair<Long, ObjectiveWriteInput>>()
    val deletedObjectives = mutableListOf<Long>()
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
    val addedDailyPlanTasks = mutableListOf<Pair<LocalDate, TaskItem>>()
    val addedManualDailyPlanItems = mutableListOf<DailyPlanItemWriteInput>()
    val updatedDailyPlanItems = mutableListOf<Pair<Long, DailyPlanItemWriteInput>>()
    val updatedDailyPlanItemTimes = mutableListOf<Triple<Long, Int?, Int?>>()
    val adjustedKeyResults = mutableListOf<Pair<Long, Double>>()
    val currentBoard: TaskBoard get() = boardFlow.value

    var lastAssignedObjectiveId: Long = 0L
        private set
    var lastAssignedTagId: Long = 0L
        private set

    private var nextGoalId: Long = 50L
    private var nextObjectiveId: Long = 100L
    private var nextListId: Long = 200L
    private var nextKeyResultId: Long = 300L
    private var nextTagId: Long = 500L
    private var nextTaskId: Long = 1_000L
    private var nextDailyPlanItemId: Long = 10_000L

    private val dailyPlansFlow = MutableStateFlow<List<DailyPlan>>(emptyList())
    val copiedDailyPlanItems = mutableListOf<DailyPlanItem>()
    val statusUpdates = mutableListOf<Pair<Long, DailyPlanItemStatus>>()
    val markedHandledItemIds = mutableListOf<Long>()
    private val periodReviewsFlow = MutableStateFlow<List<PeriodReview>>(emptyList())

    private val journalEntriesFlow = MutableStateFlow<List<JournalEntry>>(emptyList())
    val addedJournalEntries = mutableListOf<JournalEntryWriteInput>()
    val updatedJournalEntries = mutableListOf<Pair<Long, JournalEntryWriteInput>>()
    val deletedJournalEntryIds = mutableListOf<Long>()
    private var nextJournalEntryId: Long = 20_000L

    private val periodPlansFlow = MutableStateFlow<List<PeriodPlan>>(emptyList())
    private val planPrioritiesFlow = MutableStateFlow<List<PlanPriority>>(emptyList())
    private val planTaskLinksFlow = MutableStateFlow<List<PlanPriorityTaskLink>>(emptyList())
    private val planDailyLinksFlow = MutableStateFlow<List<PlanPriorityDailyPlanItemLink>>(emptyList())
    private var nextPeriodPlanId: Long = 30_000L
    private var nextPlanPriorityId: Long = 31_000L
    val addedPlanPriorities = mutableListOf<PlanPriorityWriteInput>()
    val updatedPlanPriorities = mutableListOf<Pair<Long, PlanPriorityWriteInput>>()
    val deletedPlanPriorityIds = mutableListOf<Long>()
    val linkedTasks = mutableListOf<Pair<Long, Long>>()
    val unlinkedTasks = mutableListOf<Pair<Long, Long>>()
    val linkedDailyPlanItems = mutableListOf<Pair<Long, Long>>()
    val unlinkedDailyPlanItems = mutableListOf<Pair<Long, Long>>()

    override fun observeTaskBoard(): Flow<TaskBoard> = boardFlow
    override fun observeDailyPlans(): Flow<List<DailyPlan>> = dailyPlansFlow

    override fun observeJournalEntries(): Flow<List<JournalEntry>> = journalEntriesFlow

    fun setJournalEntries(entries: List<JournalEntry>) {
        journalEntriesFlow.value = entries
    }

    override suspend fun addJournalEntry(input: JournalEntryWriteInput): Long {
        addedJournalEntries.add(input)
        val id = nextJournalEntryId++
        val entry = JournalEntry(
            id = id,
            dateEpochDays = input.date.toEpochDays().toInt(),
            context = input.context,
            content = input.content,
            moods = input.moods,
            tags = boardFlow.value.tags.filter { it.id in input.tagIds },
            createdTimeMinutes = 1,
            attachments = input.attachments
        )
        journalEntriesFlow.update { it + entry }
        return id
    }

    fun currentJournalEntry(entryId: Long): JournalEntry? =
        journalEntriesFlow.value.firstOrNull { it.id == entryId }

    override suspend fun updateJournalEntry(entryId: Long, input: JournalEntryWriteInput) {
        updatedJournalEntries.add(entryId to input)
        journalEntriesFlow.update { entries ->
            entries.map { entry ->
                if (entry.id == entryId) {
                    entry.copy(
                        dateEpochDays = input.date.toEpochDays().toInt(),
                        context = input.context,
                        content = input.content,
                        moods = input.moods,
                        tags = boardFlow.value.tags.filter { tag -> tag.id in input.tagIds },
                        attachments = input.attachments
                    )
                } else {
                    entry
                }
            }
        }
    }

    override suspend fun deleteJournalEntry(entryId: Long) {
        deletedJournalEntryIds.add(entryId)
        journalEntriesFlow.update { entries -> entries.filterNot { it.id == entryId } }
    }

    fun setDailyPlans(plans: List<DailyPlan>) {
        dailyPlansFlow.value = plans
    }

    fun setDayReviews(records: List<PeriodReview>) {
        periodReviewsFlow.value = records
    }

    override suspend fun addGoal(input: GoalWriteInput): Long {
        addedGoals.add(input)
        val id = nextGoalId++
        boardFlow.update { board ->
            board.copy(
                goals = board.goals + Goal(
                    id = id,
                    title = input.title,
                    color = input.color,
                    icon = input.icon,
                    sortOrder = board.goals.size
                )
            )
        }
        return id
    }

    override suspend fun updateGoal(goalId: Long, input: GoalWriteInput) {
        updatedGoals.add(goalId to input)
        boardFlow.update { board ->
            board.copy(
                goals = board.goals.map { goal ->
                    if (goal.id == goalId) {
                        goal.copy(title = input.title, color = input.color, icon = input.icon)
                    } else {
                        goal
                    }
                }
            )
        }
    }

    override suspend fun deleteGoal(goalId: Long) {
        deletedGoals.add(goalId)
        boardFlow.update { board ->
            board.copy(
                goals = board.goals.filterNot { it.id == goalId },
                objectives = board.objectives.filterNot { it.goalId == goalId }
            )
        }
    }

    override suspend fun addObjective(input: ObjectiveWriteInput): Long {
        addedObjectives.add(input)
        val id = nextObjectiveId++
        lastAssignedObjectiveId = id
        boardFlow.update { board ->
            board.copy(
                objectives = board.objectives + Objective(
                    id = id,
                    goalId = input.goalId,
                    name = input.name,
                    color = input.color,
                    icon = input.icon,
                    sortOrder = board.objectives.size
                )
            )
        }
        return id
    }

    override suspend fun updateObjective(objectiveId: Long, input: ObjectiveWriteInput) {
        updatedObjectives.add(objectiveId to input)
        boardFlow.update { board ->
            board.copy(
                objectives = board.objectives.map { list ->
                    if (list.id == objectiveId) {
                        list.copy(goalId = input.goalId, name = input.name, color = input.color, icon = input.icon)
                    } else {
                        list
                    }
                }
            )
        }
    }

    override suspend fun deleteObjective(objectiveId: Long) {
        deletedObjectives.add(objectiveId)
        boardFlow.update { board ->
            board.copy(
                objectives = board.objectives.filterNot { it.id == objectiveId },
                keyResults = board.keyResults.filterNot { it.objectiveId == objectiveId }
            )
        }
    }

    override suspend fun addList(input: ListWriteInput): Long {
        addedLists.add(input)
        val id = nextListId++
        boardFlow.update { board ->
            board.copy(
                lists = board.lists + ListItem(
                    id = id,
                    title = input.title,
                    color = input.color,
                    icon = input.icon,
                    sortOrder = board.lists.size
                )
            )
        }
        return id
    }

    override suspend fun updateList(listId: Long, input: ListWriteInput) {
        updatedLists.add(listId to input)
        boardFlow.update { board ->
            board.copy(
                lists = board.lists.map { list ->
                    if (list.id == listId) {
                        list.copy(title = input.title, color = input.color, icon = input.icon)
                    } else {
                        list
                    }
                }
            )
        }
    }

    override suspend fun deleteList(listId: Long) {
        deletedLists.add(listId)
        boardFlow.update { board ->
            val inbox = board.lists.firstOrNull { it.title == "Inbox" } ?: ListItem.None
            board.copy(
                lists = board.lists.filterNot { it.id == listId },
                tasks = board.tasks.map { task ->
                    if (task.list.id == listId) task.copy(list = inbox) else task
                },
                notes = board.notes.map { note ->
                    if (note.list.id == listId) note.copy(list = inbox) else note
                }
            )
        }
    }

    override suspend fun addKeyResult(input: KeyResultWriteInput): Long {
        addedKeyResults.add(input)
        val id = nextKeyResultId++
        boardFlow.update { board ->
            board.copy(
                keyResults = board.keyResults + KeyResult(
                    id = id,
                    objectiveId = input.objectiveId,
                    title = input.title,
                    targetValue = input.targetValue,
                    currentValue = input.currentValue,
                    unit = input.unit,
                    sortOrder = board.keyResults.count { it.objectiveId == input.objectiveId }
                )
            )
        }
        return id
    }

    override suspend fun updateKeyResult(keyResultId: Long, input: KeyResultWriteInput) {
        updatedKeyResults.add(keyResultId to input)
        boardFlow.update { board ->
            board.copy(
                keyResults = board.keyResults.map { keyResult ->
                    if (keyResult.id == keyResultId) {
                        keyResult.copy(
                            objectiveId = input.objectiveId,
                            title = input.title,
                            targetValue = input.targetValue,
                            currentValue = input.currentValue,
                            unit = input.unit
                        )
                    } else {
                        keyResult
                    }
                }
            )
        }
    }

    override suspend fun deleteKeyResult(keyResultId: Long) {
        deletedKeyResults.add(keyResultId)
        boardFlow.update { board ->
            board.copy(
                keyResults = board.keyResults.filterNot { it.id == keyResultId },
                tasks = board.tasks.map { task ->
                    if (task.keyResult?.id == keyResultId) task.copy(keyResult = null) else task
                }
            )
        }
    }

    override suspend fun addTag(input: TagWriteInput): Long {
        addedTags.add(input)
        val id = nextTagId++
        lastAssignedTagId = id
        boardFlow.update { board ->
            board.copy(
                tags = board.tags + TagItem(
                    id = id,
                    name = input.name,
                    color = input.color
                )
            )
        }
        return id
    }

    override suspend fun updateTag(tagId: Long, input: TagWriteInput) {
        updatedTags.add(tagId to input)
        boardFlow.update { board ->
            board.copy(
                tags = board.tags.map { tag ->
                    if (tag.id == tagId) {
                        tag.copy(name = input.name, color = input.color)
                    } else {
                        tag
                    }
                }
            )
        }
    }

    override suspend fun updateTagSortOrder(tagId: Long, sortOrder: Int) {
        updatedTagSortOrders.add(tagId to sortOrder)
        boardFlow.update { board ->
            board.copy(tags = board.tags.map { tag ->
                if (tag.id == tagId) tag.copy(sortOrder = sortOrder) else tag
            })
        }
    }

    override suspend fun deleteTag(tagId: Long) {
        deletedTags.add(tagId)
        boardFlow.update { board ->
            board.copy(
                tags = board.tags.filterNot { it.id == tagId },
                tasks = board.tasks.map { task ->
                    task.copy(tags = task.tags.filterNot { it.id == tagId })
                },
                notes = board.notes.map { note ->
                    note.copy(tags = note.tags.filterNot { it.id == tagId })
                }
            )
        }
    }

    override suspend fun isTagNameTaken(name: String, excludeTagId: Long?): Boolean =
        boardFlow.value.tags.any { tag ->
            tag.name.equals(name, ignoreCase = false) && tag.id != excludeTagId
        }

    override suspend fun addTask(input: TaskWriteInput): Long {
        addedTasks.add(input)
        val id = nextTaskId++
        val priority = input.planPriorityId?.let { priorityId ->
            planPrioritiesFlow.value.firstOrNull { it.id == priorityId }
        }
        boardFlow.update { board ->
            board.copy(
                tasks = board.tasks + input.toTaskItem(
                    taskId = id,
                    sortOrder = board.tasks.size,
                    planPriority = priority
                )
            )
        }
        if (priority != null) {
            planTaskLinksFlow.update { links ->
                links.filterNot { it.taskId == id } +
                    PlanPriorityTaskLink(priority.id, id, 0)
            }
        }
        return id
    }

    override suspend fun updateTask(taskId: Long, input: TaskWriteInput) {
        updatedTasks.add(taskId to input)
        val priority = input.planPriorityId?.let { priorityId ->
            planPrioritiesFlow.value.firstOrNull { it.id == priorityId }
        }
        boardFlow.update { board ->
            board.copy(
                tasks = board.tasks.map { task ->
                    if (task.id == taskId) {
                        input.toTaskItem(
                            taskId = taskId,
                            sortOrder = task.sortOrder,
                            planPriority = priority,
                            createdAtMillis = task.createdAtMillis,
                            updatedAtMillis = task.updatedAtMillis + 1
                        )
                    } else {
                        task
                    }
                }
            )
        }
        if (priority != null) {
            planTaskLinksFlow.update { links ->
                links.filterNot { it.taskId == taskId } +
                    PlanPriorityTaskLink(priority.id, taskId, 0)
            }
        } else {
            planTaskLinksFlow.update { links ->
                links.filterNot { it.taskId == taskId }
            }
        }
    }
    override suspend fun trashTask(taskId: Long) = Unit
    override suspend fun restoreTask(taskId: Long) = Unit
    override suspend fun completeTask(taskId: Long) = Unit
    override suspend fun openTask(taskId: Long) = Unit
    override suspend fun completeNote(noteId: Long) = Unit
    override suspend fun openNote(noteId: Long) = Unit
    override suspend fun addTaskToDailyPlan(date: LocalDate, task: TaskItem): Long {
        addedDailyPlanTasks.add(date to task)
        val itemId = nextDailyPlanItemId++
        val status = if (task.status == com.checkit.domain.TaskStatus.Completed) {
            DailyPlanItemStatus.Done
        } else {
            DailyPlanItemStatus.Planned
        }
        val item = DailyPlanItem(
            id = itemId,
            dateEpochDays = date.toEpochDays().toInt(),
            taskId = task.id,
            title = task.name,
            source = DailyPlanItemSource.ExistingTask,
            status = status,
            tags = task.tags,
            isHabit = task.type == com.checkit.domain.TaskType.Habit,
            sortOrder = task.sortOrder,
            startTimeMinutes = task.startTimeMinutes,
            endTimeMinutes = task.endTimeMinutes,
            addedAtMillis = 0L,
            completedAtMillis = if (status == DailyPlanItemStatus.Done) 1L else null
        )
        dailyPlansFlow.update { plans ->
            val existing = plans.firstOrNull { it.date == date }
            if (existing == null) {
                plans + DailyPlan(date = date, items = listOf(item))
            } else {
                plans.map { plan ->
                    if (plan.date == date) plan.copy(items = plan.items + item) else plan
                }
            }
        }
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
        val newId = nextDailyPlanItemId++
        val item = DailyPlanItem(
            id = newId,
            dateEpochDays = date.toEpochDays().toInt(),
            title = title,
            note = note,
            source = source,
            status = status,
            sortOrder = 0,
            startTimeMinutes = startTimeMinutes,
            endTimeMinutes = if (source.hasEndTime()) endTimeMinutes else null,
            addedAtMillis = 0L,
            completedAtMillis = if (status == DailyPlanItemStatus.Done) 1L else null
        )
        addedManualDailyPlanItems.add(
            DailyPlanItemWriteInput(
                title = title,
                note = note,
                source = source,
                status = status,
                startTimeMinutes = startTimeMinutes,
                endTimeMinutes = endTimeMinutes,
                tagIds = tagIds
            )
        )
        dailyPlansFlow.update { plans ->
            val existing = plans.firstOrNull { it.date == date }
            if (existing == null) {
                plans + DailyPlan(date = date, items = listOf(item))
            } else {
                plans.map { plan ->
                    if (plan.date == date) plan.copy(items = plan.items + item) else plan
                }
            }
        }
        return newId
    }
    override suspend fun updateDailyPlanItemTime(itemId: Long, startTimeMinutes: Int?, endTimeMinutes: Int?) {
        updatedDailyPlanItemTimes.add(Triple(itemId, startTimeMinutes, endTimeMinutes))
        dailyPlansFlow.update { plans ->
            plans.map { plan ->
                plan.copy(
                    items = plan.items.map { item ->
                        if (item.id == itemId) {
                            item.copy(
                                startTimeMinutes = startTimeMinutes,
                                endTimeMinutes = endTimeMinutes
                            )
                        } else {
                            item
                        }
                    }
                )
            }
        }
    }
    override suspend fun updateDailyPlanItemStatus(itemId: Long, status: DailyPlanItemStatus) {
        statusUpdates.add(itemId to status)
        dailyPlansFlow.update { plans ->
            plans.map { plan ->
                plan.copy(
                    items = plan.items.map { item ->
                        if (item.id == itemId) item.copy(status = status) else item
                    }
                )
            }
        }
    }
    override suspend fun updateDailyPlanItemsStatus(itemIds: List<Long>, status: DailyPlanItemStatus) {
        itemIds.forEach { itemId ->
            statusUpdates.add(itemId to status)
        }
        dailyPlansFlow.update { plans ->
            plans.map { plan ->
                plan.copy(
                    items = plan.items.map { item ->
                        if (item.id in itemIds) item.copy(status = status) else item
                    }
                )
            }
        }
    }
    override suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput) {
        updatedDailyPlanItems.add(itemId to input)
        dailyPlansFlow.update { plans ->
            plans.map { plan ->
                plan.copy(
                    items = plan.items.map { item ->
                        if (item.id == itemId) {
                            item.copy(
                                title = input.title,
                                note = input.note,
                                source = input.source,
                                status = input.status,
                                startTimeMinutes = input.startTimeMinutes,
                                endTimeMinutes = input.endTimeMinutes
                            )
                        } else {
                            item
                        }
                    }
                )
            }
        }
    }
    override suspend fun updateDailyPlanItemTags(itemId: Long, tagIds: List<Long>) {
        val tagById = boardFlow.value.tags.associateBy { it.id }
        dailyPlansFlow.update { plans ->
            plans.map { plan ->
                plan.copy(
                    items = plan.items.map { item ->
                        if (item.id == itemId) {
                            item.copy(tags = tagIds.mapNotNull { tagById[it] })
                        } else {
                            item
                        }
                    }
                )
            }
        }
    }
    val deletedDailyPlanItemIds = mutableListOf<Long>()
    override suspend fun deleteDailyPlanItem(itemId: Long) {
        deletedDailyPlanItemIds.add(itemId)
        dailyPlansFlow.update { plans ->
            plans.map { plan ->
                plan.copy(items = plan.items.filterNot { it.id == itemId })
            }
        }
    }

    val addedDailyPlanItems = mutableListOf<DailyPlanItem>()

    override suspend fun getDailyPlanItem(itemId: Long): DailyPlanItem? =
        addedDailyPlanItems.find { it.id == itemId }
            ?: dailyPlansFlow.value.flatMap { it.items }.find { it.id == itemId }

    override suspend fun dailyPlanForDate(date: LocalDate): DailyPlan? =
        dailyPlansFlow.value.firstOrNull { it.date == date }

    override suspend fun copyDailyPlanItemToDate(
        source: DailyPlanItem,
        targetDate: LocalDate,
        clearTimes: Boolean
    ): Long? {
        val targetEpoch = targetDate.toEpochDays().toInt()
        val targetItems = dailyPlansFlow.value
            .firstOrNull { it.date == targetDate }
            ?.items
            .orEmpty()
        val alreadyPresent = targetItems.any { item ->
            (source.taskId != null && item.taskId == source.taskId) ||
                (item.carriedFromItemId != null && item.carriedFromItemId == source.id)
        }
        if (alreadyPresent) {
            markHandled(listOf(source.id))
            return null
        }
        val newId = nextDailyPlanItemId++
        val copy = source.copy(
            id = newId,
            dateEpochDays = targetEpoch,
            status = DailyPlanItemStatus.Planned,
            startTimeMinutes = if (clearTimes) null else source.startTimeMinutes,
            endTimeMinutes = if (clearTimes) null else source.endTimeMinutes,
            completedAtMillis = null,
            carriedFromItemId = source.id
        )
        copiedDailyPlanItems.add(copy)
        dailyPlansFlow.update { plans ->
            val existing = plans.firstOrNull { it.date == targetDate }
            if (existing == null) {
                plans + DailyPlan(date = targetDate, items = listOf(copy))
            } else {
                plans.map { plan ->
                    if (plan.date == targetDate) plan.copy(items = plan.items + copy) else plan
                }
            }
        }
        markHandled(listOf(source.id))
        return newId
    }

    override fun observePeriodReviews(): Flow<List<PeriodReview>> = periodReviewsFlow

    override suspend fun periodReviewFor(period: ReviewPeriod, date: LocalDate): PeriodReview? =
        periodReviewsFlow.value.firstOrNull { it.period == period && it.periodStartDate == date }

    override suspend fun savePeriodReview(review: PeriodReview) {
        periodReviewsFlow.update { reviews ->
            reviews.filterNot {
                it.period == review.period && it.periodStartEpochDays == review.periodStartEpochDays
            } + review
        }
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
    ): DayCloseCommitResult {
        updateDailyPlanItemsStatus(markDoneItemIds, DailyPlanItemStatus.Done)
        markHandled(markDoneItemIds)
        markHandled(dropItemIds)
        val allItems = dailyPlansFlow.value.flatMap { it.items }
        var carried = 0
        var skipped = 0
        carryItemIds.forEach { itemId ->
            val source = allItems.find { it.id == itemId } ?: return@forEach
            val targetItems = dailyPlansFlow.value
                .firstOrNull { it.date == targetDate }
                ?.items
                .orEmpty()
            val alreadyPresent = targetItems.any { item ->
                (source.taskId != null && item.taskId == source.taskId) ||
                    (item.carriedFromItemId != null && item.carriedFromItemId == source.id)
            }
            if (alreadyPresent) {
                skipped += 1
            } else {
                val newId = nextDailyPlanItemId++
                val copy = source.copy(
                    id = newId,
                    dateEpochDays = targetDate.toEpochDays().toInt(),
                    status = DailyPlanItemStatus.Planned,
                    startTimeMinutes = null,
                    endTimeMinutes = null,
                    completedAtMillis = null,
                    carriedFromItemId = source.id
                )
                copiedDailyPlanItems.add(copy)
                dailyPlansFlow.update { plans ->
                    val existing = plans.firstOrNull { it.date == targetDate }
                    if (existing == null) {
                        plans + DailyPlan(date = targetDate, items = listOf(copy))
                    } else {
                        plans.map { plan ->
                            if (plan.date == targetDate) plan.copy(items = plan.items + copy) else plan
                        }
                    }
                }
                carried += 1
            }
            markHandled(listOf(source.id))
        }
        periodReviewsFlow.update { reviews ->
            reviews.filterNot {
                it.period == ReviewPeriod.Day && it.periodStartDate == date
            } + PeriodReview(
                id = 0L,
                period = ReviewPeriod.Day,
                periodStartEpochDays = date.toEpochDays().toInt(),
                periodEndEpochDays = date.toEpochDays().toInt() + 1,
                content = winNote?.trim().orEmpty(),
                intentNext = tomorrowGoal?.trim()?.takeIf { it.isNotEmpty() },
                source = ReviewSource.Manual,
                status = ReviewStatus.Complete,
                completedAtMillis = nowMillis,
                editedAtMillis = nowMillis
            )
        }
        return DayCloseCommitResult(
            carriedCount = carried,
            skippedCount = skipped
        )
    }

    private suspend fun markHandled(itemIds: List<Long>) {
        if (itemIds.isEmpty()) return
        markedHandledItemIds.addAll(itemIds)
        dailyPlansFlow.update { plans ->
            plans.map { plan ->
                plan.copy(
                    items = plan.items.map { item ->
                        if (item.id in itemIds) item.copy(handledAtMillis = 1L) else item
                    }
                )
            }
        }
    }

    override suspend fun countDoneDailyPlanItemsForTaskOnDate(
        taskId: Long,
        dateEpochDays: Int,
        excludeItemId: Long
    ): Int = addedDailyPlanItems.count { it.taskId == taskId && it.dateEpochDays == dateEpochDays && it.status == DailyPlanItemStatus.Done && it.id != excludeItemId }

    override suspend fun adjustKeyResultValue(keyResultId: Long, delta: Double) {
        adjustedKeyResults.add(keyResultId to delta)
    }

    override suspend fun getKeyResultForTask(taskId: Long): KeyResult? {
        return currentBoard.tasksById[taskId]?.keyResult
    }

    override suspend fun addNote(input: NoteWriteInput): Long = 0L
    override suspend fun updateNote(noteId: Long, input: NoteWriteInput) = Unit
    override suspend fun trashNote(noteId: Long) = Unit
    override suspend fun restoreNote(noteId: Long) = Unit

    override fun observePeriodPlans(): Flow<List<PeriodPlan>> = periodPlansFlow

    fun setPeriodPlans(plans: List<PeriodPlan>) {
        periodPlansFlow.value = plans
    }

    override fun observePlanPriorities(): Flow<List<PlanPriority>> = planPrioritiesFlow

    fun setPlanPriorities(priorities: List<PlanPriority>) {
        planPrioritiesFlow.value = priorities
    }

    override fun observePlanPriorityTaskIds(): Flow<List<PlanPriorityTaskLink>> = planTaskLinksFlow

    fun setPlanPriorityTaskLinks(links: List<PlanPriorityTaskLink>) {
        planTaskLinksFlow.value = links
    }

    override fun observePlanPriorityDailyPlanItemIds(): Flow<List<PlanPriorityDailyPlanItemLink>> =
        planDailyLinksFlow

    fun setPlanPriorityDailyPlanItemLinks(links: List<PlanPriorityDailyPlanItemLink>) {
        planDailyLinksFlow.value = links
    }

    override suspend fun getOrCreatePeriodPlan(
        period: PlanPeriod,
        start: LocalDate,
        endInclusive: LocalDate
    ): PeriodPlan {
        val startEpochDays = start.toEpochDays().toInt()
        val existing = periodPlansFlow.value.firstOrNull {
            it.period == period && it.startEpochDays == startEpochDays
        }
        if (existing != null) return existing
        val plan = PeriodPlan(
            id = nextPeriodPlanId++,
            period = period,
            startEpochDays = startEpochDays,
            endEpochDays = endInclusive.toEpochDays().toInt()
        )
        periodPlansFlow.update { it + plan }
        return plan
    }

    override suspend fun addPlanPriority(input: PlanPriorityWriteInput): Long {
        addedPlanPriorities.add(input)
        val id = nextPlanPriorityId++
        val priority = PlanPriority(
            id = id,
            periodPlanId = input.periodPlanId,
            parentId = input.parentId,
            title = input.title.trim(),
            note = input.note,
            sortOrder = input.sortOrder ?: planPrioritiesFlow.value.size,
            isDone = input.isDone,
            createdAtMillis = 0L,
            updatedAtMillis = 0L,
            completedAtMillis = if (input.isDone) 1L else null
        )
        planPrioritiesFlow.update { it + priority }
        return id
    }

    override suspend fun updatePlanPriority(id: Long, input: PlanPriorityWriteInput) {
        updatedPlanPriorities.add(id to input)
        planPrioritiesFlow.update { priorities ->
            priorities.map { priority ->
                if (priority.id == id) {
                    priority.copy(
                        parentId = input.parentId,
                        title = input.title.trim(),
                        note = input.note,
                        sortOrder = input.sortOrder ?: priority.sortOrder,
                        isDone = input.isDone,
                        updatedAtMillis = priority.updatedAtMillis + 1,
                        completedAtMillis = if (input.isDone) 1L else null
                    )
                } else {
                    priority
                }
            }
        }
    }

    override suspend fun deletePlanPriority(id: Long) {
        deletedPlanPriorityIds.add(id)
        planPrioritiesFlow.update { priorities ->
            priorities.filterNot { it.id == id }.map { priority ->
                if (priority.parentId == id) priority.copy(parentId = null) else priority
            }
        }
        planTaskLinksFlow.update { it.filterNot { link -> link.priorityId == id } }
        planDailyLinksFlow.update { it.filterNot { link -> link.priorityId == id } }
    }

    override suspend fun setPlanPriorityDone(id: Long, isDone: Boolean) {
        planPrioritiesFlow.update { priorities ->
            priorities.map { priority ->
                if (priority.id == id) {
                    priority.copy(isDone = isDone, completedAtMillis = if (isDone) 1L else null)
                } else {
                    priority
                }
            }
        }
    }

    override suspend fun setPlanPriorityParent(id: Long, parentId: Long?) {
        planPrioritiesFlow.update { priorities ->
            priorities.map { priority ->
                if (priority.id == id) priority.copy(parentId = parentId) else priority
            }
        }
    }

    override suspend fun reorderPlanPriorities(periodPlanId: Long, orderedIds: List<Long>) {
        planPrioritiesFlow.update { priorities ->
            priorities.map { priority ->
                val index = orderedIds.indexOf(priority.id)
                if (index >= 0) priority.copy(sortOrder = index) else priority
            }
        }
    }

    override suspend fun linkTaskToPriority(priorityId: Long, taskId: Long) {
        linkedTasks.add(priorityId to taskId)
        planTaskLinksFlow.update { links ->
            links.filterNot { it.priorityId == priorityId && it.taskId == taskId } +
                PlanPriorityTaskLink(priorityId, taskId, 0)
        }
    }

    override suspend fun unlinkTaskFromPriority(priorityId: Long, taskId: Long) {
        unlinkedTasks.add(priorityId to taskId)
        planTaskLinksFlow.update { links ->
            links.filterNot { it.priorityId == priorityId && it.taskId == taskId }
        }
    }

    override suspend fun linkDailyPlanItemToPriority(priorityId: Long, dailyPlanItemId: Long) {
        linkedDailyPlanItems.add(priorityId to dailyPlanItemId)
        planDailyLinksFlow.update { links ->
            links.filterNot { it.priorityId == priorityId && it.dailyPlanItemId == dailyPlanItemId } +
                PlanPriorityDailyPlanItemLink(priorityId, dailyPlanItemId, 0)
        }
    }

    override suspend fun unlinkDailyPlanItemFromPriority(priorityId: Long, dailyPlanItemId: Long) {
        unlinkedDailyPlanItems.add(priorityId to dailyPlanItemId)
        planDailyLinksFlow.update { links ->
            links.filterNot { it.priorityId == priorityId && it.dailyPlanItemId == dailyPlanItemId }
        }
    }
}

internal class FakeSettingsRepository(
    initialSettings: UserSettings = UserSettings()
) : SettingsRepository {
    private val settingsFlow = MutableStateFlow(initialSettings)
    override val settings: Flow<UserSettings> = settingsFlow

    override suspend fun setLanguageCode(code: String) {
        settingsFlow.update { it.copy(languageCode = code) }
    }

    override suspend fun setThemeModeCode(code: String) {
        settingsFlow.update { it.copy(themeModeCode = code) }
    }

    override suspend fun setColorSchemeModeCode(code: String) {
        settingsFlow.update { it.copy(colorSchemeModeCode = code) }
    }

    override suspend fun setTaskWorkspaceViewCode(code: String) {
        settingsFlow.update { it.copy(taskWorkspaceViewCode = code) }
    }

    override suspend fun setTaskListDisplayTypeCode(code: String) {
        settingsFlow.update { it.copy(taskListDisplayTypeCode = code) }
    }

    override suspend fun setTaskShowCompleted(showCompleted: Boolean) {
        settingsFlow.update { it.copy(taskShowCompleted = showCompleted) }
    }

    override suspend fun setTaskSortOptionCode(code: String) {
        settingsFlow.update { it.copy(taskSortOptionCode = code) }
    }

    override suspend fun setPlanReminderEnabled(enabled: Boolean) {
        settingsFlow.update { it.copy(planReminderEnabled = enabled) }
    }

    override suspend fun setPlanReminderTimeMinutes(minutes: Int) {
        settingsFlow.update { it.copy(planReminderTimeMinutes = minutes) }
    }

    override suspend fun setReviewReminderEnabled(enabled: Boolean) {
        settingsFlow.update { it.copy(reviewReminderEnabled = enabled) }
    }

    override suspend fun setReviewReminderTimeMinutes(minutes: Int) {
        settingsFlow.update { it.copy(reviewReminderTimeMinutes = minutes) }
    }

    override suspend fun setCheckInReminderEnabled(enabled: Boolean) {
        settingsFlow.update { it.copy(checkInReminderEnabled = enabled) }
    }

    override suspend fun setScheduleReminderEnabled(enabled: Boolean) {
        settingsFlow.update { it.copy(scheduleReminderEnabled = enabled) }
    }

    override suspend fun setCheckInReminderLastShownAtMillis(millis: Long) {
        settingsFlow.update { it.copy(checkInReminderLastShownAtMillis = millis) }
    }

    override suspend fun setAutoMyDayLastRunEpochDay(epochDay: Int) {
        settingsFlow.update { it.copy(autoMyDayLastRunEpochDay = epochDay) }
    }

    override suspend fun setLastDayCloseEpochDay(epochDay: Int) {
        settingsFlow.update { it.copy(lastDayCloseEpochDay = epochDay) }
    }

    override suspend fun setAutoCarryOverLeftovers(enabled: Boolean) {
        settingsFlow.update { it.copy(autoCarryOverLeftovers = enabled) }
    }

    override suspend fun setAutoCarryOverLastRunEpochDay(epochDay: Int) {
        settingsFlow.update { it.copy(autoCarryOverLastRunEpochDay = epochDay) }
    }

    override suspend fun setLeftoversBannerDismissedEpochDay(epochDay: Int) {
        settingsFlow.update { it.copy(leftoversBannerDismissedEpochDay = epochDay) }
    }

    override suspend fun setLastDayPlanDismissedEpochDay(epochDay: Int) {
        settingsFlow.update { it.copy(lastDayPlanDismissedEpochDay = epochDay) }
    }

    override suspend fun setLastFabAction(type: String, id: Long?) {
        settingsFlow.update { it.copy(lastFabActionType = type, lastFabActionId = id) }
    }

    fun currentSettings(): UserSettings = settingsFlow.value
}

private fun TaskWriteInput.toTaskItem(
    taskId: Long,
    sortOrder: Int,
    planPriority: PlanPriority? = null,
    createdAtMillis: Long = 0L,
    updatedAtMillis: Long = 0L
) = TaskItem(
    id = taskId,
    list = ListItem.None, // Needs proper resolution if testing specific list assignment
    planPriority = planPriority,
    name = name,
    description = description,
    subtasks = subtasks.mapIndexed { index, subtask ->
        SubTaskItem(
            id = index + 1L,
            taskId = taskId,
            name = subtask.name,
            isCompleted = subtask.isCompleted,
            sortOrder = index
        )
    },
    status = status,
    priority = priority,
    type = type,
    doDate = doDate,
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = endTimeMinutes,
    reminders = reminders.mapIndexed { index, reminder ->
        TaskReminder(
            id = index + 1L,
            taskId = taskId,
            remindAtMillis = reminder.remindAtMillis,
            label = reminder.label
        )
    },
    repeatRRule = repeatRRule,
    sortOrder = sortOrder,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)
