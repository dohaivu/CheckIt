package com.checkit.ui.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.JournalEntry
import com.checkit.domain.MoodCalmEmojis
import com.checkit.domain.MoodEnergeticEmojis
import com.checkit.domain.MoodFocusedEmojis
import com.checkit.domain.MoodHappyEmojis
import com.checkit.domain.MoodLovedEmojis
import com.checkit.domain.MoodSadEmojis
import com.checkit.domain.MoodTiredEmojis
import com.checkit.domain.MoodWorriedEmojis
import com.checkit.ui.components.EmojiPicker
import com.checkit.ui.components.asAnnotatedString
import com.checkit.ui.components.getMoodColorFromEmoji
import kotlinx.coroutines.launch

/** Quick label presets shown as tappable chips in the entry editor. */
data class JournalLabelPreset(
    val type: String,
    val prompt: String,
    val template: String
)

internal val JournalLabelPresets = listOf(
    JournalLabelPreset(
        type = "gratitude",
        prompt = "What are you thankful for today?",
        template =
""":
1. 
2. 
3. 
""".trimMargin()
    ),
    JournalLabelPreset(
        type = "growth log",
        prompt = "How did today go? Any wins or lessons?",
        template =
"""
- **Win**: 
- **Friction**:
- **Insight**: 
""".trimMargin()
    ),
    JournalLabelPreset(
        type = "deep thoughts",
        prompt = "What's on your mind right now?",
        template = ""
    ),
    JournalLabelPreset(
        type = "idea",
        prompt = "Got a new idea? Jot it down.",
        template = ""
    ),
    JournalLabelPreset(
        type = "random",
        prompt = "Anything else you want to record?",
        template = ""
    ),
    JournalLabelPreset(
        type = "lazying",
        prompt = "How's your rest going?",
        template = ""
    ),
    JournalLabelPreset(
        type = "biking",
        prompt = "How was the ride?",
        template = ""
    ),
    JournalLabelPreset(
        type = "coding",
        prompt = "What are you working on?",
        template = ""
    ),
    JournalLabelPreset(
        type = "reading",
        prompt = "What are you reading about?",
        template = ""
    ),
    JournalLabelPreset(
        type = "learning",
        prompt = "What's something new you learned?",
        template = ""
    ),
    JournalLabelPreset(
        type = "event",
        prompt = "How was the event?",
        template = ""
    ),
    JournalLabelPreset(
        type = "at home",
        prompt = "How's the vibe at home?",
        template = ""
    )
)

private val MoodCategories = listOf(
    "Happy" to MoodHappyEmojis,
    "Energetic" to MoodEnergeticEmojis,
    "Calm" to MoodCalmEmojis,
    "Loved" to MoodLovedEmojis,
    "Focused" to MoodFocusedEmojis,
    "Tired" to MoodTiredEmojis,
    "Worried" to MoodWorriedEmojis,
    "Sad" to MoodSadEmojis,
)

internal enum class JournalPeriod(val label: String) {
    Morning("Morning"),
    Afternoon("Afternoon"),
    Evening("Evening")
}

internal fun Int.toJournalPeriod(): JournalPeriod {
    return when {
        this < 12 * 60 -> JournalPeriod.Morning
        this < 18 * 60 -> JournalPeriod.Afternoon
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
                    .clickable {
                        onViewClick()
                    }
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add check-in",
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
    onToggle: (String) -> Unit,
    isEditMode: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(!isEditMode) }
    var showFullEmojiPicker by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)
    val shape = RoundedCornerShape(12.dp)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Selected moods row (Header/Toggle)
        // ... (existing code for Selected moods row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mood",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                if (moods.isEmpty()) {
                    Text(
                        text = "How are you feeling?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        items(moods.toList()) { mood ->
                            val moodColor = getMoodColorFromEmoji(mood)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(moodColor.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = mood, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MoodCategories.forEach { (name, emojis) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            emojis.forEach { mood ->
                                val selected = mood in moods
                                val moodColor = getMoodColorFromEmoji(mood)
                                Box(
                                    modifier = Modifier
                                        .clip(shape)
                                        .background(
                                            color = if (selected) moodColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = shape
                                        )
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) moodColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            shape = shape
                                        )
                                        .clickable { onToggle(mood) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = mood,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Inline EmojiPicker Toggle
                OutlinedButton(
                    onClick = { showFullEmojiPicker = true },
                    modifier = Modifier.padding(top = 8.dp),
                    shape = shape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "More Emojis...",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showFullEmojiPicker) {
        EmojiPicker(
            onDismiss = { showFullEmojiPicker = false },
            onEmojiSelect = { emoji -> onToggle(emoji.details.string) }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalThoughtCard(
    entry: JournalEntry,
    modifier: Modifier = Modifier
) {
    val tooltipState = rememberTooltipState(isPersistent = false)
    val scope = rememberCoroutineScope()
    
    val moodColor = entry.moods.firstOrNull()?.let { getMoodColorFromEmoji(it) } ?: MaterialTheme.colorScheme.primary

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above
        ),
        tooltip = {
            RichTooltip(
                title = { Text(entry.label ?: "Check-In") }
            ) {
                Text(entry.content.asAnnotatedString())
            }
        },
        state = tooltipState
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(moodColor.copy(alpha = 0.12f))
                .drawBehind {
                    drawLine(
                        color = moodColor.copy(alpha = 0.6f),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 8.dp.toPx()
                    )
                }
                .clickable { scope.launch { tooltipState.show() } }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val annotatedContent = entry.content.asAnnotatedString()
        Text(
                text = if (entry.content.isNotBlank()) annotatedContent else AnnotatedString(entry.label.orEmpty()),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Cursive,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
