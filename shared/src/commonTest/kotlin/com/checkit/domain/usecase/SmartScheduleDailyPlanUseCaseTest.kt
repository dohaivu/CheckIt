package com.checkit.domain.usecase

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TaskTag
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SmartScheduleDailyPlanUseCaseTest {
    private val workTag = TaskTag(id = 1, name = "Work", color = "#2563EB")
    private val lifeTag = TaskTag(id = 2, name = "Life", color = "#7C3AED")
    private val today = LocalDate(2026, 6, 10)

    @Test
    fun schedulesFromModeOfCompletedHistory() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(
                plan(day(9), item(2, day(9), workTag, Done, 540, 600)),
                plan(day(8), item(3, day(8), workTag, Done, 540, 600)),
                plan(day(7), item(4, day(7), workTag, Done, 600, 660))
            ),
            todayItems = listOf(item(1, today, workTag, Planned))
        )
        val useCase = useCase(repository)

        val result = useCase()

        assertEquals(SmartScheduleResult(scheduledCount = 1, candidateCount = 1), result.getOrThrow())
        assertEquals(1, repository.updatedDailyPlanItemTimes.size)
        assertEquals(Triple(1L, 540, 600), repository.updatedDailyPlanItemTimes.single())
    }

    @Test
    fun defaultFallbackUsesNowAsStart() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = emptyList(),
            todayItems = listOf(item(1, today, workTag, Planned))
        )
        val useCase = useCase(repository, nowMinutes = 100)

        val result = useCase()

        assertEquals(1, result.getOrThrow().scheduledCount)
        assertEquals(Triple(1L, 100, 145), repository.updatedDailyPlanItemTimes.single())
    }

    @Test
    fun clampsPreferredStartToNow() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(
                plan(day(9), item(2, day(9), workTag, Done, 540, 600)),
                plan(day(8), item(3, day(8), workTag, Done, 540, 600))
            ),
            todayItems = listOf(item(1, today, workTag, Planned))
        )
        val useCase = useCase(repository, nowMinutes = 600)

        val result = useCase()

        assertEquals(1, result.getOrThrow().scheduledCount)
        assertEquals(Triple(1L, 600, 660), repository.updatedDailyPlanItemTimes.single())
    }

    @Test
    fun onlyUsesMostRecentThreeDaysPerTag() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(
                plan(day(4), item(2, day(4), workTag, Done, 540, 600)),
                plan(day(5), item(3, day(5), workTag, Done, 540, 600)),
                plan(day(6), item(4, day(6), workTag, Done, 540, 600)),
                plan(day(7), item(5, day(7), workTag, Done, 1200, 1260), item(6, day(7), workTag, Done, 1200, 1260)),
                plan(day(8), item(7, day(8), workTag, Done, 1200, 1260), item(8, day(8), workTag, Done, 1200, 1260)),
                plan(day(9), item(9, day(9), workTag, Done, 1200, 1260), item(10, day(9), workTag, Done, 1200, 1260))
            ),
            todayItems = listOf(item(1, today, workTag, Planned))
        )
        val useCase = useCase(repository)

        val result = useCase()

        assertEquals(1, result.getOrThrow().scheduledCount)
        assertEquals(Triple(1L, 540, 600), repository.updatedDailyPlanItemTimes.single())
    }

    @Test
    fun usesFirstTagOnly() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(
                plan(day(9), item(2, day(9), workTag, Done, 540, 600)),
                plan(day(8), item(3, day(8), workTag, Done, 540, 600)),
                plan(day(9), item(4, day(9), lifeTag, Done, 840, 900)),
                plan(day(8), item(5, day(8), lifeTag, Done, 840, 900))
            ),
            todayItems = listOf(item(1, today, listOf(workTag, lifeTag), Planned))
        )
        val useCase = useCase(repository)

        val result = useCase()

        assertEquals(1, result.getOrThrow().scheduledCount)
        assertEquals(Triple(1L, 540, 600), repository.updatedDailyPlanItemTimes.single())
    }

    @Test
    fun skipsUntaggedItems() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(
                plan(day(9), item(2, day(9), workTag, Done, 540, 600))
            ),
            todayItems = listOf(
                item(1, today, workTag, Planned),
                item(5, today, emptyList(), Planned)
            )
        )
        val useCase = useCase(repository)

        val result = useCase()

        assertEquals(SmartScheduleResult(scheduledCount = 1, candidateCount = 1), result.getOrThrow())
        assertEquals(1, repository.updatedDailyPlanItemTimes.size)
    }

    @Test
    fun schedulesWithDefaultsWhenNoHistory() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = emptyList(),
            todayItems = listOf(item(1, today, workTag, Planned))
        )
        val useCase = useCase(repository)

        val result = useCase()

        assertEquals(SmartScheduleResult(scheduledCount = 1, candidateCount = 1), result.getOrThrow())
        assertEquals(listOf<Triple<Long, Int?, Int?>>(Triple(1L, 0, 45)), repository.updatedDailyPlanItemTimes)
    }

    @Test
    fun fallsBackToDefaultDurationWhenHistoryHasNoEnd() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(
                plan(day(9), item(2, day(9), workTag, Done, 540)),
                plan(day(8), item(3, day(8), workTag, Done, 540))
            ),
            todayItems = listOf(item(1, today, workTag, Planned))
        )
        val useCase = useCase(repository)

        val result = useCase()

        assertEquals(1, result.getOrThrow().scheduledCount)
        assertEquals(Triple(1L, 540, 585), repository.updatedDailyPlanItemTimes.single())
    }

    @Test
    fun storesNullEndForNotes() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(
                plan(day(9), item(2, day(9), workTag, Done, 540, 600)),
                plan(day(8), item(3, day(8), workTag, Done, 540, 600))
            ),
            todayItems = listOf(
                item(1, today, workTag, Planned, source = DailyPlanItemSource.MyDayNote)
            )
        )
        val useCase = useCase(repository)

        val result = useCase()

        assertEquals(1, result.getOrThrow().scheduledCount)
        val (id, start, end) = repository.updatedDailyPlanItemTimes.single()
        assertEquals(1L, id)
        assertEquals(540, start)
        assertNull(end)
    }

    @Test
    fun accumulatesScheduledItemsToAvoidOverlap() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(
                plan(day(9), item(3, day(9), workTag, Done, 540, 600)),
                plan(day(8), item(4, day(8), workTag, Done, 540, 600))
            ),
            todayItems = listOf(
                item(1, today, workTag, Planned),
                item(2, today, workTag, Planned)
            )
        )
        val useCase = useCase(repository)

        val result = useCase()

        assertEquals(SmartScheduleResult(scheduledCount = 2, candidateCount = 2), result.getOrThrow())
        assertEquals(
            listOf<Triple<Long, Int?, Int?>>(Triple(1L, 540, 600), Triple(2L, 600, 660)),
            repository.updatedDailyPlanItemTimes
        )
    }

    @Test
    fun returnsNothingWhenNoCandidates() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(plan(day(9), item(2, day(9), workTag, Done, 540, 600))),
            todayItems = listOf(item(1, today, workTag, Done))
        )
        val useCase = useCase(repository)

        val result = useCase()

        assertEquals(SmartScheduleResult(scheduledCount = 0, candidateCount = 0), result.getOrThrow())
        assertEquals(0, repository.updatedDailyPlanItemTimes.size)
    }

    @Test
    fun keepsExistingTimesForUntaggedAndDoneItems() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(
                plan(day(9), item(4, day(9), workTag, Done, 540, 600)),
                plan(day(8), item(5, day(8), workTag, Done, 540, 600))
            ),
            todayItems = listOf(
                item(1, today, workTag, Planned),
                item(2, today, emptyList(), Planned, start = 300, end = 360),
                item(3, today, workTag, Done, start = 540, end = 600)
            )
        )
        val useCase = useCase(repository)

        val result = useCase()

        assertEquals(1, result.getOrThrow().scheduledCount)
        assertEquals(Triple(1L, 600, 660), repository.updatedDailyPlanItemTimes.single())
    }

    @Test
    fun protectsCandidateWithNarrowerPreferenceInsteadOfInputOrder() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = listOf(
                plan(day(9), item(3, day(9), workTag, Done, 540, 600)),
                plan(day(8), item(4, day(8), workTag, Done, 540, 600)),
                plan(day(9), item(5, day(9), lifeTag, Done, 540, 585)),
                plan(day(8), item(6, day(8), lifeTag, Done, 540, 585))
            ),
            todayItems = listOf(
                item(2, today, lifeTag, Planned),
                item(1, today, workTag, Planned),
                item(7, today, emptyList(), Planned, start = 600, end = 660)
            )
        )

        val result = useCase(repository)()

        assertEquals(2, result.getOrThrow().scheduledCount)
        assertEquals(
            setOf(
                Triple(1L, 540, 600),
                Triple(2L, 660, 705)
            ),
            repository.updatedDailyPlanItemTimes.toSet()
        )
    }

    @Test
    fun doesNotScheduleBeforeNowWhenOnlyPastSpaceIsAvailable() = runTest {
        val repository = repositoryWithHistory(
            pastPlans = emptyList(),
            todayItems = listOf(
                item(1, today, workTag, Planned),
                item(2, today, emptyList(), Planned, start = 600, end = 1440)
            )
        )

        val result = useCase(repository, nowMinutes = 600)()

        assertEquals(SmartScheduleResult(scheduledCount = 0, candidateCount = 1), result.getOrThrow())
        assertEquals(emptyList(), repository.updatedDailyPlanItemTimes)
    }

    @Test
    fun choosesAnActualHistoricalRangeInsteadOfCombiningIndependentModes() {
        assertEquals(
            540 to 60,
            bestSmartScheduleRange(
                samples = listOf(
                    SmartTimeSample(540, 600),
                    SmartTimeSample(540, 660),
                    SmartTimeSample(600, 660)
                ),
                nowMinutes = 0
            )
        )
    }

    private fun useCase(
        repository: FakeCheckItRepository,
        nowMinutes: Int = 0
    ) = SmartScheduleDailyPlanUseCase(
        repository = repository,
        todayDate = { today },
        nowMinutes = { nowMinutes }
    )

    private fun repositoryWithHistory(
        pastPlans: List<DailyPlan>,
        todayItems: List<DailyPlanItem>
    ): FakeCheckItRepository {
        val plans = pastPlans + DailyPlan(date = today, items = todayItems)
        val repository = FakeCheckItRepository()
        repository.setDailyPlans(plans.sortedByDescending { it.date })
        return repository
    }

    private fun day(offset: Int): LocalDate = today.minus(offset, kotlinx.datetime.DateTimeUnit.DAY)

    private fun plan(date: LocalDate, vararg items: DailyPlanItem): DailyPlan = DailyPlan(date = date, items = items.toList())

    private fun item(
        id: Long,
        date: LocalDate,
        tag: TaskTag,
        status: DailyPlanItemStatus,
        start: Int? = null,
        end: Int? = null,
        source: DailyPlanItemSource = DailyPlanItemSource.MyDayTask
    ): DailyPlanItem = item(id, date, listOf(tag), status, start, end, source)

    private fun item(
        id: Long,
        date: LocalDate,
        tags: List<TaskTag>,
        status: DailyPlanItemStatus,
        start: Int? = null,
        end: Int? = null,
        source: DailyPlanItemSource = DailyPlanItemSource.MyDayTask
    ): DailyPlanItem = DailyPlanItem(
        id = id,
        dateEpochDays = date.toEpochDays().toInt(),
        taskId = null,
        title = "Item $id",
        note = null,
        source = source,
        status = status,
        tags = tags,
        isHabit = false,
        sortOrder = 0,
        startTimeMinutes = start,
        endTimeMinutes = end,
        addedAtMillis = 0L,
        completedAtMillis = if (status == DailyPlanItemStatus.Done) 1L else null
    )

    private companion object {
        val Done = DailyPlanItemStatus.Done
        val Planned = DailyPlanItemStatus.Planned
    }
}
