package com.checkit.ui.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.ui.TaskWorkspaceView
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

internal fun TaskWorkspaceView.icon(): ImageVector = when (this) {
    TaskWorkspaceView.List -> Icons.Default.ViewList
    TaskWorkspaceView.Agenda -> Icons.Default.ViewAgenda
    TaskWorkspaceView.Timeline -> Icons.Default.Schedule
}

internal fun materialIcon(name: String): ImageVector = when (name) {
    "Delete" -> Icons.Default.Delete
    "Home" -> Icons.Default.Home
    "Inbox" -> Icons.Default.Inbox
    "Notes" -> Icons.Default.Notes
    "PriorityHigh" -> Icons.Default.PriorityHigh
    "Schedule" -> Icons.Default.Schedule
    "TaskAlt" -> Icons.Default.TaskAlt
    "Today" -> Icons.Default.Today
    "Work" -> Icons.Default.Work
    else -> Icons.Default.LocalOffer
}

internal fun TaskPriority.color(): Color = when (this) {
    TaskPriority.None -> Color(0xFF64748B)
    TaskPriority.Low -> Color(0xFF0891B2)
    TaskPriority.Medium -> Color(0xFFCA8A04)
    TaskPriority.High -> Color(0xFFDC2626)
    TaskPriority.Urgent -> Color(0xFF9333EA)
}

internal fun String.toColor(): Color =
    removePrefix("#")
        .toIntOrNull(16)
        ?.let { rgb ->
            Color(
                red = ((rgb shr 16) and 0xFF) / 255f,
                green = ((rgb shr 8) and 0xFF) / 255f,
                blue = (rgb and 0xFF) / 255f
            )
        }
        ?: Color(0xFF64748B)

internal fun LocalDate.compact(): String = "${month.number}/$day/$year"

internal fun TaskItem.timeRangeLabel(): String {
    val start = startTimeMinutes?.toClockLabel() ?: "Any time"
    val end = endTimeMinutes?.toClockLabel()
    return if (end == null) start else "$start - $end"
}

internal fun Int.formatDuration(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

internal fun Int.toClockLabel(): String {
    val hour = this / 60
    val minute = this % 60
    val suffix = if (hour >= 12) "PM" else "AM"
    val displayHour = when (val normalized = hour % 12) {
        0 -> 12
        else -> normalized
    }
    return "$displayHour:${minute.toString().padStart(2, '0')} $suffix"
}
