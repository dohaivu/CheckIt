package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The five zoom levels of time periods used in the app.
 */
enum class PlanPeriod {
    Year,
    Quarter,
    Month,
    Week,
    Day
}

/**
 * Identifies a single period by its zoom level plus any anchor date inside it.
 */
data class PlanFocus(
    val period: PlanPeriod,
    val anchorDate: LocalDate
) {
    val start: LocalDate get() = period.startOf(anchorDate)
    val endExclusive: LocalDate get() = period.endExclusive(anchorDate)

    /** Inclusive last day of the period (storage convention). */
    val endInclusive: LocalDate get() = endExclusive.minus(1, DateTimeUnit.DAY)

    val startEpochDays: Int get() = start.toEpochDays().toInt()
    val endInclusiveEpochDays: Int get() = endExclusive.toEpochDays().toInt() - 1

    /** Zoom out one level (Day -> Week -> Month -> Quarter -> Year); stays at Year. */
    fun zoomOut(): PlanFocus = when (period) {
        PlanPeriod.Day -> PlanFocus(PlanPeriod.Week, anchorDate)
        PlanPeriod.Week -> PlanFocus(PlanPeriod.Month, anchorDate)
        PlanPeriod.Month -> PlanFocus(PlanPeriod.Quarter, anchorDate)
        PlanPeriod.Quarter -> PlanFocus(PlanPeriod.Year, anchorDate)
        PlanPeriod.Year -> this
    }

    /** Zoom into [to] keeping an anchor; only allows finer periods than current. */
    fun zoomIn(to: PlanPeriod, anchor: LocalDate = anchorDate): PlanFocus =
        if (to.ordinal > period.ordinal) PlanFocus(to, anchor) else this

    fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive

    /** Move to the previous (negative) / next (positive) period of the same type. */
    fun shift(amount: Int): PlanFocus = PlanFocus(period, period.move(anchorDate, amount))

    /** The coarser period that contains this one, or null for Year. */
    fun parentPeriod(): PlanPeriod? = when (period) {
        PlanPeriod.Year -> null
        PlanPeriod.Quarter -> PlanPeriod.Year
        PlanPeriod.Month -> PlanPeriod.Quarter
        PlanPeriod.Week -> PlanPeriod.Month
        PlanPeriod.Day -> PlanPeriod.Week
    }

    /** The finer period nested inside this one, or null for Day. */
    fun childPeriod(): PlanPeriod? = when (period) {
        PlanPeriod.Year -> PlanPeriod.Quarter
        PlanPeriod.Quarter -> PlanPeriod.Month
        PlanPeriod.Month -> PlanPeriod.Week
        PlanPeriod.Week -> PlanPeriod.Day
        PlanPeriod.Day -> null
    }
}

fun PlanPeriod.startOf(anchorDate: LocalDate): LocalDate = when (this) {
    PlanPeriod.Day -> anchorDate
    PlanPeriod.Week -> anchorDate.minus(anchorDate.dayOfWeek.ordinal, DateTimeUnit.DAY)
    PlanPeriod.Month -> LocalDate(anchorDate.year, anchorDate.monthNumber, 1)
    PlanPeriod.Quarter -> LocalDate(anchorDate.year, ((anchorDate.monthNumber - 1) / 3) * 3 + 1, 1)
    PlanPeriod.Year -> LocalDate(anchorDate.year, 1, 1)
}

fun PlanPeriod.endExclusive(anchorDate: LocalDate): LocalDate = when (this) {
    PlanPeriod.Day -> startOf(anchorDate).plus(1, DateTimeUnit.DAY)
    PlanPeriod.Week -> startOf(anchorDate).plus(7, DateTimeUnit.DAY)
    PlanPeriod.Month -> startOf(anchorDate).plus(1, DateTimeUnit.MONTH)
    PlanPeriod.Quarter -> startOf(anchorDate).plus(3, DateTimeUnit.MONTH)
    PlanPeriod.Year -> startOf(anchorDate).plus(1, DateTimeUnit.YEAR)
}

fun PlanPeriod.move(anchorDate: LocalDate, amount: Int): LocalDate = when (this) {
    PlanPeriod.Day -> anchorDate.plus(amount, DateTimeUnit.DAY)
    PlanPeriod.Week -> anchorDate.plus(amount * 7, DateTimeUnit.DAY)
    PlanPeriod.Month -> startOf(anchorDate.plus(amount, DateTimeUnit.MONTH))
    PlanPeriod.Quarter -> startOf(anchorDate.plus(amount * 3, DateTimeUnit.MONTH))
    PlanPeriod.Year -> startOf(anchorDate.plus(amount, DateTimeUnit.YEAR))
}
