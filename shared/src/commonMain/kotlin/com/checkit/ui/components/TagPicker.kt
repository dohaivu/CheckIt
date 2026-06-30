package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskTag
import com.checkit.ui.tasks.views.ContentAlpha
import com.checkit.ui.theme.toColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagOptionMenu(
    availableTags: List<TaskTag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit
) {
    if (availableTags.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val hasSelectedTags = selectedTagIds.isNotEmpty()

    AppleStylePopup(
        isExpanded = expanded,
        onDismissRequest = { expanded = false },
        anchor = {
            IconButton(
                onClick = {
                    expanded = true
                }
            ) {
                Box(modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = if (hasSelectedTags) "Tag filters active" else "View options",
                        tint = if (hasSelectedTags) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    if (hasSelectedTags) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            availableTags.forEach { tag ->
                TagPill(
                    tag = tag,
                    selected = selectedTagIds.contains(tag.id),
                    onClick = { onTagToggle(tag.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPicker(
    availableTags: List<TaskTag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit,
    enabled: Boolean = true
) {
    if (availableTags.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    val selectedTags = remember(selectedTagIds, availableTags) {
        availableTags.filter { it.id in selectedTagIds }
    }
    AppleStylePopup(
        isExpanded = expanded,
        onDismissRequest = { expanded = false },
        anchor = {
            if (selectedTags.isEmpty()) {
                TagPill(tag = TaskTag.None, selected = false, onClick = { if (enabled) expanded = true })
            } else {
                FlowRow(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedTags.forEach { tag ->
                        TagPill(
                            tag = tag,
                            selected = false,
                            onClick = { if (enabled) expanded = true }
                        )
                    }
                }
            }
        }
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            availableTags.forEach { tag ->
                TagPill(
                    tag = tag,
                    selected = tag.id in selectedTagIds,
                    onClick = { onTagToggle(tag.id) }
                )
            }
        }
    }
}

@Composable
internal fun TagPill(
    tag: TaskTag,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tagColor = tag.color.toColor()
    DetailChip(
        icon = Icons.AutoMirrored.Filled.Label,
        label = tag.name,
        modifier = modifier,
        iconTint = tagColor,
        backgroundColor = if (selected) tagColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        borderColor = if (selected) tagColor.copy(alpha = ContentAlpha) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        onClick = onClick
    )
}
