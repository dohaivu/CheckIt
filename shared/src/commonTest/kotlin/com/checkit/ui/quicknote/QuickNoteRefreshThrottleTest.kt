package com.checkit.ui.quicknote

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickNoteRefreshThrottleTest {
    private val now = 1_700_000_000_000L

    @Test
    fun forceAlwaysRefreshes() {
        assertTrue(shouldRefreshNotes(now, now, force = true))
    }

    @Test
    fun firstRunAlwaysRefreshes() {
        assertTrue(shouldRefreshNotes(now, 0L, force = false))
    }

    @Test
    fun skipsCallsInsideInterval() {
        val last = now - QUICK_NOTE_REFRESH_MIN_INTERVAL_MILLIS + 60_000L
        assertFalse(shouldRefreshNotes(now, last, force = false))
    }

    @Test
    fun refreshesAfterInterval() {
        val last = now - QUICK_NOTE_REFRESH_MIN_INTERVAL_MILLIS
        assertTrue(shouldRefreshNotes(now, last, force = false))
    }
}
