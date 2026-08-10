package com.checkit.ui.tasks

import com.checkit.domain.ListItem
import com.checkit.domain.usecase.AddListUseCase
import com.checkit.domain.usecase.DeleteListUseCase
import com.checkit.domain.usecase.UpdateListUseCase
import com.checkit.ui.tasks.list.ListViewModel
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
class ListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: ListViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        viewModel = ListViewModel(
            addList = AddListUseCase(repository),
            updateList = UpdateListUseCase(repository),
            deleteList = DeleteListUseCase(repository)
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
        assertEquals("", editor.title)
        assertNull(editor.listId)
    }

    @Test
    fun openEditListPrefillsExistingValues() = runTest(dispatcher) {
        val list = ListItem(
            id = 12L,
            title = "Reading",
            color = "#7C3AED",
            icon = "Notes",
            sortOrder = 0
        )

        viewModel.openEditList(list)

        val editor = viewModel.uiState.value.editor
        assertNotNull(editor)
        assertEquals(EditorMode.Edit, editor.mode)
        assertEquals(12L, editor.listId)
        assertEquals("Reading", editor.title)
        assertEquals("#7C3AED", editor.color)
        assertEquals("Notes", editor.icon)
    }

    @Test
    fun saveListEditorWithBlankNameKeepsEditorAndShowsMessage() = runTest(dispatcher) {
        viewModel.openNewList()
        viewModel.updateTitle("   ")

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.editor)
        assertTrue(repository.addedLists.isEmpty())
    }

    @Test
    fun saveNewListPersistsTrimmedInputAndReportsSavedId() = runTest(dispatcher) {
        viewModel.openNewList()
        viewModel.updateTitle("  Reading  ")
        viewModel.updateColor("#059669")
        viewModel.updateIcon("Notes")

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.addedLists.size)
        val added = repository.addedLists.single()
        assertEquals("Reading", added.title)
        assertEquals("#059669", added.color)
        assertEquals("Notes", added.icon)
        assertNull(viewModel.uiState.value.editor)
    }
}
