package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.MetricItem
import com.checkit.domain.MetricUnit
import com.checkit.ui.countdownLabel
import com.checkit.ui.displayName
import com.checkit.ui.dueLocalDate
import com.checkit.ui.isDateBased
import com.checkit.ui.isMetricOverdue
import com.checkit.ui.progressRatio
import com.checkit.ui.tasks.views.ContentAlpha
import com.checkit.ui.today
import kotlin.math.roundToInt

/** Add/edit/delete custom metrics (name/value/target/unit), shared by goal and task editors. */
@Composable
internal fun MetricsSection(
    metrics: List<MetricItem>,
    enabled: Boolean,
    onMetricsChange: (List<MetricItem>) -> Unit,
    modifier: Modifier = Modifier,
    emptyHint: String? = null
) {
    var unitExpandedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CUSTOM METRICS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = {
                    if (!enabled) return@TextButton
                    onMetricsChange(
                        metrics + MetricItem(
                            name = "",
                            value = "",
                            sortOrder = metrics.size
                        )
                    )
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("Add", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (metrics.isEmpty() && emptyHint != null) {
            Text(
                text = emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        metrics.forEachIndexed { index, metric ->
            fun update(next: MetricItem) {
                onMetricsChange(metrics.toMutableList().also { it[index] = next })
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (metric.isCompleted) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                        contentDescription = if (metric.isCompleted) "Mark incomplete" else "Mark complete",
                        tint = if (metric.isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                        },
                        modifier = Modifier
                            .size(22.dp)
                            .then(if (enabled) Modifier.clickable { update(metric.copy(isCompleted = !metric.isCompleted)) } else Modifier)
                    )
                    CompactFlatTextField(
                        value = metric.name,
                        onValueChange = { value ->
                            update(metric.copy(name = value))
                        },
                        placeholder = "Metric name",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onMetricsChange(metrics.filterIndexed { metricIndex, _ -> metricIndex != index }) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete metric",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ContentAlpha),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (metric.isDateBased()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (metric.unit) {
                            MetricUnit.Countdown -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(2f)
                                ) {
                                    DatePicker(
                                        date = metric.dueLocalDate(),
                                        onDateChange = { date ->
                                            update(
                                                metric.copy(
                                                    dueDateEpochDays = date?.toEpochDays()?.toInt()
                                                )
                                            )
                                        },
                                        enabled = enabled,
                                        isOverdue = metric.isMetricOverdue(),
                                    )
                                    Text(
                                        text = metric.countdownLabel(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (metric.isMetricOverdue()) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            MetricUnit.DueDate -> {
                                DatePicker(
                                    date = metric.dueLocalDate(),
                                    startTimeMinutes = metric.dueTimeMinutes,
                                    endTimeMinutes = null,
                                    onDateChange = { date ->
                                        update(
                                            metric.copy(
                                                dueDateEpochDays = date?.toEpochDays()?.toInt()
                                            )
                                        )
                                    },
                                    onTimeChange = { start, _ ->
                                        update(metric.copy(dueTimeMinutes = start))
                                    },
                                    supportsEndTime = false,
                                    enabled = enabled,
                                    isOverdue = metric.isMetricOverdue(),
                                    modifier = Modifier.weight(2f)
                                )
                            }
                            else -> Unit
                        }
                        UnitDropdown(
                            metric = metric,
                            enabled = enabled,
                            expanded = unitExpandedIndex == index,
                            onExpandedChange = { unitExpandedIndex = if (it) index else null },
                            onUnitChange = { unit -> update(metric.applyUnitChange(unit)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactFlatTextField(
                            value = metric.value,
                            onValueChange = { value ->
                                update(metric.copy(value = value))
                            },
                            placeholder = "Value",
                            modifier = Modifier.weight(1f)
                        )
                        CompactFlatTextField(
                            value = metric.targetValue.orEmpty(),
                            onValueChange = { value ->
                                update(metric.copy(targetValue = value))
                            },
                            placeholder = "Target",
                            modifier = Modifier.weight(1f)
                        )
                        UnitDropdown(
                            metric = metric,
                            enabled = enabled,
                            expanded = unitExpandedIndex == index,
                            onExpandedChange = { unitExpandedIndex = if (it) index else null },
                            onUnitChange = { unit -> update(metric.applyUnitChange(unit)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (metric.unit == MetricUnit.Custom) {
                        CompactFlatTextField(
                            value = metric.customUnit.orEmpty(),
                            onValueChange = { value ->
                                update(metric.copy(customUnit = value))
                            },
                            placeholder = "Custom unit (e.g. kg, pts)",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                metric.progressRatio()?.let { ratio ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${(ratio * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/** Maps unit switches to due-date storage: date-based units gain a due date, others clear it. */
private fun MetricItem.applyUnitChange(unit: MetricUnit): MetricItem = when (unit) {
    MetricUnit.Countdown -> copy(
        unit = unit,
        customUnit = null,
        value = "",
        targetValue = null,
        dueDateEpochDays = dueDateEpochDays ?: today().toEpochDays().toInt(),
        dueTimeMinutes = null
    )
    MetricUnit.DueDate -> copy(
        unit = unit,
        customUnit = null,
        value = "",
        targetValue = null,
        dueDateEpochDays = dueDateEpochDays ?: today().toEpochDays().toInt()
    )
    MetricUnit.Custom -> copy(
        unit = unit,
        dueDateEpochDays = null,
        dueTimeMinutes = null
    )
    else -> copy(
        unit = unit,
        customUnit = null,
        dueDateEpochDays = null,
        dueTimeMinutes = null
    )
}

@Composable
private fun UnitDropdown(
    metric: MetricItem,
    enabled: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onUnitChange: (MetricUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .clickable(enabled = enabled) { onExpandedChange(true) }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = metric.unit.displayName(metric.customUnit),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            MetricUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.displayName(), style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onUnitChange(unit)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.height(30.dp)
                )
            }
        }
    }
}
