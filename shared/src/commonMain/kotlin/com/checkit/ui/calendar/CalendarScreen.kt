package com.checkit.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.calendar_title
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.ui.CalendarUiState
import com.checkit.ui.components.AppHorizontalDivider
import com.checkit.ui.components.MonthHeader
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.isSameMonth
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.shortName
import com.checkit.ui.TaskListDisplayType
import com.checkit.ui.myday.DailyPlanCard
import com.checkit.ui.tasks.TaskListView
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarScreen(
    state: CalendarUiState,
    calendarViewModel: CalendarViewModel,
    onDateDoubleClick: (LocalDate) -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasksForDate = state.tasksForDate(state.selectedDate)
    val notesForDate = state.notesForDate(state.selectedDate)
    val showDailyPlan = state.selectedDate <= today()
    val dailyPlanItems = state.dailyPlanForDate(state.selectedDate)?.items.orEmpty()
    val taskById = remember(state.board.tasks) { state.board.tasks.associateBy { it.id } }
    val hasItemsForDate = if (showDailyPlan) {
        dailyPlanItems.isNotEmpty()
    } else {
        tasksForDate.isNotEmpty() || notesForDate.isNotEmpty()
    }
    var listDisplayType by remember { mutableStateOf(TaskListDisplayType.Brief) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                title = {
                    Text(stringResource(Res.string.calendar_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                },
                actions = {

                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 0.dp)
                .padding(top = padding.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MonthHeader(
                month = state.selectedMonth,
                onPreviousMonth = calendarViewModel::previousMonth,
                onNextMonth = calendarViewModel::nextMonth,
                onCurrentMonth = calendarViewModel::resetToToday
            )
            MonthCalendar(
                month = state.selectedMonth,
                selectedDate = state.selectedDate,
                onDateSelected = calendarViewModel::selectDate,
                onDateDoubleClick = onDateDoubleClick,
                state = state
            )
            AppHorizontalDivider(
                modifier = Modifier.padding(top = 6.dp)
            )
            SelectedDateHeader(
                date = state.selectedDate,
                taskCount = if (showDailyPlan) dailyPlanItems.size else tasksForDate.size,
                noteCount = if (showDailyPlan) 0 else notesForDate.size
            )
            if (hasItemsForDate) {
                if (showDailyPlan) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(dailyPlanItems, key = { it.id }) { item ->
                            DailyPlanCard(
                                item = item,
                                onToggleDone = { },
                                onClick = item.taskId?.let { taskId -> { taskById[taskId]?.let(onTaskClick) } }
                            )
                        }
                    }
                } else {
                    TaskListView(
                        tasks = tasksForDate,
                        notes = notesForDate,
                        lists = state.board.lists,
                        showListName = true,
                        displayType = listDisplayType,
                        onDisplayTypeChange = { listDisplayType = it },
                        onTaskClick = onTaskClick,
                        onNoteClick = onNoteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showDailyPlan) "No My Day history for this day" else "No tasks or notes for this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDateHeader(
    date: LocalDate,
    taskCount: Int,
    noteCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = date.localizedCompactDateWithDayName(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountBadge(icon = Icons.Default.TaskAlt, count = taskCount)
            CountBadge(icon = Icons.AutoMirrored.Filled.Notes, count = noteCount)
        }
    }
}

@Composable
private fun CountBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int) {
    if (count <= 0) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthCalendar(
    month: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
    state: CalendarUiState
) {
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(colorScheme) {
        CalendarCellColors(
            outline = colorScheme.outline.copy(alpha = 0.22f),
            selectedBackground = colorScheme.primaryContainer.copy(alpha = 0.45f),
            defaultBackground = colorScheme.surface,
            heatmapHighBackground = colorScheme.primaryContainer,
            headerBackground = colorScheme.surfaceVariant,
            disabledDay = colorScheme.onSurface.copy(alpha = 0.32f),
            saturday = Color(0xFF249AC8),
            sunday = colorScheme.error,
            day = colorScheme.onSurface,
            headerDay = colorScheme.onSurfaceVariant
        )
    }
    val dates = remember(month) { calendarGridDates(month) }
    val weeks = remember(dates) { dates.chunked(7) }

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
        weeks.forEach { week ->
            CalendarWeekRow(
                week = week,
                month = month,
                selectedDate = selectedDate,
                colors = colors,
                onDateSelected = onDateSelected,
                onDateDoubleClick = onDateDoubleClick,
                state = state
            )
        }
    }
}


@Composable
private fun CalendarWeekRow(
    week: List<LocalDate>,
    month: LocalDate,
    selectedDate: LocalDate,
    colors: CalendarCellColors,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
    state: CalendarUiState
) {
    Row(Modifier.fillMaxWidth()) {
        week.forEach { date ->
            CalendarDayCell(
                date = date,
                month = month,
                isSelected = date == selectedDate,
                colors = colors,
                onDateSelected = onDateSelected,
                onDateDoubleClick = onDateDoubleClick,
                markerColors = state.markerColorsForDate(date),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    month: LocalDate,
    isSelected: Boolean,
    colors: CalendarCellColors,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
    markerColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val isMonthDate = date.isSameMonth(month)
    val dayColor = when {
        !isMonthDate -> colors.disabledDay
        date.dayOfWeek == DayOfWeek.SATURDAY -> colors.saturday
        date.dayOfWeek == DayOfWeek.SUNDAY -> colors.sunday
        else -> colors.day
    }
    val backgroundColor = if (isSelected) colors.selectedBackground else colors.defaultBackground

    Box(
        modifier = modifier
            .height(44.dp)
            .border(0.5.dp, colors.outline)
            .background(backgroundColor)
            .combinedClickable(
                enabled = isMonthDate,
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
            Text(
                text = date.day.toString(),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = dayColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Start
            )
            if (isMonthDate && markerColors.isNotEmpty()) {
                DayMarkers(colors = markerColors)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayMarkers(colors: List<Color>) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        maxItemsInEachRow = MaxMarkersPerRow
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

private const val MaxMarkersPerRow: Int = 6

private fun calendarGridDates(month: LocalDate): List<LocalDate> {
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

data class CalendarCellColors(
    val outline: Color,
    val selectedBackground: Color,
    val defaultBackground: Color,
    val heatmapHighBackground: Color,
    val headerBackground: Color,
    val disabledDay: Color,
    val saturday: Color,
    val sunday: Color,
    val day: Color,
    val headerDay: Color
)

private val calendarWeekDays: List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

private fun daysFromMonday(dayOfWeek: DayOfWeek): Int =
    (dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal + 7) % 7

private fun daysToSunday(dayOfWeek: DayOfWeek): Int =
    (DayOfWeek.SUNDAY.ordinal - dayOfWeek.ordinal + 7) % 7

internal fun DayOfWeek.headerColor(colors: CalendarCellColors): Color = when (this) {
    DayOfWeek.SATURDAY -> colors.saturday
    DayOfWeek.SUNDAY -> colors.sunday
    else -> colors.headerDay
}
