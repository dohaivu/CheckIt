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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
            ReviewSummaryRow(state)

            if (state.summary.topTags.isNotEmpty()) {
                TagInsightsRow(state.summary.topTags)
            }

            ReflectionSection(
                value = state.winNote,
                onValueChange = onWinNoteChange,
                enabled = !state.isSubmitting
            )

            TomorrowGoalSection(
                value = state.tomorrowGoal,
                onValueChange = onTomorrowGoalChange,
                enabled = !state.isSubmitting
            )

            Text(
                text = stringResource(Res.string.day_review_leftovers_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            val leftoverItems = remember(state.summary) {
                state.summary.plannedItems + state.summary.alreadyCarriedItems
            }
            if (leftoverItems.isEmpty()) {
                Text(
                    text = stringResource(Res.string.day_review_leftovers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(
                        leftoverItems,
                        key = { _, item -> item.id }) { index, item ->
                        LeftoverReviewRow(
                            item = item,
                            action = state.actionFor(item),
                            enabled = !state.isSubmitting,
                            onAction = { onLeftoverAction(item.id, it) }
                        )
                        if (index < leftoverItems.lastIndex) {
                            AppHorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
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
        RichTextComposer(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(Res.string.day_review_win_note_placeholder),
            modifier = Modifier.fillMaxWidth(),
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
private fun TagInsightsRow(tags: List<DayReviewTagMinutes>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        tags.forEach { tag ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
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
private fun ReviewSummaryRow(state: DayReviewUiState) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(
            label = stringResource(Res.string.day_review_done_count, state.summary.doneCount)
        )
        SummaryChip(
            label = stringResource(Res.string.day_review_planned_count, state.summary.plannedCount)
        )
        SummaryChip(
            label = stringResource(
                Res.string.day_review_done_minutes,
                state.summary.doneMinutes
            )
        )
        if (state.streak > 0) {
            SummaryChip(
                label = "${state.streak}-day review streak"
            )
        }
    }
}

@Composable
private fun SummaryChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            DailyPlanTimelineCard(
                item = item,
                isOverdue = item.isOverdue(today())
            )
            FlowRow(
                modifier = Modifier.padding(start = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
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
}
