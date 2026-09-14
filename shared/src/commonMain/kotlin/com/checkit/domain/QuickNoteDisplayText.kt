package com.checkit.domain

/**
 * Single source for QuickNote list badges shown on Android
 * (`formatReminder`/`formatRemaining` delegate here) and macOS SwiftUI.
 * Pure functions of (timestamp, now) so every platform renders identically.
 */
object QuickNoteDisplayText {
    fun reminderText(remindAt: Long, now: Long): String {
        val minutes = ((remindAt - now).coerceAtLeast(0L)) / 60_000L
        if (minutes < 60) return "in ${minutes.coerceAtLeast(1)}m"
        return "in ${minutes / 60}h"
    }

    fun remainingText(deleteAt: Long?, now: Long): String {
        if (deleteAt == null) return ""
        val remaining = (deleteAt - now).coerceAtLeast(0L)
        val hours = remaining / 3_600_000L
        if (hours >= 1) return "${hours}h"
        return "${(remaining / 60_000L).coerceAtLeast(1L)}m"
    }
}
