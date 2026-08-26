package com.checkit.ui.myday

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.PeriodGoal
import com.checkit.domain.JournalEntry
import com.checkit.domain.LeftoverAction
import com.checkit.domain.Period
import com.checkit.domain.TagItem
import com.checkit.domain.usecase.AddDailyPlanItemUseCase
import com.checkit.domain.usecase.AddJournalEntryUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.BuildDayCloseSummaryUseCase
import com.checkit.domain.usecase.CarryOverDailyPlanItemsUseCase
import com.checkit.domain.usecase.CompleteDayCloseUseCase
import com.checkit.domain.usecase.ObserveNotesForDateUseCase
import com.checkit.domain.usecase.ObservePeriodGoalsUseCase
import com.checkit.domain.usecase.ObserveTagsUseCase
import com.checkit.domain.usecase.ObserveWorkingTasksUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.DeleteJournalEntryUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveJournalEntriesUseCase
import com.checkit.domain.usecase.GetTaskUseCase
import com.checkit.domain.SprintManager
import com.checkit.domain.usecase.UpdateDailyPlanItemStatusUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpsertDailyPlanItemUseCase
import com.checkit.domain.usecase.UpdateJournalEntryUseCase
import com.checkit.domain.usecase.AddSuggestedTaskToMyDayUseCase
import com.checkit.domain.usecase.SprintTransitionUseCase
import com.checkit.domain.usecase.SaveSprintAsWinUseCase
import com.checkit.domain.usecase.SmartScheduleDailyPlanUseCase
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
        val buildSummary = BuildDayCloseSummaryUseCase(dispatcher)
        val carryOver = CarryOverDailyPlanItemsUseCase(repository, dispatcher)
        val observeDailyPlans = ObserveDailyPlansUseCase(repository)
        val observeJournalEntries = ObserveJournalEntriesUseCase(repository)
        val observePeriodGoals = ObservePeriodGoalsUseCase(repository)
        val addTaskToDailyPlan = AddTaskToDailyPlanUseCase(repository)
        val updateDailyPlanItemTime = UpdateDailyPlanItemTimeUseCase(repository)

        return MyDayViewModel(
            observeDailyPlans = observeDailyPlans,
            observeJournalEntries = observeJournalEntries,
            observePeriodGoals = observePeriodGoals,
            observeTags = ObserveTagsUseCase(repository),
            observeWorkingTasks = ObserveWorkingTasksUseCase(repository),
            observeNotesForDate = ObserveNotesForDateUseCase(repository),
            addJournalEntry = AddJournalEntryUseCase(repository),
            updateJournalEntry = UpdateJournalEntryUseCase(repository),
            deleteJournalEntry = DeleteJournalEntryUseCase(repository),
            deleteDailyPlanItemUseCase = DeleteDailyPlanItemUseCase(repository),
            settingsRepository = settingsRepository,
            buildDayCloseSummary = buildSummary,
            completeDayClose = CompleteDayCloseUseCase(
                repository = repository,
                settingsRepository = settingsRepository,
                buildSummary = buildSummary,
                dispatcher = dispatcher
            ),
            carryOverDailyPlanItems = carryOver,
            upsertDailyPlanItem = UpsertDailyPlanItemUseCase(repository),
            addSuggestedTaskToMyDay = AddSuggestedTaskToMyDayUseCase(
                repository = repository,
                addTaskToDailyPlan = addTaskToDailyPlan,
                updateDailyPlanItemTime = updateDailyPlanItemTime
            ),
            updateDailyPlanItemTime = updateDailyPlanItemTime,
            smartSchedule = SmartScheduleDailyPlanUseCase(repository),
            sprintManager = SprintManager(NoOpSprintNotificationScheduler()),
            sprintTransition = SprintTransitionUseCase(
                sprintManager = SprintManager(NoOpSprintNotificationScheduler()), // Separate instance for transition if needed or reuse
                saveSprintAsWin = SaveSprintAsWinUseCase(
                    repository = repository,
                    addTaskToDailyPlan = addTaskToDailyPlan,
                    addDailyPlanItem = AddDailyPlanItemUseCase(repository),
                    updateDailyPlanItemTime = updateDailyPlanItemTime,
                    updateDailyPlanItemStatus = UpdateDailyPlanItemStatusUseCase(repository)
                ),
                getTask = GetTaskUseCase(repository)
            )
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openDayClosePrefillsExistingWinNoteFromHistory() = runTest(dispatcher) {
        val today = today()
        repository.setDayGoals(
            listOf(
                PeriodGoal(
                    period = Period.Day,
                    startEpochDays = today.toEpochDays().toInt(),
                    endEpochDays = today.toEpochDays().toInt() + 1,
                    review = "Shipped the review loop",
                    completedAtMillis = 1L
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDayClose()
        dispatcher.scheduler.advanceUntilIdle()

        val review = viewModel.uiState.value.dayClose
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
    fun reOpenDayCloseExcludesAlreadyHandledItems() = runTest(dispatcher) {
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

        viewModel.openDayClose()
        dispatcher.scheduler.advanceUntilIdle()
        val first = viewModel.uiState.value.dayClose
        assertNotNull(first)
        assertEquals(LeftoverAction.None, first.actionFor(first.summary.plannedItems.single()))

        viewModel.setLeftoverAction(7L, LeftoverAction.Drop)
        viewModel.confirmDayClose()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDayClose()
        dispatcher.scheduler.advanceUntilIdle()
        val reopened = viewModel.uiState.value.dayClose
        assertNotNull(reopened)
        assertTrue(reopened.summary.plannedItems.none { it.id == 7L })
        assertEquals(listOf(7L), reopened.summary.alreadyCarriedItems.map { it.id })
        assertEquals(LeftoverAction.Drop, reopened.actionFor(reopened.summary.alreadyCarriedItems.single()))
    }

    @Test
    fun openDayCloseBeforeDataLoadUsesLatestPlanNotEmptyState() = runTest(dispatcher) {
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
        coldStartViewModel.openDayClose()
        dispatcher.scheduler.advanceUntilIdle()

        val review = coldStartViewModel.uiState.value.dayClose
        assertNotNull(review)
        assertEquals(listOf("Real item"), review.summary.plannedItems.map { it.title })
    }

    @Test
    fun openJournalEditorAddModeSavesNewEntry() = runTest(dispatcher) {
        val tag = TagItem(id = 1L, name = "Work", color = "#FF0000")
        repository.addTag(com.checkit.data.TagWriteInput(name = "Work", color = "#FF0000"))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openNewJournalEntry()
        val editor = viewModel.uiState.value.journalEditor
        assertNotNull(editor)
        assertEquals(false, editor.isEditMode)
        assertEquals(today(), editor.date)

        viewModel.updateJournalEditorLabel("Biking")
        viewModel.updateJournalEditorContent("Covered 20 km")
        viewModel.toggleJournalEditorMood("🔥")
        viewModel.toggleJournalEditorTag(tag.id)
        viewModel.saveJournalEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val input = repository.addedJournalEntries.single()
        assertEquals("Biking", input.label)
        assertEquals("Covered 20 km", input.content)
        assertEquals(listOf("🔥"), input.moods)
        assertEquals(today(), input.date)

        assertEquals(null, viewModel.uiState.value.journalEditor)
        assertEquals(1, viewModel.uiState.value.journalEntries.size)
        assertEquals("Biking", viewModel.uiState.value.journalEntries.single().label)
    }

    @Test
    fun saveBlankJournalEditorDoesNotPersist() = runTest(dispatcher) {
        viewModel.openNewJournalEntry()
        viewModel.saveJournalEditor()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repository.addedJournalEntries.isEmpty())
    }

    @Test
    fun openAndDismissJournalListSheet() = runTest(dispatcher) {
        assertEquals(false, viewModel.uiState.value.showJournalList)

        viewModel.openJournalList()
        assertEquals(true, viewModel.uiState.value.showJournalList)

        viewModel.dismissJournalList()
        assertEquals(false, viewModel.uiState.value.showJournalList)
    }

    @Test
    fun journalEntriesIncludeOnlyToday() = runTest(dispatcher) {
        val today = today()
        repository.setJournalEntries(
            listOf(
                JournalEntry(
                    id = 1L,
                    dateEpochDays = today.toEpochDays().toInt(),
                    label = "Biking",
                    content = "Ride",
                    createdTimeMinutes = 1
                ),
                JournalEntry(
                    id = 2L,
                    dateEpochDays = today.toEpochDays().toInt(),
                    label = "Cafe",
                    content = "Coffee",
                    createdTimeMinutes = 2
                ),
                JournalEntry(
                    id = 3L,
                    dateEpochDays = today.toEpochDays().toInt() - 1,
                    label = "Old",
                    content = "Yesterday",
                    createdTimeMinutes = 3
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.journalEntries.map { it.id })
    }

    @Test
    fun openJournalEditorPrefillsAndSavePersistsUpdates() = runTest(dispatcher) {
        val today = today()
        repository.setJournalEntries(
            listOf(
                JournalEntry(
                    id = 5L,
                    dateEpochDays = today.toEpochDays().toInt(),
                    label = "Biking",
                    content = "Ride",
                    moods = listOf("😀"),
                    createdTimeMinutes = 1
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openJournalEditor(repository.currentJournalEntry(5L)!!)
        val editor = viewModel.uiState.value.journalEditor
        assertNotNull(editor)
        assertEquals("Biking", editor.label)
        assertEquals("Ride", editor.content)

        viewModel.updateJournalEditorContent("Ride + sprint")
        viewModel.saveJournalEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val (entryId, input) = repository.updatedJournalEntries.single()
        assertEquals(5L, entryId)
        assertEquals("Ride + sprint", input.content)
        assertEquals(null, viewModel.uiState.value.journalEditor)
    }

    @Test
    fun deleteJournalEntryRemovesEntryAndClosesEditor() = runTest(dispatcher) {
        val today = today()
        repository.setJournalEntries(
            listOf(
                JournalEntry(
                    id = 7L,
                    dateEpochDays = today.toEpochDays().toInt(),
                    content = "Doomed",
                    createdTimeMinutes = 1
                )
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openJournalEditor(repository.currentJournalEntry(7L)!!)
        viewModel.deleteJournalEntry(7L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(7L), repository.deletedJournalEntryIds)
        assertEquals(null, viewModel.uiState.value.journalEditor)
        assertTrue(viewModel.uiState.value.journalEntries.isEmpty())
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
