package com.checkit.ui.journal

import com.checkit.domain.JournalEntry
import com.checkit.domain.MoodFilter
import com.checkit.domain.TagItem
import com.checkit.domain.usecase.ObservePeriodGoalsUseCase
import com.checkit.domain.usecase.ObserveTagsUseCase
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import com.checkit.ui.today
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
                entry(id = 1L, day = today().minus(3, DateTimeUnit.DAY), content = "Great run", moods = listOf("😊"), tagIds = listOf(1L)),
                entry(id = 2L, day = today().minus(2, DateTimeUnit.DAY), content = "Tough day", moods = listOf("😢"), label = "work"),
                entry(id = 3L, day = today().minus(1, DateTimeUnit.DAY), content = "Quiet evening"),
                // Outside the initial 7-day window; reachable via loadOlder().
                entry(id = 4L, day = today().minus(10, DateTimeUnit.DAY), content = "Old memory"),
                // Even older: proves hasOlder stays true after one expansion.
                entry(id = 5L, day = today().minus(40, DateTimeUnit.DAY), content = "Ancient memory")
            )
        )
        repository.setDayGoals(
            listOf(
                review(day = today().minus(2, DateTimeUnit.DAY), content = "A review")
            )
        )
        viewModel = JournalHistoryViewModel(
            repository = repository,
            observeTags = ObserveTagsUseCase(repository),
            observePeriodGoals = ObservePeriodGoalsUseCase(repository)
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsRecentWindowNewestFirst() {
        val state = viewModel.uiState.value
        assertEquals(listOf(3L, 2L, 1L), state.entries.map { it.id })
        assertEquals(true, state.hasOlder)
    }

    @Test
    fun loadOlderExpandsWindowToIncludeOlderEntries() {
        viewModel.loadOlder()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(3L, 2L, 1L, 4L), viewModel.uiState.value.entries.map { it.id })
        assertTrue(viewModel.uiState.value.hasOlder)
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

    @Test
    fun dayReviewsEmptyWhenFiltersActive() {
        // Initial state: has review
        assertEquals(1, viewModel.uiState.value.dayGoals.size)

        // Apply filter
        viewModel.updateSearchText("run")
        dispatcher.scheduler.advanceUntilIdle()

        // Reviews should be hidden
        assertEquals(emptyList(), viewModel.uiState.value.dayGoals)

        // Clear filter
        viewModel.clearFilters()
        dispatcher.scheduler.advanceUntilIdle()

        // Reviews should return
        assertEquals(1, viewModel.uiState.value.dayGoals.size)
    }

    private fun review(day: LocalDate, content: String) = com.checkit.domain.PeriodGoal(
        startEpochDays = day.toEpochDays().toInt(),
        endEpochDays = day.toEpochDays().toInt() + 1,
        review = content
    )

    private fun entry(
        id: Long,
        day: LocalDate,
        content: String,
        moods: List<String> = emptyList(),
        label: String? = null,
        tagIds: List<Long> = emptyList()
    ) = JournalEntry(
        id = id,
        dateEpochDays = day.toEpochDays().toInt(),
        label = label,
        content = content,
        moods = moods,
        tags = tagIds.map { TagItem(id = it, name = "Tag $it", color = "#FFFFFF") },
        createdTimeMinutes = 0,
        attachments = emptyList()
    )
}
