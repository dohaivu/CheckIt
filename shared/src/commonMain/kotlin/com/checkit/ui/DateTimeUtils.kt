package com.checkit.ui

import kotlinx.datetime.LocalDate

internal fun Int.shortcutDurationLabel(): String =
    when {
        this < 60 -> "${this}m"
        this % 60 == 0 -> "${this / 60}h"
        else -> "${this / 60}h ${this % 60}m"
    }

internal fun validTimeRangeEnd(startTime: Int?, endTime: Int?): Int? =
    when {
        startTime == null -> null
        endTime == null -> null
        startTime > endTime -> null
        else -> endTime
    }

internal fun duration(startTimeMinutes: Int?, endTimeMinutes: Int?): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
    return if (end >= start) {
        end - start
    } else {
        MinutesPerDay - start + end
    }
}

const val HoursPerDay = 24
const val MinutesPerDay = 24 * 60
val TimeRangeShortcutDurations = listOf(30, 60, 120)
