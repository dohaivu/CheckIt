package com.checkit.ui.myday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import checkit.shared.generated.resources.day_review_finish_and_report
import checkit.shared.generated.resources.day_review_leftovers_empty
import checkit.shared.generated.resources.day_review_leftovers_title
import checkit.shared.generated.resources.day_review_planned_count
import checkit.shared.generated.resources.day_review_title
import checkit.shared.generated.resources.day_review_win_note_label
import checkit.shared.generated.resources.day_review_win_note_placeholder
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.LeftoverAction
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayReviewSheet(
    state: DayReviewUiState,
    onDismiss: () -> Unit,
    onLeftoverAction: (Long, LeftoverAction) -> Unit,
    onWinNoteChange: (String) -> Unit,
    onConfirm: (openReportAfter: Boolean) -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.day_review_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            ReviewSummaryRow(state)
            Text(
                text = stringResource(Res.string.day_review_leftovers_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (state.summary.plannedItems.isEmpty()) {
                Text(
                    text = stringResource(Res.string.day_review_leftovers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(state.summary.plannedItems, key = { it.id }) { item ->
                        LeftoverReviewRow(
                            item = item,
                            action = state.actionFor(item.id),
                            enabled = !state.isSubmitting,
                            onAction = { onLeftoverAction(item.id, it) }
                        )
                    }
                }
            }
            Text(
                text = stringResource(Res.string.day_review_win_note_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppOutlinedTextField(
                value = state.winNote,
                onValueChange = onWinNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.day_review_win_note_placeholder),
                minLines = 2,
                maxLines = 4,
                enabled = !state.isSubmitting
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.isSubmitting
                ) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = { onConfirm(true) },
                    enabled = !state.isSubmitting
                ) {
                    Text(stringResource(Res.string.day_review_finish_and_report))
                }
                Button(
                    onClick = { onConfirm(false) },
                    enabled = !state.isSubmitting
                ) {
                    Text(stringResource(Res.string.day_review_finish))
                }
            }
        }
    }
}

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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LeftoverAction.entries.forEach { option ->
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
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
