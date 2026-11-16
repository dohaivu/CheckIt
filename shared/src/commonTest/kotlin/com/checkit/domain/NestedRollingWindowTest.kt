package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Rolling presets are local attention views. Calendar windows include today
 * and slide with the clock, while [NestedRollingPreset.Overdue] is a deadline
 * predicate. They are not calendar periods, so they never expose a navigable
 * [FocusPeriod].
 */
class NestedRollingWindowTest {

    private val today = LocalDate(2026, 11, 14)

    @Test
    fun last7DaysIncludesTodayAndThePreviousSix() {
        assertEquals(
            NestedDateWindow(LocalDate(2026, 11, 8), today),
            NestedRollingPreset.Last7Days.window(today)
        )
    }

    @Test
    fun next7DaysStartsToday() {
        assertEquals(
            NestedDateWindow(today, LocalDate(2026, 11, 20)),
            NestedRollingPreset.Next7Days.window(today)
        )
    }

    @Test
    fun last30DaysSpansThirtyInclusiveDays() {
        val window = assertNotNull(NestedRollingPreset.Last30Days.window(today))
        assertEquals(today, window.endInclusive)
        assertEquals(LocalDate(2026, 10, 16), window.start)
    }

    @Test
    fun rollingWindowsCrossMonthYearAndLeapBoundaries() {
        val jan1 = LocalDate(2026, 1, 1)
        assertEquals(
            NestedDateWindow(LocalDate(2025, 12, 26), jan1),
            NestedRollingPreset.Last7Days.window(jan1)
        )

        val dec31 = LocalDate(2026, 12, 31)
        assertEquals(
            NestedDateWindow(dec31, LocalDate(2027, 1, 6)),
            NestedRollingPreset.Next7Days.window(dec31)
        )

        val leap = LocalDate(2024, 3, 1)
        assertEquals(
            NestedDateWindow(LocalDate(2024, 2, 24), leap),
            NestedRollingPreset.Last7Days.window(leap)
        )
    }

    @Test
    fun focusFilterIsNavigableWhileRollingIsNot() {
        val focus = NestedDateFilter.Focus(FocusPeriod(Period.Week, today))
        assertEquals(FocusPeriod(Period.Week, today), focus.calendarFocus)
        assertNull(NestedDateFilter.Rolling(NestedRollingPreset.Last7Days).calendarFocus)
    }

    @Test
    fun rollingWindowSelectsOpenEndedNodes() {
        val roots = listOf(
            NestedItemNode(
                item = NestedListItem(
                    id = "closed",
                    documentId = "doc",
                    parentId = null,
                    position = 0,
                    text = "closed",
                    startDate = LocalDate(2026, 11, 1),
                    endDate = LocalDate(2026, 11, 7),
                    createdAtMillis = 0L,
                    updatedAtMillis = 0L
                )
            ),
            NestedItemNode(
                item = NestedListItem(
                    id = "ongoing",
                    documentId = "doc",
                    parentId = null,
                    position = 1,
                    text = "ongoing",
                    startDate = LocalDate(2026, 11, 2),
                    endDate = null,
                    createdAtMillis = 0L,
                    updatedAtMillis = 0L
                )
            )
        )
        val window = assertNotNull(NestedRollingPreset.Last7Days.window(today))

        val filtered = filterNestedTree(roots, start = window.start, end = window.endInclusive)

        assertEquals(listOf("ongoing"), filtered.map { it.item.id })
    }

    @Test
    fun overdueHasNoWindowAndUsesExclusiveCutoff() {
        assertNull(NestedRollingPreset.Overdue.window(today))

        val overdue = NestedDateFilter.Rolling(NestedRollingPreset.Overdue)
        assertEquals(today, overdue.overdueBefore(today))
        assertNull(NestedDateFilter.Rolling(NestedRollingPreset.Last7Days).overdueBefore(today))
        assertNull(NestedDateFilter.Focus(FocusPeriod(Period.Week, today)).overdueBefore(today))
    }

    @Test
    fun overdueFilterKeepsOnlyDeadlinesBeforeToday() {
        fun node(id: String, start: LocalDate? = null, end: LocalDate? = null) = NestedItemNode(
            item = NestedListItem(
                id = id,
                documentId = "doc",
                parentId = null,
                position = 0,
                text = id,
                startDate = start,
                endDate = end,
                createdAtMillis = 0L,
                updatedAtMillis = 0L
            )
        )

        val roots = listOf(
            node("past", end = today.minus(1, DateTimeUnit.DAY)),
            node("today", end = today),
            node("future", end = today.plus(1, DateTimeUnit.DAY)),
            node("ongoing", start = today.minus(2, DateTimeUnit.DAY), end = today.plus(2, DateTimeUnit.DAY)),
            node("startOnly", start = today.minus(2, DateTimeUnit.DAY)),
            node("undated")
        )

        val filtered = filterNestedTree(roots, overdueBefore = today)

        assertEquals(listOf("past"), filtered.map { it.item.id })
    }
}
