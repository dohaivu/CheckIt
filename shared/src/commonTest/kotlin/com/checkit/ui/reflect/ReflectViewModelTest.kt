package com.checkit.ui.reflect

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.JournalEntry
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewPeriod
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import com.checkit.domain.TagItem
import com.checkit.domain.endExclusive
import com.checkit.domain.usecase.BuildPeriodReviewDraftUseCase
import com.checkit.domain.usecase.ObservePeriodReviewsUseCase
import com.checkit.domain.usecase.SavePeriodReviewUseCase
import com.checkit.ui.UiEvent
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.tasks.FakeCheckItRepository
import com.checkit.ui.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReflectViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: ReflectViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        viewModel = createViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(repository: FakeCheckItRepository): ReflectViewModel =
        ReflectViewModel(
            repository = repository,
            observePeriodReviews = ObservePeriodReviewsUseCase(repository),
            savePeriodReview = SavePeriodReviewUseCase(repository),
            buildDraft = BuildPeriodReviewDraftUseCase()
        )

    @Test
    fun initialStateDefaultsToWeekAndCurrentDate() {
        val state = viewModel.uiState.value
        assertEquals(ReportPeriod.Week, state.selectedPeriod)
        assertEquals(today(), state.selectedDate)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun focusReviewMatchesSelectedPeriodAndDate() = runTest(dispatcher) {
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        repository.savePeriodReview(
            review(period = ReviewPeriod.Week, start = weekStart, content = "Week recap")
        )
        advanceUntilIdle()

        assertEquals("Week recap", viewModel.uiState.value.focusReview?.content)
    }

    @Test
    fun selectingDailyMovesFocusToDay() {
        viewModel.selectPeriod(ReportPeriod.Daily)
        assertEquals(ReviewPeriod.Day, viewModel.uiState.value.focus.period)
        assertEquals(today(), viewModel.uiState.value.focus.start)
    }

    @Test
    fun selectingMonthClampsDateToMonthStart() {
        viewModel.selectPeriod(ReportPeriod.Month)
        assertEquals(ReviewPeriod.Month, viewModel.uiState.value.focus.period)
        assertEquals(LocalDate(today().year, today().month, 1), viewModel.uiState.value.focus.start)
    }

    @Test
    fun previousAndNextPeriodMoveDate() {
        val before = viewModel.uiState.value.selectedDate
        viewModel.previousPeriod()
        val previous = viewModel.uiState.value.selectedDate
        assertEquals(before.minus(7, DateTimeUnit.DAY), previous)
        viewModel.nextPeriod()
        assertEquals(before, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun resetToCurrentPeriodRestoresToday() {
        viewModel.previousPeriod()
        viewModel.resetToCurrentPeriod()
        assertEquals(today(), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun openEditorPrefillsFromExistingReview() = runTest(dispatcher) {
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        repository.savePeriodReview(
            review(
                period = ReviewPeriod.Week,
                start = weekStart,
                content = "Existing content",
                intentNext = "Existing intent"
            )
        )
        advanceUntilIdle()

        viewModel.openEditor()
        val editor = assertNotNull(viewModel.uiState.value.editor)
        assertEquals("Existing content", editor.content)
        assertEquals("Existing intent", editor.intentNext)
    }

    @Test
    fun saveEditorPersistsAndDismisses() = runTest(dispatcher) {
        viewModel.openEditor()
        viewModel.updateEditorContent("Great week")
        viewModel.updateEditorIntentNext("Ship more")
        viewModel.saveEditor()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editor)
        val saved = repository.observePeriodReviews().first().single()
        assertEquals("Great week", saved.content)
        assertEquals("Ship more", saved.intentNext)
    }

    @Test
    fun saveEditorEmitsSnackbar() = runTest(dispatcher) {
        viewModel.openEditor()
        viewModel.updateEditorContent("Done")
        viewModel.saveEditor()
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertTrue(event is UiEvent.ShowSnackbar)
    }

    @Test
    fun generateDraftOpensEditorWithDraftContent() = runTest(dispatcher) {
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = weekStart,
                    items = listOf(
                        item(
                            id = 1L,
                            title = "Deep work",
                            status = DailyPlanItemStatus.Done,
                            startTimeMinutes = 9 * 60,
                            endTimeMinutes = 10 * 60,
                            tags = listOf(TagItem(id = 1L, name = "Work", color = "#2563EB"))
                        ),
                        item(
                            id = 2L,
                            title = "Shipped PR",
                            status = DailyPlanItemStatus.Done,
                            startTimeMinutes = 14 * 60,
                            endTimeMinutes = 15 * 60
                        )
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.generateDraft()
        advanceUntilIdle()

        val editor = assertNotNull(viewModel.uiState.value.editor)
        assertTrue(editor.isDraft)
        assertEquals(ReviewSource.Hybrid, editor.source)
        assertTrue(editor.content.contains("2 items"))
        assertTrue(editor.content.contains("Work"))
        assertNotNull(editor.statsJson)
        assertNotNull(editor.highlightsJson)
    }

    @Test
    fun generateDraftWithoutActivityOpensEmptyManualEditor() = runTest(dispatcher) {
        viewModel.generateDraft()
        advanceUntilIdle()

        val editor = assertNotNull(viewModel.uiState.value.editor)
        assertEquals(false, editor.isDraft)
        assertEquals(ReviewSource.Manual, editor.source)
        assertEquals("", editor.content)
    }

    @Test
    fun generateDraftSeedsFromHighestLevelReviewCoveringFocus() = runTest(dispatcher) {
        val yearStart = LocalDate(today().year, 1, 1)
        repository.savePeriodReview(
            review(
                period = ReviewPeriod.Year,
                start = yearStart,
                content = "Annual context"
            )
        )
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = weekStart,
                    items = listOf(
                        item(
                            id = 1L,
                            title = "Deep work",
                            status = DailyPlanItemStatus.Done,
                            startTimeMinutes = 9 * 60,
                            endTimeMinutes = 10 * 60
                        )
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.generateDraft()
        advanceUntilIdle()

        val editor = assertNotNull(viewModel.uiState.value.editor)
        assertTrue(editor.isDraft)
        assertTrue(editor.content.startsWith("Annual context"))
    }

    @Test
    fun savingGeneratedDraftPersistsHybridSourceAndStats() = runTest(dispatcher) {
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = weekStart,
                    items = listOf(
                        item(
                            id = 1L,
                            title = "Deep work",
                            status = DailyPlanItemStatus.Done,
                            startTimeMinutes = 9 * 60,
                            endTimeMinutes = 10 * 60
                        )
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.generateDraft()
        advanceUntilIdle()
        viewModel.updateEditorIntentNext("Keep shipping")
        viewModel.saveEditor()
        advanceUntilIdle()

        val saved = repository.observePeriodReviews().first().single()
        assertEquals(ReviewSource.Hybrid, saved.source)
        assertEquals("Keep shipping", saved.intentNext)
        assertNotNull(saved.statsJson)
        assertNotNull(saved.highlightsJson)
        assertNotNull(saved.generatedAtMillis)
    }

    @Test
    fun zoomInAndZoomOutNavigateLevels() {
        assertEquals(ReportPeriod.Week, viewModel.uiState.value.selectedPeriod)
        viewModel.zoomIn()
        assertEquals(ReportPeriod.Daily, viewModel.uiState.value.selectedPeriod)
        viewModel.zoomOut()
        assertEquals(ReportPeriod.Week, viewModel.uiState.value.selectedPeriod)
        viewModel.zoomOut()
        assertEquals(ReportPeriod.Month, viewModel.uiState.value.selectedPeriod)
        viewModel.zoomIn()
        assertEquals(ReportPeriod.Week, viewModel.uiState.value.selectedPeriod)
    }

    @Test
    fun openReviewMovesFocusAndOpensEditor() = runTest(dispatcher) {
        val monthStart = LocalDate(today().year, today().month, 1)
        repository.savePeriodReview(
            review(period = ReviewPeriod.Month, start = monthStart, content = "Monthly recap")
        )
        advanceUntilIdle()

        val record = repository.observePeriodReviews().first().single()
        viewModel.openReview(record)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReportPeriod.Month, state.selectedPeriod)
        assertEquals(monthStart, state.selectedDate)
        val editor = assertNotNull(state.editor)
        assertEquals("Monthly recap", editor.content)
    }

    @Test
    fun historyListsReviewsNewestFirst() = runTest(dispatcher) {
        val todayDate = today()
        val yesterday = todayDate.minus(1, DateTimeUnit.DAY)
        val twoWeeksAgo = todayDate.minus(14, DateTimeUnit.DAY)
        repository.savePeriodReview(
            review(period = ReviewPeriod.Day, start = todayDate, content = "Today")
        )
        repository.savePeriodReview(
            review(period = ReviewPeriod.Day, start = yesterday, content = "Yesterday")
        )
        repository.savePeriodReview(
            review(period = ReviewPeriod.Week, start = twoWeeksAgo, content = "Two weeks ago")
        )
        advanceUntilIdle()

        val dates = viewModel.uiState.value.history.map { it.periodStartDate }
        assertEquals(listOf(todayDate, yesterday, twoWeeksAgo), dates)
    }

    @Test
    fun zoomInToAnchorsOnGivenDate() {
        val monday = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        viewModel.zoomInTo(monday)
        assertEquals(ReportPeriod.Daily, viewModel.uiState.value.selectedPeriod)
        assertEquals(monday, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun focusDayJumpsStraightToThatDay() {
        val yesterday = today().minus(1, DateTimeUnit.DAY)
        viewModel.focusDay(yesterday)
        val state = viewModel.uiState.value
        assertEquals(ReportPeriod.Daily, state.selectedPeriod)
        assertEquals(yesterday, state.selectedDate)
    }

    @Test
    fun zoomOutToUsesBreadcrumbLevelKeepingAnchor() {
        val anchor = today()
        viewModel.zoomOutTo(ReportPeriod.Month)
        val state = viewModel.uiState.value
        assertEquals(ReportPeriod.Month, state.selectedPeriod)
        assertEquals(anchor, state.selectedDate)
    }

    @Test
    fun statsDeriveDoneMinutesAndJournalsForFocusedWeek() = runTest(dispatcher) {
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = weekStart,
                    items = listOf(
                        item(
                            id = 1L,
                            title = "Deep work",
                            status = DailyPlanItemStatus.Done,
                            startTimeMinutes = 9 * 60,
                            endTimeMinutes = 10 * 60
                        ),
                        item(id = 2L, title = "Open task", status = DailyPlanItemStatus.Planned)
                    )
                )
            )
        )
        repository.setJournalEntries(
            listOf(
                JournalEntry(
                    id = 1L,
                    dateEpochDays = weekStart.toEpochDays().toInt(),
                    context = "Cafe",
                    content = "Coffee",
                    createdTimeMinutes = 1
                )
            )
        )
        advanceUntilIdle()

        val stats = viewModel.uiState.value.stats
        assertEquals(1, stats.doneCount)
        assertEquals(60, stats.totalMinutes)
        assertEquals(1, stats.journalCount)
    }

    @Test
    fun weekChildrenReturnSevenDays() {
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        val children = viewModel.uiState.value.children
        assertEquals(7, children.size)
        assertEquals(ReviewPeriod.Day, children.first().period)
        assertEquals(weekStart, children.first().start)
        assertEquals(weekStart.plus(6, DateTimeUnit.DAY), children.last().start)
    }

    @Test
    fun monthChildrenReturnWeeksInsideMonth() {
        viewModel.selectPeriod(ReportPeriod.Month)
        val children = viewModel.uiState.value.children
        assertTrue(children.isNotEmpty())
        assertTrue(children.all { it.period == ReviewPeriod.Week })
        assertTrue(children.all { it.start < viewModel.uiState.value.focus.endExclusive })
        assertEquals(children.first().start, viewModel.uiState.value.focus.start.minus(
            viewModel.uiState.value.focus.start.dayOfWeek.ordinal, DateTimeUnit.DAY
        ))
    }

    @Test
    fun yearChildrenReturnTwelveMonths() {
        viewModel.selectPeriod(ReportPeriod.Annual)
        val children = viewModel.uiState.value.children
        assertEquals(12, children.size)
        assertEquals(LocalDate(today().year, 1, 1), children.first().start)
        assertEquals(LocalDate(today().year, 12, 1), children.last().start)
    }

    @Test
    fun hasReviewFlagsChildDay() = runTest(dispatcher) {
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        repository.savePeriodReview(
            review(period = ReviewPeriod.Day, start = weekStart, content = "Won the day")
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasReview(state.children.first()))
        assertTrue(!state.hasReview(state.children.last()))
    }

    private fun review(
        period: ReviewPeriod,
        start: LocalDate,
        content: String,
        intentNext: String? = null
    ) = PeriodReview(
        id = 0L,
        period = period,
        periodStartEpochDays = start.toEpochDays().toInt(),
        periodEndEpochDays = period.endExclusive(start).toEpochDays().toInt(),
        content = content,
        intentNext = intentNext,
        source = ReviewSource.Manual,
        status = ReviewStatus.Complete,
        completedAtMillis = 1L,
        editedAtMillis = 1L
    )

    private fun item(
        id: Long,
        title: String,
        status: DailyPlanItemStatus,
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null,
        tags: List<TagItem> = emptyList(),
        dateEpochDays: Int = today().toEpochDays().toInt()
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = dateEpochDays,
        title = title,
        source = DailyPlanItemSource.MyDayTask,
        status = status,
        tags = tags,
        sortOrder = id.toInt(),
        startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes,
        addedAtMillis = 0L,
        completedAtMillis = if (status == DailyPlanItemStatus.Done) 1L else null
    )
}
