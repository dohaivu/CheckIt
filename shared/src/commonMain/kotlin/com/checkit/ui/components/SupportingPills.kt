package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
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

@Composable
internal fun TagPill(tag: TagItem) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tag.color.toColor().copy(alpha = 0.08f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(tag.color.toColor(), CircleShape)
        )
        Text(
            text = tag.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = tag.color.toColor(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
