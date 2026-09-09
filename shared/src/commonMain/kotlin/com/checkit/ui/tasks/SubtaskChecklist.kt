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
import androidx.compose.ui.platform.LocalFocusManager
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
import kotlinx.coroutines.Job
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
    val focusManager = LocalFocusManager.current
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
                                    val targetIndex = findSubtaskReorderTargetFast(
                                        subtasks = latest,
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
                Box(
                    modifier = Modifier
                        .size(32.dp)
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
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DragIndicator,
                        contentDescription = "Reorder subtask",
                        tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ContentAlpha),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Holds the transient drag-reorder state for the subtask checklist.
 *
 * Performance notes:
 * - [bounds] is a plain (non-snapshot) map. Row positions are only consumed
 *   inside pointer-input callbacks and [onGloballyPositioned], never during
 *   composition, so snapshot observability would only add overhead and extra
 *   recompositions on every layout pass.
 * - Only [draggingKey]/[previousKey] (rarely changing) plus the two float
 *   states below are observable. [dragDelta] and [draggedLayoutTop] are read
 *   inside `graphicsLayer` blocks, which invalidates just the layer instead of
 *   triggering recomposition at 60-120 Hz during a drag.
 */
private class SubtaskDragState(
    private val scope: CoroutineScope
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    var previousKey by mutableStateOf<Any?>(null)
        private set

    val previousOffset = Animatable(0f)

    /** Layout positions keyed by row stable key. Plain map: see class docs. */
    val bounds = mutableMapOf<Any, SubtaskRowBounds>()

    private var dragDelta by mutableFloatStateOf(0f)
    private var draggedLayoutTop by mutableFloatStateOf(0f)
    private var initialTop = 0f
    private var settleJob: Job? = null

    val draggingOffset: Float
        get() = initialTop + dragDelta - draggedLayoutTop

    /** Records a row layout. Called from `onGloballyPositioned`, never composes. */
    fun onPositioned(key: Any, top: Float, heightPx: Int) {
        val next = SubtaskRowBounds(top = top, heightPx = heightPx)
        if (bounds[key] != next) {
            bounds[key] = next
        }
        if (key == draggingKey && draggedLayoutTop != top) {
            // The dragged row re-laid-out (e.g. after a reorder swap):
            // keep the visual position stable by re-basing the offset.
            draggedLayoutTop = top
        }
    }

    fun onDragStart(key: Any) {
        settleJob?.cancel()
        settleJob = null
        val top = bounds[key]?.top ?: draggedLayoutTop
        draggingKey = key
        previousKey = null
        dragDelta = 0f
        initialTop = top
        draggedLayoutTop = top
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
            settleJob?.cancel()
            settleJob = scope.launch {
                previousOffset.snapTo(startOffset)
                previousOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = 0.5f
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
    val center: Float get() = top + heightPx / 2f
}

/**
 * Determines the target index for the dragged item by comparing its visual center
 * against the centers of adjacent items, preventing rapid oscillation and handling
 * variable-height rows smoothly.
 *
 * Verified behavior (see `SubtaskReorderTest`):
 * - no movement -> null (no-op, avoids useless list copies in the ViewModel)
 * - the dragged center must strictly cross a neighbor's center before swapping,
 *   which gives hysteresis after a swap and stops thrashing on variable heights
 * - a fast fling across several rows resolves to the furthest crossed index in a
 *   single move instead of one swap per frame
 * - rows with unknown bounds stop the walk instead of jumping
 */
internal fun findSubtaskReorderTarget(
    draggedKey: Any?,
    draggedDelta: Float,
    items: List<Pair<Any, SubtaskRowBounds?>>
): Int? {
    if (draggedKey == null || items.isEmpty()) return null
    val currentIndex = items.indexOfFirst { it.first == draggedKey }
    if (currentIndex < 0) return null

    val currentBounds = items[currentIndex].second ?: return null
    val visualCenter = currentBounds.top + currentBounds.height / 2f + draggedDelta

    return walkReorderTarget(
        fromIndex = currentIndex,
        lastIndex = items.lastIndex,
        visualCenter = visualCenter,
        centerAt = { index -> items[index].second?.center }
    )
}

/**
 * Allocation-free variant of [findSubtaskReorderTarget] for the drag hot path
 * (pointer-input callbacks fire at 60-120 Hz). Reads row bounds in place instead
 * of building a `List<Pair<...>>` per event.
 */
private fun findSubtaskReorderTargetFast(
    subtasks: List<SubTaskEditorState>,
    bounds: Map<Any, SubtaskRowBounds>,
    draggedKey: Any,
    draggedDelta: Float,
    fromIndex: Int
): Int? {
    if (subtasks.isEmpty() || fromIndex !in subtasks.indices) return null
    val currentBounds = bounds[subtasks[fromIndex].stableKey()] ?: return null
    val visualCenter = currentBounds.center + draggedDelta

    return walkReorderTarget(
        fromIndex = fromIndex,
        lastIndex = subtasks.lastIndex,
        visualCenter = visualCenter,
        centerAt = { index -> bounds[subtasks[index].stableKey()]?.center }
    )
}

/**
 * Shared center-crossing walk. Moves outward from [fromIndex] while the dragged
 * visual center has strictly crossed each neighbor's center. A missing center
 * (unmeasured row) stops the walk so the item never jumps over unknown layout.
 */
private inline fun walkReorderTarget(
    fromIndex: Int,
    lastIndex: Int,
    visualCenter: Float,
    centerAt: (Int) -> Float?
): Int? {
    var targetIndex = fromIndex

    // Upward movement (crossing centers of items above)
    while (targetIndex > 0) {
        val prevCenter = centerAt(targetIndex - 1) ?: break
        if (visualCenter < prevCenter) {
            targetIndex--
        } else {
            break
        }
    }

    // Downward movement (crossing centers of items below)
    while (targetIndex < lastIndex) {
        val nextCenter = centerAt(targetIndex + 1) ?: break
        if (visualCenter > nextCenter) {
            targetIndex++
        } else {
            break
        }
    }

    return if (targetIndex != fromIndex) targetIndex else null
}

private fun SubTaskEditorState.stableKey(): Any =
    id ?: editorKey

/** Plain (non-state) holder so layout tracking never recomposes rows. */
private class PlacementAnimationState {
    var previousTop: Float? = null
    var job: Job? = null
}

private fun Modifier.subtaskReorderGraphics(
    key: Any,
    dragState: SubtaskDragState
): Modifier = composed {
    val isDragging = dragState.draggingKey == key
    val isSettling = dragState.previousKey == key
    val scope = rememberCoroutineScope()
    val placementOffset = remember(key) { Animatable(0f) }
    // Plain holder, not state: position bookkeeping must not recompose rows on
    // every layout pass (e.g. parent scroll). Only isDragging/isSettling flips
    // recompose; drag frames invalidate just the graphics layer.
    val placementState = remember(key) { PlacementAnimationState() }

    onGloballyPositioned { coordinates ->
        val layoutTop = coordinates.positionInParent().y
        dragState.onPositioned(key, layoutTop, coordinates.size.height)

        if (isDragging || isSettling) {
            placementState.previousTop = layoutTop
            placementState.job?.cancel()
            placementState.job = null
            if (placementOffset.value != 0f) {
                scope.launch { placementOffset.snapTo(0f) }
            }
            return@onGloballyPositioned
        }

        val lastTop = placementState.previousTop
        placementState.previousTop = layoutTop
        if (lastTop == null || abs(lastTop - layoutTop) <= 1f) {
            return@onGloballyPositioned
        }
        // A sibling moved: glide from the old position instead of jumping.
        // The previous glide is cancelled so fast successive swaps don't pile
        // up coroutines and springs fighting over the same Animatable.
        placementState.job?.cancel()
        placementState.job = scope.launch {
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
        .zIndex(if (isDragging || isSettling) 2f else 0f)
        .graphicsLayer {
            translationY = when {
                isDragging -> dragState.draggingOffset
                isSettling -> dragState.previousOffset.value
                else -> placementOffset.value
            }
            // Static lift/shadow on purpose: the previous per-row
            // animateFloatAsState pair (scale + elevation on every row) kept N
            // animation clocks alive and recomposed each row every frame, and
            // animated shadowElevation forces costly offscreen re-renders.
            // The background + elevation switch already signals the drag.
            scaleX = if (isDragging) 1.02f else 1f
            scaleY = if (isDragging) 1.02f else 1f
            shadowElevation = if (isDragging || isSettling) 8f else 0f
            shape = RoundedCornerShape(12.dp)
            clip = false
        }
}
