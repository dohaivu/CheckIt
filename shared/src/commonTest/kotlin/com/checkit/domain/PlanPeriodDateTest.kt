package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanPeriodDateTest {

    private val day = LocalDate(2026, 3, 10) // Tuesday

    @Test
    fun dayStartIsAnchor() {
        val focus = PlanFocus(PlanPeriod.Day, day)
        assertEquals(day, focus.start)
        assertEquals(day.plus(1, DateTimeUnit.DAY), focus.endExclusive)
        assertEquals(day.toEpochDays().toInt(), focus.startEpochDays)
        assertEquals(day.toEpochDays().toInt(), focus.endInclusiveEpochDays)
    }

    @Test
    fun weekStartIsMonday() {
        val focus = PlanFocus(PlanPeriod.Week, day)
        assertEquals(LocalDate(2026, 3, 9), focus.start) // Monday
        assertEquals(LocalDate(2026, 3, 16), focus.endExclusive)
        assertEquals(LocalDate(2026, 3, 15), focus.endInclusive)
    }

    @Test
    fun sundayStillSnapsToSameMonday() {
        val sunday = LocalDate(2026, 3, 15)
        val focus = PlanFocus(PlanPeriod.Week, sunday)
        assertEquals(LocalDate(2026, 3, 9), focus.start)
        assertEquals(LocalDate(2026, 3, 16), focus.endExclusive)
    }

    @Test
    fun monthStartIsFirstOfMonth() {
        val focus = PlanFocus(PlanPeriod.Month, LocalDate(2026, 2, 27))
        assertEquals(LocalDate(2026, 2, 1), focus.start)
        assertEquals(LocalDate(2026, 3, 1), focus.endExclusive)
    }

    @Test
    fun quarterBoundaries() {
        val mar = PlanFocus(PlanPeriod.Quarter, LocalDate(2026, 3, 20))
        assertEquals(LocalDate(2026, 1, 1), mar.start)
        assertEquals(LocalDate(2026, 4, 1), mar.endExclusive)

        val jun = PlanFocus(PlanPeriod.Quarter, LocalDate(2026, 6, 20))
        assertEquals(LocalDate(2026, 4, 1), jun.start)
        assertEquals(LocalDate(2026, 7, 1), jun.endExclusive)

        val oct = PlanFocus(PlanPeriod.Quarter, LocalDate(2026, 10, 20))
        assertEquals(LocalDate(2026, 10, 1), oct.start)
        assertEquals(LocalDate(2027, 1, 1), oct.endExclusive)
    }

    @Test
    fun yearStartIsJanFirst() {
        val focus = PlanFocus(PlanPeriod.Year, LocalDate(2026, 12, 31))
        assertEquals(LocalDate(2026, 1, 1), focus.start)
        assertEquals(LocalDate(2027, 1, 1), focus.endExclusive)
    }

    @Test
    fun shiftDayAndWeek() {
        assertEquals(day.plus(2, DateTimeUnit.DAY), PlanFocus(PlanPeriod.Day, day).shift(2).start)
        assertEquals(day.minus(1, DateTimeUnit.DAY), PlanFocus(PlanPeriod.Day, day).shift(-1).start)
        assertEquals(
            LocalDate(2026, 3, 16),
            PlanFocus(PlanPeriod.Week, day).shift(1).start
        )
        assertEquals(
            LocalDate(2026, 3, 2),
            PlanFocus(PlanPeriod.Week, day).shift(-1).start
        )
    }

    @Test
    fun shiftMonthSnapsToMonthStart() {
        val anchor = LocalDate(2026, 3, 10)
        assertEquals(LocalDate(2026, 4, 1), PlanFocus(PlanPeriod.Month, anchor).shift(1).start)
        assertEquals(LocalDate(2026, 2, 1), PlanFocus(PlanPeriod.Month, anchor).shift(-1).start)
        assertEquals(LocalDate(2026, 6, 1), PlanFocus(PlanPeriod.Month, anchor).shift(3).start)
    }

    @Test
    fun shiftQuarterSnapsToQuarterStart() {
        val anchor = LocalDate(2026, 3, 10)
        assertEquals(LocalDate(2026, 4, 1), PlanFocus(PlanPeriod.Quarter, anchor).shift(1).start)
        assertEquals(LocalDate(2025, 10, 1), PlanFocus(PlanPeriod.Quarter, anchor).shift(-1).start)
        assertEquals(LocalDate(2026, 10, 1), PlanFocus(PlanPeriod.Quarter, anchor).shift(3).start)
    }

    @Test
    fun shiftYear() {
        val anchor = LocalDate(2026, 3, 10)
        assertEquals(LocalDate(2027, 1, 1), PlanFocus(PlanPeriod.Year, anchor).shift(1).start)
        assertEquals(LocalDate(2025, 1, 1), PlanFocus(PlanPeriod.Year, anchor).shift(-1).start)
    }

    @Test
    fun zoomOutChain() {
        assertEquals(PlanPeriod.Week, PlanFocus(PlanPeriod.Day, day).zoomOut().period)
        assertEquals(PlanPeriod.Month, PlanFocus(PlanPeriod.Week, day).zoomOut().period)
        assertEquals(PlanPeriod.Quarter, PlanFocus(PlanPeriod.Month, day).zoomOut().period)
        assertEquals(PlanPeriod.Year, PlanFocus(PlanPeriod.Quarter, day).zoomOut().period)
        assertEquals(PlanPeriod.Year, PlanFocus(PlanPeriod.Year, day).zoomOut().period)
    }

    @Test
    fun zoomInOnlyAllowsFinerPeriods() {
        val focus = PlanFocus(PlanPeriod.Week, day)
        assertEquals(PlanPeriod.Day, focus.zoomIn(PlanPeriod.Day).period)
        // Coarser or equal periods are ignored.
        assertEquals(PlanPeriod.Week, focus.zoomIn(PlanPeriod.Week).period)
        assertEquals(PlanPeriod.Week, focus.zoomIn(PlanPeriod.Year).period)
        assertEquals(PlanPeriod.Week, focus.zoomIn(PlanPeriod.Quarter).period)
    }

    @Test
    fun parentChildChain() {
        assertNull(PlanPeriod.Year.parent())
        assertEquals(PlanPeriod.Year, PlanPeriod.Quarter.parent())
        assertEquals(PlanPeriod.Quarter, PlanPeriod.Month.parent())
        assertEquals(PlanPeriod.Month, PlanPeriod.Week.parent())
        assertEquals(PlanPeriod.Week, PlanPeriod.Day.parent())

        assertNull(PlanPeriod.Day.child())
        assertEquals(PlanPeriod.Day, PlanPeriod.Week.child())
        assertEquals(PlanPeriod.Week, PlanPeriod.Month.child())
        assertEquals(PlanPeriod.Month, PlanPeriod.Quarter.child())
        assertEquals(PlanPeriod.Quarter, PlanPeriod.Year.child())
    }

    @Test
    fun periodPlanCoversFocus() {
        val weekPlan = PeriodPlan(
            period = PlanPeriod.Week,
            startEpochDays = PlanFocus(PlanPeriod.Week, day).startEpochDays,
            endEpochDays = PlanFocus(PlanPeriod.Week, day).endInclusiveEpochDays
        )
        assertTrue(weekPlan.covers(PlanFocus(PlanPeriod.Week, day)))
        assertTrue(weekPlan.covers(PlanFocus(PlanPeriod.Week, LocalDate(2026, 3, 12))))
        assertFalse(weekPlan.covers(PlanFocus(PlanPeriod.Week, LocalDate(2026, 3, 23))))
        assertFalse(weekPlan.covers(PlanFocus(PlanPeriod.Month, day)))
    }

    @Test
    fun cycleDetectionRejectsSelfAndDescendant() {
        val a = priority(1, parentId = null)
        val b = priority(2, parentId = 1)
        val c = priority(3, parentId = 2)
        val priorities = listOf(a, b, c)

        assertTrue(wouldCreateCycle(priorities, id = 1, newParentId = 1))
        assertTrue(wouldCreateCycle(priorities, id = 1, newParentId = 3))
        assertTrue(wouldCreateCycle(priorities, id = 2, newParentId = 3))
        assertFalse(wouldCreateCycle(priorities, id = 3, newParentId = 1))
        assertFalse(wouldCreateCycle(priorities, id = 2, newParentId = 1))
        assertFalse(wouldCreateCycle(priorities, id = 1, newParentId = null))
    }

    @Test
    fun cycleDetectionAllowsReparentSiblings() {
        val a = priority(1, parentId = null)
        val b = priority(2, parentId = 1)
        val c = priority(3, parentId = 1)
        val priorities = listOf(a, b, c)
        assertFalse(wouldCreateCycle(priorities, id = 2, newParentId = 3))
        assertFalse(wouldCreateCycle(priorities, id = 3, newParentId = 2))
    }

    private fun priority(id: Long, parentId: Long?) = PlanPriority(
        id = id,
        periodPlan = PeriodPlan(id = 1L, period = PlanPeriod.Year, startEpochDays = 0, endEpochDays = 0),
        parentId = parentId,
        title = "P$id",
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )
}

private fun PlanPeriod.parent(): PlanPeriod? = when (this) {
    PlanPeriod.Year -> null
    PlanPeriod.Quarter -> PlanPeriod.Year
    PlanPeriod.Month -> PlanPeriod.Quarter
    PlanPeriod.Week -> PlanPeriod.Month
    PlanPeriod.Day -> PlanPeriod.Week
}

private fun PlanPeriod.child(): PlanPeriod? = when (this) {
    PlanPeriod.Year -> PlanPeriod.Quarter
    PlanPeriod.Quarter -> PlanPeriod.Month
    PlanPeriod.Month -> PlanPeriod.Week
    PlanPeriod.Week -> PlanPeriod.Day
    PlanPeriod.Day -> null
}
