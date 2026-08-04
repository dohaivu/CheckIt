package com.checkit.ui.myday

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.JournalEntry
import com.checkit.domain.TagItem
import com.checkit.ui.components.TagPill
import com.checkit.ui.toTimeMinutes
import com.checkit.ui.tasks.toClockLabel

internal val JournalMoodOptions = listOf("😀", "🙂", "😐", "😢", "😡", "🔥", "💪", "😴", "🎉", "❤️")

/** Quick context presets shown as tappable chips in the entry editor. */
internal val JournalContextPresets = listOf("cafe", "important event", "at home", "code", "surf web")

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

/** Compact single-line header for today's journal: name, entry count, and add/view actions. */
@Composable
internal fun JournalSection(
    entries: List<JournalEntry>,
    onAddClick: () -> Unit,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Notes,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Check-Ins",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (entries.isNotEmpty()) {
            Text(
                text = "${entries.size}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add check-in",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onViewClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = "View check-ins",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
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
            Box(
                modifier = Modifier
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggle(mood) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = mood,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
internal fun JournalEntryList(
    entries: List<JournalEntry>,
    onEntryClick: (JournalEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            style = MaterialTheme.typography.bodyMedium,
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
