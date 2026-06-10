package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskTag
import com.checkit.ui.EditorMode
import com.checkit.ui.RepeatPreset
import com.checkit.ui.TaskEditorState
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DatePickerRow
import com.checkit.ui.components.DateTimeRangeDetailChip
import com.checkit.ui.components.DetailChip
import com.checkit.ui.components.ListPicker
import com.checkit.ui.components.PriorityPicker
import com.checkit.ui.components.PriorityPill
import com.checkit.ui.components.ReminderPicker
import com.checkit.ui.components.RepeatPicker
import com.checkit.ui.components.TagPicker
import kotlinx.datetime.LocalDate

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
    onTaskDoDateChange: (LocalDate?) -> Unit,
    onTaskStartTimeChange: (Int?) -> Unit,
    onTaskEndTimeChange: (Int?) -> Unit,
    onTaskRepeatChange: (RepeatPreset) -> Unit,
    onTaskPriorityChange: (TaskPriority) -> Unit,
    onTaskReminderToggle: (Int) -> Unit,
    onSubTaskToggle: (Int) -> Unit,
    onSubTaskAdd: () -> Unit,
    onSubTaskNameChange: (Int, String) -> Unit,
    onSubTaskRemove: (Int) -> Unit,
    onTaskTagToggle: (Long) -> Unit,
    onNoteTitleChange: (String) -> Unit,
    onNoteContentChange: (String) -> Unit,
    onNoteListChange: (Long) -> Unit,
    onNoteDateChange: (LocalDate) -> Unit,
    onNoteStartTimeChange: (Int?) -> Unit,
    onNoteTagToggle: (Long) -> Unit,
    onSwitchAddModeToTask: () -> Unit,
    onSwitchAddModeToNote: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
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
                                onNameChange = onTaskNameChange,
                                onListChange = onTaskListChange,
                                onDescriptionChange = onTaskDescriptionChange,
                                onDoDateChange = onTaskDoDateChange,
                                onStartTimeChange = onTaskStartTimeChange,
                                onEndTimeChange = onTaskEndTimeChange,
                                onRepeatChange = onTaskRepeatChange,
                                onPriorityChange = onTaskPriorityChange,
                                onReminderToggle = onTaskReminderToggle,
                                onSubTaskToggle = onSubTaskToggle,
                                onSubTaskAdd = onSubTaskAdd,
                                onSubTaskNameChange = onSubTaskNameChange,
                                onSubTaskRemove = onSubTaskRemove,
                                onTagToggle = onTaskTagToggle
                            )
                        } else {
                            TaskFormContent(
                                form = editor,
                                availableLists = availableLists,
                                availableTags = availableTags,
                                onNameChange = onTaskNameChange,
                                onListChange = onTaskListChange,
                                onDescriptionChange = onTaskDescriptionChange,
                                onDoDateChange = onTaskDoDateChange,
                                onStartTimeChange = onTaskStartTimeChange,
                                onEndTimeChange = onTaskEndTimeChange,
                                onRepeatChange = onTaskRepeatChange,
                                onPriorityChange = onTaskPriorityChange,
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
                                onTitleChange = onNoteTitleChange,
                                onContentChange = onNoteContentChange,
                                onListChange = onNoteListChange,
                                onDateChange = onNoteDateChange,
                                onStartTimeChange = onNoteStartTimeChange,
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
            if (isAddMode) {
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
    onNameChange: (String) -> Unit,
    onListChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDoDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onRepeatChange: (RepeatPreset) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onReminderToggle: (Int) -> Unit,
    onSubTaskToggle: (Int) -> Unit,
    onSubTaskAdd: () -> Unit,
    onSubTaskNameChange: (Int, String) -> Unit,
    onSubTaskRemove: (Int) -> Unit,
    onTagToggle: (Long) -> Unit
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
            DateTimeRangeDetailChip(form.doDate, form.startTimeMinutes, form.endTimeMinutes)
            form.durationMinutes?.let { DetailChip(Icons.Default.Schedule, it.formatDuration()) }
            if (form.priority != TaskPriority.None) {
                PriorityPill(priority = form.priority, selected = true)
            }
        }

        RepeatPicker(selected = form.repeatPreset, onSelect = onRepeatChange)
        SubtaskChecklist(
            subtasks = form.subtasks,
            mode = EditorMode.Edit,
            onToggle = onSubTaskToggle,
            onAdd = onSubTaskAdd,
            onNameChange = onSubTaskNameChange,
            onRemove = onSubTaskRemove
        )

        SupportingPills(list = selectedList, tags = availableTags.filter { it.id in form.selectedTagIds })
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
        if (form.title.isNotBlank()) {
            Text(
                text = form.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = form.content.ifBlank { "Empty note" },
            style = MaterialTheme.typography.bodyLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailChip(Icons.Default.Event, form.date.compact())
            form.startTimeMinutes?.let { start ->
                DetailChip(Icons.Default.Schedule, start.toClockLabel())
            }
        }
        SupportingPills(list = selectedList, tags = availableTags.filter { it.id in form.selectedTagIds })
    }
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
private fun TaskFormContent(
    form: TaskEditorState.TaskForm,
    availableLists: List<TaskList>,
    availableTags: List<TaskTag>,
    onNameChange: (String) -> Unit,
    onListChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDoDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onRepeatChange: (RepeatPreset) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onReminderToggle: (Int) -> Unit,
    onSubTaskToggle: (Int) -> Unit,
    onSubTaskAdd: () -> Unit,
    onSubTaskNameChange: (Int, String) -> Unit,
    onSubTaskRemove: (Int) -> Unit,
    onTagToggle: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TaskStatusIcon(form.status, form.priority)
            DatePickerRow(
                modifier = Modifier.weight(1f),
                date = form.doDate,
                onDateChange = onDoDateChange,
                startTimeMinutes = form.startTimeMinutes,
                endTimeMinutes = form.endTimeMinutes,
                durationMinutes = form.durationMinutes,
                onStartTimeChange = onStartTimeChange,
                onEndTimeChange = onEndTimeChange
            )
        }

        AppOutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            placeholder = "What would you like to do?"
        )
        AppOutlinedTextField(
            value = form.description,
            onValueChange = onDescriptionChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 5
        )

        SubtaskChecklist(
            subtasks = form.subtasks,
            mode = form.mode,
            onToggle = onSubTaskToggle,
            onAdd = onSubTaskAdd,
            onNameChange = onSubTaskNameChange,
            onRemove = onSubTaskRemove
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PriorityPicker(selected = form.priority, onSelect = onPriorityChange)
            RepeatPicker(selected = form.repeatPreset, onSelect = onRepeatChange)
            ReminderPicker(
                reminderOffsets = form.reminderOffsets,
                hasDate = form.doDate != null,
                startTimeMinutes = form.startTimeMinutes,
                onReminderToggle = onReminderToggle
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ListPicker(
                selectedListId = form.listId,
                lists = availableLists,
                onListChange = onListChange
            )
            TagPicker(
                availableTags = availableTags,
                selectedTagIds = form.selectedTagIds,
                onTagToggle = onTagToggle
            )
        }
    }
}

@Composable
private fun NoteFormContent(
    form: TaskEditorState.NoteForm,
    availableLists: List<TaskList>,
    availableTags: List<TaskTag>,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onListChange: (Long) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onTagToggle: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppOutlinedTextField(
            value = form.title,
            onValueChange = onTitleChange,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            placeholder = "Note title"
        )
        AppOutlinedTextField(
            value = form.content,
            onValueChange = onContentChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 10,
            placeholder = "Write something"
        )

        DatePickerRow(
            date = form.date,
            onDateChange = {
                it?.let {onDateChange.invoke(it)  }
            },
            startTimeMinutes = form.startTimeMinutes,
            endTimeMinutes = null,
            durationMinutes = null,
            onStartTimeChange = onStartTimeChange,
            onEndTimeChange = null
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ListPicker(
                selectedListId = form.listId,
                lists = availableLists,
                onListChange = onListChange
            )
            TagPicker(
                availableTags = availableTags,
                selectedTagIds = form.selectedTagIds,
                onTagToggle = onTagToggle
            )
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


