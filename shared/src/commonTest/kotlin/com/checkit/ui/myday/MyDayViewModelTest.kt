package com.checkit.ui.myday

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DayReviewRecord
import com.checkit.domain.LeftoverAction
import com.checkit.domain.usecase.AddDailyPlanItemUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.BuildDayReviewSummaryUseCase
import com.checkit.domain.usecase.CarryOverDailyPlanItemsUseCase
import com.checkit.domain.usecase.CompleteDayReviewUseCase
import com.checkit.domain.usecase.ObserveDayReviewsUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.SyncKeyResultFromDailyPlanUseCase
import com.checkit.domain.SprintManager
import com.checkit.domain.usecase.UpdateDailyPlanItemStatusUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpsertDailyPlanItemUseCase
import com.checkit.domain.usecase.AddSuggestedTaskToMyDayUseCase
import com.checkit.domain.usecase.SprintTransitionUseCase
import com.checkit.domain.usecase.SaveSprintAsWinUseCase
import com.checkit.notifications.NoOpSprintNotificationScheduler
import com.checkit.ui.tasks.FakeCheckItRepository
import com.checkit.ui.tasks.FakeSettingsRepository
import com.checkit.ui.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyDayViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var viewModel: MyDayViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        settingsRepository = FakeSettingsRepository()
        viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()
    }

    private fun createViewModel(): MyDayViewModel {
        val buildSummary = BuildDayReviewSummaryUseCase(dispatcher)
        val carryOver = CarryOverDailyPlanItemsUseCase(repository, dispatcher)
        val observeTaskBoard = ObserveTaskBoardUseCase(repository)
        val observeDailyPlans = ObserveDailyPlansUseCase(repository)
        val observeDayReviews = ObserveDayReviewsUseCase(repository)
        val syncKeyResult = SyncKeyResultFromDailyPlanUseCase(repository)
        val addTaskToDailyPlan = AddTaskToDailyPlanUseCase(repository)
        val updateDailyPlanItemTime = UpdateDailyPlanItemTimeUseCase(repository)

        return MyDayViewModel(
            observeTaskBoard = observeTaskBoard,
            observeDailyPlans = observeDailyPlans,
            ensureDefaultTaskData = EnsureDefaultTaskDataUseCase(repository),
            deleteDailyPlanItemUseCase = DeleteDailyPlanItemUseCase(repository),
            settingsRepository = settingsRepository,
            buildDayReviewSummary = buildSummary,
            completeDayReview = CompleteDayReviewUseCase(
                repository = repository,
                settingsRepository = settingsRepository,
                buildSummary = buildSummary,
                dispatcher = dispatcher
            ),
            carryOverDailyPlanItems = carryOver,
            observeDayReviews = observeDayReviews,
            upsertDailyPlanItem = UpsertDailyPlanItemUseCase(repository, syncKeyResult),
            addSuggestedTaskToMyDay = AddSuggestedTaskToMyDayUseCase(
                repository = repository,
                addTaskToDailyPlan = addTaskToDailyPlan,
                updateDailyPlanItemTime = updateDailyPlanItemTime
            ),
            syncKeyResultFromDailyPlan = syncKeyResult,
            updateDailyPlanItemTime = updateDailyPlanItemTime,
            sprintManager = SprintManager(NoOpSprintNotificationScheduler()),
            sprintTransition = SprintTransitionUseCase(
                sprintManager = SprintManager(NoOpSprintNotificationScheduler()), // Separate instance for transition if needed or reuse
                saveSprintAsWin = SaveSprintAsWinUseCase(
                    repository = repository,
                    addTaskToDailyPlan = addTaskToDailyPlan,
                    addDailyPlanItem = AddDailyPlanItemUseCase(repository),
                    updateDailyPlanItemTime = updateDailyPlanItemTime,
                    updateDailyPlanItemStatus = UpdateDailyPlanItemStatusUseCase(repository),
                    syncKeyResultFromDailyPlan = syncKeyResult
                ),
                observeTaskBoard = observeTaskBoard
            )
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openDayReviewPrefillsExistingWinNoteFromHistory() = runTest(dispatcher) {
        val today = today()
        repository.setDayReviews(
            listOf(
                DayReviewRecord(
                    date = today,
                    doneCount = 1,
                    plannedCount = 0,
                    doneMinutes = 30,
                    winNote = "Shipped the review loop",
                    completedAtMillis = 1L
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDayReview()
        dispatcher.scheduler.advanceUntilIdle()

        val review = viewModel.uiState.value.dayReview
        assertNotNull(review)
        assertEquals("Shipped the review loop", review.winNote)
    }

    @Test
    fun addCheckInWithoutTimePersistsPlannedNote() = runTest(dispatcher) {
        viewModel.openDailyPlan()
        viewModel.updateEditorSource(DailyPlanItemSource.MyDayNote)
        viewModel.updateTitle("Draft proposal")

        viewModel.addDailyPlan()
        dispatcher.scheduler.advanceUntilIdle()

        val item = repository.addedManualDailyPlanItems.single()
        assertEquals(DailyPlanItemSource.MyDayNote, item.source)
        assertEquals(DailyPlanItemStatus.Done, item.status)
        assertEquals(null, item.startTimeMinutes)
        assertEquals(null, item.endTimeMinutes)
    }

    @Test
    fun addNoteWithStartTimeDoesNotInferDoneItem() = runTest(dispatcher) {
        viewModel.openDailyPlan(startTimeMinutes = 0, endTimeMinutes = 30)
        viewModel.updateEditorSource(DailyPlanItemSource.MyDayNote)
        viewModel.updateTitle("Morning thought")

        viewModel.addDailyPlan()
        dispatcher.scheduler.advanceUntilIdle()

        val item = repository.addedManualDailyPlanItems.single()
        assertEquals(DailyPlanItemSource.MyDayNote, item.source)
        assertEquals(DailyPlanItemStatus.Done, item.status)
        assertEquals(0, item.startTimeMinutes)
        assertEquals(null, item.endTimeMinutes)
    }

    @Test
    fun addReminderPersistsStartTimeOnlyAndPlannedStatus() = runTest(dispatcher) {
        viewModel.openDailyPlan(startTimeMinutes = 23 * 60 + 59, endTimeMinutes = null)
        viewModel.updateEditorSource(DailyPlanItemSource.MyDayReminder)
        viewModel.updateTitle("Send invoice")

        viewModel.addDailyPlan()
        dispatcher.scheduler.advanceUntilIdle()

        val reminder = repository.addedManualDailyPlanItems.single()
        assertEquals(DailyPlanItemSource.MyDayReminder, reminder.source)
        assertEquals(DailyPlanItemStatus.Planned, reminder.status)
        assertEquals(23 * 60 + 59, reminder.startTimeMinutes)
        assertEquals(null, reminder.endTimeMinutes)
    }

    @Test
    fun editTitleAndNoteSaveAfterDebounce() = runTest(dispatcher) {
        viewModel.openItemEditor(dailyPlanItem(), TestDate)

        viewModel.updateTitle("Dr")
        viewModel.updateTitle("Draft")
        viewModel.updateNote("Feels tidy")

        assertEquals(0, repository.updatedDailyPlanItems.size)

        dispatcher.scheduler.advanceTimeBy(599)
        assertEquals(0, repository.updatedDailyPlanItems.size)

        dispatcher.scheduler.advanceTimeBy(1)
        dispatcher.scheduler.advanceUntilIdle()

        val (_, input) = repository.updatedDailyPlanItems.single()
        assertEquals("Draft", input.title)
        assertEquals("Feels tidy", input.note)
    }

    @Test
    fun immediateEditSavesOnceAndCancelsPendingTextSave() = runTest(dispatcher) {
        viewModel.openItemEditor(
            dailyPlanItem(
                source = DailyPlanItemSource.MyDayTask,
                status = DailyPlanItemStatus.Planned,
                startTimeMinutes = 60,
                endTimeMinutes = 90
            ),
            TestDate
        )
        viewModel.updateTitle("Updated win")

        viewModel.updateStatus(isDone = true)
        dispatcher.scheduler.advanceUntilIdle()

        val (_, input) = repository.updatedDailyPlanItems.single()
        assertEquals("Updated win", input.title)
        assertEquals(DailyPlanItemStatus.Done, input.status)

        dispatcher.scheduler.advanceTimeBy(600)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repository.updatedDailyPlanItems.size)
    }

    @Test
    fun dismissFlushesPendingEditTextSave() = runTest(dispatcher) {
        viewModel.openItemEditor(dailyPlanItem(), TestDate)
        viewModel.updateTitle("Closed quickly")

        viewModel.dismissDailyPlanEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val (_, input) = repository.updatedDailyPlanItems.single()
        assertEquals("Closed quickly", input.title)
    }

    @Test
    fun duplicateDailyPlanItemCopiesFieldsAndPlacesAtNextAvailableSlot() = runTest(dispatcher) {
        val today = today()
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = today,
                    items = listOf(
                        DailyPlanItem(
                            id = 42L,
                            dateEpochDays = today.toEpochDays().toInt(),
                            title = "Original",
                            note = "Old note",
                            source = DailyPlanItemSource.MyDayTask,
                            status = DailyPlanItemStatus.Planned,
                            sortOrder = 0,
                            startTimeMinutes = 600,
                            endTimeMinutes = 645,
                            addedAtMillis = 0L
                        )
                    )
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openItemEditor(viewModel.uiState.value.plan!!.items.single(), today)
        viewModel.duplicateDailyPlanItem()
        dispatcher.scheduler.advanceUntilIdle()

        val editor = viewModel.uiState.value.itemEditor
        assertNotNull(editor)
        assertTrue(editor.isAddMode)
        assertEquals(null, editor.itemId)
        assertEquals("Original", editor.title)
        assertEquals("Old note", editor.note)
        assertEquals(DailyPlanItemSource.MyDayTask, editor.source)
        assertNotNull(editor.startTimeMinutes)
        assertNotNull(editor.endTimeMinutes)
        // The copy lands in a free slot, never reusing the source item's (600, 645) range.
        assertTrue(editor.startTimeMinutes >= 645 || editor.endTimeMinutes <= 600)
        assertEquals(45, editor.endTimeMinutes - editor.startTimeMinutes)
    }

    @Test
    fun reOpenDayReviewExcludesAlreadyHandledItems() = runTest(dispatcher) {
        val today = today()
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = today,
                    items = listOf(
                        DailyPlanItem(
                            id = 7L,
                            dateEpochDays = today.toEpochDays().toInt(),
                            title = "Standalone task",
                            source = DailyPlanItemSource.MyDayTask,
                            status = DailyPlanItemStatus.Planned,
                            sortOrder = 0,
                            addedAtMillis = 0L
                        )
                    )
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDayReview()
        dispatcher.scheduler.advanceUntilIdle()
        val first = viewModel.uiState.value.dayReview
        assertNotNull(first)
        assertEquals(LeftoverAction.None, first.actionFor(first.summary.plannedItems.single()))

        viewModel.setLeftoverAction(7L, LeftoverAction.Drop)
        viewModel.confirmDayReview()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDayReview()
        dispatcher.scheduler.advanceUntilIdle()
        val reopened = viewModel.uiState.value.dayReview
        assertNotNull(reopened)
        assertTrue(reopened.summary.plannedItems.none { it.id == 7L })
        assertEquals(listOf(7L), reopened.summary.alreadyCarriedItems.map { it.id })
        assertEquals(LeftoverAction.Drop, reopened.actionFor(reopened.summary.alreadyCarriedItems.single()))
    }

    @Test
    fun reopenReviewPreselectsCarryOverWhenCopyExistsTomorrow() = runTest(dispatcher) {
        val today = today()
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = today,
                    items = listOf(
                        DailyPlanItem(
                            id = 7L,
                            dateEpochDays = today.toEpochDays().toInt(),
                            title = "Standalone task",
                            source = DailyPlanItemSource.MyDayTask,
                            status = DailyPlanItemStatus.Planned,
                            sortOrder = 0,
                            addedAtMillis = 0L,
                            handledAtMillis = 100L
                        )
                    )
                ),
                DailyPlan(
                    date = tomorrow,
                    items = listOf(
                        DailyPlanItem(
                            id = 70L,
                            dateEpochDays = tomorrow.toEpochDays().toInt(),
                            title = "Standalone task (tomorrow)",
                            source = DailyPlanItemSource.MyDayTask,
                            status = DailyPlanItemStatus.Planned,
                            sortOrder = 0,
                            addedAtMillis = 100L,
                            carriedFromItemId = 7L
                        )
                    )
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDayReview()
        dispatcher.scheduler.advanceUntilIdle()
        val reopened = viewModel.uiState.value.dayReview
        assertNotNull(reopened)
        assertEquals(listOf(7L), reopened.summary.alreadyCarriedItems.map { it.id })
        assertEquals(LeftoverAction.CarryOver, reopened.actionFor(reopened.summary.alreadyCarriedItems.single()))
    }

    @Test
    fun openDayReviewBeforeDataLoadUsesLatestPlanNotEmptyState() = runTest(dispatcher) {
        val today = today()
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = today,
                    items = listOf(
                        DailyPlanItem(
                            id = 9L,
                            dateEpochDays = today.toEpochDays().toInt(),
                            title = "Real item",
                            source = DailyPlanItemSource.MyDayTask,
                            status = DailyPlanItemStatus.Planned,
                            sortOrder = 0,
                            addedAtMillis = 0L
                        )
                    )
                )
            )
        )
        // Notification tap on cold start: a fresh ViewModel whose loader has not emitted yet.
        val coldStartViewModel = createViewModel()
        coldStartViewModel.openDayReview()
        dispatcher.scheduler.advanceUntilIdle()

        val review = coldStartViewModel.uiState.value.dayReview
        assertNotNull(review)
        assertEquals(listOf("Real item"), review.summary.plannedItems.map { it.title })
    }

    private fun dailyPlanItem(
        source: DailyPlanItemSource = DailyPlanItemSource.MyDayNote,
        status: DailyPlanItemStatus = DailyPlanItemStatus.Planned,
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null
    ) = DailyPlanItem(
        id = 42L,
        dateEpochDays = 1,
        title = "Original",
        note = "Old note",
        source = source,
        status = status,
        sortOrder = 0,
        startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes,
        addedAtMillis = 0L
    )

    private companion object {
        val TestDate = LocalDate(2026, 1, 1)
    }
}
