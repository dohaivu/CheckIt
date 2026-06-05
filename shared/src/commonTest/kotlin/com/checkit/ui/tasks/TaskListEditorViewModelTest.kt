package com.checkit.ui.tasks

import com.checkit.domain.TaskList
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddTaskListUseCase
import com.checkit.domain.usecase.AddTaskTagUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateTaskListUseCase
import com.checkit.domain.usecase.UpdateTaskTagUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
import com.checkit.ui.EditorMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: TaskViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        viewModel = TaskViewModel(
            observeTaskBoard = ObserveTaskBoardUseCase(repository),
            ensureDefaultTaskData = EnsureDefaultTaskDataUseCase(repository),
            selectTaskBoardItems = SelectTaskBoardItemsUseCase(),
            addTask = AddTaskUseCase(repository),
            updateTask = UpdateTaskUseCase(repository),
            deleteTask = DeleteTaskUseCase(repository),
            completeTask = CompleteTaskUseCase(repository),
            addNote = AddNoteUseCase(repository),
            updateNote = UpdateNoteUseCase(repository),
            deleteNote = DeleteNoteUseCase(repository),
            addTaskList = AddTaskListUseCase(repository),
            updateTaskList = UpdateTaskListUseCase(repository),
            addTaskTag = AddTaskTagUseCase(repository),
            updateTaskTag = UpdateTaskTagUseCase(repository),
            isTagNameTaken = IsTagNameTakenUseCase(repository)
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openNewListProducesEmptyAddEditor() = runTest(dispatcher) {
        viewModel.openNewList()

        val editor = viewModel.uiState.value.listEditor
        assertNotNull(editor)
        assertEquals(EditorMode.Add, editor.mode)
        assertEquals("", editor.name)
        assertNull(editor.listId)
    }

    @Test
    fun openEditListPrefillsExistingValues() = runTest(dispatcher) {
        val list = TaskList(
            id = 12L,
            name = "Reading",
            color = "#7C3AED",
            icon = "Notes",
            sortOrder = 0
        )

        viewModel.openEditList(list)

        val editor = viewModel.uiState.value.listEditor
        assertNotNull(editor)
        assertEquals(EditorMode.Edit, editor.mode)
        assertEquals(12L, editor.listId)
        assertEquals("Reading", editor.name)
        assertEquals("#7C3AED", editor.color)
        assertEquals("Notes", editor.icon)
    }

    @Test
    fun saveListEditorWithBlankNameKeepsEditorAndShowsMessage() = runTest(dispatcher) {
        viewModel.openNewList()
        viewModel.updateListEditorName("   ")

        viewModel.saveListEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.listEditor)
        assertEquals("Add a list name", state.message)
        assertTrue(repository.addedLists.isEmpty())
    }

    @Test
    fun saveNewListPersistsTrimmedInputAndSelectsIt() = runTest(dispatcher) {
        viewModel.openNewList()
        viewModel.updateListEditorName("  Reading  ")
        viewModel.updateListEditorColor("#059669")
        viewModel.updateListEditorIcon("Notes")

        viewModel.saveListEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.addedLists.size)
        val added = repository.addedLists.single()
        assertEquals("Reading", added.name)
        assertEquals("#059669", added.color)
        assertEquals("Notes", added.icon)
        val state = viewModel.uiState.value
        assertNull(state.listEditor)
        assertEquals(repository.lastAssignedListId, state.selectedListId)
    }

    @Test
    fun saveEditedListWritesUpdateForExistingId() = runTest(dispatcher) {
        val list = TaskList(
            id = 7L,
            name = "Home",
            color = "#2563EB",
            icon = "Home",
            sortOrder = 0
        )
        viewModel.openEditList(list)
        viewModel.updateListEditorName("House")
        viewModel.updateListEditorColor("#DC2626")

        viewModel.saveListEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.updatedLists.size)
        val (updatedId, input) = repository.updatedLists.single()
        assertEquals(7L, updatedId)
        assertEquals("House", input.name)
        assertEquals("#DC2626", input.color)
        assertEquals("Home", input.icon)
        assertTrue(repository.addedLists.isEmpty())
        assertNull(viewModel.uiState.value.listEditor)
    }

    @Test
    fun dismissListEditorClearsState() = runTest(dispatcher) {
        viewModel.openNewList()
        viewModel.dismissListEditor()

        assertNull(viewModel.uiState.value.listEditor)
    }
}
