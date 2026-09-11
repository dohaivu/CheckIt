package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickNoteDisplayTextTest {
    private val now = 1_700_000_000_000L

    @Test
    fun reminderUnderAnHourShowsMinutes() {
        assertEquals("in 15m", QuickNoteDisplayText.reminderText(now + 15 * 60_000L, now))
    }

    @Test
    fun reminderRoundsUpToAtLeastOneMinute() {
        assertEquals("in 1m", QuickNoteDisplayText.reminderText(now + 1_000L, now))
    }

    @Test
    fun reminderAtOrPastDueShowsOneMinute() {
        assertEquals("in 1m", QuickNoteDisplayText.reminderText(now, now))
        assertEquals("in 1m", QuickNoteDisplayText.reminderText(now - 60_000L, now))
    }

    @Test
    fun reminderOverAnHourShowsHours() {
        assertEquals("in 2h", QuickNoteDisplayText.reminderText(now + 150 * 60_000L, now))
    }

    @Test
    fun remainingNullShowsEmpty() {
        assertEquals("", QuickNoteDisplayText.remainingText(null, now))
    }

    @Test
    fun remainingOverAnHourShowsHours() {
        assertEquals("23h", QuickNoteDisplayText.remainingText(now + 23 * 3_600_000L, now))
    }

    @Test
    fun remainingUnderAnHourShowsMinutes() {
        assertEquals("59m", QuickNoteDisplayText.remainingText(now + 59 * 60_000L, now))
    }

    @Test
    fun remainingPastDueShowsOneMinute() {
        assertEquals("1m", QuickNoteDisplayText.remainingText(now - 1_000L, now))
    }
}
