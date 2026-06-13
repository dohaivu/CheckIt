package com.checkit.ui.tasks

import com.checkit.domain.TaskList
import com.checkit.domain.usecase.AddTaskListUseCase
import com.checkit.domain.usecase.DeleteTaskListUseCase
import com.checkit.domain.usecase.UpdateTaskListUseCase
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
    private lateinit var viewModel: TaskListViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        viewModel = TaskListViewModel(
            addTaskList = AddTaskListUseCase(repository),
            updateTaskList = UpdateTaskListUseCase(repository),
            deleteTaskList = DeleteTaskListUseCase(repository)
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openNewListProducesEmptyAddEditor() = runTest(dispatcher) {
        viewModel.openNewList()

        val editor = viewModel.uiState.value.editor
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

        val editor = viewModel.uiState.value.editor
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
        viewModel.updateName("   ")

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.editor)
        assertEquals("Add a list name", state.message)
        assertTrue(repository.addedLists.isEmpty())
    }

    @Test
    fun saveNewListPersistsTrimmedInputAndReportsSavedId() = runTest(dispatcher) {
        var savedId: Long? = null
        viewModel.openNewList()
        viewModel.updateName("  Reading  ")
        viewModel.updateColor("#059669")
        viewModel.updateIcon("Notes")

        viewModel.saveEditor(onSaved = { savedId = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.addedLists.size)
        val added = repository.addedLists.single()
        assertEquals("Reading", added.name)
        assertEquals("#059669", added.color)
        assertEquals("Notes", added.icon)
        assertNull(viewModel.uiState.value.editor)
        assertEquals(repository.lastAssignedListId, savedId)
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
        viewModel.updateName("House")
        viewModel.updateColor("#DC2626")

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.updatedLists.size)
        val (updatedId, input) = repository.updatedLists.single()
        assertEquals(7L, updatedId)
        assertEquals("House", input.name)
        assertEquals("#DC2626", input.color)
        assertEquals("Home", input.icon)
        assertTrue(repository.addedLists.isEmpty())
        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun dismissListEditorClearsState() = runTest(dispatcher) {
        viewModel.openNewList()
        viewModel.dismissEditor()

        assertNull(viewModel.uiState.value.editor)
    }
}
