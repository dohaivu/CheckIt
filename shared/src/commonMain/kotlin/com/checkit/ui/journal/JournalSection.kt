package com.checkit.ui.journal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Short labels for the entry label field. Prompts live in JournalPrompts. */
internal val JournalLabels = listOf(
    "gratitude",
    "growth log",
    "deep thoughts",
    "idea",
    "random",
    "lazying",
    "biking",
    "coding",
    "reading",
    "learning",
    "event",
    "at home"
)

internal val MoodCategories = listOf(
    "Happy" to MoodHappyEmojis,
    "Energetic" to MoodEnergeticEmojis,
    "Calm" to MoodCalmEmojis,
    "Loved" to MoodLovedEmojis,
    "Focused" to MoodFocusedEmojis,
    "Tired" to MoodTiredEmojis,
    "Worried" to MoodWorriedEmojis,
    "Sad" to MoodSadEmojis,
)

enum class JournalPeriod(val label: String) {
    Morning("Morning"),
    Afternoon("Afternoon"),
    Evening("Evening")
}

fun Int.toJournalPeriod(): JournalPeriod {
    return when {
        this < 12 * 60 -> JournalPeriod.Morning
        this < 18 * 60 -> JournalPeriod.Afternoon
        else -> JournalPeriod.Evening
    }
}

/** Feeling-first rotating greetings per period. Short, single-line, invitation tone. */
internal fun greetingsForPeriod(period: JournalPeriod): List<String> = when (period) {
    JournalPeriod.Morning -> listOf(
        "Good morning — how are you arriving today?",
        "Morning — what's alive in you right now?",
        "Good morning — how are you, really?",
        "A new page — what needs air today?",
        "Morning light — what do you notice first?",
        "Good morning — what feels tender today?",
        "Waking up — what dreams linger?",
        "Morning — what would feel kind today?"
    )
    JournalPeriod.Afternoon -> listOf(
        "Good afternoon — how's your heart doing?",
        "Pause — what are you carrying?",
        "Afternoon — what does your body say?",
        "Halfway — what needs softening?",
        "Afternoon — where is your energy now?",
        "A small pause — how's your breath?",
        "Midday — what needs your attention?",
        "Afternoon sun — what feels heavy?"
    )
    JournalPeriod.Evening -> listOf(
        "Good evening — how are you, really?",
        "Unwind — what stays with you tonight?",
        "Evening — what can you set down?",
        "Today is done — how do you feel?",
        "Night falls — what are you grateful for?",
        "Evening — who made you feel seen?",
        "Quiet hour — what wants to be said?",
        "Good night — what eases your mind?"
    )
}

/** Compact inviting journal entry point: period greeting, suggested prompt, entry count. */
@Composable
internal fun JournalSection(
    entries: List<JournalEntry>,
    nowMinutes: Int,
    onAddClick: () -> Unit,
    onAddWithPrompt: (String) -> Unit,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestion = remember(nowMinutes, entries.size) {
        suggestedPrompt(nowMinutes, entries.isNotEmpty())
    }
    val period = remember(nowMinutes) { nowMinutes.toJournalPeriod() }
    val greetings = remember(period) { greetingsForPeriod(period) }
    var greetingIndex by remember(period) { mutableIntStateOf(0) }
    LaunchedEffect(period) {
        while (true) {
            delay(3500)
            greetingIndex = (greetingIndex + 1) % greetings.size
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            AnimatedContent(
                targetState = greetings[greetingIndex],
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height } + fadeOut())
                },
                label = "journal-greeting",
                modifier = Modifier.heightIn(min = 20.dp)
            ) { greeting ->
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (suggestion != null) {
                    Text(
                        text = "Try ${suggestion.title}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onAddWithPrompt(suggestion.id) }
                    )
                }
                if (entries.isNotEmpty()) {
                    Text(
                        text = "· ${entries.size} today ·",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = "View",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.clickable { onViewClick() }
                    )
                } else {
                    Text(
                        text = "· put today into words",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        androidx.compose.material3.FilledTonalButton(
            onClick = {
                val id = suggestion?.id
                if (id != null) onAddWithPrompt(id) else onAddClick()
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(4.dp))
            Text("Write")
        }
    }
}

internal fun suggestionCtaLabel(nowMinutes: Int, hasEntryToday: Boolean, suggestion: JournalPrompt): String {
    val prefix = when (nowMinutes.toJournalPeriod()) {
        JournalPeriod.Morning -> if (hasEntryToday) "More room? Try" else "Arriving today? Try"
        JournalPeriod.Afternoon -> "Pause for a moment? Try"
        JournalPeriod.Evening -> if (hasEntryToday) "Unwind further? Try" else "Unwind? Try"
    }
    return "$prefix ${suggestion.title}"
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
