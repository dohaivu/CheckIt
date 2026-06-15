package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val fillAvailableWidth = periods.size > 2
    Row(
        modifier = if (fillAvailableWidth) {
            modifier
                .fillMaxWidth()
                .height(44.dp)
        } else {
            modifier.height(44.dp)
        },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        periods.forEach { period ->
            val selected = selectedPeriod == period
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .then(
                        if (fillAvailableWidth) {
                            Modifier.weight(1f)
                        } else {
                            Modifier.widthIn(min = 96.dp)
                        }
                    )
                    .clip(CircleShape)
                    .background(
                        brush = if (selected) {
                            Brush.horizontalGradient(listOf(ReportHeaderBlue, ReportHeaderPurple))
                        } else {
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) {
                            Color.Transparent
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
                        },
                        shape = CircleShape
                    )
                    .clickable { onPeriodSelected(period) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = previousContentDescription)
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
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (subtitle == null) title else "$title ($subtitle)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = nextContentDescription)
            }
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
