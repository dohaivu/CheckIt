package com.checkit.ui.myday

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.TagItem
import com.checkit.ui.components.statusBreathingGlow
import com.checkit.ui.shortcutDurationLabel
import com.checkit.ui.tasks.cardColor
import com.checkit.ui.theme.toColor
import kotlin.math.roundToInt

@Composable
internal fun DayLinearTimeline(
    items: List<DailyPlanItem>,
    modifier: Modifier = Modifier,
    showTotal: Boolean = true,
    showLabels: Boolean = true,
    showTagTotals: Boolean = true
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
            if (showTagTotals) {
                DayTagTotals(items)
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

@Composable
private fun DayTagTotals(items: List<DailyPlanItem>) {
    val aggregates = remember(items) { items.tagTimeTotals() }
    if (aggregates.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        aggregates.forEach { aggregate ->
            TagTimeChip(tag = aggregate.tag, minutes = aggregate.minutes)
        }
    }
}

@Composable
private fun TagTimeChip(tag: TagItem, minutes: Int) {
    val tagColor = remember(tag) { tag.color.toColor() }
    Text(
        text = "${tag.name} ${minutes.shortcutDurationLabel()}",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier
            .background(tagColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** Narrow chip matching the timeline height, showing hours/minutes with a breathing status aura. */
@Composable
internal fun WorkTimeChip(
    minutes: Int,
    modifier: Modifier = Modifier
) {
    if (minutes <= 0) return
    val totalHours = minutes / 60f
    val hours = minutes / 60
    val mins = minutes % 60

    // Define status color and pulse duration based on severity
    val (statusColor, duration) = when {
        totalHours < 3f -> Color(0xFFFF5252) to 1200 // Faster alert for critical
        totalHours < 6f -> Color(0xFFFFD740) to 2000 // Steady warning
        else -> Color(0xFF69F0AE) to 3000 // Slow healthy breath
    }

    val orbit = rememberInfiniteTransition(label = "workTimePulse")
    val pulse by orbit.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val baseSurface = MaterialTheme.colorScheme.surfaceContainerHigh
    val chipBgColor = remember(statusColor, baseSurface) {
        statusColor.copy(alpha = 0.12f).compositeOver(baseSurface)
    }
    val chipBorderColor = statusColor.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .padding(4.dp)
            .statusBreathingGlow(
                color = statusColor,
                pulseFraction = { pulse },
                cornerRadius = 10.dp + 4.dp
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    color = chipBgColor,
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.dp,
                    color = chipBorderColor,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (hours > 0) {
                Text(
                    text = "${hours}h",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (mins > 0 || hours == 0) {
                Text(
                    text = "${mins}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun DayTrack(blocks: List<DayTimelineBlock>) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
    val focusColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(50))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val corner = size.height / 2f
            drawRect(color = trackColor)
            DayFocusRanges.forEach { (startRatio, endRatio) ->
                drawRect(
                    color = focusColor,
                    topLeft = Offset(x = startRatio * size.width, y = 0f),
                    size = Size(width = (endRatio - startRatio) * size.width, height = size.height)
                )
            }
            blocks.forEach { block ->
                drawRoundRect(
                    color = block.color,
                    topLeft = Offset(x = size.width * block.startFraction, y = 0f),
                    size = Size(width = size.width * block.widthFraction, height = size.height),
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
) {
    val startFraction: Float =
        (startMinutes - DayTimelineStartMinutes).toFloat() / DayTimelineTotalMinutes
    val widthFraction: Float =
        (endMinutes - startMinutes).toFloat() / DayTimelineTotalMinutes
}

internal data class TagTimeAggregate(
    val tag: TagItem,
    val minutes: Int
)

internal fun List<DailyPlanItem>.tagTimeTotals(): List<TagTimeAggregate> {
    val totals = mutableMapOf<Long, TagTimeAggregate>()
    this.forEach { item ->
        val minutes = item.workMinutes()
        if (minutes <= 0) return@forEach
        item.tags.forEach { tag ->
            val current = totals[tag.id] ?: TagTimeAggregate(tag, 0)
            totals[tag.id] = current.copy(minutes = current.minutes + minutes)
        }
    }
    return totals.values.sortedWith(
        compareByDescending<TagTimeAggregate> { it.minutes }.thenBy { it.tag.name.lowercase() }
    )
}

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

private const val DayTimelineStartMinutes = 5 * 60
private const val DayTimelineEndMinutes = 22 * 60
private const val DayTimelineTotalMinutes = DayTimelineEndMinutes - DayTimelineStartMinutes

/** Prime hours (9-11, 13-17, 19-22) highlighted on the track, precomputed as [startRatio, endRatio] fractions. */
private val DayFocusRanges = listOf(
    8 * 60 to 11 * 60,
    13 * 60 to 16 * 60,
    19 * 60 to 22 * 60
).map { (startMinutes, endMinutes) ->
    (startMinutes - DayTimelineStartMinutes).toFloat() / DayTimelineTotalMinutes to
        (endMinutes - DayTimelineStartMinutes).toFloat() / DayTimelineTotalMinutes
}

private val DayTimelineTicks = listOf(
    DayTimelineTick(label = "6am", minutes = 6 * 60),
    DayTimelineTick(label = "12pm", minutes = 12 * 60),
    DayTimelineTick(label = "6pm", minutes = 18 * 60),
    DayTimelineTick(label = "10pm", minutes = 22 * 60)
)
