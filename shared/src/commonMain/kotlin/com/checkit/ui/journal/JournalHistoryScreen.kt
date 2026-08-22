package com.checkit.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.journal_agenda_review_card_title
import com.checkit.domain.JournalEntry
import com.checkit.domain.PeriodReview
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.TagPlain
import com.checkit.ui.components.asAnnotatedString
import com.checkit.ui.components.getMoodColorFromEmoji
import com.checkit.ui.tasks.TimelineItem
import com.checkit.ui.tasks.TimelineItemType
import com.checkit.ui.tasks.views.AgendaView
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalHistorySheet(
    entries: List<JournalEntry>,
    dayReviews: List<PeriodReview> = emptyList(),
    onEntryClick: (JournalEntry) -> Unit,
    onReviewClick: (PeriodReview) -> Unit = {},
    onDismiss: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        JournalAgendaView(
            journalEntries = entries,
            dayReviews = dayReviews,
            onEntryClick = onEntryClick,
            onReviewClick = onReviewClick,
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
        )
    }
}

@Composable
internal fun JournalAgendaView(
    journalEntries: List<JournalEntry>,
    dayReviews: List<PeriodReview> = emptyList(),
    onEntryClick: (JournalEntry) -> Unit,
    onReviewClick: (PeriodReview) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timelineItems = remember(journalEntries, dayReviews) {
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
            dayReviews.forEach { review ->
                add(
                    TimelineItem(
                        id = "review-${review.id}",
                        type = TimelineItemType.Journal,
                        date = review.periodStartDate,
                        startTimeMinutes = null,
                        endTimeMinutes = null,
                        sortOrder = 0,
                        isResizable = false,
                        tag = review
                    )
                )
            }
        }.sortedByDescending { it.date }
    }

    AgendaView(
        items = timelineItems,
        onItemClick = { item -> },
        itemContent = { item ->
            when (val tag = item.tag) {
                is JournalEntry -> JournalHistoryEntryCard(
                    entry = tag,
                    onClick = { onEntryClick(tag) },
                    modifier = Modifier.padding(start = 8.dp)
                )
                is PeriodReview -> JournalAgendaReviewCard(
                    review = tag,
                    onClick = { onReviewClick(tag)},
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun JournalAgendaReviewCard(
    review: PeriodReview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .pointerInput(review.id) {
                detectTapGestures(
                    onLongPress = { onClick() }
                )
            },
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = stringResource(Res.string.journal_agenda_review_card_title, review.periodStartDate.day),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (review.content.isNotBlank()) {
            Text(
                text = review.content.asAnnotatedString(),
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