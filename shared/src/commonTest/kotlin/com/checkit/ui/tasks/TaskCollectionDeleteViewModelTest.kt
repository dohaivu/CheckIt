package com.checkit.ui.tasks

import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskTag
import com.checkit.domain.usecase.AddObjectiveUseCase
import com.checkit.domain.usecase.AddTagUseCase
import com.checkit.domain.usecase.DeleteObjectiveUseCase
import com.checkit.domain.usecase.DeleteTagUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.UpdateObjectiveUseCase
import com.checkit.domain.usecase.UpdateTagUseCase
import com.checkit.ui.okr.ObjectiveViewModel
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

    private val inbox = Objective(id = 1L, name = "Inbox", color = "#2563EB", icon = "Inbox", sortOrder = 0)
    private val errands = Objective(id = 2L, name = "Errands", color = "#059669", icon = "List", sortOrder = 1)
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
                objectives = listOf(inbox, errands),
                tasks = listOf(task(id = 20L, objective = errands)),
                notes = listOf(note(id = 30L, objective = errands))
            )
        )
        val viewModel = taskListViewModel(repository)
        viewModel.openEditObjective(errands)

        viewModel.deleteEditorList()
        dispatcher.scheduler.advanceUntilIdle()

        val board = repository.currentBoard
        assertEquals(listOf(errands.id), repository.deletedObjectives)
        assertEquals(listOf(inbox), board.objectives)
        assertEquals(inbox.id, board.tasks.single().objective.id)
        assertEquals(inbox.id, board.notes.single().objective.id)
        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun deleteEditorTagRemovesTagAndClearsEditor() = runTest(dispatcher) {
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                objectives = listOf(inbox),
                tags = listOf(tag),
                tasks = listOf(task(id = 20L, objective = inbox, tags = listOf(tag))),
                notes = listOf(note(id = 30L, objective = inbox, tags = listOf(tag)))
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

    private fun taskListViewModel(repository: FakeCheckItRepository) = ObjectiveViewModel(
        addObjective = AddObjectiveUseCase(repository),
        updateObjective = UpdateObjectiveUseCase(repository),
        deleteObjective = DeleteObjectiveUseCase(repository)
    )

    private fun taskTagViewModel(repository: FakeCheckItRepository) = TagViewModel(
        addTaskTag = AddTagUseCase(repository),
        updateTaskTag = UpdateTagUseCase(repository),
        deleteTaskTag = DeleteTagUseCase(repository),
        isTagNameTaken = IsTagNameTakenUseCase(repository)
    )

    private fun task(id: Long, objective: Objective, tags: List<TaskTag> = emptyList()) = TaskItem(
        id = id,
        objective = objective,
        name = "Task $id",
        tags = tags,
        sortOrder = 0,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun note(id: Long, objective: Objective, tags: List<TaskTag> = emptyList()) = NoteItem(
        id = id,
        objective = objective,
        content = "Note $id",
        tags = tags,
        date = LocalDate(2026, 6, 13),
        createdAtMillis = 0L,
        editedAtMillis = 0L,
        sortOrder = 0
    )
}
