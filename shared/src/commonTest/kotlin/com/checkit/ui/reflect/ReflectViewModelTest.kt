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
import com.checkit.ui.firstDayOfMonth
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
        val editor = assertNotNull(viewModel.editor.value)
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

        assertNull(viewModel.editor.value)
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

        val editor = assertNotNull(viewModel.editor.value)
        assertEquals(ReviewSource.Auto, editor.source)
        assertTrue(editor.content.contains("2 items"))
        assertTrue(editor.content.contains("Work"))
        assertNotNull(editor.statsJson)
        assertNotNull(editor.highlightsJson)
    }

    @Test
    fun generateDraftWithoutActivityOpensEmptyManualEditor() = runTest(dispatcher) {
        viewModel.generateDraft()
        advanceUntilIdle()

        val editor = assertNotNull(viewModel.editor.value)
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

        val editor = assertNotNull(viewModel.editor.value)
        assertEquals(ReviewSource.Auto, editor.source)
        assertTrue(editor.content.startsWith("Annual context"))
    }

    @Test
    fun savingGeneratedDraftPersistsAutoSourceAndStats() = runTest(dispatcher) {
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
        assertEquals(ReviewSource.Auto, saved.source)
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
        val editor = assertNotNull(viewModel.editor.value)
        assertEquals("Monthly recap", editor.content)
    }

    @Test
    fun reviewsForSelectedPeriodShowChildPeriodWithinWindow() = runTest(dispatcher) {
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        repository.savePeriodReview(
            review(period = ReviewPeriod.Day, start = weekStart, content = "Mon")
        )
        repository.savePeriodReview(
            review(period = ReviewPeriod.Day, start = weekStart.plus(1, DateTimeUnit.DAY), content = "Tue")
        )
        repository.savePeriodReview(
            review(period = ReviewPeriod.Day, start = weekStart.minus(1, DateTimeUnit.DAY), content = "Outside")
        )
        repository.savePeriodReview(
            review(period = ReviewPeriod.Week, start = weekStart, content = "Week excluded")
        )
        advanceUntilIdle()

        // Week view shows only this week's Day reviews, newest first.
        val weekDates = viewModel.uiState.value.reviewsForSelectedPeriod.map { it.periodStartDate }
        assertEquals(listOf(weekStart.plus(1, DateTimeUnit.DAY), weekStart), weekDates)

        // Daily behaves like Week: the same week's Day reviews.
        viewModel.selectPeriod(ReportPeriod.Daily)
        assertEquals(weekDates, viewModel.uiState.value.reviewsForSelectedPeriod.map { it.periodStartDate })
    }

    @Test
    fun monthViewShowsWeekReviewsWithinMonth() = runTest(dispatcher) {
        val monthStart = today().firstDayOfMonth()
        val inside = monthStart
        val outside = monthStart.minus(1, DateTimeUnit.DAY)
        repository.savePeriodReview(
            review(period = ReviewPeriod.Week, start = inside, content = "Week inside")
        )
        repository.savePeriodReview(
            review(period = ReviewPeriod.Week, start = outside, content = "Week outside")
        )
        repository.savePeriodReview(
            review(period = ReviewPeriod.Month, start = monthStart, content = "Month excluded")
        )
        advanceUntilIdle()

        viewModel.selectPeriod(ReportPeriod.Month)
        val dates = viewModel.uiState.value.reviewsForSelectedPeriod.map { it.periodStartDate }
        assertEquals(listOf(inside), dates)
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

        val digest = viewModel.uiState.value.digestReport
        assertEquals(1, digest.doneItemCount)
        assertEquals(60, digest.totalMinutes)
        assertEquals(1, digest.journalCount)
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
