package com.checkit.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.Objective
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskTag
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppHorizontalDivider
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DatePicker
import com.checkit.ui.components.DeleteOverflowMenu
import com.checkit.ui.components.ListPicker
import com.checkit.ui.components.PriorityPicker
import com.checkit.ui.components.TagPicker
import com.checkit.ui.components.TimeRangePicker
import com.checkit.ui.tasks.views.ContentAlpha
import com.checkit.ui.today
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskEditorSheet(
    editor: TaskEditorState,
    availableLists: List<Objective>,
    availableTags: List<TaskTag>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onComplete: () -> Unit,
    onOpen: () -> Unit,
    onAddToMyDay: () -> Unit,
    onTaskNameChange: (String) -> Unit,
    onTaskListChange: (Long) -> Unit,
    onTaskDescriptionChange: (String) -> Unit,
    onTaskDoDateChange: (LocalDate?) -> Unit,
    onTaskStartTimeChange: (Int?) -> Unit,
    onTaskEndTimeChange: (Int?) -> Unit,
    onDailyPlanStartTimeChange: (Int?) -> Unit,
    onDailyPlanEndTimeChange: (Int?) -> Unit,
    onDailyPlanStatus: () -> Unit,
    onDailyPlanDelete: (Long) -> Unit,
    onTaskRepeatChange: (RepeatPreset) -> Unit,
    onTaskPriorityChange: (TaskPriority) -> Unit,
    onTaskReminderToggle: (Int) -> Unit,
    onSubTaskToggle: (Int) -> Unit,
    onSubTaskAdd: () -> Unit,
    onSubTaskNameChange: (Int, String) -> Unit,
    onSubTaskRemove: (Int) -> Unit,
    onSubTaskMove: (Int, Int) -> Unit,
    onTaskTagToggle: (Long) -> Unit,
    onNoteTitleChange: (String) -> Unit,
    onNoteContentChange: (String) -> Unit,
    onNoteListChange: (Long) -> Unit,
    onNoteDateChange: (LocalDate?) -> Unit,
    onNoteStartTimeChange: (Int?) -> Unit,
    onNoteTagToggle: (Long) -> Unit,
    onSwitchAddModeToTask: () -> Unit,
    onSwitchAddModeToNote: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight(0.7f)
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        SheetHeader(
            isAddMode = editor.isAddMode(),
            isTaskSelected = editor is TaskEditorState.TaskForm,
            onSwitchAddModeToTask = onSwitchAddModeToTask,
            onSwitchAddModeToNote = onSwitchAddModeToNote,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        TrashedStatusSection(
            isTrashed = editor.isTrashed(),
            onRestore = onRestore,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (editor) {
                is TaskEditorState.TaskForm -> {
                    editor.dailyPlanItem?.let { dailyPlanItem ->
                        item {
                            DailyPlanSection(
                                item = dailyPlanItem,
                                onStartTimeChange = onDailyPlanStartTimeChange,
                                onEndTimeChange = onDailyPlanEndTimeChange,
                                onStatusChange = onDailyPlanStatus,
                                onDelete = onDailyPlanDelete,
                                enabled = editor.isFormEditable()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            AppHorizontalDivider()
                        }
                    }

                    item {
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
                            onSubTaskMove = onSubTaskMove,
                            onTagToggle = onTaskTagToggle,
                            enabled = editor.isFormEditable()
                        )
                    }
                }

                is TaskEditorState.NoteForm -> {
                    item {
                        NoteFormContent(
                            form = editor,
                            availableLists = availableLists,
                            availableTags = availableTags,
                            onTitleChange = onNoteTitleChange,
                            onContentChange = onNoteContentChange,
                            onListChange = onNoteListChange,
                            onDateChange = onNoteDateChange,
                            onStartTimeChange = onNoteStartTimeChange,
                            onTagToggle = onNoteTagToggle,
                            enabled = editor.isFormEditable()
                        )
                    }
                }
            }
        }
        SheetFooter(
            canDelete = editor.canDelete(),
            isTrashed = editor.isTrashed(),
            isAddMode = editor.isAddMode(),
            showAddToMyDay = editor.shouldShowAddToMyDay(),
            isCompletable = editor.isCompletableView(),
            isOpenable = editor.isOpenableView(),
            onSave = onSave,
            onAddToMyDay = onAddToMyDay,
            onDelete = onDelete,
            onComplete = onComplete,
            onOpen = onOpen,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
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
            label = { Text("Task") },
            colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.primaryContainer, activeContentColor = MaterialTheme.colorScheme.primary)
        )
        SegmentedButton(
            selected = !isTaskSelected,
            onClick = onNoteClick,
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
            label = { Text("Note") },
            colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.primaryContainer, activeContentColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun SheetHeader(
    isAddMode: Boolean,
    isTaskSelected: Boolean,
    onSwitchAddModeToTask: () -> Unit,
    onSwitchAddModeToNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (isAddMode) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                AddModeSwitch(
                    isTaskSelected = isTaskSelected,
                    onTaskClick = onSwitchAddModeToTask,
                    onNoteClick = onSwitchAddModeToNote
                )
            }
        }
    }
}

@Composable
private fun TrashedStatusSection(
    isTrashed: Boolean,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isTrashed) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = "This item is in trash",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(onClick = onRestore) {
                Text("Restore")
            }
        }
    }
}

@Composable
private fun SheetFooter(
    canDelete: Boolean,
    isTrashed: Boolean,
    isAddMode: Boolean,
    showAddToMyDay: Boolean,
    isCompletable: Boolean,
    isOpenable: Boolean,
    onSave: () -> Unit,
    onAddToMyDay: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showOptionsMenu = canDelete && !isTrashed
    if (!showOptionsMenu && !isAddMode && !showAddToMyDay && !isCompletable && !isOpenable) return

    Row(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showAddToMyDay) {
                OutlinedButton(onClick = onAddToMyDay) {
                    Text("Add to MyDay")
                }
            }
            if (isAddMode) {
                Button(onClick = onSave) {
                    Text("Save")
                }
            }
            if (isCompletable) {
                Button(onClick = onComplete) {
                    Text("Complete")
                }
            }
            if (isOpenable) {
                Button(onClick = onOpen) {
                    Text("Open")
                }
            }
        }
        if (showOptionsMenu) {
            DeleteOverflowMenu(onDelete = onDelete)
        }
    }
}

@Composable
private fun TaskFormContent(
    form: TaskEditorState.TaskForm,
    availableLists: List<Objective>,
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
    onSubTaskMove: (Int, Int) -> Unit,
    onTagToggle: (Long) -> Unit,
    enabled: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TaskIcon(
                completed = form.status == TaskStatus.Completed,
                color = form.priority.priorityColor()
            )
            DatePicker(
                modifier = Modifier.weight(1f),
                date = form.doDate,
                onDateChange = onDoDateChange,
                startTimeMinutes = form.startTimeMinutes,
                endTimeMinutes = form.endTimeMinutes,
                onStartTimeChange = onStartTimeChange,
                onEndTimeChange = onEndTimeChange,
                enabled = enabled,
                isOverdue = form.isOverdue()
            )
            PriorityPicker(selected = form.priority, onSelect = onPriorityChange, enabled = enabled)
        }

        AppOutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 3,
            placeholder = "What would you like to do?",
            enabled = enabled
        )
        AppOutlinedTextField(
            value = form.description,
            onValueChange = onDescriptionChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 5,
            enabled = enabled
        )

        SubtaskChecklist(
            subtasks = form.subtasks,
            onToggle = onSubTaskToggle,
            onAdd = onSubTaskAdd,
            onNameChange = onSubTaskNameChange,
            onRemove = onSubTaskRemove,
            onMove = onSubTaskMove,
            enabled = enabled
        )

//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(6.dp),
//        ) {
//            RepeatPicker(selected = form.repeatPreset, onSelect = onRepeatChange, enabled = enabled)
//            ReminderPicker(
//                reminderOffsets = form.reminderOffsets,
//                hasDate = form.doDate != null,
//                startTimeMinutes = form.startTimeMinutes,
//                onReminderToggle = onReminderToggle,
//                enabled = enabled
//            )
//        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ListPicker(
                selectedListId = form.objectiveId,
                lists = availableLists,
                onListChange = onListChange,
                enabled = enabled
            )
            TagPicker(
                availableTags = availableTags,
                selectedTagIds = form.selectedTagIds,
                onTagToggle = onTagToggle,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun DailyPlanSection(
    item: DailyPlanItem?,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onStatusChange: () -> Unit,
    onDelete: (Long) -> Unit,
    enabled: Boolean = true
) {
    if (item == null) return
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = ContentAlpha), RoundedCornerShape(16.dp))
            .padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
        ,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeRangePicker(
                startTimeMinutes = item.startTimeMinutes,
                endTimeMinutes = item.endTimeMinutes,
                onStartTimeChange = onStartTimeChange,
                onEndTimeChange = onEndTimeChange,
                modifier = Modifier,
                enabled = enabled,
                isOverdue = item.isOverdue(today()),
                clearEnabled = false
            )

            if (enabled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(onClick = {onDelete(item.id)}) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete from My Day",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onStatusChange) {
                        Icon(
                            imageVector = if (item.status == DailyPlanItemStatus.Done) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check,
                            contentDescription = "Done from My Day",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteFormContent(
    form: TaskEditorState.NoteForm,
    availableLists: List<Objective>,
    availableTags: List<TaskTag>,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onListChange: (Long) -> Unit,
    onDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onTagToggle: (Long) -> Unit,
    enabled: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NoteIcon(status = form.status)
            DatePicker(
                date = form.date,
                onDateChange = onDateChange,
                startTimeMinutes = form.startTimeMinutes,
                endTimeMinutes = null,
                onStartTimeChange = onStartTimeChange,
                onEndTimeChange = null,
                enabled = enabled
            )
        }
        AppOutlinedTextField(
            value = form.title,
            onValueChange = onTitleChange,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 3,
            placeholder = "Note title",
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        AppOutlinedTextField(
            value = form.content,
            onValueChange = onContentChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            ),
            minLines = 5,
            maxLines = 10,
            placeholder = "Add more details",
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(130.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ListPicker(
                selectedListId = form.objectiveId,
                lists = availableLists,
                onListChange = onListChange,
                enabled = enabled
            )
            TagPicker(
                availableTags = availableTags,
                selectedTagIds = form.selectedTagIds,
                onTagToggle = onTagToggle,
                enabled = enabled
            )
        }
    }
}

private fun TaskEditorState.isAddMode(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode == EditorMode.Add
    is TaskEditorState.NoteForm -> mode == EditorMode.Add
}

private fun TaskEditorState.isTrashed(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> trashedAtMillis != null
    is TaskEditorState.NoteForm -> trashedAtMillis != null
}

private fun TaskEditorState.isFormEditable(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode == EditorMode.Add || (mode == EditorMode.Edit && status == TaskStatus.Open && trashedAtMillis == null)
    is TaskEditorState.NoteForm -> mode == EditorMode.Add || (mode == EditorMode.Edit && status == TaskStatus.Open && trashedAtMillis == null)
}

private fun TaskEditorState.canDelete(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode != EditorMode.Add
    is TaskEditorState.NoteForm -> mode != EditorMode.Add
}

private fun TaskEditorState.shouldShowAddToMyDay(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> taskId != null && mode != EditorMode.Add && isFormEditable()
    is TaskEditorState.NoteForm -> false
}

private fun TaskEditorState.isCompletableView(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode == EditorMode.Edit && status == TaskStatus.Open && trashedAtMillis == null
    is TaskEditorState.NoteForm -> mode == EditorMode.Edit && status == TaskStatus.Open && trashedAtMillis == null
}

private fun TaskEditorState.isOpenableView(): Boolean = when (this) {
    is TaskEditorState.TaskForm -> mode == EditorMode.Edit && status == TaskStatus.Completed && trashedAtMillis == null
    is TaskEditorState.NoteForm -> mode == EditorMode.Edit && status == TaskStatus.Completed && trashedAtMillis == null
}

private fun TaskEditorState.TaskForm.isOverdue(): Boolean {
    return doDate.isOverdue(today(), endTimeMinutes ?: startTimeMinutes, status == TaskStatus.Completed )
}
