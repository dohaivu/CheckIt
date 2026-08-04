package com.checkit.domain

import com.checkit.ui.currentMyDayTimeMinutes

fun nextAvailableTimeRange(
    preferredStartTimeMinutes: Int,
    durationMinutes: Int,
    items: List<DailyPlanItem>,
    earliestStartTimeMinutes: Int = 0
): Pair<Int?, Int?> {
    val duration = durationMinutes.coerceIn(MinimumPlanDurationMinutes, MyDayMinutesPerDay)
    val lastStart = MyDayMinutesPerDay - duration
    val earliestStart = earliestStartTimeMinutes.coerceIn(0, lastStart)
    val preferredStart = preferredStartTimeMinutes.coerceIn(earliestStart, lastStart)
    val occupiedRanges = items
        .mapNotNull { it.occupiedTimeRange() }
        .sortedBy { it.first }

    findAvailableStart(preferredStart, duration, occupiedRanges)?.let { start ->
        return start to start + duration
    }
    findAvailableStart(earliestStart, duration, occupiedRanges)?.let { start ->
        return start to start + duration
    }
    return null to null
}

fun DailyPlanItem.occupiedTimeRange(): Pair<Int, Int>? {
    val start = startTimeMinutes ?: return null
    val end = (endTimeMinutes ?: (start + DefaultTaskDurationMinutes)).coerceAtMost(MyDayMinutesPerDay)
    return if (end > start) start.coerceIn(0, MyDayMinutesPerDay) to end else null
}

private fun findAvailableStart(
    preferredStart: Int,
    durationMinutes: Int,
    occupiedRanges: List<Pair<Int, Int>>
): Int? {
    val lastStart = MyDayMinutesPerDay - durationMinutes
    var candidate = preferredStart.coerceIn(0, lastStart)
    occupiedRanges.forEach { (occupiedStart, occupiedEnd) ->
        if (candidate + durationMinutes <= occupiedStart) return candidate
        if (candidate < occupiedEnd && candidate + durationMinutes > occupiedStart) {
            candidate = occupiedEnd.coerceAtMost(lastStart)
        }
    }
    return candidate.takeIf { candidate + durationMinutes <= MyDayMinutesPerDay && !it.overlapsAny(durationMinutes, occupiedRanges) }
}

private fun Int.overlapsAny(durationMinutes: Int, occupiedRanges: List<Pair<Int, Int>>): Boolean =
    occupiedRanges.any { (occupiedStart, occupiedEnd) ->
        this < occupiedEnd && this + durationMinutes > occupiedStart
    }

const val DefaultTaskDurationMinutes = 45
const val MinimumPlanDurationMinutes = 15
const val MyDayMinutesPerDay = 24 * 60
