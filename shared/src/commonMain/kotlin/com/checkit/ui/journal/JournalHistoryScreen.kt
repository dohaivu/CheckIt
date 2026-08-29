package com.checkit.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.JournalEntry
import com.checkit.domain.MoodFilter
import com.checkit.domain.PeriodGoal
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.TagOptionMenu
import com.checkit.ui.components.TagPlain
import com.checkit.ui.components.asAnnotatedString
import com.checkit.ui.components.getMoodColorFromEmoji
import com.checkit.ui.reflect.label
import com.checkit.ui.reflect.reviewIcon
import com.checkit.ui.TimelineItem
import com.checkit.ui.TimelineItemType
import com.checkit.ui.color
import com.checkit.ui.components.RatingBar
import com.checkit.ui.tasks.views.AgendaView
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalHistorySheet(
    state: JournalHistoryUiState,
    onMoodToggle: (MoodFilter) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onTagToggle: (Long) -> Unit,
    onEntryClick: (JournalEntry) -> Unit,
    onGoalClick: (PeriodGoal) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onDismiss: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f),
        sheetGesturesEnabled = false
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            JournalFilterBar(
                state = state,
                onMoodToggle = onMoodToggle,
                onSearchTextChange = onSearchTextChange,
                onTagToggle = onTagToggle
            )
            JournalAgendaView(
                journalEntries = state.entries,
                dayGoals = state.dayGoals,
                hasOlder = state.hasOlder,
                onLoadMore = onLoadMore,
                onEntryClick = onEntryClick,
                onGoalClick = onGoalClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun JournalFilterBar(
    state: JournalHistoryUiState,
    onMoodToggle: (MoodFilter) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onTagToggle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MoodFilter.entries.forEach { mood ->
            FilterChip(
                selected = state.filters.mood == mood,
                onClick = { onMoodToggle(mood) },
                label = { Text(mood.label) }
            )
        }
        AppOutlinedTextField(
            value = state.filters.searchText,
            onValueChange = onSearchTextChange,
            placeholder = "Search",
            modifier = Modifier.weight(1f)
        )
        TagOptionMenu(
            availableTags = state.tags,
            selectedTagIds = state.filters.tagId?.let(::setOf) ?: emptySet(),
            onTagToggle = onTagToggle
        )
    }
}

/** Marker tag for the auto-load sentinel at the end of the history agenda. */
private data object LoadOlderMarker

@Composable
internal fun JournalAgendaView(
    journalEntries: List<JournalEntry>,
    dayGoals: List<PeriodGoal> = emptyList(),
    hasOlder: Boolean = false,
    onLoadMore: () -> Unit = {},
    onEntryClick: (JournalEntry) -> Unit,
    onGoalClick: (PeriodGoal) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timelineItems = remember(journalEntries, dayGoals, hasOlder) {
        buildList {
            journalEntries.forEach { entry ->
                add(
                    TimelineItem(
                        id = "journal-${entry.id}",
                        type = TimelineItemType.Journal,
                        date = LocalDate.fromEpochDays(entry.dateEpochDays),
                        startTimeMinutes = entry.createdTimeMinutes,
                        endTimeMinutes = null,
                        sortOrder = 0,
                        isResizable = false,
                        tag = entry
                    )
                )
            }
            dayGoals.forEach { goal ->
                add(
                    TimelineItem(
                        id = "goal-${goal.id}",
                        type = TimelineItemType.Journal,
                        date = goal.endDateInclusive,
                        startTimeMinutes = null,
                        endTimeMinutes = null,
                        sortOrder = 0,
                        isResizable = false,
                        tag = goal
                    )
                )
            }
        }.sortedByDescending { it.date }
    }

    val agendaItems = if (hasOlder) {
        timelineItems + TimelineItem(
            id = "load-older-sentinel",
            type = TimelineItemType.Journal,
            date = LocalDate.fromEpochDays(0),
            sortOrder = Int.MIN_VALUE,
            tag = LoadOlderMarker
        )
    } else {
        timelineItems
    }

    AgendaView(
        items = agendaItems,
        onItemClick = { item -> },
        itemContent = { item ->
            when (val tag = item.tag) {
                LoadOlderMarker -> LoadOlderIndicator(onLoadMore)
                is JournalEntry -> JournalHistoryEntryCard(
                    entry = tag,
                    onClick = { onEntryClick(tag) },
                    modifier = Modifier.padding(start = 8.dp)
                )
                is PeriodGoal -> JournalAgendaGoalCard(
                    goal = tag,
                    onClick = { onGoalClick(tag)},
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        modifier = modifier
    )
}

/** Composing this row (i.e., the user scrolled to the bottom) loads older history. */
@Composable
private fun LoadOlderIndicator(onLoadMore: () -> Unit) {
    LaunchedEffect(Unit) { onLoadMore() }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun JournalAgendaGoalCard(
    goal: PeriodGoal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .pointerInput(goal.id) {
                detectTapGestures(
                    onLongPress = { onClick() }
                )
            },
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = goal.period.reviewIcon(),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = goal.period.color()
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${goal.period.label()} ",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            RatingBar(goal.rating, modifier = Modifier.width(80.dp), iconTint = goal.period.color())
        }
        if (goal.review.isNotBlank()) {
            Text(
                text = goal.review.asAnnotatedString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JournalHistoryEntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moodColor = entry.moods.firstOrNull()?.let { getMoodColorFromEmoji(it) } ?: MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(moodColor.copy(alpha = 0.08f))
            .drawBehind {
                drawLine(
                    color = moodColor.copy(alpha = 0.4f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 8.dp.toPx()
                )
            }
            .pointerInput(entry.id) {
                detectTapGestures(
                    onLongPress = { onClick() }
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = entry.content.asAnnotatedString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 24.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!entry.label.isNullOrBlank()) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }

            if (entry.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entry.tags.forEach { tag ->
                        TagPlain(tag = tag)
                    }
                }
            }
        }
    }
}