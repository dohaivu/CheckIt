package com.checkit.ui.tasks

import com.checkit.data.CheckItRepository
import com.checkit.data.NoteWriteInput
import com.checkit.data.TaskListWriteInput
import com.checkit.data.TaskTagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.DailyPlan
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
    val addedTags = mutableListOf<TaskTagWriteInput>()
    val updatedTags = mutableListOf<Pair<Long, TaskTagWriteInput>>()
    val addedTasks = mutableListOf<TaskWriteInput>()
    val updatedTasks = mutableListOf<Pair<Long, TaskWriteInput>>()

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
    override suspend fun completeTask(taskId: Long) = Unit
    override suspend fun addTaskToDailyPlan(date: LocalDate, task: TaskItem): Long = 0L
    override suspend fun addManualDoneToDailyPlan(
        date: LocalDate,
        title: String,
        note: String?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?
    ): Long = 0L
    override suspend fun addNoteToDailyPlan(date: LocalDate, note: String): Long = 0L
    override suspend fun updateDailyPlanItemStatus(itemId: Long, status: DailyPlanItemStatus) = Unit
    override suspend fun updateDailyPlanItemTime(itemId: Long, startTimeMinutes: Int?, endTimeMinutes: Int?) = Unit
    override suspend fun addNote(input: NoteWriteInput): Long = 0L
    override suspend fun updateNote(noteId: Long, input: NoteWriteInput) = Unit
    override suspend fun trashNote(noteId: Long) = Unit
}

private fun TaskWriteInput.toTaskItem(
    taskId: Long,
    sortOrder: Int,
    createdAtMillis: Long = 0L,
    updatedAtMillis: Long = 0L
) = TaskItem(
    id = taskId,
    listId = listId,
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
    dueDate = dueDate,
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
