package com.checkit.ui.tasks.views

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.checkit.ui.components.HoursPerDay
import com.checkit.ui.components.MinutesPerDay
import com.checkit.ui.tasks.TimelineItem
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock

@Composable
internal fun TimelineView(
    items: List<TimelineItem>,
    onItemClick: (TimelineItem) -> Unit,
    onCreateRequest: (startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    onTimeChange: (TimelineItem, startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    modifier: Modifier = Modifier,
    allDayItemContent: @Composable (TimelineItem) -> Unit,
    timedItemContent: @Composable BoxScope.(TimelineItem, isSelected: Boolean, displayMode: TimelineItemDisplayMode) -> Unit
) {
    val allDayItems = remember(items) { items.filter { it.startTimeMinutes == null } }
    val timedItems = remember(items) { items.filter { it.startTimeMinutes != null } }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AllDaySection(
            items = allDayItems,
            onItemClick = onItemClick,
            itemContent = allDayItemContent
        )
        TimelineGrid(
            items = timedItems,
            onItemClick = onItemClick,
            onCreateRequest = onCreateRequest,
            onTimeChange = onTimeChange,
            itemContent = timedItemContent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AllDaySection(
    items: List<TimelineItem>,
    onItemClick: (TimelineItem) -> Unit,
    itemContent: @Composable (TimelineItem) -> Unit
) {
    if (items.isEmpty()) return

    val totalItemCount = items.size
    var expanded by remember(totalItemCount) { mutableStateOf(totalItemCount <= CollapsedAllDayItemCount) }
    val visibleItems = if (expanded) items else items.take(CollapsedAllDayItemCount)
    val hiddenItemCount = totalItemCount - visibleItems.size

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All-day",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (totalItemCount > CollapsedAllDayItemCount) {
                Text(
                    text = if (expanded) "Show less" else "+$hiddenItemCount more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            visibleItems.forEach { item ->
                Box(modifier = Modifier.clickable { onItemClick(item) }) {
                    itemContent(item)
                }
            }
        }
    }
}

@Composable
private fun TimelineGrid(
    items: List<TimelineItem>,
    onItemClick: (TimelineItem) -> Unit,
    onCreateRequest: (startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    onTimeChange: (TimelineItem, startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    itemContent: @Composable BoxScope.(TimelineItem, isSelected: Boolean, displayMode: TimelineItemDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var hourHeight by remember { mutableStateOf(DefaultTimelineHourHeight) }
    val axisWidth = 56.dp
    val axisWidthPx = with(density) { axisWidth.toPx() }
    val layouts = remember(items) { buildTimelineLayouts(items) }
    val currentTimeMinutes = remember { currentTimeMinutes() }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var hasScrolledToCurrentTime by remember { mutableStateOf(false) }
    var isWorkdayZoomed by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val minHourHeight = if (maxHeight.value.isFinite() && maxHeight > 0.dp) {
            (maxHeight / HoursPerDay).coerceAtLeast(MinTimelineHourHeight)
        } else {
            MinTimelineHourHeight
        }
        val maxHourHeight = MaxTimelineHourHeight.coerceAtLeast(minHourHeight)
        val timelineHourHeight = hourHeight.coerceIn(minHourHeight, maxHourHeight)

        LaunchedEffect(minHourHeight, maxHourHeight) {
            hourHeight = hourHeight.coerceIn(minHourHeight, maxHourHeight)
        }

        val totalHeight = timelineHourHeight * HoursPerDay
        val hourHeightPx = with(density) { timelineHourHeight.toPx() }
        val timelineViewportHeight = maxHeight
        val taskAreaWidth = (maxWidth - axisWidth - 14.dp).coerceAtLeast(1.dp)
        LaunchedEffect(currentTimeMinutes, hasScrolledToCurrentTime) {
            if (hasScrolledToCurrentTime) return@LaunchedEffect
            scrollState.scrollToCurrentTime(currentTimeMinutes, hourHeightPx)
            hasScrolledToCurrentTime = true
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
                    .pointerInput(hourHeightPx, minHourHeight, maxHourHeight) {
                        detectTimelineZoomGestures { centroid, pan, zoom ->
                            if (abs(zoom - 1f) < TimelineZoomEpsilon) return@detectTimelineZoomGestures

                            val previousHourHeight = timelineHourHeight
                            val nextHourHeight = (previousHourHeight * zoom)
                                .coerceIn(minHourHeight, maxHourHeight)
                            if (nextHourHeight == previousHourHeight) return@detectTimelineZoomGestures

                            val previousHourHeightPx = with(density) { previousHourHeight.toPx() }
                            val nextHourHeightPx = with(density) { nextHourHeight.toPx() }
                            val focalMinutes = centroid.y.toMinutes(previousHourHeightPx)
                                .coerceIn(0, MinutesPerDay)
                            val previousFocalY = minutesToY(focalMinutes, previousHourHeightPx)
                            val nextFocalY = minutesToY(focalMinutes, nextHourHeightPx)
                            val scrollDelta = nextFocalY - previousFocalY - pan.y

                            isWorkdayZoomed = false
                            hourHeight = nextHourHeight
                            coroutineScope.launch {
                                scrollState.scrollTo(
                                    (scrollState.value + scrollDelta)
                                        .roundToInt()
                                        .coerceAtLeast(0)
                                )
                            }
                        }
                    }
                    .pointerInput(hourHeightPx, axisWidthPx) {
                        detectTapGestures(
                            onTap = { selectedItemId = null },
                            onDoubleTap = {
                                selectedItemId = null
                                if (isWorkdayZoomed) {
                                    val nextHourHeight = DefaultTimelineHourHeight
                                        .coerceIn(minHourHeight, maxHourHeight)
                                    hourHeight = nextHourHeight
                                    isWorkdayZoomed = false
                                    coroutineScope.launch {
                                        scrollState.scrollToCurrentTime(
                                            currentTimeMinutes,
                                            with(density) { nextHourHeight.toPx() }
                                        )
                                    }
                                } else {
                                    val nextHourHeight = (timelineViewportHeight / WorkdayVisibleHours)
                                        .coerceIn(minHourHeight, maxHourHeight)
                                    hourHeight = nextHourHeight
                                    isWorkdayZoomed = true
                                    coroutineScope.launch {
                                        scrollState.scrollToStartMinute(
                                            WorkdayStartMinutes,
                                            with(density) { nextHourHeight.toPx() }
                                        )
                                    }
                                }
                            },
                            onLongPress = { offset ->
                                if (offset.x >= axisWidthPx) {
                                    val start = offset.y.toMinutes(hourHeightPx)
                                        .snapToQuarterHour()
                                        .coerceIn(0, LastStartMinute)
                                    onCreateRequest(start, start + DefaultDurationMinutes)
                                }
                            }
                        )
                    }
            ) {
                HourRows(
                    hourHeight = timelineHourHeight,
                    axisWidth = axisWidth
                )
                CurrentTimeLine(
                    currentTimeMinutes = currentTimeMinutes,
                    hourHeight = timelineHourHeight,
                    axisWidth = axisWidth
                )
                layouts.forEach { layout ->
                    TimelineItemCard(
                        layout = layout,
                        item = layout.item,
                        axisWidth = axisWidth,
                        taskAreaWidth = taskAreaWidth,
                        hourHeight = timelineHourHeight,
                        hourHeightPx = hourHeightPx,
                        isSelected = selectedItemId == layout.item.id,
                        onClick = { onItemClick(layout.item) },
                        onSelect = { selectedItemId = layout.item.id },
                        onTimeChange = onTimeChange,
                        content = itemContent
                    )
                }
            }
        }
    }
}

@Composable
private fun HourRows(
    hourHeight: Dp,
    axisWidth: Dp
) {
    Column {
        repeat(HoursPerDay) { hour ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight)
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = (-12).dp)
                        .width(axisWidth)
                        .height(24.dp)
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = hour.hourLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(x = axisWidth)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
                )
                repeat(3) { quarter ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = axisWidth,
                                y = hourHeight * ((quarter + 1) / 4f)
                            )
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentTimeLine(
    currentTimeMinutes: Int,
    hourHeight: Dp,
    axisWidth: Dp
) {
    val color = MaterialTheme.colorScheme.error
    val y = hourHeight * (currentTimeMinutes / 60f)
    Box(
        modifier = Modifier
            .offset(y = y - 4.dp)
            .fillMaxWidth()
            .height(8.dp)
            .zIndex(2f)
    ) {
        Box(
            modifier = Modifier
                .offset(x = axisWidth - 5.dp, y = 1.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .offset(x = axisWidth, y = 4.dp)
                .fillMaxWidth()
                .height(1.5.dp)
                .background(color)
        )
    }
}

@Composable
private fun TimelineItemCard(
    layout: TimelineItemLayout,
    item: TimelineItem,
    axisWidth: Dp,
    taskAreaWidth: Dp,
    hourHeight: Dp,
    hourHeightPx: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit,
    onTimeChange: (TimelineItem, startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    content: @Composable BoxScope.(TimelineItem, Boolean, TimelineItemDisplayMode) -> Unit
) {
    val start = item.startTimeMinutes ?: return
    val end = item.endTimeMinutes ?: (start + DefaultDurationMinutes)
    val duration = (end - start).coerceAtLeast(MinimumDurationMinutes)
    val y = hourHeight * (start / 60f)
    val minimumHeight = hourHeight / 4f
    val height = (hourHeight * (duration / 60f)).coerceAtLeast(minimumHeight)
    val density = LocalDensity.current
    val yPx = with(density) { y.roundToPx() }
    val laneWidth = taskAreaWidth / layout.laneCount
    val cardWidth = (laneWidth - 4.dp).coerceAtLeast(44.dp)
    val x = axisWidth + 6.dp + laneWidth * layout.lane
    val xPx = with(density) { x.roundToPx() }

    var dragOffsetY by remember(item.id, start, end) { mutableFloatStateOf(0f) }
    var topResizeOffsetY by remember(item.id, start, end) { mutableFloatStateOf(0f) }
    var bottomResizeOffsetY by remember(item.id, start, end) { mutableFloatStateOf(0f) }
    val resizeHeightDelta = with(density) { (bottomResizeOffsetY - topResizeOffsetY).toDp() }
    val visualHeight = (height + resizeHeightDelta).coerceAtLeast(minimumHeight)
    val displayMode = when {
        visualHeight < UltraCompactTimelineItemHeight -> TimelineItemDisplayMode.UltraCompact
        visualHeight < CompactTimelineItemHeight -> TimelineItemDisplayMode.Compact
        else -> TimelineItemDisplayMode.Comfortable
    }
    val latestItem by rememberUpdatedState(item)
    val latestOnClick by rememberUpdatedState(onClick)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestOnTimeChange by rememberUpdatedState(onTimeChange)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = xPx,
                    y = yPx + (dragOffsetY + topResizeOffsetY).roundToInt()
                )
            }
            .width(cardWidth)
            .height(visualHeight)
            .zIndex(if (isSelected) 3f else 1f + layout.lane)
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { latestOnClick() },
                    onLongPress = { latestOnSelect() }
                )
            }
            .pointerInput(item.id, start, end, hourHeightPx) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { latestOnSelect() },
                    onDragEnd = {
                        val deltaMinutes = dragOffsetY.toMinutes(hourHeightPx)
                        val (nextStart, nextEnd) = moveTimelineRange(start, end, deltaMinutes)
                        latestOnTimeChange(latestItem, nextStart, nextEnd)
                        dragOffsetY = 0f
                    },
                    onDragCancel = { dragOffsetY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                    }
                )
            }
    ) {
        content(item, isSelected, displayMode)
        if (isSelected && item.isResizable) {
            ResizeHandle(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .pointerInput(item.id, start, end, hourHeightPx) {
                        detectDragGestures(
                            onDragEnd = {
                                val deltaMinutes = topResizeOffsetY.toMinutes(hourHeightPx)
                                val (nextStart, nextEnd) = resizeTimelineStart(start, end, deltaMinutes)
                                latestOnTimeChange(latestItem, nextStart, nextEnd)
                                topResizeOffsetY = 0f
                            },
                            onDragCancel = { topResizeOffsetY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                topResizeOffsetY += dragAmount.y
                            }
                        )
                    }
            )
            ResizeHandle(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .pointerInput(item.id, start, end, hourHeightPx) {
                        detectDragGestures(
                            onDragEnd = {
                                val deltaMinutes = bottomResizeOffsetY.toMinutes(hourHeightPx)
                                val (nextStart, nextEnd) = resizeTimelineEnd(start, end, deltaMinutes)
                                latestOnTimeChange(latestItem, nextStart, nextEnd)
                                bottomResizeOffsetY = 0f
                            },
                            onDragCancel = { bottomResizeOffsetY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                bottomResizeOffsetY += dragAmount.y
                            }
                        )
                    }
            )
        }
    }
}

internal enum class TimelineItemDisplayMode {
    Comfortable,
    Compact,
    UltraCompact
}

@Composable
private fun ResizeHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 28.dp)
            .fillMaxWidth()
            .height(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ContentContainerAlpha))
        )
    }
}

internal data class TimelineItemLayout(
    val item: TimelineItem,
    val lane: Int,
    val laneCount: Int
)

internal fun buildTimelineLayouts(
    items: List<TimelineItem>
): List<TimelineItemLayout> {
    val timedItems = items.filter { it.startTimeMinutes != null }
    val entries = timedItems
        .sortedWith(compareBy<TimelineItem> { it.startTimeMinutes }.thenBy { it.sortOrder })

    val clusters = mutableListOf<List<TimelineItem>>()
    var currentCluster = mutableListOf<TimelineItem>()
    var currentClusterEnd = -1
    entries.forEach { entry ->
        val start = entry.startTimeMinutes!!
        val end = entry.endTimeMinutes ?: (start + DefaultDurationMinutes)
        if (currentCluster.isNotEmpty() && start >= currentClusterEnd) {
            clusters += currentCluster
            currentCluster = mutableListOf()
            currentClusterEnd = -1
        }
        currentCluster += entry
        currentClusterEnd = maxOf(currentClusterEnd, end)
    }
    if (currentCluster.isNotEmpty()) clusters += currentCluster

    return clusters.flatMap { cluster -> buildClusterLayouts(cluster) }
}

private fun buildClusterLayouts(entries: List<TimelineItem>): List<TimelineItemLayout> {
    val active = mutableListOf<Pair<Int, Int>>()
    val assigned = entries.map { entry ->
        val start = entry.startTimeMinutes!!
        val end = entry.endTimeMinutes ?: (start + DefaultDurationMinutes)
        active.removeAll { (_, e) -> e <= start }
        val usedLanes = active.map { it.first }.toSet()
        val lane = generateSequence(0) { it + 1 }.first { it !in usedLanes }
        active.add(lane to end)
        entry to lane
    }
    val laneCount = assigned.maxOfOrNull { (_, lane) -> lane + 1 } ?: 1
    return assigned.map { (entry, lane) ->
        TimelineItemLayout(item = entry, lane = lane, laneCount = laneCount)
    }
}

private suspend fun PointerInputScope.detectTimelineZoomGestures(
    onZoom: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        do {
            val event = awaitPointerEvent()
            val pressedPointerCount = event.changes.count { it.pressed }
            if (pressedPointerCount > 1) {
                val zoom = event.calculateZoom()
                if (zoom.isFinite() && abs(zoom - 1f) >= TimelineZoomEpsilon) {
                    onZoom(
                        event.calculateCentroid(),
                        event.calculatePan(),
                        zoom
                    )
                    event.changes.forEach { change ->
                        if (change.positionChanged()) {
                            change.consume()
                        }
                    }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

private fun Offset.toMinutes(hourHeightPx: Float): Int =
    y.toMinutes(hourHeightPx)

private fun Float.toMinutes(hourHeightPx: Float): Int =
    ((this / hourHeightPx) * 60f).roundToInt()

private suspend fun ScrollState.scrollToCurrentTime(
    currentTimeMinutes: Int,
    hourHeightPx: Float
) {
    scrollTo(
        (minutesToY(currentTimeMinutes, hourHeightPx) -
            hourHeightPx * CurrentTimeVisibleHoursBefore)
            .roundToInt()
            .coerceAtLeast(0)
    )
}

private suspend fun ScrollState.scrollToStartMinute(
    startTimeMinutes: Int,
    hourHeightPx: Float
) {
    scrollTo(minutesToY(startTimeMinutes, hourHeightPx).roundToInt().coerceAtLeast(0))
}

private fun minutesToY(minutes: Int, hourHeightPx: Float): Float =
    (minutes / 60f) * hourHeightPx

internal fun currentTimeMinutes(): Int {
    val time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return time.hour * 60 + time.minute
}

internal fun Int.snapToQuarterHour(): Int =
    (this / TimelineStepMinutes.toFloat()).roundToInt() * TimelineStepMinutes

internal fun moveTimelineRange(
    startTimeMinutes: Int,
    endTimeMinutes: Int,
    deltaMinutes: Int
): Pair<Int, Int> {
    val duration = (endTimeMinutes - startTimeMinutes).coerceAtLeast(MinimumDurationMinutes)
    val nextStart = (startTimeMinutes + deltaMinutes)
        .snapToQuarterHour()
        .coerceIn(0, MinutesPerDay - duration)
    return nextStart to nextStart + duration
}

internal fun resizeTimelineStart(
    startTimeMinutes: Int,
    endTimeMinutes: Int,
    deltaMinutes: Int
): Pair<Int, Int> {
    val maxStart = (endTimeMinutes - MinimumDurationMinutes)
        .floorToQuarterHour()
        .coerceAtLeast(0)
    val nextStart = (startTimeMinutes + deltaMinutes)
        .snapToQuarterHour()
        .coerceIn(0, maxStart)
    return nextStart to endTimeMinutes
}

internal fun resizeTimelineEnd(
    startTimeMinutes: Int,
    endTimeMinutes: Int,
    deltaMinutes: Int
): Pair<Int, Int> {
    val minEnd = (startTimeMinutes + MinimumDurationMinutes)
        .ceilToQuarterHour()
        .coerceAtMost(MinutesPerDay)
    val nextEnd = (endTimeMinutes + deltaMinutes)
        .snapToQuarterHour()
        .coerceIn(minEnd, MinutesPerDay)
    return startTimeMinutes to nextEnd
}

internal fun Int.floorToQuarterHour(): Int =
    (this / TimelineStepMinutes) * TimelineStepMinutes

internal fun Int.ceilToQuarterHour(): Int =
    if (this % TimelineStepMinutes == 0) {
        this
    } else {
        floorToQuarterHour() + TimelineStepMinutes
    }

internal fun Int.hourLabel(): String =
    when {
        this == 0 -> "12 AM"
        this < 12 -> "$this AM"
        this == 12 -> "12 PM"
        else -> "${this - 12} PM"
    }


internal const val TimelineStepMinutes = 15
internal const val DefaultDurationMinutes = 30
internal const val NoteDurationMinutes = 30
internal const val MinimumDurationMinutes = 15
internal const val LastStartMinute = MinutesPerDay - MinimumDurationMinutes
internal const val CurrentTimeVisibleHoursBefore = 2f
internal const val CollapsedAllDayItemCount = 2
internal const val DefaultTaskCardAlpha = 0.17f
internal const val SelectedTaskCardAlpha = 0.28f
private const val WorkdayStartMinutes = 8 * 60
private const val WorkdayEndMinutes = 18 * 60
private const val WorkdayVisibleHours = (WorkdayEndMinutes - WorkdayStartMinutes) / 60
private val CompactTimelineItemHeight = 48.dp
private val UltraCompactTimelineItemHeight = 34.dp
private val MinTimelineHourHeight = 12.dp
private val DefaultTimelineHourHeight = 80.dp
private val MaxTimelineHourHeight = 160.dp
private const val TimelineZoomEpsilon = 0.001f
