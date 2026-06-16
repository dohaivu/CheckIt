package com.checkit.ui.tasks

import com.checkit.data.CheckItRepository
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.data.NoteWriteInput
import com.checkit.data.SettingsRepository
import com.checkit.data.TaskListWriteInput
import com.checkit.data.TaskTagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.data.UserSettings
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
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
    val addedLists = mutableListOf<TaskListWriteInput>()
    val updatedLists = mutableListOf<Pair<Long, TaskListWriteInput>>()
    val deletedLists = mutableListOf<Long>()
    val addedTags = mutableListOf<TaskTagWriteInput>()
    val updatedTags = mutableListOf<Pair<Long, TaskTagWriteInput>>()
    val deletedTags = mutableListOf<Long>()
    val addedTasks = mutableListOf<TaskWriteInput>()
    val updatedTasks = mutableListOf<Pair<Long, TaskWriteInput>>()
    val addedDailyPlanTasks = mutableListOf<Pair<LocalDate, TaskItem>>()
    val addedManualDailyPlanItems = mutableListOf<DailyPlanItemWriteInput>()
    val currentBoard: TaskBoard get() = boardFlow.value

    var lastAssignedListId: Long = 0L
        private set
    var lastAssignedTagId: Long = 0L
        private set

    private var nextListId: Long = 100L
    private var nextTagId: Long = 500L
    private var nextTaskId: Long = 1_000L

    override fun observeTaskBoard(): Flow<TaskBoard> = boardFlow
    override fun observeDailyPlans(): Flow<List<DailyPlan>> = MutableStateFlow(emptyList())

    override suspend fun ensureDefaultTaskData() = Unit

    override suspend fun addList(input: TaskListWriteInput): Long {
        addedLists.add(input)
        val id = nextListId++
        lastAssignedListId = id
        boardFlow.update { board ->
            board.copy(
                lists = board.lists + TaskList(
                    id = id,
                    name = input.name,
                    color = input.color,
                    icon = input.icon,
                    sortOrder = board.lists.size
                )
            )
        }
        return id
    }

    override suspend fun updateList(listId: Long, input: TaskListWriteInput) {
        updatedLists.add(listId to input)
        boardFlow.update { board ->
            board.copy(
                lists = board.lists.map { list ->
                    if (list.id == listId) {
                        list.copy(name = input.name, color = input.color, icon = input.icon)
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
            val inbox = board.lists.firstOrNull { it.name == "Inbox" } ?: return@update board
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

    override suspend fun addTag(input: TaskTagWriteInput): Long {
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

    override suspend fun updateTag(tagId: Long, input: TaskTagWriteInput) {
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
    override suspend fun addManualDoneToDailyPlan(
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
    override suspend fun updateDailyPlanItem(itemId: Long, input: DailyPlanItemWriteInput) = Unit
    override suspend fun deleteDailyPlanItem(itemId: Long) = Unit
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
}

private fun TaskWriteInput.toTaskItem(
    taskId: Long,
    sortOrder: Int,
    createdAtMillis: Long = 0L,
    updatedAtMillis: Long = 0L
) = TaskItem(
    id = taskId,
    list = TaskList.None,
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
