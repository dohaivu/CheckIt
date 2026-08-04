package com.checkit.ui.myday

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.JournalEntry
import com.checkit.domain.TagItem
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.TagPicker
import com.checkit.ui.components.TagPill
import com.checkit.ui.toTimeMinutes
import com.checkit.ui.tasks.toClockLabel

internal val JournalMoodOptions = listOf("😀", "🙂", "😐", "😢", "😡", "🔥", "💪", "😴", "🎉", "❤️")

internal enum class JournalPeriod(val label: String) {
    Morning("Morning"),
    Afternoon("Afternoon"),
    Evening("Evening")
}

internal fun Long.toJournalPeriod(): JournalPeriod {
    val minutes = toTimeMinutes()
    return when {
        minutes < 12 * 60 -> JournalPeriod.Morning
        minutes < 18 * 60 -> JournalPeriod.Afternoon
        else -> JournalPeriod.Evening
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JournalSection(
    entries: List<JournalEntry>,
    capture: JournalCaptureState,
    availableTags: List<TagItem>,
    recentContexts: List<String>,
    activeTagFilter: Long?,
    onContextChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onContextSuggestion: (String) -> Unit,
    onMoodToggle: (String) -> Unit,
    onTagToggle: (Long) -> Unit,
    onSubmit: () -> Unit,
    onEntryClick: (JournalEntry) -> Unit,
    onTagFilter: (Long?) -> Unit,
    onNewTagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entryTags = entries.flatMap { it.tags }.distinctBy { it.id }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        JournalHeader()

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppOutlinedTextField(
                        value = capture.context,
                        onValueChange = onContextChange,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        ),
                        placeholder = "Context (Biking, Cafe…)",
                        maxLines = 1,
                        modifier = Modifier.weight(0.42f)
                    )
                    AppOutlinedTextField(
                        value = capture.content,
                        onValueChange = onContentChange,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        placeholder = "Freeform status…",
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        modifier = Modifier.weight(0.58f)
                    )
                    IconButton(onClick = onSubmit, enabled = capture.canSubmit) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save journal entry",
                            tint = if (capture.canSubmit) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                    }
                }

                if (recentContexts.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recentContexts.forEach { context ->
                            JournalContextChip(
                                label = context,
                                onClick = { onContextSuggestion(context) }
                            )
                        }
                    }
                }

                MoodRow(
                    moods = capture.selectedMoods,
                    onToggle = onMoodToggle
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TagPicker(
                        availableTags = availableTags,
                        selectedTagIds = capture.selectedTagIds,
                        onTagToggle = onTagToggle,
                        onNewTagClick = onNewTagClick
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (capture.selectedMoods.isEmpty() && capture.selectedTagIds.isEmpty() && capture.context.isBlank() && capture.content.isBlank()) {
                            "Tap ✓ to save"
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (entryTags.isNotEmpty() || activeTagFilter != null) {
            JournalTagFilterRow(
                tags = entryTags,
                activeTagFilter = activeTagFilter,
                onTagFilter = onTagFilter
            )
        }

        if (entries.isEmpty()) {
            Text(
                text = "No journal entries yet. Capture a thought above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        } else {
            JournalEntryList(
                entries = entries,
                onEntryClick = onEntryClick
            )
        }
    }
}

@Composable
private fun JournalHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Notes,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Journal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun JournalContextChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoodRow(
    moods: Set<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        JournalMoodOptions.forEach { mood ->
            val selected = mood in moods
            Surface(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier.clickable { onToggle(mood) }
            ) {
                Text(
                    text = mood,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalTagFilterRow(
    tags: List<TagItem>,
    activeTagFilter: Long?,
    onTagFilter: (Long?) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        JournalFilterChip(
            label = "All",
            selected = activeTagFilter == null,
            onClick = { onTagFilter(null) }
        )
        tags.forEach { tag ->
            JournalFilterChip(
                label = tag.name,
                selected = activeTagFilter == tag.id,
                onClick = { onTagFilter(tag.id) }
            )
        }
    }
}

@Composable
private fun JournalFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun JournalEntryList(
    entries: List<JournalEntry>,
    onEntryClick: (JournalEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        JournalPeriod.entries.forEach { period ->
            val periodEntries = entries.filter { it.createdAtMillis.toJournalPeriod() == period }
            if (periodEntries.isNotEmpty()) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    periodEntries.forEach { entry ->
                        JournalEntryCard(
                            entry = entry,
                            onClick = { onEntryClick(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun JournalEntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (entry.hasContent || !entry.context.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!entry.context.isNullOrBlank()) {
                            Text(
                                text = entry.context,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (entry.hasContent) {
                            Text(
                                text = entry.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (entry.moods.isNotEmpty()) {
                        Text(
                            text = entry.moods.joinToString(" "),
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = entry.createdAtMillis.toTimeMinutes().toClockLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (entry.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.forEach { tag ->
                        TagPill(tag = tag)
                    }
                }
            }
        }
    }
}
