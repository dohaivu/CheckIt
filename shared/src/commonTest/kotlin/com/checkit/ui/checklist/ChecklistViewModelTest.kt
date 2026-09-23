package com.checkit.ui.checklist

import com.checkit.checklist.ChecklistDocument
import com.checkit.checklist.ChecklistStorage
import com.checkit.checklist.filterMarkdownDocuments
import com.checkit.data.UserSettings
import com.checkit.ui.tasks.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeChecklistStorage(
    var files: List<ChecklistDocument> = emptyList(),
    var contents: Map<String, String> = emptyMap(),
    var listError: String? = null,
    var readError: String? = null
) : ChecklistStorage {
    var listCalls = 0
        private set

    override suspend fun listMarkdownFiles(folderUri: String): List<ChecklistDocument> {
        listCalls++
        listError?.let { error(it) }
        return files
    }

    override suspend fun readMarkdownFile(documentUri: String): String {
        readError?.let { error(it) }
        return contents[documentUri] ?: error("missing file $documentUri")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var storage: FakeChecklistStorage
    private lateinit var settings: FakeSettingsRepository
    private lateinit var viewModel: ChecklistViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        storage = FakeChecklistStorage()
        settings = FakeSettingsRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ChecklistViewModel(storage, settings)
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun startsEmptyWithoutFolder() {
        createViewModel()

        val state = viewModel.uiState.value
        assertNull(state.folderUri)
        assertEquals(emptyList(), state.documents)
        assertEquals(0, storage.listCalls)
    }

    @Test
    fun loadsDocumentsWhenFolderIsSet() {
        storage.files = listOf(
            ChecklistDocument(uri = "b", name = "b.md"),
            ChecklistDocument(uri = "a", name = "a.md")
        )
        createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        // Folder arrives via settings flow; documents load on folder change.
        assertEquals(0, storage.listCalls)
        viewModel.setFolder("folder", "Docs")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("folder", state.folderUri)
        assertEquals("Docs", state.folderName)
        assertEquals(listOf("b.md", "a.md"), state.documents.map { it.name })
        assertNull(state.errorMessage)
    }

    @Test
    fun showsErrorWhenListingFails() {
        storage.listError = "no access"
        createViewModel()

        viewModel.setFolder("folder", null)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(emptyList(), state.documents)
        assertEquals("no access", state.errorMessage)
    }

    @Test
    fun openDocumentLoadsMarkdown() {
        storage.contents = mapOf("a" to "# Hello")
        createViewModel()

        viewModel.openDocument(ChecklistDocument(uri = "a", name = "hello.md"))
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("a", state.selectedUri)
        assertEquals("# Hello", state.selectedMarkdown)
        assertNull(state.selectedError)
    }

    @Test
    fun openDocumentSurfacesReadErrorAndRetry() {
        storage.readError = "unreadable"
        createViewModel()

        viewModel.openDocument(ChecklistDocument(uri = "a", name = "hello.md"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("unreadable", viewModel.uiState.value.selectedError)

        storage.readError = null
        storage.contents = mapOf("a" to "# Retry")
        viewModel.retrySelected()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("# Retry", state.selectedMarkdown)
        assertNull(state.selectedError)
    }

    @Test
    fun changingFolderClearsSelection() {
        storage.contents = mapOf("a" to "# Hello")
        createViewModel()

        viewModel.openDocument(ChecklistDocument(uri = "a", name = "hello.md"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("a", viewModel.uiState.value.selectedUri)

        viewModel.setFolder("other", null)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.selectedUri)
        assertNull(state.selectedMarkdown)
    }

    @Test
    fun picksUpInitialFolderFromSettings() {
        settings = FakeSettingsRepository(
            UserSettings(checklistFolderUri = "folder", checklistFolderName = "Docs")
        )
        storage.files = listOf(ChecklistDocument(uri = "a", name = "a.md"))
        createViewModel()

        val state = viewModel.uiState.value
        assertEquals("folder", state.folderUri)
        assertEquals(listOf("a.md"), state.documents.map { it.name })
    }
}

class FilterMarkdownDocumentsTest {
    @Test
    fun keepsOnlyMarkdownFilesSortedCaseInsensitively() {
        val result = filterMarkdownDocuments(
            listOf(
                "u3" to "notes.txt",
                "u2" to "B.MD",
                "u1" to "a.md",
                "u4" to "README"
            )
        )

        assertEquals(
            listOf(
                ChecklistDocument(uri = "u1", name = "a.md"),
                ChecklistDocument(uri = "u2", name = "B.MD")
            ),
            result
        )
    }

    @Test
    fun titleStripsExtension() {
        assertEquals("groceries", ChecklistDocument(uri = "u", name = "groceries.md").title)
        assertTrue(ChecklistDocument(uri = "u", name = ".md").title.isNotEmpty())
    }
}
