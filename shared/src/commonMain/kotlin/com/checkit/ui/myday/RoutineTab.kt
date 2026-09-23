package com.checkit.ui.myday

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.checkit.domain.AllWeekdays
import com.checkit.domain.Routine
import com.checkit.domain.RoutineStepTemplate
import com.checkit.domain.WeekdayPresetWeekdays
import com.checkit.domain.WeekdayPresetWeekends
import com.checkit.domain.isRoutineScheduled
import com.checkit.domain.routinePercent
import com.checkit.domain.usecase.toClockLabel
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DeleteOverflowMenu
import com.checkit.ui.components.MarkdownVisualTransformation
import com.checkit.ui.components.ReorderDragState
import com.checkit.ui.components.TimePicker
import com.checkit.ui.components.asMarkdownAnnotatedString
import com.checkit.ui.components.findReorderTarget
import com.checkit.ui.components.reorderableRowGraphics
import com.checkit.ui.shortName
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

private data class RoutineEditorState(
    val id: String?,
    val title: String,
    val description: String,
    val reminderMinutes: Int?,
    val activeWeekdays: Set<DayOfWeek>,
    val steps: List<RoutineStepTemplate>
)

@Composable
internal fun RoutineTab(
    routines: List<Routine>,
    checks: Map<String, Set<String>>,
    today: LocalDate,
    onToggleStep: (String, String) -> Unit,
    onSaveRoutine: (String?, String, String, Int?, Set<DayOfWeek>, List<RoutineStepTemplate>) -> Unit,
    onDeleteRoutine: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editor by remember { mutableStateOf<RoutineEditorState?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (routines.isNotEmpty()) {
                    Text(
                        text = "${routines.size} ${if (routines.size == 1) "routine" else "routines"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                FilledTonalButton(
                    onClick = {
                        editor = RoutineEditorState(id = null, title = "", description = "", reminderMinutes = null, activeWeekdays = AllWeekdays, steps = emptyList())
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New routine",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (routines.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "No routines yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Set up daily checklists that reset every day to build habits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        Button(
                            onClick = {
                                editor = RoutineEditorState(id = null, title = "", description = "", reminderMinutes = null, activeWeekdays = AllWeekdays, steps = emptyList())
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create routine", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            items(routines, key = { it.id }) { routine ->
                RoutineCard(
                    routine = routine,
                    checkedStepIds = checks[routine.id].orEmpty(),
                    scheduledToday = isRoutineScheduled(today, routine.activeWeekdays),
                    onToggleStep = { stepId -> onToggleStep(routine.id, stepId) },
                    onEdit = {
                        editor = RoutineEditorState(
                            id = routine.id,
                            title = routine.title,
                            description = routine.description,
                            reminderMinutes = routine.reminderMinutes,
                            activeWeekdays = routine.activeWeekdays,
                            steps = routine.steps
                        )
                    }
                )
            }
            item {
                Spacer(Modifier.height(36.dp))
            }
        }
    }

    editor?.let { state ->
        RoutineEditorSheet(
            state = state,
            onDismiss = { editor = null },
            onSave = { id, title, description, reminderMinutes, activeWeekdays, steps ->
                onSaveRoutine(id, title, description, reminderMinutes, activeWeekdays, steps)
                editor = null
            },
            onDelete = { id ->
                onDeleteRoutine(id)
                editor = null
            }
        )
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    checkedStepIds: Set<String>,
    scheduledToday: Boolean,
    onToggleStep: (String) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validStepIds = remember(routine.steps) { routine.steps.map { it.id }.toSet() }
    val percent = remember(routine.steps, checkedStepIds) {
        routinePercent(routine.steps.size, checkedStepIds, validStepIds)
    }
    val completedCount = remember(routine.steps, checkedStepIds) {
        routine.steps.count { it.id in checkedStepIds }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = routine.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Edit routine",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            if (routine.description.isNotBlank()) {
                Text(
                    text = routine.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                routine.reminderMinutes?.let { minutes ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = minutes.toClockLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (routine.activeWeekdays != AllWeekdays) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatActiveWeekdays(routine.activeWeekdays),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (routine.steps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$completedCount of ${routine.steps.size} completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (percent == 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val animatedProgress by animateFloatAsState(
                        targetValue = percent / 100f,
                        label = "routineProgress"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(CircleShape)
                                .background(
                                    if (percent == 100) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                        )
                    }
                }
            }

            if (routine.steps.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.alpha(if (scheduledToday) 1f else 0.55f)
                ) {
                    routine.steps.forEach { step ->
                        val checked = step.id in checkedStepIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    enabled = scheduledToday,
                                    onClick = { onToggleStep(step.id) }
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (checked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                },
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 1.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (checked) TextDecoration.LineThrough else null
                                    ),
                                    color = if (checked) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (step.description.isNotBlank()) {
                                    Text(
                                        text = step.description.asMarkdownAnnotatedString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
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
private fun RoutineEditorSheet(
    state: RoutineEditorState,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, Int?, Set<DayOfWeek>, List<RoutineStepTemplate>) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember(state) { mutableStateOf(state.title) }
    var description by remember(state) { mutableStateOf(state.description) }
    var reminderMinutes by remember(state) { mutableStateOf(state.reminderMinutes) }
    var activeWeekdays by remember(state) { mutableStateOf(state.activeWeekdays) }
    var steps by remember(state) { mutableStateOf(state.steps) }

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .padding(bottom = 12.dp),
        sheetGesturesEnabled = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.id == null) "New routine" else "Edit routine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.id != null) {
                        DeleteOverflowMenu(onDelete = { onDelete(state.id) })
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AppOutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2,
                        placeholder = "Routine name",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    AppOutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 2,
                        placeholder = "Add details (optional)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Reminder",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TimePicker(
                            label = "reminder",
                            timeMinutes = reminderMinutes,
                            initialTimeMinutes = 8 * 60,
                            onTimeChange = { reminderMinutes = it }
                        )
                    }
                }

                item {
                    WeekdayPicker(
                        selected = activeWeekdays,
                        onChange = { activeWeekdays = it }
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Steps",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (steps.isNotEmpty()) {
                                Text(
                                    text = "${steps.size} ${if (steps.size == 1) "step" else "steps"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        RoutineStepsEditor(
                            steps = steps,
                            onStepsChange = { steps = it }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                state.id,
                                title.trim(),
                                description.trim(),
                                reminderMinutes,
                                activeWeekdays,
                                steps.filter { it.title.isNotBlank() }
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (state.id == null) "Create Routine" else "Save Changes",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Weekday multi-select with Everyday/Weekdays/Weekends presets.
 * An empty selection means paused; the save button stays enabled and the
 * routine simply stops appearing and notifying until re-enabled.
 */
@Composable
private fun WeekdayPicker(
    selected: Set<DayOfWeek>,
    onChange: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WeekdayPresetChip(
                label = "Everyday",
                isSelected = selected == AllWeekdays,
                onClick = { onChange(AllWeekdays) },
                modifier = Modifier.weight(1f)
            )
            WeekdayPresetChip(
                label = "Weekdays",
                isSelected = selected == WeekdayPresetWeekdays,
                onClick = { onChange(WeekdayPresetWeekdays) },
                modifier = Modifier.weight(1f)
            )
            WeekdayPresetChip(
                label = "Weekends",
                isSelected = selected == WeekdayPresetWeekends,
                onClick = { onChange(WeekdayPresetWeekends) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = day in selected
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                        .clickable {
                            onChange(
                                if (isSelected) selected - day else selected + day
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.shortName().take(2),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
        if (selected.isEmpty()) {
            Text(
                text = "Paused — no days selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeekdayPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

private fun formatActiveWeekdays(days: Set<DayOfWeek>): String = when {
    days == AllWeekdays -> "Everyday"
    days == WeekdayPresetWeekdays -> "Weekdays"
    days == WeekdayPresetWeekends -> "Weekends"
    days.isEmpty() -> "Paused"
    else -> days.sortedBy { it.ordinal }.joinToString(", ") { it.shortName().take(2) }
}

/**
 * Template step editor: inline text rows with an add row, auto-focus on the new row,
 * Enter/Next appends the next row, and long-press drag reorders via the shared
 * [ReorderDragState] machinery. Flat design with reorder handles and clean containers.
 */
@Composable
private fun RoutineStepsEditor(
    steps: List<RoutineStepTemplate>,
    onStepsChange: (List<RoutineStepTemplate>) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val dragState = remember { ReorderDragState(scope) }
    val currentSteps by rememberUpdatedState(steps)
    val currentOnMove by rememberUpdatedState(onStepsChange)

    var previousSize by remember { mutableIntStateOf(steps.size) }
    var stepIdToFocus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(steps.size) {
        if (steps.size > previousSize) {
            stepIdToFocus = steps.lastOrNull()?.id
        }
        previousSize = steps.size
    }

    fun addStep() {
        onStepsChange(
            steps + RoutineStepTemplate(
                id = Uuid.random().toString(),
                title = "",
                sortOrder = steps.size
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            steps.forEach { step ->
                key(step.id) {
                    val focusRequester = remember { FocusRequester() }
                    val isDragging = dragState.draggingKey == step.id ||
                        dragState.previousKey == step.id

                    if (stepIdToFocus == step.id) {
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                            stepIdToFocus = null
                        }
                    }

                    val currentOnDragStart by rememberUpdatedState {
                        focusManager.clearFocus()
                        dragState.onDragStart(step.id)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    val currentOnDragEnd by rememberUpdatedState { dragState.onDragEnd() }
                    val currentOnMoveStep by rememberUpdatedState { dragAmountY: Float ->
                        dragState.onDrag(dragAmountY)
                        val latest = currentSteps
                        val key = dragState.draggingKey
                        if (key != null) {
                            val fromIndex = latest.indexOfFirst { it.id == key }
                            if (fromIndex >= 0) {
                                val targetIndex = findReorderTarget(
                                    count = latest.size,
                                    keyAt = { row -> latest[row].id },
                                    bounds = dragState.bounds,
                                    draggedKey = key,
                                    draggedDelta = dragState.draggingOffset,
                                    fromIndex = fromIndex
                                )
                                if (targetIndex != null && targetIndex != fromIndex) {
                                    currentOnMove(
                                        latest.toMutableList().apply {
                                            add(targetIndex, removeAt(fromIndex))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val detailStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .reorderableRowGraphics(step.id, dragState)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { currentOnDragStart() },
                                    onDragEnd = { currentOnDragEnd() },
                                    onDragCancel = { currentOnDragEnd() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentOnMoveStep(dragAmount.y)
                                    }
                                )
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                                BasicTextField(
                                    value = step.title,
                                    onValueChange = { value ->
                                        onStepsChange(
                                            steps.map { row ->
                                                if (row.id == step.id) row.copy(title = value) else row
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    textStyle = textStyle,
                                    singleLine = false,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { addStep() }),
                                    decorationBox = { innerTextField ->
                                        if (step.title.isEmpty()) {
                                            Text(
                                                "Step title",
                                                style = textStyle.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            onStepsChange(steps.filterNot { row -> row.id == step.id })
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove step",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            BasicTextField(
                                value = step.description,
                                onValueChange = { value ->
                                    onStepsChange(
                                        steps.map { row ->
                                            if (row.id == step.id) row.copy(description = value) else row
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 22.dp),
                                textStyle = detailStyle,
                                singleLine = false,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (step.description.isEmpty()) {
                                        Text(
                                            "Add details (optional)",
                                            style = detailStyle.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                },
                                visualTransformation = remember { MarkdownVisualTransformation() }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = { addStep() })
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Add step",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


