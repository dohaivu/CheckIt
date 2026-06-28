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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.ui.MinutesPerDay
import com.checkit.ui.TimeRangeShortcutDurations
import com.checkit.ui.duration
import com.checkit.ui.shortcutDurationLabel
import com.checkit.ui.tasks.toClockLabel
import com.checkit.ui.tasks.toDurationLabel
import com.checkit.ui.tasks.views.ContentContainerAlpha
import com.checkit.ui.tasks.views.currentTimeMinutes
import com.checkit.ui.toUtcLocalDate
import com.checkit.ui.toUtcStartMillis
import com.checkit.ui.validTimeRangeEnd
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePicker(
    modifier: Modifier = Modifier,
    date: LocalDate?,
    startTimeMinutes: Int?,
    endTimeMinutes: Int?,
    onDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: ((Int?) -> Unit),
    onEndTimeChange: ((Int?) -> Unit)?,
    enabled: Boolean = true,
    isOverdue: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(startTimeMinutes) }
    var endTime by remember { mutableStateOf(validTimeRangeEnd(startTimeMinutes, endTimeMinutes)) }
    val durationMinutes by remember(startTime, endTime) { derivedStateOf { duration(startTime, endTime) } }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailChip(
            icon = Icons.Default.Event,
            label = if (date == null) "No date" else dateTimeRangeDetailLabel(date, startTimeMinutes, endTimeMinutes),
            isHighlighted = isOverdue,
            onClick = {
                if (enabled) showPicker = true
            }
        )
    }

    if (enabled && showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date?.toUtcStartMillis())
        
        fun resetLocalState() {
            startTime = startTimeMinutes
            endTime = validTimeRangeEnd(startTimeMinutes, endTimeMinutes)
        }

        AppPickerDialog(
            onDismissRequest = {
                showPicker = false
                resetLocalState()
            },
            onClear = {
                onDateChange(null)
                onStartTimeChange(null)
                onEndTimeChange?.invoke(null)
                showPicker = false
            },
            onConfirm = {
                val nextDate = datePickerState.selectedDateMillis?.toUtcLocalDate()
                if (nextDate != date) onDateChange(nextDate)
                if (startTime != startTimeMinutes) onStartTimeChange.invoke(startTime)
                if (endTime != validTimeRangeEnd(startTimeMinutes, endTimeMinutes)) onEndTimeChange?.invoke(endTime)
                showPicker = false
            }
        ) {
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

                TimeRangeSelectionRow(
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    onStartTimeChange = { value ->
                        startTime = value
                        endTime = validTimeRangeEnd(value, endTime)
                    },
                    onEndTimeChange = if (onEndTimeChange != null) {
                        { endTime = validTimeRangeEnd(startTime, it) }
                    } else null,
                    enabled = enabled
                )
            }
        }
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
        AppPickerDialog(
            onDismissRequest = { showPicker = false },
            onClear = {
                onRangeChange(null, null)
                showPicker = false
            },
            onConfirm = {
                val nextStart = state.selectedStartDateMillis?.toUtcLocalDate()
                val nextEnd = state.selectedEndDateMillis?.toUtcLocalDate()
                if (nextStart != startDate || nextEnd != endDate) {
                    onRangeChange(nextStart, nextEnd)
                }
                showPicker = false
            }
        ) {
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
            if (enabled) showPicker = true
        }
    )
    if (showPicker && enabled) {
        val initial = timeMinutes ?: initialTimeMinutes.coerceIn(0, MinutesPerDay - 1)
        val timePickerState = rememberTimePickerState(
            initialHour = initial / 60,
            initialMinute = initial % 60,
            is24Hour = false
        )
        AppPickerDialog(
            onDismissRequest = {
                showPicker = false
            },
            onClear = {
                onTimeChange.invoke(null)
                showPicker = false
            },
            onConfirm = {
                val nextTime = timePickerState.hour * 60 + timePickerState.minute
                if (nextTime != timeMinutes) onTimeChange(nextTime)
                showPicker = false
            }
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

@Composable
internal fun TimeRangePicker(
    startTimeMinutes: Int?,
    endTimeMinutes: Int?,
    onStartTimeChange: ((Int?) -> Unit),
    onEndTimeChange: ((Int?) -> Unit)?,
    enabled: Boolean = true,
    isOverdue: Boolean = false,
    clearEnabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var showPicker by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(startTimeMinutes) }
    var endTime by remember { mutableStateOf(validTimeRangeEnd(startTimeMinutes, endTimeMinutes)) }
    val durationMinutes by remember(startTime, endTime) { derivedStateOf { duration(startTime, endTime) } }

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
        AppPickerDialog(
            onDismissRequest = {
                showPicker = false
                startTime = startTimeMinutes
                endTime = validTimeRangeEnd(startTimeMinutes, endTimeMinutes)
            },
            onClear = if (clearEnabled) {
                        {
                            onStartTimeChange(null)
                            onEndTimeChange?.invoke(null)
                            showPicker = false
                        }
                    } else null,
            onConfirm = {
                if (startTime != startTimeMinutes) onStartTimeChange.invoke(startTime)
                if (endTime != validTimeRangeEnd(startTimeMinutes, endTimeMinutes)) onEndTimeChange?.invoke(endTime)
                showPicker = false
            }
        ) {
            TimeRangeSelectionRow(
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                onStartTimeChange = { value ->
                    startTime = value
                    endTime = validTimeRangeEnd(value, endTime)
                },
                onEndTimeChange = if (onEndTimeChange != null) {
                    { endTime = validTimeRangeEnd(startTime, it) }
                } else null,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun AppPickerDialog(
    onDismissRequest: () -> Unit,
    onClear: (() -> Unit)? = null,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onClear != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onConfirm) {
                    Text("OK")
                }
            }
        },
        text = content
    )
}

@Composable
private fun TimeRangeSelectionRow(
    startTime: Int?,
    endTime: Int?,
    durationMinutes: Int?,
    onStartTimeChange: (Int?) -> Unit,
    onEndTimeChange: ((Int?) -> Unit)?,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimePicker(
                label = "Start",
                timeMinutes = startTime,
                initialTimeMinutes = currentTimeMinutes(),
                onTimeChange = onStartTimeChange,
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
                    onTimeChange = onEndTimeChange,
                )
            }
        }
        if (enabled && onEndTimeChange != null) {
            PickerShortcutRow {
                TimeRangeShortcutDurations.forEach { duration ->
                    PickerShortcut(
                        text = duration.shortcutDurationLabel(),
                        onClick = {
                            val start = startTime ?: currentTimeMinutes()
                            onStartTimeChange(start)
                            onEndTimeChange((start + duration).coerceAtMost(MinutesPerDay - 1))
                        }
                    )
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
        text = duration.toDurationLabel(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
