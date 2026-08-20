package com.checkit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.ui.tasks.PredefinedLabels

@Composable
internal fun LabelSuggestions(
    currentLabel: String,
    recentLabels: List<String>,
    onLabelSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = remember(currentLabel, recentLabels) {
        (recentLabels + PredefinedLabels)
            .distinct()
            .filter { it.contains(currentLabel, ignoreCase = true) && it != currentLabel }
            .take(10)
    }

    if (suggestions.isNotEmpty()) {
        LazyRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(suggestions) { label ->
                LabelSuggestionChip(
                    label = label,
                    onClick = { onLabelSelect(label) }
                )
            }
        }
    }
}

@Composable
private fun LabelSuggestionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
