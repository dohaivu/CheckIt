package com.checkit.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskList
import com.checkit.domain.TaskReminderPreset
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskTag
import com.checkit.ui.EditorMode
import com.checkit.ui.RepeatPreset
import com.checkit.ui.TaskEditorState
import com.checkit.ui.toUtcLocalDate
import com.checkit.ui.toUtcStartMillis
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskEditorSheet(
    editor: TaskEditorState,
    availableLists: List<TaskList>,
    availableTags: List<TaskTag>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onOpen: () -> Unit,
    onTaskNameChange: (String) -> Unit,
    onTaskListChange: (Long) -> Unit,
    onTaskDescriptionChange: (String) -> Unit,
    onTaskDueDateChange: (LocalDate?) -> Unit,
    onTaskStartTimeChange: (Int?) -> Unit,
    onTaskEndTimeChange: (Int?) -> Unit,
    onTaskRepeatChange: (RepeatPreset) -> Unit,
    onTaskPriorityChange: (TaskPriority) -> Unit,
    onTaskRemindersEnabledChange: (Boolean) -> Unit,
    onTaskReminderToggle: (Int) -> Unit,
    onSubTaskToggle: (Int) -> Unit,
    onSubTaskAdd: () -> Unit,
    onSubTaskNameChange: (Int, String) -> Unit,
    onSubTaskRemove: (Int) -> Unit,
    onTaskTagToggle: (Long) -> Unit,
    onNoteContentChange: (String) -> Unit,
    onNoteListChange: (Long) -> Unit,
    onNoteDateChange: (LocalDate) -> Unit,
    onNoteTagToggle: (Long) -> Unit,
    onSwitchAddModeToTask: () -> Unit,
    onSwitchAddModeToNote: () -> Unit
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
            SheetHeader(
                isViewMode = editor.isViewMode(),
                isAddMode = editor.isAddMode(),
                isTaskSelected = editor is TaskEditorState.TaskForm,
                canDelete = editor.canDelete(),
                onDismiss = onDismiss,
                onEdit = onEdit,
                onSave = onSave,
                onDelete = onDelete,
                onSwitchAddModeToTask = onSwitchAddModeToTask,
                onSwitchAddModeToNote = onSwitchAddModeToNote,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (editor) {
                    is TaskEditorState.TaskForm -> item {
                        if (editor.mode == EditorMode.View) {
                            TaskViewContent(
                                form = editor,
                                availableLists = availableLists,
                                availableTags = availableTags,
                                onSubTaskToggle = onSubTaskToggle
                            )
                        } else {
                            TaskFormContent(
                                form = editor,
                                availableLists = availableLists,
                                availableTags = availableTags,
                                onNameChange = onTaskNameChange,
                                onListChange = onTaskListChange,
                                onDescriptionChange = onTaskDescriptionChange,
                                onDueDateChange = onTaskDueDateChange,
                                onStartTimeChange = onTaskStartTimeChange,
                                onEndTimeChange = onTaskEndTimeChange,
                                onRepeatChange = onTaskRepeatChange,
                                onPriorityChange = onTaskPriorityChange,
                                onRemindersEnabledChange = onTaskRemindersEnabledChange,
                                onReminderToggle = onTaskReminderToggle,
                                onSubTaskToggle = onSubTaskToggle,
                                onSubTaskAdd = onSubTaskAdd,
                                onSubTaskNameChange = onSubTaskNameChange,
                                onSubTaskRemove = onSubTaskRemove,
                                onTagToggle = onTaskTagToggle
                            )
                        }
                    }
                    is TaskEditorState.NoteForm -> item {
                        if (editor.mode == EditorMode.View) {
                            NoteViewContent(editor, availableLists, availableTags)
                        } else {
                            NoteFormContent(
                                form = editor,
                                availableLists = availableLists,
                                availableTags = availableTags,
                                onContentChange = onNoteContentChange,
                                onListChange = onNoteListChange,
                                onDateChange = onNoteDateChange,
                                onTagToggle = onNoteTagToggle
                            )
                        }
                    }
                }
                if (editor.isCompletableView()) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = onComplete) {
                                Text("Complete")
                            }
                        }
                    }
                }
                if (editor.isOpenableView()) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = onOpen) {
                                Text("Open")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddModeSwitch(
    isTaskSelected: Boolean,
    onTaskClick: () -> Unit,
    onNoteClick: () -> Unit
) {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = isTaskSelected,
            onClick = onTaskClick,
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = { Icon(Icons.Default.TaskAlt, contentDescription = null) },
            label = { Text("Task") }
        )
        SegmentedButton(
            selected = !isTaskSelected,
            onClick = onNoteClick,
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = { Icon(Icons.Default.Notes, contentDescription = null) },
            label = { Text("Note") }
        )
    }
}

@Composable
private fun SheetHeader(
    isViewMode: Boolean,
    isAddMode: Boolean,
    isTaskSelected: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onSwitchAddModeToTask: () -> Unit,
    onSwitchAddModeToNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }

        if (isAddMode) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                AddModeSwitch(
                    isTaskSelected = isTaskSelected,
                    onTaskClick = onSwitchAddModeToTask,
                    onNoteClick = onSwitchAddModeToNote
                )
            }
        }

        Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            if (isViewMode) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
            if (canDelete && isViewMode) {
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
            if (!isViewMode) {
                Button(onClick = onSave) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun TaskViewContent(
    form: TaskEditorState.TaskForm,
    availableLists: List<TaskList>,
    availableTags: List<TaskTag>,
    onSubTaskToggle: (Int) -> Unit
) {
    val selectedList = availableLists.firstOrNull { it.id == form.listId }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = form.name.ifBlank { "Untitled task" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        form.description.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedList?.let { list ->
                DetailChip(materialIcon(list.icon), list.name, iconTint = list.color.toColor())
            }
            DateTimeRangeDetailChip(form.doDate, form.startTimeMinutes, form.endTimeMinutes)
            form.durationMinutes?.let { DetailChip(Icons.Default.Schedule, it.formatDuration()) }
            if (form.priority != TaskPriority.None) {
                PriorityPill(priority = form.priority, selected = true)
            }
        }
        if (form.repeatPreset != RepeatPreset.None) {
            RepeatPill(form.repeatPreset.rrule)
        }
        ReminderDisplayRow(
            reminderOffsets = form.reminderOffsets,
            startTimeMinutes = form.startTimeMinutes
        )
        SubtaskChecklist(
            subtasks = form.subtasks,
            mode = form.mode,
            onToggle = onSubTaskToggle,
            onAdd = {},
            onNameChange = { _, _ -> },
            onRemove = {}
        )
        TagDisplayRow(
            selectedTagIds = form.selectedTagIds,
            availableTags = availableTags
        )
    }
}

@Composable
private fun ReminderDisplayRow(
    reminderOffsets: Set<Int>,
    startTimeMinutes: Int?
) {
    if (reminderOffsets.isEmpty()) return
    QuietSection {
        DetailRow(
            icon = Icons.Default.Notifications,
            primary = reminderOffsets
                .sorted()
                .joinToString { TaskReminderPreset.labelFor(it, startTimeMinutes) }
        )
    }
}

@Composable
private fun NoteViewContent(
    form: TaskEditorState.NoteForm,
    availableLists: List<TaskList>,
    availableTags: List<TaskTag>
) {
    val selectedList = availableLists.firstOrNull { it.id == form.listId }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = form.content.ifBlank { "Empty note" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedList?.let { list ->
                DetailChip(materialIcon(list.icon), list.name, iconTint = list.color.toColor())
            }
            DetailChip(Icons.Default.Event, form.date.compact())
            DetailChip(Icons.Default.CheckCircle, form.status.name)
        }
        TagDisplayRow(
            selectedTagIds = form.selectedTagIds,
            availableTags = availableTags
        )
    }
}

@Composable
private fun EditorSection(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun QuietSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    primary: String,
    secondary: String? = null
) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(primary, style = MaterialTheme.typography.bodyLarge)
            secondary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TagDisplayRow(
    selectedTagIds: Set<Long>,
    availableTags: List<TaskTag>
) {
    val selectedTags = availableTags.filter { it.id in selectedTagIds }
    if (selectedTags.isEmpty()) return
    QuietSection {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            selectedTags.forEach { tag ->
                TaskTagPill(tag = tag, selected = true)
            }
        }
    }
}

@Composable
private fun TagPickerRow(
    availableTags: List<TaskTag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit
) {
    if (availableTags.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            availableTags.forEach { tag ->
                TaskTagPill(
                    tag = tag,
                    selected = tag.id in selectedTagIds,
                    onClick = { onTagToggle(tag.id) }
                )
            }
        }
    }
}

@Composable
private fun TaskFormContent(
    form: TaskEditorState.TaskForm,
    availableLists: List<TaskList>,
    availableTags: List<TaskTag>,
    onNameChange: (String) -> Unit,
    onListChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDueDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onRepeatChange: (RepeatPreset) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onReminderToggle: (Int) -> Unit,
    onSubTaskToggle: (Int) -> Unit,
    onSubTaskAdd: () -> Unit,
    onSubTaskNameChange: (Int, String) -> Unit,
    onSubTaskRemove: (Int) -> Unit,
    onTagToggle: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Task title") },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            shape = MaterialTheme.shapes.medium,
            colors = editorTextFieldColors()
        )
        OutlinedTextField(
            value = form.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Add details") },
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            colors = editorTextFieldColors()
        )
        EditorSection {
            ListPickerRow(
                selectedListId = form.listId,
                lists = availableLists,
                onListChange = onListChange
            )
            DatePickerRow(date = form.doDate, onDateChange = onDueDateChange)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimePickerRow(
                    label = "Start",
                    timeMinutes = form.startTimeMinutes,
                    initialTimeMinutes = currentTimeMinutes(),
                    onTimeChange = onStartTimeChange,
                    modifier = Modifier.weight(1f)
                )
                form.durationMinutes?.let { duration ->
                    DurationText(
                        duration = duration,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                TimePickerRow(
                    label = "End",
                    timeMinutes = form.endTimeMinutes,
                    initialTimeMinutes = ((form.startTimeMinutes ?: currentTimeMinutes()) + 60).coerceAtMost(MinutesPerDay - 1),
                    enabled = form.startTimeMinutes != null,
                    onTimeChange = onEndTimeChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        EditorSection {
            PriorityPickerRow(selected = form.priority, onSelect = onPriorityChange)
            RepeatDropdown(selected = form.repeatPreset, onSelect = onRepeatChange)
            ReminderPicker(
                reminderOffsets = form.reminderOffsets,
                hasDate = form.doDate != null,
                startTimeMinutes = form.startTimeMinutes,
                onEnabledChange = onRemindersEnabledChange,
                onReminderToggle = onReminderToggle
            )
        }
        SubtaskChecklist(
            subtasks = form.subtasks,
            mode = form.mode,
            onToggle = onSubTaskToggle,
            onAdd = onSubTaskAdd,
            onNameChange = onSubTaskNameChange,
            onRemove = onSubTaskRemove
        )
        TagPickerRow(
            availableTags = availableTags,
            selectedTagIds = form.selectedTagIds,
            onTagToggle = onTagToggle
        )
    }
}

@Composable
fun editorTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerRow(
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    SelectableInfoRow(
        icon = Icons.Default.Event,
        label = "Date",
        value = date?.compact() ?: "No date",
        onClick = { showPicker = true },
        onClear = if (date == null) null else ({ onDateChange(null) })
    )
    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date?.toUtcStartMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateChange(datePickerState.selectedDateMillis?.toUtcLocalDate())
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
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerRow(
    label: String,
    timeMinutes: Int?,
    initialTimeMinutes: Int,
    onTimeChange: (Int?) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    SelectableInfoRow(
        icon = Icons.Default.Schedule,
        label = label,
        value = timeMinutes?.toClockLabel() ?: "No time",
        onClick = { showPicker = true },
        onClear = if (timeMinutes == null || !enabled) null else ({ onTimeChange(null) }),
        enabled = enabled,
        modifier = modifier
    )
    if (showPicker && enabled) {
        val initial = timeMinutes ?: initialTimeMinutes.coerceIn(0, MinutesPerDay - 1)
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

@Composable
internal fun DurationText(
    duration: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = duration.formatDuration(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun ListPickerRow(
    selectedListId: Long,
    lists: List<TaskList>,
    onListChange: (Long) -> Unit
) {
    if (lists.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val selectedList = lists.firstOrNull { it.id == selectedListId } ?: lists.first()
    Box {
        SelectableInfoRow(
            icon = materialIcon(selectedList.icon),
            label = "List",
            value = selectedList.name,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            lists.forEach { list ->
                DropdownMenuItem(
                    text = { Text(list.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = materialIcon(list.icon),
                            contentDescription = null,
                            tint = list.color.toColor()
                        )
                    },
                    onClick = {
                        onListChange(list.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RepeatDropdown(
    selected: RepeatPreset,
    onSelect: (RepeatPreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SelectableInfoRow(
            icon = Icons.Default.MoreTime,
            label = "Repeat",
            value = selected.label,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RepeatPreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.label) },
                    onClick = {
                        onSelect(preset)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderPicker(
    reminderOffsets: Set<Int>,
    hasDate: Boolean,
    startTimeMinutes: Int?,
    onEnabledChange: (Boolean) -> Unit,
    onReminderToggle: (Int) -> Unit
) {
    val enabled = reminderOffsets.isNotEmpty()
    val presets = TaskReminderPreset.availableFor(startTimeMinutes)
    var showOptions by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (hasDate) {
                            Modifier.clickable {
                                if (!enabled) onEnabledChange(true)
                                showOptions = true
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Reminders", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        text = when {
                            !hasDate -> "Set a date first"
                            !enabled -> "Off"
                            else -> reminderOffsets
                                .sorted()
                                .joinToString { TaskReminderPreset.labelFor(it, startTimeMinutes) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (hasDate || !checked) {
                            onEnabledChange(checked)
                            showOptions = checked
                        }
                    },
                    enabled = hasDate || enabled
                )
            }
        }
        if (showOptions && hasDate) {
            AlertDialog(
                onDismissRequest = { showOptions = false },
                title = { Text("Reminders") },
                text = {
                    Column {
                        presets.forEach { preset ->
                            ReminderOptionRow(
                                label = TaskReminderPreset.labelFor(preset.offsetMinutes, startTimeMinutes),
                                selected = preset.offsetMinutes in reminderOffsets,
                                onClick = { onReminderToggle(preset.offsetMinutes) }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showOptions = false }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

@Composable
private fun ReminderOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SelectableInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClear: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            onClear?.let {
                IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                }
            }
        }
    }
}

@Composable
private fun NoteFormContent(
    form: TaskEditorState.NoteForm,
    availableLists: List<TaskList>,
    availableTags: List<TaskTag>,
    onContentChange: (String) -> Unit,
    onListChange: (Long) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTagToggle: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        OutlinedTextField(
            value = form.content,
            onValueChange = onContentChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Write a note") },
            minLines = 6,
            shape = MaterialTheme.shapes.medium,
            colors = editorTextFieldColors()
        )
        EditorSection {
            ListPickerRow(
                selectedListId = form.listId,
                lists = availableLists,
                onListChange = onListChange
            )
            RequiredDatePickerRow(date = form.date, onDateChange = onDateChange)
        }
        TagPickerRow(
            availableTags = availableTags,
            selectedTagIds = form.selectedTagIds,
            onTagToggle = onTagToggle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequiredDatePickerRow(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    SelectableInfoRow(
        icon = Icons.Default.Event,
        label = "Date",
        value = date.compact(),
        onClick = { showPicker = true }
    )
    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.toUtcStartMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateChange(datePickerState.selectedDateMillis?.toUtcLocalDate() ?: date)
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
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun TaskEditorState.isViewMode(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode == EditorMode.View
    is TaskEditorState.NoteForm -> mode == EditorMode.View
}

private fun TaskEditorState.isAddMode(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode == EditorMode.Add
    is TaskEditorState.NoteForm -> mode == EditorMode.Add
}

private fun TaskEditorState.canDelete(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode != EditorMode.Add
    is TaskEditorState.NoteForm -> mode != EditorMode.Add
}

private fun TaskEditorState.isCompletableView(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode == EditorMode.View && status != TaskStatus.Completed
    is TaskEditorState.NoteForm -> mode == EditorMode.View && status != TaskStatus.Completed
}

private fun TaskEditorState.isOpenableView(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode == EditorMode.View && status == TaskStatus.Completed
    is TaskEditorState.NoteForm -> mode == EditorMode.View && status == TaskStatus.Completed
}

private fun currentTimeMinutes(): Int {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return now.hour * 60 + now.minute
}

private const val MinutesPerDay = 24 * 60
