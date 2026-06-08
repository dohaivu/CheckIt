package com.checkit.ui.myday

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Notes
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.MyDayUiState
import com.checkit.ui.MyDayView
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.tasks.TaskAgendaView
import com.checkit.ui.tasks.TaskCard
import com.checkit.ui.tasks.TaskStatusIcon
import com.checkit.ui.tasks.TaskTimelineView
import com.checkit.ui.tasks.priorityColor
import com.checkit.ui.tasks.taskCardColor
import com.checkit.ui.tasks.timeRangeLabel
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
                        Icon(Icons.Default.Add, contentDescription = "Add to My Day")
                    }
                    TextButton(onClick = viewModel::openCheckIn) {
                        Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("CheckIn")
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
                    onItemClick = viewModel::openItemEditor,
                    onNoteClick = onNoteClick,
                    modifier = Modifier.weight(1f)
                )
                MyDayView.Timeline -> MyDayTimeline(
                    state = state,
                    onItemClick = viewModel::openItemEditor,
                    onNoteClick = onNoteClick,
                    onCreateTask = viewModel::createFromTimelineRange,
                    onItemTimeChange = viewModel::updateItemTime,
                    onNoteTimeChange = onNoteTimeChange,
                    modifier = Modifier.weight(1f)
                )
                MyDayView.Board -> MyDayBoard(
                    state = state,
                    onItemClick = viewModel::openItemEditor,
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

    state.itemEditor?.let { editor ->
        val task = editor.taskId?.let { taskId -> state.board.tasks.firstOrNull { it.id == taskId } }
        DailyPlanItemEditorSheet(
            state = editor,
            onDismiss = viewModel::dismissCheckIn,
            onDoneTitleChange = viewModel::updateDoneTitle,
            onDoneNoteChange = viewModel::updateDoneNote,
            onSourceChange = viewModel::updateEditorSource,
            onStartTimeChange = viewModel::updateStartTime,
            onEndTimeChange = viewModel::updateEndTime,
            onEdit = viewModel::editItemEditor,
            onSave = viewModel::saveCheckIn,
            onDone = viewModel::markEditorDone,
            onDelete = viewModel::deleteEditorItem,
            onOpenTask = task?.let { { onTaskClick(it) } }
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
                label = { Text(view.label()) }
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
    TaskAgendaView(
        tasks = projection.tasks,
        notes = projection.notes,
        lists = projection.lists,
        showListName = false,
        onTaskClick = { task -> projection.dailyItemFor(task)?.let(onItemClick) },
        onNoteClick = { note -> projection.dailyItemFor(note)?.let(onItemClick) ?: onNoteClick(note) },
        dayLimit = 1,
        focusedDate = date,
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
    TaskTimelineView(
        tasks = projection.tasks,
        notes = projection.notes,
        lists = projection.lists,
        showListName = false,
        onTaskClick = { task -> projection.dailyItemFor(task)?.let(onItemClick) },
        onNoteClick = { note -> projection.dailyItemFor(note)?.let(onItemClick) ?: onNoteClick(note) },
        onCreateTask = onCreateTask,
        onTaskTimeChange = { task, start, end ->
            projection.dailyItemFor(task)?.let { onItemTimeChange(it, start, end) }
        },
        onNoteTimeChange = { note, start ->
            projection.dailyItemFor(note)?.let { onItemTimeChange(it, start, start + 30) } ?: onNoteTimeChange(note, start)
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
                DailyPlanCard(
                    item = item,
                    task = item.taskId?.let { taskById[it] },
                    list = item.taskId?.let { taskById[it] }?.let { task -> listById[task.listId] },
                    onClick = { onItemClick(item) }
                )
            }
        }
        item { SectionLabel("Done") }
        if (state.doneItems.isEmpty()) {
            item { EmptyStateText("Nothing done yet") }
        } else {
            items(state.doneItems, key = { "done-${it.id}" }) { item ->
                DailyPlanCard(
                    item = item,
                    task = item.taskId?.let { taskById[it] },
                    list = item.taskId?.let { taskById[it] }?.let { task -> listById[task.listId] },
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
internal fun DailyPlanCard(
    item: DailyPlanItem,
    task: TaskItem? = null,
    list: TaskList? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDone = item.status == DailyPlanItemStatus.Done
    if (item.taskId != null && task != null) {
        TaskCard(
            title = item.titleSnapshot.ifBlank { "Untitled task" },
            timeLabel = item.timeLabel(),
            supportingText = item.note?.takeIf { it.isNotBlank() },
            leadingContent = { TaskStatusIcon(task.status, task.priority) },
            color = dailyItemColor(task, list),
            completed = isDone,
            onClick = onClick,
            modifier = modifier
        )
        return
    }

    TaskCard(
        title = item.displayTitle(),
        timeLabel = item.timeLabel(),
        supportingText = item.displaySupportingText(),
        color = dailyItemColor(task, list),
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
