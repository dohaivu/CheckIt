package com.checkit.ui.tasks

import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskTag
import com.checkit.domain.usecase.AddTagUseCase
import com.checkit.domain.usecase.DeleteTagUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.UpdateTagUseCase
import com.checkit.ui.tasks.tag.TagViewModel
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
class TaskTagEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: TagViewModel

    private val existingTag = TaskTag(
        id = 42L,
        name = "Work",
        color = "#7C3AED"
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository(initialBoard = TaskBoard(tags = listOf(existingTag)))
        viewModel = TagViewModel(
            addTaskTag = AddTagUseCase(repository),
            updateTaskTag = UpdateTagUseCase(repository),
            deleteTaskTag = DeleteTagUseCase(repository),
            isTagNameTaken = IsTagNameTakenUseCase(repository)
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openNewTagProducesEmptyAddEditor() = runTest(dispatcher) {
        viewModel.openNewTag()

        val editor = viewModel.uiState.value.editor
        assertNotNull(editor)
        assertEquals(EditorMode.Add, editor.mode)
        assertEquals("", editor.name)
        assertNull(editor.tagId)
    }

    @Test
    fun openEditTagPrefillsExistingValues() = runTest(dispatcher) {
        viewModel.openEditTag(existingTag)

        val editor = viewModel.uiState.value.editor
        assertNotNull(editor)
        assertEquals(EditorMode.Edit, editor.mode)
        assertEquals(42L, editor.tagId)
        assertEquals("Work", editor.name)
        assertEquals("#7C3AED", editor.color)
    }

    @Test
    fun saveTagEditorWithBlankNameShowsMessageAndDoesNotPersist() = runTest(dispatcher) {
        viewModel.openNewTag()
        viewModel.updateName("   ")

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.editor)
        assertTrue(repository.addedTags.isEmpty())
    }

    @Test
    fun saveTagEditorRejectsDuplicateName() = runTest(dispatcher) {
        viewModel.openNewTag()
        viewModel.updateName("  Work  ")

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.editor)
        assertTrue(repository.addedTags.isEmpty())
    }

    @Test
    fun saveNewTagPersistsTrimmedInputAndReportsSavedId() = runTest(dispatcher) {
        var savedId: Long? = null
        viewModel.openNewTag()
        viewModel.updateName("  Personal  ")
        viewModel.updateColor("#059669")

        viewModel.saveEditor(onSaved = { savedId = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.addedTags.size)
        val added = repository.addedTags.single()
        assertEquals("Personal", added.name)
        assertEquals("#059669", added.color)
        assertNull(viewModel.uiState.value.editor)
        assertEquals(repository.lastAssignedTagId, savedId)
    }

    @Test
    fun saveEditedTagKeepingNameDoesNotTriggerDuplicateError() = runTest(dispatcher) {
        viewModel.openEditTag(existingTag)
        viewModel.updateColor("#DC2626")

        viewModel.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.updatedTags.size)
        val (updatedId, input) = repository.updatedTags.single()
        assertEquals(42L, updatedId)
        assertEquals("Work", input.name)
        assertEquals("#DC2626", input.color)
        assertTrue(repository.addedTags.isEmpty())
        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun dismissTagEditorClearsState() = runTest(dispatcher) {
        viewModel.openNewTag()
        viewModel.dismissEditor()

        assertNull(viewModel.uiState.value.editor)
    }
}
