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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.calendar_title
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.usecase.BuildDailyPlanMarkdownSummaryUseCase
import com.checkit.ui.CalendarUiState
import com.checkit.ui.components.AppHorizontalDivider
import com.checkit.ui.components.MonthHeader
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.isSameMonth
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.shortName
import com.checkit.ui.myday.DayLinearTimeline
import com.checkit.ui.myday.MyDayAgenda
import com.checkit.ui.tasks.ContentContainerAlpha
import com.checkit.ui.tasks.TaskAgendaView
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
    onDailyPlanItemClick: (DailyPlanItem, LocalDate) -> Unit,
    onAddDailyPlanItem: (LocalDate) -> Unit,
    onTaskClick: (TaskItem, DailyPlanItem?) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasksForDate = state.tasksForDate(state.selectedDate)
    val notesForDate = state.notesForDate(state.selectedDate)
    val showDailyPlan = state.selectedDate <= today()
    val selectedDailyPlan = state.dailyPlanForDate(state.selectedDate)
    val dailyPlanItems = selectedDailyPlan?.items.orEmpty()
    val hasItemsForDate = if (showDailyPlan) {
        dailyPlanItems.isNotEmpty()
    } else {
        tasksForDate.isNotEmpty()
    }
    val handleDateDoubleClick: (LocalDate) -> Unit = { date ->
        calendarViewModel.selectDate(date)
        if (date <= today()) {
            onAddDailyPlanItem(date)
        } else {
            onDateDoubleClick(date)
        }
    }

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
                onDateDoubleClick = handleDateDoubleClick,
                state = state
            )
            SelectedDateHeader(
                date = state.selectedDate,
                taskCount = if (showDailyPlan) dailyPlanItems.size else tasksForDate.size,
                noteCount = notesForDate.size,
                summaryEnabled = showDailyPlan && state.showDailyPlanSummary,
                summaryAvailable = showDailyPlan,
                onSummaryToggle = calendarViewModel::toggleDailyPlanSummary
            )
            if (showDailyPlan) {
                DayLinearTimeline(
                    items = dailyPlanItems,
                    board = state.board,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 4.dp)
                )
            }
            if (showDailyPlan && state.showDailyPlanSummary) {
                val summaryBuilder = remember { BuildDailyPlanMarkdownSummaryUseCase() }
                val summaryMarkdown = remember(state.selectedDate, selectedDailyPlan, state.board) {
                    summaryBuilder(
                        date = state.selectedDate,
                        plan = selectedDailyPlan,
                        board = state.board
                    )
                }
                DailyPlanMarkdownSummary(
                    markdown = summaryMarkdown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else if (hasItemsForDate) {
                if (showDailyPlan) {
                    MyDayAgenda(
                        items = dailyPlanItems,
                        board = state.board,
                        date = state.selectedDate,
                        onItemClick = { onDailyPlanItemClick(it, state.selectedDate) },
                        onTaskClick = onTaskClick,
                        onNoteClick = onNoteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    TaskAgendaView(
                        tasks = tasksForDate,
                        notes = notesForDate,
                        onTaskClick = {
                            onTaskClick(it, null)
                        },
                        onNoteClick = onNoteClick,
                        dayLimit = 1,
                        focusedDate = state.selectedDate,
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
    noteCount: Int,
    summaryEnabled: Boolean,
    summaryAvailable: Boolean,
    onSummaryToggle: () -> Unit
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
            if (summaryAvailable) {
                IconButton(
                    onClick = onSummaryToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Article,
                        contentDescription = if (summaryEnabled) "Hide summary" else "Show summary",
                        modifier = Modifier.size(18.dp),
                        tint = if (summaryEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyPlanMarkdownSummary(
    markdown: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = markdown,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 12.dp)
    )
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
            selectedBackground = colorScheme.primaryContainer.copy(alpha = ContentContainerAlpha),
            defaultBackground = colorScheme.surface,
            heatmapHighBackground = colorScheme.primaryContainer,
            workLabel = colorScheme.primary,
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
                workMinutes = state.dailyPlanWorkMinutesForDate(date),
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
    workMinutes: Int,
    modifier: Modifier = Modifier
) {
    val isMonthDate = date.isSameMonth(month)
    val dayColor = when {
        !isMonthDate -> colors.disabledDay
        date.dayOfWeek == DayOfWeek.SATURDAY -> colors.saturday
        date.dayOfWeek == DayOfWeek.SUNDAY -> colors.sunday
        else -> colors.day
    }
    val backgroundColor = when {
        isSelected -> colors.selectedBackground
        isMonthDate && workMinutes > 0 -> colors.workHeatBackground(workMinutes)
        else -> colors.defaultBackground
    }

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
                if (isMonthDate && workMinutes > 0) {
                    Text(
                        text = workMinutes.compactDurationLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.workLabel,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                }
            }
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
private const val HeatmapMaxMinutes: Int = 8 * 60
private const val HeatmapMinAlpha: Float = 0.10f
private const val HeatmapMaxAlpha: Float = 0.42f

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
    val workLabel: Color,
    val headerBackground: Color,
    val disabledDay: Color,
    val saturday: Color,
    val sunday: Color,
    val day: Color,
    val headerDay: Color
)

private fun CalendarCellColors.workHeatBackground(workMinutes: Int): Color {
    val fraction = (workMinutes.toFloat() / HeatmapMaxMinutes).coerceIn(0f, 1f)
    val alpha = HeatmapMinAlpha + (HeatmapMaxAlpha - HeatmapMinAlpha) * fraction
    return heatmapHighBackground.copy(alpha = alpha)
}

internal fun Int.compactDurationLabel(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

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
