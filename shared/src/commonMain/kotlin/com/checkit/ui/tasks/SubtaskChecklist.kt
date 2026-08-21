package com.checkit.ui.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.checkit.domain.SubTaskItem
import com.checkit.ui.tasks.views.ContentAlpha
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
internal fun SubtaskBriefList(subtasks: List<SubTaskItem>) {
    val activeSubtasks = subtasks.filter { !it.isCompleted }
    if (activeSubtasks.isEmpty()) return

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
    val scope = rememberCoroutineScope()
    val dragState = remember { SubtaskDragState(scope) }
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

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
        )
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            val fromIndex = latest.indexOfFirst { it.stableKey() == dragState.draggingKey }
                            val targetIndex = findSubtaskReorderTarget(
                                draggedKey = dragState.draggingKey,
                                visualMiddleY = dragState.visualMiddleY(),
                                items = latest.map { entry ->
                                    entry.stableKey() to dragState.bounds[entry.stableKey()]
                                }
                            )
                            if (fromIndex >= 0 && targetIndex != null && targetIndex != fromIndex) {
                                currentOnMove(fromIndex, targetIndex)
                            }
                        },
                        onDragStart = {
                            dragState.onDragStart(rowKey)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = dragState::onDragEnd,
                        modifier = Modifier.subtaskReorderGraphics(rowKey, dragState),
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
                style = textStyle,
                maxLines = 3
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

private class SubtaskDragState(
    private val scope: CoroutineScope
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    var previousKey by mutableStateOf<Any?>(null)
        private set

    val previousOffset = Animatable(0f)
    val bounds = mutableStateMapOf<Any, SubtaskRowBounds>()

    private var dragDelta by mutableFloatStateOf(0f)
    private var initialTop by mutableFloatStateOf(0f)

    val draggingOffset: Float
        get() {
            val top = bounds[draggingKey]?.top ?: return dragDelta
            return initialTop + dragDelta - top
        }

    fun visualMiddleY(): Float {
        val row = bounds[draggingKey] ?: return 0f
        return row.top + draggingOffset + row.height / 2f
    }

    fun onDragStart(key: Any) {
        val top = bounds[key]?.top ?: return
        draggingKey = key
        previousKey = null
        dragDelta = 0f
        initialTop = top
        scope.launch { previousOffset.snapTo(0f) }
    }

    fun onDrag(delta: Float) {
        if (draggingKey == null) return
        dragDelta += delta
    }

    fun onDragEnd() {
        val key = draggingKey
        if (key != null) {
            previousKey = key
            val startOffset = draggingOffset
            scope.launch {
                previousOffset.snapTo(startOffset)
                previousOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = 1f
                    )
                )
                if (previousKey == key) {
                    previousKey = null
                }
            }
        }
        draggingKey = null
        dragDelta = 0f
    }
}

internal data class SubtaskRowBounds(
    val top: Float,
    val heightPx: Int
) {
    val bottom: Float get() = top + heightPx
    val height: Float get() = heightPx.toFloat()
}

internal fun findSubtaskReorderTarget(
    draggedKey: Any?,
    visualMiddleY: Float,
    items: List<Pair<Any, SubtaskRowBounds?>>
): Int? {
    if (draggedKey == null) return null
    return items.indices.firstOrNull { index ->
        val (key, bounds) = items[index]
        bounds != null && key != draggedKey && visualMiddleY in bounds.top..bounds.bottom
    }
}

private fun SubTaskEditorState.stableKey(): Any =
    id ?: editorKey

private fun Modifier.subtaskReorderGraphics(
    key: Any,
    dragState: SubtaskDragState
): Modifier = composed {
    val isDragging = dragState.draggingKey == key
    val isSettling = dragState.previousKey == key
    val scope = rememberCoroutineScope()
    val placementOffset = remember(key) { Animatable(0f) }
    var previousTop by remember(key) { mutableStateOf<Float?>(null) }
    val lift by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "subtask-drag-lift"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isDragging || isSettling) 12f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "subtask-drag-elevation"
    )

    onGloballyPositioned { coordinates ->
        val layoutTop = coordinates.positionInParent().y
        dragState.bounds[key] = SubtaskRowBounds(
            top = layoutTop,
            heightPx = coordinates.size.height
        )

        if (isDragging || isSettling) {
            previousTop = layoutTop
            scope.launch { placementOffset.snapTo(0f) }
            return@onGloballyPositioned
        }

        val lastTop = previousTop
        if (lastTop != null && abs(lastTop - layoutTop) > 0.5f) {
            scope.launch {
                placementOffset.snapTo(lastTop - layoutTop)
                placementOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
        previousTop = layoutTop
    }
        .zIndex(if (isDragging || isSettling) 1f else 0f)
        .graphicsLayer {
            translationY = when {
                isDragging -> dragState.draggingOffset
                isSettling -> dragState.previousOffset.value
                else -> placementOffset.value
            }
            scaleX = lift
            scaleY = lift
            shadowElevation = elevation
            shape = RoundedCornerShape(12.dp)
            clip = false
        }
}
