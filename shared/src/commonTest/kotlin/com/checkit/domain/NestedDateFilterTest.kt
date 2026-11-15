package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Date-window matching for the outline filter: closed ranges overlap, and a
 * missing bound is open-ended, so start-only and end-only nodes participate
 * instead of being excluded.
 */
class NestedDateFilterTest {

    private fun d(month: Int, day: Int, year: Int = 2026) = LocalDate(year, month, day)

    private fun item(
        id: String,
        parentId: String? = null,
        start: LocalDate? = null,
        end: LocalDate? = null,
        text: String = id
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = 0,
        text = text,
        startDate = start,
        endDate = end,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun ids(nodes: List<NestedItemNode>) = nodes.map { it.item.id }

    @Test
    fun startOnlyMatchesItsDayAndLater() {
        val roots = listOf(NestedItemNode(item = item("a", start = d(11, 10))))

        assertEquals(listOf("a"), ids(filterNestedTree(roots, start = d(11, 10), end = d(11, 10))))
        assertEquals(listOf("a"), ids(filterNestedTree(roots, start = d(11, 20), end = d(11, 22))))
        assertTrue(filterNestedTree(roots, start = d(11, 9), end = d(11, 9)).isEmpty())
    }

    @Test
    fun endOnlyMatchesItsDayAndEarlier() {
        val roots = listOf(NestedItemNode(item = item("a", end = d(11, 10))))

        assertEquals(listOf("a"), ids(filterNestedTree(roots, start = d(11, 10), end = d(11, 10))))
        assertEquals(listOf("a"), ids(filterNestedTree(roots, start = d(11, 1), end = d(11, 5))))
        assertTrue(filterNestedTree(roots, start = d(11, 11), end = d(11, 12)).isEmpty())
    }

    @Test
    fun closedRangeMatchesOnlyOnOverlap() {
        val roots = listOf(NestedItemNode(item = item("a", start = d(11, 10), end = d(11, 12))))

        assertTrue(filterNestedTree(roots, start = d(11, 9), end = d(11, 9)).isEmpty())
        assertEquals(listOf("a"), ids(filterNestedTree(roots, start = d(11, 12), end = d(11, 20))))
        assertEquals(listOf("a"), ids(filterNestedTree(roots, start = d(11, 1), end = d(11, 10))))
        assertTrue(filterNestedTree(roots, start = d(11, 13), end = d(11, 15)).isEmpty())
    }

    @Test
    fun undatedNodesNeverMatchADateWindow() {
        val roots = listOf(NestedItemNode(item = item("a")))

        assertTrue(filterNestedTree(roots, start = d(11, 10), end = d(11, 10)).isEmpty())
    }

    @Test
    fun windowBoundariesAreInclusive() {
        // Ends exactly on the window's first day.
        assertTrue(matchesNestedDateWindow(d(11, 10), d(11, 12), d(11, 12), d(11, 20)))
        // Starts exactly on the window's last day.
        assertTrue(matchesNestedDateWindow(d(11, 10), d(11, 12), d(11, 1), d(11, 10)))
        // One day outside on either side.
        assertFalse(matchesNestedDateWindow(d(11, 10), d(11, 10), d(11, 11), d(11, 11)))
        assertFalse(matchesNestedDateWindow(d(11, 10), d(11, 10), d(11, 9), d(11, 9)))
        // Open-ended sides.
        assertTrue(matchesNestedDateWindow(d(11, 10), null, d(11, 10), d(11, 10)))
        assertFalse(matchesNestedDateWindow(d(11, 10), null, d(11, 9), d(11, 9)))
        assertTrue(matchesNestedDateWindow(null, d(11, 10), d(11, 10), d(11, 10)))
        assertFalse(matchesNestedDateWindow(null, d(11, 10), d(11, 11), d(11, 11)))
        // No dates at all.
        assertFalse(matchesNestedDateWindow(null, null, d(11, 1), d(11, 30)))
    }

    @Test
    fun matchKeepsUndatedDescendantsVisible() {
        val grandchild = NestedItemNode(item = item("grandchild", parentId = "child"))
        val child = NestedItemNode(
            item = item("child", parentId = "parent"),
            children = listOf(grandchild)
        )
        val parent = NestedItemNode(
            item = item("parent", start = d(11, 10)),
            children = listOf(child)
        )

        val filtered = filterNestedTree(listOf(parent), start = d(11, 10), end = d(11, 10))

        assertEquals(listOf("parent"), ids(filtered))
        assertEquals(listOf("child"), ids(filtered.single().children))
        assertEquals(listOf("grandchild"), ids(filtered.single().children.single().children))
    }

    @Test
    fun matchKeepsDatedDescendantsOutsideTheWindow() {
        val child = NestedItemNode(
            item = item("child", parentId = "parent", start = d(11, 20), end = d(11, 21))
        )
        val parent = NestedItemNode(
            item = item("parent", start = d(11, 10)),
            children = listOf(child)
        )

        val filtered = filterNestedTree(listOf(parent), start = d(11, 10), end = d(11, 10))

        assertEquals(listOf("parent"), ids(filtered))
        assertEquals(listOf("child"), ids(filtered.single().children))
    }

    @Test
    fun undatedAncestorStaysAsContext() {
        val child = NestedItemNode(
            item = item("child", parentId = "parent", start = d(11, 10))
        )
        val parent = NestedItemNode(item = item("parent"), children = listOf(child))

        val projection = filterNestedTreeWithContext(
            listOf(parent),
            start = d(11, 10),
            end = d(11, 10)
        )

        assertEquals(listOf("parent"), ids(projection.roots))
        assertEquals(listOf("child"), ids(projection.roots.single().children))
        assertEquals(listOf("parent"), projection.contextIds)
    }

    @Test
    fun undatedSiblingsAreHiddenWhenAWindowIsActive() {
        val roots = listOf(
            NestedItemNode(item = item("dated", start = d(11, 10))),
            NestedItemNode(item = item("undated"))
        )

        assertEquals(
            listOf("dated"),
            ids(filterNestedTree(roots, start = d(11, 10), end = d(11, 10)))
        )
    }

    @Test
    fun noMatchesProducesAnEmptyProjection() {
        val roots = listOf(NestedItemNode(item = item("a", start = d(11, 10))))

        val projection = filterNestedTreeWithContext(
            roots,
            start = d(11, 1),
            end = d(11, 5)
        )

        assertTrue(projection.roots.isEmpty())
        assertTrue(projection.contextIds.isEmpty())
    }

    @Test
    fun dateCriterionNeedsBothWindowBounds() {
        val roots = listOf(NestedItemNode(item = item("a", end = d(11, 1))))

        // A half-open window is not a calendar period; the date criterion stays
        // inactive so this documents the contract rather than silently pruning.
        assertEquals(listOf("a"), ids(filterNestedTree(roots, start = d(11, 20))))
    }

    @Test
    fun dateWindowCombinesWithQuery() {
        val roots = listOf(
            NestedItemNode(item = item("alpha", start = d(11, 10), text = "alpha")),
            NestedItemNode(item = item("beta", start = d(11, 10), text = "beta")),
            NestedItemNode(item = item("old-alpha", end = d(11, 9), text = "alpha"))
        )

        val filtered = filterNestedTree(
            roots,
            start = d(11, 10),
            end = d(11, 10),
            query = "alpha"
        )

        assertEquals(listOf("alpha"), ids(filtered))
    }

    @Test
    fun presetsResolveCalendarWindows() {
        val today = LocalDate(2026, 11, 14) // Saturday

        assertEquals(NestedDateWindow(d(11, 14), d(11, 14)), NestedDatePreset.Today.window(today))
        assertEquals(NestedDateWindow(d(11, 13), d(11, 13)), NestedDatePreset.Yesterday.window(today))
        assertEquals(NestedDateWindow(d(11, 15), d(11, 15)), NestedDatePreset.Tomorrow.window(today))
        assertEquals(NestedDateWindow(d(11, 9), d(11, 15)), NestedDatePreset.ThisWeek.window(today))
        assertEquals(NestedDateWindow(d(11, 16), d(11, 22)), NestedDatePreset.NextWeek.window(today))
        assertEquals(NestedDateWindow(d(11, 1), d(11, 30)), NestedDatePreset.ThisMonth.window(today))
        assertEquals(NestedDateWindow(d(12, 1), d(12, 31)), NestedDatePreset.NextMonth.window(today))
        assertEquals(
            NestedDateWindow(LocalDate(2027, 1, 1), LocalDate(2027, 1, 31)),
            NestedDatePreset.NextMonth.window(LocalDate(2026, 12, 20))
        )
    }

    @Test
    fun presetsHandleMonthEndAndLeapDay() {
        val jan31 = LocalDate(2026, 1, 31) // Saturday

        assertEquals(NestedDateWindow(jan31, jan31), NestedDatePreset.Today.window(jan31))
        assertEquals(
            NestedDateWindow(LocalDate(2026, 2, 1), LocalDate(2026, 2, 1)),
            NestedDatePreset.Tomorrow.window(jan31)
        )
        assertEquals(
            NestedDateWindow(LocalDate(2026, 1, 1), jan31),
            NestedDatePreset.ThisMonth.window(jan31)
        )
        assertEquals(
            NestedDateWindow(LocalDate(2026, 2, 2), LocalDate(2026, 2, 8)),
            NestedDatePreset.NextWeek.window(jan31)
        )
        assertEquals(
            NestedDateWindow(LocalDate(2026, 2, 1), LocalDate(2026, 2, 28)),
            NestedDatePreset.NextMonth.window(jan31)
        )

        val leapDay = LocalDate(2024, 2, 29)
        assertEquals(
            NestedDateWindow(LocalDate(2024, 2, 1), leapDay),
            NestedDatePreset.ThisMonth.window(leapDay)
        )
        assertEquals(
            NestedDateWindow(LocalDate(2024, 3, 1), LocalDate(2024, 3, 1)),
            NestedDatePreset.Tomorrow.window(leapDay)
        )
    }

    @Test
    fun presetMatchesFocusByResolvedWindow() {
        val today = LocalDate(2026, 11, 14)

        assertTrue(NestedDatePreset.ThisWeek.matchesFocus(FocusPeriod(Period.Week, today), today))
        // Any anchor inside the same week is the same window.
        assertTrue(
            NestedDatePreset.ThisWeek.matchesFocus(
                FocusPeriod(Period.Week, today.plus(1, DateTimeUnit.DAY)),
                today
            )
        )
        assertTrue(
            NestedDatePreset.Yesterday.matchesFocus(
                FocusPeriod(Period.Day, today).shift(-1),
                today
            )
        )
        assertFalse(NestedDatePreset.ThisWeek.matchesFocus(FocusPeriod(Period.Day, today), today))
        assertFalse(NestedDatePreset.ThisWeek.matchesFocus(null, today))
    }

    @Test
    fun presetWindowSelectsTheExpectedNodes() {
        val today = LocalDate(2026, 11, 14)
        val window = NestedDatePreset.NextWeek.window(today)
        val roots = listOf(
            NestedItemNode(item = item("this-week", start = d(11, 9), end = d(11, 15))),
            NestedItemNode(item = item("next-week", start = d(11, 16), end = d(11, 22))),
            NestedItemNode(item = item("undated"))
        )

        val filtered = filterNestedTree(roots, start = window.start, end = window.endInclusive)

        assertEquals(listOf("next-week"), ids(filtered))
    }
}
