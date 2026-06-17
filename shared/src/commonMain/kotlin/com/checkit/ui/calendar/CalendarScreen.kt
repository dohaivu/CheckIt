package com.checkit.ui.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.ViewDay
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.calendar_title
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.usecase.BuildDailyPlanMarkdownSummaryUseCase
import com.checkit.ui.CalendarDateMarkers
import com.checkit.ui.CalendarDisplayMode
import com.checkit.ui.CalendarUiState
import com.checkit.ui.components.TagOptionMenu
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.isSameMonth
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.localizedMonthTitle
import com.checkit.ui.localizedShortMonthName
import com.checkit.ui.myday.DayLinearTimeline
import com.checkit.ui.myday.MyDayAgenda
import com.checkit.ui.shortName
import com.checkit.ui.tasks.ContentContainerAlpha
import com.checkit.ui.tasks.TaskAgendaView
import com.checkit.ui.tasks.toDurationLabel
import com.checkit.ui.today
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
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
    val today = today()
    val selectedContent = remember(state, today) { state.selectedDateContent(today) }
    val handleDateDoubleClick: (LocalDate) -> Unit = { date ->
        calendarViewModel.selectDate(date)
        if (date <= today) {
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
                    TagOptionMenu(
                        availableTags = state.board.tags,
                        selectedTagIds = state.selectedTagIds,
                        onTagToggle = calendarViewModel::toggleTagFilter
                    )
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
            CalendarPeriodHeader(
                month = state.selectedMonth,
                selectedDate = state.selectedDate,
                displayMode = state.calendarDisplayMode,
                onPreviousPeriod = calendarViewModel::previousPeriod,
                onNextPeriod = calendarViewModel::nextPeriod,
                onCurrentMonth = calendarViewModel::resetToToday,
                onDisplayModeToggle = calendarViewModel::toggleCalendarDisplayMode
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                when (state.calendarDisplayMode) {
                    CalendarDisplayMode.Month -> MonthCalendar(
                        month = state.selectedMonth,
                        selectedDate = state.selectedDate,
                        onDateSelected = calendarViewModel::selectDate,
                        onDateDoubleClick = handleDateDoubleClick,
                        state = state
                    )
                    CalendarDisplayMode.Week -> WeekCalendar(
                        selectedDate = state.selectedDate,
                        onDateSelected = calendarViewModel::selectDate,
                        onDateDoubleClick = handleDateDoubleClick,
                        state = state
                    )
                }
            }
            SelectedDateHeader(
                date = state.selectedDate,
                taskCount = selectedContent.taskCount,
                noteCount = selectedContent.noteCount,
                summaryEnabled = selectedContent.showDailyPlan && state.showDailyPlanSummary,
                summaryAvailable = selectedContent.showDailyPlan,
                onSummaryToggle = calendarViewModel::toggleDailyPlanSummary
            )
            if (selectedContent.showDailyPlan) {
                DayLinearTimeline(
                    items = selectedContent.dailyPlanItems,
                    board = state.board,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 4.dp)
                )
            }
            SelectedDateContent(
                content = selectedContent,
                showDailyPlanSummary = state.showDailyPlanSummary,
                onDailyPlanItemClick = onDailyPlanItemClick,
                onTaskClick = onTaskClick,
                onNoteClick = onNoteClick,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}

@Composable
private fun SelectedDateContent(
    content: SelectedCalendarDateContent,
    showDailyPlanSummary: Boolean,
    onDailyPlanItemClick: (DailyPlanItem, LocalDate) -> Unit,
    onTaskClick: (TaskItem, DailyPlanItem?) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (content.showDailyPlan && showDailyPlanSummary) {
        val summaryBuilder = remember { BuildDailyPlanMarkdownSummaryUseCase() }
        val summaryMarkdown = remember(content.date, content.dailyPlan, content.board) {
            summaryBuilder(
                date = content.date,
                plan = content.dailyPlan,
                board = content.board
            )
        }
        DailyPlanMarkdownSummary(
            markdown = summaryMarkdown,
            modifier = modifier
        )
    } else if (content.hasItems) {
        if (content.showDailyPlan) {
            MyDayAgenda(
                items = content.dailyPlanItems,
                board = content.board,
                date = content.date,
                onItemClick = { onDailyPlanItemClick(it, content.date) },
                onTaskClick = onTaskClick,
                onNoteClick = onNoteClick,
                modifier = modifier
            )
        } else {
            TaskAgendaView(
                tasks = content.tasks,
                notes = content.notes,
                onTaskClick = { onTaskClick(it, null) },
                onNoteClick = onNoteClick,
                dayLimit = 1,
                focusedDate = content.date,
                modifier = modifier
            )
        }
    } else {
        EmptySelectedDateMessage(
            showDailyPlan = content.showDailyPlan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 8.dp)
        )
    }
}

@Composable
private fun EmptySelectedDateMessage(
    showDailyPlan: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (showDailyPlan) "No My Day history for this day" else "No tasks or notes for this day",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarPeriodHeader(
    month: LocalDate,
    selectedDate: LocalDate,
    displayMode: CalendarDisplayMode,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentMonth: () -> Unit,
    onDisplayModeToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onPreviousPeriod) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous period")
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .clickable(onClick = onCurrentMonth)
                .padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (displayMode) {
                    CalendarDisplayMode.Month -> month.localizedMonthTitle()
                    CalendarDisplayMode.Week -> selectedDate.localizedWeekRangeTitle()
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onDisplayModeToggle,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = when (displayMode) {
                        CalendarDisplayMode.Month -> Icons.Outlined.ViewDay
                        CalendarDisplayMode.Week -> Icons.Default.CalendarMonth
                    },
                    contentDescription = when (displayMode) {
                        CalendarDisplayMode.Month -> "Show week"
                        CalendarDisplayMode.Week -> "Show month"
                    },
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(onClick = onNextPeriod) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next period")
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
    val scrollState = rememberScrollState()
    Markdown(
        modifier = modifier.verticalScroll(scrollState),
        content = markdown,
        typography = markdownTypography(
            h1 = MaterialTheme.typography.headlineSmall,
            h2 = MaterialTheme.typography.titleLarge,
            h3 = MaterialTheme.typography.titleMedium,
            h4 = MaterialTheme.typography.bodyMedium,
            h5 = MaterialTheme.typography.bodySmall,
            h6 = MaterialTheme.typography.bodySmall
        ),
    )
}

private data class SelectedCalendarDateContent(
    val date: LocalDate,
    val board: TaskBoard,
    val showDailyPlan: Boolean,
    val dailyPlan: DailyPlan?,
    val dailyPlanItems: List<DailyPlanItem>,
    val tasks: List<TaskItem>,
    val notes: List<NoteItem>
) {
    val taskCount: Int get() = if (showDailyPlan) dailyPlanItems.size else tasks.size
    val noteCount: Int get() = notes.size
    val hasItems: Boolean get() = if (showDailyPlan) dailyPlanItems.isNotEmpty() else tasks.isNotEmpty() || notes.isNotEmpty()
}

private fun CalendarUiState.selectedDateContent(today: LocalDate): SelectedCalendarDateContent {
    val showDailyPlan = selectedDate <= today
    val dailyPlan = dailyPlanForDate(selectedDate)
    return SelectedCalendarDateContent(
        date = selectedDate,
        board = board,
        showDailyPlan = showDailyPlan,
        dailyPlan = dailyPlan,
        dailyPlanItems = dailyPlan?.items.orEmpty(),
        tasks = tasksForDate(selectedDate),
        notes = notesForDate(selectedDate)
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
                state = state,
                isDateEnabled = { it.isSameMonth(month) }
            )
        }
    }
}

@Composable
private fun WeekCalendar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
    state: CalendarUiState
) {
    val colors = rememberCalendarCellColors()
    val week = remember(selectedDate) { weekDates(selectedDate) }

    CalendarGrid(colors = colors) {
        CalendarWeekRow(
            week = week,
            selectedDate = selectedDate,
            colors = colors,
            onDateSelected = onDateSelected,
            onDateDoubleClick = onDateDoubleClick,
            state = state,
            isDateEnabled = { true }
        )
    }
}

@Composable
private fun CalendarGrid(
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
private fun CalendarWeekRow(
    week: List<LocalDate>,
    selectedDate: LocalDate,
    colors: CalendarCellColors,
    onDateSelected: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
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
                markers = if (isEnabled) state.markersForDate(date) else CalendarDateMarkers.Empty,
                workMinutes = if (isEnabled) state.dailyPlanWorkMinutesForDate(date) else 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarDayCell(
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
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = dayColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Start
                )
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
private fun rememberCalendarCellColors(): CalendarCellColors {
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

private fun weekDates(date: LocalDate): List<LocalDate> {
    val start = date.minus(daysFromMonday(date.dayOfWeek), DateTimeUnit.DAY)
    return List(7) { index -> start.plus(index, DateTimeUnit.DAY) }
}

@Composable
private fun LocalDate.localizedWeekRangeTitle(): String {
    val dates = remember(this) { weekDates(this) }
    val start = dates.first()
    val end = dates.last()
    val startLabel = "${start.localizedShortMonthName()} ${start.day}"
    val endLabel = if (start.month == end.month && start.year == end.year) {
        "${end.localizedShortMonthName()} ${end.day}"
    } else if (start.year == end.year) {
        "${end.localizedShortMonthName()} ${end.day}"
    } else {
        "${end.localizedShortMonthName()} ${end.day}, ${end.year}"
    }
    return "$startLabel - $endLabel"
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

private fun CalendarCellColors.workHeatBackground(workMinutes: Int): Color {
    val fraction = (workMinutes.toFloat() / HeatmapMaxMinutes).coerceIn(0f, 1f)
    val alpha = HeatmapMinAlpha + (HeatmapMaxAlpha - HeatmapMinAlpha) * fraction
    return heatmapHighBackground.copy(alpha = alpha)
}

private fun CalendarDateMarkers.countLabel(): String =
    if (totalCount > MaxVisibleMarkerCount) "${MaxVisibleMarkerCount}+" else totalCount.toString()

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
