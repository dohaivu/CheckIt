package com.checkit.ui.myday

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.checkit.ui.DailyPlanItemEditorState
import com.checkit.ui.EditorMode
import com.checkit.ui.MyDayUiState
import com.checkit.ui.MyDayView
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.tasks.TaskAgendaView
import com.checkit.ui.tasks.TaskCard
import com.checkit.ui.tasks.TaskTimelineView
import com.checkit.ui.tasks.DetailChip
import com.checkit.ui.tasks.DurationText
import com.checkit.ui.tasks.TimeRangeDetailChip
import com.checkit.ui.tasks.TimePickerRow
import com.checkit.ui.tasks.editorTextFieldColors
import com.checkit.ui.tasks.taskCardColor
import com.checkit.ui.tasks.timeRangeLabel
import com.checkit.ui.tasks.toClockLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

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
            DayLinearTimeline(
                items = state.items,
                board = state.board,
                modifier = Modifier.fillMaxWidth()
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
            onEdit = viewModel::editItemEditor,
            onSave = viewModel::saveCheckIn,
            onDone = viewModel::markEditorDone,
            onDelete = viewModel::deleteEditorItem,
            onOpenTask = task?.let { { onTaskClick(it) } }
        )
    }
}

@Composable
internal fun DayLinearTimeline(
    items: List<DailyPlanItem>,
    board: TaskBoard,
    modifier: Modifier = Modifier
) {
    val blocks = remember(items, board) { items.toDayTimelineBlocks(board) }
    val workMinutes = remember(blocks) { blocks.totalOccupiedMinutes() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val corner = size.height / 2f
            drawRoundRect(
                color = DayTimelineTrackColor,
                cornerRadius = CornerRadius(corner, corner),
                size = size
            )
            blocks.forEach { block ->
                val startFraction = (block.startMinutes - DayTimelineStartMinutes).toFloat() / DayTimelineTotalMinutes
                val widthFraction = (block.endMinutes - block.startMinutes).toFloat() / DayTimelineTotalMinutes
                drawRoundRect(
                    color = block.color,
                    topLeft = Offset(x = size.width * startFraction, y = 0f),
                    size = Size(width = size.width * widthFraction, height = size.height),
                    cornerRadius = CornerRadius(corner, corner)
                )
            }
        }
        Text(
            text = "Total work: ${workMinutes.toDurationLabel()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
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
    DailyPlanAgenda(
        items = state.items,
        board = state.board,
        date = state.today,
        onItemClick = onItemClick,
        modifier = modifier
    )
}

@Composable
internal fun DailyPlanAgenda(
    items: List<DailyPlanItem>,
    board: TaskBoard,
    date: LocalDate,
    onItemClick: (DailyPlanItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val projection = remember(items, board, date) { items.toTaskViewProjection(board = board, date = date) }
    TaskAgendaView(
        tasks = projection.tasks,
        notes = projection.notes,
        lists = projection.lists,
        showListName = false,
        onTaskClick = { task -> projection.dailyItemFor(task)?.let(onItemClick) },
        onNoteClick = { note -> projection.dailyItemFor(note)?.let(onItemClick) },
        dayLimit = 1,
        focusedDate = date,
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
    val projection = remember(state.items, state.board, state.today) {
        state.items.toTaskViewProjection(board = state.board, date = state.today)
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyPlanItemEditorSheet(
    state: DailyPlanItemEditorState,
    onDismiss: () -> Unit,
    onDoneTitleChange: (String) -> Unit,
    onDoneNoteChange: (String) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onEdit: () -> Unit,
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
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            DailyPlanItemSheetHeader(
                state = state,
                onDismiss = onDismiss,
                onEdit = onEdit,
                onSave = onSave,
                onDelete = onDelete
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    if (state.isViewMode) {
                        DailyPlanItemViewContent(state = state, hasTask = onOpenTask != null)
                    } else {
                        DailyPlanItemFormContent(
                            state = state,
                            onDoneTitleChange = onDoneTitleChange,
                            onDoneNoteChange = onDoneNoteChange,
                            onStartTimeChange = onStartTimeChange,
                            onEndTimeChange = onEndTimeChange
                        )
                    }
                }
                if (state.isViewMode) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onOpenTask != null) {
                                TextButton(onClick = onOpenTask) {
                                    Text("Open Task")
                                }
                            }
                            if (state.status != DailyPlanItemStatus.Done) {
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
}

@Composable
private fun DailyPlanItemSheetHeader(
    state: DailyPlanItemEditorState,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }

        Text(
            text = when (state.mode) {
                EditorMode.Add -> "Add done"
                EditorMode.View -> "My Day item"
                EditorMode.Edit -> "Edit item"
            },
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            if (state.isViewMode) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
            if (state.canDelete && state.isViewMode) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            if (!state.isViewMode) {
                Button(onClick = onSave) {
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyPlanItemViewContent(
    state: DailyPlanItemEditorState,
    hasTask: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = state.title.ifBlank { "Untitled item" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        state.note.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailChip(Icons.Default.Event, "My Day")
            TimeRangeDetailChip(state.startTimeMinutes, state.endTimeMinutes)
            DetailChip(Icons.Default.CheckCircle, if (state.status == DailyPlanItemStatus.Done) "Done" else "Planned")
            if (hasTask) {
                DetailChip(Icons.Default.TaskAlt, "Task")
            }
        }
    }
}

@Composable
private fun DailyPlanItemFormContent(
    state: DailyPlanItemEditorState,
    onDoneTitleChange: (String) -> Unit,
    onDoneNoteChange: (String) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        OutlinedTextField(
            value = state.title,
            onValueChange = onDoneTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Done outside the app") },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            shape = MaterialTheme.shapes.medium,
            colors = editorTextFieldColors()
        )
        OutlinedTextField(
            value = state.note,
            onValueChange = onDoneNoteChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Add details") },
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            colors = editorTextFieldColors()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimePickerRow(
                label = "Start",
                timeMinutes = state.startTimeMinutes,
                initialTimeMinutes = currentMyDayTimeMinutes(),
                onTimeChange = onStartTimeChange,
                modifier = Modifier.weight(1f)
            )
            state.durationMinutes()?.let { duration ->
                DurationText(
                    duration = duration,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            TimePickerRow(
                label = "End",
                timeMinutes = state.endTimeMinutes,
                initialTimeMinutes = ((state.startTimeMinutes ?: currentMyDayTimeMinutes()) + 60)
                    .coerceAtMost(MyDayMinutesPerDay - 1),
                enabled = state.startTimeMinutes != null,
                onTimeChange = onEndTimeChange,
                modifier = Modifier.weight(1f)
            )
        }
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

private fun List<DailyPlanItem>.toTaskViewProjection(
    board: TaskBoard,
    date: LocalDate
): MyDayTaskViewProjection {
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

    forEach { item ->
        when (item.source) {
            DailyPlanItemSource.CheckInNote -> {
                projectedNotes += NoteItem(
                    id = item.id,
                    listId = listId,
                    content = item.note.orEmpty(),
                    status = item.status.toTaskStatus(),
                    date = date,
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
                    doDate = date,
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
                    doDate = date,
                    completedDate = date.takeIf { item.status == DailyPlanItemStatus.Done },
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

private fun DailyPlanItemEditorState.durationMinutes(): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
    return (end - start).takeIf { it >= 0 }
}

private data class DayTimelineBlock(
    val startMinutes: Int,
    val endMinutes: Int,
    val color: Color
)

private fun List<DailyPlanItem>.toDayTimelineBlocks(board: TaskBoard): List<DayTimelineBlock> {
    val tasksById = board.tasks.associateBy { it.id }
    val listsById = board.lists.associateBy { it.id }
    return mapNotNull { item ->
        val start = item.startTimeMinutes ?: return@mapNotNull null
        val end = item.endTimeMinutes ?: return@mapNotNull null
        val clippedStart = start.coerceIn(DayTimelineStartMinutes, DayTimelineEndMinutes)
        val clippedEnd = end.coerceIn(DayTimelineStartMinutes, DayTimelineEndMinutes)
        if (clippedEnd <= clippedStart) {
            null
        } else {
            val task = item.taskId?.let { tasksById[it] }
            val list = task?.let { listsById[it.listId] }
            DayTimelineBlock(
                startMinutes = clippedStart,
                endMinutes = clippedEnd,
                color = dailyItemColor(task, list)
            )
        }
    }
}

private fun List<DayTimelineBlock>.totalOccupiedMinutes(): Int {
    if (isEmpty()) return 0
    val sorted = sortedBy { it.startMinutes }
    var total = 0
    var currentStart = sorted.first().startMinutes
    var currentEnd = sorted.first().endMinutes
    sorted.drop(1).forEach { block ->
        if (block.startMinutes <= currentEnd) {
            currentEnd = maxOf(currentEnd, block.endMinutes)
        } else {
            total += currentEnd - currentStart
            currentStart = block.startMinutes
            currentEnd = block.endMinutes
        }
    }
    return total + currentEnd - currentStart
}

private fun Int.toDurationLabel(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours == 0 -> "${minutes}m"
        minutes == 0 -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

private fun currentMyDayTimeMinutes(): Int {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return now.hour * 60 + now.minute
}

private fun MyDayView.icon(): ImageVector = when (this) {
    MyDayView.Agenda -> Icons.Default.ViewAgenda
    MyDayView.Timeline -> Icons.Default.Schedule
    MyDayView.Board -> Icons.Default.Dashboard
}

private const val MyDayMinutesPerDay = 24 * 60
private const val MyDayListId = -10_000L
private const val DayTimelineStartMinutes = 6 * 60
private const val DayTimelineEndMinutes = 22 * 60
private const val DayTimelineTotalMinutes = DayTimelineEndMinutes - DayTimelineStartMinutes
private val DayTimelineTrackColor = Color(0xFFE5E7EB)

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
