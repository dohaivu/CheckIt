package com.checkit.domain.usecase

import com.checkit.domain.CarryOverTimePolicy
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayReviewBannerPolicy
import com.checkit.domain.DayReviewConfirmInput
import com.checkit.domain.LeftoverAction
import com.checkit.domain.TaskTag
import com.checkit.ui.tasks.FakeCheckItRepository
import com.checkit.ui.tasks.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DayReviewUseCasesTest {
    private val date = LocalDate(2026, 7, 9)
    private val tomorrow = LocalDate(2026, 7, 10)
    private val buildSummary = BuildDayReviewSummaryUseCase(Dispatchers.Unconfined)

    @Test
    fun summaryCountsMinutesAndTopTags() = runTest {
        val work = TaskTag(id = 1L, name = "Work", color = "#2563EB")
        val plan = DailyPlan(
            date = date,
            items = listOf(
                item(
                    id = 1L,
                    title = "Deep work",
                    status = DailyPlanItemStatus.Done,
                    startTimeMinutes = 9 * 60,
                    endTimeMinutes = 10 * 60,
                    tags = listOf(work)
                ),
                item(
                    id = 2L,
                    title = "Later",
                    status = DailyPlanItemStatus.Planned,
                    startTimeMinutes = 14 * 60
                ),
                item(
                    id = 3L,
                    title = "No times done",
                    status = DailyPlanItemStatus.Done
                )
            )
        )

        val summary = buildSummary(date, plan)
        assertEquals(2, summary.doneCount)
        assertEquals(1, summary.plannedCount)
        assertEquals(60, summary.doneMinutes)
        assertEquals(listOf("Work"), summary.topTags.map { it.name })
        assertEquals(60, summary.topTags.single().totalMinutes)
        assertEquals(listOf(2L), summary.plannedItems.map { it.id })
    }

    @Test
    fun emptyPlanSummaryIsZeroed() = runTest {
        val summary = buildSummary(date, null)
        assertEquals(0, summary.doneCount)
        assertEquals(0, summary.plannedCount)
        assertEquals(0, summary.doneMinutes)
        assertTrue(summary.plannedItems.isEmpty())
        assertTrue(summary.topTags.isEmpty())
    }

    @Test
    fun carryOverCopiesWithClearedTimesAndSkipsDuplicateTask() = runTest {
        val repository = FakeCheckItRepository()
        val carryOver = CarryOverDailyPlanItemsUseCase(repository, Dispatchers.Unconfined)
        val planned = item(
            id = 11L,
            taskId = 100L,
            title = "Ship PR",
            status = DailyPlanItemStatus.Planned,
            startTimeMinutes = 10 * 60,
            endTimeMinutes = 11 * 60,
            tags = listOf(TaskTag(id = 5L, name = "Code", color = "#059669"))
        )
        repository.setDailyPlans(
            listOf(
                DailyPlan(date = date, items = listOf(planned)),
                DailyPlan(
                    date = tomorrow,
                    items = listOf(
                        planned.copy(
                            id = 99L,
                            dateEpochDays = tomorrow.toEpochDays().toInt(),
                            startTimeMinutes = null,
                            endTimeMinutes = null
                        )
                    )
                )
            )
        )

        val skipped = carryOver(
            items = listOf(planned),
            itemIds = setOf(11L),
            toDate = tomorrow,
            timePolicy = CarryOverTimePolicy.ClearTimes
        )
        assertEquals(0, skipped.carriedCount)
        assertEquals(1, skipped.skippedCount)

        repository.setDailyPlans(listOf(DailyPlan(date = date, items = listOf(planned))))
        val carried = carryOver(
            items = listOf(planned),
            itemIds = setOf(11L),
            toDate = tomorrow,
            timePolicy = CarryOverTimePolicy.ClearTimes
        )
        assertEquals(1, carried.carriedCount)
        val copy = repository.copiedDailyPlanItems.single()
        assertEquals(tomorrow.toEpochDays().toInt(), copy.dateEpochDays)
        assertEquals(DailyPlanItemStatus.Planned, copy.status)
        assertNull(copy.startTimeMinutes)
        assertNull(copy.endTimeMinutes)
        assertEquals(100L, copy.taskId)
        assertEquals(listOf("Code"), copy.tags.map { it.name })
    }

    @Test
    fun completeReviewAppliesActionsWinNoteAndSettings() = runTest {
        val repository = FakeCheckItRepository()
        val settings = FakeSettingsRepository()
        val complete = CompleteDayReviewUseCase(
            repository = repository,
            settingsRepository = settings,
            carryOverDailyPlanItems = CarryOverDailyPlanItemsUseCase(repository, Dispatchers.Unconfined),
            upsertWinNote = UpsertDayReviewWinNoteUseCase(repository, Dispatchers.Unconfined),
            buildSummary = buildSummary,
            dispatcher = Dispatchers.Unconfined
        )
        val plannedA = item(id = 1L, title = "A", status = DailyPlanItemStatus.Planned)
        val plannedB = item(id = 2L, title = "B", status = DailyPlanItemStatus.Planned)
        val plannedC = item(id = 3L, title = "C", status = DailyPlanItemStatus.Planned)
        val done = item(
            id = 4L,
            title = "Done",
            status = DailyPlanItemStatus.Done,
            startTimeMinutes = 8 * 60,
            endTimeMinutes = 9 * 60
        )
        val plan = DailyPlan(date = date, items = listOf(plannedA, plannedB, plannedC, done))
        repository.setDailyPlans(listOf(plan))

        val result = complete(
            plan = plan,
            input = DayReviewConfirmInput(
                date = date,
                leftoverActions = mapOf(
                    1L to LeftoverAction.MarkDone,
                    2L to LeftoverAction.CarryOver,
                    3L to LeftoverAction.Drop
                ),
                winNote = " Shipped review  "
            )
        ).getOrThrow()

        assertEquals(1, result.markedDoneCount)
        assertEquals(1, result.carriedCount)
        assertEquals(1, result.droppedCount)
        assertTrue(result.winNoteSaved)
        assertEquals(listOf(1L to DailyPlanItemStatus.Done), repository.statusUpdates)
        assertEquals(1, repository.copiedDailyPlanItems.size)
        assertEquals("Win", repository.addedManualDailyPlanItems.single().title)
        assertEquals("Shipped review", repository.addedManualDailyPlanItems.single().note)
        assertEquals(date.toEpochDays().toInt(), settings.currentSettings().lastDayReviewEpochDay)
    }

    @Test
    fun summaryLoadsExistingWinNoteAndExcludesItFromCounts() = runTest {
        val win = item(
            id = 50L,
            title = "Win",
            status = DailyPlanItemStatus.Done,
            source = DailyPlanItemSource.MyDayNote,
            note = "Closed the review loop"
        )
        val done = item(
            id = 1L,
            title = "Deep work",
            status = DailyPlanItemStatus.Done,
            startTimeMinutes = 9 * 60,
            endTimeMinutes = 10 * 60
        )
        val plan = DailyPlan(date = date, items = listOf(win, done))

        val summary = buildSummary(date, plan)
        assertEquals(50L, summary.winNoteItemId)
        assertEquals("Closed the review loop", summary.winNote)
        assertEquals(1, summary.doneCount)
        assertEquals(listOf(1L), summary.doneItems.map { it.id })
    }

    @Test
    fun completeReviewUpdatesExistingWinNote() = runTest {
        val repository = FakeCheckItRepository()
        val settings = FakeSettingsRepository()
        val complete = CompleteDayReviewUseCase(
            repository = repository,
            settingsRepository = settings,
            carryOverDailyPlanItems = CarryOverDailyPlanItemsUseCase(repository, Dispatchers.Unconfined),
            upsertWinNote = UpsertDayReviewWinNoteUseCase(repository, Dispatchers.Unconfined),
            buildSummary = buildSummary,
            dispatcher = Dispatchers.Unconfined
        )
        val win = item(
            id = 50L,
            title = "Win",
            status = DailyPlanItemStatus.Done,
            source = DailyPlanItemSource.MyDayNote,
            note = "First draft"
        )
        val plan = DailyPlan(date = date, items = listOf(win))
        repository.setDailyPlans(listOf(plan))

        val result = complete(
            plan = plan,
            input = DayReviewConfirmInput(
                date = date,
                leftoverActions = emptyMap(),
                winNote = "Updated win",
                winNoteItemId = 50L
            )
        ).getOrThrow()

        assertTrue(result.winNoteSaved)
        assertTrue(repository.addedManualDailyPlanItems.isEmpty())
        val update = repository.updatedDailyPlanItems.single()
        assertEquals(50L, update.first)
        assertEquals("Updated win", update.second.note)
        assertEquals("Win", update.second.title)
    }

    @Test
    fun completeReviewDeletesWinNoteWhenCleared() = runTest {
        val repository = FakeCheckItRepository()
        val settings = FakeSettingsRepository()
        val complete = CompleteDayReviewUseCase(
            repository = repository,
            settingsRepository = settings,
            carryOverDailyPlanItems = CarryOverDailyPlanItemsUseCase(repository, Dispatchers.Unconfined),
            upsertWinNote = UpsertDayReviewWinNoteUseCase(repository, Dispatchers.Unconfined),
            buildSummary = buildSummary,
            dispatcher = Dispatchers.Unconfined
        )
        val win = item(
            id = 50L,
            title = "Win",
            status = DailyPlanItemStatus.Done,
            source = DailyPlanItemSource.MyDayNote,
            note = "Temporary"
        )
        val plan = DailyPlan(date = date, items = listOf(win))
        repository.setDailyPlans(listOf(plan))

        val result = complete(
            plan = plan,
            input = DayReviewConfirmInput(
                date = date,
                leftoverActions = emptyMap(),
                winNote = "   ",
                winNoteItemId = 50L
            )
        ).getOrThrow()

        assertTrue(result.winNoteSaved)
        assertEquals(listOf(50L), repository.deletedDailyPlanItemIds)
    }

    @Test
    fun bannerPolicyRespectsTimeSettingsAndCompletion() {
        assertTrue(
            DayReviewBannerPolicy.shouldShow(
                hasPlanItems = true,
                reviewReminderEnabled = true,
                reviewReminderTimeMinutes = 21 * 60,
                lastDayReviewEpochDay = null,
                todayEpochDay = 10,
                nowMinutes = 21 * 60
            )
        )
        assertFalse(
            DayReviewBannerPolicy.shouldShow(
                hasPlanItems = true,
                reviewReminderEnabled = true,
                reviewReminderTimeMinutes = 21 * 60,
                lastDayReviewEpochDay = null,
                todayEpochDay = 10,
                nowMinutes = 20 * 60
            )
        )
        assertFalse(
            DayReviewBannerPolicy.shouldShow(
                hasPlanItems = true,
                reviewReminderEnabled = true,
                reviewReminderTimeMinutes = 21 * 60,
                lastDayReviewEpochDay = 10,
                todayEpochDay = 10,
                nowMinutes = 22 * 60
            )
        )
        assertFalse(
            DayReviewBannerPolicy.shouldShow(
                hasPlanItems = false,
                reviewReminderEnabled = true,
                reviewReminderTimeMinutes = 21 * 60,
                lastDayReviewEpochDay = null,
                todayEpochDay = 10,
                nowMinutes = 22 * 60
            )
        )
        assertFalse(
            DayReviewBannerPolicy.shouldShow(
                hasPlanItems = true,
                reviewReminderEnabled = false,
                reviewReminderTimeMinutes = 21 * 60,
                lastDayReviewEpochDay = null,
                todayEpochDay = 10,
                nowMinutes = 22 * 60
            )
        )
    }

    private fun item(
        id: Long,
        title: String,
        status: DailyPlanItemStatus,
        taskId: Long? = null,
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null,
        tags: List<TaskTag> = emptyList(),
        source: DailyPlanItemSource = if (taskId != null) {
            DailyPlanItemSource.ExistingTask
        } else {
            DailyPlanItemSource.MyDayTask
        },
        note: String? = null,
        addedAtMillis: Long = 0L
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = date.toEpochDays().toInt(),
        taskId = taskId,
        title = title,
        note = note,
        source = source,
        status = status,
        tags = tags,
        sortOrder = id.toInt(),
        startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes,
        addedAtMillis = addedAtMillis,
        completedAtMillis = if (status == DailyPlanItemStatus.Done) 1L else null
    )
}
