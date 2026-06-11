package com.checkit.ui.myday

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.ui.parseHexColorOrNull
import com.checkit.ui.components.priorityColor
import com.checkit.ui.tasks.cardColor
import kotlin.math.roundToInt

@Composable
internal fun DayLinearTimeline(
    items: List<DailyPlanItem>,
    board: TaskBoard,
    modifier: Modifier = Modifier
) {
    val blocks = remember(items, board) { items.toDayTimelineBlocks(board) }
    val workMinutes = remember(blocks) { blocks.totalOccupiedMinutes() }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh//.copy(alpha = 0.75f)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val corner = size.height / 2f
            drawRoundRect(
                color = trackColor,
                cornerRadius = CornerRadius(corner, corner),
                size = size
            )
            blocks.forEach { block ->
                val startFraction = (block.startMinutes - DayTimelineStartMinutes).toFloat() / DayTimelineTotalMinutes
                val widthFraction = (block.endMinutes - block.startMinutes).toFloat() / DayTimelineTotalMinutes
                drawRoundRect(
                    color = block.color,
                    topLeft = Offset(x = size.width * startFraction, y = 0f),
                    size = Size(width = size.width * widthFraction, height = size.height),
                    cornerRadius = CornerRadius(corner, corner)
                )
            }
        }
        DayTimelineLabels()
        Text(
            text = "Total work: ${workMinutes.toDurationLabel()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun DayTimelineLabels() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val labelWidth = 44.dp
        val density = LocalDensity.current
        val labelWidthPx = with(density) { labelWidth.toPx() }
        val trackWidthPx = with(density) { maxWidth.toPx() }
        DayTimelineTicks.forEach { tick ->
            Text(
                text = tick.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier
                    .width(labelWidth)
                    .offset {
                        val x = (trackWidthPx * tick.fraction - labelWidthPx / 2f)
                            .coerceIn(0f, trackWidthPx - labelWidthPx)
                        IntOffset(x = x.roundToInt(), y = 0)
                    },
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class DayTimelineBlock(
    val startMinutes: Int,
    val endMinutes: Int,
    val color: Color
)

private data class DayTimelineTick(
    val label: String,
    val minutes: Int
) {
    val fraction: Float =
        ((minutes - DayTimelineStartMinutes).toFloat() / DayTimelineTotalMinutes).coerceIn(0f, 1f)
}

private fun List<DailyPlanItem>.toDayTimelineBlocks(board: TaskBoard): List<DayTimelineBlock> {
    val tasksById = board.tasksById
    val listsById = board.lists.associateBy { it.id }
    return mapNotNull { item ->
        val start = item.startTimeMinutes ?: return@mapNotNull null
        val end = item.endTimeMinutes ?: return@mapNotNull null
        val clippedStart = start.coerceIn(DayTimelineStartMinutes, DayTimelineEndMinutes)
        val clippedEnd = end.coerceIn(DayTimelineStartMinutes, DayTimelineEndMinutes)
        if (clippedEnd <= clippedStart) {
            null
        } else {
            val task = item.taskId?.let { tasksById[it] }
            val list = task?.list
            DayTimelineBlock(
                startMinutes = clippedStart,
                endMinutes = clippedEnd,
                color = dailyItemColor(task, list)
            )
        }
    }
}

private fun List<DayTimelineBlock>.totalOccupiedMinutes(): Int {
    if (isEmpty()) return 0
    val sorted = sortedBy { it.startMinutes }
    var total = 0
    var currentStart = sorted.first().startMinutes
    var currentEnd = sorted.first().endMinutes
    sorted.drop(1).forEach { block ->
        if (block.startMinutes <= currentEnd) {
            currentEnd = maxOf(currentEnd, block.endMinutes)
        } else {
            total += currentEnd - currentStart
            currentStart = block.startMinutes
            currentEnd = block.endMinutes
        }
    }
    return total + currentEnd - currentStart
}

private fun Int.toDurationLabel(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours == 0 -> "${minutes}m"
        minutes == 0 -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

internal fun dailyItemColor(task: TaskItem?, list: TaskList?): Color =
    list?.color?.parseHexColorOrNull()
        ?: task?.priority?.priorityColor()
        ?: Color(0xFF64748B)

private const val DayTimelineStartMinutes = 6 * 60
private const val DayTimelineEndMinutes = 22 * 60
private const val DayTimelineTotalMinutes = DayTimelineEndMinutes - DayTimelineStartMinutes
private val DayTimelineTicks = listOf(
    DayTimelineTick(label = "6am", minutes = 6 * 60),
    DayTimelineTick(label = "12pm", minutes = 12 * 60),
    DayTimelineTick(label = "6pm", minutes = 18 * 60),
    DayTimelineTick(label = "10pm", minutes = 22 * 60)
)
