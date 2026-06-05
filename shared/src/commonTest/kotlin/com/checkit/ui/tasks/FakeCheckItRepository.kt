package com.checkit.ui.tasks

import com.checkit.data.CheckItRepository
import com.checkit.data.NoteWriteInput
import com.checkit.data.TaskListWriteInput
import com.checkit.data.TaskTagWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskList
import com.checkit.domain.TaskTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FakeCheckItRepository(
    initialBoard: TaskBoard = TaskBoard()
) : CheckItRepository {
    private val boardFlow = MutableStateFlow(initialBoard)
    val addedLists = mutableListOf<TaskListWriteInput>()
    val updatedLists = mutableListOf<Pair<Long, TaskListWriteInput>>()
    val addedTags = mutableListOf<TaskTagWriteInput>()
    val updatedTags = mutableListOf<Pair<Long, TaskTagWriteInput>>()

    var lastAssignedListId: Long = 0L
        private set
    var lastAssignedTagId: Long = 0L
        private set

    private var nextListId: Long = 100L
    private var nextTagId: Long = 500L

    override fun observeTaskBoard(): Flow<TaskBoard> = boardFlow

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

    override suspend fun addTask(input: TaskWriteInput): Long = 0L
    override suspend fun updateTask(taskId: Long, input: TaskWriteInput) = Unit
    override suspend fun trashTask(taskId: Long) = Unit
    override suspend fun completeTask(taskId: Long) = Unit
    override suspend fun addNote(input: NoteWriteInput): Long = 0L
    override suspend fun updateNote(noteId: Long, input: NoteWriteInput) = Unit
    override suspend fun trashNote(noteId: Long) = Unit
}
