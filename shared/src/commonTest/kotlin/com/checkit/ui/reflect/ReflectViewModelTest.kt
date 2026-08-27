package com.checkit.ui.reflect

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.JournalEntry
import com.checkit.domain.PeriodGoal
import com.checkit.domain.Period


import com.checkit.domain.TagItem
import com.checkit.domain.endExclusive
import com.checkit.domain.usecase.ObservePeriodGoalsUseCase
import com.checkit.domain.usecase.SavePeriodGoalUseCase
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
            observePeriodGoals = ObservePeriodGoalsUseCase(repository),
            savePeriodGoal = SavePeriodGoalUseCase(repository),
            dataDispatcher = dispatcher
        )

    @Test
    fun initialStateDefaultsToWeekAndCurrentDate() {
        val state = viewModel.uiState.value
        assertEquals(ReportPeriod.Week, state.selectedPeriod)
        assertEquals(today(), state.selectedDate)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun focusGoalMatchesSelectedPeriodAndDate() = runTest(dispatcher) {
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        repository.savePeriodGoal(
            review(period = Period.Week, start = weekStart, content = "Week recap")
        )
        advanceUntilIdle()

        assertEquals("Week recap", viewModel.uiState.value.focusGoal?.review)
    }

    @Test
    fun selectingDailyMovesFocusToDay() {
        viewModel.selectPeriod(ReportPeriod.Daily)
        assertEquals(Period.Day, viewModel.uiState.value.focus.period)
        assertEquals(today(), viewModel.uiState.value.focus.start)
    }

    @Test
    fun selectingMonthClampsDateToMonthStart() {
        viewModel.selectPeriod(ReportPeriod.Month)
        assertEquals(Period.Month, viewModel.uiState.value.focus.period)
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
        repository.savePeriodGoal(
            review(
                period = Period.Week,
                start = weekStart,
                content = "Existing content",
                periodIntent = "Existing intent"
            )
        )
        advanceUntilIdle()

        viewModel.openEditor()
        advanceUntilIdle()
        val editor = assertNotNull(viewModel.editor.value)
        assertEquals("Existing content", editor.review)
        assertEquals("Existing intent", editor.goal)
    }

    @Test
    fun saveEditorPersistsAndDismisses() = runTest(dispatcher) {
        viewModel.openEditor()
        viewModel.updateEditorReview("Great week")
        viewModel.updateEditorGoal("Ship more")
        viewModel.saveEditor()
        advanceUntilIdle()

        assertNull(viewModel.editor.value)
        val goals = repository.observePeriodGoals().first().single()
        assertEquals("Great week", goals.review)
        assertEquals("Ship more", goals.goal)
    }

    @Test
    fun saveEditorEmitsSnackbar() = runTest(dispatcher) {
        viewModel.openEditor()
        viewModel.updateEditorReview("Done")
        viewModel.saveEditor()
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertTrue(event is UiEvent.ShowSnackbar)
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
    fun openGoalMovesFocusAndOpensEditor() = runTest(dispatcher) {
        val monthStart = LocalDate(today().year, today().month, 1)
        repository.savePeriodGoal(
            review(period = Period.Month, start = monthStart, content = "Monthly recap")
        )
        advanceUntilIdle()

        val record = repository.observePeriodGoals().first().single()
        viewModel.openGoal(record)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReportPeriod.Month, state.selectedPeriod)
        assertEquals(monthStart, state.selectedDate)
        val editor = assertNotNull(viewModel.editor.value)
        assertEquals("Monthly recap", editor.review)
    }

    @Test
    fun goalsForSelectedPeriodShowChildPeriodWithinWindow() = runTest(dispatcher) {
        val weekStart = today().minus(today().dayOfWeek.ordinal, DateTimeUnit.DAY)
        repository.savePeriodGoal(
            review(period = Period.Day, start = weekStart, content = "Mon")
        )
        repository.savePeriodGoal(
            review(period = Period.Day, start = weekStart.plus(1, DateTimeUnit.DAY), content = "Tue")
        )
        repository.savePeriodGoal(
            review(period = Period.Day, start = weekStart.minus(1, DateTimeUnit.DAY), content = "Outside")
        )
        repository.savePeriodGoal(
            review(period = Period.Week, start = weekStart, content = "Week excluded")
        )
        advanceUntilIdle()

        // Week view shows only this week's Day reviews, newest first.
        val weekDates = viewModel.uiState.value.goalsForSelectedPeriod.map { it.startDate }
        assertEquals(listOf(weekStart.plus(1, DateTimeUnit.DAY), weekStart), weekDates)

        // Daily behaves like Week: the same week's Day reviews.
        viewModel.selectPeriod(ReportPeriod.Daily)
        assertEquals(weekDates, viewModel.uiState.value.goalsForSelectedPeriod.map { it.startDate })
    }

    @Test
    fun monthViewShowsWeekReviewsWithinMonth() = runTest(dispatcher) {
        val monthStart = today().firstDayOfMonth()
        val inside = monthStart
        val outside = monthStart.minus(1, DateTimeUnit.DAY)
        repository.savePeriodGoal(
            review(period = Period.Week, start = inside, content = "Week inside")
        )
        repository.savePeriodGoal(
            review(period = Period.Week, start = outside, content = "Week outside")
        )
        repository.savePeriodGoal(
            review(period = Period.Month, start = monthStart, content = "Month excluded")
        )
        advanceUntilIdle()

        viewModel.selectPeriod(ReportPeriod.Month)
        advanceUntilIdle()
        val dates = viewModel.uiState.value.goalsForSelectedPeriod.map { it.startDate }
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
                    label = "Cafe",
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
        period: Period,
        start: LocalDate,
        content: String,
        periodIntent: String? = null
    ) = PeriodGoal(
        id = 0L,
        period = period,
        startEpochDays = start.toEpochDays().toInt(),
        endEpochDays = period.endExclusive(start).toEpochDays().toInt(),
        review = content,
        goal = periodIntent,
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
