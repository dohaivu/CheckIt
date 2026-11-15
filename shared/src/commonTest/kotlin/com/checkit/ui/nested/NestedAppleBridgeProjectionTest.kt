package com.checkit.ui.nested

import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The macOS outline projection takes epoch-day window bounds with the shared
 * `Int.MIN_VALUE` sentinel. These tests pin the sentinel mapping and the
 * preset-name lookup so Swift never has to reimplement period arithmetic.
 */
class NestedAppleBridgeProjectionTest {

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

    private fun node(
        id: String,
        parentId: String? = null,
        start: LocalDate? = null,
        end: LocalDate? = null,
        text: String = id,
        children: List<NestedItemNode> = emptyList()
    ) = NestedItemNode(
        item = item(id, parentId, start, end, text),
        children = children
    )

    private fun ids(nodes: List<NestedItemNode>) = nodes.map { it.item.id }

    private val windowStart = LocalDate(2026, 11, 16)
    private val windowEnd = LocalDate(2026, 11, 22)

    @Test
    fun sentinelBoundsKeepTheProjectionDateFree() {
        val roots = listOf(
            node("undated"),
            node("old", start = LocalDate(2020, 1, 1), end = LocalDate(2020, 1, 2))
        )

        val projection = NestedAppleBridge.filterOutlineProjection(
            roots = roots,
            query = "",
            workingOnly = false,
            windowStartEpochDays = Int.MIN_VALUE,
            windowEndEpochDays = Int.MIN_VALUE
        )

        assertEquals(listOf("undated", "old"), ids(projection.roots))
        assertTrue(projection.contextIds.isEmpty())
    }

    @Test
    fun epochDayWindowUsesOpenEndedMatching() {
        val roots = listOf(
            node("inside", start = windowStart, end = windowStart),
            node("start-only", start = LocalDate(2026, 11, 20)),
            node("end-only", end = LocalDate(2026, 11, 17)),
            node("before", start = LocalDate(2026, 11, 1), end = LocalDate(2026, 11, 15)),
            node("undated")
        )

        val projection = NestedAppleBridge.filterOutlineProjection(
            roots = roots,
            query = "",
            workingOnly = false,
            windowStartEpochDays = windowStart.toEpochDays().toInt(),
            windowEndEpochDays = windowEnd.toEpochDays().toInt()
        )

        assertEquals(listOf("inside", "start-only", "end-only"), ids(projection.roots))
    }

    @Test
    fun matchingParentKeepsUndatedDescendantThroughTheBridge() {
        val child = node("child", parentId = "parent")
        val parent = node("parent", start = windowStart, children = listOf(child))

        val projection = NestedAppleBridge.filterOutlineProjection(
            roots = listOf(parent),
            query = "",
            workingOnly = false,
            windowStartEpochDays = windowStart.toEpochDays().toInt(),
            windowEndEpochDays = windowEnd.toEpochDays().toInt()
        )

        assertEquals(listOf("parent"), ids(projection.roots))
        assertEquals(listOf("child"), ids(projection.roots.single().children))
    }

    @Test
    fun halfOpenBridgeWindowLeavesDateFilteringOff() {
        val roots = listOf(node("undated"))
        val projection = NestedAppleBridge.filterOutlineProjection(
            roots = roots,
            query = "",
            workingOnly = false,
            windowStartEpochDays = windowStart.toEpochDays().toInt(),
            windowEndEpochDays = Int.MIN_VALUE
        )

        assertEquals(listOf("undated"), ids(projection.roots))
    }

    @Test
    fun presetNamesResolveToEpochDayWindows() {
        val today = LocalDate(2026, 11, 14)
        val window = assertNotNull(
            NestedAppleBridge.datePresetWindow(
                presetName = "NextWeek",
                todayEpochDays = today.toEpochDays().toInt()
            )
        )

        assertEquals(windowStart.toEpochDays().toInt(), window.startEpochDays)
        assertEquals(windowEnd.toEpochDays().toInt(), window.endEpochDays)
    }

    @Test
    fun nextMonthPresetResolvesThroughTheBridge() {
        val today = LocalDate(2026, 11, 14)
        val window = assertNotNull(
            NestedAppleBridge.datePresetWindow(
                presetName = "NextMonth",
                todayEpochDays = today.toEpochDays().toInt()
            )
        )

        assertEquals(LocalDate(2026, 12, 1).toEpochDays().toInt(), window.startEpochDays)
        assertEquals(LocalDate(2026, 12, 31).toEpochDays().toInt(), window.endEpochDays)
    }

    @Test
    fun unknownPresetNameReturnsNull() {
        assertNull(
            NestedAppleBridge.datePresetWindow(
                presetName = "LastSevenDays",
                todayEpochDays = 0
            )
        )
    }

    @Test
    fun orderedStartAfterEndClearsTheEndThroughTheBridge() {
        val range = NestedAppleBridge.orderedRangeForStart(
            startEpochDays = windowEnd.toEpochDays().toInt(),
            endEpochDays = windowStart.toEpochDays().toInt()
        )

        assertEquals(windowEnd.toEpochDays().toInt(), range.startEpochDays)
        assertEquals(Int.MIN_VALUE, range.endEpochDays)
    }

    @Test
    fun orderedStartEqualEndKeepsBothSides() {
        val range = NestedAppleBridge.orderedRangeForStart(
            startEpochDays = windowStart.toEpochDays().toInt(),
            endEpochDays = windowStart.toEpochDays().toInt()
        )

        assertEquals(windowStart.toEpochDays().toInt(), range.startEpochDays)
        assertEquals(windowStart.toEpochDays().toInt(), range.endEpochDays)
    }

    @Test
    fun orderedEndBeforeStartClearsTheStartThroughTheBridge() {
        val range = NestedAppleBridge.orderedRangeForEnd(
            startEpochDays = windowEnd.toEpochDays().toInt(),
            endEpochDays = windowStart.toEpochDays().toInt()
        )

        assertEquals(Int.MIN_VALUE, range.startEpochDays)
        assertEquals(windowStart.toEpochDays().toInt(), range.endEpochDays)
    }

    @Test
    fun orderedRangeKeepsUnsetSidesAsSentinel() {
        val startOnly = NestedAppleBridge.orderedRangeForEnd(
            startEpochDays = windowStart.toEpochDays().toInt(),
            endEpochDays = Int.MIN_VALUE
        )
        assertEquals(windowStart.toEpochDays().toInt(), startOnly.startEpochDays)
        assertEquals(Int.MIN_VALUE, startOnly.endEpochDays)

        val endOnly = NestedAppleBridge.orderedRangeForStart(
            startEpochDays = Int.MIN_VALUE,
            endEpochDays = windowEnd.toEpochDays().toInt()
        )
        assertEquals(Int.MIN_VALUE, endOnly.startEpochDays)
        assertEquals(windowEnd.toEpochDays().toInt(), endOnly.endEpochDays)
    }

    @Test
    fun rollingPresetNamesResolveToEpochDayWindows() {
        val today = LocalDate(2026, 11, 14)
        val window = assertNotNull(
            NestedAppleBridge.rollingWindow(
                presetName = "Last7Days",
                todayEpochDays = today.toEpochDays().toInt()
            )
        )

        assertEquals(LocalDate(2026, 11, 8).toEpochDays().toInt(), window.startEpochDays)
        assertEquals(today.toEpochDays().toInt(), window.endEpochDays)
        assertNull(
            NestedAppleBridge.rollingWindow(
                presetName = "ThisWeek",
                todayEpochDays = today.toEpochDays().toInt()
            )
        )
    }
}
