package com.checkit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.checkit.domain.MetricUnit
import com.checkit.ui.formatMetricDouble
import com.checkit.ui.parseMetricDouble
import com.checkit.ui.sanitizeMetricInput

/** − / value / + stepper for whole-number units. Blank stays blank; + starts at min + step. */
@Composable
internal fun StepperInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    step: Int = 1,
    min: Int = 0
) {
    val current = value.toIntOrNull()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = "Decrease",
            tint = if (enabled && current != null && current > min) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled && current != null && current > min) {
                    if (current != null) onValueChange(((current - step).coerceAtLeast(min)).toString())
                }
                .padding(6.dp)
        )
        CompactFlatTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
            placeholder = "Value",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Increase",
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled) {
                    onValueChange((((current ?: (min - step)) + step).coerceAtLeast(min)).toString())
                }
                .padding(6.dp)
        )
    }
}

/** Fixed-scale integer star input. Tap the current rating again to clear. */
@Composable
internal fun StarRatingInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    max: Int = 5
) {
    val current = value.toIntOrNull()?.coerceIn(0, max) ?: 0
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (star in 1..max) {
            Icon(
                imageVector = if (star <= current) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Rate $star of $max",
                tint = if (star <= current) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clickable(enabled = enabled) {
                        onValueChange(if (star == current) "" else star.toString())
                    }
            )
        }
    }
}

/**
 * Percentage input: 0–100 slider synced with a decimal field.
 * The field may exceed 100 (e.g. value 120 of target 200); the slider caps display at 100.
 */
@Composable
internal fun PercentageInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val numeric = parseMetricDouble(value)
    val sliderInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Slider(
            value = numeric?.toFloat()?.coerceIn(0f, 100f) ?: 0f,
            onValueChange = { onValueChange(formatMetricDouble(it.toDouble())) },
            valueRange = 0f..100f,
            enabled = enabled,
            interactionSource = sliderInteraction,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = sliderInteraction,
                    thumbSize = DpSize(12.dp, 12.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        CompactFlatTextField(
            value = value,
            onValueChange = { onValueChange(sanitizeMetricInput(MetricUnit.Percentage, it)) },
            placeholder = "Value",
            suffix = "%",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(96.dp)
        )
    }
}
