package com.checkit.ui.tasks

import com.checkit.domain.ListItem
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TagItem
import com.checkit.domain.usecase.AddListUseCase
import com.checkit.domain.usecase.AddObjectiveUseCase
import com.checkit.domain.usecase.AddTagUseCase
import com.checkit.domain.usecase.DeleteListUseCase
import com.checkit.domain.usecase.DeleteObjectiveUseCase
import com.checkit.domain.usecase.DeleteTagUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.UpdateListUseCase
import com.checkit.domain.usecase.UpdateObjectiveUseCase
import com.checkit.domain.usecase.UpdateTagUseCase
import com.checkit.ui.okr.ObjectiveViewModel
import com.checkit.ui.tasks.list.ListViewModel
import com.checkit.ui.tasks.tag.TagViewModel
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

    private val inbox = ListItem(id = 1L, title = "Inbox", color = "#2563EB", icon = "Inbox", sortOrder = 0)
    private val errands = ListItem(id = 2L, title = "Errands", color = "#059669", icon = "List", sortOrder = 1)
    private val tag = TagItem(id = 10L, name = "Work", color = "#7C3AED")

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

    private fun taskListViewModel(repository: FakeCheckItRepository) = ListViewModel(
        addList = AddListUseCase(repository),
        updateList = UpdateListUseCase(repository),
        deleteList = DeleteListUseCase(repository)
    )

    private fun taskTagViewModel(repository: FakeCheckItRepository) = TagViewModel(
        addTaskTag = AddTagUseCase(repository),
        updateTaskTag = UpdateTagUseCase(repository),
        deleteTaskTag = DeleteTagUseCase(repository),
        isTagNameTaken = IsTagNameTakenUseCase(repository)
    )

    private fun task(id: Long, list: ListItem, tags: List<TagItem> = emptyList()) = TaskItem(
        id = id,
        list = list,
        name = "Task $id",
        tags = tags,
        sortOrder = 0,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun note(id: Long, list: ListItem, tags: List<TagItem> = emptyList()) = NoteItem(
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
