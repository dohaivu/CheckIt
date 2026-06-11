package com.checkit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskList
import com.checkit.ui.RepeatPreset
import com.checkit.ui.tasks.materialIcon
import com.checkit.ui.tasks.toColor

@Composable
internal fun ListPicker(
    selectedListId: Long,
    lists: List<TaskList>,
    onListChange: (Long) -> Unit
) {
    if (lists.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val selectedList by remember(selectedListId) { mutableStateOf( lists.firstOrNull { it.id == selectedListId } ?: lists.first())}

    AppleStylePopup(
        isExpanded = expanded,
        onDismissRequest = { expanded = false },
        anchor = {
            DetailChip(icon = materialIcon(selectedList.icon), label = selectedList.name, onClick = { expanded = true }, iconTint = selectedList.color.toColor(),)
        }
    ) {
        Column(
            modifier = Modifier
                .width(250.dp)
                .padding(vertical = 0.dp)
        ) {
            lists.forEachIndexed { index, list ->
                Row(
                    modifier = Modifier
                        .clickable(onClick = {
                            onListChange(list.id)
                            expanded = false
                        })
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = materialIcon(list.icon),
                        contentDescription = null,
                        tint = list.color.toColor(),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = list.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Draw standard iOS dividers between rows (excluding last element)
                if (index < lists.size - 1) {
                    HorizontalDivider(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}