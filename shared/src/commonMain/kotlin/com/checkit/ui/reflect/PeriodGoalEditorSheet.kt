package com.checkit.ui.reflect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.reflect_review_card_title
import checkit.shared.generated.resources.reflect_review_save
import com.checkit.domain.MetricItem
import com.checkit.domain.MetricUnit
import com.checkit.ui.components.RatingBar
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.CompactFlatTextField
import com.checkit.ui.components.MarkdownVisualTransformation
import com.checkit.ui.components.icons.AppIcons
import com.checkit.ui.components.icons.Target
import com.checkit.ui.displayName
import com.checkit.ui.periodDetail
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodGoalEditorSheet(
    editor: ReflectGoalEditorState,
    onReviewChange: (String) -> Unit,
    onGoalChange: (String) -> Unit,
    onRatingChange: (Float) -> Unit,
    onMetricsChange: (List<MetricItem>) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val periodLabel = editor.focus.periodDetail()
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        sheetGesturesEnabled = false,
        modifier = Modifier
            .fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (editor.mode == ReflectGoalEditorMode.GoalOnly) AppIcons.Target else editor.focus.period.reviewIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (editor.mode == ReflectGoalEditorMode.GoalOnly) {
                            "${periodLabel.uppercase()} GOAL"
                        } else {
                            stringResource(Res.string.reflect_review_card_title, periodLabel).uppercase()
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (editor.mode == ReflectGoalEditorMode.Full) {
                Text(
                    text = "What are your wins? frictions? lessons?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AppOutlinedTextField(
                    value = editor.review,
                    onValueChange = onReviewChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    placeholder = "Jot down your reflection ...",
                    minLines = 6,
                    enabled = !editor.isSaving,
                    visualTransformation = remember { MarkdownVisualTransformation() }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    RatingBar(
                        rating = editor.rating,
                        onRatingChange = onRatingChange,
                        enabled = !editor.isSaving,
                        modifier = Modifier
                            .width(150.dp)
                            .height(32.dp)
                    )
                }
            }

            if (editor.mode == ReflectGoalEditorMode.Full) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Target,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "GOAL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "What are the 3 non-negotiable priority tasks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AppOutlinedTextField(
                value = editor.goal,
                onValueChange = onGoalChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                placeholder = "What will you focus on?",
                minLines = 4,
                enabled = !editor.isSaving,
                visualTransformation = remember { MarkdownVisualTransformation() }
            )

            PeriodMetricsSection(
                metrics = editor.metrics,
                enabled = !editor.isSaving,
                onMetricsChange = onMetricsChange
            )

            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSave, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.reflect_review_save))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Add/edit/delete custom metrics for the goal (mirrors nested-item details dialog, section 3). */
@Composable
private fun PeriodMetricsSection(
    metrics: List<MetricItem>,
    enabled: Boolean,
    onMetricsChange: (List<MetricItem>) -> Unit
) {
    var unitExpandedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
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

        if (metrics.isEmpty()) {
            Text(
                text = "No custom metrics added.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        metrics.forEachIndexed { index, metric ->
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
                            .then(if (enabled) Modifier.clickable { onMetricsChange(metrics.toMutableList().also { it[index] = metric.copy(isCompleted = !metric.isCompleted) }) } else Modifier)
                    )
                    CompactFlatTextField(
                        value = metric.name,
                        onValueChange = { value ->
                            onMetricsChange(metrics.toMutableList().also { it[index] = metric.copy(name = value) })
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
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactFlatTextField(
                        value = metric.value,
                        onValueChange = { value ->
                            onMetricsChange(metrics.toMutableList().also { it[index] = metric.copy(value = value) })
                        },
                        placeholder = "Value",
                        modifier = Modifier.weight(1f)
                    )
                    CompactFlatTextField(
                        value = metric.targetValue.orEmpty(),
                        onValueChange = { value ->
                            onMetricsChange(metrics.toMutableList().also { it[index] = metric.copy(targetValue = value) })
                        },
                        placeholder = "Target",
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .clickable(enabled = enabled) { unitExpandedIndex = index }
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
                            expanded = unitExpandedIndex == index,
                            onDismissRequest = { unitExpandedIndex = null }
                        ) {
                            MetricUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.displayName(), style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        onMetricsChange(
                                            metrics.toMutableList().also {
                                                it[index] = metric.copy(
                                                    unit = unit,
                                                    customUnit = if (unit == MetricUnit.Custom) metric.customUnit else null
                                                )
                                            }
                                        )
                                        unitExpandedIndex = null
                                    },
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }
                    }
                }

                if (metric.unit == MetricUnit.Custom) {
                    CompactFlatTextField(
                        value = metric.customUnit.orEmpty(),
                        onValueChange = { value ->
                            onMetricsChange(metrics.toMutableList().also { it[index] = metric.copy(customUnit = value) })
                        },
                        placeholder = "Custom unit (e.g. kg, pts)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}