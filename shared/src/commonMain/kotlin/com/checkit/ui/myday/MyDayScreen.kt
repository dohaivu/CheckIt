package com.checkit.ui.myday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskStatus
import com.checkit.ui.MyDayUiState
import com.checkit.ui.MyDayView
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.tasks.AgendaView
import com.checkit.ui.tasks.TaskCard
import com.checkit.ui.tasks.TaskStatusIcon
import com.checkit.ui.tasks.TimelineView
import com.checkit.ui.tasks.TimelineItem
import com.checkit.ui.tasks.TimelineItemType
import com.checkit.ui.tasks.taskCardColor
import com.checkit.ui.tasks.timeRangeLabel
import com.checkit.ui.tasks.toColor
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyDayScreen(
    viewModel: MyDayViewModel,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onNoteTimeChange: (NoteItem, Int) -> Unit,
    onCreateTask: () -> Unit,
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
            MyDayViewSelector(
                selectedView = state.selectedView,
                onSelect = viewModel::selectView
            )
            DayLinearTimeline(
                items = state.items,
                board = state.board,
                modifier = Modifier.fillMaxWidth()
            )
            when (state.selectedView) {
                MyDayView.Agenda -> MyDayAgenda(
                    state = state,
                    onItemClick = { viewModel.openItemEditor(it, state.today) },
                    onNoteClick = onNoteClick,
                    modifier = Modifier.weight(1f)
                )
                MyDayView.Timeline -> MyDayTimeline(
                    state = state,
                    onItemClick = { viewModel.openItemEditor(it, state.today) },
                    onNoteClick = onNoteClick,
                    onCreateTask = viewModel::createFromTimelineRange,
                    onItemTimeChange = viewModel::updateItemTime,
                    onNoteTimeChange = onNoteTimeChange,
                    modifier = Modifier.weight(1f)
                )
                MyDayView.Board -> MyDayBoard(
                    state = state,
                    onItemClick = { viewModel.openItemEditor(it, state.today) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (state.showSuggestions) {
        SuggestionsSheet(
            tasks = state.suggestedTasks,
            lists = state.board.lists,
            onDismiss = viewModel::dismissSuggestions,
            onTaskClick = onTaskClick,
            onAddTask = viewModel::addTaskFromSuggestion,
            onCreateTask = {
                viewModel.dismissSuggestions()
                onCreateTask()
            }
        )
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
private fun MyDayAgenda(
    state: MyDayUiState,
    onItemClick: (DailyPlanItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    DailyPlanAgenda(
        items = state.items,
        board = state.board,
        date = state.today,
        onItemClick = onItemClick,
        onNoteClick = onNoteClick,
        modifier = modifier
    )
}

@Composable
internal fun DailyPlanAgenda(
    items: List<DailyPlanItem>,
    board: TaskBoard,
    date: LocalDate,
    onItemClick: (DailyPlanItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val projection = remember(items, board, date) { items.toTaskViewProjection(board = board, date = date) }
    val timelineItems = remember(projection, date) {
        val tasks = projection.tasks.map { task ->
            TimelineItem(
                id = "task-${task.id}",
                type = TimelineItemType.Task,
                date = date,
                startTimeMinutes = task.startTimeMinutes,
                endTimeMinutes = task.endTimeMinutes,
                sortOrder = task.sortOrder,
                tag = task
            )
        }
        val notes = projection.notes.map { note ->
            TimelineItem(
                id = "note-${note.id}",
                type = TimelineItemType.Note,
                date = date,
                startTimeMinutes = note.startTimeMinutes,
                endTimeMinutes = note.startTimeMinutes?.let { it + 30 },
                sortOrder = note.sortOrder,
                tag = note
            )
        }
        val checkIns = projection.checkIns.map { checkIn ->
            TimelineItem(
                id = "checkin-${checkIn.id}",
                type = TimelineItemType.CheckIn,
                date = date,
                startTimeMinutes = checkIn.startTimeMinutes,
                endTimeMinutes = checkIn.endTimeMinutes,
                sortOrder = checkIn.sortOrder,
                tag = checkIn
            )
        }
        (tasks + notes + checkIns).sortedWith(compareBy<TimelineItem> { it.startTimeMinutes ?: -1 }.thenBy { it.sortOrder })
    }

    AgendaView(
        items = timelineItems,
        onItemClick = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> onItemClick(tag)
                is NoteItem -> onNoteClick(tag)
                is TaskItem -> {
                    // Find the DailyPlanItem for this task
                    val dailyItem = items.find { it.taskId == tag.id }
                    if (dailyItem != null) onItemClick(dailyItem)
                }
            }
        },
        dayLimit = 1,
        focusedDate = date,
        itemContent = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> {
                    val list = projection.lists.find { it.id == tag.taskId?.let { tid -> board.tasks.find { t -> t.id == tid }?.listId } }
                    DailyPlanCard(
                        item = tag
                    )
                }
                is NoteItem -> {
                    val list = projection.lists.find { it.id == tag.listId }
                    TaskCard(
                        title = tag.content.ifBlank { "Empty note" },
                        supportingText = list?.name,
                        color = list?.color?.toColor() ?: MaterialTheme.colorScheme.secondary,
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        completed = tag.status == TaskStatus.Completed
                    )
                }
                is TaskItem -> {
                    val list = projection.lists.find { it.id == tag.listId }
                    TaskCard(
                        title = tag.name,
                        color = taskCardColor(tag, list),
                        timeLabel = tag.timeRangeLabel(),
                        leadingContent = { TaskStatusIcon(tag.status, tag.priority) }
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun MyDayTimeline(
    state: MyDayUiState,
    onItemClick: (DailyPlanItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onCreateTask: (Int, Int) -> Unit,
    onItemTimeChange: (DailyPlanItem, Int, Int) -> Unit,
    onNoteTimeChange: (NoteItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val projection = remember(state.items, state.board, state.today) {
        state.items.toTaskViewProjection(board = state.board, date = state.today)
    }
    val timelineItems = remember(projection) {
        val tasks = projection.tasks.map { task ->
            TimelineItem(
                id = "task-${task.id}",
                type = TimelineItemType.Task,
                startTimeMinutes = task.startTimeMinutes,
                endTimeMinutes = task.endTimeMinutes,
                sortOrder = task.sortOrder,
                isResizable = true,
                tag = task
            )
        }
        val notes = projection.notes.map { note ->
            TimelineItem(
                id = "note-${note.id}",
                type = TimelineItemType.Note,
                startTimeMinutes = note.startTimeMinutes,
                endTimeMinutes = note.startTimeMinutes?.let { it + 30 },
                sortOrder = note.sortOrder,
                isResizable = false,
                tag = note
            )
        }
        val checkIns = projection.checkIns.map { checkIn ->
            TimelineItem(
                id = "checkin-${checkIn.id}",
                type = TimelineItemType.CheckIn,
                startTimeMinutes = checkIn.startTimeMinutes,
                endTimeMinutes = checkIn.endTimeMinutes,
                sortOrder = checkIn.sortOrder,
                isResizable = checkIn.source != DailyPlanItemSource.CheckInNote,
                tag = checkIn
            )
        }
        (tasks + notes + checkIns).sortedWith(compareBy<TimelineItem> { it.startTimeMinutes ?: -1 }.thenBy { it.sortOrder })
    }

    TimelineView(
        items = timelineItems,
        onItemClick = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> onItemClick(tag)
                is NoteItem -> onNoteClick(tag)
                is TaskItem -> {
                    val dailyItem = state.items.find { it.taskId == tag.id }
                    if (dailyItem != null) onItemClick(dailyItem)
                }
            }
        },
        onCreateRequest = onCreateTask,
        onTimeChange = { item, start, end ->
            when (val tag = item.tag) {
                is DailyPlanItem -> onItemTimeChange(tag, start, end)
                is NoteItem -> onNoteTimeChange(tag, start)
                is TaskItem -> {
                    val dailyItem = state.items.find { it.taskId == tag.id }
                    if (dailyItem != null) onItemTimeChange(dailyItem, start, end)
                }
            }
        },
        allDayItemContent = { item ->
            when (val tag = item.tag) {
                is DailyPlanItem -> DailyPlanCard(item = tag, onClick = { onItemClick(tag) })
                is NoteItem -> { /* handle if needed */ }
                is TaskItem -> {
                    val dailyItem = state.items.find { it.taskId == tag.id }
                    if (dailyItem != null) {
                        DailyPlanCard(item = dailyItem, onClick = { onItemClick(dailyItem) })
                    }
                }
            }
        },
        timedItemContent = { item, isSelected ->
            when (val tag = item.tag) {
                is DailyPlanItem -> {
                    val task = projection.tasks.find { it.id == tag.taskId }
                    val list = projection.lists.find { it.id == task?.listId }
                    DailyPlanCard(
                        item = tag,
                        modifier = Modifier.matchParentSize()
                    )
                }
                is NoteItem -> {
                    val list = projection.lists.find { it.id == tag.listId }
                    TaskCard(
                        title = tag.content.ifBlank { "Empty note" },
                        supportingText = list?.name,
                        color = list?.color?.toColor() ?: MaterialTheme.colorScheme.secondary,
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        completed = tag.status == TaskStatus.Completed,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                is TaskItem -> {
                    val list = projection.lists.find { it.id == tag.listId }
                    TaskCard(
                        title = tag.name,
                        color = taskCardColor(tag, list),
                        timeLabel = tag.timeRangeLabel(),
                        leadingContent = { TaskStatusIcon(tag.status, tag.priority) },
                        modifier = Modifier.matchParentSize(),
                        containerAlpha = if (isSelected) 0.28f else 0.17f
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun MyDayBoard(
    state: MyDayUiState,
    onItemClick: (DailyPlanItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val taskById = remember(state.board.tasks) { state.board.tasks.associateBy { it.id } }
    val listById = remember(state.board.lists) { state.board.lists.associateBy { it.id } }
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
                if (item.taskId != null) {
                    val task = item.taskId?.let { taskById[it] }
                    val list = item.taskId?.let { taskById[it] }?.let { task -> listById[task.listId] }
                    if (task != null) {
                        TaskCard(
                            title = task.name,
                            color = taskCardColor(task, list),
                            timeLabel = task.timeRangeLabel(),
                            leadingContent = { TaskStatusIcon(task.status, task.priority) }
                        )
                    }
                } else {
                    DailyPlanCard(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                }

            }
        }
        item { SectionLabel("Done") }
        if (state.doneItems.isEmpty()) {
            item { EmptyStateText("Nothing done yet") }
        } else {
            items(state.doneItems, key = { "done-${it.id}" }) { item ->
                if (item.taskId != null) {
                    val task = item.taskId?.let { taskById[it] }
                    val list = item.taskId?.let { taskById[it] }?.let { task -> listById[task.listId] }
                    if (task != null) {
                        TaskCard(
                            title = task.name,
                            color = taskCardColor(task, list),
                            timeLabel = task.timeRangeLabel(),
                            leadingContent = { TaskStatusIcon(task.status, task.priority) },
                            completed = true
                        )
                    }
                } else {
                    DailyPlanCard(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun DailyPlanCard(
    item: DailyPlanItem,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDone = item.status == DailyPlanItemStatus.Done

    TaskCard(
        title = item.displayTitle(),
        timeLabel = item.timeLabel(),
        supportingText = item.displaySupportingText(),
        color = MyDayListColor,
        leadingContent = if (item.source == DailyPlanItemSource.CheckInNote) {
            { Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            { DailyPlanStatusIcon(item.status) }
        },
        completed = isDone,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun SuggestionCard(
    task: TaskItem,
    list: TaskList?,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        TaskCard(
            title = task.name.ifBlank { "Untitled task" },
            timeLabel = task.timeRangeLabel(),
            supportingText = task.doDate?.localizedCompactDateWithDayName() ?: list?.name,
            color = taskCardColor(task, list),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        )
        IconButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionsSheet(
    tasks: List<TaskItem>,
    lists: List<TaskList>,
    onDismiss: () -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    onAddTask: (TaskItem) -> Unit,
    onCreateTask: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listById = remember(lists) { lists.associateBy { it.id } }
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        sheetState = sheetState,
        sheetGesturesEnabled = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add to My Day",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Button(onClick = onCreateTask) {
                    Text("Add")
                }
            }
            if (tasks.isEmpty()) {
                EmptyStateText("No suggested tasks")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        SuggestionCard(
                            task = task,
                            list = listById[task.listId],
                            onClick = { onTaskClick(task) },
                            onAdd = { onAddTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EmptyStateText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
internal fun DailyPlanStatusIcon(status: DailyPlanItemStatus) {
    Icon(
        imageVector = if (status == DailyPlanItemStatus.Done) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

private fun MyDayView.icon(): ImageVector = when (this) {
    MyDayView.Agenda -> Icons.Default.ViewAgenda
    MyDayView.Timeline -> Icons.Default.Schedule
    MyDayView.Board -> Icons.Default.Dashboard
}

private fun MyDayView.label(): String = when (this) {
    MyDayView.Agenda -> "Agenda"
    MyDayView.Timeline -> "Timeline"
    MyDayView.Board -> "Board"
}

internal val MyDayListColor = "#64748B".toColor()