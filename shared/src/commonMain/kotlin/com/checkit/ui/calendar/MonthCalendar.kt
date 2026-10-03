package com.checkit.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.TaskBoard
import com.checkit.ui.components.RatingBarDefaults
import com.checkit.ui.components.RatingStar
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.isSameMonth
import com.checkit.ui.shortName
import com.checkit.ui.tasks.views.ContentContainerAlpha
import com.checkit.ui.toDurationLabel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Composable
internal fun MonthCalendar(
    month: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
    board: TaskBoard,
    state: CalendarUiState
) {
    val colors = rememberCalendarCellColors()
    val dates = remember(month) { calendarGridDates(month) }
    val weeks = remember(dates) { dates.chunked(7) }

    CalendarGrid(colors = colors) {
        weeks.forEach { week ->
            CalendarWeekRow(
                week = week,
                selectedDate = selectedDate,
                colors = colors,
                onDateSelected = onDateSelected,
                onDateDoubleClick = onDateDoubleClick,
                board = board,
                state = state,
                isDateEnabled = { it.isSameMonth(month) }
            )
        }
    }
}

@Composable
internal fun CalendarGrid(
    colors: CalendarCellColors,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            calendarWeekDays.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.shortName(),
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.headerBackground)
                        .border(0.5.dp, colors.outline)
                        .padding(vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = dayOfWeek.headerColor(colors),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        content()
    }
}

@Composable
internal fun CalendarWeekRow(
    week: List<LocalDate>,
    selectedDate: LocalDate,
    colors: CalendarCellColors,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
    board: TaskBoard,
    state: CalendarUiState,
    isDateEnabled: (LocalDate) -> Boolean
) {
    Row(Modifier.fillMaxWidth()) {
        week.forEach { date ->
            val isEnabled = isDateEnabled(date)
            CalendarDayCell(
                date = date,
                isSelected = date == selectedDate,
                isEnabled = isEnabled,
                colors = colors,
                onDateSelected = onDateSelected,
                onDateDoubleClick = onDateDoubleClick,
                markers = if (isEnabled) state.markersForDate(board, date) else CalendarDateMarkers.Empty,
                workMinutes = if (isEnabled) state.doneMinutesForDate(date) else 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isEnabled: Boolean,
    colors: CalendarCellColors,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
    markers: CalendarDateMarkers,
    workMinutes: Int,
    modifier: Modifier = Modifier
) {
    val dayColor = when {
        !isEnabled -> colors.disabledDay
        date.dayOfWeek == DayOfWeek.SATURDAY -> colors.saturday
        date.dayOfWeek == DayOfWeek.SUNDAY -> colors.sunday
        else -> colors.day
    }
    val backgroundColor = when {
        isSelected -> colors.selectedBackground
        isEnabled && workMinutes > 0 -> colors.workHeatBackground(workMinutes)
        else -> colors.defaultBackground
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .border(0.5.dp, colors.outline)
            .background(backgroundColor)
            .combinedClickable(
                enabled = isEnabled,
                onClick = { onDateSelected(date) },
                onDoubleClick = { onDateDoubleClick(date) }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp, bottom = 2.dp, start = 2.dp, end = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = dayColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Start
                )
                if (markers.rating != null && markers.rating > 0) {
                    RatingStar(
                        rating = markers.rating,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            if (isEnabled && (markers.hasMarkers || workMinutes > 0)) {
                DateCellMetadata(
                    markers = markers,
                    workMinutes = workMinutes,
                    colors = colors
                )
            }
        }
    }
}

@Composable
private fun DateCellMetadata(
    markers: CalendarDateMarkers,
    workMinutes: Int,
    colors: CalendarCellColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (markers.hasMarkers) {
            Text(
                text = markers.countLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.markerLabel,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        } else {
            Box(modifier = Modifier.weight(1f))
        }
        if (workMinutes > 0) {
            Text(
                text = workMinutes.toDurationLabel(compact = true),
                style = MaterialTheme.typography.labelSmall,
                color = colors.workLabel,
                maxLines = 1,
                textAlign = TextAlign.End
            )
        }
    }
}

private const val MaxVisibleMarkerCount: Int = 9
private const val HeatmapMaxMinutes: Int = 8 * 60
private const val HeatmapMinAlpha: Float = 0.10f
private const val HeatmapMaxAlpha: Float = 0.42f

@Composable
internal fun rememberCalendarCellColors(): CalendarCellColors {
    val colorScheme = MaterialTheme.colorScheme
    return remember(colorScheme) {
        CalendarCellColors(
            outline = colorScheme.outline.copy(alpha = 0.22f),
            selectedBackground = colorScheme.primaryContainer.copy(alpha = ContentContainerAlpha),
            defaultBackground = colorScheme.surface,
            heatmapHighBackground = colorScheme.primaryContainer,
            workLabel = colorScheme.primary,
            headerBackground = colorScheme.surfaceVariant,
            disabledDay = colorScheme.onSurface.copy(alpha = 0.32f),
            saturday = Color(0xFF249AC8),
            sunday = colorScheme.error,
            day = colorScheme.onSurface,
            headerDay = colorScheme.onSurfaceVariant,
            markerLabel = colorScheme.onSurfaceVariant
        )
    }
}

internal fun calendarGridDates(month: LocalDate): List<LocalDate> {
    val first = month.firstDayOfMonth()
    val last = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    val start = first.minus(daysFromMonday(first.dayOfWeek), DateTimeUnit.DAY)
    val end = last.plus(daysToSunday(last.dayOfWeek), DateTimeUnit.DAY)
    val dates = mutableListOf<LocalDate>()
    var current = start
    while (current <= end) {
        dates += current
        current = current.plus(1, DateTimeUnit.DAY)
    }
    return dates
}

internal fun weekDates(date: LocalDate): List<LocalDate> {
    val start = date.minus(daysFromMonday(date.dayOfWeek), DateTimeUnit.DAY)
    return List(7) { index -> start.plus(index, DateTimeUnit.DAY) }
}

data class CalendarCellColors(
    val outline: Color,
    val selectedBackground: Color,
    val defaultBackground: Color,
    val heatmapHighBackground: Color,
    val workLabel: Color,
    val headerBackground: Color,
    val disabledDay: Color,
    val saturday: Color,
    val sunday: Color,
    val day: Color,
    val headerDay: Color,
    val markerLabel: Color
)

internal fun CalendarCellColors.workHeatBackground(workMinutes: Int): Color {
    val fraction = (workMinutes.toFloat() / HeatmapMaxMinutes).coerceIn(0f, 1f)
    val alpha = HeatmapMinAlpha + (HeatmapMaxAlpha - HeatmapMinAlpha) * fraction
    return heatmapHighBackground.copy(alpha = alpha)
}

internal fun CalendarDateMarkers.countLabel(): String =
    if (totalCount > MaxVisibleMarkerCount) "${MaxVisibleMarkerCount}+" else totalCount.toString()

internal val calendarWeekDays: List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

internal fun daysFromMonday(dayOfWeek: DayOfWeek): Int =
    (dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal + 7) % 7

internal fun daysToSunday(dayOfWeek: DayOfWeek): Int =
    (DayOfWeek.SUNDAY.ordinal - dayOfWeek.ordinal + 7) % 7

internal fun DayOfWeek.headerColor(colors: CalendarCellColors): Color = when (this) {
    DayOfWeek.SATURDAY -> colors.saturday
    DayOfWeek.SUNDAY -> colors.sunday
    else -> colors.headerDay
}

/**
 * Compact non-interactive month grid for the year overview. Cells show only
 * the day rating and done minutes from [YearUiState]; taps are disabled so
 * no details are ever displayed.
 */
@Composable
internal fun YearMonthCalendar(
    month: LocalDate,
    state: YearUiState,
    today: LocalDate
) {
    val colors = rememberCalendarCellColors()
    val weeks = remember(month) { calendarGridDates(month).chunked(7) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                week.forEach { date ->
                    val isEnabled = date.isSameMonth(month)
                    YearDayCell(
                        date = date,
                        isEnabled = isEnabled,
                        isToday = date == today,
                        rating = if (isEnabled) state.ratingFor(date) else null,
                        doneMinutes = if (isEnabled) state.doneMinutesFor(date) else 0,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun YearDayCell(
    date: LocalDate,
    isEnabled: Boolean,
    isToday: Boolean,
    rating: Float?,
    doneMinutes: Int,
    colors: CalendarCellColors,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) {
        Box(modifier = modifier.height(29.dp))
        return
    }

    val dayColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        date.dayOfWeek == DayOfWeek.SATURDAY -> colors.saturday
        date.dayOfWeek == DayOfWeek.SUNDAY -> colors.sunday
        else -> colors.day
    }

    val backgroundColor = when {
        doneMinutes > 0 -> colors.workHeatBackground(doneMinutes)
        rating != null && rating > 0f -> RatingBarDefaults.getRatingColor(rating).copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val shape = RoundedCornerShape(4.dp)
    val cellModifier = if (isToday) {
        modifier
            .height(29.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, MaterialTheme.colorScheme.primary, shape)
    } else {
        modifier
            .height(29.dp)
            .clip(shape)
            .background(backgroundColor)
    }

    Box(
        modifier = cellModifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 1.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.day.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = dayColor,
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                    textAlign = TextAlign.Start
                )
                if (rating != null && rating > 0f) {
                    RatingStar(
                        rating = rating,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            if (doneMinutes > 0) {
                Text(
                    text = doneMinutes.toDurationLabel(compact = true),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.workLabel,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
