package com.checkit.ui.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
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
import com.checkit.ui.shortName
import com.checkit.ui.shortMonthName
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus

internal fun TaskWorkspaceView.icon(): ImageVector = when (this) {
    TaskWorkspaceView.List -> Icons.Default.ViewList
    TaskWorkspaceView.Agenda -> Icons.Default.ViewAgenda
    TaskWorkspaceView.Timeline -> Icons.Default.Schedule
}

internal fun materialIcon(name: String): ImageVector = when (name) {
    "AllInclusive" -> Icons.Default.AllInclusive
    "AttachMoney" -> Icons.Default.AttachMoney
    "Bookmark" -> Icons.Default.Bookmark
    "Code" -> Icons.Default.Code
    "Delete" -> Icons.Default.Delete
    "Email" -> Icons.Default.Email
    "Favorite" -> Icons.Default.Favorite
    "FitnessCenter" -> Icons.Default.FitnessCenter
    "Flag" -> Icons.Default.Flag
    "Flight" -> Icons.Default.Flight
    "Folder" -> Icons.Default.Folder
    "Home" -> Icons.Default.Home
    "Inbox" -> Icons.Default.Inbox
    "Lightbulb" -> Icons.Default.Lightbulb
    "LocalOffer" -> Icons.Default.LocalOffer
    "MusicNote" -> Icons.Default.MusicNote
    "Notes" -> Icons.Default.Notes
    "Person" -> Icons.Default.Person
    "Pets" -> Icons.Default.Pets
    "PriorityHigh" -> Icons.Default.PriorityHigh
    "Restaurant" -> Icons.Default.Restaurant
    "Schedule" -> Icons.Default.Schedule
    "School" -> Icons.Default.School
    "ShoppingCart" -> Icons.Default.ShoppingCart
    "Star" -> Icons.Default.Star
    "TaskAlt" -> Icons.Default.TaskAlt
    "Today" -> Icons.Default.Today
    "Work" -> Icons.Default.Work
    else -> Icons.Default.LocalOffer
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

internal fun LocalDate.compact(): String {
    val today = today()
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
            val monthDay = "${shortMonthName()} $day"
            if (year == today.year) monthDay else "$monthDay, $year"
        }
    }
}

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
