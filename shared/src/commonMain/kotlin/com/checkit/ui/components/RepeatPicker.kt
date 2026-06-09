package com.checkit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.checkit.ui.RepeatPreset
import com.checkit.ui.tasks.DetailChip

@Composable
fun RepeatPicker(
    selected: RepeatPreset,
    onSelect: (RepeatPreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(selected) }

    AppleStylePopup(
        isExpanded = expanded,
        onDismissRequest = { expanded = false },
        anchor = {
            if (selectedPreset == RepeatPreset.None) {
                DetailChip(icon = Icons.Default.Repeat, label = "None", iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f), onClick = { expanded = true })
            } else {
                DetailChip(icon = Icons.Default.Repeat, label = selectedPreset.label, onClick = { expanded = true })
            }
        }
    ) {
        Column(
            modifier = Modifier
                .width(150.dp)
                .padding(vertical = 0.dp)
        ) {
            RepeatPreset.entries.forEachIndexed { index, preset ->
                Row(
                    modifier = Modifier
                        .clickable(onClick = {
                            selectedPreset = preset
                            onSelect(preset)
                            expanded = false
                        })
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

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
internal fun RepeatPill(
    repeatRRule: String?
) {
    val label = repeatRRule.repeatLabel() ?: return
    DetailChip(Icons.Default.MoreTime, label)
}

internal fun String?.repeatLabel(): String? {
    val rrule = this ?: return null
    val preset = RepeatPreset.fromRRule(rrule)
    if (preset != RepeatPreset.None) return preset.label

    val frequency = rrule
        .split(";")
        .firstOrNull { it.startsWith("FREQ=") }
        ?.substringAfter("=")

    return when (frequency) {
        "DAILY" -> "Everyday"
        "WEEKLY" -> "Weekly"
        "MONTHLY" -> "Monthly"
        "YEARLY" -> "Yearly"
        else -> "Repeats"
    }
}
