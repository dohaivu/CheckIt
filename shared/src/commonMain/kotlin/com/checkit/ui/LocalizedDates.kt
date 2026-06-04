package com.checkit.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.*

@Composable
internal fun LocalDate.localizedCompactDateWithDayName(): String =
    "${month.number}.$day (${dayOfWeek.localizedShortName()})"

@Composable
internal fun LocalDate.localizedShortMonthName(): String =
    month.localizedShortName()

@Composable
internal fun LocalDate.localizedMonthTitle(): String =
    "${month.localizedName()} $year"

@Composable
internal fun localizedMonthName(monthNumber: Int): String = when (monthNumber) {
    1 -> stringResource(Res.string.month_january)
    2 -> stringResource(Res.string.month_february)
    3 -> stringResource(Res.string.month_march)
    4 -> stringResource(Res.string.month_april)
    5 -> stringResource(Res.string.month_may)
    6 -> stringResource(Res.string.month_june)
    7 -> stringResource(Res.string.month_july)
    8 -> stringResource(Res.string.month_august)
    9 -> stringResource(Res.string.month_september)
    10 -> stringResource(Res.string.month_october)
    11 -> stringResource(Res.string.month_november)
    12 -> stringResource(Res.string.month_december)
    else -> monthNumber.toString()
}

@Composable
private fun Month.localizedName(): String = when (this) {
    Month.JANUARY -> stringResource(Res.string.month_january)
    Month.FEBRUARY -> stringResource(Res.string.month_february)
    Month.MARCH -> stringResource(Res.string.month_march)
    Month.APRIL -> stringResource(Res.string.month_april)
    Month.MAY -> stringResource(Res.string.month_may)
    Month.JUNE -> stringResource(Res.string.month_june)
    Month.JULY -> stringResource(Res.string.month_july)
    Month.AUGUST -> stringResource(Res.string.month_august)
    Month.SEPTEMBER -> stringResource(Res.string.month_september)
    Month.OCTOBER -> stringResource(Res.string.month_october)
    Month.NOVEMBER -> stringResource(Res.string.month_november)
    Month.DECEMBER -> stringResource(Res.string.month_december)
}

@Composable
private fun Month.localizedShortName(): String = when (this) {
    Month.JANUARY -> stringResource(Res.string.month_short_january)
    Month.FEBRUARY -> stringResource(Res.string.month_short_february)
    Month.MARCH -> stringResource(Res.string.month_short_march)
    Month.APRIL -> stringResource(Res.string.month_short_april)
    Month.MAY -> stringResource(Res.string.month_short_may)
    Month.JUNE -> stringResource(Res.string.month_short_june)
    Month.JULY -> stringResource(Res.string.month_short_july)
    Month.AUGUST -> stringResource(Res.string.month_short_august)
    Month.SEPTEMBER -> stringResource(Res.string.month_short_september)
    Month.OCTOBER -> stringResource(Res.string.month_short_october)
    Month.NOVEMBER -> stringResource(Res.string.month_short_november)
    Month.DECEMBER -> stringResource(Res.string.month_short_december)
}

@Composable
private fun DayOfWeek.localizedShortName(): String = when (this) {
    DayOfWeek.MONDAY -> stringResource(Res.string.day_short_monday)
    DayOfWeek.TUESDAY -> stringResource(Res.string.day_short_tuesday)
    DayOfWeek.WEDNESDAY -> stringResource(Res.string.day_short_wednesday)
    DayOfWeek.THURSDAY -> stringResource(Res.string.day_short_thursday)
    DayOfWeek.FRIDAY -> stringResource(Res.string.day_short_friday)
    DayOfWeek.SATURDAY -> stringResource(Res.string.day_short_saturday)
    DayOfWeek.SUNDAY -> stringResource(Res.string.day_short_sunday)
}
