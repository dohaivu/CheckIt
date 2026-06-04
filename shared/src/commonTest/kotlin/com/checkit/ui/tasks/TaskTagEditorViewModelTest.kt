package com.checkit.ui.tasks

import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskTag
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
class TaskTagEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: TaskViewModel

    private val existingTag = TaskTag(
        id = 42L,
        name = "Work",
        icon = "Work",
        color = "#7C3AED"
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository(initialBoard = TaskBoard(tags = listOf(existingTag)))
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
    fun openNewTagProducesEmptyAddEditor() = runTest(dispatcher) {
        viewModel.openNewTag()

        val editor = viewModel.uiState.value.tagEditor
        assertNotNull(editor)
        assertEquals(EditorMode.Add, editor.mode)
        assertEquals("", editor.name)
        assertNull(editor.tagId)
    }

    @Test
    fun openEditTagPrefillsExistingValues() = runTest(dispatcher) {
        viewModel.openEditTag(existingTag)

        val editor = viewModel.uiState.value.tagEditor
        assertNotNull(editor)
        assertEquals(EditorMode.Edit, editor.mode)
        assertEquals(42L, editor.tagId)
        assertEquals("Work", editor.name)
        assertEquals("#7C3AED", editor.color)
        assertEquals("Work", editor.icon)
    }

    @Test
    fun saveTagEditorWithBlankNameShowsMessageAndDoesNotPersist() = runTest(dispatcher) {
        viewModel.openNewTag()
        viewModel.updateTagEditorName("   ")

        viewModel.saveTagEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.tagEditor)
        assertEquals("Add a tag name", state.message)
        assertTrue(repository.addedTags.isEmpty())
    }

    @Test
    fun saveTagEditorRejectsDuplicateName() = runTest(dispatcher) {
        viewModel.openNewTag()
        viewModel.updateTagEditorName("  Work  ")

        viewModel.saveTagEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.tagEditor)
        assertEquals("Tag name already exists", state.message)
        assertTrue(repository.addedTags.isEmpty())
    }

    @Test
    fun saveNewTagPersistsTrimmedInputAndSelectsIt() = runTest(dispatcher) {
        viewModel.openNewTag()
        viewModel.updateTagEditorName("  Personal  ")
        viewModel.updateTagEditorColor("#059669")
        viewModel.updateTagEditorIcon("Home")

        viewModel.saveTagEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.addedTags.size)
        val added = repository.addedTags.single()
        assertEquals("Personal", added.name)
        assertEquals("#059669", added.color)
        assertEquals("Home", added.icon)
        val state = viewModel.uiState.value
        assertNull(state.tagEditor)
        assertEquals(repository.lastAssignedTagId, state.selectedTagId)
        assertNull(state.selectedListId)
    }

    @Test
    fun saveEditedTagKeepingNameDoesNotTriggerDuplicateError() = runTest(dispatcher) {
        viewModel.openEditTag(existingTag)
        viewModel.updateTagEditorColor("#DC2626")

        viewModel.saveTagEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.updatedTags.size)
        val (updatedId, input) = repository.updatedTags.single()
        assertEquals(42L, updatedId)
        assertEquals("Work", input.name)
        assertEquals("#DC2626", input.color)
        assertTrue(repository.addedTags.isEmpty())
        val state = viewModel.uiState.value
        assertNull(state.tagEditor)
        assertEquals(42L, state.selectedTagId)
    }

    @Test
    fun dismissTagEditorClearsState() = runTest(dispatcher) {
        viewModel.openNewTag()
        viewModel.dismissTagEditor()

        assertNull(viewModel.uiState.value.tagEditor)
    }
}
