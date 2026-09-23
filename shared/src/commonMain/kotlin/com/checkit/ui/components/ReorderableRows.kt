package com.checkit.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Shared long-press drag-reorder machinery (extracted from the subtask
 * checklist). Rows are identified by stable keys; callers map their own
 * items to keys and apply the returned target index to their list.
 *
 * Performance notes (inherited from the original):
 * - [bounds] is a plain (non-snapshot) map. Row positions are only consumed
 *   inside pointer-input callbacks and [onGloballyPositioned], never during
 *   composition, so snapshot observability would only add overhead and extra
 *   recompositions on every layout pass.
 * - Only [draggingKey]/[previousKey] (rarely changing) plus the two float
 *   states below are observable. [dragDelta] and [draggedLayoutTop] are read
 *   inside `graphicsLayer` blocks, which invalidates just the layer instead of
 *   triggering recomposition at 60-120 Hz during a drag.
 */
internal class ReorderDragState(
    private val scope: CoroutineScope
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    var previousKey by mutableStateOf<Any?>(null)
        private set

    val previousOffset = Animatable(0f)

    /** Layout positions keyed by row stable key. Plain map: see class docs. */
    val bounds = mutableMapOf<Any, RowBounds>()

    private var dragDelta by mutableFloatStateOf(0f)
    private var draggedLayoutTop by mutableFloatStateOf(0f)
    private var initialTop = 0f
    private var settleJob: Job? = null

    val draggingOffset: Float
        get() = initialTop + dragDelta - draggedLayoutTop

    /** Records a row layout. Called from `onGloballyPositioned`, never composes. */
    fun onPositioned(key: Any, top: Float, heightPx: Int) {
        val next = RowBounds(top = top, heightPx = heightPx)
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

internal data class RowBounds(
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
internal fun findReorderTarget(
    draggedKey: Any?,
    draggedDelta: Float,
    items: List<Pair<Any, RowBounds?>>
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
 * Allocation-free variant of [findReorderTarget] for the drag hot path
 * (pointer-input callbacks fire at 60-120 Hz). Reads row bounds in place via
 * [keyAt] instead of building a `List<Pair<...>>` per event.
 */
internal inline fun findReorderTarget(
    count: Int,
    crossinline keyAt: (Int) -> Any,
    bounds: Map<Any, RowBounds>,
    draggedKey: Any,
    draggedDelta: Float,
    fromIndex: Int
): Int? {
    if (count <= 0 || fromIndex !in 0 until count) return null
    val currentBounds = bounds[keyAt(fromIndex)] ?: return null
    val visualCenter = currentBounds.center + draggedDelta

    return walkReorderTarget(
        fromIndex = fromIndex,
        lastIndex = count - 1,
        visualCenter = visualCenter,
        centerAt = { index -> bounds[keyAt(index)]?.center }
    )
}

/**
 * Shared center-crossing walk. Moves outward from [fromIndex] while the dragged
 * visual center has strictly crossed each neighbor's center. A missing center
 * (unmeasured row) stops the walk so the item never jumps over unknown layout.
 */
internal inline fun walkReorderTarget(
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

/** Plain (non-state) holder so layout tracking never recomposes rows. */
private class PlacementAnimationState {
    var previousTop: Float? = null
    var job: Job? = null
}

internal fun Modifier.reorderableRowGraphics(
    key: Any,
    dragState: ReorderDragState
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
