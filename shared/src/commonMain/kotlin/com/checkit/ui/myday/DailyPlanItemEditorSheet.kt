package com.checkit.ui.myday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.TaskTag
import com.checkit.ui.DailyPlanItemEditorState
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.TagPicker
import com.checkit.ui.components.TimePicker
import com.checkit.ui.components.TimeRangePicker
import com.checkit.ui.tasks.currentTimeMinutes
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

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
    onAdd: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enabled = state.isEditableByDate()
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
                onDelete = onDelete,
                enabled = enabled
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    DailyPlanItemFormContent(
                        state = state,
                        availableTags = availableTags,
                        onDoneTitleChange = onDoneTitleChange,
                        onDoneNoteChange = onDoneNoteChange,
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
}

@Composable
private fun DailyPlanItemSheetHeader(
    state: DailyPlanItemEditorState,
    onSourceChange: (DailyPlanItemSource) -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        DailyPlanSourceSwitch(
            selected = state.source,
            onSelect = onSourceChange,
            enabled = enabled,
            modifier = Modifier.align(Alignment.Center)
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
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
private fun DailyPlanItemSheetFooter(
    isAddMode: Boolean,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    if (isAddMode && enabled) {
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

@Composable
private fun DailyPlanSourceSwitch(
    selected: DailyPlanItemSource,
    onSelect: (DailyPlanItemSource) -> Unit,
    enabled: Boolean,
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
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    Icon(
                        imageVector = if (source == DailyPlanItemSource.CheckInNote) Icons.Default.Notes else Icons.Default.TaskAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = { Text(label) },
                colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.primaryContainer, activeContentColor = MaterialTheme.colorScheme.primary)
            )
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
    onTagToggle: (Long) -> Unit,
    enabled: Boolean
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
                placeholder = "Title (optional)",
                enabled = enabled
            )

            AppOutlinedTextField(
                value = state.note,
                onValueChange = onDoneNoteChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 5,
                placeholder = "Note",
                enabled = enabled
            )

            TimePicker(
                label = "Start",
                timeMinutes = state.startTimeMinutes,
                initialTimeMinutes = currentTimeMinutes(),
                onTimeChange = onStartTimeChange,
                enabled = enabled
            )

            TagPicker(
                availableTags = availableTags,
                selectedTagIds = state.selectedTagIds,
                onTagToggle = onTagToggle,
                enabled = enabled
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
                placeholder = "What have you done?",
                enabled = enabled
            )

            AppOutlinedTextField(
                value = state.note,
                onValueChange = onDoneNoteChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 5,
                enabled = enabled
            )

            TimeRangePicker(
                startTimeMinutes = state.startTimeMinutes,
                endTimeMinutes = state.endTimeMinutes,
                durationMinutes = state.durationMinutes(),
                onStartTimeChange = onStartTimeChange,
                onEndTimeChange = onEndTimeChange,
                enabled = enabled
            )

            TagPicker(
                availableTags = availableTags,
                selectedTagIds = state.selectedTagIds,
                onTagToggle = onTagToggle,
                enabled = enabled
            )
        }
    }
}

private fun DailyPlanItemEditorState.isEditableByDate(): Boolean =
    date > today().minus(2, DateTimeUnit.DAY)

private fun DailyPlanItemEditorState.durationMinutes(): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
    return (end - start).takeIf { it >= 0 }
}
