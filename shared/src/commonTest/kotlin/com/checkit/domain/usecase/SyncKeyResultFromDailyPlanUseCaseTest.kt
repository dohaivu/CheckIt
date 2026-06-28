package com.checkit.domain.usecase

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.KeyResult
import com.checkit.domain.Objective
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncKeyResultFromDailyPlanUseCaseTest {

    private fun objective(id: Long) = Objective(
        id = id, name = "Objective $id", color = "#2563EB", icon = "Inbox", sortOrder = 0
    )

    private fun keyResult(
        id: Long,
        objectiveId: Long,
        currentValue: Double = 0.0,
        unit: String = "Number",
        targetValue: Double = 10.0,
    ) = KeyResult(
        id = id, objectiveId = objectiveId, title = "KR $id",
        targetValue = targetValue, currentValue = currentValue,
        unit = unit, sortOrder = 0
    )

    private fun task(
        id: Long,
        objective: Objective,
        keyResult: KeyResult? = null,
    ) = TaskItem(
        id = id, objective = objective, keyResult = keyResult, name = "Task $id",
        sortOrder = 0, createdAtMillis = 0L, updatedAtMillis = 0L
    )

    private fun dailyPlanItem(
        id: Long,
        taskId: Long?,
        status: DailyPlanItemStatus = DailyPlanItemStatus.Planned,
        startTime: Int? = null,
        endTime: Int? = null,
        dateEpochDays: Int = 1
    ) = DailyPlanItem(
        id = id, dateEpochDays = dateEpochDays, taskId = taskId, title = "Item $id",
        source = DailyPlanItemSource.ExistingTask, status = status,
        sortOrder = 0, startTimeMinutes = startTime,
        endTimeMinutes = endTime, addedAtMillis = 0L
    )

    @Test
    fun `hours unit adds duration when marking done`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, unit = "Hours")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val repository = FakeCheckItRepository(initialBoard = TaskBoard(tasks = listOf(t)))
        val item = dailyPlanItem(id = 100L, taskId = 10L, startTime = 9 * 60, endTime = 11 * 60)
        repository.addedDailyPlanItems.add(item)
        val useCase = SyncKeyResultFromDailyPlanUseCase(repository)

        useCase(itemId = 100L, proposedStatus = DailyPlanItemStatus.Done)

        assertEquals(1, repository.adjustedKeyResults.size)
        assertEquals(2.0, repository.adjustedKeyResults[0].second)
    }

    @Test
    fun `hours unit adjusts duration when time changes for done item`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, unit = "Hours")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val repository = FakeCheckItRepository(initialBoard = TaskBoard(tasks = listOf(t)))
        // Already Done with 2 hours
        val item = dailyPlanItem(id = 100L, taskId = 10L, status = DailyPlanItemStatus.Done, startTime = 9 * 60, endTime = 11 * 60)
        repository.addedDailyPlanItems.add(item)
        val useCase = SyncKeyResultFromDailyPlanUseCase(repository)

        // Change to 3 hours
        useCase(itemId = 100L, proposedStartTime = 9 * 60, proposedEndTime = 12 * 60)

        assertEquals(1, repository.adjustedKeyResults.size)
        assertEquals(1.0, repository.adjustedKeyResults[0].second) // +1 hour diff
    }

    @Test
    fun `days unit only increments once per day`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, unit = "Days")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val repository = FakeCheckItRepository(initialBoard = TaskBoard(tasks = listOf(t)))
        
        val item1 = dailyPlanItem(id = 100L, taskId = 10L, status = DailyPlanItemStatus.Done, dateEpochDays = 1)
        val item2 = dailyPlanItem(id = 101L, taskId = 10L, status = DailyPlanItemStatus.Planned, dateEpochDays = 1)
        repository.addedDailyPlanItems.addAll(listOf(item1, item2))
        
        val useCase = SyncKeyResultFromDailyPlanUseCase(repository)

        // item1 is already done, so marking item2 done should NOT increment
        useCase(itemId = 101L, proposedStatus = DailyPlanItemStatus.Done)

        assertTrue(repository.adjustedKeyResults.isEmpty())
    }

    @Test
    fun `number unit increments on every completion`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, unit = "Number")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val repository = FakeCheckItRepository(initialBoard = TaskBoard(tasks = listOf(t)))
        val item = dailyPlanItem(id = 100L, taskId = 10L)
        repository.addedDailyPlanItems.add(item)
        val useCase = SyncKeyResultFromDailyPlanUseCase(repository)

        useCase(itemId = 100L, proposedStatus = DailyPlanItemStatus.Done)

        assertEquals(1, repository.adjustedKeyResults.size)
        assertEquals(1.0, repository.adjustedKeyResults[0].second)
    }
}
