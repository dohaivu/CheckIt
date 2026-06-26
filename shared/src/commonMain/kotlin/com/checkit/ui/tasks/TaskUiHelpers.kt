package com.checkit.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.TaskWorkspaceView
import com.checkit.ui.shortMonthName
import com.checkit.ui.shortName
import com.checkit.ui.tasks.views.currentTimeMinutes
import com.checkit.ui.theme.AppIconColorDefaults
import com.checkit.ui.theme.AppIconColorDefaults.FallbackColor
import com.checkit.ui.theme.toColor
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

internal fun TaskWorkspaceView.icon(): ImageVector = when (this) {
    TaskWorkspaceView.List -> Icons.AutoMirrored.Filled.ViewList
    TaskWorkspaceView.Agenda -> Icons.Default.ViewAgenda
    TaskWorkspaceView.Timeline -> Icons.Default.Schedule
}

@Composable
internal fun NoteIcon(status: TaskStatus) {
    Icon(
        imageVector = if (status == TaskStatus.Completed) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Notes,
        contentDescription = null,
        tint = if (status == TaskStatus.Completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
internal fun TaskIcon(completed: Boolean, color: Color) {
    Icon(
        imageVector = if (completed) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
        contentDescription = null,
        tint = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else color,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
internal fun DailyPlanIcon(source: DailyPlanItemSource, isDone: Boolean) {
    val icon = when (source) {
        DailyPlanItemSource.MyDayNote -> Icons.AutoMirrored.Filled.EventNote
        DailyPlanItemSource.MyDayReminder -> Icons.Default.Schedule
        else -> Icons.Default.EventAvailable
    }
    if (source == DailyPlanItemSource.MyDayNote) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    } else {
        BadgedActionIcon(baseIcon = icon, isDone = isDone)
    }
}

@Composable
fun BadgedActionIcon(
    baseIcon: ImageVector,
    isDone: Boolean,
    modifier: Modifier = Modifier,
    baseIconSize: Dp = 20.dp,
    badgeSize: Dp = 10.dp,
    baseIconTint: Color = MaterialTheme.colorScheme.onSurface,
    doneColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier.size(baseIconSize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = baseIcon,
            contentDescription = null,
            tint = baseIconTint,
            modifier = Modifier.size(baseIconSize)
        )

        Box(
            modifier = Modifier
                .size(badgeSize)
                .align(Alignment.BottomEnd)
                .offset(x = 1.dp, y = 0.dp)
                .clip(CircleShape)
                .background(if (isDone) doneColor else baseIconTint)
            ,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isDone) "Completed" else "Not Completed",
                tint = Color.White,
                modifier = Modifier.size(badgeSize * 0.7f)
            )
        }
    }
}

internal fun LocalDate.compact(): String {
    val today = today()
    val monthDay = "${shortMonthName()} $day"
    return when (this) {
        today -> "Today"
        today.plus(1, DateTimeUnit.DAY) -> "Tomorrow"
        today.plus(-1, DateTimeUnit.DAY) -> "Yesterday"
        today.plus(2, DateTimeUnit.DAY),
        today.plus(3, DateTimeUnit.DAY),
        today.plus(4, DateTimeUnit.DAY),
        today.plus(5, DateTimeUnit.DAY),
        today.plus(6, DateTimeUnit.DAY) -> dayOfWeek.shortName()
        else -> {
            if (year == today.year) monthDay else "$monthDay, $year"
        }
    }
}

fun TaskPriority.priorityColor(): Color = when (this) {
    TaskPriority.High -> Color(0xFFDC2626)
    TaskPriority.Medium -> Color(0xFFCA8A04)
    TaskPriority.Low -> Color(0xFF2563EB)
    TaskPriority.None -> Color(0xFF64748B)
}

fun NoteItem?.cardColor(): Color {
    return this?.tags?.firstOrNull()?.color?.toColor() ?: this?.objective?.color?.toColor() ?: FallbackColor
}

fun TaskItem?.cardColor(): Color {
    return this?.tags?.firstOrNull()?.color?.toColor() ?: this?.objective?.color?.toColor() ?: this?.priority?.priorityColor() ?: FallbackColor
}

fun DailyPlanItem.cardColor(): Color {
    return this.tags.firstOrNull()?.color?.toColor() ?: AppIconColorDefaults.DailyPlanCardColor
}

fun TaskItem.timeRangeLabel(): String {
    val start = startTimeMinutes?.toClockLabel() ?: "Any time"
    val end = endTimeMinutes?.toClockLabel()
    return if (end == null) start else "$start - $end"
}

fun TaskItem.isOverdue(): Boolean {
    return doDate.isOverdue(today(), endTimeMinutes, status == TaskStatus.Completed)
}

fun DailyPlanItem.isOverdue(date: LocalDate): Boolean {
    return date.isOverdue(today(), endTimeMinutes ?: startTimeMinutes, status == DailyPlanItemStatus.Done)
}

fun LocalDate?.isOverdue(today: LocalDate, deadline: Int?, isCompleted: Boolean): Boolean =
    when {
        isCompleted || this == null -> false
        this < today -> true
        this == today -> deadline != null && currentTimeMinutes() > deadline
        else -> false
    }

fun Int.toDurationLabel(compact: Boolean = false): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h${if(compact) "" else " "}${minutes}m"
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
