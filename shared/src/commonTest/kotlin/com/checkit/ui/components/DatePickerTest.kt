package com.checkit.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DatePickerTest {
    @Test
    fun validTimeRangeEndClearsEndWhenStartIsMissing() {
        assertNull(validTimeRangeEnd(startTime = null, endTime = 600))
    }

    @Test
    fun validTimeRangeEndClearsEndWhenStartIsAfterEnd() {
        assertNull(validTimeRangeEnd(startTime = 720, endTime = 600))
    }

    @Test
    fun validTimeRangeEndKeepsEndWhenStartMatchesEnd() {
        assertEquals(600, validTimeRangeEnd(startTime = 600, endTime = 600))
    }

    @Test
    fun validTimeRangeEndKeepsEndWhenStartIsBeforeEnd() {
        assertEquals(720, validTimeRangeEnd(startTime = 600, endTime = 720))
    }
}
