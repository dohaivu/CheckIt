package com.checkit.ui.reflect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.reflect_review_card_title
import checkit.shared.generated.resources.reflect_review_save
import com.checkit.domain.MetricUnit
import com.checkit.domain.PeriodMetric
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.CompactFlatTextField
import com.checkit.ui.components.MarkdownVisualTransformation
import com.checkit.ui.displayName
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodGoalEditorSheet(
    editor: ReflectGoalEditorState,
    onReviewChange: (String) -> Unit,
    onGoalChange: (String) -> Unit,
    onRatingChange: (Float) -> Unit,
    onMetricsChange: (List<PeriodMetric>) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val periodLabel = editor.focus.period.label()
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = editor.focus.period.reviewIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.reflect_review_card_title, periodLabel).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
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
                    modifier = Modifier.width(150.dp).height(32.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "PERIOD GOAL",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
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

@Composable
private fun RatingBar(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val starValue = index + 1f
            val isFull = rating >= starValue
            val isHalf = rating >= starValue - 0.5f && !isFull

            val icon = when {
                isFull -> Icons.Filled.Star
                isHalf -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }

            val tint = if (isFull || isHalf) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .pointerInput(enabled, rating) {
                        if (enabled) {
                            detectTapGestures { offset ->
                                val isLeft = offset.x < size.width / 2
                                val newRating = if (isLeft) starValue - 0.5f else starValue
                                // Toggle logic: if tapping 0.5 and it's already 0.5, set to 0
                                val finalRating = if (newRating == 0.5f && rating == 0.5f) 0f else newRating
                                onRatingChange(finalRating)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Rate $starValue stars",
                    tint = tint,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/** Add/edit/delete custom metrics for the goal (mirrors nested-item details dialog, section 3). */
@Composable
private fun PeriodMetricsSection(
    metrics: List<PeriodMetric>,
    enabled: Boolean,
    onMetricsChange: (List<PeriodMetric>) -> Unit
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
                        metrics + PeriodMetric(
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
                            modifier = Modifier.size(16.dp)
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