package com.checkit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskTag
import com.checkit.ui.tasks.TaskTagPill

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPicker(
    availableTags: List<TaskTag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit
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
                TaskTagPill(tag = TaskTag.None, selected = false, onClick = { expanded = true })
            } else {
                FlowRow(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedTags.forEach { tag ->
                        TaskTagPill(
                            tag = tag,
                            selected = false,
                            onClick = { expanded = true }
                        )
                    }
                }
            }
        }
    ) {
        FlowRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            availableTags.forEach { tag ->
                TaskTagPill(
                    tag = tag,
                    selected = tag.id in selectedTagIds,
                    onClick = { onTagToggle(tag.id) }
                )
            }
        }
    }
}