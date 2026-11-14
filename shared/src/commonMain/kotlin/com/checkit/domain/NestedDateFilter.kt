package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Rolling attention windows. Unlike [NestedDatePreset] these are not calendar
 * periods, so they cannot be represented as a [FocusPeriod] and have no
 * previous/next navigation. "Last 7 days" includes today.
 */
enum class NestedRollingPreset(val days: Int, val label: String) {
    Last7Days(days = 7, label = "Last 7 days"),
    Next7Days(days = 7, label = "Next 7 days"),
    Last30Days(days = 30, label = "Last 30 days");

    fun window(today: LocalDate): NestedDateWindow = when (this) {
        Last7Days, Last30Days ->
            NestedDateWindow(today.minus(days - 1, DateTimeUnit.DAY), today)
        Next7Days ->
            NestedDateWindow(today, today.plus(days - 1, DateTimeUnit.DAY))
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

    fun window(today: LocalDate): NestedDateWindow = when (this) {
        is Focus -> focus.asDateWindow()
        is Rolling -> preset.window(today)
    }

    /** The navigable calendar focus, or null for a rolling window. */
    val calendarFocus: FocusPeriod? get() = (this as? Focus)?.focus
}
