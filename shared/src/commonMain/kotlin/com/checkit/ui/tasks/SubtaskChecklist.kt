package com.checkit.ui.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.checkit.ui.tasks.views.ContentAlpha
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank

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
    val rowBounds = remember { mutableStateMapOf<Any, SubtaskRowBounds>() }
    val draggedIndex = remember { mutableIntStateOf(-1) }
    val draggedCenterY = remember { mutableFloatStateOf(0f) }

    val draggedKey = remember(draggedIndex.intValue, subtasks) {
        subtasks.getOrNull(draggedIndex.intValue)?.stableKey()
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            subtasks.forEachIndexed { index, subtask ->
                val rowKey = subtask.stableKey()
                val isDragging = draggedKey == rowKey
                key(rowKey) {
                    SubtaskRow(
                        subtask = subtask,
                        isDragging = isDragging,
                        onToggle = { onToggle(index) },
                        onNameChange = { onNameChange(index, it) },
                        onRemove = { onRemove(index) },
                        onMove = { dragAmountY ->
                            val currentDraggedIndex = subtasks.indexOfFirst { it.stableKey() == draggedKey }
                            if (currentDraggedIndex == -1) return@SubtaskRow
                            
                            draggedCenterY.floatValue += dragAmountY
                            
                            val targetIndex = subtasks.indices.firstOrNull { i ->
                                if (i == currentDraggedIndex) return@firstOrNull false
                                val key = subtasks[i].stableKey()
                                val bounds = rowBounds[key] ?: return@firstOrNull false
                                draggedCenterY.floatValue in bounds.top..bounds.bottom
                            } ?: return@SubtaskRow
                            
                            onMove(currentDraggedIndex, targetIndex)
                            draggedIndex.intValue = targetIndex
                        },
                        onDragStart = {
                            rowBounds[rowKey]?.let { bounds ->
                                draggedIndex.intValue = index
                                draggedCenterY.floatValue = bounds.center
                            }
                        },
                        onDragEnd = {
                            draggedIndex.intValue = -1
                            draggedCenterY.floatValue = 0f
                        },
                        modifier = Modifier
                            .animateSubtaskPlacement(rowKey, isDragging) { baseTop, height ->
                                rowBounds[rowKey] = SubtaskRowBounds(
                                    top = baseTop,
                                    bottom = baseTop + height
                                )
                            }
                            .graphicsLayer {
                                val center = rowBounds[rowKey]?.center ?: 0f
                                translationY = if (isDragging) draggedCenterY.floatValue - center else 0f
                                scaleX = if (isDragging) 1.02f else 1f
                                scaleY = if (isDragging) 1.02f else 1f
                                shadowElevation = if (isDragging) 8f else 0f
                                shape = RoundedCornerShape(12.dp)
                            },
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
    onMove: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val rowAlpha = if (subtask.isCompleted) ContentAlpha else 1f
    
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnMove by rememberUpdatedState(onMove)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .graphicsLayer { alpha = rowAlpha },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                style = textStyle
            )
        } else {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = subtask.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    textStyle = textStyle,
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { }),
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
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ContentAlpha),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onRemove() }
                )
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = "Reorder subtask",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ContentAlpha),
                    modifier = Modifier
                        .size(20.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { currentOnDragStart() },
                                onDragEnd = { currentOnDragEnd() },
                                onDragCancel = { currentOnDragEnd() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentOnMove(dragAmount.y)
                                }
                            )
                        }
                )
            }
        }
    }
}

private data class SubtaskRowBounds(
    val top: Float,
    val bottom: Float
) {
    val center: Float get() = (top + bottom) / 2f
}

private fun SubTaskEditorState.stableKey(): Any =
    id ?: editorKey

private fun Modifier.animateSubtaskPlacement(
    key: Any,
    isDragging: Boolean,
    onPositioned: (Float, Int) -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val offsetY = remember(key) { Animatable(0f) }
    var previousTop by remember(key) { mutableStateOf<Float?>(null) }

    onGloballyPositioned { coordinates ->
        val nextTop = coordinates.positionInParent().y
        
        // Report the base position (excluding current animation offset) to SubtaskChecklist
        // This ensures reordering logic uses the stable layout positions.
        onPositioned(nextTop - offsetY.value, coordinates.size.height)
        
        if (isDragging) {
            previousTop = nextTop
            scope.launch { offsetY.snapTo(0f) }
            return@onGloballyPositioned
        }

        val lastTop = previousTop
        if (lastTop != null && lastTop != nextTop) {
            scope.launch {
                offsetY.snapTo(lastTop - nextTop)
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
        previousTop = nextTop
    }.offset { IntOffset(x = 0, y = offsetY.value.roundToInt()) }
}
