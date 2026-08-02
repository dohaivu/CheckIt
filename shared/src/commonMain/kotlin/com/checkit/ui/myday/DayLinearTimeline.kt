package com.checkit.ui.myday

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
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
        modifier = modifier.height(IntrinsicSize.Max),
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
            WorkTimeChip(
                minutes = workMinutes,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

/** Narrow chip matching the timeline height, showing hours/minutes on two lines. */
@Composable
internal fun WorkTimeChip(
    minutes: Int,
    modifier: Modifier = Modifier
) {
    if (minutes <= 0) return
    val hours = minutes / 60
    val mins = minutes % 60
    Column(
        modifier = modifier
            .widthIn(min = 44.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hours > 0) {
            Text(
                text = "${hours}h",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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

@Composable
private fun DayTrack(blocks: List<DayTimelineBlock>) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
    val focusColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        val corner = size.height / 2f
        val pill = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(corner, corner)))
        }
        clipPath(pill) {
            drawRect(color = trackColor)
            DayFocusRanges.forEach { (startMinutes, endMinutes) ->
                val left = (startMinutes - DayTimelineStartMinutes).toFloat() / DayTimelineTotalMinutes * size.width
                val right = (endMinutes - DayTimelineStartMinutes).toFloat() / DayTimelineTotalMinutes * size.width
                drawRect(
                    color = focusColor,
                    topLeft = Offset(x = left, y = 0f),
                    size = Size(width = right - left, height = size.height)
                )
            }
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
}

@Composable
private fun DayTimelineLabels(modifier: Modifier = Modifier) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            DayTimelineTicks.forEach { tick ->
                Text(
                    text = tick.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0

        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val tick = DayTimelineTicks[index]
                val labelWidthPx = placeable.width.toFloat()
                val x = (width * tick.fraction - labelWidthPx / 2f)
                    .coerceIn(0f, width - labelWidthPx)
                placeable.placeRelative(x.roundToInt(), 0)
            }
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

/** Prime hours (9-11, 13-17, 19-22) highlighted on the track so users see time spent in focus windows. */
private val DayFocusRanges = listOf(
    8 * 60 to 11 * 60,
    13 * 60 to 16 * 60,
    19 * 60 to 22 * 60
)

private val DayTimelineTicks = listOf(
    DayTimelineTick(label = "6am", minutes = 6 * 60),
    DayTimelineTick(label = "12pm", minutes = 12 * 60),
    DayTimelineTick(label = "6pm", minutes = 18 * 60),
    DayTimelineTick(label = "10pm", minutes = 22 * 60)
)
