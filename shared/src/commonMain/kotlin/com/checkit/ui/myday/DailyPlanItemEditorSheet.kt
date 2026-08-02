package com.checkit.ui.myday

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.TaskTag
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DeleteOverflowMenu
import com.checkit.ui.components.RichTextComposer
import com.checkit.ui.components.TagPicker
import com.checkit.ui.components.TagTitleAppender
import com.checkit.ui.components.TimePicker
import com.checkit.ui.components.TimeRangePicker
import com.checkit.ui.tasks.views.currentTimeMinutes
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyPlanItemEditorSheet(
    state: DailyPlanItemEditorState,
    availableTags: List<TaskTag>,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onStatusChange: (Boolean) -> Unit,
    onSourceChange: (DailyPlanItemSource) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onTagToggle: (Long) -> Unit,
    onNewTagClick: () -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onStartSprint: () -> Unit,
    onStartOngoingSprint: () -> Unit
) {
    val enabled = state.isEditableByDate()

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .padding(bottom = 24.dp)
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        DailyPlanItemSheetHeader(
            state = state,
            onDelete = onDelete
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DailyPlanItemFormContent(
                    state = state,
                    availableTags = availableTags,
                    onTitleChange = onTitleChange,
                    onNoteChange = onNoteChange,
                    onStatusChange = onStatusChange,
                    onSourceChange = onSourceChange,
                    onStartTimeChange = onStartTimeChange,
                    onEndTimeChange = onEndTimeChange,
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
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
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
            if (state.canDelete) {
                DeleteOverflowMenu(onDelete = onDelete)
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isAddMode) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to My Day")
                }
            } else {
                if (state.source == DailyPlanItemSource.MyDayTask) {
                    if (state.status == DailyPlanItemStatus.Planned && state.startTimeMinutes != null) {
                        Button(
                            onClick = onStartOngoingSprint,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Focus ongoing")
                        }
                    }

                    Button(
                        onClick = onStartSprint,
                        modifier = if (state.status == DailyPlanItemStatus.Planned && state.startTimeMinutes != null) {
                            Modifier.weight(1f)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (state.status == DailyPlanItemStatus.Done) "Start new session" else "Start focus")
                    }
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
    availableTags: List<TaskTag>,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onStatusChange: (Boolean) -> Unit,
    onSourceChange: (DailyPlanItemSource) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onTagToggle: (Long) -> Unit,
    onNewTagClick: () -> Unit,
    enabled: Boolean
) {
    val sourceLocked = state.isEditMode
    val displaySource = state.displaySource()
    val doneChecked = state.status == DailyPlanItemStatus.Done
    val doneTypeChecked = state.source == DailyPlanItemSource.MyDayTask
    val reminderChecked = state.source == DailyPlanItemSource.MyDayReminder

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppOutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            minLines = 1,
            maxLines = 3,
            placeholder = displaySource.titlePlaceholder(),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )

        RichTextComposer(
            value = state.note,
            onValueChange = onNoteChange,
            placeholder = displaySource.notePlaceholder(),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )

        if (sourceLocked) {
            FixedTypeControls(
                source = displaySource,
                doneChecked = doneChecked,
                onDoneChange = onStatusChange,
                enabled = enabled
            )
        } else {
            AddModeIntentControls(
                doneTypeChecked = doneTypeChecked,
                reminderChecked = reminderChecked,
                onDoneTypeChange = { checked ->
                    val nextSource = if (checked) DailyPlanItemSource.MyDayTask else DailyPlanItemSource.MyDayNote
                    val nextStatus = nextSource.inferredAddStatus(state.startTimeMinutes)
                    onStatusChange(nextStatus == DailyPlanItemStatus.Done)
                    onSourceChange(nextSource)
                },
                onReminderChange = { checked ->
                    val nextSource = if (checked) {
                        DailyPlanItemSource.MyDayReminder
                    } else if (doneTypeChecked) {
                        DailyPlanItemSource.MyDayTask
                    } else {
                        DailyPlanItemSource.MyDayNote
                    }
                    val nextStatus = nextSource.inferredAddStatus(state.startTimeMinutes)
                    onStatusChange(nextStatus == DailyPlanItemStatus.Done)
                    onSourceChange(nextSource)
                },
                enabled = enabled
            )
        }

        TimeSection(
            source = displaySource,
            startTimeMinutes = state.startTimeMinutes,
            endTimeMinutes = state.endTimeMinutes,
            isOverdue = state.isOverdue,
            onStartTimeChange = { timeMinutes ->
                onStartTimeChange(timeMinutes)
                if (!sourceLocked) {
                    val nextStatus = displaySource.inferredAddStatus(timeMinutes)
                    onStatusChange(nextStatus == DailyPlanItemStatus.Done)
                }
            },
            onEndTimeChange = onEndTimeChange,
            enabled = enabled
        )

        LabeledTagPicker(
            source = displaySource,
            availableTags = availableTags,
            selectedTagIds = state.selectedTagIds,
            onTagToggle = { tagId ->
                onTagToggle(tagId)
                availableTags.find { it.id == tagId }?.let { tag ->
                    onTitleChange(TagTitleAppender.appendTagActionText(state.title, tag.name))
                }
            },
            onNewTagClick = onNewTagClick,
            enabled = enabled
        )
    }
}

@Composable
private fun FixedTypeControls(
    source: DailyPlanItemSource,
    doneChecked: Boolean,
    onDoneChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    if (!source.usesStatusControl()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = source.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = source.statusTitle(doneChecked),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            Switch(
                checked = doneChecked,
                onCheckedChange = onDoneChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun TimeSection(
    source: DailyPlanItemSource,
    startTimeMinutes: Int?,
    endTimeMinutes: Int?,
    isOverdue: Boolean,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = source.timeLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        if (source.usesTimePicker()) {
            TimePicker(
                label = "",
                timeMinutes = startTimeMinutes,
                initialTimeMinutes = currentTimeMinutes(),
                onTimeChange = onStartTimeChange,
                enabled = enabled,
                isOverdue = isOverdue
            )
        } else {
            TimeRangePicker(
                startTimeMinutes = startTimeMinutes,
                endTimeMinutes = endTimeMinutes,
                onStartTimeChange = onStartTimeChange,
                onEndTimeChange = onEndTimeChange,
                enabled = enabled,
                isOverdue = isOverdue
            )
        }
    }
}

@Composable
private fun AddModeIntentControls(
    doneTypeChecked: Boolean,
    reminderChecked: Boolean,
    onDoneTypeChange: (Boolean) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = doneTypeChecked,
                    onCheckedChange = onDoneTypeChange,
                    enabled = enabled
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reminder",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = reminderChecked,
                    onCheckedChange = onReminderChange,
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun LabeledTagPicker(
    source: DailyPlanItemSource,
    availableTags: List<TaskTag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit,
    onNewTagClick: () -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = source.tagsLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        TagPicker(
            availableTags = availableTags,
            selectedTagIds = selectedTagIds,
            onTagToggle = onTagToggle,
            onNewTagClick = onNewTagClick,
            enabled = enabled
        )
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
    date > today().minus(2, DateTimeUnit.DAY)

private fun DailyPlanItemSource.titlePlaceholder(): String = when (this) {
    DailyPlanItemSource.ExistingTask,
    DailyPlanItemSource.MyDayTask -> "Task name"
    DailyPlanItemSource.MyDayNote -> "Note title"
    DailyPlanItemSource.MyDayReminder -> "Reminder"
}

private fun DailyPlanItemSource.notePlaceholder(): String? = when (this) {
    DailyPlanItemSource.ExistingTask,
    DailyPlanItemSource.MyDayTask,
    DailyPlanItemSource.MyDayNote,
    DailyPlanItemSource.MyDayReminder -> "Add details"
}

private fun DailyPlanItemSource.statusTitle(doneChecked: Boolean): String = when (this) {
    DailyPlanItemSource.ExistingTask,
    DailyPlanItemSource.MyDayTask -> if (doneChecked) "Completed" else "Not completed"
    DailyPlanItemSource.MyDayReminder -> if (doneChecked) "Reminder passed" else "Reminder pending"
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

private fun DailyPlanItemSource.usesTimePicker(): Boolean =
    this == DailyPlanItemSource.MyDayNote || this == DailyPlanItemSource.MyDayReminder

private fun DailyPlanItemSource.timeLabel(): String = "Time"

private fun DailyPlanItemSource.tagsLabel(): String = "Tags"

private fun DailyPlanItemSource.icon(): ImageVector = when (this) {
    DailyPlanItemSource.MyDayNote -> Icons.AutoMirrored.Filled.Notes
    DailyPlanItemSource.MyDayReminder -> Icons.Default.Schedule
    else -> Icons.Default.TaskAlt
}
