package com.checkit.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.SubTaskItem
import com.checkit.ui.components.ReorderDragState
import com.checkit.ui.components.findReorderTarget
import com.checkit.ui.components.reorderableRowGraphics
import com.checkit.ui.tasks.views.ContentAlpha

@Composable
internal fun SubtaskBriefList(subtasks: List<SubTaskItem>) {
    val activeSubtasks = subtasks.filter { !it.isCompleted }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        if (activeSubtasks.isEmpty()) {
            Text(
                text = "${subtasks.count { it.isCompleted }}/${subtasks.size} subtasks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                activeSubtasks.forEach { subtask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ContentAlpha)
                        )
                        Text(
                            text = subtask.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubtaskChecklist(
    subtasks: List<SubTaskEditorState>,
    onToggle: (Int) -> Unit,
    onAdd: () -> Unit,
    onNameChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (subtasks.isEmpty() && !enabled) return
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val dragState = remember { ReorderDragState(scope) }
    val currentSubtasks by rememberUpdatedState(subtasks)
    val currentOnMove by rememberUpdatedState(onMove)

    var previousSize by remember { mutableIntStateOf(subtasks.size) }
    var subtaskIdToFocus by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(subtasks.size) {
        if (subtasks.size > previousSize) {
            subtaskIdToFocus = subtasks.lastOrNull()?.stableKey()
        }
        previousSize = subtasks.size
    }

    Box(modifier = modifier
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
            subtasks.forEachIndexed { index, subtask ->
                val rowKey = subtask.stableKey()
                val isDragging = dragState.draggingKey == rowKey
                val isSettling = dragState.previousKey == rowKey
                key(rowKey) {
                    val focusRequester = remember { FocusRequester() }

                    if (subtaskIdToFocus == rowKey) {
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                            subtaskIdToFocus = null
                        }
                    }

                    SubtaskRow(
                        subtask = subtask,
                        isDragging = isDragging || isSettling,
                        onToggle = { onToggle(index) },
                        onNameChange = { onNameChange(index, it) },
                        onRemove = { onRemove(index) },
                        onAdd = onAdd,
                        focusRequester = focusRequester,
                        onMove = { dragAmountY ->
                            dragState.onDrag(dragAmountY)
                            val latest = currentSubtasks
                            val key = dragState.draggingKey
                            if (key != null) {
                                val fromIndex = latest.indexOfFirst { it.stableKey() == key }
                                if (fromIndex >= 0) {
                                    // Fast path: no Pair-list allocation per drag event,
                                    // the center walk reads bounds in place.
                                    val targetIndex = findReorderTarget(
                                        count = latest.size,
                                        keyAt = { index -> latest[index].stableKey() },
                                        bounds = dragState.bounds,
                                        draggedKey = key,
                                        draggedDelta = dragState.draggingOffset,
                                        fromIndex = fromIndex
                                    )
                                    if (targetIndex != null && targetIndex != fromIndex) {
                                        currentOnMove(fromIndex, targetIndex)
                                    }
                                }
                            }
                        },
                        onDragStart = {
                            focusManager.clearFocus()
                            dragState.onDragStart(rowKey)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = dragState::onDragEnd,
                        modifier = Modifier.reorderableRowGraphics(rowKey, dragState),
                        enabled = enabled
                    )
                }
            }
            if (enabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAdd)
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
                        "Add Subtask",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtaskRow(
    subtask: SubTaskEditorState,
    isDragging: Boolean,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit,
    onAdd: () -> Unit,
    onMove: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    enabled: Boolean = true
) {
    val rowAlpha = if (subtask.isCompleted) ContentAlpha else 1f

    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnMove by rememberUpdatedState(onMove)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { currentOnDragStart() },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnMove(dragAmount.y)
                            }
                        )
                    }
                } else Modifier
            )
            .background(
                color = when {
                    isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> Color.Transparent
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
            .graphicsLayer { alpha = rowAlpha },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (subtask.isCompleted) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
            contentDescription = if (subtask.isCompleted) "Mark incomplete" else "Mark complete",
            tint = if (subtask.isCompleted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            modifier = Modifier
                .size(20.dp)
                .then(if (enabled) Modifier.clickable { onToggle() } else Modifier)
        )

        val textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )

        if (!enabled) {
            Text(
                text = subtask.name,
                modifier = Modifier.weight(1f),
                style = textStyle,
                maxLines = 3
            )
        } else {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicTextField(
                    value = subtask.name,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = textStyle,
                    singleLine = false,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { onAdd() }),
                    decorationBox = { innerTextField ->
                        if (subtask.name.isEmpty()) {
                            Text(
                                "Subtask",
                                style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ContentAlpha))
                            )
                        }
                        innerTextField()
                    }
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ContentAlpha),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun SubTaskEditorState.stableKey(): Any =
    id ?: editorKey

