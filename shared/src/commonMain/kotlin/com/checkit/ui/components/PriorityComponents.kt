package com.checkit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskPriority
import com.checkit.ui.tasks.priorityColor
import com.checkit.ui.tasks.views.ContentAlpha
import com.checkit.ui.tasks.views.ContentContainerAlpha

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
                selected = true,
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
    DetailChip(
        icon = Icons.Default.Flag,
        label = priority.name,
        modifier = modifier,
        iconTint = priorityColor,
        backgroundColor = if (selected) priorityColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        borderColor = if (selected) priorityColor.copy(alpha = ContentAlpha) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = ContentContainerAlpha),
        onClick = onClick
    )
}

private val EditablePriorities = listOf(
    TaskPriority.High,
    TaskPriority.Medium,
    TaskPriority.Low
)
