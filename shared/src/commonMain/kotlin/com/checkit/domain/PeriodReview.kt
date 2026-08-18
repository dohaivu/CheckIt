package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** The four zoom levels of the unified review/reflection system. */
enum class ReviewPeriod {
    Day,
    Week,
    Month,
    Year
}

enum class ReviewSource {
    Auto,
    Manual
}

enum class ReviewStatus {
    Draft,
    Complete
}

/**
 * Canonical navigation currency for the Reflect hub. Identifies a single period
 * by its zoom level plus any anchor date inside it.
 */
data class PeriodFocus(
    val period: ReviewPeriod,
    val anchorDate: LocalDate
) {
    val start: LocalDate get() = period.startOf(anchorDate)
    val endExclusive: LocalDate get() = period.endExclusive(anchorDate)

    /** Zoom out one level (Day -> Week -> Month -> Year); stays at Year. */
    fun zoomOut(): PeriodFocus = when (period) {
        ReviewPeriod.Day -> PeriodFocus(ReviewPeriod.Week, anchorDate)
        ReviewPeriod.Week -> PeriodFocus(ReviewPeriod.Month, anchorDate)
        ReviewPeriod.Month -> PeriodFocus(ReviewPeriod.Year, anchorDate)
        ReviewPeriod.Year -> this
    }

    /** Zoom into [to] keeping an anchor inside the child period. */
    fun zoomIn(to: ReviewPeriod, anchor: LocalDate = anchorDate): PeriodFocus =
        PeriodFocus(to, anchor)
}

fun ReviewPeriod.startOf(anchorDate: LocalDate): LocalDate = when (this) {
    ReviewPeriod.Day -> anchorDate
    ReviewPeriod.Week -> anchorDate.firstDayOfWeek()
    ReviewPeriod.Month -> anchorDate.firstDayOfMonth()
    ReviewPeriod.Year -> LocalDate(anchorDate.year, 1, 1)
}

fun ReviewPeriod.endExclusive(anchorDate: LocalDate): LocalDate = when (this) {
    ReviewPeriod.Day -> startOf(anchorDate).plus(1, DateTimeUnit.DAY)
    ReviewPeriod.Week -> startOf(anchorDate).plus(7, DateTimeUnit.DAY)
    ReviewPeriod.Month -> startOf(anchorDate).plus(1, DateTimeUnit.MONTH)
    ReviewPeriod.Year -> startOf(anchorDate).plus(1, DateTimeUnit.YEAR)
}

fun ReviewPeriod.move(anchorDate: LocalDate, amount: Int): LocalDate = when (this) {
    ReviewPeriod.Day -> anchorDate.plus(amount, DateTimeUnit.DAY)
    ReviewPeriod.Week -> anchorDate.plus(amount * 7, DateTimeUnit.DAY)
    ReviewPeriod.Month -> anchorDate.plus(amount, DateTimeUnit.MONTH).firstDayOfMonth()
    ReviewPeriod.Year -> anchorDate.plus(amount, DateTimeUnit.YEAR)
}

/**
 * A single narrative document for one period (day | week | month | year).
 * At most one record per [period] + [periodStartEpochDays] (unique index).
 */
data class PeriodReview(
    val id: Long = 0L,
    val period: ReviewPeriod = ReviewPeriod.Day,
    val periodStartEpochDays: Int,
    val periodEndEpochDays: Int,
    val content: String = "",
    val highlightsJson: String? = null,
    val intentNext: String? = null,
    val source: ReviewSource = ReviewSource.Manual,
    val status: ReviewStatus = ReviewStatus.Draft,
    val completedAtMillis: Long? = null,
    val generatedAtMillis: Long? = null,
    val editedAtMillis: Long? = null,
    val statsJson: String? = null
) {
    val periodStartDate: LocalDate get() = LocalDate.fromEpochDays(periodStartEpochDays)
    val isComplete: Boolean get() = status == ReviewStatus.Complete
}

private fun LocalDate.firstDayOfWeek(): LocalDate =
    minus(dayOfWeek.ordinal, DateTimeUnit.DAY)

private fun LocalDate.firstDayOfMonth(): LocalDate =
    LocalDate(year, monthNumber, 1)
