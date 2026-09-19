package com.checkit.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.ui.components.AppleStylePopup
import com.checkit.ui.components.DetailChip

@Composable
internal fun PromptPicker(
    selectedPromptId: String?,
    onPromptSelected: (JournalPrompt) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = remember(selectedPromptId) { findJournalPrompt(selectedPromptId) }

    AppleStylePopup(
        isExpanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = modifier,
        anchor = {
            DetailChip(
                icon = Icons.Outlined.EditNote,
                label = selected?.title ?: "Prompt",
                onClick = { if (enabled) expanded = true },
                iconTint = if (selected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .widthIn(max = 340.dp)
                .padding(vertical = 4.dp)
        ) {
            if (selected != null) {
                Row(
                    modifier = Modifier
                        .clickable {
                            onClear()
                            expanded = false
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "No prompt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 0.5.dp)
            }
            JournalPrompts.forEachIndexed { index, prompt ->
                Column(
                    modifier = Modifier
                        .clickable {
                            onPromptSelected(prompt)
                            expanded = false
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = prompt.guidingQuestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (index < JournalPrompts.lastIndex) {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 0.5.dp)
                }
            }
        }
    }
}
