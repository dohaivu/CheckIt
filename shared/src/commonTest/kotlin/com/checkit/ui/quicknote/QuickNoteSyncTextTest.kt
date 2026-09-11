package com.checkit.ui.quicknote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuickNoteSyncTextTest {
    @Test
    fun nullShowsPlainSynced() {
        assertEquals("Synced", formatSyncedAt(null))
    }

    @Test
    fun timestampShowsSyncedWithTime() {
        val text = formatSyncedAt(1_700_000_000_000L)
        assertTrue(text.startsWith("Synced "))
        assertTrue(Regex("""Synced \d{2}:\d{2}""").matches(text))
    }

    @Test
    fun midnightPadsBothParts() {
        // Shape check only: exact wall time depends on device time zone.
        val text = formatSyncedAt(0L)
        assertTrue(Regex("""Synced \d{2}:\d{2}""").matches(text))
    }
}
