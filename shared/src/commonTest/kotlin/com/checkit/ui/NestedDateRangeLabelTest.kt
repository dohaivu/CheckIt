package com.checkit.ui

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Metadata preview date pill: both ends are optional and independent, and the
 * end date is inclusive when deciding whether the range is overdue.
 */
class NestedDateRangeLabelTest {

    // Monday; a fixed reference keeps Today/Tomorrow/weekday wording stable.
    private val today = LocalDate(2026, 10, 26)

    @Test
    fun noDatesHasNoLabel() {
        assertNull(nestedDateRangeLabel(null, null, today))
    }

    @Test
    fun sameDayCollapsesToOneDate() {
        val label = assertNotNull(nestedDateRangeLabel(today, today, today))
        assertEquals("Today", label.text)
        assertFalse(label.isOverdue)
    }

    @Test
    fun oneSidedEndStaysExplicitEvenWithinTheWeek() {
        val label = assertNotNull(nestedDateRangeLabel(null, today.plus(3, DateTimeUnit.DAY), today))
        assertEquals("Until Oct 29", label.text)
    }

    @Test
    fun startOnlyReadsAsFrom() {
        val label = assertNotNull(
            nestedDateRangeLabel(today.minus(6, DateTimeUnit.DAY), null, today)
        )
        assertEquals("From Oct 20", label.text)
        assertFalse(label.isOverdue)
    }

    @Test
    fun endOnlyReadsAsUntil() {
        val label = assertNotNull(nestedDateRangeLabel(null, LocalDate(2026, 11, 3), today))
        assertEquals("Until Nov 3", label.text)
        assertFalse(label.isOverdue)
    }

    @Test
    fun rangeSpellsOutBothDatesWithoutWeekdayWording() {
        val label = assertNotNull(
            nestedDateRangeLabel(
                startDate = LocalDate(2026, 10, 20),
                endDate = LocalDate(2026, 10, 28),
                today = today
            )
        )
        assertEquals("Oct 20 → Oct 28", label.text)
        assertFalse(label.isOverdue)
    }

    @Test
    fun rangeAcrossYearsShowsTheYear() {
        val label = assertNotNull(
            nestedDateRangeLabel(
                startDate = LocalDate(2025, 12, 30),
                endDate = LocalDate(2026, 1, 2),
                today = today
            )
        )
        assertEquals("Dec 30, 2025 → Jan 2, 2026", label.text)
    }

    @Test
    fun endYesterdayIsOverdue() {
        val label = assertNotNull(
            nestedDateRangeLabel(null, today.minus(1, DateTimeUnit.DAY), today)
        )
        assertEquals("Until Oct 25", label.text)
        assertTrue(label.isOverdue)
    }

    @Test
    fun endTodayIsNotOverdue() {
        val label = assertNotNull(
            nestedDateRangeLabel(LocalDate(2026, 10, 20), today, today)
        )
        assertEquals("Oct 20 → Oct 26", label.text)
        assertFalse(label.isOverdue)
    }

    @Test
    fun pastStartWithoutEndIsNeverOverdue() {
        val label = assertNotNull(
            nestedDateRangeLabel(today.minus(30, DateTimeUnit.DAY), null, today)
        )
        assertFalse(label.isOverdue)
    }
}
