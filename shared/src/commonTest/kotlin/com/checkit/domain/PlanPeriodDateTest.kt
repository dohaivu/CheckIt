package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlanPeriodDateTest {

    private val day = LocalDate(2026, 3, 10) // Tuesday

    @Test
    fun dayStartIsAnchor() {
        val focus = PeriodFocus(Period.Day, day)
        assertEquals(day, focus.start)
        assertEquals(day.plus(1, DateTimeUnit.DAY), focus.endExclusive)
        assertEquals(day.toEpochDays().toInt(), focus.startEpochDays)
        assertEquals(day.toEpochDays().toInt(), focus.endInclusiveEpochDays)
    }

    @Test
    fun weekStartIsMonday() {
        val focus = PeriodFocus(Period.Week, day)
        assertEquals(LocalDate(2026, 3, 9), focus.start) // Monday
        assertEquals(LocalDate(2026, 3, 16), focus.endExclusive)
        assertEquals(LocalDate(2026, 3, 15), focus.endInclusive)
    }

    @Test
    fun sundayStillSnapsToSameMonday() {
        val sunday = LocalDate(2026, 3, 15)
        val focus = PeriodFocus(Period.Week, sunday)
        assertEquals(LocalDate(2026, 3, 9), focus.start)
        assertEquals(LocalDate(2026, 3, 16), focus.endExclusive)
    }

    @Test
    fun monthStartIsFirstOfMonth() {
        val focus = PeriodFocus(Period.Month, LocalDate(2026, 2, 27))
        assertEquals(LocalDate(2026, 2, 1), focus.start)
        assertEquals(LocalDate(2026, 3, 1), focus.endExclusive)
    }

    @Test
    fun quarterBoundaries() {
        val mar = PeriodFocus(Period.Quarter, LocalDate(2026, 3, 20))
        assertEquals(LocalDate(2026, 1, 1), mar.start)
        assertEquals(LocalDate(2026, 4, 1), mar.endExclusive)

        val jun = PeriodFocus(Period.Quarter, LocalDate(2026, 6, 20))
        assertEquals(LocalDate(2026, 4, 1), jun.start)
        assertEquals(LocalDate(2026, 7, 1), jun.endExclusive)

        val oct = PeriodFocus(Period.Quarter, LocalDate(2026, 10, 20))
        assertEquals(LocalDate(2026, 10, 1), oct.start)
        assertEquals(LocalDate(2027, 1, 1), oct.endExclusive)
    }

    @Test
    fun yearStartIsJanFirst() {
        val focus = PeriodFocus(Period.Year, LocalDate(2026, 12, 31))
        assertEquals(LocalDate(2026, 1, 1), focus.start)
        assertEquals(LocalDate(2027, 1, 1), focus.endExclusive)
    }

    @Test
    fun shiftDayAndWeek() {
        assertEquals(day.plus(2, DateTimeUnit.DAY), PeriodFocus(Period.Day, day).shift(2).start)
        assertEquals(day.minus(1, DateTimeUnit.DAY), PeriodFocus(Period.Day, day).shift(-1).start)
        assertEquals(
            LocalDate(2026, 3, 16),
            PeriodFocus(Period.Week, day).shift(1).start
        )
        assertEquals(
            LocalDate(2026, 3, 2),
            PeriodFocus(Period.Week, day).shift(-1).start
        )
    }

    @Test
    fun shiftMonthSnapsToMonthStart() {
        val anchor = LocalDate(2026, 3, 10)
        assertEquals(LocalDate(2026, 4, 1), PeriodFocus(Period.Month, anchor).shift(1).start)
        assertEquals(LocalDate(2026, 2, 1), PeriodFocus(Period.Month, anchor).shift(-1).start)
        assertEquals(LocalDate(2026, 6, 1), PeriodFocus(Period.Month, anchor).shift(3).start)
    }

    @Test
    fun shiftQuarterSnapsToQuarterStart() {
        val anchor = LocalDate(2026, 3, 10)
        assertEquals(LocalDate(2026, 4, 1), PeriodFocus(Period.Quarter, anchor).shift(1).start)
        assertEquals(LocalDate(2025, 10, 1), PeriodFocus(Period.Quarter, anchor).shift(-1).start)
        assertEquals(LocalDate(2026, 10, 1), PeriodFocus(Period.Quarter, anchor).shift(3).start)
    }

    @Test
    fun shiftYear() {
        val anchor = LocalDate(2026, 3, 10)
        assertEquals(LocalDate(2027, 1, 1), PeriodFocus(Period.Year, anchor).shift(1).start)
        assertEquals(LocalDate(2025, 1, 1), PeriodFocus(Period.Year, anchor).shift(-1).start)
    }

    @Test
    fun zoomOutChain() {
        assertEquals(Period.Week, PeriodFocus(Period.Day, day).zoomOut().period)
        assertEquals(Period.Month, PeriodFocus(Period.Week, day).zoomOut().period)
        assertEquals(Period.Quarter, PeriodFocus(Period.Month, day).zoomOut().period)
        assertEquals(Period.Year, PeriodFocus(Period.Quarter, day).zoomOut().period)
        assertEquals(Period.Year, PeriodFocus(Period.Year, day).zoomOut().period)
    }

    @Test
    fun zoomInOnlyAllowsFinerPeriods() {
        val focus = PeriodFocus(Period.Week, day)
        assertEquals(Period.Day, focus.zoomIn(Period.Day).period)
        // Coarser or equal periods are ignored.
        assertEquals(Period.Week, focus.zoomIn(Period.Week).period)
        assertEquals(Period.Week, focus.zoomIn(Period.Year).period)
        assertEquals(Period.Week, focus.zoomIn(Period.Quarter).period)
    }

    @Test
    fun parentChildChain() {
        assertNull(Period.Year.parent())
        assertEquals(Period.Year, Period.Quarter.parent())
        assertEquals(Period.Quarter, Period.Month.parent())
        assertEquals(Period.Month, Period.Week.parent())
        assertEquals(Period.Week, Period.Day.parent())

        assertNull(Period.Day.child())
        assertEquals(Period.Day, Period.Week.child())
        assertEquals(Period.Week, Period.Month.child())
        assertEquals(Period.Month, Period.Quarter.child())
        assertEquals(Period.Quarter, Period.Year.child())
    }
}

private fun Period.parent(): Period? = when (this) {
    Period.Year -> null
    Period.Quarter -> Period.Year
    Period.Month -> Period.Quarter
    Period.Week -> Period.Month
    Period.Day -> Period.Week
}

private fun Period.child(): Period? = when (this) {
    Period.Year -> Period.Quarter
    Period.Quarter -> Period.Month
    Period.Month -> Period.Week
    Period.Week -> Period.Day
    Period.Day -> null
}
