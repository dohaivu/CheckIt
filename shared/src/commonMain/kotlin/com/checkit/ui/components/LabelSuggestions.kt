package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            contentPadding = PaddingValues(horizontal = 8.dp)
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(
                alpha = 0.5f
            ))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = 0.5f
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}
