package com.checkit.ui.myday

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.checkit.domain.Routine
import com.checkit.domain.RoutineStepTemplate
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
import kotlin.uuid.Uuid

private data class RoutineEditorState(
    val id: String?,
    val title: String,
    val reminderMinutes: Int?,
    val steps: List<RoutineStepTemplate>
)

@Composable
internal fun RoutineTab(
    routines: List<Routine>,
    checks: Map<String, Set<String>>,
    onToggleStep: (String, String) -> Unit,
    onSaveRoutine: (String?, String, Int?, List<RoutineStepTemplate>) -> Unit,
    onDeleteRoutine: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editor by remember { mutableStateOf<RoutineEditorState?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's routines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = {
                        editor = RoutineEditorState(id = null, title = "", reminderMinutes = null, steps = emptyList())
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(text = "New routine")
                }
            }
        }
        if (routines.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Text(
                        text = "No routines yet. Create one to start a daily checklist that resets tomorrow.",
                        modifier = Modifier.padding(22.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(routines, key = { it.id }) { routine ->
            RoutineCard(
                routine = routine,
                checkedStepIds = checks[routine.id].orEmpty(),
                onToggleStep = { stepId -> onToggleStep(routine.id, stepId) },
                onEdit = {
                    editor = RoutineEditorState(
                        id = routine.id,
                        title = routine.title,
                        reminderMinutes = routine.reminderMinutes,
                        steps = routine.steps
                    )
                }
            )
        }
    }

    editor?.let { state ->
        RoutineEditorSheet(
            state = state,
            onDismiss = { editor = null },
            onSave = { id, title, reminderMinutes, steps ->
                onSaveRoutine(id, title, reminderMinutes, steps)
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
    onToggleStep: (String) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validStepIds = remember(routine.steps) { routine.steps.map { it.id }.toSet() }
    val percent = remember(routine.steps, checkedStepIds) {
        routinePercent(routine.steps.size, checkedStepIds, validStepIds)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = routine.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Edit routine",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                routine.reminderMinutes?.let { minutes ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = minutes.toClockLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (routine.steps.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            routine.steps.forEach { step ->
                val checked = step.id in checkedStepIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleStep(step.id) },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                onToggleStep(step.id)
                            }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (checked) TextDecoration.LineThrough else null
                            ),
                            color = if (checked) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (step.description.isNotBlank()) {
                            Text(
                                text = step.description.asMarkdownAnnotatedString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
    onSave: (String?, String, Int?, List<RoutineStepTemplate>) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember(state) { mutableStateOf(state.title) }
    var reminderMinutes by remember(state) { mutableStateOf(state.reminderMinutes) }
    var steps by remember(state) { mutableStateOf(state.steps) }

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .padding(bottom = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    placeholder = "Routine name"
                )
            }
            item {
                TimePicker(
                    label = "reminder",
                    timeMinutes = reminderMinutes,
                    initialTimeMinutes = 8 * 60,
                    onTimeChange = { reminderMinutes = it }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Steps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    RoutineStepsEditor(
                        steps = steps,
                        onStepsChange = { steps = it }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(state.id, title.trim(), reminderMinutes, steps.filter { it.title.isNotBlank() })
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                ) {
                    Text("Save")
                }
            }

            if (state.id != null) {
                DeleteOverflowMenu(onDelete = { onDelete(state.id) })
            }
        }

    }
}

/**
 * Template step editor modeled on [com.checkit.ui.tasks.SubtaskChecklist]:
 * inline text rows with an add row, auto-focus on the new row, Enter/Next
 * appends the next row, and long-press drag reorders via the shared
 * [ReorderDragState] machinery. No checkbox: templates carry no done state.
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
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
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
                    Column(
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
                            .background(
                                color = if (isDragging) {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .then(
                                if (isDragging) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else Modifier
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                            "Step",
                                            style = textStyle.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable {
                                        onStepsChange(steps.filterNot { row -> row.id == step.id })
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove step",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
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
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = detailStyle,
                            singleLine = false,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (step.description.isEmpty()) {
                                    Text(
                                        "Add details",
                                        style = detailStyle.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = { addStep() })
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Add step",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
