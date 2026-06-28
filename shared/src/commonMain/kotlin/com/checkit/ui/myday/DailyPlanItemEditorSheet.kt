package com.checkit.ui.myday

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.checkit.ui.components.TagPicker
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
    onAdd: () -> Unit,
    onDelete: () -> Unit
) {
    val enabled = state.isEditableByDate()

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight()
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
                    enabled = enabled
                )
            }
        }
        DailyPlanItemSheetFooter(state.isAddMode, enabled, onAdd)
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
                    text = if (state.isAddMode) "Add to My Day" else "Edit My Day item",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.displaySource().supportingLabel(),
                    style = MaterialTheme.typography.bodyMedium,
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
    isAddMode: Boolean,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    if (isAddMode && enabled) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to My Day")
            }
        }
    }
}

@Composable
private fun SourceIconBadge(source: DailyPlanItemSource) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = source.icon(),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
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

        AppOutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            ),
            minLines = 5,
            maxLines = 10,
            placeholder = displaySource.notePlaceholder(),
            enabled = enabled
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
                inferredSource = displaySource,
                inferredStatus = state.inferredAddStatus(),
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
            isEditMode = state.isEditMode,
            startTimeMinutes = state.startTimeMinutes,
            endTimeMinutes = state.endTimeMinutes,
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
            isEditMode = state.isEditMode,
            availableTags = availableTags,
            selectedTagIds = state.selectedTagIds,
            onTagToggle = onTagToggle,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TypeSummary(source = source, label = "Saved as ${source.shortLabel().lowercase()}")
            if (source.usesStatusControl()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Switch(
                        checked = doneChecked,
                        onCheckedChange = onDoneChange,
                        enabled = enabled
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = source.statusTitle(doneChecked),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = source.statusMessage(doneChecked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSection(
    source: DailyPlanItemSource,
    isEditMode: Boolean,
    startTimeMinutes: Int?,
    endTimeMinutes: Int?,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = source.timeLabel(isEditMode),
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
                enabled = enabled
            )
        } else {
            TimeRangePicker(
                startTimeMinutes = startTimeMinutes,
                endTimeMinutes = endTimeMinutes,
                onStartTimeChange = onStartTimeChange,
                onEndTimeChange = onEndTimeChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun AddModeIntentControls(
    doneTypeChecked: Boolean,
    reminderChecked: Boolean,
    inferredSource: DailyPlanItemSource,
    inferredStatus: DailyPlanItemStatus,
    onDoneTypeChange: (Boolean) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Celebrate a win",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (doneTypeChecked) {
                            "This becomes a tiny victory in your day"
                        } else {
                            "Leave it as a light note for now"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = doneTypeChecked,
                    onCheckedChange = onDoneTypeChange,
                    enabled = enabled
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gentle reminder",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (reminderChecked) {
                            "Future you gets a well-timed nudge"
                        } else {
                            "No ping, just calmly saved in My Day"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminderChecked,
                    onCheckedChange = onReminderChange,
                    enabled = enabled
                )
            }
            TypeSummary(source = inferredSource, label = inferredSource.addModeFeelingLabel(inferredStatus))
        }
    }
}

@Composable
private fun LabeledTagPicker(
    source: DailyPlanItemSource,
    isEditMode: Boolean,
    availableTags: List<TaskTag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit,
    enabled: Boolean
) {
    if (availableTags.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = source.tagsLabel(isEditMode),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        TagPicker(
            availableTags = availableTags,
            selectedTagIds = selectedTagIds,
            onTagToggle = onTagToggle,
            enabled = enabled
        )
    }
}

@Composable
private fun TypeSummary(source: DailyPlanItemSource, label: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = source.icon(),
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun DailyPlanItemEditorState.displaySource(): DailyPlanItemSource =
    source

private fun DailyPlanItemEditorState.inferredAddStatus(): DailyPlanItemStatus =
    source.inferredAddStatus(startTimeMinutes)

private fun DailyPlanItemSource.inferredAddStatus(startTimeMinutes: Int?): DailyPlanItemStatus =
    if (infersAddStatusFromStartTime() && startTimeMinutes != null && startTimeMinutes < currentTimeMinutes()) {
        DailyPlanItemStatus.Done
    } else {
        DailyPlanItemStatus.Planned
    }

private fun DailyPlanItemSource.infersAddStatusFromStartTime(): Boolean =
    this == DailyPlanItemSource.MyDayTask || this == DailyPlanItemSource.MyDayReminder

private fun DailyPlanItemEditorState.isEditableByDate(): Boolean =
    date > today().minus(2, DateTimeUnit.DAY)

private fun DailyPlanItemSource.titlePlaceholder(): String = when (this) {
    DailyPlanItemSource.ExistingTask -> "Give this plan a clear little name"
    DailyPlanItemSource.MyDayTask -> "What win should today remember?"
    DailyPlanItemSource.MyDayNote -> "Catch the thought before it drifts"
    DailyPlanItemSource.MyDayReminder -> "What should future you remember?"
}

private fun DailyPlanItemSource.notePlaceholder(): String? = when (this) {
    DailyPlanItemSource.ExistingTask -> "Add the context that will make this easy later"
    DailyPlanItemSource.MyDayTask -> "Add the tiny detail that made it satisfying"
    DailyPlanItemSource.MyDayNote -> "Jot the useful details while they are still fresh"
    DailyPlanItemSource.MyDayReminder -> "Add a kind nudge, reason, or place"
}

private fun DailyPlanItemSource.shortLabel(): String = when (this) {
    DailyPlanItemSource.ExistingTask -> "Task"
    DailyPlanItemSource.MyDayTask -> "Done item"
    DailyPlanItemSource.MyDayNote -> "Note"
    DailyPlanItemSource.MyDayReminder -> "Reminder"
}

private fun DailyPlanItemSource.statusTitle(doneChecked: Boolean): String = when (this) {
    DailyPlanItemSource.ExistingTask -> if (doneChecked) "Task delivered" else "Task still open"
    DailyPlanItemSource.MyDayTask -> if (doneChecked) "Win delivered" else "Win still ahead"
    DailyPlanItemSource.MyDayReminder -> if (doneChecked) "Reminder has passed" else "Reminder is waiting"
    DailyPlanItemSource.MyDayNote -> "Note saved"
}

private fun DailyPlanItemSource.statusMessage(doneChecked: Boolean): String = when (this) {
    DailyPlanItemSource.ExistingTask -> if (doneChecked) {
        "This planned task is complete for today"
    } else {
        "Keep it on today's plan until it lands"
    }
    DailyPlanItemSource.MyDayTask -> if (doneChecked) {
        "Count it as a completed moment"
    } else {
        "Keep a little room to finish it later"
    }
    DailyPlanItemSource.MyDayReminder -> if (doneChecked) {
        "The reminder time is behind you now"
    } else {
        "A gentle nudge is still coming"
    }
    DailyPlanItemSource.MyDayNote -> "No completion needed for notes"
}

private fun DailyPlanItemSource.supportingLabel(): String = when (this) {
    DailyPlanItemSource.ExistingTask -> "A task already planned for today"
    DailyPlanItemSource.MyDayTask -> "Log something you finished"
    DailyPlanItemSource.MyDayNote -> "Capture a quick note"
    DailyPlanItemSource.MyDayReminder -> "Keep a timed reminder"
}

private fun DailyPlanItemSource.addModeFeelingLabel(status: DailyPlanItemStatus): String = when (this) {
    DailyPlanItemSource.MyDayTask -> if (status == DailyPlanItemStatus.Done) {
        "Looks like a completed win"
    } else {
        "Ready for a small future victory"
    }
    DailyPlanItemSource.MyDayNote -> "Saved as a bright note for today"
    DailyPlanItemSource.MyDayReminder -> if (status == DailyPlanItemStatus.Done) {
        "The reminder time has already passed"
    } else {
        "Future you gets a gentle nudge"
    }
    DailyPlanItemSource.ExistingTask -> "Connected to a planned task"
}

private fun DailyPlanItemSource.usesStatusControl(): Boolean =
    this != DailyPlanItemSource.MyDayNote

private fun DailyPlanItemSource.usesTimePicker(): Boolean =
    this == DailyPlanItemSource.MyDayNote || this == DailyPlanItemSource.MyDayReminder

private fun DailyPlanItemSource.timeLabel(isEditMode: Boolean): String = when (this) {
    DailyPlanItemSource.ExistingTask -> if (isEditMode) "Adjust the planned window" else "Choose a time window"
    DailyPlanItemSource.MyDayTask -> if (isEditMode) "Refine when it happened" else "When did it happen?"
    DailyPlanItemSource.MyDayNote -> if (isEditMode) "Adjust the note time" else "Give this note a time"
    DailyPlanItemSource.MyDayReminder -> if (isEditMode) "Adjust the reminder time" else "When should it remind you?"
}

private fun DailyPlanItemSource.tagsLabel(isEditMode: Boolean): String = when (this) {
    DailyPlanItemSource.ExistingTask -> if (isEditMode) "Tune its task tags" else "Group this task"
    DailyPlanItemSource.MyDayTask -> if (isEditMode) "Tune the win tags" else "Mark the kind of win"
    DailyPlanItemSource.MyDayNote -> if (isEditMode) "Tune the note tags" else "Give this thought a home"
    DailyPlanItemSource.MyDayReminder -> if (isEditMode) "Tune the reminder tags" else "Place this nudge where it belongs"
}

private fun DailyPlanItemSource.icon(): ImageVector = when (this) {
    DailyPlanItemSource.MyDayNote -> Icons.AutoMirrored.Filled.Notes
    DailyPlanItemSource.MyDayReminder -> Icons.Default.Schedule
    else -> Icons.Default.TaskAlt
}
