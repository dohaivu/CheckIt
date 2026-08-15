package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TwelveWeekTest {

    private val startEpochDays = 20_000

    @Test
    fun endDateIsStartPlusEightyThreeDays() {
        assertEquals(startEpochDays + 83, twelveWeekEndEpochDays(startEpochDays))
        assertEquals(
            TWELVE_WEEK_LENGTH_DAYS,
            twelveWeekEndEpochDays(startEpochDays) - startEpochDays + 1
        )
    }

    @Test
    fun weekZeroIsFirstSevenDays() {
        assertEquals(0, weekIndexFor(startEpochDays, startEpochDays))
        assertEquals(0, weekIndexFor(startEpochDays, startEpochDays + 6))
        assertEquals(startEpochDays..(startEpochDays + 6), weekDateRange(startEpochDays, 0))
    }

    @Test
    fun weekElevenIsLastSevenDays() {
        val lastDay = twelveWeekEndEpochDays(startEpochDays)
        assertEquals(TWELVE_WEEK_LAST_INDEX, weekIndexFor(startEpochDays, lastDay))
        assertEquals(TWELVE_WEEK_LAST_INDEX, weekIndexFor(startEpochDays, lastDay - 6))
        assertEquals((lastDay - 6)..lastDay, weekDateRange(startEpochDays, TWELVE_WEEK_LAST_INDEX))
    }

    @Test
    fun weekIndexIsNullOutsideCycle() {
        assertNull(weekIndexFor(startEpochDays, startEpochDays - 1))
        assertNull(weekIndexFor(startEpochDays, twelveWeekEndEpochDays(startEpochDays) + 1))
    }

    @Test
    fun weekBoundariesAdvanceEverySevenDays() {
        assertEquals(0, weekIndexFor(startEpochDays, startEpochDays + 6))
        assertEquals(1, weekIndexFor(startEpochDays, startEpochDays + 7))
        assertEquals(1, weekIndexFor(startEpochDays, startEpochDays + 13))
        assertEquals(2, weekIndexFor(startEpochDays, startEpochDays + 14))
    }

    @Test
    fun executionScoreAveragesSavedScores() {
        assertNull(executionScore(emptyList()))
        assertEquals(7.0, executionScore(listOf(7)))
        assertEquals(6.0, executionScore(listOf(4, 8)))
    }
}
