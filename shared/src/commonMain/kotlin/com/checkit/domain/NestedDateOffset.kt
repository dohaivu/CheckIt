package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

/**
 * Shifts both date bounds by [days], preserving open-ended ranges.
 *
 * Duplicating or pasting a template resolves relative intent once, at copy
 * time, and stores absolute dates: the copy never moves again. Calendar math
 * goes through [kotlinx.datetime.LocalDate.plus], so month ends, leap days and
 * year rollovers are handled by the date library.
 */
fun NestedListItem.shiftedDatesBy(days: Int): NestedListItem {
    if (days == 0 || (startDate == null && endDate == null)) return this
    return copy(
        startDate = startDate?.plus(days, DateTimeUnit.DAY),
        endDate = endDate?.plus(days, DateTimeUnit.DAY)
    )
}

/** Clears both bounds; used by the "duplicate without dates" copy variant. */
fun NestedListItem.withoutDates(): NestedListItem =
    if (startDate == null && endDate == null) this else copy(startDate = null, endDate = null)
