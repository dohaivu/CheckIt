package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.annual
import checkit.shared.generated.resources.daily
import checkit.shared.generated.resources.habits
import checkit.shared.generated.resources.monthly
import checkit.shared.generated.resources.next_day
import checkit.shared.generated.resources.next_month
import checkit.shared.generated.resources.next_week
import checkit.shared.generated.resources.next_year
import checkit.shared.generated.resources.previous_day
import checkit.shared.generated.resources.previous_month
import checkit.shared.generated.resources.previous_week
import checkit.shared.generated.resources.previous_year
import checkit.shared.generated.resources.weekly
import checkit.shared.generated.resources.year_range
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.localizedMonthTitle
import com.checkit.ui.localizedName
import com.checkit.ui.localizedShortMonthName
import com.checkit.ui.localizedShortName
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalIsoWeekDate
import org.jetbrains.compose.resources.stringResource

enum class ReportPeriod {
    Daily,
    Week,
    Month,
    Annual,
    Habit
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
            .fillMaxWidth()
            .height(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        periods.forEach { period ->
            val selected = selectedPeriod == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        brush = if (selected) {
                            Brush.horizontalGradient(listOf(ReportHeaderBlue, ReportHeaderPurple))
                        } else {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onPeriodSelected(period) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ReportPeriod.label(): String = when (this) {
    ReportPeriod.Daily -> stringResource(Res.string.daily)
    ReportPeriod.Week -> stringResource(Res.string.weekly)
    ReportPeriod.Month -> stringResource(Res.string.monthly)
    ReportPeriod.Annual -> stringResource(Res.string.annual)
    ReportPeriod.Habit -> stringResource(Res.string.habits)
}

@Composable
internal fun ReportPeriodHeader(
    selectedPeriod: ReportPeriod,
    selectedDate: LocalDate,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
    onZoomOutTo: (ReportPeriod) -> Unit = {},
    periods: List<ReportPeriod> = ReportPeriod.entries,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ReportPeriodSwitcher(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected,
            periods = periods
        )
        when (selectedPeriod) {
            ReportPeriod.Habit -> Unit
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
        if (selectedPeriod != ReportPeriod.Habit) {
            BreadcrumbRow(
                selectedDate = selectedDate,
                selectedPeriod = selectedPeriod,
                onZoomOutTo = onZoomOutTo
            )
        }
    }
}

@Composable
private fun BreadcrumbRow(
    selectedDate: LocalDate,
    selectedPeriod: ReportPeriod,
    onZoomOutTo: (ReportPeriod) -> Unit
) {
    val weekStart = selectedDate.firstDayOfWeek()
    val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
    val yearLabel = selectedDate.year.toString()
    val monthLabel = selectedDate.month.localizedName()
    val weekLabel = "W${weekStart.toLocalIsoWeekDate().isoWeekNumber} ${weekStart.day} - ${weekEnd.day}"
    val dayLabel = "${selectedDate.day} ${selectedDate.dayOfWeek.localizedShortName()}"
    val crumbs = remember(selectedDate, selectedPeriod) {
        buildList {
            if (selectedPeriod.ordinal <= ReportPeriod.Annual.ordinal) {
                add(PeriodCrumb(ReportPeriod.Annual, yearLabel))
            }
            if (selectedPeriod.ordinal <= ReportPeriod.Month.ordinal) {
                add(PeriodCrumb(ReportPeriod.Month, monthLabel))
            }
            if (selectedPeriod.ordinal <= ReportPeriod.Week.ordinal) {
                add(PeriodCrumb(ReportPeriod.Week, weekLabel))
            }
            if (selectedPeriod.ordinal <= ReportPeriod.Daily.ordinal) {
                add(PeriodCrumb(ReportPeriod.Daily, dayLabel))
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        crumbs.forEachIndexed { index, crumb ->
            if (index > 0) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (crumb.period == selectedPeriod) {
                Text(
                    text = crumb.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = crumb.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onZoomOutTo(crumb.period) }
                )
            }
        }
    }
}

private data class PeriodCrumb(
    val period: ReportPeriod,
    val label: String
)

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
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = previousContentDescription,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(onCurrentPeriod) {
                    detectTapGestures(onDoubleTap = { onCurrentPeriod() })
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = " ($subtitle)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = nextContentDescription,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
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

private val ReportHeaderBlue = Color(0xFF3E72F2)
private val ReportHeaderPurple = Color(0xFF7B5CF0)
