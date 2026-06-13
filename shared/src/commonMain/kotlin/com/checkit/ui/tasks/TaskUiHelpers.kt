package com.checkit.ui.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.ui.TaskWorkspaceView
import com.checkit.ui.shortMonthName
import com.checkit.ui.shortName
import com.checkit.ui.theme.toColor
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

internal fun TaskWorkspaceView.icon(): ImageVector = when (this) {
    TaskWorkspaceView.List -> Icons.Default.ViewList
    TaskWorkspaceView.Agenda -> Icons.Default.ViewAgenda
    TaskWorkspaceView.Timeline -> Icons.Default.Schedule
}

internal fun LocalDate.compact(): String {
    val today = today()
    val monthDay = "${shortMonthName()} $day"
    return when (this) {
        today -> "Today, $monthDay"
        today.plus(1, DateTimeUnit.DAY) -> "Tomorrow, $monthDay"
        today.plus(-1, DateTimeUnit.DAY) -> "Yesterday, $monthDay"
        today.plus(2, DateTimeUnit.DAY),
        today.plus(3, DateTimeUnit.DAY),
        today.plus(4, DateTimeUnit.DAY),
        today.plus(5, DateTimeUnit.DAY),
        today.plus(6, DateTimeUnit.DAY) -> "${dayOfWeek.shortName()}, $monthDay"
        else -> {
            if (year == today.year) monthDay else "$monthDay, $year"
        }
    }
}

fun NoteItem.cardColor(): Color {
    return list.color.toColor()
}

fun TaskItem.cardColor(): Color {
    return list.color.toColor()
}

fun TaskItem.timeRangeLabel(): String {
    val start = startTimeMinutes?.toClockLabel() ?: "Any time"
    val end = endTimeMinutes?.toClockLabel()
    return if (end == null) start else "$start - $end"
}

fun Int.formatDuration(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

fun Int.toClockLabel(): String {
    val hour = this / 60
    val minute = this % 60
    val suffix = if (hour >= 12) "PM" else "AM"
    val displayHour = when (val normalized = hour % 12) {
        0 -> 12
        else -> normalized
    }
    return "$displayHour:${minute.toString().padStart(2, '0')} $suffix"
}

enum class TimelineItemType { Task, Note, CheckIn }

data class TimelineItem(
    val id: String,
    val type: TimelineItemType,
    val date: LocalDate? = null,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val sortOrder: Int = 0,
    val isResizable: Boolean = false,
    val tag: Any? = null
)

fun Modifier.dashedBorder(
    width: Dp = 2.dp,
    color: Color = Color.Black,
    dashLength: Dp = 10.dp,
    gapLength: Dp = 10.dp,
    cornerRadius: Dp = 0.dp
): Modifier = this.drawBehind {
    // Convert Dp dimensions to target device pixel size
    val strokeWidthPx = width.toPx()
    val dashLengthPx = dashLength.toPx()
    val gapLengthPx = gapLength.toPx()
    val cornerRadiusPx = cornerRadius.toPx()

    val stroke = Stroke(
        width = strokeWidthPx,
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashLengthPx, gapLengthPx),
            phase = 0f
        )
    )

    // Adjust boundaries to ensure the line width is fully inside the component
    val halfWidth = strokeWidthPx / 2
    val sizeWithStroke = this.size.copy(
        width = this.size.width - strokeWidthPx,
        height = this.size.height - strokeWidthPx
    )

    // Draw the actual border shape
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(halfWidth, halfWidth),
        size = sizeWithStroke,
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        style = stroke
    )
}
