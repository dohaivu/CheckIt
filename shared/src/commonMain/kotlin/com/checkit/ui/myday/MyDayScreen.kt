package com.checkit.ui.myday

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.day_close_open
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.JournalEntry
import com.checkit.domain.NoteItem
import com.checkit.domain.Period
import com.checkit.domain.PeriodGoal
import com.checkit.domain.SprintState
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import com.checkit.domain.hasEndTime
import com.checkit.ui.components.MetricChip
import com.checkit.ui.components.PeriodGoalRow
import com.checkit.ui.components.RatingBar
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.journal.JournalListSheet
import com.checkit.ui.journal.JournalSection
import com.checkit.ui.journal.JournalThoughtCard
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.TimelineItem
import com.checkit.ui.TimelineItemType
import com.checkit.ui.isOverdue
import com.checkit.ui.reflect.ReflectGoalEditorMode
import com.checkit.ui.tasks.views.AgendaView
import com.checkit.ui.tasks.views.DailyPlanAllDayCard
import com.checkit.ui.tasks.views.DailyPlanTimelineCard
import com.checkit.ui.tasks.views.NoteAllDayCard
import com.checkit.ui.tasks.views.NoteTimelineCard
import com.checkit.ui.tasks.views.SprintButton
import com.checkit.ui.tasks.views.TaskAllDayCard
import com.checkit.ui.tasks.views.TaskTimelineCard
import com.checkit.ui.tasks.views.TimelineView
import com.checkit.ui.today
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyDayScreen(
    viewModel: MyDayViewModel,
    onTaskClick: (Long, DailyPlanItem?) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onNoteTimeChange: (NoteItem, Int) -> Unit,
    onCreateTask: (addToMyDayOnSave: Boolean) -> Unit,
    onOpenGoalEditor: (LocalDate, com.checkit.domain.Period, ReflectGoalEditorMode) -> Unit,
    onNewTagClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val sprintState by viewModel.sprintManager.state.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                title = {
                    Column {
                        Text("My Day", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            state.today.localizedCompactDateWithDayName(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::smartSchedule) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Smart Schedule"
                        )
                    }
                    IconButton(onClick = viewModel::openDayClose) {
                        Icon(
                            Icons.Default.RateReview,
                            contentDescription = stringResource(Res.string.day_close_open)
                        )
                    }
                    IconButton(onClick = viewModel::openSuggestions) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Add to My Day")
                    }
                    IconButton(onClick = viewModel::openDailyPlan) {
                        Icon(Icons.Default.AddTask, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            if (sprintState is SprintState.Idle) {
                SpeedSprintFab(
                    lastAction = state.lastFabAction,
                    recentTags = state.recentTags,
                    onExecuteAction = viewModel::executeFabAction
                )
            }
        }
    ) { padding ->
        val activeSprint = when (val s = sprintState) {
            is SprintState.Running -> s
            is SprintState.Paused -> s.runningState
            else -> null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MyDayViewSelector(
                    selectedView = state.selectedView,
                    onSelect = viewModel::selectView
                )
                DayLinearTimeline(
                    items = state.items,
                    modifier = Modifier.fillMaxWidth()
                )
                JournalSection(
                    entries = state.journalEntries,
                    onAddClick = viewModel::openNewJournalEntry,
                    onViewClick = viewModel::openJournalList
                )

                val dayBannerType = state.dayBannerType
                val weekBannerType = state.weekBannerType
                val monthBannerType = state.monthBannerType

                // Period banners - use cached goals/banners to avoid repeated goalFor() scans
                if (!state.isLoading) {
                    val dayGoal = state.dayGoal
                    val weekGoal = state.weekGoal
                    val monthGoal = state.monthGoal
                    listOf(
                        Triple(Period.Day, dayBannerType, dayGoal),
                        Triple(Period.Week, weekBannerType, weekGoal),
                        Triple(Period.Month, monthBannerType, monthGoal)
                    ).forEach { (period, type, goal) ->
                        when (type) {
                            PeriodBannerType.ReviewPending -> {
                                ReviewReminder(
                                    period = period,
                                    onClick = {
                                        if (period == Period.Day) {
                                            viewModel.openDayClose()
                                        } else {
                                            onOpenGoalEditor(state.today, period, ReflectGoalEditorMode.Full)
                                        }
                                    }
                                )
                            }

                            PeriodBannerType.MissingGoal -> {
                                GoalReminder(
                                    period = period,
                                    onClick = {
                                        onOpenGoalEditor(state.today, period, ReflectGoalEditorMode.GoalOnly)
                                    }
                                )
                            }

                            PeriodBannerType.ActiveGoal -> {
                                if (period == Period.Day) {
                                    DayGoalBanner(
                                        goal = goal!!,
                                        weekGoal = if (weekBannerType == PeriodBannerType.ActiveGoal) weekGoal else null,
                                        monthGoal = if (monthBannerType == PeriodBannerType.ActiveGoal) monthGoal else null
                                    )
                                }
                            }
                        }
                    }
                }

                when (state.selectedView) {
                    MyDayView.Agenda -> MyDayAgenda(
                        items = state.items,
                        notes = state.notes,
                        date = state.today,
                        activeSprint = activeSprint,
                        journalEntries = state.journalEntries,
                        onItemClick = { viewModel.openItemEditor(it, state.today) },
                        onTaskClick = onTaskClick,
                        onNoteClick = onNoteClick,
                        onSprintClick = viewModel::startSprint,
                        modifier = Modifier.weight(1f)
                    )
                    MyDayView.Timeline -> MyDayTimeline(
                        items = state.items,
                        notes = state.notes,
                        date = state.today,
                        activeSprint = activeSprint,
                        onItemClick = { viewModel.openItemEditor(it, state.today) },
                        onTaskClick = onTaskClick,
                        onNoteClick = onNoteClick,
                        onSprintClick = viewModel::startSprint,
                        onCreateTask = viewModel::createFromTimelineRange,
                        onItemTimeChange = viewModel::updateItemTime,
                        onNoteTimeChange = onNoteTimeChange,
                        modifier = Modifier.weight(1f)
                    )
                    MyDayView.Board -> MyDayBoard(
                        state = state,
                        activeSprint = activeSprint,
                        onItemClick = { viewModel.openItemEditor(it, state.today) },
                        onTaskClick = onTaskClick,
                        onSprintClick = viewModel::startSprint,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (activeSprint != null) {
                SprintBar(
                    state = activeSprint,
                    isPaused = sprintState is SprintState.Paused,
                    onPause = viewModel::pauseSprint,
                    onResume = viewModel::resumeSprint,
                    onStop = viewModel::completeSprint,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }

            AnimatedVisibility(
                visible = state.showFloatingQuickAdd && activeSprint == null,
                enter = fadeIn() + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 70.dp, bottom = 24.dp)
            ) {
                FloatingQuickAddBar(onSubmit = { title -> viewModel.addDailyPlanItem(title, emptyList()) })
            }
        }
    }

    if (state.showQuickSprintSheet) {
        QuickSprintSheet(
            suggestedToday = state.sprintSuggestedToday,
            suggestedYesterday = state.sprintSuggestedYesterday,
            suggestedTasks = state.sprintSuggestedTasks,
            availableTags = state.tags,
            continueItem = state.continueSprintItem,
            onStartSprint = { taskId, dailyPlanItemId, description, tagIds ->
                viewModel.startSprint(taskId, dailyPlanItemId, description, tagIds)
            },
            onStartSprintWithChoice = viewModel::startSprintWithChoice,
            onStartSprintWithTask = viewModel::startSprintWithTask,
            onNewTagClick = onNewTagClick,
            onDismiss = viewModel::dismissQuickSprint
        )
    }

    if (sprintState is SprintState.Finished) {
        SprintCompletionDialog(
            state = sprintState as SprintState.Finished,
            onSaveWin = viewModel::saveSprintAsWin,
            onSaveAndBreak = viewModel::saveAndBreak,
            onContinueNewPomodoro = viewModel::continueNewPomodoro,
            onStartPomodoro = viewModel::upgradeToPomodoro,
            onStartNextPomodoro = viewModel::startNextPomodoro,
            onDismiss = viewModel::dismissFinishedSprint
        )
    }

    if (state.showSuggestions) {
        state.periodGoals
        SuggestionsSheet(
            tasks = state.suggestedTasks,
            leftovers = state.pendingYesterdayLeftovers,
            availableTags = state.tags,
            onDismiss = viewModel::dismissSuggestions,
            onTaskClick = {
                onTaskClick.invoke(it.id, null)
            },
            onAddTask = viewModel::addTaskFromSuggestion,
            onQuickAdd = viewModel::addDailyPlanItem,
            onCarryLeftover = viewModel::carryYesterdayLeftover,
            onCarryAllLeftovers = viewModel::carryAllYesterdayLeftovers,
            onCreateTask = {
                viewModel.dismissSuggestions()
                onCreateTask(true)
            },
            onNewTagClick = onNewTagClick
        )
    }

    state.dayClose?.let { review ->
        DayCloseSheet(
            state = review,
            onDismiss = viewModel::dismissDayClose,
            onLeftoverAction = viewModel::setLeftoverAction,
            onWinNoteChange = viewModel::updateWinNote,
            onTomorrowGoalChange = viewModel::updateTomorrowGoal,
            onConfirm = viewModel::confirmDayClose
        )
    }

    if (state.showJournalList) {
        JournalListSheet(
            entries = state.journalEntries,
            onEntryClick = { entry ->
                viewModel.dismissJournalList()
                viewModel.openJournalEditor(entry)
            },
            onDismiss = viewModel::dismissJournalList
        )
    }

    CelebrationOverlay(visible = state.showCelebration)
}

@Composable
private fun CelebrationOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Great work today!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReviewReminder(
    period: Period,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val periodLabel = when (period) {
        Period.Day -> "today"
        Period.Week -> "this week"
        Period.Month -> "this month"
        else -> period.name.lowercase()
    }
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.5.dp,
                brush = gradient,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(gradient, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RateReview,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Time to Reflect".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "How was $periodLabel? Jot down your wins and lessons.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun GoalReminder(
    period: Period,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val periodLabel = when (period) {
        Period.Day -> "today's"
        Period.Week -> "this week's"
        Period.Month -> "this month's"
        else -> period.name.lowercase()
    }
    val color = when (period) {
        Period.Day -> MaterialTheme.colorScheme.secondary
        Period.Week -> MaterialTheme.colorScheme.tertiary
        Period.Month -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),

        contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onError
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Missing Goal".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = 1.sp
            )
            Text(
                text = "Tap to set $periodLabel focus and metrics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DayGoalBanner(
    goal: PeriodGoal,
    weekGoal: PeriodGoal? = null,
    monthGoal: PeriodGoal? = null,
    modifier: Modifier = Modifier
) {
    val color = when (goal.period) {
        Period.Day -> MaterialTheme.colorScheme.secondary
        Period.Week -> MaterialTheme.colorScheme.tertiary
        Period.Month -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    var expanded by remember { mutableStateOf(false) }
    var bannerSize by remember { mutableStateOf(IntSize.Zero) }

    val hasWeekGoal = weekGoal?.goal?.isNotBlank() == true || weekGoal?.metrics?.isNotEmpty() == true
    val hasMonthGoal = monthGoal?.goal?.isNotBlank() == true || monthGoal?.metrics?.isNotEmpty() == true
    val hasMoreGoals = hasWeekGoal || hasMonthGoal

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { bannerSize = it }
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.08f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color.copy(alpha = 0.12f),
                            androidx.compose.foundation.shape.RoundedCornerShape(5.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = color
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${goal.period.name.uppercase()} FOCUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                    )
                    goal.goal?.takeIf { it.isNotBlank() }?.let { intent ->
                        Text(
                            text = intent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (goal.metrics.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            goal.metrics.forEach { metric ->
                                MetricChip(metric)
                            }
                        }
                    }
                }
                if (hasMoreGoals) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(18.dp),
                        tint = color.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (hasMoreGoals && expanded) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, bannerSize.height),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier
                        .width(with(density) { bannerSize.width.toDp() })
                        .clip(RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                    ) {
                        weekGoal?.let {
                            if (it.goal?.isNotBlank() == true || it.metrics.isNotEmpty()) {
                                PeriodGoalRow(
                                    icon = Icons.Default.DateRange,
                                    label = "WEEK",
                                    goal = it,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        monthGoal?.let {
                            if (it.goal?.isNotBlank() == true || it.metrics.isNotEmpty()) {
                                PeriodGoalRow(
                                    icon = Icons.Default.CalendarMonth,
                                    label = "MONTH",
                                    goal = it,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyDayViewSelector(
    selectedView: MyDayView,
    onSelect: (MyDayView) -> Unit,
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
        MyDayView.entries.forEach { view ->
            val selected = selectedView == view
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        brush = if (selected) {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onSelect(view) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = view.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = view.label(),
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
}

@Composable
internal fun MyDayAgenda(
    items: List<DailyPlanItem>,
    notes: List<NoteItem>,
    tasks: List<TaskItem> = emptyList(),
    journalEntries: List<JournalEntry> = emptyList(),
    date: LocalDate,
    activeSprint: SprintState.Running?,
    onItemClick: (DailyPlanItem) -> Unit,
    onTaskClick: (Long, DailyPlanItem?) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onSprintClick: ((Long?, Long?, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val projection = remember(items, notes, journalEntries) { items.toDayViewProjection(notes = notes, journalEntries = journalEntries) }
    val timelineItems = remember(projection, tasks, date) {
        projection.toTimelineItems(tasks = tasks, date = date)
    }

    AgendaView(
        items = timelineItems,
        onItemClick = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> {
                    if (tag.taskId != null) {
                        onTaskClick(tag.taskId, tag)
                    } else {
                        onItemClick(tag)
                    }
                }
                is TaskItem -> onTaskClick(tag.id, null)
                is NoteItem -> onNoteClick(tag)
            }
        },
        dayLimit = 1,
        focusedDate = date,
        itemContent = { item ->
            when (val tag = item.tag) {
                is JournalEntry -> JournalThoughtCard(entry = tag)
                is TaskItem -> if (item.startTimeMinutes == null) {
                    TaskAllDayCard(tag, completedOverlay = tag.status == TaskStatus.Completed)
                } else {
                    TaskTimelineCard(tag, completedOverlay = tag.status == TaskStatus.Completed)
                }
                is DailyPlanItem -> if (item.startTimeMinutes == null) {
                    DailyPlanAllDayCard(
                        item = tag,
                        trailingContent = onSprintClick?.let {
                            { SprintTrailingContent(tag, activeSprint, it) }
                        }
                    )
                } else {
                    DailyPlanTimelineCard(
                        item = tag,
                        isOverdue = tag.isOverdue(date),
                        trailingContent = onSprintClick?.let {
                            { SprintTrailingContent(tag, activeSprint, it) }
                        }
                    )
                }
                is NoteItem -> if (item.startTimeMinutes == null) NoteAllDayCard(tag) else NoteTimelineCard(tag)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun MyDayTimeline(
    items: List<DailyPlanItem>,
    notes: List<NoteItem>,
    date: LocalDate,
    activeSprint: SprintState.Running?,
    onItemClick: (DailyPlanItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onTaskClick: (Long, DailyPlanItem?) -> Unit,
    onSprintClick: ((Long?, Long?, String) -> Unit)? = null,
    onCreateTask: (Int, Int) -> Unit,
    onItemTimeChange: (DailyPlanItem, Int, Int) -> Unit,
    onNoteTimeChange: (NoteItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val projection = remember(items, notes, date) {
        items.toDayViewProjection(notes = notes, journalEntries = emptyList())
    }
    val timelineItems = remember(projection) {
        projection.toTimelineItems(resizable = true)
    }

    TimelineView(
        items = timelineItems,
        onItemClick = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> {
                    if (tag.taskId != null) {
                        onTaskClick(tag.taskId, tag)
                    } else {
                        onItemClick(tag)
                    }
                }
                is NoteItem -> onNoteClick(tag)
            }
        },
        onCreateRequest = onCreateTask,
        onTimeChange = { item, start, end ->
            when (val tag = item.tag) {
                is DailyPlanItem -> onItemTimeChange(tag, start, end)
                is NoteItem -> onNoteTimeChange(tag, start)
            }
        },
        allDayItemContent = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> DailyPlanAllDayCard(
                    item = tag,
                    trailingContent = onSprintClick?.let {
                        { SprintTrailingContent(tag, activeSprint, it) }
                    }
                )
                is NoteItem -> NoteAllDayCard(tag)
            }
        },
        timedItemContent = { item, isSelected, displayMode ->
            when (val tag = item.tag) {
                is DailyPlanItem -> DailyPlanTimelineCard(
                    item = tag,
                    selected = isSelected,
                    modifier = Modifier.matchParentSize(),
                    displayMode = displayMode,
                    isOverdue = tag.isOverdue(date),
                    trailingContent = onSprintClick?.let {
                        { SprintTrailingContent(tag, activeSprint, it) }
                    }
                )
                is NoteItem -> NoteTimelineCard(tag, selected = isSelected, modifier = Modifier.matchParentSize())
            }
        },
        modifier = modifier
    )
}

@Composable
private fun MyDayBoard(
    state: MyDayUiState,
    activeSprint: SprintState.Running?,
    onItemClick: (DailyPlanItem) -> Unit,
    onTaskClick: (Long, DailyPlanItem?) -> Unit,
    onSprintClick: ((Long?, Long?, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionLabel("Planned") }
        if (state.plannedItems.isEmpty()) {
            item { EmptyStateText("Nothing planned") }
        } else {
            items(state.plannedItems, key = { "planned-${it.id}" }) { item ->
                MyDayBoardItem(
                    item = item,
                    activeSprint = activeSprint,
                    onItemClick = onItemClick,
                    onTaskClick = onTaskClick,
                    onSprintClick = onSprintClick
                )
            }
        }
        item { SectionLabel("Done") }
        if (state.doneItems.isEmpty()) {
            item { EmptyStateText("Nothing done yet") }
        } else {
            items(state.doneItems, key = { "done-${it.id}" }) { item ->
                MyDayBoardItem(
                    item = item,
                    activeSprint = activeSprint,
                    onItemClick = onItemClick,
                    onTaskClick = onTaskClick,
                    onSprintClick = onSprintClick
                )
            }
        }
    }
}

@Composable
private fun MyDayBoardItem(
    item: DailyPlanItem,
    activeSprint: SprintState.Running?,
    onItemClick: (DailyPlanItem) -> Unit,
    onTaskClick: (Long, DailyPlanItem?) -> Unit,
    onSprintClick: ((Long?, Long?, String) -> Unit)? = null
) {
    DailyPlanTimelineCard(
        item = item,
        onClick = {
            if (item.taskId != null) {
                onTaskClick(item.taskId, item)
            } else {
                onItemClick(item)
            }
        },
        isOverdue = item.isOverdue(today()),
        trailingContent = onSprintClick?.let {
            { SprintTrailingContent(item, activeSprint, it) }
        }
    )
}

@Composable
private fun SprintTrailingContent(
    item: DailyPlanItem,
    activeSprint: SprintState.Running?,
    onSprintClick: ((Long?, Long?, String) -> Unit)?,
    taskId: Long? = item.taskId,
    title: String = item.title
) {
    if (item.status == DailyPlanItemStatus.Done || onSprintClick == null) return

    if (activeSprint?.dailyPlanItemId == item.id) {
        val dotColor = when {
            activeSprint.isBreak -> MaterialTheme.colorScheme.secondary
            activeSprint.isPomodoro -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.tertiary
        }
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            PulsingDot(color = dotColor)
        }
    } else {
        SprintButton(onClick = { onSprintClick(taskId, item.id, title) })
    }
}

private fun DailyPlanItem.isDone(): Boolean = status == DailyPlanItemStatus.Done

private fun DayViewProjection.toTimelineItems(
    tasks: List<TaskItem> = emptyList(),
    date: LocalDate? = null,
    resizable: Boolean = false
): List<TimelineItem> {
    val planItems = items.map { item ->
        TimelineItem(
            id = "dailyplan-${item.id}",
            type = if (item.taskId != null) TimelineItemType.Task else TimelineItemType.DailyPlan,
            date = date,
            startTimeMinutes = item.startTimeMinutes,
            endTimeMinutes = item.endTimeMinutes,
            sortOrder = item.sortOrder,
            isResizable = resizable && (item.taskId != null || item.source.hasEndTime()),
            tag = item
        )
    }
    val taskItems = tasks.map { task ->
        TimelineItem(
            id = "task-${task.id}",
            type = TimelineItemType.Task,
            date = date,
            startTimeMinutes = task.startTimeMinutes,
            endTimeMinutes = task.endTimeMinutes,
            sortOrder = task.sortOrder,
            isResizable = false,
            tag = task
        )
    }
    val noteItems = notes.map { note ->
        TimelineItem(
            id = "note-${note.id}",
            type = TimelineItemType.Note,
            date = date,
            startTimeMinutes = note.startTimeMinutes,
            endTimeMinutes = null,
            sortOrder = note.sortOrder,
            isResizable = false,
            tag = note
        )
    }
    val journalItems = journalEntries.map { entry ->
        TimelineItem(
            id = "journal-${entry.id}",
            type = TimelineItemType.Journal,
            date = date,
            startTimeMinutes = entry.createdTimeMinutes,
            endTimeMinutes = null,
            sortOrder = 0,
            isResizable = false,
            tag = entry
        )
    }
    return (planItems + noteItems + journalItems + taskItems)
        .sortedWith(compareBy<TimelineItem> { it.startTimeMinutes ?: -1 }.thenBy { it.sortOrder })
}

private fun MyDayView.icon(): ImageVector = when (this) {
    MyDayView.Agenda -> Icons.AutoMirrored.Filled.ViewList
    MyDayView.Timeline -> Icons.Default.Schedule
    MyDayView.Board -> Icons.Default.Dashboard
}

private fun MyDayView.label(): String = when (this) {
    MyDayView.Agenda -> "Agenda"
    MyDayView.Timeline -> "Timeline"
    MyDayView.Board -> "Board"
}
