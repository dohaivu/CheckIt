package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.ui.localizedMonthTitle
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.localizedShortMonthName
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.*

enum class ReportPeriod {
    Daily,
    Week,
    Month,
    Annual
}

@Composable
internal fun ReportPeriodSwitcher(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit,
    periods: List<ReportPeriod> = ReportPeriod.entries,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(34.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        periods.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        text = period.label(),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                modifier = Modifier.height(32.dp),
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedPeriod == period,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                )
            )
        }
    }
}

@Composable
private fun ReportPeriod.label(): String = when (this) {
    ReportPeriod.Daily -> stringResource(Res.string.daily)
    ReportPeriod.Week -> stringResource(Res.string.weekly)
    ReportPeriod.Month -> stringResource(Res.string.monthly)
    ReportPeriod.Annual -> stringResource(Res.string.annual)
}

@Composable
internal fun ReportPeriodHeader(
    selectedPeriod: ReportPeriod,
    selectedDate: LocalDate,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
    periods: List<ReportPeriod> = ReportPeriod.entries,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ReportPeriodSwitcher(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected,
            periods = periods
        )
        when (selectedPeriod) {
            ReportPeriod.Daily -> DayHeader(
                day = selectedDate,
                onPreviousDay = onPreviousPeriod,
                onNextDay = onNextPeriod,
                onCurrentDay = onCurrentPeriod
            )
            ReportPeriod.Week -> WeekHeader(
                week = selectedDate,
                onPreviousWeek = onPreviousPeriod,
                onNextWeek = onNextPeriod,
                onCurrentWeek = onCurrentPeriod
            )
            ReportPeriod.Month -> MonthHeader(
                month = selectedDate,
                onPreviousMonth = onPreviousPeriod,
                onNextMonth = onNextPeriod,
                onCurrentMonth = onCurrentPeriod
            )
            ReportPeriod.Annual -> YearHeader(
                year = selectedDate.year,
                onPreviousYear = onPreviousPeriod,
                onNextYear = onNextPeriod,
                onCurrentYear = onCurrentPeriod
            )
        }
    }
}

@Composable
internal fun DayHeader(
    day: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onCurrentDay: () -> Unit
) {
    PeriodHeader(
        title = day.localizedCompactDateWithDayName(),
        onPrevious = onPreviousDay,
        onNext = onNextDay,
        onCurrentPeriod = onCurrentDay,
        previousContentDescription = stringResource(Res.string.previous_day),
        nextContentDescription = stringResource(Res.string.next_day)
    )
}

@Composable
internal fun WeekHeader(
    week: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit
) {
    val start = week.firstDayOfWeek()
    val end = start.plus(6, DateTimeUnit.DAY)
    PeriodHeader(
        title = weekRangeTitle(start, end),
        onPrevious = onPreviousWeek,
        onNext = onNextWeek,
        onCurrentPeriod = onCurrentWeek,
        previousContentDescription = stringResource(Res.string.previous_week),
        nextContentDescription = stringResource(Res.string.next_week)
    )
}

@Composable
internal fun MonthHeader(
    month: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit
) {
    PeriodHeader(
        title = month.localizedMonthTitle(),
        onPrevious = onPreviousMonth,
        onNext = onNextMonth,
        onCurrentPeriod = onCurrentMonth,
        previousContentDescription = stringResource(Res.string.previous_month),
        nextContentDescription = stringResource(Res.string.next_month)
    )
}

@Composable
internal fun YearHeader(
    year: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onCurrentYear: () -> Unit
) {
    PeriodHeader(
        title = "$year",
        subtitle = stringResource(Res.string.year_range),
        onPrevious = onPreviousYear,
        onNext = onNextYear,
        onCurrentPeriod = onCurrentYear,
        previousContentDescription = stringResource(Res.string.previous_year),
        nextContentDescription = stringResource(Res.string.next_year)
    )
}

@Composable
private fun PeriodHeader(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrentPeriod: () -> Unit,
    previousContentDescription: String,
    nextContentDescription: String,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = previousContentDescription)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .pointerInput(onCurrentPeriod) {
                    detectTapGestures(onDoubleTap = { onCurrentPeriod() })
                }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (subtitle != null) {
                Text(
                    text = "($subtitle)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = nextContentDescription)
        }
    }
}

private fun LocalDate.firstDayOfWeek(): LocalDate =
    minus(dayOfWeek.ordinal, DateTimeUnit.DAY)

@Composable
private fun weekRangeTitle(start: LocalDate, end: LocalDate): String {
    val startLabel = "${start.localizedShortMonthName()} ${start.day}"
    val endLabel = "${end.localizedShortMonthName()} ${end.day}"
    return if (start.year == end.year) {
        "$startLabel - $endLabel, ${end.year}"
    } else {
        "$startLabel, ${start.year} - $endLabel, ${end.year}"
    }
}
