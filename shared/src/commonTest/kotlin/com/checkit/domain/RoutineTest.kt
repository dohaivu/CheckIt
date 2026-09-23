package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
            RoutineStepTemplate(id = "id-1", title = "Drink water", description = "2 glasses", sortOrder = 0),
            RoutineStepTemplate(id = "id-2", title = "Read", sortOrder = 1)
        )
        assertEquals(steps, decodeRoutineSteps(encodeRoutineSteps(steps)))
    }

    @Test
    fun stepsJsonWithoutDescriptionDecodesToBlank() {
        val legacy = """[{"id":"id-1","title":"Drink water","sortOrder":0}]"""
        assertEquals(
            listOf(RoutineStepTemplate(id = "id-1", title = "Drink water", sortOrder = 0)),
            decodeRoutineSteps(legacy)
        )
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

    @Test
    fun weekdaysJsonRoundTrip() {
        val days = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY)
        assertEquals(days, decodeActiveWeekdays(encodeActiveWeekdays(days)))
        assertEquals(emptySet(), decodeActiveWeekdays(encodeActiveWeekdays(emptySet())))
    }

    @Test
    fun weekdaysJsonFallsBackToAllOnBlankOrCorrupt() {
        assertEquals(AllWeekdays, decodeActiveWeekdays(null))
        assertEquals(AllWeekdays, decodeActiveWeekdays(""))
        assertEquals(AllWeekdays, decodeActiveWeekdays("not-json"))
    }

    @Test
    fun weekdaysJsonIgnoresUnknownNames() {
        assertEquals(
            setOf(DayOfWeek.MONDAY),
            decodeActiveWeekdays("[\"MONDAY\",\"FUNDAY\"]")
        )
    }

    @Test
    fun isRoutineScheduledRespectsWeekdays() {
        val monday = LocalDate(2026, 9, 21)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertTrue(isRoutineScheduled(monday, AllWeekdays))
        assertTrue(isRoutineScheduled(monday, setOf(DayOfWeek.MONDAY)))
        assertFalse(isRoutineScheduled(monday, setOf(DayOfWeek.TUESDAY)))
        assertFalse(isRoutineScheduled(monday, emptySet()))
    }

    @Test
    fun nextScheduledDateFindsSameDayNextDayAndWraps() {
        val monday = LocalDate(2026, 9, 21)
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals(monday, nextScheduledDate(monday, weekdays))
        assertEquals(LocalDate(2026, 9, 23), nextScheduledDate(monday.plus(1, DateTimeUnit.DAY), weekdays))
        assertEquals(LocalDate(2026, 9, 28), nextScheduledDate(LocalDate(2026, 9, 27), weekdays))
        assertEquals(monday, nextScheduledDate(monday, AllWeekdays))
        assertNull(nextScheduledDate(monday, emptySet()))
    }

    @Test
    fun streakSkipsOffScheduleDays() {
        // Mon 21 done, Tue 22 off-schedule, Wed 23 done, today Thu 24 done.
        val done = setOf(
            LocalDate(2026, 9, 21),
            LocalDate(2026, 9, 23),
            LocalDate(2026, 9, 24)
        )
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)
        assertEquals(3, calculateRoutineStreak(done, weekdays, LocalDate(2026, 9, 24)))
    }

    @Test
    fun streakBreaksOnScheduledMissButNotOnOffDay() {
        // Wed 23 scheduled but missed; Tue 22 off-schedule.
        val done = setOf(LocalDate(2026, 9, 21))
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)
        assertEquals(0, calculateRoutineStreak(done, weekdays, LocalDate(2026, 9, 24)))
    }

    @Test
    fun streakCountsFromYesterdayWhenTodayIsOffSchedule() {
        val done = setOf(LocalDate(2026, 9, 23))
        val weekdays = setOf(DayOfWeek.WEDNESDAY)
        // Thu 24 and Fri 25 are off-schedule: streak still counts Wed 23.
        assertEquals(1, calculateRoutineStreak(done, weekdays, LocalDate(2026, 9, 24)))
        assertEquals(1, calculateRoutineStreak(done, weekdays, LocalDate(2026, 9, 25)))
        // But a missed scheduled Wednesday breaks it.
        assertEquals(0, calculateRoutineStreak(setOf(LocalDate(2026, 9, 16)), weekdays, LocalDate(2026, 9, 25)))
    }

    @Test
    fun streakIsZeroWhenPaused() {
        val done = setOf(LocalDate(2026, 9, 24))
        assertEquals(0, calculateRoutineStreak(done, emptySet(), LocalDate(2026, 9, 24)))
    }

    @Test
    fun shouldScheduleRequiresTimeAndNonEmptyWeekdays() {
        assertTrue(shouldScheduleRoutineReminder(8 * 60, AllWeekdays))
        assertFalse(shouldScheduleRoutineReminder(null, AllWeekdays))
        assertFalse(shouldScheduleRoutineReminder(8 * 60, emptySet()))
        assertFalse(shouldScheduleRoutineReminder(null, emptySet()))
    }
}
