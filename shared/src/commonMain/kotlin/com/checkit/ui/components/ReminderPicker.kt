package com.checkit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskReminderPreset
import com.checkit.ui.RepeatPreset
import com.checkit.ui.tasks.DetailChip

@Composable
internal fun ReminderPicker(
    reminderOffsets: Set<Int>,
    hasDate: Boolean,
    startTimeMinutes: Int?,
    onReminderToggle: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val presets = TaskReminderPreset.availableFor(startTimeMinutes)

    AppleStylePopup(
        isExpanded = expanded,
        onDismissRequest = { expanded = false },
        anchor = {
            val labels = reminderOffsets
                .sorted()
                .joinToString { TaskReminderPreset.labelFor(it, startTimeMinutes) }
            if (reminderOffsets.isEmpty()) {
                DetailChip(icon = Icons.Default.Notifications, label = "Off", iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),  onClick = { if (hasDate) expanded = true })
            } else {
                DetailChip(icon = Icons.Default.NotificationsActive, label = labels, onClick = { expanded = true })
            }
        }
    ) {
        Column(
            modifier = Modifier
                .width(250.dp)
                .padding(vertical = 0.dp)
        ) {
            presets.forEachIndexed { index, preset ->
                ReminderOptionRow(
                    label = TaskReminderPreset.labelFor(preset.offsetMinutes, startTimeMinutes),
                    selected = preset.offsetMinutes in reminderOffsets,
                    onClick = {
                        onReminderToggle(preset.offsetMinutes)
                        expanded = false
                    }
                )

                // Draw standard iOS dividers between rows (excluding last element)
                if (index < RepeatPreset.entries.size - 1) {
                    HorizontalDivider(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}