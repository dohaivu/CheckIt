package com.checkit.ui.myday

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.day_review_action_carry
import checkit.shared.generated.resources.day_review_action_done
import checkit.shared.generated.resources.day_review_action_drop
import checkit.shared.generated.resources.day_review_done_count
import checkit.shared.generated.resources.day_review_done_minutes
import checkit.shared.generated.resources.day_review_finish
import checkit.shared.generated.resources.day_review_leftovers_empty
import checkit.shared.generated.resources.day_review_leftovers_title
import checkit.shared.generated.resources.day_review_planned_count
import checkit.shared.generated.resources.day_review_title
import checkit.shared.generated.resources.day_review_win_note_label
import checkit.shared.generated.resources.day_review_win_note_placeholder
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DayReviewTagMinutes
import com.checkit.domain.LeftoverAction
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppHorizontalDivider
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.RichTextComposer
import com.checkit.ui.tasks.isOverdue
import com.checkit.ui.tasks.toDurationLabel
import com.checkit.ui.tasks.views.DailyPlanTimelineCard
import com.checkit.ui.theme.toColor
import com.checkit.ui.today
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayReviewSheet(
    state: DayReviewUiState,
    onDismiss: () -> Unit,
    onLeftoverAction: (Long, LeftoverAction) -> Unit,
    onWinNoteChange: (String) -> Unit,
    onTomorrowGoalChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .padding(bottom = 16.dp),
        sheetGesturesEnabled = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .alpha(if (state.isSubmitting) 0.5f else 1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.day_review_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            SummaryAndTagsRow(state)

            val leftoverItems = remember(state.summary) {
                state.summary.plannedItems + state.summary.alreadyCarriedItems
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                item {
                    ReflectionSection(
                        value = state.winNote,
                        onValueChange = onWinNoteChange,
                        enabled = !state.isSubmitting
                    )
                }

                item {
                    TomorrowGoalSection(
                        value = state.tomorrowGoal,
                        onValueChange = onTomorrowGoalChange,
                        enabled = !state.isSubmitting
                    )
                }

                item {
                    Text(
                        text = stringResource(Res.string.day_review_leftovers_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (leftoverItems.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.day_review_leftovers_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    itemsIndexed(
                        leftoverItems,
                        key = { _, item -> item.id }) { index, item ->
                        LeftoverReviewRow(
                            item = item,
                            action = state.actionFor(item),
                            enabled = !state.isSubmitting,
                            onAction = { onLeftoverAction(item.id, it) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isSubmitting
            ) {
                Text(stringResource(Res.string.cancel))
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onConfirm,
                enabled = !state.isSubmitting
            ) {
                Text(stringResource(Res.string.day_review_finish))
            }
        }
    }
}

@Composable
private fun ReflectionSection(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean
) {
    val prompt = remember { WinNotePrompts.random() }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.day_review_win_note_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tip: $prompt",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
//        RichTextComposer(
//            value = value,
//            onValueChange = onValueChange,
//            placeholder = stringResource(Res.string.day_review_win_note_placeholder),
//            modifier = Modifier.fillMaxWidth(),
//            enabled = enabled
//        )

        AppOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.day_review_win_note_placeholder),
            minLines = 4,
            maxLines = 8,
            enabled = enabled
        )
    }
}

@Composable
private fun TomorrowGoalSection(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Tomorrow's Top Priority",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.fillMaxWidth(),
            placeholder = "One thing you want to focus on...",
            minLines = 1,
            enabled = enabled
        )
    }
}

@Composable
private fun SummaryAndTagsRow(state: DayReviewUiState) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(label = stringResource(Res.string.day_review_done_count, state.summary.doneCount))
        SummaryChip(label = stringResource(Res.string.day_review_planned_count, state.summary.plannedCount))
        SummaryChip(label = stringResource(Res.string.day_review_done_minutes, state.summary.doneMinutes))
        
        if (state.streak > 0) {
            SummaryChip(label = "${state.streak}-day review streak")
        }

        state.summary.topTags.forEach { tag ->
            TagChip(tag)
        }
    }
}

@Composable
private fun TagChip(tag: DayReviewTagMinutes) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(32.dp)
            .clip(MaterialTheme.shapes.small)
            .background(tag.color.toColor().copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(tag.color.toColor())
        )
        Text(
            text = "${tag.name} (${tag.totalMinutes.toDurationLabel()})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private val WinNotePrompts = listOf(
    "What is one thing you're proud of today?",
    "What was the highlight of your day?",
    "What made you smile today?",
    "What is a small win you achieved?",
    "What did you learn today?"
)

@Composable
private fun SummaryChip(label: String) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun LeftoverReviewRow(
    item: DailyPlanItem,
    action: LeftoverAction,
    enabled: Boolean,
    onAction: (LeftoverAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        DailyPlanTimelineCard(
            item = item,
            isOverdue = item.isOverdue(today())
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.Center
        ) {
            LeftoverAction.entries.filter { it != LeftoverAction.None }.forEach { option ->
                FilterChip(
                    selected = action == option,
                    onClick = { onAction(option) },
                    enabled = enabled,
                    label = {
                        Text(
                            when (option) {
                                LeftoverAction.MarkDone ->
                                    stringResource(Res.string.day_review_action_done)
                                LeftoverAction.CarryOver ->
                                    stringResource(Res.string.day_review_action_carry)
                                LeftoverAction.Drop ->
                                    stringResource(Res.string.day_review_action_drop)
                                LeftoverAction.None -> error("None is filtered out")
                            }
                        )
                    },
                    leadingIcon = if (action == option) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}
