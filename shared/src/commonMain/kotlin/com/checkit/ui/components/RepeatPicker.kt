package com.checkit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.checkit.domain.RepeatFrequency
import com.checkit.domain.RepeatRule
import com.checkit.domain.label

/**
 * Unified repeat picker shared by tasks and routines: the familiar
 * Does-not-repeat / Everyday / Weekly / Monthly choices, with the weekday grid
 * expanding in place when Weekly is selected.
 *
 * [anchorDayOfMonth] seeds the day a new Monthly rule fires on (the task's due
 * date, when it has one); it is ignored for every other frequency.
 */
@Composable
fun RepeatPicker(
    selected: RepeatRule,
    onRepeatChange: (RepeatRule) -> Unit,
    modifier: Modifier = Modifier,
    anchorDayOfMonth: Int? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    AppleStylePopup(
        isExpanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = modifier,
        anchor = {
            if (!selected.isActive) {
                DetailChip(
                    icon = Icons.Default.Repeat,
                    label = "None",
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
                    onClick = { if (enabled) expanded = true }
                )
            } else {
                DetailChip(
                    icon = Icons.Default.Repeat,
                    label = selected.label(),
                    onClick = { if (enabled) expanded = true }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .width(260.dp)
                .padding(vertical = 0.dp)
        ) {
            RepeatOptionRow(
                label = "Does not repeat",
                selected = selected.frequency == RepeatFrequency.None,
                onClick = { onRepeatChange(RepeatRule.None) }
            )
            RepeatDivider()
            RepeatOptionRow(
                label = "Everyday",
                selected = selected.frequency == RepeatFrequency.Daily,
                onClick = { onRepeatChange(RepeatRule.daily()) }
            )
            RepeatDivider()
            RepeatOptionRow(
                label = "Weekly",
                selected = selected.frequency == RepeatFrequency.Weekly,
                onClick = { onRepeatChange(RepeatRule.weekly(selected.weekdays)) }
            )
            if (selected.frequency == RepeatFrequency.Weekly) {
                RepeatDivider()
                WeekdayPicker(
                    selected = selected.weekdays,
                    onChange = { onRepeatChange(RepeatRule.weekly(it)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    showPausedHint = false
                )
            }
            RepeatDivider()
            RepeatOptionRow(
                label = "Monthly",
                selected = selected.frequency == RepeatFrequency.Monthly,
                onClick = {
                    onRepeatChange(RepeatRule.monthly(anchorDayOfMonth ?: selected.dayOfMonth ?: 1))
                }
            )
        }
    }
}

@Composable
private fun RepeatOptionRow(
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RepeatDivider() {
    HorizontalDivider(
        color = Color.LightGray.copy(alpha = 0.4f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
