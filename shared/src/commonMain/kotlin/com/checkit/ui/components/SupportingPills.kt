package com.checkit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.ListItem
import com.checkit.domain.TagItem
import com.checkit.ui.theme.materialIcon
import com.checkit.ui.theme.toColor

@Composable
internal fun SupportingPills(
    list: ListItem? = null,
    tags: List<TagItem> = emptyList(),
    overflowCount: Int = 0,
    modifier: Modifier = Modifier
) {
    if (list == null && tags.isEmpty() && overflowCount == 0) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        list?.let {
            DetailChip(
                icon = materialIcon(it.icon),
                label = it.title,
                iconTint = it.color.toColor()
            )
        }

        tags.forEach { tag -> TagPill(tag = tag) }
        if (overflowCount > 0) {
            Text(
                text = "+$overflowCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 2.dp).align(Alignment.CenterVertically)
            )
        }
    }
}
