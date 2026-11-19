package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Rolling attention presets. Unlike [NestedDatePreset] these are not calendar
 * periods, so they cannot be represented as a [FocusPeriod] and have no
 * previous/next navigation. "Last n days" windows include today. [Overdue] is
 * a predicate rather than a window: its end date is stored as a deadline, so
 * overlap with a closed window would also match ongoing work.
 */
enum class NestedRollingPreset(val label: String) {
    Last7Days(label = "Last 7 days"),
    Next7Days(label = "Next 7 days"),
    Last30Days(label = "Last 30 days"),
    Overdue(label = "Overdue");

    /** Closed window, or null for predicate presets like [Overdue]. */
    fun window(today: LocalDate): NestedDateWindow? = when (this) {
        Last7Days -> NestedDateWindow(today.minus(6, DateTimeUnit.DAY), today)
        Next7Days -> NestedDateWindow(today, today.plus(6, DateTimeUnit.DAY))
        Last30Days -> NestedDateWindow(today.minus(29, DateTimeUnit.DAY), today)
        Overdue -> null
    }
}

/**
 * Active date criterion for the outline.
 *
 * [Focus] is a navigable calendar period; [Rolling] is a local attention
 * window that slides with the clock. Keeping them in one sealed type prevents
 * the two modes from being active at the same time.
 */
sealed interface NestedDateFilter {
    data class Focus(val focus: FocusPeriod) : NestedDateFilter
    data class Rolling(val preset: NestedRollingPreset) : NestedDateFilter

    /** Closed date window, or null when the criterion is a predicate. */
    fun window(today: LocalDate): NestedDateWindow? = when (this) {
        is Focus -> focus.asDateWindow()
        is Rolling -> preset.window(today)
    }

    /** The navigable calendar focus, or null for a rolling window. */
    val calendarFocus: FocusPeriod? get() = (this as? Focus)?.focus
}

/**
 * Exclusive cutoff for [NestedRollingPreset.Overdue]: an item is overdue when
 * its inclusive end date is before [today]. Other criteria return null
 * because their [NestedDateFilter.window] fully describes them.
 */
fun NestedDateFilter.overdueBefore(today: LocalDate): LocalDate? =
    if (this is NestedDateFilter.Rolling && preset == NestedRollingPreset.Overdue) today else null
