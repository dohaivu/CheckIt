package com.checkit.ui.myday

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.day_review_banner_subtitle
import checkit.shared.generated.resources.day_review_banner_title
import checkit.shared.generated.resources.day_review_open
import checkit.shared.generated.resources.leftovers_banner_carry_all
import checkit.shared.generated.resources.leftovers_banner_dismiss
import checkit.shared.generated.resources.leftovers_banner_review
import checkit.shared.generated.resources.leftovers_banner_subtitle
import checkit.shared.generated.resources.leftovers_banner_title
import checkit.shared.generated.resources.leftovers_item_carry
import checkit.shared.generated.resources.leftovers_section_title
import checkit.shared.generated.resources.plan_assist_banner_dismiss
import checkit.shared.generated.resources.plan_assist_banner_subtitle
import checkit.shared.generated.resources.plan_assist_banner_title
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.hasEndTime
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.tasks.TimelineItem
import com.checkit.ui.tasks.TimelineItemType
import com.checkit.ui.tasks.isOverdue
import com.checkit.ui.tasks.timeRangeLabel
import com.checkit.ui.tasks.toClockLabel
import com.checkit.ui.tasks.views.AgendaView
import com.checkit.ui.tasks.views.DailyPlanAllDayCard
import com.checkit.ui.tasks.views.DailyPlanTimelineCard
import com.checkit.ui.tasks.views.NoteAllDayCard
import com.checkit.ui.tasks.views.NoteTimelineCard
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
    onTaskClick: (TaskItem, DailyPlanItem?) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onNoteTimeChange: (NoteItem, Int) -> Unit,
    onCreateTask: (addToMyDayOnSave: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
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
                    IconButton(onClick = viewModel::openDayReview) {
                        Icon(
                            Icons.Default.RateReview,
                            contentDescription = stringResource(Res.string.day_review_open)
                        )
                    }
                    IconButton(onClick = viewModel::openSuggestions) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Add to My Day")
                    }
                    IconButton(onClick = viewModel::openCheckIn) {
                        Icon(Icons.Default.AddTask, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.showLeftoversBanner) {
                LeftoversBanner(
                    count = state.pendingYesterdayLeftovers.size,
                    onCarryAll = viewModel::carryAllYesterdayLeftovers,
                    onReview = viewModel::openLeftoversSheet,
                    onDismiss = viewModel::dismissLeftoversBanner
                )
            }
            if (state.showPlanAssistBanner) {
                PlanAssistBanner(
                    onPlan = viewModel::openPlanAssist,
                    onDismiss = viewModel::dismissPlanAssist
                )
            }
            if (state.showDayReviewBanner) {
                DayReviewBanner(onClick = viewModel::openDayReview)
            }
            MyDayViewSelector(
                selectedView = state.selectedView,
                onSelect = viewModel::selectView
            )
            DayLinearTimeline(
                items = state.items,
                modifier = Modifier.fillMaxWidth()
            )
            when (state.selectedView) {
                MyDayView.Agenda -> MyDayAgenda(
                    items = state.items,
                    board = state.board,
                    date = state.today,
                    onItemClick = { viewModel.openItemEditor(it, state.today) },
                    onTaskClick = onTaskClick,
                    onNoteClick = onNoteClick,
                    modifier = Modifier.weight(1f)
                )
                MyDayView.Timeline -> MyDayTimeline(
                    items = state.items,
                    board = state.board,
                    date = state.today,
                    onItemClick = { viewModel.openItemEditor(it, state.today) },
                    onTaskClick = onTaskClick,
                    onNoteClick = onNoteClick,
                    onCreateTask = viewModel::createFromTimelineRange,
                    onItemTimeChange = viewModel::updateItemTime,
                    onNoteTimeChange = onNoteTimeChange,
                    modifier = Modifier.weight(1f)
                )
                MyDayView.Board -> MyDayBoard(
                    state = state,
                    onItemClick = { viewModel.openItemEditor(it, state.today) },
                    onTaskClick = onTaskClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (state.showSuggestions) {
        SuggestionsSheet(
            tasks = state.suggestedTasks,
            leftovers = state.pendingYesterdayLeftovers,
            onDismiss = viewModel::dismissSuggestions,
            onTaskClick = {
                onTaskClick.invoke(it, null)
            },
            onAddTask = viewModel::addTaskFromSuggestion,
            onCarryLeftover = viewModel::carryYesterdayLeftover,
            onCarryAllLeftovers = viewModel::carryAllYesterdayLeftovers,
            onCreateTask = {
                viewModel.dismissSuggestions()
                onCreateTask(true)
            }
        )
    }

    if (state.showLeftoversSheet) {
        LeftoversSheet(
            items = state.pendingYesterdayLeftovers,
            onDismiss = viewModel::dismissLeftoversSheet,
            onCarry = viewModel::carryYesterdayLeftover,
            onCarryAll = viewModel::carryAllYesterdayLeftovers
        )
    }

    state.dayReview?.let { review ->
        DayReviewSheet(
            state = review,
            onDismiss = viewModel::dismissDayReview,
            onLeftoverAction = viewModel::setLeftoverAction,
            onWinNoteChange = viewModel::updateWinNote,
            onTomorrowGoalChange = viewModel::updateTomorrowGoal,
            onConfirm = viewModel::confirmDayReview
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
private fun DayReviewBanner(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.RateReview,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.day_review_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.day_review_banner_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun LeftoversBanner(
    count: Int,
    onCarryAll: () -> Unit,
    onReview: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.leftovers_banner_title, count),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.leftovers_banner_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.leftovers_banner_dismiss),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReview) {
                    Text(stringResource(Res.string.leftovers_banner_review))
                }
                FilledTonalButton(
                    onClick = onCarryAll,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Text(stringResource(Res.string.leftovers_banner_carry_all))
                }
            }
        }
    }
}

@Composable
private fun PlanAssistBanner(
    onPlan: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlan),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.plan_assist_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.plan_assist_banner_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.plan_assist_banner_dismiss),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MyDayViewSelector(
    selectedView: MyDayView,
    onSelect: (MyDayView) -> Unit
) {
    SingleChoiceSegmentedButtonRow {
        MyDayView.entries.forEachIndexed { index, view ->
            SegmentedButton(
                selected = selectedView == view,
                onClick = { onSelect(view) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = MyDayView.entries.size),
                icon = { Icon(view.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text(view.label()) },
                colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.primaryContainer, activeContentColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
internal fun MyDayAgenda(
    items: List<DailyPlanItem>,
    board: TaskBoard,
    date: LocalDate,
    onItemClick: (DailyPlanItem) -> Unit,
    onTaskClick: (TaskItem, DailyPlanItem?) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val projection = remember(items, board, date) { items.toTaskViewProjection(board = board, date = date) }
    val timelineItems = remember(projection, date) {
        projection.toTimelineItems(date = date)
    }

    AgendaView(
        items = timelineItems,
        onItemClick = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> onItemClick(tag)
                is NoteItem -> onNoteClick(tag)
                is PlannedTaskProjection -> onTaskClick(tag.task, tag.dailyPlanItem)
            }
        },
        dayLimit = 1,
        focusedDate = date,
        itemContent = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> if (item.startTimeMinutes == null) DailyPlanAllDayCard(tag) else DailyPlanTimelineCard(tag, isOverdue = tag.isOverdue(date))
                is NoteItem -> if (item.startTimeMinutes == null) NoteAllDayCard(tag) else NoteTimelineCard(tag)
                is PlannedTaskProjection -> {
                    val task = tag.task
                    if (item.startTimeMinutes == null) {
                        TaskAllDayCard(task)
                    } else {
                        TaskTimelineCard(
                            task = task,
                            timeLabel = tag.dailyPlanItem.dailyPlanTimeLabel(),
                            completed = tag.dailyPlanItem.isDone(),
                            isOverdue = tag.dailyPlanItem.isOverdue(date)
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun MyDayTimeline(
    items: List<DailyPlanItem>,
    board: TaskBoard,
    date: LocalDate,
    onItemClick: (DailyPlanItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onTaskClick: (TaskItem, DailyPlanItem?) -> Unit,
    onCreateTask: (Int, Int) -> Unit,
    onItemTimeChange: (DailyPlanItem, Int, Int) -> Unit,
    onNoteTimeChange: (NoteItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val projection = remember(items, board, date) {
        items.toTaskViewProjection(board = board, date = date)
    }
    val timelineItems = remember(projection) {
        projection.toTimelineItems(resizable = true)
    }

    TimelineView(
        items = timelineItems,
        onItemClick = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> onItemClick(tag)
                is NoteItem -> onNoteClick(tag)
                is PlannedTaskProjection -> onTaskClick(tag.task, tag.dailyPlanItem)
            }
        },
        onCreateRequest = onCreateTask,
        onTimeChange = { item, start, end ->
            when (val tag = item.tag) {
                is DailyPlanItem -> onItemTimeChange(tag, start, end)
                is NoteItem -> onNoteTimeChange(tag, start)
                is PlannedTaskProjection -> onItemTimeChange(tag.dailyPlanItem, start, end)
            }
        },
        allDayItemContent = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> DailyPlanAllDayCard(tag)
                is NoteItem -> NoteAllDayCard(tag)
                is PlannedTaskProjection -> TaskAllDayCard(tag.task)
            }
        },
        timedItemContent = { item, isSelected, displayMode ->
            when (val tag = item.tag) {
                is DailyPlanItem -> DailyPlanTimelineCard(
                    item = tag,
                    selected = isSelected,
                    modifier = Modifier.matchParentSize(),
                    displayMode = displayMode,
                    isOverdue = tag.isOverdue(date)
                )
                is NoteItem -> NoteTimelineCard(tag, selected = isSelected, modifier = Modifier.matchParentSize())
                is PlannedTaskProjection -> TaskTimelineCard(
                    task = tag.task,
                    timeLabel = tag.dailyPlanItem.dailyPlanTimeLabel(),
                    selected = isSelected,
                    completed = tag.dailyPlanItem.isDone(),
                    modifier = Modifier.matchParentSize(),
                    isOverdue = tag.dailyPlanItem.isOverdue(date),
                    displayMode = displayMode
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun MyDayBoard(
    state: MyDayUiState,
    onItemClick: (DailyPlanItem) -> Unit,
    onTaskClick: (TaskItem, DailyPlanItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    val plannedTasksByDailyItemId = remember(state.items, state.board, state.today) {
        state.items
            .toTaskViewProjection(board = state.board, date = state.today)
            .plannedTasks
            .associateBy { it.dailyPlanItem.id }
    }
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
                    plannedTask = plannedTasksByDailyItemId[item.id],
                    onItemClick = onItemClick,
                    onTaskClick = onTaskClick
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
                    plannedTask = plannedTasksByDailyItemId[item.id],
                    onItemClick = onItemClick,
                    onTaskClick = onTaskClick
                )
            }
        }
    }
}

@Composable
private fun MyDayBoardItem(
    item: DailyPlanItem,
    plannedTask: PlannedTaskProjection?,
    onItemClick: (DailyPlanItem) -> Unit,
    onTaskClick: (TaskItem, DailyPlanItem?) -> Unit
) {
    if (plannedTask != null) {
        val task = plannedTask.task
        TaskTimelineCard(
            task = task,
            timeLabel = plannedTask.dailyPlanItem.dailyPlanTimeLabel(),
            completed = plannedTask.dailyPlanItem.isDone(),
            onClick = { onTaskClick(task, plannedTask.dailyPlanItem) }
        )
    } else {
        DailyPlanTimelineCard(
            item = item,
            onClick = { onItemClick(item) },
            isOverdue = item.isOverdue(today())
        )
    }
}

private fun DailyPlanItem.dailyPlanTimeLabel(): String? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes
    return if (end == null) start.toClockLabel() else "${start.toClockLabel()} - ${end.toClockLabel()}"
}

private fun DailyPlanItem.isDone(): Boolean = status == DailyPlanItemStatus.Done

private fun MyDayTaskViewProjection.toTimelineItems(
    date: LocalDate? = null,
    resizable: Boolean = false
): List<TimelineItem> {
    val tasks = plannedTasks.map { plannedTask ->
        val item = plannedTask.dailyPlanItem
        TimelineItem(
            id = "daily-task-${item.id}",
            type = TimelineItemType.Task,
            date = date,
            startTimeMinutes = item.startTimeMinutes,
            endTimeMinutes = item.endTimeMinutes,
            sortOrder = item.sortOrder,
            isResizable = resizable,
            tag = plannedTask
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
    val checkInItems = checkIns.map { checkIn ->
        TimelineItem(
            id = "checkin-${checkIn.id}",
            type = TimelineItemType.CheckIn,
            date = date,
            startTimeMinutes = checkIn.startTimeMinutes,
            endTimeMinutes = checkIn.endTimeMinutes,
            sortOrder = checkIn.sortOrder,
            isResizable = resizable && checkIn.source.hasEndTime(),
            tag = checkIn
        )
    }
    return (tasks + noteItems + checkInItems)
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
