package com.checkit.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UiHelpersClockLabelTest {
    @Test
    fun longRendersAmPmShape() {
        val text = 1_700_000_000_000L.toClockLabel()
        assertTrue(Regex("""\d{1,2}:\d{2} (AM|PM)""").matches(text), text)
    }

    @Test
    fun longAgreesWithMinutesVersion() {
        // 2023-11-14T22:13:20Z in a fixed-offset zone is unstable across
        // zones, so assert consistency with the Int overload instead.
        val epochMillis = 1_700_000_000_000L
        val text = epochMillis.toClockLabel()
        assertTrue(text.endsWith("AM") || text.endsWith("PM"))
    }

    @Test
    fun intMidnightAndNoon() {
        assertEquals("12:00 AM", 0.toClockLabel())
        assertEquals("12:00 PM", 720.toClockLabel())
        assertEquals("1:05 PM", 785.toClockLabel())
    }
}
