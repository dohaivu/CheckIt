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

class AutoUpdateKeyResultCurrentValueUseCaseTest {

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
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null,
    ) = TaskItem(
        id = id, objective = objective, keyResult = keyResult, name = "Task $id",
        startTimeMinutes = startTimeMinutes, endTimeMinutes = endTimeMinutes,
        sortOrder = 0, createdAtMillis = 0L, updatedAtMillis = 0L
    )

    private fun dailyPlanItem(
        id: Long,
        taskId: Long?,
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null,
        status: DailyPlanItemStatus = DailyPlanItemStatus.Planned,
    ) = DailyPlanItem(
        id = id, dailyPlanId = 1L, taskId = taskId, title = "Item $id",
        source = DailyPlanItemSource.ExistingTask, status = status,
        sortOrder = 0, startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes, addedAtMillis = 0L
    )

    @Test
    fun `number unit skips update`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, currentValue = 3.0, unit = "Number")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val board = TaskBoard(objectives = listOf(obj), tasks = listOf(t), keyResults = listOf(kr))
        val repository = FakeCheckItRepository(initialBoard = board)
        val useCase = AutoUpdateKeyResultCurrentValueUseCase(repository)

        useCase(dailyPlanItem(id = 100L, taskId = 10L), DailyPlanItemStatus.Planned, DailyPlanItemStatus.Done, board)

        assertTrue(repository.updatedKeyResults.isEmpty())
    }

    @Test
    fun `binary unit skips update`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, currentValue = 0.0, unit = "Binary")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val board = TaskBoard(objectives = listOf(obj), tasks = listOf(t), keyResults = listOf(kr))
        val repository = FakeCheckItRepository(initialBoard = board)
        val useCase = AutoUpdateKeyResultCurrentValueUseCase(repository)

        useCase(dailyPlanItem(id = 100L, taskId = 10L), DailyPlanItemStatus.Planned, DailyPlanItemStatus.Done, board)

        assertTrue(repository.updatedKeyResults.isEmpty())
    }

    @Test
    fun `hours unit adds duration to currentValue when marking done`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, currentValue = 2.0, unit = "Hours")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val board = TaskBoard(objectives = listOf(obj), tasks = listOf(t), keyResults = listOf(kr))
        val repository = FakeCheckItRepository(initialBoard = board)
        val useCase = AutoUpdateKeyResultCurrentValueUseCase(repository)

        useCase(
            dailyPlanItem(id = 100L, taskId = 10L, startTimeMinutes = 9 * 60, endTimeMinutes = 12 * 60),
            DailyPlanItemStatus.Planned, DailyPlanItemStatus.Done, board
        )

        assertEquals(1, repository.updatedKeyResults.size)
        assertEquals(5.0, repository.updatedKeyResults[0].second.currentValue)
    }

    @Test
    fun `hours unit subtracts duration when unmarking done`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, currentValue = 10.0, unit = "Hours")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val board = TaskBoard(objectives = listOf(obj), tasks = listOf(t), keyResults = listOf(kr))
        val repository = FakeCheckItRepository(initialBoard = board)
        val useCase = AutoUpdateKeyResultCurrentValueUseCase(repository)

        useCase(
            dailyPlanItem(id = 100L, taskId = 10L, status = DailyPlanItemStatus.Done, startTimeMinutes = 9 * 60, endTimeMinutes = 11 * 60),
            DailyPlanItemStatus.Done, DailyPlanItemStatus.Planned, board
        )

        assertEquals(1, repository.updatedKeyResults.size)
        assertEquals(8.0, repository.updatedKeyResults[0].second.currentValue)
    }

    @Test
    fun `hours unit defaults to 1 hour when no time data`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, currentValue = 0.0, unit = "Hours")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val board = TaskBoard(objectives = listOf(obj), tasks = listOf(t), keyResults = listOf(kr))
        val repository = FakeCheckItRepository(initialBoard = board)
        val useCase = AutoUpdateKeyResultCurrentValueUseCase(repository)

        useCase(dailyPlanItem(id = 100L, taskId = 10L), DailyPlanItemStatus.Planned, DailyPlanItemStatus.Done, board)

        assertEquals(1, repository.updatedKeyResults.size)
        assertEquals(1.0, repository.updatedKeyResults[0].second.currentValue)
    }

    @Test
    fun `days unit adds 1 when marking done`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, currentValue = 0.0, unit = "Days")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val board = TaskBoard(objectives = listOf(obj), tasks = listOf(t), keyResults = listOf(kr))
        val repository = FakeCheckItRepository(initialBoard = board)
        val useCase = AutoUpdateKeyResultCurrentValueUseCase(repository)

        useCase(
            dailyPlanItem(id = 100L, taskId = 10L, startTimeMinutes = 9 * 60, endTimeMinutes = 17 * 60),
            DailyPlanItemStatus.Planned, DailyPlanItemStatus.Done, board
        )

        assertEquals(1, repository.updatedKeyResults.size)
        assertEquals(1.0, repository.updatedKeyResults[0].second.currentValue)
    }

    @Test
    fun `no taskId on dailyPlanItem skips update`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, currentValue = 0.0, unit = "Number")
        val board = TaskBoard(objectives = listOf(obj), keyResults = listOf(kr))
        val repository = FakeCheckItRepository(initialBoard = board)
        val useCase = AutoUpdateKeyResultCurrentValueUseCase(repository)

        useCase(dailyPlanItem(id = 100L, taskId = null), DailyPlanItemStatus.Planned, DailyPlanItemStatus.Done, board)

        assertTrue(repository.updatedKeyResults.isEmpty())
    }

    @Test
    fun `no keyResult on task skips update`() = runTest {
        val obj = objective(1L)
        val t = task(id = 10L, objective = obj, keyResult = null)
        val board = TaskBoard(objectives = listOf(obj), tasks = listOf(t))
        val repository = FakeCheckItRepository(initialBoard = board)
        val useCase = AutoUpdateKeyResultCurrentValueUseCase(repository)

        useCase(dailyPlanItem(id = 100L, taskId = 10L), DailyPlanItemStatus.Planned, DailyPlanItemStatus.Done, board)

        assertTrue(repository.updatedKeyResults.isEmpty())
    }

    @Test
    fun `same previous and next status skips update`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, currentValue = 0.0, unit = "Number")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val board = TaskBoard(objectives = listOf(obj), tasks = listOf(t), keyResults = listOf(kr))
        val repository = FakeCheckItRepository(initialBoard = board)
        val useCase = AutoUpdateKeyResultCurrentValueUseCase(repository)

        useCase(dailyPlanItem(id = 100L, taskId = 10L, status = DailyPlanItemStatus.Done), DailyPlanItemStatus.Done, DailyPlanItemStatus.Done, board)

        assertTrue(repository.updatedKeyResults.isEmpty())
    }

    @Test
    fun `currentValue never goes below zero for hours unit`() = runTest {
        val obj = objective(1L)
        val kr = keyResult(id = 1L, objectiveId = 1L, currentValue = 0.5, unit = "Hours")
        val t = task(id = 10L, objective = obj, keyResult = kr)
        val board = TaskBoard(objectives = listOf(obj), tasks = listOf(t), keyResults = listOf(kr))
        val repository = FakeCheckItRepository(initialBoard = board)
        val useCase = AutoUpdateKeyResultCurrentValueUseCase(repository)

        useCase(
            dailyPlanItem(id = 100L, taskId = 10L, status = DailyPlanItemStatus.Done, startTimeMinutes = 9 * 60, endTimeMinutes = 11 * 60),
            DailyPlanItemStatus.Done, DailyPlanItemStatus.Planned, board
        )

        assertEquals(1, repository.updatedKeyResults.size)
        assertEquals(0.0, repository.updatedKeyResults[0].second.currentValue)
    }
}
