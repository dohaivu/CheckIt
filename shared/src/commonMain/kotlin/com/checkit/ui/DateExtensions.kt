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

fun Long.toTimeMinutes(): Int {
    val time = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).time
    return time.hour * 60 + time.minute
}

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

/**
 * Compact metadata label for a nested item's dates, mirroring the macOS row:
 * the two ends are independent, so a lone start reads as "From Oct 20" and a
 * lone end as "Until Nov 3". Ranges spell out both dates; weekday-relative
 * wording ("Wed") is reserved for a single day, where it is unambiguous.
 */
internal class NestedDateRangeLabel(
    val text: String,
    val isOverdue: Boolean
)

internal fun nestedDateRangeLabel(
    startDate: LocalDate?,
    endDate: LocalDate?,
    today: LocalDate = today()
): NestedDateRangeLabel? {
    if (startDate == null && endDate == null) return null
    val includeYear = (startDate != null && startDate.year != today.year) ||
        (endDate != null && endDate.year != today.year)
    val text = when {
        startDate != null && endDate != null && startDate == endDate ->
            startDate.compact(today)
        startDate != null && endDate != null ->
            "${startDate.metadataDate(includeYear)} → ${endDate.metadataDate(includeYear)}"
        startDate != null -> "From ${startDate.metadataDate(includeYear)}"
        endDate != null -> "Until ${endDate.metadataDate(includeYear)}"
        else -> return null
    }
    // The end date is inclusive: only a date before today is overdue.
    return NestedDateRangeLabel(
        text = text,
        isOverdue = endDate != null && endDate < today
    )
}

/** Calendar date without relative wording, so range endpoints stay explicit. */
private fun LocalDate.metadataDate(includeYear: Boolean): String =
    if (includeYear) "${shortMonthName()} $day, $year" else "${shortMonthName()} $day"

/**
 * Resolves the pair after the user picks a [start] date. An inverted range is
 * not allowed, so a start after the current [end] clears that end instead;
 * equal dates stay valid because the end date is inclusive.
 */
internal fun orderedRangeForStart(
    start: LocalDate?,
    end: LocalDate?
): Pair<LocalDate?, LocalDate?> =
    if (start != null && end != null && start > end) start to null else start to end

/**
 * Mirror of [orderedRangeForStart] for a newly picked [end]: an end before
 * the start clears the start, so the latest pick always survives.
 */
internal fun orderedRangeForEnd(
    start: LocalDate?,
    end: LocalDate?
): Pair<LocalDate?, LocalDate?> =
    if (start != null && end != null && end < start) null to end else start to end

internal fun LocalDate.monthAxisLabel(): String {
    val monthLabel = shortMonthName()
    return if (month.number == 1) "$monthLabel $year" else monthLabel
}

fun today(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun currentMyDayTimeMinutes(): Int {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return now.hour * 60 + now.minute
}

fun LocalDate.firstDayOfMonth(): LocalDate = LocalDate(year, month, 1)
