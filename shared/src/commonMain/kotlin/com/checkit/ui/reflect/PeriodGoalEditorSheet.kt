package com.checkit.ui.reflect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastJoinToString
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.reflect_review_card_title
import checkit.shared.generated.resources.reflect_review_save
import com.checkit.domain.MetricItem
import com.checkit.domain.Period
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.MarkdownTextField
import com.checkit.ui.components.MetricsSection
import com.checkit.ui.components.RatingBar
import com.checkit.ui.components.icons.AppIcons
import com.checkit.ui.components.icons.Target
import com.checkit.ui.periodDetail
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PeriodGoalEditorSheet(
    editor: ReflectGoalEditorState,
    onReviewChange: (String) -> Unit,
    onGoalChange: (String) -> Unit,
    onRatingChange: (Float) -> Unit,
    onMetricsChange: (List<MetricItem>) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val periodLabel = editor.focus.periodDetail()
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        sheetGesturesEnabled = false,
        modifier = Modifier
            .fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (editor.mode == ReflectGoalEditorMode.GoalOnly) AppIcons.Target else editor.focus.period.reviewIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (editor.mode == ReflectGoalEditorMode.GoalOnly) {
                            "${periodLabel.uppercase()} GOAL"
                        } else {
                            stringResource(Res.string.reflect_review_card_title, periodLabel).uppercase()
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (editor.mode == ReflectGoalEditorMode.Full) {
                Text(
                    text = ReflectionPrompts.get(editor.focus.period).reviewText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                MarkdownTextField(
                    value = editor.review,
                    onValueChange = onReviewChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    placeholder = "Jot down your reflection ...",
                    minLines = 6,
                    enabled = !editor.isSaving
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    RatingBar(
                        rating = editor.rating,
                        onRatingChange = onRatingChange,
                        enabled = !editor.isSaving,
                        modifier = Modifier
                            .width(150.dp)
                            .height(32.dp)
                    )
                }
            }

            if (editor.mode == ReflectGoalEditorMode.Full) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Target,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "GOAL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (editor.mode == ReflectGoalEditorMode.GoalOnly) {
                editor.parentGoal?.goal?.takeIf { it.isNotBlank() }?.let { parentGoalText ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "PARENT GOAL · ${editor.parentGoal.periodDetail().uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .fillMaxHeight()
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = parentGoalText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }

            Text(
                text = ReflectionPrompts.get(editor.focus.period).goalText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            MarkdownTextField(
                value = editor.goal,
                onValueChange = onGoalChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                placeholder = "What will you focus on?",
                minLines = 5,
                enabled = !editor.isSaving
            )

            MetricsSection(
                metrics = editor.metrics,
                enabled = !editor.isSaving,
                onMetricsChange = onMetricsChange
            )

            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSave, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.reflect_review_save))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

internal data class ReflectionPrompt(
    val reviewSections: List<String>,
    val goalPrompts: List<String>
) {
    val reviewText: String get() = reviewSections.fastJoinToString("\n")
    val goalText: String get() = goalPrompts.fastJoinToString("\n")
}

internal object ReflectionPrompts {
    fun get(period: Period): ReflectionPrompt = when (period) {
        Period.Day -> ReflectionPrompt(
            reviewSections = listOf(
                "What were your wins?",
                "Where did you feel friction? What distracted you? How can I prevent it tomorrow?",
            ),
            goalPrompts = listOf(
                "What can I make slightly better today? Why? If/When-Then",
                "How you want to be?"
            )
        )
        Period.Week -> ReflectionPrompt(
            reviewSections = listOf(
                "What was completed vs. planned this week?",
                "Which tactics worked well? What didn't work?",
                "What needs to change next week?"
            ),
            goalPrompts = listOf("What are the 3 primary objectives for this week?")
        )
        Period.Month -> ReflectionPrompt(
            reviewSections = listOf(
                "What significant goals met or personal highlights achieved this month?",
                "How is your big-picture progress? Review active quarterly/yearly goals.",
                "What macro trends are you noticing? What recurring friction or bottlenecks appeared?"
            ),
            goalPrompts = listOf("What are the 3 key milestones for this month?")
        )
        Period.Quarter -> ReflectionPrompt(
            reviewSections = listOf(
                "What are the quarterly trends?",
                "How is the macro progress?"
            ),
            goalPrompts = listOf("What are the 3 key results for this quarter?")
        )
        Period.Year -> ReflectionPrompt(
            reviewSections = listOf(
                "What are your biggest annual achievements?",
                "How have you progressed toward your long-term vision?"
            ),
            goalPrompts = listOf("What are the 3 most important goals for the coming year?")
        )
    }
}
