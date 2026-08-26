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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.ListItem
import com.checkit.domain.TagItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskType
import com.checkit.ui.HabitIcon
import com.checkit.ui.NoteIcon
import com.checkit.ui.TaskIcon
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppHorizontalDivider
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DatePicker
import com.checkit.ui.components.EditorOverflowMenu
import com.checkit.ui.components.LabelSuggestions
import com.checkit.ui.components.ListPicker
import com.checkit.ui.components.MarkdownVisualTransformation
import com.checkit.ui.components.PriorityPicker
import com.checkit.ui.components.TagPicker
import com.checkit.ui.components.TimeRangePicker
import com.checkit.ui.isOverdue
import com.checkit.ui.priorityColor
import com.checkit.ui.today
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskEditorSheet(
    editor: TaskEditorState,
    availableLists: List<ListItem>,
    availableTags: List<TagItem>,
    actions: TaskEditorActions,
    recentLabels: List<String> = emptyList()
) {
    val onDismiss = actions.onDismiss
    val onSave = actions.onSave
    val onDelete = actions.onDelete
    val onRestore = actions.onRestore
    val onComplete = actions.onComplete
    val onReopen = actions.onReopen
    val onAddToMyDay = actions.onAddToMyDay
    val onTaskNameChange = actions.onTaskNameChange
    val onTaskListChange = actions.onTaskListChange
    val onTaskDescriptionChange = actions.onTaskDescriptionChange
    val onTaskDoDateChange = actions.onTaskDoDateChange
    val onTaskTimeChange = actions.onTaskTimeChange
    val onDailyPlanTimeChange = actions.onDailyPlanTimeChange
    val onDailyPlanStatus = actions.onDailyPlanStatus
    val onDailyPlanDelete = actions.onDailyPlanDelete
    val onDailyPlanStartSprint = actions.onDailyPlanStartSprint
    val onDailyPlanStartOngoingSprint = actions.onDailyPlanStartOngoingSprint
    val onTaskPriorityChange = actions.onTaskPriorityChange
    val onSubTaskToggle = actions.onSubTaskToggle
    val onSubTaskAdd = actions.onSubTaskAdd
    val onSubTaskNameChange = actions.onSubTaskNameChange
    val onSubTaskRemove = actions.onSubTaskRemove
    val onSubTaskMove = actions.onSubTaskMove
    val onTaskTagToggle = actions.onTaskTagToggle
    val onNoteTagToggle = actions.onNoteTagToggle
    val onNewTagClick = actions.onNewTagClick
    val onNoteTitleChange = actions.onNoteTitleChange
    val onNoteContentChange = actions.onNoteContentChange
    val onNoteListChange = actions.onNoteListChange
    val onNoteDateChange = actions.onNoteDateChange
    val onNoteStartTimeChange = actions.onNoteStartTimeChange
    val onPinToggle = actions.onPinToggle
    val onTaskLabelChange = actions.onTaskLabelChange
    val onNoteLabelChange = actions.onNoteLabelChange

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .windowInsetsPadding(WindowInsets.ime)
    ) {
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
                                onTimeChange = onDailyPlanTimeChange,
                                onStatusChange = onDailyPlanStatus,
                                onDelete = onDailyPlanDelete,
                                onStartSprint = onDailyPlanStartSprint,
                                onStartOngoingSprint = onDailyPlanStartOngoingSprint,
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
                            recentLabels = recentLabels,
                            onNameChange = onTaskNameChange,
                            onListChange = onTaskListChange,
                            onDescriptionChange = onTaskDescriptionChange,
                            onDoDateChange = onTaskDoDateChange,
                            onTimeChange = onTaskTimeChange,
                            onPriorityChange = onTaskPriorityChange,
                            onSubTaskToggle = onSubTaskToggle,
                            onSubTaskAdd = onSubTaskAdd,
                            onSubTaskNameChange = onSubTaskNameChange,
                            onSubTaskRemove = onSubTaskRemove,
                            onSubTaskMove = onSubTaskMove,
                            onTagToggle = onTaskTagToggle,
                            onLabelChange = onTaskLabelChange,
                            onNewTagClick = onNewTagClick,
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
                            recentLabels = recentLabels,
                            onTitleChange = onNoteTitleChange,
                            onContentChange = onNoteContentChange,
                            onListChange = onNoteListChange,
                            onDateChange = onNoteDateChange,
                            onStartTimeChange = onNoteStartTimeChange,
                            onTagToggle = onNoteTagToggle,
                            onLabelChange = onNoteLabelChange,
                            onNewTagClick = onNewTagClick,
                            enabled = editor.isFormEditable()
                        )
                    }
                }
            }
        }
        SheetFooter(
            canDelete = editor.canDelete(),
            isTrashed = editor.isTrashed(),
            isPinned = when(editor) {
                is TaskEditorState.TaskForm -> editor.isPinned
                is TaskEditorState.NoteForm -> editor.isPinned
            },
            isAddMode = editor.isAddMode(),
            showAddToMyDay = editor.shouldShowAddToMyDay(),
            isCompletable = editor.isCompletableView(),
            isOpenable = editor.isOpenableView(),
            onSave = onSave,
            onAddToMyDay = onAddToMyDay,
            onDelete = onDelete,
            onComplete = onComplete,
            onReopen = onReopen,
            onPinToggle = onPinToggle,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TrashedStatusSection(
    isTrashed: Boolean,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isTrashed) return
    val colorScheme = MaterialTheme.colorScheme
    val contentColor = colorScheme.onErrorContainer
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.RestoreFromTrash,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            Text(
                text = "This item is in trash",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
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
    isPinned: Boolean,
    isAddMode: Boolean,
    showAddToMyDay: Boolean,
    isCompletable: Boolean,
    isOpenable: Boolean,
    onSave: () -> Unit,
    onAddToMyDay: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onReopen: () -> Unit,
    onPinToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showOptionsMenu = (canDelete || isCompletable || isOpenable) && !isTrashed
    if (!showOptionsMenu && !isAddMode && !showAddToMyDay) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAddToMyDay) {
            OutlinedButton(onClick = onAddToMyDay) {
                Text("Schedule")
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isAddMode) {
            Button(onClick = onSave) {
                Text("Save")
            }
        }

        if (showOptionsMenu) {
            EditorOverflowMenu { onDismiss ->
                if (!isTrashed && (isCompletable || isOpenable || !isAddMode)) {
                    DropdownMenuItem(
                        text = { Text(if (isPinned) "Unpin" else "Pin") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = if (isPinned) "Unpin" else "Pin",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            onDismiss()
                            onPinToggle()
                        }
                    )
                }
                if (isCompletable) {
                    DropdownMenuItem(
                        text = { Text("Complete") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            onComplete()
                        }
                    )
                }
                if (isOpenable) {
                    DropdownMenuItem(
                        text = { Text("Reopen") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            onReopen()
                        }
                    )
                }
                if (canDelete) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            onDismiss()
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskFormContent(
    form: TaskEditorState.TaskForm,
    availableLists: List<ListItem>,
    availableTags: List<TagItem>,
    onNameChange: (String) -> Unit,
    onListChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDoDateChange: (LocalDate?) -> Unit,
    onTimeChange: (Int?, Int?) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onSubTaskToggle: (Int) -> Unit,
    onSubTaskAdd: () -> Unit,
    onSubTaskNameChange: (Int, String) -> Unit,
    onSubTaskRemove: (Int) -> Unit,
    onSubTaskMove: (Int, Int) -> Unit,
    onTagToggle: (Long) -> Unit,
    onLabelChange: (String) -> Unit,
    onNewTagClick: () -> Unit,
    recentLabels: List<String>,
    enabled: Boolean = true
) {
    val isHabit = form.type == TaskType.Habit
    val namePlaceholder = when (form.type) {
        TaskType.Task -> "What would you like to do?"
        TaskType.Habit -> "What habit do you want to build?"
    }
    var labelFocused by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppOutlinedTextField(
                value = form.label.orEmpty(),
                onValueChange = onLabelChange,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .widthIn(max = 80.dp)
                    .onFocusChanged { labelFocused = it.isFocused },
                textStyle = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                placeholder = "Add label",
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
            )

            if (labelFocused) {
                LabelSuggestions(
                    currentLabel = form.label.orEmpty(),
                    recentLabels = recentLabels,
                    onLabelSelect = onLabelChange,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        if (isHabit) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HabitIcon(
                    completed = form.status == TaskStatus.Completed,
                    color = form.priority.priorityColor()
                )
                Text(
                    text = "Every day · auto-added to My Day",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PriorityPicker(selected = form.priority, onSelect = onPriorityChange, enabled = enabled)
            }
        } else {
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
                    onTimeChange = onTimeChange,
                    supportsEndTime = !isHabit,
                    enabled = enabled,
                    isOverdue = form.isOverdue()
                )
                PriorityPicker(selected = form.priority, onSelect = onPriorityChange, enabled = enabled)
            }
        }

        AppOutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 3,
            placeholder = namePlaceholder,
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
            enabled = enabled,
            visualTransformation = remember { MarkdownVisualTransformation() }
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

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            form.listId?.let { listId ->
                ListPicker(
                    selectedListId = listId,
                    lists = availableLists,
                    onListChange = onListChange,
                    enabled = enabled
                )
            }
            TagPicker(
                availableTags = availableTags,
                selectedTagIds = form.selectedTagIds,
                onTagToggle = onTagToggle,
                onNewTagClick = onNewTagClick,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun DailyPlanSection(
    item: DailyPlanItem?,
    onTimeChange: (Int?, Int?) -> Unit,
    onStatusChange: () -> Unit,
    onDelete: (Long) -> Unit,
    onStartSprint: (DailyPlanItem) -> Unit,
    onStartOngoingSprint: (DailyPlanItem) -> Unit,
    enabled: Boolean = true
) {
    if (item == null) return
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colorScheme.primary
            )
            Text(
                text = "MY DAY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                letterSpacing = 0.5.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.primaryContainer.copy(alpha = 0.2f))
                .border(
                    width = 1.dp,
                    color = colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeRangePicker(
                    startTimeMinutes = item.startTimeMinutes,
                    endTimeMinutes = item.endTimeMinutes,
                    onTimeChange = onTimeChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    isOverdue = item.isOverdue(today()),
                    clearEnabled = true
                )

                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (enabled) {
                        if (item.status == DailyPlanItemStatus.Planned && item.startTimeMinutes != null) {
                            IconButton(
                                onClick = { onStartOngoingSprint(item) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Focus ongoing",
                                    modifier = Modifier.size(18.dp),
                                    tint = colorScheme.primary
                                )
                            }
                        }

                        if (item.status == DailyPlanItemStatus.Planned) {
                            IconButton(
                                onClick = { onStartSprint(item) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Start Focus",
                                    modifier = Modifier.size(18.dp),
                                    tint = colorScheme.primary
                                )
                            }
                        }
                    }


                    IconButton(
                        onClick = { onDelete(item.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete from My Day",
                            modifier = Modifier.size(18.dp),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    if (enabled) {
                        IconButton(
                            onClick = onStatusChange,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (item.status == DailyPlanItemStatus.Done) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check,
                                contentDescription = "Done from My Day",
                                modifier = Modifier.size(18.dp),
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteFormContent(
    form: TaskEditorState.NoteForm,
    availableLists: List<ListItem>,
    availableTags: List<TagItem>,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onListChange: (Long) -> Unit,
    onDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onTagToggle: (Long) -> Unit,
    onLabelChange: (String) -> Unit,
    onNewTagClick: () -> Unit,
    recentLabels: List<String>,
    enabled: Boolean = true
) {
    var labelFocused by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppOutlinedTextField(
                    value = form.label.orEmpty(),
                    onValueChange = onLabelChange,
                    modifier = Modifier.widthIn(max = 120.dp).onFocusChanged { labelFocused = it.isFocused },
                    textStyle = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    placeholder = "Add label",
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                )
                Spacer(Modifier.weight(1f))
            }

            if (labelFocused) {
                LabelSuggestions(
                    currentLabel = form.label.orEmpty(),
                    recentLabels = recentLabels,
                    onLabelSelect = onLabelChange,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

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
                onTimeChange = { start, _ -> onStartTimeChange(start) },
                supportsEndTime = false,
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
            modifier = Modifier.fillMaxWidth().height(130.dp),
            visualTransformation = remember { MarkdownVisualTransformation() }
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            form.listId?.let { listId ->
                ListPicker(
                    selectedListId = listId,
                    lists = availableLists,
                    onListChange = onListChange,
                    enabled = enabled
                )
            }
            TagPicker(
                availableTags = availableTags,
                selectedTagIds = form.selectedTagIds,
                onTagToggle = onTagToggle,
                onNewTagClick = onNewTagClick,
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
