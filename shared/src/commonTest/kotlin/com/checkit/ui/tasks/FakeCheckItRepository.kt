package com.checkit.ui.tasks

import com.checkit.data.CheckItRepository
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.data.GoalWriteInput
import com.checkit.data.KeyResultWriteInput
import com.checkit.data.NoteWriteInput
import com.checkit.data.SettingsRepository
import com.checkit.data.ObjectiveWriteInput
import com.checkit.data.TagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.data.UserSettings
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.Goal
import com.checkit.domain.KeyResult
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskReminder
import com.checkit.domain.TaskTag
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
    val addedTags = mutableListOf<TagWriteInput>()
    val updatedTags = mutableListOf<Pair<Long, TagWriteInput>>()
    val deletedTags = mutableListOf<Long>()
    val addedTasks = mutableListOf<TaskWriteInput>()
    val updatedTasks = mutableListOf<Pair<Long, TaskWriteInput>>()
    val deletedTasks = mutableListOf<Long>()
    val addedDailyPlanTasks = mutableListOf<Pair<LocalDate, TaskItem>>()
    val addedManualDailyPlanItems = mutableListOf<DailyPlanItemWriteInput>()
    val updatedDailyPlanItems = mutableListOf<Pair<Long, DailyPlanItemWriteInput>>()
    val adjustedKeyResults = mutableListOf<Pair<Long, Double>>()
    val currentBoard: TaskBoard get() = boardFlow.value

    var lastAssignedObjectiveId: Long = 0L
        private set
    var lastAssignedTagId: Long = 0L
        private set

    private var nextGoalId: Long = 50L
    private var nextObjectiveId: Long = 100L
    private var nextKeyResultId: Long = 300L
    private var nextTagId: Long = 500L
    private var nextTaskId: Long = 1_000L

    override fun observeTaskBoard(): Flow<TaskBoard> = boardFlow
    override fun observeDailyPlans(): Flow<List<DailyPlan>> = MutableStateFlow(emptyList())

    override suspend fun ensureDefaultTaskData() = Unit

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
                objectives = board.objectives.map { list ->
                    if (list.goalId == goalId) list.copy(goalId = null) else list
                }
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
                        list.copy(name = input.name, color = input.color, icon = input.icon)
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
            val inbox = board.objectives.firstOrNull { it.name == "Inbox" } ?: return@update board
            board.copy(
                objectives = board.objectives.filterNot { it.id == objectiveId },
                tasks = board.tasks.map { task ->
                    if (task.objective.id == objectiveId) task.copy(objective = inbox) else task
                },
                notes = board.notes.map { note ->
                    if (note.objective.id == objectiveId) note.copy(objective = inbox) else note
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
                tags = board.tags + TaskTag(
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
        boardFlow.update { board ->
            board.copy(
                tasks = board.tasks + input.toTaskItem(
                    taskId = id,
                    sortOrder = board.tasks.size
                )
            )
        }
        return id
    }

    override suspend fun updateTask(taskId: Long, input: TaskWriteInput) {
        updatedTasks.add(taskId to input)
        boardFlow.update { board ->
            board.copy(
                tasks = board.tasks.map { task ->
                    if (task.id == taskId) {
                        input.toTaskItem(
                            taskId = taskId,
                            sortOrder = task.sortOrder,
                            createdAtMillis = task.createdAtMillis,
                            updatedAtMillis = task.updatedAtMillis + 1
                        )
                    } else {
                        task
                    }
                }
            )
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
        return addedDailyPlanTasks.size.toLong()
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
        return addedManualDailyPlanItems.size.toLong()
    }
    override suspend fun updateDailyPlanItemTime(itemId: Long, startTimeMinutes: Int?, endTimeMinutes: Int?) = Unit
    override suspend fun updateDailyPlanItemStatus(itemId: Long, status: DailyPlanItemStatus) = Unit
    override suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput) {
        updatedDailyPlanItems.add(itemId to input)
    }
    override suspend fun deleteDailyPlanItem(itemId: Long) = Unit

    val addedDailyPlanItems = mutableListOf<DailyPlanItem>()

    override suspend fun getDailyPlanItem(itemId: Long): DailyPlanItem? = addedDailyPlanItems.find { it.id == itemId }

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
}

private fun TaskWriteInput.toTaskItem(
    taskId: Long,
    sortOrder: Int,
    createdAtMillis: Long = 0L,
    updatedAtMillis: Long = 0L
) = TaskItem(
    id = taskId,
        objective = Objective.None,
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
    doDate = doDate,
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = endTimeMinutes,
    durationMinutes = durationMinutes,
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
