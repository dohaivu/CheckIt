package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Rolling windows are local attention views: they include today and slide
 * with the clock. They are not calendar periods, so they never expose a
 * navigable [FocusPeriod].
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
        val window = NestedRollingPreset.Last30Days.window(today)
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
        val window = NestedRollingPreset.Last7Days.window(today)

        val filtered = filterNestedTree(roots, start = window.start, end = window.endInclusive)

        assertEquals(listOf("ongoing"), filtered.map { it.item.id })
    }
}
