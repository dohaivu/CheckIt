package com.checkit.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import kotlin.math.roundToInt

@Composable
internal fun TaskTimelineView(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onCreateTask: (startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    onTaskTimeChange: (TaskItem, startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDayTasks = tasks.filter { it.startTimeMinutes == null }
    val timedTasks = tasks
        .filter { it.startTimeMinutes != null }
        .sortedWith(compareBy<TaskItem> { it.startTimeMinutes ?: Int.MAX_VALUE }.thenBy { it.sortOrder })
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AllDaySection(
            tasks = allDayTasks,
            notes = notes,
            lists = lists,
            showListName = showListName,
            onTaskClick = onTaskClick,
            onNoteClick = onNoteClick
        )
        TimelineGrid(
            tasks = timedTasks,
            lists = lists,
            showListName = showListName,
            onTaskClick = onTaskClick,
            onCreateTask = onCreateTask,
            onTaskTimeChange = onTaskTimeChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AllDaySection(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit
) {
    if (tasks.isEmpty() && notes.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "All-day",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tasks.forEach { task ->
                AllDayChip(
                    label = task.name.ifBlank { "Untitled task" },
                    icon = { Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    color = task.priority.color(),
                    onClick = { onTaskClick(task) },
                    supportingLabel = if (showListName) lists.firstOrNull { it.id == task.listId }?.name else null
                )
            }
            notes.forEach { note ->
                AllDayChip(
                    label = note.content.ifBlank { "Empty note" },
                    icon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onNoteClick(note) },
                    supportingLabel = if (showListName) lists.firstOrNull { it.id == note.listId }?.name else null
                )
            }
        }
    }
}

@Composable
private fun AllDayChip(
    label: String,
    icon: @Composable () -> Unit,
    color: Color,
    onClick: () -> Unit,
    supportingLabel: String?
) {
    AssistChip(
        onClick = onClick,
        leadingIcon = icon,
        label = {
            Text(
                text = supportingLabel?.let { "$label · $it" } ?: label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = Modifier,
        border = null,
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            leadingIconContentColor = color
        )
    )
}

@Composable
private fun TimelineGrid(
    tasks: List<TaskItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onCreateTask: (startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    onTaskTimeChange: (TaskItem, startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val hourHeight = 88.dp
    val axisWidth = 58.dp
    val totalHeight = hourHeight * HoursPerDay
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val axisWidthPx = with(density) { axisWidth.toPx() }
    val taskLayouts = remember(tasks) { buildTaskLayouts(tasks) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        val taskAreaWidth = (maxWidth - axisWidth - 14.dp).coerceAtLeast(1.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .pointerInput(hourHeightPx, axisWidthPx) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            if (offset.x >= axisWidthPx) {
                                val start = offset.y.toMinutes(hourHeightPx).snapToQuarterHour()
                                    .coerceIn(0, LastStartMinute)
                                onCreateTask(start, start + DefaultDurationMinutes)
                            }
                        }
                    )
                }
        ) {
            HourRows(
                hourHeight = hourHeight,
                axisWidth = axisWidth
            )
            taskLayouts.forEach { layout ->
                TimelineTaskCard(
                    layout = layout,
                    lists = lists,
                    axisWidth = axisWidth,
                    taskAreaWidth = taskAreaWidth,
                    hourHeight = hourHeight,
                    hourHeightPx = hourHeightPx,
                    showListName = showListName,
                    onClick = { onTaskClick(layout.task) },
                    onTimeChange = { start, end -> onTaskTimeChange(layout.task, start, end) }
                )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight)
            ) {
                Box(
                    modifier = Modifier
                        .width(axisWidth)
                        .height(hourHeight)
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        text = hour.hourLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(hourHeight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
                    )
                    repeat(3) { quarter ->
                        Box(
                            modifier = Modifier
                                .offset(y = hourHeight * ((quarter + 1) / 4f))
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTaskCard(
    layout: TimelineTaskLayout,
    lists: List<TaskList>,
    axisWidth: Dp,
    taskAreaWidth: Dp,
    hourHeight: Dp,
    hourHeightPx: Float,
    showListName: Boolean,
    onClick: () -> Unit,
    onTimeChange: (startTimeMinutes: Int, endTimeMinutes: Int) -> Unit
) {
    val task = layout.task
    val start = task.startTimeMinutes ?: return
    val end = task.endTimeMinutes ?: (start + DefaultDurationMinutes).coerceAtMost(MinutesPerDay)
    val duration = (end - start).coerceAtLeast(MinimumDurationMinutes)
    val y = hourHeight * (start / 60f)
    val height = (hourHeight * (duration / 60f)).coerceAtLeast(36.dp)
    val density = LocalDensity.current
    val yPx = with(density) { y.roundToPx() }
    val laneWidth = taskAreaWidth / layout.laneCount
    val cardWidth = (laneWidth - 4.dp).coerceAtLeast(44.dp)
    val x = axisWidth + 6.dp + laneWidth * layout.lane
    val xPx = with(density) { x.roundToPx() }
    val list = if (showListName) lists.firstOrNull { it.id == task.listId } else null
    var dragOffsetY by remember(task.id, start, end) { mutableFloatStateOf(0f) }
    var topResizeOffsetY by remember(task.id, start, end) { mutableFloatStateOf(0f) }
    var bottomResizeOffsetY by remember(task.id, start, end) { mutableFloatStateOf(0f) }
    val resizeHeightDelta = with(density) { (bottomResizeOffsetY - topResizeOffsetY).toDp() }
    val visualHeight = (height + resizeHeightDelta).coerceAtLeast(36.dp)

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = xPx,
                    y = yPx + (dragOffsetY + topResizeOffsetY).roundToInt()
                )
            }
            .width(cardWidth)
            .height(visualHeight)
            .zIndex(1f + layout.lane)
            .clickable(onClick = onClick)
            .pointerInput(task.id, start, end, hourHeightPx) {
                detectDragGestures(
                    onDragEnd = {
                        val deltaMinutes = dragOffsetY.toMinutes(hourHeightPx).snapToQuarterHour()
                        val (nextStart, nextEnd) = moveTimelineRange(start, end, deltaMinutes)
                        onTimeChange(nextStart, nextEnd)
                        dragOffsetY = 0f
                    },
                    onDragCancel = { dragOffsetY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = task.priority.color().copy(alpha = 0.17f),
        tonalElevation = 1.dp
    ) {
        Box {
            ResizeHandle(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .pointerInput(task.id, start, end, hourHeightPx) {
                        detectDragGestures(
                            onDragEnd = {
                                val deltaMinutes = topResizeOffsetY.toMinutes(hourHeightPx).snapToQuarterHour()
                                val (nextStart, nextEnd) = resizeTimelineStart(start, end, deltaMinutes)
                                onTimeChange(nextStart, nextEnd)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = task.name.ifBlank { "Untitled task" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${start.toClockLabel()} - ${end.toClockLabel()}${list?.let { " · ${it.name}" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ResizeHandle(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .pointerInput(task.id, start, end, hourHeightPx) {
                        detectDragGestures(
                            onDragEnd = {
                                val deltaMinutes = bottomResizeOffsetY.toMinutes(hourHeightPx).snapToQuarterHour()
                                val (nextStart, nextEnd) = resizeTimelineEnd(start, end, deltaMinutes)
                                onTimeChange(nextStart, nextEnd)
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
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
        )
    }
}

internal data class TimelineTaskLayout(
    val task: TaskItem,
    val lane: Int,
    val laneCount: Int
)

internal fun buildTaskLayouts(tasks: List<TaskItem>): List<TimelineTaskLayout> {
    val clusters = mutableListOf<List<TaskItem>>()
    var currentCluster = mutableListOf<TaskItem>()
    var currentClusterEnd = -1
    tasks.forEach { task ->
        val start = task.startTimeMinutes ?: 0
        val end = task.endTimeMinutes ?: (start + DefaultDurationMinutes)
        if (currentCluster.isNotEmpty() && start >= currentClusterEnd) {
            clusters += currentCluster
            currentCluster = mutableListOf()
            currentClusterEnd = -1
        }
        currentCluster += task
        currentClusterEnd = maxOf(currentClusterEnd, end)
    }
    if (currentCluster.isNotEmpty()) clusters += currentCluster

    return clusters.flatMap { cluster -> buildClusterLayouts(cluster) }
}

private fun buildClusterLayouts(tasks: List<TaskItem>): List<TimelineTaskLayout> {
    val active = mutableListOf<Pair<Int, Int>>()
    val assigned = tasks.map { task ->
        val start = task.startTimeMinutes ?: 0
        active.removeAll { (_, end) -> end <= start }
        val usedLanes = active.map { it.first }.toSet()
        val lane = generateSequence(0) { it + 1 }.first { it !in usedLanes }
        active.add(lane to (task.endTimeMinutes ?: (start + DefaultDurationMinutes)))
        task to lane
    }
    val laneCount = assigned.maxOfOrNull { (_, lane) -> lane + 1 } ?: 1
    return assigned.map { (task, lane) ->
        TimelineTaskLayout(task = task, lane = lane, laneCount = laneCount)
    }
}

private fun Offset.toMinutes(hourHeightPx: Float): Int =
    y.toMinutes(hourHeightPx)

private fun Float.toMinutes(hourHeightPx: Float): Int =
    ((this / hourHeightPx) * 60f).roundToInt()

internal fun Int.snapToQuarterHour(): Int =
    (this / TimelineStepMinutes.toFloat()).roundToInt() * TimelineStepMinutes

internal fun moveTimelineRange(
    startTimeMinutes: Int,
    endTimeMinutes: Int,
    deltaMinutes: Int
): Pair<Int, Int> {
    val duration = (endTimeMinutes - startTimeMinutes).coerceAtLeast(MinimumDurationMinutes)
    val nextStart = (startTimeMinutes + deltaMinutes).coerceIn(0, MinutesPerDay - duration)
    return nextStart to nextStart + duration
}

internal fun resizeTimelineStart(
    startTimeMinutes: Int,
    endTimeMinutes: Int,
    deltaMinutes: Int
): Pair<Int, Int> {
    val nextStart = (startTimeMinutes + deltaMinutes).coerceIn(
        0,
        endTimeMinutes - MinimumDurationMinutes
    )
    return nextStart to endTimeMinutes
}

internal fun resizeTimelineEnd(
    startTimeMinutes: Int,
    endTimeMinutes: Int,
    deltaMinutes: Int
): Pair<Int, Int> {
    val nextEnd = (endTimeMinutes + deltaMinutes).coerceIn(
        startTimeMinutes + MinimumDurationMinutes,
        MinutesPerDay
    )
    return startTimeMinutes to nextEnd
}

private fun Int.hourLabel(): String =
    when {
        this == 0 -> "12 AM"
        this < 12 -> "$this AM"
        this == 12 -> "12 PM"
        else -> "${this - 12} PM"
    }

private const val HoursPerDay = 24
private const val MinutesPerDay = 24 * 60
private const val TimelineStepMinutes = 15
private const val DefaultDurationMinutes = 60
private const val MinimumDurationMinutes = 15
private const val LastStartMinute = MinutesPerDay - MinimumDurationMinutes
