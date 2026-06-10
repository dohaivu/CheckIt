package com.checkit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.checkit.ui.tasks.ContentContainerAlpha
import com.checkit.ui.tasks.currentTimeMinutes
import com.checkit.ui.tasks.formatDuration
import com.checkit.ui.tasks.toClockLabel
import com.checkit.ui.toUtcLocalDate
import com.checkit.ui.toUtcStartMillis
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerRow(
    modifier: Modifier = Modifier,
    date: LocalDate?,
    startTimeMinutes: Int?,
    endTimeMinutes: Int?,
    durationMinutes: Int?,
    onDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: ((Int?) -> Unit)?,
    onEndTimeChange: ((Int?) -> Unit)?,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailChip(
            icon = Icons.Default.Event,
            label = if (date == null) "No date" else dateTimeRangeDetailLabel(date, startTimeMinutes, endTimeMinutes),
            onClick = {
                showPicker = true
            }
        )
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date?.toUtcStartMillis())

        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.Start // Forces left alignment
                ) {
                    TextButton(onClick = {
                        onDateChange(null)
                        onStartTimeChange?.invoke(null)
                        onEndTimeChange?.invoke(null)

                        showPicker = false
                    }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { showPicker = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onDateChange(datePickerState.selectedDateMillis?.toUtcLocalDate())
                            showPicker = false
                        }
                    ) {
                        Text("OK")
                    }
                }

            },
            dismissButton = null,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DatePicker(
                        state = datePickerState,
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    )
                    if (onStartTimeChange != null) {
                        TimeRangePicker(
                            startTimeMinutes = startTimeMinutes,
                            endTimeMinutes = endTimeMinutes,
                            durationMinutes = durationMinutes,
                            onStartTimeChange = onStartTimeChange,
                            onEndTimeChange = onEndTimeChange
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePicker(
    label: String,
    timeMinutes: Int?,
    initialTimeMinutes: Int,
    onTimeChange: (Int?) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.Companion
) {
    var showPicker by remember { mutableStateOf(false) }
    DetailChip(
        icon = Icons.Default.Schedule,
        label = timeMinutes?.toClockLabel() ?: "No $label time",
        onClick = {
            showPicker = true
        }
    )
    if (showPicker && enabled) {
        val initial = timeMinutes ?: initialTimeMinutes.coerceIn(0, MinutesPerDay - 1)
        val timePickerState = rememberTimePickerState(
            initialHour = initial / 60,
            initialMinute = initial % 60,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(timePickerState.hour * 60 + timePickerState.minute)
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            },
            text = { TimePicker(state = timePickerState, modifier = Modifier.scale(0.8f)) }
        )
    }
}

@Composable
internal fun TimeRangePicker(
    startTimeMinutes: Int?,
    endTimeMinutes: Int?,
    durationMinutes: Int?,
    onStartTimeChange: ((Int?) -> Unit)?,
    onEndTimeChange: ((Int?) -> Unit)?,
    modifier: Modifier = Modifier.Companion
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimePicker(
                label = "Start",
                timeMinutes = startTimeMinutes,
                initialTimeMinutes = currentTimeMinutes(),
                onTimeChange = { value ->
                    onStartTimeChange?.invoke(value)
                    if (value == null && endTimeMinutes != null) onEndTimeChange?.invoke(null)
                },
                modifier = Modifier.weight(1f)
            )
            if (onEndTimeChange != null) {
                durationMinutes?.let { duration ->
                    DurationText(
                        duration = duration,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                TimePicker(
                    label = "End",
                    timeMinutes = endTimeMinutes,
                    initialTimeMinutes = ((startTimeMinutes ?: currentTimeMinutes()) + 60).coerceAtMost(MinutesPerDay - 1),
                    enabled = startTimeMinutes != null,
                    onTimeChange = {
                        onEndTimeChange.invoke(it)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (onEndTimeChange != null) {
            Row {
                PickerShortcutRow(modifier = Modifier.weight(1f)) {
                    TimeRangeShortcutDurations.forEach { duration ->
                        PickerShortcut(
                            text = duration.shortcutDurationLabel(),
                            onClick = {
                                val start = startTimeMinutes ?: currentTimeMinutes()
                                onStartTimeChange?.invoke(start)
                                onEndTimeChange.invoke((start + duration).coerceAtMost(MinutesPerDay - 1))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerShortcutRow(modifier: Modifier = Modifier.Companion, content: @Composable () -> Unit) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun PickerShortcut(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        },
        contentColor = if (enabled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ContentContainerAlpha)
        },
        modifier = Modifier
            .height(30.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.Companion)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun DurationText(
    duration: Int,
    modifier: Modifier = Modifier.Companion
) {
    Text(
        text = duration.formatDuration(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

internal fun Int.shortcutDurationLabel(): String =
    when {
        this < 60 -> "${this}m"
        this % 60 == 0 -> "${this / 60}h"
        else -> "${this / 60}h ${this % 60}m"
    }


internal const val HoursPerDay = 24
internal const val MinutesPerDay = 24 * 60
internal val TimeRangeShortcutDurations = listOf(30, 60, 120)