package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Copy-time date offsets: duplicated and pasted rows resolve relative intent
 * once and store absolute dates, preserving open-ended ranges.
 */
class NestedDateOffsetTest {

    private fun item(
        id: String,
        start: LocalDate? = null,
        end: LocalDate? = null,
        position: Int = 0,
        parentId: String? = null
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = id,
        startDate = start,
        endDate = end,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    @Test
    fun shiftMovesBothBounds() {
        val shifted = item("a", LocalDate(2026, 11, 7), LocalDate(2026, 11, 8))
            .shiftedDatesBy(7)

        assertEquals(LocalDate(2026, 11, 14), shifted.startDate)
        assertEquals(LocalDate(2026, 11, 15), shifted.endDate)
    }

    @Test
    fun shiftPreservesOpenEndedRanges() {
        val startOnly = item("a", start = LocalDate(2026, 11, 7)).shiftedDatesBy(1)
        assertEquals(LocalDate(2026, 11, 8), startOnly.startDate)
        assertNull(startOnly.endDate)

        val endOnly = item("b", end = LocalDate(2026, 11, 7)).shiftedDatesBy(1)
        assertNull(endOnly.startDate)
        assertEquals(LocalDate(2026, 11, 8), endOnly.endDate)
    }

    @Test
    fun shiftHandlesMonthEndLeapDayAndYearRollover() {
        val monthEnd = item("a", LocalDate(2026, 1, 31), LocalDate(2026, 1, 31))
            .shiftedDatesBy(1)
        assertEquals(LocalDate(2026, 2, 1), monthEnd.startDate)
        assertEquals(LocalDate(2026, 2, 1), monthEnd.endDate)

        val leap = item("b", LocalDate(2024, 2, 28), LocalDate(2024, 2, 29))
            .shiftedDatesBy(1)
        assertEquals(LocalDate(2024, 2, 29), leap.startDate)
        assertEquals(LocalDate(2024, 3, 1), leap.endDate)

        val year = item("c", LocalDate(2026, 12, 31), LocalDate(2026, 12, 31))
            .shiftedDatesBy(1)
        assertEquals(LocalDate(2027, 1, 1), year.startDate)
        assertEquals(LocalDate(2027, 1, 1), year.endDate)
    }

    @Test
    fun zeroOffsetAndUndatedRowsAreIdentity() {
        val dated = item("a", LocalDate(2026, 11, 7), LocalDate(2026, 11, 8))
        assertEquals(dated, dated.shiftedDatesBy(0))

        val undated = item("b")
        assertEquals(undated, undated.shiftedDatesBy(7))
    }

    @Test
    fun duplicateShiftsDatesAndStillResetsExecutionState() {
        val source = item("source", LocalDate(2026, 11, 7), LocalDate(2026, 11, 8))
            .copy(checked = true, actualMinutes = 30)

        val duplicate = assertNotNull(
            planNestedItemDuplicate(
                items = listOf(source),
                itemId = "source",
                newId = "new",
                nowMillis = 99L,
                dateOffsetDays = 7
            )
        )

        assertEquals(LocalDate(2026, 11, 14), duplicate.startDate)
        assertEquals(LocalDate(2026, 11, 15), duplicate.endDate)
        assertEquals(false, duplicate.checked)
        assertEquals(0, duplicate.actualMinutes)
    }

    @Test
    fun duplicateWithoutDatesClearsBothBounds() {
        val source = item("source", LocalDate(2026, 11, 7), LocalDate(2026, 11, 8))

        val duplicate = assertNotNull(
            planNestedItemDuplicate(
                items = listOf(source),
                itemId = "source",
                newId = "new",
                nowMillis = 99L,
                dateOffsetDays = 7,
                clearDates = true
            )
        )

        assertNull(duplicate.startDate)
        assertNull(duplicate.endDate)
    }

    @Test
    fun pastedSubtreeShiftsEveryRowAndKeepsNulls() {
        val child = NestedItemNode(
            item = item("child", start = LocalDate(2026, 11, 9), parentId = "root")
        )
        val root = NestedItemNode(
            item = item("root", start = LocalDate(2026, 11, 7), end = LocalDate(2026, 11, 8)),
            children = listOf(child)
        )
        val clip = NestedSubtreeClip(roots = listOf(root), sourceDocumentId = "doc-1")
        var nextId = 0

        val pasted = planNestedSubtreePaste(
            clip = clip,
            documentId = "doc-2",
            parentId = null,
            startPosition = 0,
            nowMillis = 99L,
            newId = { "new-${nextId++}" },
            dateOffsetDays = 7
        )

        assertEquals(2, pasted.size)
        val pastedRoot = pasted.first { it.text == "root" }
        val pastedChild = pasted.first { it.text == "child" }
        assertEquals(LocalDate(2026, 11, 14), pastedRoot.startDate)
        assertEquals(LocalDate(2026, 11, 15), pastedRoot.endDate)
        assertEquals(LocalDate(2026, 11, 16), pastedChild.startDate)
        assertNull(pastedChild.endDate)
    }
}
