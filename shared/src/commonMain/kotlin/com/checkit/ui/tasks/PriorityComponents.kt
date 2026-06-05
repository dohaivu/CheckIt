package com.checkit.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskPriority

@Composable
internal fun PriorityPill(
    priority: TaskPriority,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val priorityColor = priority.priorityColor()
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) {
            priorityColor.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) priorityColor.copy(alpha = 0.62f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
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
}

@Composable
internal fun PriorityPickerRow(
    selected: TaskPriority,
    onSelect: (TaskPriority) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        EditablePriorities.forEach { priority ->
            PriorityPill(
                priority = priority,
                selected = selected == priority,
                onClick = {
                    onSelect(if (selected == priority) TaskPriority.None else priority)
                }
            )
        }
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
