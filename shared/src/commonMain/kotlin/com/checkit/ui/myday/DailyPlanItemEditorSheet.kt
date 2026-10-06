package com.checkit.ui.myday

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TagItem
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AutocompleteTextField
import com.checkit.ui.components.DatePicker
import com.checkit.ui.components.EditorOverflowMenu
import com.checkit.ui.components.LabelTextField
import com.checkit.ui.components.MarkdownTextField
import com.checkit.ui.components.TagPicker
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

@Composable
internal fun DailyPlanItemEditorSheet(
    state: DailyPlanItemEditorState,
    availableTags: List<TagItem>,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onStatusChange: (Boolean) -> Unit,
    onSourceChange: (DailyPlanItemSource) -> Unit,
    onDateChange: (LocalDate?) -> Unit,
    onTimeChange: (Int?, Int?) -> Unit,
    onTagToggle: (String) -> Unit,
    onNewTagClick: () -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onStartSprint: () -> Unit,
    onStartOngoingSprint: () -> Unit,
    onUpgradeToTask: () -> Unit,
    recentLabels: List<String> = emptyList(),
    suggestions: List<String> = emptyList(),
) {
    val enabled = state.isEditableByDate()

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .padding(bottom = 24.dp)
    ) {
        DailyPlanItemSheetHeader(
            state = state,
            onDelete = onDelete,
            onDuplicate = onDuplicate,
            onUpgradeToTask = onUpgradeToTask
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DailyPlanItemFormContent(
                    state = state,
                    availableTags = availableTags,
                    recentLabels = recentLabels,
                    suggestions = suggestions,
                    onTitleChange = onTitleChange,
                    onNoteChange = onNoteChange,
                    onLabelChange = onLabelChange,
                    onStatusChange = onStatusChange,
                    onSourceChange = onSourceChange,
                    onDateChange = onDateChange,
                    onTimeChange = onTimeChange,
                    onTagToggle = onTagToggle,
                    onNewTagClick = onNewTagClick,
                    enabled = enabled
                )
            }
        }
        DailyPlanItemSheetFooter(
            state = state,
            enabled = enabled,
            onAdd = onAdd,
            onStartSprint = onStartSprint,
            onStartOngoingSprint = onStartOngoingSprint
        )
    }
}

@Composable
private fun DailyPlanItemSheetHeader(
    state: DailyPlanItemEditorState,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onUpgradeToTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceIconBadge(source = state.displaySource())
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.sheetTitle(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.displaySource().supportingLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val hasDuplicate = state.isEditMode && state.source == DailyPlanItemSource.MyDayTask
            val hasUpgrade = state.isEditMode && state.taskId == null
            val hasDelete = state.canDelete

            if (hasDuplicate || hasUpgrade || hasDelete) {
                EditorOverflowMenu { onDismiss ->
                    if (hasDuplicate) {
                        DropdownMenuItem(
                            text = { Text("Schedule new session") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                onDismiss()
                                onDuplicate()
                            }
                        )
                    }
                    if (hasUpgrade) {
                        DropdownMenuItem(
                            text = { Text("Upgrade to task") },
                            leadingIcon = { Icon(Icons.Default.TaskAlt, contentDescription = null) },
                            onClick = {
                                onDismiss()
                                onUpgradeToTask()
                            }
                        )
                    }
                    if (hasDelete) {
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
}

@Composable
private fun DailyPlanItemSheetFooter(
    state: DailyPlanItemEditorState,
    enabled: Boolean,
    onAdd: () -> Unit,
    onStartSprint: () -> Unit,
    onStartOngoingSprint: () -> Unit
) {
    if (enabled) {
        if (state.isAddMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to My Day")
                }
            }
        } else if (state.source == DailyPlanItemSource.MyDayTask) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (state.status == DailyPlanItemStatus.Planned && state.startTimeMinutes != null) {
                    Button(
                        onClick = onStartOngoingSprint,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Focus ongoing")
                    }
                }

                Button(
                    onClick = onStartSprint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (state.status == DailyPlanItemStatus.Done) "Start new focus" else "Start focus")
                }
            }
        }
    }
}

@Composable
private fun SourceIconBadge(source: DailyPlanItemSource) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = source.icon(),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DailyPlanItemFormContent(
    state: DailyPlanItemEditorState,
    availableTags: List<TagItem>,
    recentLabels: List<String>,
    suggestions: List<String> = emptyList(),
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onStatusChange: (Boolean) -> Unit,
    onSourceChange: (DailyPlanItemSource) -> Unit,
    onDateChange: (LocalDate?) -> Unit,
    onTimeChange: (Int?, Int?) -> Unit,
    onTagToggle: (String) -> Unit,
    onNewTagClick: () -> Unit,
    enabled: Boolean
) {
    val sourceLocked = state.isEditMode
    val displaySource = state.displaySource()
    val doneChecked = state.status == DailyPlanItemStatus.Done

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LabelTextField(
                value = state.label.orEmpty(),
                onValueChange = onLabelChange,
                recentLabels = recentLabels,
                placeholder = "Add label",
                enabled = enabled,
                maxWidth = 100.dp
            )
            state.nestedListItemId?.let {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Link, contentDescription = "item link", modifier = Modifier.size(20.dp))
            }
        }

        // Choice Chips for Type Selection (Add Mode) or Status Chip (Edit Mode)
        if (!sourceLocked) {
            TypeChoiceChips(
                selectedSource = state.source,
                onSourceSelected = { nextSource ->
                    val nextStatus = if (nextSource == DailyPlanItemSource.MyDayNote) {
                        DailyPlanItemStatus.Done
                    } else {
                        nextSource.inferredAddStatus(state.startTimeMinutes)
                    }
                    onStatusChange(nextStatus == DailyPlanItemStatus.Done)
                    onSourceChange(nextSource)
                },
                enabled = enabled
            )
        } else if (displaySource.usesStatusControl()) {
            StatusChoiceChip(
                source = displaySource,
                doneChecked = doneChecked,
                onDoneChange = onStatusChange,
                enabled = enabled
            )
        }

        AutocompleteTextField(
            value = state.title,
            onValueChange = onTitleChange,
            suggestions = suggestions,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            minLines = 1,
            maxLines = 2,
            placeholder = displaySource.titlePlaceholder(),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            isError = state.error != null
        )

        MarkdownTextField(
            value = state.note,
            onValueChange = onNoteChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            ),
            minLines = 3,
            maxLines = 5,
            placeholder = if (sourceLocked) null else "Add details",
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )

        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        TimeSection(
            source = displaySource,
            date = state.date,
            startTimeMinutes = state.startTimeMinutes,
            endTimeMinutes = state.endTimeMinutes,
            isOverdue = state.isOverdue,
            onDateChange = onDateChange,
            onTimeChange = { startTime, endTime ->
                onTimeChange(startTime, endTime)
                if (!sourceLocked) {
                    val nextStatus = displaySource.inferredAddStatus(startTime)
                    onStatusChange(nextStatus == DailyPlanItemStatus.Done)
                }
            },
            enabled = enabled
        )

        LabeledTagPicker(
            availableTags = availableTags,
            selectedTagIds = state.selectedTagIds,
            onTagToggle = onTagToggle,
            onNewTagClick = onNewTagClick,
            enabled = enabled
        )
    }
}

@Composable
private fun TypeChoiceChips(
    selectedSource: DailyPlanItemSource,
    onSourceSelected: (DailyPlanItemSource) -> Unit,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedSource == DailyPlanItemSource.MyDayTask,
            onClick = { onSourceSelected(DailyPlanItemSource.MyDayTask) },
            label = { Text("Task") },
            leadingIcon = {
                Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            enabled = enabled
        )
        FilterChip(
            selected = selectedSource == DailyPlanItemSource.MyDayNote,
            onClick = { onSourceSelected(DailyPlanItemSource.MyDayNote) },
            label = { Text("Note") },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            enabled = enabled
        )
        FilterChip(
            selected = selectedSource == DailyPlanItemSource.MyDayReminder,
            onClick = { onSourceSelected(DailyPlanItemSource.MyDayReminder) },
            label = { Text("Reminder") },
            leadingIcon = {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            enabled = enabled
        )
    }
}

@Composable
private fun StatusChoiceChip(
    source: DailyPlanItemSource,
    doneChecked: Boolean,
    onDoneChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = doneChecked,
            onClick = { onDoneChange(!doneChecked) },
            label = { Text(source.statusTitle(doneChecked)) },
            leadingIcon = {
                Icon(
                    imageVector = if (doneChecked) Icons.Default.TaskAlt else source.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            enabled = enabled
        )
    }
}

@Composable
private fun TimeSection(
    source: DailyPlanItemSource,
    date: LocalDate?,
    startTimeMinutes: Int?,
    endTimeMinutes: Int?,
    isOverdue: Boolean,
    onDateChange: (LocalDate?) -> Unit,
    onTimeChange: (Int?, Int?) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Schedule",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        DatePicker(
            date = date,
            startTimeMinutes = startTimeMinutes,
            endTimeMinutes = endTimeMinutes,
            onDateChange = onDateChange,
            onTimeChange = onTimeChange,
            supportsEndTime = source == DailyPlanItemSource.ExistingTask || source == DailyPlanItemSource.MyDayTask,
            enabled = enabled,
            isOverdue = isOverdue
        )
    }
}

@Composable
private fun LabeledTagPicker(
    availableTags: List<TagItem>,
    selectedTagIds: Set<String>,
    onTagToggle: (String) -> Unit,
    onNewTagClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier.weight(1f, fill = false),
            contentAlignment = Alignment.CenterEnd
        ) {
            TagPicker(
                availableTags = availableTags,
                selectedTagIds = selectedTagIds,
                onTagToggle = onTagToggle,
                onNewTagClick = onNewTagClick,
                enabled = enabled
            )
        }
    }
}

private fun DailyPlanItemEditorState.displaySource(): DailyPlanItemSource =
    source

private fun DailyPlanItemEditorState.sheetTitle(): String = when {
    isAddMode -> "Add to My Day"
    else -> when (source) {
        DailyPlanItemSource.ExistingTask,
        DailyPlanItemSource.MyDayTask -> "Edit task"
        DailyPlanItemSource.MyDayNote -> "Edit note"
        DailyPlanItemSource.MyDayReminder -> "Edit reminder"
    }
}

private fun DailyPlanItemEditorState.isEditableByDate(): Boolean =
    date > today().minus(3, DateTimeUnit.DAY)

private fun DailyPlanItemSource.titlePlaceholder(): String = when (this) {
    DailyPlanItemSource.ExistingTask,
    DailyPlanItemSource.MyDayTask -> "Task name"
    DailyPlanItemSource.MyDayNote -> "Note title"
    DailyPlanItemSource.MyDayReminder -> "Reminder"
}

private fun DailyPlanItemSource.statusTitle(doneChecked: Boolean): String = when (this) {
    DailyPlanItemSource.ExistingTask,
    DailyPlanItemSource.MyDayTask -> if (doneChecked) "Completed" else "Incomplete"
    DailyPlanItemSource.MyDayReminder -> if (doneChecked) "Passed" else "Pending"
    DailyPlanItemSource.MyDayNote -> "Saved"
}

private fun DailyPlanItemSource.supportingLabel(): String = when (this) {
    DailyPlanItemSource.ExistingTask -> "Planned task"
    DailyPlanItemSource.MyDayTask -> "Task for today"
    DailyPlanItemSource.MyDayNote -> "Note for today"
    DailyPlanItemSource.MyDayReminder -> "Timed reminder"
}

private fun DailyPlanItemSource.usesStatusControl(): Boolean =
    this != DailyPlanItemSource.MyDayNote

private fun DailyPlanItemSource.icon(): ImageVector = when (this) {
    DailyPlanItemSource.MyDayNote -> Icons.AutoMirrored.Filled.Notes
    DailyPlanItemSource.MyDayReminder -> Icons.Default.Schedule
    else -> Icons.Default.TaskAlt
}
