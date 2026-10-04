package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.checkit.domain.MetricItem
import com.checkit.ui.isMetricOverdue
import com.checkit.ui.toAnnotatedString

@Composable
fun MetricChip(
    metric: MetricItem,
    modifier: Modifier = Modifier.Companion
) {
    val isOverdue = metric.isMetricOverdue()
    val valueColor = when {
        isOverdue -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val content = metric.toAnnotatedString(valueColor = valueColor)

    val isCompleted = metric.isCompleted
    val containerColor = when {
        isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
    }
    val contentColor = when {
        isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
        isOverdue -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = content,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1
        )
    }
}