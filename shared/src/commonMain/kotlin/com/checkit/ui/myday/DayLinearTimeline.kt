package com.checkit.ui.myday

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.ui.tasks.cardColor
import kotlin.math.roundToInt

@Composable
internal fun DayLinearTimeline(
    items: List<DailyPlanItem>,
    modifier: Modifier = Modifier,
    showTotal: Boolean = true,
    showLabels: Boolean = true
) {
    val blocks = remember(items) { items.toDayTimelineBlocks() }
    val workMinutes = remember(blocks) { blocks.totalOccupiedMinutes() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DayTrack(blocks)
            if (showLabels) {
                DayTimelineLabels()
            }
        }
        if (showTotal) {
            WorkTimeChip(minutes = workMinutes)
        }
    }
}

/** Narrow chip matching the timeline height, showing hours/minutes on two lines. */
@Composable
internal fun WorkTimeChip(minutes: Int) {
    if (minutes <= 0) return
    val hours = minutes / 60
    val mins = minutes % 60
    Surface(
        modifier = Modifier.widthIn(min = 44.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (hours > 0) {
                Text(
                    text = "${hours}h",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (mins > 0 || hours == 0) {
                Text(
                    text = "${mins}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun DayTrack(blocks: List<DayTimelineBlock>) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
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
}

@Composable
private fun DayTimelineLabels(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
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

private fun List<DailyPlanItem>.toDayTimelineBlocks(): List<DayTimelineBlock> {
    return mapNotNull { item ->
        val start = item.startTimeMinutes ?: return@mapNotNull null
        val end = item.endTimeMinutes ?: return@mapNotNull null
        val clippedStart = start.coerceIn(DayTimelineStartMinutes, DayTimelineEndMinutes)
        val clippedEnd = end.coerceIn(DayTimelineStartMinutes, DayTimelineEndMinutes)
        if (clippedEnd <= clippedStart) {
            null
        } else {
            DayTimelineBlock(
                startMinutes = clippedStart,
                endMinutes = clippedEnd,
                color = item.cardColor()
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

private const val DayTimelineStartMinutes = 6 * 60
private const val DayTimelineEndMinutes = 22 * 60
private const val DayTimelineTotalMinutes = DayTimelineEndMinutes - DayTimelineStartMinutes
private val DayTimelineTicks = listOf(
    DayTimelineTick(label = "6am", minutes = 6 * 60),
    DayTimelineTick(label = "12pm", minutes = 12 * 60),
    DayTimelineTick(label = "6pm", minutes = 18 * 60),
    DayTimelineTick(label = "10pm", minutes = 22 * 60)
)
