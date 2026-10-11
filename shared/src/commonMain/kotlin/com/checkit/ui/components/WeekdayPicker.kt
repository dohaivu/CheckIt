package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.AllWeekdays
import com.checkit.domain.WeekdayPresetWeekdays
import com.checkit.domain.WeekdayPresetWeekends
import com.checkit.domain.shortLabel
import kotlinx.datetime.DayOfWeek

/**
 * Weekday multi-select with Everyday/Weekdays/Weekends presets.
 * Shared by the task repeat picker and the routine editor.
 *
 * An empty selection means "off": for tasks it maps to
 * [RepeatRule.None][com.checkit.domain.RepeatRule.None] and for routines to a
 * paused routine, so the save button stays enabled either way.
 */
@Composable
internal fun WeekdayPicker(
    selected: Set<DayOfWeek>,
    onChange: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier,
    showPausedHint: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WeekdayPresetChip(
                label = "Everyday",
                isSelected = selected == AllWeekdays,
                onClick = { onChange(AllWeekdays) },
                modifier = Modifier.weight(1f)
            )
            WeekdayPresetChip(
                label = "Weekdays",
                isSelected = selected == WeekdayPresetWeekdays,
                onClick = { onChange(WeekdayPresetWeekdays) },
                modifier = Modifier.weight(1f)
            )
            WeekdayPresetChip(
                label = "Weekends",
                isSelected = selected == WeekdayPresetWeekends,
                onClick = { onChange(WeekdayPresetWeekends) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
                .widthIn(max = MaxDaysRowWidth),
            horizontalArrangement = Arrangement.spacedBy(DayCircleSpacing)
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = day in selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = DayCircleSize)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                        .clickable {
                            onChange(
                                if (isSelected) selected - day else selected + day
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.shortLabel(),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
        if (selected.isEmpty() && showPausedHint) {
            Text(
                text = "Off — no days selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeekdayPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/** Diameter of a single day circle at the widest the row is allowed to be. */
private val DayCircleSize = 34.dp

/** Gap between adjacent day circles. */
private val DayCircleSpacing = 4.dp

/** Widest the day row may get: seven circles at [DayCircleSize] plus their gaps. */
private val MaxDaysRowWidth = DayCircleSize * 7 + DayCircleSpacing * 6
