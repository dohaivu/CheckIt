package com.checkit.ui.myday

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.TagItem
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DeleteOverflowMenu
import com.checkit.ui.components.TagPicker

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun JournalEntryEditorSheet(
    state: JournalEntryEditorState,
    availableTags: List<TagItem>,
    onDismiss: () -> Unit,
    onContextChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onMoodToggle: (String) -> Unit,
    onTagToggle: (Long) -> Unit,
    onNewTagClick: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .heightIn(max = 700.dp)
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        var contextFocused by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.isEditMode) "Edit entry" else "Add entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (state.isEditMode) {
                DeleteOverflowMenu(onDelete = onDelete)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppOutlinedTextField(
                    value = state.context,
                    onValueChange = onContextChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    ),
                    placeholder = "Context (Biking, Cafe…)",
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { contextFocused = it.isFocused }
                )
                if (contextFocused) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        JournalContextPresets.forEach { preset ->
                            PresetChip(
                                label = preset,
                                onClick = { onContextChange(appendContextPreset(state.context, preset)) }
                            )
                        }
                    }
                }
            }
            item {
                AppOutlinedTextField(
                    value = state.content,
                    onValueChange = onContentChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    placeholder = "Freeform status…",
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                MoodRow(
                    moods = state.moods.toSet(),
                    onToggle = onMoodToggle
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    TagPicker(
                        availableTags = availableTags,
                        selectedTagIds = state.selectedTagIds,
                        onTagToggle = onTagToggle,
                        onNewTagClick = onNewTagClick
                    )
                }
            }
            item {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isEditMode) "Save" else "Add")
                }
            }
        }
    }
}

/** Appends a preset to the current context, avoiding duplicates. */
private fun appendContextPreset(current: String, preset: String): String {
    val parts = current.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (preset in parts) return current
    val combined = if (parts.isEmpty()) preset else parts.joinToString(", ") + ", " + preset
    return combined
}

@Composable
private fun PresetChip(
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
