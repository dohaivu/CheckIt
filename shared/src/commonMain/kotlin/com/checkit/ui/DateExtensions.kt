package com.checkit.ui

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

internal fun LocalDate.toUtcStartMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun Long.toUtcLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

internal fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).date

internal fun LocalDate.isSameMonth(month: LocalDate): Boolean =
    year == month.year && this.month == month.month

internal fun LocalDate.compactDateWithDayName(): String =
    "${month.number}.${day} (${dayOfWeek.shortName()})"

internal fun Map<LocalDate, Int>.firstHeaderIndexForMonth(month: LocalDate): Int? =
    entries
        .filter { (date, _) -> date.isSameMonth(month) }
        .maxByOrNull { (date, _) -> date }
        ?.value

internal fun Month.shortName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

internal fun DayOfWeek.shortName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

internal fun LocalDate.shortMonthName(): String =
    month.shortName()

internal fun LocalDate.monthAxisLabel(): String {
    val monthLabel = shortMonthName()
    return if (month.number == 1) "$monthLabel $year" else monthLabel
}

fun today(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LocalDate.firstDayOfMonth(): LocalDate = LocalDate(year, month, 1)
