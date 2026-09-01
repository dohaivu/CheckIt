package com.checkit.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.JournalEntry
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.TagPlain
import com.checkit.ui.components.asAnnotatedString
import com.checkit.ui.components.getMoodColorFromEmoji
import com.checkit.ui.toClockLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalListSheet(
    entries: List<JournalEntry>,
    onEntryClick: (JournalEntry) -> Unit,
    onDismiss: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.heightIn(min = 600.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Check-Ins",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (entries.size == 1) "1 entry" else "${entries.size} entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (entries.isEmpty()) {
                    Text(
                        text = "No entries yet. Tap + to add a check-in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    JournalEntryList(
                        entries = entries,
                        onEntryClick = onEntryClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
            val periodEntries = entries.filter { it.createdTimeMinutes.toJournalPeriod() == period }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JournalEntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moodColor = entry.moods.firstOrNull()?.let { getMoodColorFromEmoji(it) }
    val hasMood = moodColor != null
    val baseSurface = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)

    val mc = moodColor
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (hasMood && mc != null) mc.copy(alpha = 0.09f) else baseSurface
            )
            .border(
                width = 1.dp,
                color = if (hasMood && mc != null) mc.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(entry.id) {
                detectTapGestures(onLongPress = { onClick() })
            }
    ) {
        // --- Atmosphere layer: mood wash + emoji watermark (compact) ---
        if (hasMood && mc != null) {
            Box(
                modifier = Modifier.matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                mc.copy(alpha = 0.18f),
                                mc.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(900f, 0f),
                            radius = 420f
                        )
                    )
            )
            Box(
                modifier = Modifier.matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                mc.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Text(
                text = entry.moods.first(),
                fontSize = 48.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 6.dp)
                    .alpha(0.13f),
            )
            if (entry.moods.size > 1) {
                Text(
                    text = entry.moods[1],
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .alpha(0.07f)
                )
            }
            if (entry.moods.size > 2) {
                Text(
                    text = entry.moods[2],
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-30).dp, y = 8.dp)
                        .alpha(0.06f)
                )
            }
        } else {
            Box(
                modifier = Modifier.matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = entry.content.asAnnotatedString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                lineHeight = 16.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.createdTimeMinutes.toClockLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f),
                    textAlign = TextAlign.Start,
                    fontSize = 10.sp
                )

                if (!entry.label.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(2.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Text(
                        text = entry.label ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasMood && mc != null) mc.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = 10.sp
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f, fill = false))
                }
            }

            if (entry.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.tags.forEach { tag ->
                        TagPlain(tag = tag)
                    }
                }
            }
        }
    }
}
