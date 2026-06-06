package com.checkit.ui.myday

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.CheckInState
import com.checkit.ui.MyDayUiState
import com.checkit.ui.MyDayView
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.tasks.TaskAgendaView
import com.checkit.ui.tasks.TaskTimelineView
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyDayScreen(
    viewModel: MyDayViewModel,
    onTaskClick: (TaskItem) -> Unit,
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
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MyDayViewSelector(
                selectedView = state.selectedView,
                onSelect = viewModel::selectView
            )
            when (state.selectedView) {
                MyDayView.Agenda -> MyDayAgenda(
                    state = state,
                    onTaskClick = onTaskClick,
                    modifier = Modifier.weight(1f)
                )
                MyDayView.Timeline -> MyDayTimeline(
                    state = state,
                    onTaskClick = onTaskClick,
                    onTaskTimeChange = viewModel::updateItemTime,
                    modifier = Modifier.weight(1f)
                )
                MyDayView.Board -> MyDayBoard(
                    state = state,
                    onToggleDone = { item ->
                        if (item.status == DailyPlanItemStatus.Done) viewModel.markPlanned(item) else viewModel.markDone(item)
                    },
                    onTaskClick = onTaskClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (state.showSuggestions) {
        SuggestionsSheet(
            tasks = state.suggestedTasks,
            onDismiss = viewModel::dismissSuggestions,
            onTaskClick = onTaskClick,
            onAddTask = viewModel::addTask
        )
    }

    state.checkIn?.let { checkIn ->
        CheckInSheet(
            state = checkIn,
            onDismiss = viewModel::dismissCheckIn,
            onDoneTitleChange = viewModel::updateDoneTitle,
            onDoneNoteChange = viewModel::updateDoneNote,
            onStatusNoteChange = viewModel::updateStatusNote,
            onStartTimeChange = viewModel::updateStartTime,
            onEndTimeChange = viewModel::updateEndTime,
            onSave = viewModel::saveCheckIn
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
    onTaskClick: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val projection = remember(state.items, state.board, state.today) { state.toTaskViewProjection() }
    TaskAgendaView(
        tasks = projection.tasks,
        notes = projection.notes,
        lists = projection.lists,
        showListName = false,
        onTaskClick = { task -> projection.realTaskFor(task)?.let(onTaskClick) },
        onNoteClick = { },
        dayLimit = 1,
        modifier = modifier
    )
}

@Composable
private fun MyDayTimeline(
    state: MyDayUiState,
    onTaskClick: (TaskItem) -> Unit,
    onTaskTimeChange: (DailyPlanItem, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val projection = remember(state.items, state.board, state.today) { state.toTaskViewProjection() }
    TaskTimelineView(
        tasks = projection.tasks,
        notes = projection.notes,
        lists = projection.lists,
        showListName = false,
        onTaskClick = { task -> projection.realTaskFor(task)?.let(onTaskClick) },
        onNoteClick = { },
        onCreateTask = { _, _ -> },
        onTaskTimeChange = { task, start, end ->
            projection.dailyItemFor(task)?.let { onTaskTimeChange(it, start, end) }
        },
        modifier = modifier
    )
}

@Composable
private fun MyDayBoard(
    state: MyDayUiState,
    onToggleDone: (DailyPlanItem) -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val taskById = remember(state.board.tasks) { state.board.tasks.associateBy { it.id } }
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
                    onToggleDone = { onToggleDone(item) },
                    onClick = item.taskId?.let { taskId -> { taskById[taskId]?.let(onTaskClick) } }
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
                    onToggleDone = { onToggleDone(item) },
                    onClick = item.taskId?.let { taskId -> { taskById[taskId]?.let(onTaskClick) } }
                )
            }
        }
    }
}

@Composable
internal fun DailyPlanCard(
    item: DailyPlanItem,
    onToggleDone: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDone = item.status == DailyPlanItemStatus.Done
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        color = dailyItemColor(item).copy(alpha = 0.11f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onToggleDone, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (item.source == DailyPlanItemSource.CheckInNote) item.note.orEmpty() else item.titleSnapshot,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.source != DailyPlanItemSource.CheckInNote && !item.note.isNullOrBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = item.timeLabel() ?: item.source.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    task: TaskItem,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).background(task.priorityColor()),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(task.name.ifBlank { "Untitled task" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = task.dueDate?.localizedCompactDateWithDayName() ?: "High priority",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionsSheet(
    tasks: List<TaskItem>,
    onDismiss: () -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    onAddTask: (TaskItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        sheetState = sheetState,
        sheetGesturesEnabled = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add to My Day", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
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
private fun EmptyMyDay() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Plan your day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Choose a few tasks below, then use CheckIn during the day to log what actually happened.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInSheet(
    state: CheckInState,
    onDismiss: () -> Unit,
    onDoneTitleChange: (String) -> Unit,
    onDoneNoteChange: (String) -> Unit,
    onStatusNoteChange: (String) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Log today", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.doneTitle,
                onValueChange = onDoneTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Done outside the app") },
                singleLine = true
            )
            OutlinedTextField(
                value = state.doneNote,
                onValueChange = onDoneNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Details") },
                minLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CheckInTimePickerRow(
                    label = "Start",
                    timeMinutes = state.startTimeMinutes,
                    onTimeChange = onStartTimeChange,
                    modifier = Modifier.weight(1f)
                )
                CheckInTimePickerRow(
                    label = "End",
                    timeMinutes = state.endTimeMinutes,
                    onTimeChange = onEndTimeChange,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = state.statusNote,
                onValueChange = onStatusNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Status note") },
                minLines = 3
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(onClick = onSave) {
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInTimePickerRow(
    label: String,
    timeMinutes: Int?,
    onTimeChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPicker = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    text = timeMinutes?.toClockLabel() ?: "No time",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (timeMinutes != null) {
                IconButton(onClick = { onTimeChange(null) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                }
            }
        }
    }
    if (showPicker) {
        val initial = timeMinutes ?: 9 * 60
        val timePickerState = rememberTimePickerState(
            initialHour = initial / 60,
            initialMinute = initial % 60,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(timePickerState.hour * 60 + timePickerState.minute)
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

private data class MyDayTaskViewProjection(
    val tasks: List<TaskItem>,
    val notes: List<NoteItem>,
    val lists: List<TaskList>,
    val dailyItemBySyntheticTaskId: Map<Long, DailyPlanItem>,
    val realTaskBySyntheticTaskId: Map<Long, TaskItem>
) {
    fun dailyItemFor(task: TaskItem): DailyPlanItem? = dailyItemBySyntheticTaskId[task.id]
    fun realTaskFor(task: TaskItem): TaskItem? = realTaskBySyntheticTaskId[task.id]
}

private fun MyDayUiState.toTaskViewProjection(): MyDayTaskViewProjection {
    val fallbackList = TaskList(
        id = MyDayListId,
        name = "My Day",
        color = "#CA8A04",
        icon = "Today",
        sortOrder = 0
    )
    val lists = if (board.lists.isEmpty()) listOf(fallbackList) else board.lists
    val listId = lists.first().id
    val realTasksById = board.tasks.associateBy { it.id }
    val dailyItemBySyntheticTaskId = mutableMapOf<Long, DailyPlanItem>()
    val realTaskBySyntheticTaskId = mutableMapOf<Long, TaskItem>()
    val projectedTasks = mutableListOf<TaskItem>()
    val projectedNotes = mutableListOf<NoteItem>()

    items.forEach { item ->
        when (item.source) {
            DailyPlanItemSource.CheckInNote -> {
                projectedNotes += NoteItem(
                    id = item.id,
                    listId = listId,
                    content = item.note.orEmpty(),
                    status = item.status.toTaskStatus(),
                    date = today,
                    createdAtMillis = item.addedAtMillis,
                    editedAtMillis = item.completedAtMillis ?: item.addedAtMillis,
                    sortOrder = item.sortOrder
                )
            }
            else -> {
                val realTask = item.taskId?.let { realTasksById[it] }
                val projectedTask = realTask?.copy(
                    id = item.id,
                    dueDate = today,
                    startTimeMinutes = item.plannedStartTimeMinutes,
                    endTimeMinutes = item.plannedEndTimeMinutes,
                    status = item.status.toTaskStatus(),
                    sortOrder = item.sortOrder
                ) ?: TaskItem(
                    id = item.id,
                    listId = listId,
                    name = item.titleSnapshot,
                    description = item.note.orEmpty(),
                    status = item.status.toTaskStatus(),
                    priority = TaskPriority.None,
                    dueDate = today,
                    completedDate = today.takeIf { item.status == DailyPlanItemStatus.Done },
                    startTimeMinutes = item.plannedStartTimeMinutes,
                    endTimeMinutes = item.plannedEndTimeMinutes,
                    durationMinutes = item.durationMinutes(),
                    sortOrder = item.sortOrder,
                    createdAtMillis = item.addedAtMillis,
                    updatedAtMillis = item.completedAtMillis ?: item.addedAtMillis
                )
                projectedTasks += projectedTask
                dailyItemBySyntheticTaskId[projectedTask.id] = item
                realTask?.let { realTaskBySyntheticTaskId[projectedTask.id] = it }
            }
        }
    }

    return MyDayTaskViewProjection(
        tasks = projectedTasks,
        notes = projectedNotes,
        lists = lists,
        dailyItemBySyntheticTaskId = dailyItemBySyntheticTaskId,
        realTaskBySyntheticTaskId = realTaskBySyntheticTaskId
    )
}

private fun DailyPlanItemStatus.toTaskStatus(): TaskStatus = when (this) {
    DailyPlanItemStatus.Planned -> TaskStatus.Open
    DailyPlanItemStatus.InProgress -> TaskStatus.InProgress
    DailyPlanItemStatus.Done -> TaskStatus.Completed
    DailyPlanItemStatus.Skipped,
    DailyPlanItemStatus.Moved -> TaskStatus.Cancelled
}

private fun DailyPlanItem.durationMinutes(): Int? {
    val start = plannedStartTimeMinutes ?: return null
    val end = plannedEndTimeMinutes ?: return null
    return (end - start).takeIf { it >= 0 }
}

private fun MyDayView.icon(): ImageVector = when (this) {
    MyDayView.Agenda -> Icons.Default.ViewAgenda
    MyDayView.Timeline -> Icons.Default.Schedule
    MyDayView.Board -> Icons.Default.Dashboard
}

private const val MyDayListId = -10_000L

private fun MyDayView.label(): String = when (this) {
    MyDayView.Agenda -> "Agenda"
    MyDayView.Timeline -> "Timeline"
    MyDayView.Board -> "Board"
}

private fun DailyPlanItem.timeLabel(): String? {
    val start = plannedStartTimeMinutes ?: return null
    val end = plannedEndTimeMinutes
    return if (end == null) start.toClockLabel() else "${start.toClockLabel()} - ${end.toClockLabel()}"
}

private fun DailyPlanItemSource.label(): String = when (this) {
    DailyPlanItemSource.ExistingTask -> "Task"
    DailyPlanItemSource.QuickTask -> "Quick task"
    DailyPlanItemSource.CheckInManualDone -> "CheckIn done"
    DailyPlanItemSource.CheckInNote -> "CheckIn note"
}

private fun dailyItemColor(item: DailyPlanItem): Color = when (item.status) {
    DailyPlanItemStatus.Done -> Color(0xFF059669)
    DailyPlanItemStatus.InProgress -> Color(0xFF2563EB)
    DailyPlanItemStatus.Skipped,
    DailyPlanItemStatus.Moved -> Color(0xFF64748B)
    DailyPlanItemStatus.Planned -> Color(0xFFCA8A04)
}

private fun TaskItem.priorityColor(): Color = when (priority.name) {
    "High" -> Color(0xFFDC2626)
    "Medium" -> Color(0xFFCA8A04)
    "Low" -> Color(0xFF2563EB)
    else -> Color(0xFF64748B)
}

private fun Int.toClockLabel(): String {
    val hour = this / 60
    val minute = this % 60
    val suffix = if (hour >= 12) "PM" else "AM"
    val displayHour = when (val normalized = hour % 12) {
        0 -> 12
        else -> normalized
    }
    return "$displayHour:${minute.toString().padStart(2, '0')} $suffix"
}
