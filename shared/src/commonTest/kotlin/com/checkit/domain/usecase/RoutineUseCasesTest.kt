package com.checkit.domain.usecase

import com.checkit.domain.AllWeekdays
import com.checkit.domain.Routine
import com.checkit.domain.RoutineLog
import com.checkit.domain.RoutineStepTemplate
import com.checkit.domain.RoutineTodayState
import com.checkit.notifications.RoutineReminderScheduler
import com.checkit.notifications.ScheduledRoutineReminder
import com.checkit.ui.myday.FakeRoutineRepository
import com.checkit.ui.myday.FakeRoutineTodayStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class RoutineUseCasesTest {

    @Test
    fun toggleSealsPercentLogForToday() = runTest {
        val routine = routineWithSteps("r1", listOf("s1", "s2", "s3", "s4"))
        val repo = FakeRoutineRepository(listOf(routine))
        val store = FakeRoutineTodayStore()
        val toggle = ToggleRoutineStepUseCase(repo, store)

        toggle("r1", "s1")
        toggle("r1", "s2")

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayEpoch = today.toEpochDays().toInt()
        assertEquals(mapOf("r1" to setOf("s1", "s2")), store.observe().first().checks)
        assertEquals(
            listOf(RoutineLog("r1", todayEpoch, 50)),
            repo.observeLogs(todayEpoch, todayEpoch).first()
        )
    }

    @Test
    fun toggleDropsStaleChecksOnRollover() = runTest {
        val routine = routineWithSteps("r1", listOf("s1", "s2"))
        val repo = FakeRoutineRepository(listOf(routine))
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayEpoch = today.toEpochDays().toInt()
        val store = FakeRoutineTodayStore(
            RoutineTodayState(epochDay = todayEpoch - 1, checks = mapOf("r1" to setOf("s1", "s2")))
        )
        val toggle = ToggleRoutineStepUseCase(repo, store)

        toggle("r1", "s1")

        assertEquals(mapOf("r1" to setOf("s1")), store.observe().first().checks)
        assertEquals(50, repo.observeLogs(todayEpoch, todayEpoch).first().single().percent)
    }

    @Test
    fun toggleUnknownRoutineIsNoop() = runTest {
        val repo = FakeRoutineRepository(emptyList())
        val store = FakeRoutineTodayStore()
        ToggleRoutineStepUseCase(repo, store)("missing", "s1")

        assertTrue(store.observe().first().checks.isEmpty())
        assertTrue(repo.logs.isEmpty())
    }

    @Test
    fun toggleRoutineNotScheduledTodayIsNoop() = runTest {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val tomorrowWeekday = today.plus(1, DateTimeUnit.DAY).dayOfWeek
        val routine = routineWithSteps("r1", listOf("s1")).copy(activeWeekdays = setOf(tomorrowWeekday))
        val repo = FakeRoutineRepository(listOf(routine))
        val store = FakeRoutineTodayStore()
        ToggleRoutineStepUseCase(repo, store)("r1", "s1")

        assertTrue(store.observe().first().checks.isEmpty())
        assertTrue(repo.logs.isEmpty())
    }

    @Test
    fun deleteRoutineClearsTodayChecks() = runTest {
        val repo = FakeRoutineRepository(listOf(routineWithSteps("r1", listOf("s1"))))
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val store = FakeRoutineTodayStore(
            RoutineTodayState(
                epochDay = today.toEpochDays().toInt(),
                checks = mapOf("r1" to setOf("s1"))
            )
        )
        val scheduler = FakeRoutineReminderScheduler()
        DeleteRoutineUseCase(repo, store, scheduler)("r1")

        assertTrue(repo.routines.isEmpty())
        assertTrue(store.observe().first().checks.isEmpty())
        assertEquals(listOf("r1"), scheduler.cancelled)
    }

    @Test
    fun saveRoutineWithReminderSchedules() = runTest {
        val repo = FakeRoutineRepository()
        val scheduler = FakeRoutineReminderScheduler()
        val save = SaveRoutineUseCase(repo, scheduler)

        val id = save(
            id = null,
            title = " Morning ",
            description = " Daily reset ",
            reminderMinutes = 8 * 60,
            activeWeekdays = AllWeekdays,
            steps = listOf(
                RoutineStepTemplate(id = "s1", title = "Water"),
                RoutineStepTemplate(id = "s2", title = "  ")
            )
        )

        assertEquals(
            listOf(
                ScheduledRoutineReminder(
                    routineId = id,
                    title = "Morning",
                    reminderMinutes = 8 * 60,
                    stepCount = 1,
                    activeWeekdays = AllWeekdays
                )
            ),
            scheduler.scheduled
        )
        assertTrue(scheduler.cancelled.isEmpty())
        assertEquals("Daily reset", repo.routines.single().description)
    }

    @Test
    fun saveRoutineWithoutReminderCancelsExisting() = runTest {
        val repo = FakeRoutineRepository(listOf(routineWithSteps("r1", listOf("s1"))))
        val scheduler = FakeRoutineReminderScheduler()
        SaveRoutineUseCase(repo, scheduler)(
            id = "r1",
            title = "Evening",
            description = "",
            reminderMinutes = null,
            activeWeekdays = AllWeekdays,
            steps = emptyList()
        )

        assertEquals(listOf("r1"), scheduler.cancelled)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun saveRoutineWithReminderButNoWeekdaysCancelsExisting() = runTest {
        val repo = FakeRoutineRepository(listOf(routineWithSteps("r1", listOf("s1"))))
        val scheduler = FakeRoutineReminderScheduler()
        SaveRoutineUseCase(repo, scheduler)(
            id = "r1",
            title = "Paused",
            description = "",
            reminderMinutes = 8 * 60,
            activeWeekdays = emptySet(),
            steps = emptyList()
        )

        assertEquals(listOf("r1"), scheduler.cancelled)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun resetStaleTodayClearsPreviousDayChecks() = runTest {
        val todayEpoch = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            .toEpochDays().toInt()
        val store = FakeRoutineTodayStore(
            RoutineTodayState(epochDay = todayEpoch - 1, checks = mapOf("r1" to setOf("s1")))
        )
        ResetStaleRoutineTodayUseCase(store)()

        val state = store.observe().first()
        assertEquals(todayEpoch, state.epochDay)
        assertTrue(state.checks.isEmpty())
    }

    @Test
    fun resetStaleTodayLeavesFreshStateUntouched() = runTest {
        val todayEpoch = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            .toEpochDays().toInt()
        val fresh = RoutineTodayState(epochDay = todayEpoch, checks = mapOf("r1" to setOf("s1")))
        val store = FakeRoutineTodayStore(fresh)
        ResetStaleRoutineTodayUseCase(store)()

        assertEquals(fresh, store.observe().first())
    }

    private fun routineWithSteps(id: String, stepIds: List<String>) = Routine(
        id = id,
        title = "Routine $id",
        steps = stepIds.mapIndexed { index, stepId ->
            RoutineStepTemplate(id = stepId, title = "Step $stepId", sortOrder = index)
        }
    )
}

private class FakeRoutineReminderScheduler : RoutineReminderScheduler {
    val scheduled = mutableListOf<ScheduledRoutineReminder>()
    val cancelled = mutableListOf<String>()

    override suspend fun scheduleRoutineReminder(reminder: ScheduledRoutineReminder) {
        scheduled.add(reminder)
    }

    override suspend fun cancelRoutineReminder(routineId: String) {
        cancelled.add(routineId)
    }
}
