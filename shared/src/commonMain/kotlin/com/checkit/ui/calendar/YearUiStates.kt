package com.checkit.ui.calendar

import com.checkit.ui.today
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

/**
 * Minimal year-overview state: only the day rating and done minutes per
 * date. Unlike [CalendarUiState] it carries no plans, tasks, notes or
 * journals, so the year grid renders from two small precomputed maps with
 * O(1) lookups per cell.
 */
data class YearUiState(
    val year: Int = today().year,
    val ratingByDate: Map<LocalDate, Float> = emptyMap(),
    val doneMinutesByDate: Map<LocalDate, Int> = emptyMap(),
    val isLoading: Boolean = true
) {
    val months: List<LocalDate>
        get() = Month.entries.map { LocalDate(year, it, 1) }

    fun ratingFor(date: LocalDate): Float? =
        ratingByDate[date]?.takeIf { it > 0f }

    fun doneMinutesFor(date: LocalDate): Int =
        doneMinutesByDate[date] ?: 0
}
