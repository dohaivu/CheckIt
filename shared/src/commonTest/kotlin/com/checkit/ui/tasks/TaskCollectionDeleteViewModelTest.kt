package com.checkit.ui.tasks

import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskTag
import com.checkit.domain.usecase.AddTaskListUseCase
import com.checkit.domain.usecase.AddTaskTagUseCase
import com.checkit.domain.usecase.DeleteTaskListUseCase
import com.checkit.domain.usecase.DeleteTaskTagUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.UpdateTaskListUseCase
import com.checkit.domain.usecase.UpdateTaskTagUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCollectionDeleteViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val inbox = TaskList(id = 1L, name = "Inbox", color = "#2563EB", icon = "Inbox", sortOrder = 0)
    private val errands = TaskList(id = 2L, name = "Errands", color = "#059669", icon = "List", sortOrder = 1)
    private val tag = TaskTag(id = 10L, name = "Work", color = "#7C3AED")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deleteEditorListMovesItemsToInboxAndClearsEditor() = runTest(dispatcher) {
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                lists = listOf(inbox, errands),
                tasks = listOf(task(id = 20L, list = errands)),
                notes = listOf(note(id = 30L, list = errands))
            )
        )
        val viewModel = taskListViewModel(repository)
        viewModel.openEditList(errands)

        viewModel.deleteEditorList()
        dispatcher.scheduler.advanceUntilIdle()

        val board = repository.currentBoard
        assertEquals(listOf(errands.id), repository.deletedLists)
        assertEquals(listOf(inbox), board.lists)
        assertEquals(inbox.id, board.tasks.single().list.id)
        assertEquals(inbox.id, board.notes.single().list.id)
        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun deleteEditorTagRemovesTagAndClearsEditor() = runTest(dispatcher) {
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                lists = listOf(inbox),
                tags = listOf(tag),
                tasks = listOf(task(id = 20L, list = inbox, tags = listOf(tag))),
                notes = listOf(note(id = 30L, list = inbox, tags = listOf(tag)))
            )
        )
        val viewModel = taskTagViewModel(repository)
        viewModel.openEditTag(tag)

        viewModel.deleteEditorTag()
        dispatcher.scheduler.advanceUntilIdle()

        val board = repository.currentBoard
        assertEquals(listOf(tag.id), repository.deletedTags)
        assertEquals(emptyList(), board.tags)
        assertEquals(emptyList(), board.tasks.single().tags)
        assertEquals(emptyList(), board.notes.single().tags)
        assertNull(viewModel.uiState.value.editor)
    }

    private fun taskListViewModel(repository: FakeCheckItRepository) = TaskListViewModel(
        addTaskList = AddTaskListUseCase(repository),
        updateTaskList = UpdateTaskListUseCase(repository),
        deleteTaskList = DeleteTaskListUseCase(repository)
    )

    private fun taskTagViewModel(repository: FakeCheckItRepository) = TaskTagViewModel(
        addTaskTag = AddTaskTagUseCase(repository),
        updateTaskTag = UpdateTaskTagUseCase(repository),
        deleteTaskTag = DeleteTaskTagUseCase(repository),
        isTagNameTaken = IsTagNameTakenUseCase(repository)
    )

    private fun task(id: Long, list: TaskList, tags: List<TaskTag> = emptyList()) = TaskItem(
        id = id,
        list = list,
        name = "Task $id",
        tags = tags,
        sortOrder = 0,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun note(id: Long, list: TaskList, tags: List<TaskTag> = emptyList()) = NoteItem(
        id = id,
        list = list,
        content = "Note $id",
        tags = tags,
        date = LocalDate(2026, 6, 13),
        createdAtMillis = 0L,
        editedAtMillis = 0L,
        sortOrder = 0
    )
}
