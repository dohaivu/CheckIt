package com.checkit.ui.tasks

import com.checkit.domain.Objective
import com.checkit.domain.usecase.AddObjectiveUseCase
import com.checkit.domain.usecase.DeleteObjectiveUseCase
import com.checkit.domain.usecase.UpdateObjectiveUseCase
import com.checkit.ui.okr.ObjectiveViewModel
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
    private lateinit var viewModel: ObjectiveViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        viewModel = ObjectiveViewModel(
            addObjective = AddObjectiveUseCase(repository),
            updateObjective = UpdateObjectiveUseCase(repository),
            deleteObjective = DeleteObjectiveUseCase(repository)
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
        assertNull(editor.objectiveId)
    }

    @Test
    fun openEditListPrefillsExistingValues() = runTest(dispatcher) {
        val list = Objective(
            id = 12L,
            name = "Reading",
            color = "#7C3AED",
            icon = "Notes",
            sortOrder = 0
        )

        viewModel.openEditObjective(list)

        val editor = viewModel.uiState.value.editor
        assertNotNull(editor)
        assertEquals(EditorMode.Edit, editor.mode)
        assertEquals(12L, editor.objectiveId)
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
        assertTrue(repository.addedObjectives.isEmpty())
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

        assertEquals(1, repository.addedObjectives.size)
        val added = repository.addedObjectives.single()
        assertEquals("Reading", added.name)
        assertEquals("#059669", added.color)
        assertEquals("Notes", added.icon)
        assertNull(viewModel.uiState.value.editor)
        assertEquals(repository.lastAssignedObjectiveId, savedId)
    }

    @Test
    fun saveEditedListWritesUpdateForExistingId() = runTest(dispatcher) {
        val list = Objective(
            id = 7L,
            name = "Home",
            color = "#2563EB",
            icon = "Home",
            sortOrder = 0
        )
        viewModel.openEditObjective(list)
        viewModel.updateName("House")
        viewModel.updateColor("#DC2626")

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.updatedObjectives.size)
        val (updatedId, input) = repository.updatedObjectives.single()
        assertEquals(7L, updatedId)
        assertEquals("House", input.name)
        assertEquals("#DC2626", input.color)
        assertEquals("Home", input.icon)
        assertTrue(repository.addedObjectives.isEmpty())
        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun dismissListEditorClearsState() = runTest(dispatcher) {
        viewModel.openNewList()
        viewModel.dismissEditor()

        assertNull(viewModel.uiState.value.editor)
    }
}
