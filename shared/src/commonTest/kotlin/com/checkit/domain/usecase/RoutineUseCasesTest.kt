package com.checkit.domain.usecase

import com.checkit.domain.Routine
import com.checkit.domain.RoutineLog
import com.checkit.domain.RoutineStepTemplate
import com.checkit.domain.RoutineTodayState
import com.checkit.ui.myday.FakeRoutineRepository
import com.checkit.ui.myday.FakeRoutineTodayStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
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
    fun deleteRoutineClearsTodayChecks() = runTest {
        val repo = FakeRoutineRepository(listOf(routineWithSteps("r1", listOf("s1"))))
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val store = FakeRoutineTodayStore(
            RoutineTodayState(
                epochDay = today.toEpochDays().toInt(),
                checks = mapOf("r1" to setOf("s1"))
            )
        )
        DeleteRoutineUseCase(repo, store)("r1")

        assertTrue(repo.routines.isEmpty())
        assertTrue(store.observe().first().checks.isEmpty())
    }

    private fun routineWithSteps(id: String, stepIds: List<String>) = Routine(
        id = id,
        title = "Routine $id",
        steps = stepIds.mapIndexed { index, stepId ->
            RoutineStepTemplate(id = stepId, title = "Step $stepId", sortOrder = index)
        }
    )
}
