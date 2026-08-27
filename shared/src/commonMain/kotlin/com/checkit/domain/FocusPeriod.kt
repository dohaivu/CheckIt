package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The five zoom levels of time periods used in the app.
 */
enum class Period {
    Year,
    Quarter,
    Month,
    Week,
    Day
}

/**
 * Identifies a single period by its zoom level plus any anchor date inside it.
 */
data class FocusPeriod(
    val period: Period,
    val anchorDate: LocalDate
) {
    val start: LocalDate get() = period.startOf(anchorDate)
    val endExclusive: LocalDate get() = period.endExclusive(anchorDate)

    /** Inclusive last day of the period (storage convention). */
    val endInclusive: LocalDate get() = endExclusive.minus(1, DateTimeUnit.DAY)

    val startEpochDays: Int get() = start.toEpochDays().toInt()
    val endInclusiveEpochDays: Int get() = endExclusive.toEpochDays().toInt() - 1

    /** Zoom out one level (Day -> Week -> Month -> Quarter -> Year); stays at Year. */
    fun zoomOut(): FocusPeriod = when (period) {
        Period.Day -> FocusPeriod(Period.Week, anchorDate)
        Period.Week -> FocusPeriod(Period.Month, anchorDate)
        Period.Month -> FocusPeriod(Period.Quarter, anchorDate)
        Period.Quarter -> FocusPeriod(Period.Year, anchorDate)
        Period.Year -> this
    }

    /** Zoom into [to] keeping an anchor; only allows finer periods than current. */
    fun zoomIn(to: Period, anchor: LocalDate = anchorDate): FocusPeriod =
        if (to.ordinal > period.ordinal) FocusPeriod(to, anchor) else this

    fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive

    /** Move to the previous (negative) / next (positive) period of the same type. */
    fun shift(amount: Int): FocusPeriod = FocusPeriod(period, period.move(anchorDate, amount))

    /** The coarser period that contains this one, or null for Year. */
    fun parentPeriod(): Period? = when (period) {
        Period.Year -> null
        Period.Quarter -> Period.Year
        Period.Month -> Period.Quarter
        Period.Week -> Period.Month
        Period.Day -> Period.Week
    }

    /** The finer period nested inside this one, or null for Day. */
    fun childPeriod(): Period? = when (period) {
        Period.Year -> Period.Quarter
        Period.Quarter -> Period.Month
        Period.Month -> Period.Week
        Period.Week -> Period.Day
        Period.Day -> null
    }

    /** Whether this focus completely covers [other]. */
    fun covers(other: FocusPeriod): Boolean = start <= other.start && endExclusive >= other.endExclusive
}

/** Whether this period represents a longer duration than [other]. */
fun Period.isBroaderThan(other: Period): Boolean = this.ordinal < other.ordinal

fun Period.startOf(anchorDate: LocalDate): LocalDate = when (this) {
    Period.Day -> anchorDate
    Period.Week -> anchorDate.minus(anchorDate.dayOfWeek.ordinal, DateTimeUnit.DAY)
    Period.Month -> LocalDate(anchorDate.year, anchorDate.monthNumber, 1)
    Period.Quarter -> LocalDate(anchorDate.year, ((anchorDate.monthNumber - 1) / 3) * 3 + 1, 1)
    Period.Year -> LocalDate(anchorDate.year, 1, 1)
}

fun Period.endExclusive(anchorDate: LocalDate): LocalDate = when (this) {
    Period.Day -> startOf(anchorDate).plus(1, DateTimeUnit.DAY)
    Period.Week -> startOf(anchorDate).plus(7, DateTimeUnit.DAY)
    Period.Month -> startOf(anchorDate).plus(1, DateTimeUnit.MONTH)
    Period.Quarter -> startOf(anchorDate).plus(3, DateTimeUnit.MONTH)
    Period.Year -> startOf(anchorDate).plus(1, DateTimeUnit.YEAR)
}

fun Period.endDateInclusive(anchorDate: LocalDate): LocalDate =
    endExclusive(anchorDate).minus(1, DateTimeUnit.DAY)

fun Period.move(anchorDate: LocalDate, amount: Int): LocalDate = when (this) {
    Period.Day -> anchorDate.plus(amount, DateTimeUnit.DAY)
    Period.Week -> anchorDate.plus(amount * 7, DateTimeUnit.DAY)
    Period.Month -> startOf(anchorDate.plus(amount, DateTimeUnit.MONTH))
    Period.Quarter -> startOf(anchorDate.plus(amount * 3, DateTimeUnit.MONTH))
    Period.Year -> startOf(anchorDate.plus(amount, DateTimeUnit.YEAR))
}
