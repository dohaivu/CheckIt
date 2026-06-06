package com.checkit.ui.myday

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notes
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.DailyPlanItemEditorState
import com.checkit.ui.MyDayUiState
import com.checkit.ui.MyDayView
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.tasks.TaskAgendaView
import com.checkit.ui.tasks.TaskCard
import com.checkit.ui.tasks.TaskTimelineView
import com.checkit.ui.tasks.taskCardColor
import com.checkit.ui.tasks.timeRangeLabel
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyDayScreen(
    viewModel: MyDayViewModel,
    onTaskClick: (TaskItem) -> Unit,
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
                    onItemClick = viewModel::openItemEditor,
                    modifier = Modifier.weight(1f)
                )
                MyDayView.Timeline -> MyDayTimeline(
                    state = state,
                    onItemClick = viewModel::openItemEditor,
                    onTaskTimeChange = viewModel::updateItemTime,
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
            onAddTask = viewModel::addTask,
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
            onStartTimeChange = viewModel::updateStartTime,
            onEndTimeChange = viewModel::updateEndTime,
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
    modifier: Modifier = Modifier
) {
    val projection = remember(state.items, state.board, state.today) { state.toTaskViewProjection() }
    TaskAgendaView(
        tasks = projection.tasks,
        notes = projection.notes,
        lists = projection.lists,
        showListName = false,
        onTaskClick = { task -> projection.dailyItemFor(task)?.let(onItemClick) },
        onNoteClick = { note -> projection.dailyItemFor(note)?.let(onItemClick) },
        dayLimit = 1,
        modifier = modifier
    )
}

@Composable
private fun MyDayTimeline(
    state: MyDayUiState,
    onItemClick: (DailyPlanItem) -> Unit,
    onTaskTimeChange: (DailyPlanItem, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val projection = remember(state.items, state.board, state.today) { state.toTaskViewProjection() }
    TaskTimelineView(
        tasks = projection.tasks,
        notes = projection.notes,
        lists = projection.lists,
        showListName = false,
        onTaskClick = { task -> projection.dailyItemFor(task)?.let(onItemClick) },
        onNoteClick = { note -> projection.dailyItemFor(note)?.let(onItemClick) },
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
    if (item.taskId != null) {
        TaskCard(
            title = item.titleSnapshot.ifBlank { "Untitled task" },
            timeLabel = item.timeLabel(),
            supportingText = item.note?.takeIf { it.isNotBlank() },
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
            { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            null
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
            supportingText = task.dueDate?.localizedCompactDateWithDayName() ?: list?.name,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyPlanItemEditorSheet(
    state: DailyPlanItemEditorState,
    onDismiss: () -> Unit,
    onDoneTitleChange: (String) -> Unit,
    onDoneNoteChange: (String) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onSave: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    onOpenTask: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            DailyPlanItemSheetHeader(
                title = if (state.isAddMode) "Log today" else "My Day item",
                onDismiss = onDismiss,
                onSave = onSave
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = onDoneTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Done outside the app") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = onDoneNoteChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Details") },
                        minLines = 2
                    )
                }
                item {
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
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.canDelete) {
                            TextButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (onOpenTask != null) {
                            TextButton(onClick = onOpenTask) {
                                Text("Open Task")
                            }
                        }
                        if (state.canDelete) {
                            Button(onClick = onDone) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyPlanItemSheetHeader(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Button(onClick = onSave) {
            Text("Save")
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
    val dailyItemBySyntheticNoteId: Map<Long, DailyPlanItem>
) {
    fun dailyItemFor(task: TaskItem): DailyPlanItem? = dailyItemBySyntheticTaskId[task.id]
    fun dailyItemFor(note: NoteItem): DailyPlanItem? = dailyItemBySyntheticNoteId[note.id]
}

private fun MyDayUiState.toTaskViewProjection(): MyDayTaskViewProjection {
    val fallbackList = TaskList(
        id = MyDayListId,
        name = "My Day",
        color = "#64748B",
        icon = "Today",
        sortOrder = 0
    )
    val lists = board.lists + fallbackList
    val listId = fallbackList.id
    val realTasksById = board.tasks.associateBy { it.id }
    val dailyItemBySyntheticTaskId = mutableMapOf<Long, DailyPlanItem>()
    val dailyItemBySyntheticNoteId = mutableMapOf<Long, DailyPlanItem>()
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
                dailyItemBySyntheticNoteId[item.id] = item
            }
            else -> {
                val realTask = item.taskId?.let { realTasksById[it] }
                val projectedTask = realTask?.copy(
                    id = item.id,
                    dueDate = today,
                    startTimeMinutes = item.startTimeMinutes,
                    endTimeMinutes = item.endTimeMinutes,
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
                    startTimeMinutes = item.startTimeMinutes,
                    endTimeMinutes = item.endTimeMinutes,
                    durationMinutes = item.durationMinutes(),
                    sortOrder = item.sortOrder,
                    createdAtMillis = item.addedAtMillis,
                    updatedAtMillis = item.completedAtMillis ?: item.addedAtMillis
                )
                projectedTasks += projectedTask
                dailyItemBySyntheticTaskId[projectedTask.id] = item
            }
        }
    }

    return MyDayTaskViewProjection(
        tasks = projectedTasks,
        notes = projectedNotes,
        lists = lists,
        dailyItemBySyntheticTaskId = dailyItemBySyntheticTaskId,
        dailyItemBySyntheticNoteId = dailyItemBySyntheticNoteId
    )
}

private fun DailyPlanItemStatus.toTaskStatus(): TaskStatus = when (this) {
    DailyPlanItemStatus.Planned -> TaskStatus.Open
    DailyPlanItemStatus.Done -> TaskStatus.Completed
}

private fun DailyPlanItem.durationMinutes(): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
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
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes
    return if (end == null) start.toClockLabel() else "${start.toClockLabel()} - ${end.toClockLabel()}"
}

private fun DailyPlanItem.displayTitle(): String =
    when (source) {
        DailyPlanItemSource.CheckInNote -> note.orEmpty().ifBlank { "Empty note" }
        else -> titleSnapshot.ifBlank { "Untitled item" }
    }

private fun DailyPlanItem.displaySupportingText(): String =
    when {
        source == DailyPlanItemSource.CheckInNote -> source.label()
        !note.isNullOrBlank() -> note.orEmpty()
        else -> source.label()
    }

private fun DailyPlanItemSource.label(): String = when (this) {
    DailyPlanItemSource.ExistingTask -> "Task"
    DailyPlanItemSource.QuickTask -> "Quick task"
    DailyPlanItemSource.CheckInManualDone -> "CheckIn done"
    DailyPlanItemSource.CheckInNote -> "CheckIn note"
}

private fun dailyItemColor(task: TaskItem?, list: TaskList?): Color =
    list?.color?.parseHexColorOrNull()
        ?: task?.priorityColor()
        ?: Color(0xFF64748B)

private fun TaskItem.priorityColor(): Color = when (priority.name) {
    "High" -> Color(0xFFDC2626)
    "Medium" -> Color(0xFFCA8A04)
    "Low" -> Color(0xFF2563EB)
    else -> Color(0xFF64748B)
}

private fun String.parseHexColorOrNull(): Color? =
    removePrefix("#")
        .toIntOrNull(16)
        ?.let { rgb ->
            Color(
                red = ((rgb shr 16) and 0xFF) / 255f,
                green = ((rgb shr 8) and 0xFF) / 255f,
                blue = (rgb and 0xFF) / 255f
            )
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
