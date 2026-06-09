package com.checkit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
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
            Row(
                modifier = Modifier
                    .clickable { expanded = true },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedPreset == RepeatPreset.None) {
                    DetailChip(icon = Icons.Default.Repeat, label = "None", iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f))
                } else {
                    DetailChip(icon = Icons.Default.Repeat, label = selectedPreset.label)
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 150.dp, max = 280.dp)
                .padding(vertical = 4.dp)
        ) {
            RepeatPreset.entries.forEachIndexed { index, preset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = {
                            selectedPreset = preset
                            onSelect(preset)
                            expanded = false
                        })
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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