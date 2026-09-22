package com.checkit.domain.usecase

import com.checkit.domain.CarryOverTimePolicy
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayCloseConfirmInput
import com.checkit.domain.LeftoverAction
import com.checkit.domain.Period
import com.checkit.domain.PeriodGoal
import com.checkit.domain.TagItem
import com.checkit.domain.defaultLeftoverAction
import com.checkit.domain.defaultReviewAction
import com.checkit.ui.tasks.FakeCheckItRepository
import com.checkit.ui.tasks.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DayCloseUseCasesTest {
    private val date = LocalDate(2026, 7, 9)
    private val tomorrow = LocalDate(2026, 7, 10)
    private val buildSummary = BuildDayCloseSummaryUseCase(Dispatchers.Unconfined)

    @Test
    fun summaryCountsMinutesAndTopTags() = runTest {
        val work = TagItem(id = "1", name = "Work", color = "#2563EB")
        val plan = DailyPlan(
            date = date,
            items = listOf(
                item(
                    id = "1",
                    title = "Deep work",
                    status = DailyPlanItemStatus.Done,
                    startTimeMinutes = 9 * 60,
                    endTimeMinutes = 10 * 60,
                    tags = listOf(work)
                ),
                item(
                    id = "2",
                    title = "Later",
                    status = DailyPlanItemStatus.Planned,
                    startTimeMinutes = 14 * 60
                ),
                item(
                    id = "3",
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
        assertEquals(listOf("2"), summary.plannedItems.map { it.id })
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
    fun summaryExcludesUnhandledItemsAndListsAlreadyCarried() = runTest {
        val plan = DailyPlan(
            date = date,
            items = listOf(
                item(
                    id = "50",
                    title = "Already carried",
                    status = DailyPlanItemStatus.Planned,
                    handledAtMillis = 1L
                ),
                item(
                    id = "51",
                    title = "Still open",
                    status = DailyPlanItemStatus.Planned
                ),
                item(
                    id = "1",
                    title = "Deep work",
                    status = DailyPlanItemStatus.Done,
                    startTimeMinutes = 9 * 60,
                    endTimeMinutes = 10 * 60
                )
            )
        )

        val summary = buildSummary(date, plan)
        assertEquals(listOf("50"), summary.alreadyCarriedItems.map { it.id })
        assertEquals(listOf("51"), summary.plannedItems.map { it.id })
        assertEquals(listOf("1"), summary.doneItems.map { it.id })
        assertEquals(1, summary.doneCount)
    }

    @Test
    fun carryOverCopiesWithClearedTimesAndSkipsDuplicateTask() = runTest {
        val repository = FakeCheckItRepository()
        val tag = TagItem(id = "5", name = "Code", color = "#059669")
        repository.addTag(com.checkit.data.TagWriteInput(name = tag.name, color = tag.color))
        val seededTag = repository.currentBoard.tags.single()
        
        val carryOver = CarryOverDailyPlanItemsUseCase(repository, Dispatchers.Unconfined)
        val planned = item(
            id = "11",
            taskId = "100",
            title = "Ship PR",
            status = DailyPlanItemStatus.Planned,
            startTimeMinutes = 10 * 60,
            endTimeMinutes = 11 * 60,
            tags = listOf(seededTag)
        )
        repository.setDailyPlans(
            listOf(
                DailyPlan(date = date, items = listOf(planned)),
                DailyPlan(
                    date = tomorrow,
                    items = listOf(
                        planned.copy(
                            id = "99",
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
            itemIds = setOf("11"),
            toDate = tomorrow,
            timePolicy = CarryOverTimePolicy.ClearTimes
        )
        assertEquals(0, skipped.carriedCount)
        assertEquals(1, skipped.skippedCount)

        repository.setDailyPlans(listOf(DailyPlan(date = date, items = listOf(planned))))
        val carried = carryOver(
            items = listOf(planned),
            itemIds = setOf("11"),
            toDate = tomorrow,
            timePolicy = CarryOverTimePolicy.ClearTimes
        )
        assertEquals(1, carried.carriedCount)
        val copy = repository.copiedDailyPlanItems.single()
        assertEquals(tomorrow.toEpochDays().toInt(), copy.dateEpochDays)
        assertEquals(DailyPlanItemStatus.Planned, copy.status)
        assertNull(copy.startTimeMinutes)
        assertNull(copy.endTimeMinutes)
        assertEquals("100", copy.taskId)
        assertEquals(listOf("Code"), copy.tags.map { it.name })
        assertEquals("11", copy.carriedFromItemId)
    }

    @Test
    fun carryOverSkipsWhenSourceAlreadyCarriedOntoDate() = runTest {
        val repository = FakeCheckItRepository()
        val carryOver = CarryOverDailyPlanItemsUseCase(repository, Dispatchers.Unconfined)
        val planned = item(
            id = "21",
            title = "Standalone task",
            status = DailyPlanItemStatus.Planned
        )
        repository.setDailyPlans(listOf(DailyPlan(date = date, items = listOf(planned))))

        val first = carryOver(
            items = listOf(planned),
            itemIds = setOf("21"),
            toDate = tomorrow,
            timePolicy = CarryOverTimePolicy.ClearTimes
        )
        assertEquals(1, first.carriedCount)
        assertEquals(0, first.skippedCount)

        val second = carryOver(
            items = listOf(planned),
            itemIds = setOf("21"),
            toDate = tomorrow,
            timePolicy = CarryOverTimePolicy.ClearTimes
        )
        assertEquals(0, second.carriedCount)
        assertEquals(1, second.skippedCount)
        assertEquals(1, repository.copiedDailyPlanItems.size)
    }

    @Test
    fun completeReviewTwiceDoesNotDuplicateCarryOrGoalRecord() = runTest {
        val repository = FakeCheckItRepository()
        val complete = CompleteDayCloseUseCase(
            repository = repository,
            buildSummary = buildSummary,
            dispatcher = Dispatchers.Unconfined
        )
        val planned = item(id = "1", title = "Standalone", status = DailyPlanItemStatus.Planned)
        val plan = DailyPlan(date = date, items = listOf(planned))
        repository.setDailyPlans(listOf(plan))

        val input = DayCloseConfirmInput(
            date = date,
            leftoverActions = mapOf("1" to LeftoverAction.CarryOver),
            tomorrowGoal = "Ship the review"
        )
        val first = complete(plan, input).getOrThrow()
        assertEquals(1, first.carriedCount)

        val second = complete(plan, input).getOrThrow()
        assertEquals(0, second.carriedCount)

        assertEquals(1, repository.copiedDailyPlanItems.size)
        val tomorrowPlan = assertNotNull(repository.dailyPlanForDate(tomorrow))
        assertEquals(1, tomorrowPlan.items.size)
        assertEquals(1, tomorrowPlan.items.count { it.carriedFromItemId == "1" })

        // The tomorrow goal is stored as tomorrow's goal.
        val record = assertNotNull(repository.periodGoalFor(Period.Day, tomorrow))
        assertEquals("Ship the review", record.goal)
        val todayRecord = repository.periodGoalFor(Period.Day, date)
        assertEquals(null, todayRecord?.goal)
    }

    @Test
    fun completeReviewLeavesNoneItemsUntouched() = runTest {
        val repository = FakeCheckItRepository()
        val complete = CompleteDayCloseUseCase(
            repository = repository,
            buildSummary = buildSummary,
            dispatcher = Dispatchers.Unconfined
        )
        val plannedA = item(id = "1", title = "A", status = DailyPlanItemStatus.Planned)
        val plannedB = item(id = "2", title = "B", status = DailyPlanItemStatus.Planned)
        val plan = DailyPlan(date = date, items = listOf(plannedA, plannedB))
        repository.setDailyPlans(listOf(plan))

        val result = complete(
            plan = plan,
            input = DayCloseConfirmInput(
                date = date,
                leftoverActions = mapOf("1" to LeftoverAction.None, "2" to LeftoverAction.None)
            )
        ).getOrThrow()

        assertEquals(0, result.markedDoneCount)
        assertEquals(0, result.carriedCount)
        assertEquals(0, result.droppedCount)
        assertTrue(repository.statusUpdates.isEmpty())
        assertTrue(repository.copiedDailyPlanItems.isEmpty())
        assertTrue(repository.markedHandledItemIds.isEmpty())
    }

    @Test
    fun completeReviewCanReDecideAlreadyCarriedItem() = runTest {
        val repository = FakeCheckItRepository()
        val complete = CompleteDayCloseUseCase(
            repository = repository,
            buildSummary = buildSummary,
            dispatcher = Dispatchers.Unconfined
        )
        val alreadyCarried = item(
            id = "5",
            title = "Already carried",
            status = DailyPlanItemStatus.Planned,
            handledAtMillis = 1L
        )
        val plan = DailyPlan(date = date, items = listOf(alreadyCarried))
        repository.setDailyPlans(listOf(plan))

        val result = complete(
            plan = plan,
            input = DayCloseConfirmInput(
                date = date,
                leftoverActions = mapOf("5" to LeftoverAction.MarkDone)
            )
        ).getOrThrow()

        assertEquals(1, result.markedDoneCount)
        assertEquals(listOf("5" to DailyPlanItemStatus.Done), repository.statusUpdates)
        assertTrue(repository.markedHandledItemIds.contains("5"))
    }

    @Test
    fun completeReviewClearsBlankWinNoteFromRecord() = runTest {
        val repository = FakeCheckItRepository()
        val complete = CompleteDayCloseUseCase(
            repository = repository,
            buildSummary = buildSummary,
            dispatcher = Dispatchers.Unconfined
        )
        val plan = DailyPlan(date = date, items = emptyList())
        repository.setDailyPlans(listOf(plan))

        val result = complete(
            plan = plan,
            input = DayCloseConfirmInput(
                date = date,
                leftoverActions = emptyMap(),
                winNote = "   "
            )
        ).getOrThrow()

        assertFalse(result.winNoteSaved)
        val record = assertNotNull(repository.periodGoalFor(Period.Day, date))
        assertEquals("", record.review)
    }

    @Test
    fun defaultLeftoverActionIsNoneRequiringExplicitChoice() {
        val linked = item(id = "1", taskId = "100", title = "Linked", status = DailyPlanItemStatus.Planned)
        val standalone = item(id = "2", title = "Standalone", status = DailyPlanItemStatus.Planned)
        assertEquals(LeftoverAction.None, linked.defaultLeftoverAction())
        assertEquals(LeftoverAction.None, standalone.defaultLeftoverAction())
    }

    @Test
    fun defaultReviewActionIsNoneForUnhandledItems() {
        val item = item(id = "1", title = "Pending", status = DailyPlanItemStatus.Planned)
        assertEquals(LeftoverAction.None, item.defaultReviewAction(emptyList()))
    }

    @Test
    fun defaultReviewActionInfersCarryOverFromTomorrowCopy() {
        val today = LocalDate(2026, 7, 10)
        val source = item(
            id = "1",
            title = "Carried",
            status = DailyPlanItemStatus.Planned,
            handledAtMillis = 10L
        )
        val copy = item(
            id = "2",
            title = "Carried (tomorrow)",
            status = DailyPlanItemStatus.Planned,
            carriedFromItemId = "1",
            dateEpochDays = today.toEpochDays().toInt() + 1
        )
        assertEquals(LeftoverAction.CarryOver, source.defaultReviewAction(listOf(DailyPlan(today, listOf(copy)))))
    }

    @Test
    fun defaultReviewActionInfersDropWhenHandledWithoutTomorrowCopy() {
        val today = LocalDate(2026, 7, 10)
        val source = item(
            id = "1",
            title = "Dropped",
            status = DailyPlanItemStatus.Planned,
            handledAtMillis = 10L
        )
        assertEquals(LeftoverAction.Drop, source.defaultReviewAction(listOf(DailyPlan(today, emptyList()))))
    }

    private fun item(
        id: String,
        title: String,
        status: DailyPlanItemStatus,
        taskId: String? = null,
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null,
        tags: List<TagItem> = emptyList(),
        source: DailyPlanItemSource = if (taskId != null) {
            DailyPlanItemSource.ExistingTask
        } else {
            DailyPlanItemSource.MyDayTask
        },
        note: String? = null,
        addedAtMillis: Long = 0L,
        handledAtMillis: Long? = null,
        carriedFromItemId: String? = null,
        dateEpochDays: Int = date.toEpochDays().toInt()
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = dateEpochDays,
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
        completedAtMillis = if (status == DailyPlanItemStatus.Done) 1L else null,
        handledAtMillis = handledAtMillis,
        carriedFromItemId = carriedFromItemId
    )
}
