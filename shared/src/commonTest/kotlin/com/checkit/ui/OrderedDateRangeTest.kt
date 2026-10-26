package com.checkit.ui

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Date dialog ordering rule: start must not be after end. Instead of blocking
 * the pick, the conflicting opposite edge is cleared so the latest pick wins.
 */
class OrderedDateRangeTest {

    private val start = LocalDate(2026, 10, 20)
    private val end = LocalDate(2026, 10, 26)

    @Test
    fun pickingStartAfterEndDropsTheEnd() {
        val (newStart, newEnd) = orderedRangeForStart(LocalDate(2026, 11, 3), end)
        assertEquals(LocalDate(2026, 11, 3), newStart)
        assertNull(newEnd)
    }

    @Test
    fun pickingStartBeforeEndKeepsBoth() {
        val (newStart, newEnd) = orderedRangeForStart(start, end)
        assertEquals(start, newStart)
        assertEquals(end, newEnd)
    }

    @Test
    fun pickingStartOnEndKeepsBoth() {
        val (newStart, newEnd) = orderedRangeForStart(end, end)
        assertEquals(end, newStart)
        assertEquals(end, newEnd)
    }

    @Test
    fun pickingStartWithoutEndKeepsStart() {
        val (newStart, newEnd) = orderedRangeForStart(start, null)
        assertEquals(start, newStart)
        assertNull(newEnd)
    }

    @Test
    fun clearingStartKeepsEnd() {
        val (newStart, newEnd) = orderedRangeForStart(null, end)
        assertNull(newStart)
        assertEquals(end, newEnd)
    }

    @Test
    fun pickingEndBeforeStartDropsTheStart() {
        val (newStart, newEnd) = orderedRangeForEnd(start, LocalDate(2026, 10, 1))
        assertNull(newStart)
        assertEquals(LocalDate(2026, 10, 1), newEnd)
    }

    @Test
    fun pickingEndAfterStartKeepsBoth() {
        val (newStart, newEnd) = orderedRangeForEnd(start, end)
        assertEquals(start, newStart)
        assertEquals(end, newEnd)
    }

    @Test
    fun pickingEndOnStartKeepsBoth() {
        val (newStart, newEnd) = orderedRangeForEnd(start, start)
        assertEquals(start, newStart)
        assertEquals(start, newEnd)
    }

    @Test
    fun pickingEndWithoutStartKeepsEnd() {
        val (newStart, newEnd) = orderedRangeForEnd(null, end)
        assertNull(newStart)
        assertEquals(end, newEnd)
    }
}
