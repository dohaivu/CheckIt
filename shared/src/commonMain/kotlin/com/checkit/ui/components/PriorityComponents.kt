package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskPriority
import com.checkit.ui.tasks.ContentAlpha
import com.checkit.ui.tasks.ContentContainerAlpha

@Composable
internal fun PriorityPicker(
    selected: TaskPriority,
    onSelect: (TaskPriority) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    AppleStylePopup(
        isExpanded = expanded,
        onDismissRequest = { expanded = false },
        anchor = {
            PriorityPill(
                priority = selected,
                selected = false,
                onClick = {
                    if (enabled) expanded = true
                }
            )
        }
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EditablePriorities.forEach { priority ->
                PriorityPill(
                    priority = priority,
                    selected = selected == priority,
                    onClick = {
                        onSelect(if (selected == priority) TaskPriority.None else priority)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
internal fun PriorityPill(
    priority: TaskPriority,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val priorityColor = priority.priorityColor()
    val backgroundColor = if (selected) priorityColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = modifier
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(backgroundColor)
            .border(1.dp, if (selected) priorityColor.copy(alpha = ContentAlpha) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = ContentContainerAlpha), CircleShape)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = null,
            tint = priorityColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = priority.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

internal fun TaskPriority.priorityColor(): Color = when (this) {
    TaskPriority.High -> Color(0xFFDC2626)
    TaskPriority.Medium -> Color(0xFFCA8A04)
    TaskPriority.Low -> Color(0xFF2563EB)
    TaskPriority.None -> Color(0xFF64748B)
}

private val EditablePriorities = listOf(
    TaskPriority.High,
    TaskPriority.Medium,
    TaskPriority.Low
)
