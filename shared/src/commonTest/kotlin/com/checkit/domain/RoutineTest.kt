package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoutineTest {

    @Test
    fun percentIsZeroWhenNoSteps() {
        assertEquals(0, routinePercent(0, setOf("a"), setOf("a")))
    }

    @Test
    fun percentCountsOnlyValidCheckedSteps() {
        val valid = setOf("a", "b", "c", "d")
        assertEquals(50, routinePercent(4, setOf("a", "b", "stale-id"), valid))
        assertEquals(100, routinePercent(4, valid, valid))
        assertEquals(0, routinePercent(4, emptySet(), valid))
    }

    @Test
    fun streakThresholdIsEightyPercent() {
        assertFalse(routineMeetsStreakThreshold(79))
        assertTrue(routineMeetsStreakThreshold(80))
        assertTrue(routineMeetsStreakThreshold(100))
    }

    @Test
    fun stepsJsonRoundTrip() {
        val steps = listOf(
            RoutineStepTemplate(id = "id-1", title = "Drink water", sortOrder = 0),
            RoutineStepTemplate(id = "id-2", title = "Read", sortOrder = 1)
        )
        assertEquals(steps, decodeRoutineSteps(encodeRoutineSteps(steps)))
    }

    @Test
    fun stepsJsonFallsBackToEmptyOnBlankOrCorrupt() {
        assertEquals(emptyList(), decodeRoutineSteps(""))
        assertEquals(emptyList(), decodeRoutineSteps("not-json"))
    }

    @Test
    fun checksJsonRoundTrip() {
        val checks = mapOf("r1" to setOf("s1", "s2"), "r2" to setOf("s3"))
        assertEquals(checks, decodeRoutineChecks(encodeRoutineChecks(checks)))
        assertEquals(emptyMap(), decodeRoutineChecks(null))
    }

    @Test
    fun rolloverKeepsChecksForTodayOnly() {
        val checks = mapOf("r1" to setOf("s1"))
        assertEquals(checks, resolveRoutineTodayChecks(100, checks, 100))
        assertEquals(emptyMap(), resolveRoutineTodayChecks(99, checks, 100))
        assertEquals(emptyMap(), resolveRoutineTodayChecks(null, checks, 100))
    }

    @Test
    fun streakDatesKeepOnlyThresholdDays() {
        val logs = listOf(
            RoutineLog("r1", LocalDate(2026, 9, 21).toEpochDays().toInt(), 100),
            RoutineLog("r1", LocalDate(2026, 9, 22).toEpochDays().toInt(), 80),
            RoutineLog("r1", LocalDate(2026, 9, 23).toEpochDays().toInt(), 79)
        )
        assertEquals(
            setOf(LocalDate(2026, 9, 21), LocalDate(2026, 9, 22)),
            routineStreakDates(logs)
        )
    }

    @Test
    fun intensityMapsPercentToFraction() {
        val logs = listOf(
            RoutineLog("r1", LocalDate(2026, 9, 21).toEpochDays().toInt(), 50)
        )
        assertEquals(
            mapOf(LocalDate(2026, 9, 21) to 0.5f),
            routineIntensityByDate(logs)
        )
    }
}
