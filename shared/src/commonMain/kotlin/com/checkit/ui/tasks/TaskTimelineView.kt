package com.checkit.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.checkit.domain.TaskStatus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock

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
    onNoteTimeChange: (NoteItem, startTimeMinutes: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val allDayTasks = tasks.filter { it.startTimeMinutes == null }
    val timedTasks = tasks.filter { it.startTimeMinutes != null }
    val allDayNotes = notes.filter { it.startTimeMinutes == null }
    val timedNotes = notes.filter { it.startTimeMinutes != null }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AllDaySection(
            tasks = allDayTasks,
            notes = allDayNotes,
            lists = lists,
            showListName = showListName,
            onTaskClick = onTaskClick,
            onNoteClick = onNoteClick
        )
        TimelineGrid(
            tasks = timedTasks,
            notes = timedNotes,
            lists = lists,
            showListName = showListName,
            onTaskClick = onTaskClick,
            onNoteClick = onNoteClick,
            onCreateTask = onCreateTask,
            onTaskTimeChange = onTaskTimeChange,
            onNoteTimeChange = onNoteTimeChange,
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

    val totalItemCount = tasks.size + notes.size
    var expanded by remember(totalItemCount) { mutableStateOf(totalItemCount <= CollapsedAllDayItemCount) }
    val visibleTasks = if (expanded) tasks else tasks.take(CollapsedAllDayItemCount)
    val remainingSlots = (CollapsedAllDayItemCount - visibleTasks.size).coerceAtLeast(0)
    val visibleNotes = if (expanded) notes else notes.take(remainingSlots)
    val hiddenItemCount = totalItemCount - visibleTasks.size - visibleNotes.size

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
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            visibleTasks.forEach { task ->
                val list = lists.firstOrNull { it.id == task.listId }
                AllDayItemRow(
                    label = task.name.ifBlank { "Untitled task" },
                    icon = { Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    color = taskCardColor(task, list),
                    onClick = { onTaskClick(task) },
                    supportingLabel = if (showListName) list?.name else null
                )
            }
            visibleNotes.forEach { note ->
                AllDayItemRow(
                    label = note.content.ifBlank { "Empty note" },
                    icon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onNoteClick(note) },
                    supportingLabel = if (showListName) lists.firstOrNull { it.id == note.listId }?.name else null
                )
            }
        }
    }
}

@Composable
private fun AllDayItemRow(
    label: String,
    icon: @Composable () -> Unit,
    color: Color,
    onClick: () -> Unit,
    supportingLabel: String?
) {
    val rowLabel = remember(label, supportingLabel) { compactAllDayLabel(label, supportingLabel) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = rowLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.8f))
        )
    }
}

@Composable
private fun TimelineGrid(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onCreateTask: (startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    onTaskTimeChange: (TaskItem, startTimeMinutes: Int, endTimeMinutes: Int) -> Unit,
    onNoteTimeChange: (NoteItem, startTimeMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val hourHeight = 80.dp
    val axisWidth = 56.dp
    val totalHeight = hourHeight * HoursPerDay
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val axisWidthPx = with(density) { axisWidth.toPx() }
    val layouts = remember(tasks, notes) { buildTimelineLayouts(tasks, notes) }
    val currentTimeMinutes = remember { currentTimeMinutes() }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        val taskAreaWidth = (maxWidth - axisWidth - 14.dp).coerceAtLeast(1.dp)
        LaunchedEffect(currentTimeMinutes, hourHeightPx) {
            val targetScroll = (minutesToY(currentTimeMinutes, hourHeightPx) -
                hourHeightPx * CurrentTimeVisibleHoursBefore).roundToInt().coerceAtLeast(0)
            scrollState.scrollTo(targetScroll)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .pointerInput(hourHeightPx, axisWidthPx) {
                    detectTapGestures(
                        onTap = { selectedTaskId = null },
                        onLongPress = { offset ->
                            if (offset.x >= axisWidthPx) {
                                val start = offset.y.toMinutes(hourHeightPx)
                                    .snapToQuarterHour()
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
            CurrentTimeLine(
                currentTimeMinutes = currentTimeMinutes,
                hourHeight = hourHeight,
                axisWidth = axisWidth
            )
            layouts.forEach { layout ->
                when (val entry = layout.entry) {
                    is TimelineEntry.Task -> {
                        TimelineTaskCard(
                            layout = layout,
                            task = entry.task,
                            lists = lists,
                            axisWidth = axisWidth,
                            taskAreaWidth = taskAreaWidth,
                            hourHeight = hourHeight,
                            hourHeightPx = hourHeightPx,
                            showListName = showListName,
                            isSelected = selectedTaskId == entry.id,
                            onClick = { onTaskClick(entry.task) },
                            onSelect = { selectedTaskId = entry.id },
                            onTimeChange = onTaskTimeChange
                        )
                    }
                    is TimelineEntry.Note -> {
                        TimelineNoteCard(
                            layout = layout,
                            note = entry.note,
                            lists = lists,
                            axisWidth = axisWidth,
                            taskAreaWidth = taskAreaWidth,
                            hourHeight = hourHeight,
                            hourHeightPx = hourHeightPx,
                            showListName = showListName,
                            onClick = { onNoteClick(entry.note) },
                            onTimeChange = onNoteTimeChange
                        )
                    }
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
private fun TimelineTaskCard(
    layout: TimelineItemLayout,
    task: TaskItem,
    lists: List<TaskList>,
    axisWidth: Dp,
    taskAreaWidth: Dp,
    hourHeight: Dp,
    hourHeightPx: Float,
    showListName: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit,
    onTimeChange: (TaskItem, startTimeMinutes: Int, endTimeMinutes: Int) -> Unit
) {
    val start = task.startTimeMinutes ?: return
    val end = task.endTimeMinutes ?: (start + DefaultDurationMinutes)
    val duration = (end - start).coerceAtLeast(MinimumDurationMinutes)
    val y = hourHeight * (start / 60f)
    val height = (hourHeight * (duration / 60f)).coerceAtLeast(36.dp)
    val density = LocalDensity.current
    val yPx = with(density) { y.roundToPx() }
    val laneWidth = taskAreaWidth / layout.laneCount
    val cardWidth = (laneWidth - 4.dp).coerceAtLeast(44.dp)
    val x = axisWidth + 6.dp + laneWidth * layout.lane
    val xPx = with(density) { x.roundToPx() }
    val list = lists.firstOrNull { it.id == task.listId }
    var dragOffsetY by remember(task.id, start, end) { mutableFloatStateOf(0f) }
    var topResizeOffsetY by remember(task.id, start, end) { mutableFloatStateOf(0f) }
    var bottomResizeOffsetY by remember(task.id, start, end) { mutableFloatStateOf(0f) }
    val resizeHeightDelta = with(density) { (bottomResizeOffsetY - topResizeOffsetY).toDp() }
    val visualHeight = (height + resizeHeightDelta).coerceAtLeast(36.dp)
    val containerAlpha = if (isSelected) SelectedTaskCardAlpha else DefaultTaskCardAlpha

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
            .pointerInput(task.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onSelect() }
                )
            }
            .pointerInput(task.id, start, end, hourHeightPx) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onSelect() },
                    onDragEnd = {
                        val deltaMinutes = dragOffsetY.toMinutes(hourHeightPx)
                        val (nextStart, nextEnd) = moveTimelineRange(start, end, deltaMinutes)
                        onTimeChange(task, nextStart, nextEnd)
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
        TaskCard(
            title = task.name.ifBlank { "Untitled task" },
            timeLabel = "${start.toClockLabel()} - ${end.toClockLabel()}",
            supportingText = if (showListName) list?.name else null,
            leadingContent = { TaskStatusIcon(task.status, task.priority) },
            color = taskCardColor(task, list),
            minHeight = 36.dp,
            titleMaxLines = 1,
            completed = task.status == TaskStatus.Completed,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            containerAlpha = containerAlpha,
            tonalElevation = if (isSelected) 3.dp else 1.dp,
            modifier = Modifier.matchParentSize()
        )
        if (isSelected) {
            ResizeHandle(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .pointerInput(task.id, start, end, hourHeightPx) {
                        detectDragGestures(
                            onDragEnd = {
                                val deltaMinutes = topResizeOffsetY.toMinutes(hourHeightPx)
                                val (nextStart, nextEnd) = resizeTimelineStart(start, end, deltaMinutes)
                                onTimeChange(task, nextStart, nextEnd)
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
                    .pointerInput(task.id, start, end, hourHeightPx) {
                        detectDragGestures(
                            onDragEnd = {
                                val deltaMinutes = bottomResizeOffsetY.toMinutes(hourHeightPx)
                                val (nextStart, nextEnd) = resizeTimelineEnd(start, end, deltaMinutes)
                                onTimeChange(task, nextStart, nextEnd)
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
private fun TimelineNoteCard(
    layout: TimelineItemLayout,
    note: NoteItem,
    lists: List<TaskList>,
    axisWidth: Dp,
    taskAreaWidth: Dp,
    hourHeight: Dp,
    hourHeightPx: Float,
    showListName: Boolean,
    onClick: () -> Unit,
    onTimeChange: (NoteItem, startTimeMinutes: Int) -> Unit
) {
    val start = note.startTimeMinutes ?: return
    val end = start + NoteDurationMinutes
    val y = hourHeight * (start / 60f)
    val height = (hourHeight * (NoteDurationMinutes / 60f)).coerceAtLeast(36.dp)
    val density = LocalDensity.current
    val yPx = with(density) { y.roundToPx() }
    val laneWidth = taskAreaWidth / layout.laneCount
    val cardWidth = (laneWidth - 4.dp).coerceAtLeast(44.dp)
    val x = axisWidth + 6.dp + laneWidth * layout.lane
    val xPx = with(density) { x.roundToPx() }
    val list = lists.firstOrNull { it.id == note.listId }
    var dragOffsetY by remember(note.id, start) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = xPx,
                    y = yPx + dragOffsetY.roundToInt()
                )
            }
            .width(cardWidth)
            .height(height)
            .zIndex(1f + layout.lane)
            .pointerInput(note.id) {
                detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(note.id, start, hourHeightPx) {
                detectDragGesturesAfterLongPress(
                    onDragEnd = {
                        val deltaMinutes = dragOffsetY.toMinutes(hourHeightPx)
                        val nextStart = (start + deltaMinutes)
                            .snapToQuarterHour()
                            .coerceIn(0, MinutesPerDay - NoteDurationMinutes)
                        onTimeChange(note, nextStart)
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
        TaskCard(
            title = note.content.ifBlank { "Empty note" },
            timeLabel = start.toClockLabel(),
            supportingText = if (showListName) list?.name else null,
            color = list?.color?.toColor() ?: MaterialTheme.colorScheme.secondary,
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
            minHeight = 36.dp,
            titleMaxLines = 1,
            completed = note.status == TaskStatus.Completed,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            containerAlpha = DefaultTaskCardAlpha,
            tonalElevation = 1.dp,
            modifier = Modifier.matchParentSize()
        )
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

internal sealed class TimelineEntry {
    abstract val id: String
    abstract val startTimeMinutes: Int
    abstract val endTimeMinutes: Int
    abstract val sortOrder: Int
    abstract val listId: Long
    abstract val name: String

    data class Task(val task: TaskItem) : TimelineEntry() {
        override val id: String = "task-${task.id}"
        override val startTimeMinutes: Int = task.startTimeMinutes!!
        override val endTimeMinutes: Int = task.endTimeMinutes ?: (startTimeMinutes + DefaultDurationMinutes)
        override val sortOrder: Int = task.sortOrder
        override val listId: Long = task.listId
        override val name: String = task.name
    }

    data class Note(val note: NoteItem) : TimelineEntry() {
        override val id: String = "note-${note.id}"
        override val startTimeMinutes: Int = note.startTimeMinutes!!
        override val endTimeMinutes: Int = startTimeMinutes + NoteDurationMinutes
        override val sortOrder: Int = note.sortOrder
        override val listId: Long = note.listId
        override val name: String = note.content
    }
}

internal data class TimelineItemLayout(
    val entry: TimelineEntry,
    val lane: Int,
    val laneCount: Int
)

internal fun buildTimelineLayouts(
    tasks: List<TaskItem>,
    notes: List<NoteItem>
): List<TimelineItemLayout> {
    val entries = (tasks.map { TimelineEntry.Task(it) } + notes.map { TimelineEntry.Note(it) })
        .sortedWith(compareBy<TimelineEntry> { it.startTimeMinutes }.thenBy { it.sortOrder })

    val clusters = mutableListOf<List<TimelineEntry>>()
    var currentCluster = mutableListOf<TimelineEntry>()
    var currentClusterEnd = -1
    entries.forEach { entry ->
        val start = entry.startTimeMinutes
        val end = entry.endTimeMinutes
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

private fun buildClusterLayouts(entries: List<TimelineEntry>): List<TimelineItemLayout> {
    val active = mutableListOf<Pair<Int, Int>>()
    val assigned = entries.map { entry ->
        val start = entry.startTimeMinutes
        active.removeAll { (_, end) -> end <= start }
        val usedLanes = active.map { it.first }.toSet()
        val lane = generateSequence(0) { it + 1 }.first { it !in usedLanes }
        active.add(lane to entry.endTimeMinutes)
        entry to lane
    }
    val laneCount = assigned.maxOfOrNull { (_, lane) -> lane + 1 } ?: 1
    return assigned.map { (entry, lane) ->
        TimelineItemLayout(entry = entry, lane = lane, laneCount = laneCount)
    }
}

private fun Offset.toMinutes(hourHeightPx: Float): Int =
    y.toMinutes(hourHeightPx)

private fun Float.toMinutes(hourHeightPx: Float): Int =
    ((this / hourHeightPx) * 60f).roundToInt()

private fun minutesToY(minutes: Int, hourHeightPx: Float): Float =
    (minutes / 60f) * hourHeightPx

private fun currentTimeMinutes(): Int {
    val time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return time.hour * 60 + time.minute
}

private fun compactAllDayLabel(label: String, supportingLabel: String?): String {
    val primary = label
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .ifBlank { label }
    return supportingLabel?.let { "$primary · $it" } ?: primary
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

private fun Int.floorToQuarterHour(): Int =
    (this / TimelineStepMinutes) * TimelineStepMinutes

private fun Int.ceilToQuarterHour(): Int =
    if (this % TimelineStepMinutes == 0) {
        this
    } else {
        floorToQuarterHour() + TimelineStepMinutes
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
private const val NoteDurationMinutes = 30
private const val MinimumDurationMinutes = 15
private const val LastStartMinute = MinutesPerDay - MinimumDurationMinutes
private const val CurrentTimeVisibleHoursBefore = 2f
private const val CollapsedAllDayItemCount = 2
private const val DefaultTaskCardAlpha = 0.17f
private const val SelectedTaskCardAlpha = 0.28f
