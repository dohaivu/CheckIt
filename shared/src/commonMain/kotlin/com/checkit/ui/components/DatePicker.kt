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
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
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
import com.checkit.ui.tasks.toDurationLabel
import com.checkit.ui.tasks.toClockLabel
import com.checkit.ui.toUtcLocalDate
import com.checkit.ui.toUtcStartMillis
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePicker(
    modifier: Modifier = Modifier,
    date: LocalDate?,
    startTimeMinutes: Int?,
    endTimeMinutes: Int?,
    durationMinutes: Int?,
    onDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: ((Int?) -> Unit),
    onEndTimeChange: ((Int?) -> Unit)?,
    enabled: Boolean = true,
    isOverdue: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(startTimeMinutes) }
    var endTime by remember { mutableStateOf(validTimeRangeEnd(startTimeMinutes, endTimeMinutes)) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailChip(
            icon = Icons.Default.Event,
            label = if (date == null) "No date" else dateTimeRangeDetailLabel(date, startTime, endTime),
            isHighlighted = isOverdue,
            onClick = {
                if (enabled) showPicker = true
            }
        )
    }

    if (enabled && showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date?.toUtcStartMillis())
        fun dismiss() {
            showPicker = false
            startTime = startTimeMinutes
            endTime = validTimeRangeEnd(startTimeMinutes, endTimeMinutes)
        }
        AlertDialog(
            onDismissRequest = { dismiss() },
            confirmButton = {
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = {
                        onDateChange(null)
                        startTime = null
                        endTime = null
                        onStartTimeChange.invoke(startTime)
                        onEndTimeChange?.invoke(endTime)

                        showPicker = false
                    }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { dismiss() }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onDateChange(datePickerState.selectedDateMillis?.toUtcLocalDate())
                            onStartTimeChange.invoke(startTime)
                            onEndTimeChange?.invoke(endTime)

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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DatePicker(
                        state = datePickerState,
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimePicker(
                            label = "Start",
                            timeMinutes = startTime,
                            initialTimeMinutes = currentTimeMinutes(),
                            onTimeChange = { value ->
                                startTime = value
                                endTime = validTimeRangeEnd(value, endTime)
                            },
                            enabled = enabled
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
                                timeMinutes = endTime,
                                initialTimeMinutes = ((startTime ?: currentTimeMinutes()) + 60).coerceAtMost(MinutesPerDay - 1),
                                enabled = enabled && startTime != null,
                                onTimeChange = {
                                    endTime = validTimeRangeEnd(startTime, it)
                                },
                            )
                        }
                    }
                    if (enabled && onEndTimeChange != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            PickerShortcutRow {
                                TimeRangeShortcutDurations.forEach { duration ->
                                    PickerShortcut(
                                        text = duration.shortcutDurationLabel(),
                                        onClick = {
                                            val start = startTime ?: currentTimeMinutes()
                                            startTime = start
                                            endTime = (start + duration).coerceAtMost(MinutesPerDay - 1)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateRangePicker(
    modifier: Modifier = Modifier,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onRangeChange: (LocalDate?, LocalDate?) -> Unit,
    enabled: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val label = when {
            startDate == null && endDate == null -> "No date range"
            startDate != null && endDate != null -> "${startDate} - ${endDate}"
            startDate != null -> "${startDate} - ..."
            else -> "... - ${endDate}"
        }
        DetailChip(
            icon = Icons.Default.Event,
            label = label,
            onClick = {
                if (enabled) showPicker = true
            }
        )
    }

    if (enabled && showPicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate?.toUtcStartMillis(),
            initialSelectedEndDateMillis = endDate?.toUtcStartMillis()
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onRangeChange(null, null)
                        showPicker = false
                    }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showPicker = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onRangeChange(
                                state.selectedStartDateMillis?.toUtcLocalDate(),
                                state.selectedEndDateMillis?.toUtcLocalDate()
                            )
                            showPicker = false
                        }
                    ) {
                        Text("OK")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            text = {
                Box(modifier = Modifier.height(400.dp)) {
                    DateRangePicker(
                        state = state,
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    )
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
    modifier: Modifier = Modifier
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
    onStartTimeChange: ((Int?) -> Unit),
    onEndTimeChange: ((Int?) -> Unit)?,
    enabled: Boolean = true,
    isOverdue: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var showPicker by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(startTimeMinutes) }
    var endTime by remember { mutableStateOf(validTimeRangeEnd(startTimeMinutes, endTimeMinutes)) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailChip(
            icon = Icons.Default.Schedule,
            label = if (startTime == null) "No time" else timeRangeDetailLabel(startTime, endTime),
            isHighlighted = isOverdue,
            onClick = {
                if (enabled) showPicker = true
            }
        )
    }

    if (enabled && showPicker) {
        fun dismiss() {
            showPicker = false
            startTime = startTimeMinutes
            endTime = validTimeRangeEnd(startTimeMinutes, endTimeMinutes)
        }
        AlertDialog(
            onDismissRequest = { dismiss() },
            confirmButton = {
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = {
                        startTime = null
                        endTime = null
                        onStartTimeChange.invoke(startTime)
                        onEndTimeChange?.invoke(endTime)

                        showPicker = false
                    }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { dismiss() }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onStartTimeChange.invoke(startTime)
                            onEndTimeChange?.invoke(endTime)

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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimePicker(
                            label = "Start",
                            timeMinutes = startTime,
                            initialTimeMinutes = currentTimeMinutes(),
                            onTimeChange = { value ->
                                startTime = value
                                endTime = validTimeRangeEnd(value, endTime)
                            },
                            enabled = enabled
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
                                timeMinutes = endTime,
                                initialTimeMinutes = ((startTime ?: currentTimeMinutes()) + 60).coerceAtMost(MinutesPerDay - 1),
                                enabled = enabled && startTime != null,
                                onTimeChange = {
                                    endTime = validTimeRangeEnd(startTime, it)
                                },
                            )
                        }
                    }
                    if (enabled && onEndTimeChange != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            PickerShortcutRow {
                                TimeRangeShortcutDurations.forEach { duration ->
                                    PickerShortcut(
                                        text = duration.shortcutDurationLabel(),
                                        onClick = {
                                            val start = startTime ?: currentTimeMinutes()
                                            startTime = start
                                            endTime = (start + duration).coerceAtMost(MinutesPerDay - 1)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
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
        text = duration.toDurationLabel(),
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

internal fun validTimeRangeEnd(startTime: Int?, endTime: Int?): Int? =
    when {
        startTime == null -> null
        endTime == null -> null
        startTime > endTime -> null
        else -> endTime
    }

internal const val HoursPerDay = 24
internal const val MinutesPerDay = 24 * 60
internal val TimeRangeShortcutDurations = listOf(30, 60, 120)
