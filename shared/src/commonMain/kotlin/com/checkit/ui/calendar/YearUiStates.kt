package com.checkit.ui.calendar

import com.checkit.ui.today
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.math.floor
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

    /**
     * Monthly rating distribution over rated days (rating > 0), built in a
     * single pass. Levels follow the same round-half-up buckets as
     * [RatingBarDefaults.getRatingColor]. Callers rendering all 12 headers
     * should read this once (e.g. via `remember(state)`) instead of calling
     * [ratingStatFor] per month.
     */
    val ratingStatsByMonth: Map<Month, MonthRatingStat>
        get() {
            if (ratingByDate.isEmpty()) return emptyMap()
            val counts = mutableMapOf<Month, MutableMap<Int, Int>>()
            for ((date, rating) in ratingByDate) {
                if (rating <= 0f) continue
                val byLevel = counts.getOrPut(date.month) { mutableMapOf() }
                val level = rating.ratingLevel()
                byLevel[level] = (byLevel[level] ?: 0) + 1
            }
            return counts.mapValues { (_, byLevel) ->
                MonthRatingStat(
                    ratedDayCount = byLevel.values.sum(),
                    countByLevel = byLevel.toMap()
                )
            }
        }

    fun ratingStatFor(month: Month): MonthRatingStat =
        ratingStatsByMonth[month] ?: MonthRatingStat()
}

/** Rating performance of one month: rated-day counts per star level (1-5). */
data class MonthRatingStat(
    val ratedDayCount: Int = 0,
    val countByLevel: Map<Int, Int> = emptyMap()
) {
    /** Non-zero levels, highest first, for header display. */
    val levelsDescending: List<Pair<Int, Int>>
        get() = countByLevel.entries
            .sortedByDescending { it.key }
            .map { it.key to it.value }
}

/** Star level (1-5) for a rating, rounding halves up like the rating colors. */
internal fun Float.ratingLevel(): Int =
    floor(this + 0.5f).toInt().coerceIn(1, 5)
