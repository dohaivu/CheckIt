package com.checkit.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskPriority
import com.checkit.ui.EditorMode
import com.checkit.ui.RepeatPreset
import com.checkit.ui.TaskEditorState
import com.checkit.ui.toUtcLocalDate
import com.checkit.ui.toUtcStartMillis
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskEditorSheet(
    editor: TaskEditorState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTaskNameChange: (String) -> Unit,
    onTaskDescriptionChange: (String) -> Unit,
    onTaskDueDateChange: (LocalDate?) -> Unit,
    onTaskStartTimeChange: (Int?) -> Unit,
    onTaskEndTimeChange: (Int?) -> Unit,
    onTaskRepeatChange: (RepeatPreset) -> Unit,
    onTaskPriorityChange: (TaskPriority) -> Unit,
    onNoteContentChange: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SheetHeader(
                    title = editor.sheetTitle(),
                    canDelete = editor.isEditable(),
                    onDismiss = onDismiss,
                    onSave = onSave,
                    onDelete = onDelete
                )
            }
            when (editor) {
                is TaskEditorState.TaskForm -> item {
                    TaskFormContent(
                        form = editor,
                        onNameChange = onTaskNameChange,
                        onDescriptionChange = onTaskDescriptionChange,
                        onDueDateChange = onTaskDueDateChange,
                        onStartTimeChange = onTaskStartTimeChange,
                        onEndTimeChange = onTaskEndTimeChange,
                        onRepeatChange = onTaskRepeatChange,
                        onPriorityChange = onTaskPriorityChange
                    )
                }
                is TaskEditorState.NoteForm -> item {
                    NoteFormContent(
                        form = editor,
                        onContentChange = onNoteContentChange
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (canDelete) {
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
        Button(onClick = onSave) {
            Text("Save")
        }
    }
}

@Composable
private fun TaskFormContent(
    form: TaskEditorState.TaskForm,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDueDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onRepeatChange: (RepeatPreset) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.TaskAlt, contentDescription = null) }
        )
        OutlinedTextField(
            value = form.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description") },
            minLines = 2,
            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) }
        )
        DatePickerRow(date = form.dueDate, onDateChange = onDueDateChange)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimePickerRow(
                label = "Start",
                timeMinutes = form.startTimeMinutes,
                onTimeChange = onStartTimeChange,
                modifier = Modifier.weight(1f)
            )
            TimePickerRow(
                label = "End",
                timeMinutes = form.endTimeMinutes,
                onTimeChange = onEndTimeChange,
                modifier = Modifier.weight(1f)
            )
        }
        InfoRow(
            icon = Icons.Default.Schedule,
            label = "Duration",
            value = form.durationMinutes?.formatDuration() ?: "Set start and end"
        )
        ChoiceRow(
            label = "Priority",
            values = TaskPriority.entries,
            selected = form.priority,
            labelFor = { it.name },
            onSelect = onPriorityChange
        )
        RepeatDropdown(selected = form.repeatPreset, onSelect = onRepeatChange)
    }
}

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
private fun TimePickerRow(
    label: String,
    timeMinutes: Int?,
    onTimeChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    SelectableInfoRow(
        icon = Icons.Default.Schedule,
        label = label,
        value = timeMinutes?.toClockLabel() ?: "No time",
        onClick = { showPicker = true },
        onClear = if (timeMinutes == null) null else ({ onTimeChange(null) }),
        modifier = modifier
    )
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
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    SelectableInfoRow(
        icon = icon,
        label = label,
        value = value,
        onClick = {},
        modifier = modifier,
        enabled = false
    )
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
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
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
    onContentChange: (String) -> Unit
) {
    OutlinedTextField(
        value = form.content,
        onValueChange = onContentChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Note") },
        minLines = 6,
        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) }
    )
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    values: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(values, key = { labelFor(it) }) { value ->
                ElevatedFilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(labelFor(value)) }
                )
            }
        }
    }
}

private fun TaskEditorState.sheetTitle(): String = when (this) {
    is TaskEditorState.TaskForm -> if (mode == EditorMode.Add) "New task" else "Edit task"
    is TaskEditorState.NoteForm -> if (mode == EditorMode.Add) "New note" else "Edit note"
}

private fun TaskEditorState.isEditable(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode == EditorMode.Edit
    is TaskEditorState.NoteForm -> mode == EditorMode.Edit
}
