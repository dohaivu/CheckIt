package com.checkit.ui.journal

import com.checkit.domain.JournalEntry
import com.checkit.domain.MoodFilter
import com.checkit.domain.TagItem
import com.checkit.domain.usecase.ObserveTagsUseCase
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class JournalHistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: JournalHistoryViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        repository.setJournalEntries(
            listOf(
                entry(id = 1L, day = 20_000, content = "Great run", moods = listOf("😊"), tagIds = listOf(1L)),
                entry(id = 2L, day = 20_001, content = "Tough day", moods = listOf("😢"), label = "work"),
                entry(id = 3L, day = 20_002, content = "Quiet evening")
            )
        )
        viewModel = JournalHistoryViewModel(
            repository = repository,
            observeTags = ObserveTagsUseCase(repository)
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsAllEntriesNewestFirst() {
        assertEquals(listOf(3L, 2L, 1L), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun moodFilterKeepsOnlyMatchingCategory() {
        viewModel.toggleMood(MoodFilter.Good)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1L), viewModel.uiState.value.entries.map { it.id })

        viewModel.toggleMood(MoodFilter.Bad)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun togglingSameMoodAgainClearsIt() {
        viewModel.toggleMood(MoodFilter.Good)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleMood(MoodFilter.Good)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.entries.size)
        assertNull(viewModel.uiState.value.filters.mood)
    }

    @Test
    fun searchMatchesContentOrLabelCaseInsensitive() {
        viewModel.updateSearchText("RUN")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1L), viewModel.uiState.value.entries.map { it.id })

        viewModel.updateSearchText("work")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.entries.map { it.id })
    }

    private fun entry(
        id: Long,
        day: Int,
        content: String,
        moods: List<String> = emptyList(),
        label: String? = null,
        tagIds: List<Long> = emptyList()
    ) = JournalEntry(
        id = id,
        dateEpochDays = day,
        label = label,
        content = content,
        moods = moods,
        tags = tagIds.map { TagItem(id = it, name = "Tag $it", color = "#FFFFFF") },
        createdTimeMinutes = 0,
        attachments = emptyList()
    )
}
