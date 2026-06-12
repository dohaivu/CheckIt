package com.checkit.ui.myday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TaskTag
import com.checkit.ui.DailyPlanItemEditorState
import com.checkit.ui.EditorMode
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DetailChip
import com.checkit.ui.components.TagPicker
import com.checkit.ui.components.TaskTagPill
import com.checkit.ui.components.TimePicker
import com.checkit.ui.components.TimeRangeDetailChip
import com.checkit.ui.components.TimeRangePicker
import com.checkit.ui.tasks.currentTimeMinutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyPlanItemEditorSheet(
    state: DailyPlanItemEditorState,
    availableTags: List<TaskTag>,
    onDismiss: () -> Unit,
    onDoneTitleChange: (String) -> Unit,
    onDoneNoteChange: (String) -> Unit,
    onSourceChange: (DailyPlanItemSource) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onTagToggle: (Long) -> Unit,
    onEdit: () -> Unit,
    onAdd: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    onOpenTask: (() -> Unit)?
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
            DailyPlanItemSheetHeader(
                state = state,
                onSourceChange = onSourceChange,
                onEdit = onEdit,
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
                        DailyPlanItemViewContent(
                            state = state,
                            availableTags = availableTags,
                            hasTask = onOpenTask != null
                        )
                    } else {
                        DailyPlanItemFormContent(
                            state = state,
                            availableTags = availableTags,
                            onDoneTitleChange = onDoneTitleChange,
                            onDoneNoteChange = onDoneNoteChange,
                            onStartTimeChange = onStartTimeChange,
                            onEndTimeChange = onEndTimeChange,
                            onTagToggle = onTagToggle
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
                            if (state.source == DailyPlanItemSource.ExistingTask && state.status != DailyPlanItemStatus.Done) {
                                Button(onClick = onDone) {
                                    Text("Done")
                                }
                            }
                        }
                    }
                }
                if(state.isAddMode) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = onAdd) {
                                Text("Add CheckIn")
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
    onSourceChange: (DailyPlanItemSource) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        if (state.isAddMode || state.isEditMode && state.taskId == null) {
            DailyPlanSourceSwitch(
                selected = state.source,
                onSelect = onSourceChange,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Text(
                text = when (state.mode) {
                    EditorMode.Add -> "Add item"
                    EditorMode.View -> if (state.source == DailyPlanItemSource.CheckInNote) "Note" else "My Day item"
                    EditorMode.Edit -> "CheckIn"
                },
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            if (state.isViewMode) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
            if (state.canDelete) {
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
        }
    }
}

@Composable
private fun DailyPlanSourceSwitch(
    selected: DailyPlanItemSource,
    onSelect: (DailyPlanItemSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        DailyPlanItemSource.CheckInNote to "Status",
        DailyPlanItemSource.CheckInManualDone to "Done"
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (source, label) ->
            SegmentedButton(
                selected = selected == source,
                onClick = { onSelect(source) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    Icon(
                        imageVector = if (source == DailyPlanItemSource.CheckInNote) Icons.Default.Notes else Icons.Default.TaskAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyPlanItemViewContent(
    state: DailyPlanItemEditorState,
    availableTags: List<TaskTag>,
    hasTask: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = state.title.ifBlank {
                if (state.source == DailyPlanItemSource.CheckInNote) "Empty note" else "Untitled item"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        state.note.takeIf { state.source != DailyPlanItemSource.CheckInNote && it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val selectedTags = remember(state.selectedTagIds, availableTags) {
            availableTags.filter { it.id in state.selectedTagIds }
        }
        if (selectedTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedTags.forEach { tag ->
                    TaskTagPill(tag = tag, selected = false, onClick = {})
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailChip(Icons.Default.Event, "My Day")
            TimeRangeDetailChip(state.startTimeMinutes, state.endTimeMinutes)
            if (state.source == DailyPlanItemSource.CheckInNote) {
                DetailChip(Icons.Default.Notes, "Note")
            } else {
                DetailChip(Icons.Default.CheckCircle, if (state.status == DailyPlanItemStatus.Done) "Done" else "Planned")
                if (hasTask) {
                    DetailChip(Icons.Default.TaskAlt, "Task")
                }
            }
        }
    }
}

@Composable
private fun DailyPlanItemFormContent(
    state: DailyPlanItemEditorState,
    availableTags: List<TaskTag>,
    onDoneTitleChange: (String) -> Unit,
    onDoneNoteChange: (String) -> Unit,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: (Int?) -> Unit,
    onTagToggle: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (state.source == DailyPlanItemSource.CheckInNote) {
            AppOutlinedTextField(
                value = state.title,
                onValueChange = onDoneTitleChange,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                placeholder = "What have you done?"
            )

            AppOutlinedTextField(
                value = state.note,
                onValueChange = onDoneNoteChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 5
            )

            TimePicker(
                label = "Start",
                timeMinutes = state.startTimeMinutes,
                initialTimeMinutes = currentTimeMinutes(),
                onTimeChange = onStartTimeChange
            )

            TagPicker(
                availableTags = availableTags,
                selectedTagIds = state.selectedTagIds,
                onTagToggle = onTagToggle
            )
        } else {
            AppOutlinedTextField(
                value = state.title,
                onValueChange = onDoneTitleChange,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                placeholder = "What have you done?"
            )

            AppOutlinedTextField(
                value = state.note,
                onValueChange = onDoneNoteChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 5
            )

            TimeRangePicker(
                startTimeMinutes = state.startTimeMinutes,
                endTimeMinutes = state.endTimeMinutes,
                durationMinutes = state.durationMinutes(),
                onStartTimeChange = onStartTimeChange,
                onEndTimeChange = onEndTimeChange
            )

            TagPicker(
                availableTags = availableTags,
                selectedTagIds = state.selectedTagIds,
                onTagToggle = onTagToggle
            )
        }
    }
}

private fun DailyPlanItemEditorState.durationMinutes(): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
    return (end - start).takeIf { it >= 0 }
}
