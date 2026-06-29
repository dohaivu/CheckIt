package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.ui.tasks.compact
import com.checkit.ui.tasks.toClockLabel
import kotlinx.datetime.LocalDate

@Composable
internal fun DetailChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.Unspecified,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .background(if (isHighlighted) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else backgroundColor)
            .border(1.dp, if (isHighlighted) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else borderColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isHighlighted) MaterialTheme.colorScheme.error else if (iconTint != Color.Unspecified) iconTint else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = if (isHighlighted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun TimeRangeDetailChip(
    startTimeMinutes: Int?,
    endTimeMinutes: Int?
) {
    if (startTimeMinutes == null && endTimeMinutes == null) return
    DetailChip(
        icon = Icons.Default.Schedule,
        label = timeRangeDetailLabel(startTimeMinutes, endTimeMinutes)
    )
}

@Composable
internal fun DateTimeRangeDetailChip(
    date: LocalDate?,
    startTimeMinutes: Int?,
    endTimeMinutes: Int?,
    isOverdue: Boolean = false
) {
    if (date == null && startTimeMinutes == null && endTimeMinutes == null) return
    DetailChip(
        icon = if (date == null) Icons.Default.Schedule else Icons.Default.Event,
        label = dateTimeRangeDetailLabel(date, startTimeMinutes, endTimeMinutes),
        isHighlighted = isOverdue
    )
}

internal fun dateTimeRangeDetailLabel(
    date: LocalDate?,
    startTimeMinutes: Int?,
    endTimeMinutes: Int?
): String {
    val timeLabel = timeRangeDetailLabel(startTimeMinutes, endTimeMinutes).takeIf { it.isNotBlank() }
    val dateLabel = date?.compact()
    return when {
        dateLabel != null && timeLabel != null -> "$dateLabel $timeLabel"
        dateLabel != null -> dateLabel
        timeLabel != null -> timeLabel
        else -> ""
    }
}

internal fun timeRangeDetailLabel(
    startTimeMinutes: Int?,
    endTimeMinutes: Int?
): String = when {
    startTimeMinutes != null && endTimeMinutes != null -> {
        "${startTimeMinutes.toClockLabel()} - ${endTimeMinutes.toClockLabel()}"
    }
    startTimeMinutes != null -> startTimeMinutes.toClockLabel()
    endTimeMinutes != null -> "Until ${endTimeMinutes.toClockLabel()}"
    else -> ""
}
