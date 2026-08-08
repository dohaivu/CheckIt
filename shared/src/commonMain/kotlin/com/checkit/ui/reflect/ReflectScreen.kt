package com.checkit.ui.reflect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.reflect_period_day
import checkit.shared.generated.resources.reflect_period_month
import checkit.shared.generated.resources.reflect_period_week
import checkit.shared.generated.resources.reflect_period_year
import checkit.shared.generated.resources.reflect_reviews_empty
import checkit.shared.generated.resources.reflect_reviews_subtitle
import checkit.shared.generated.resources.reflect_reviews_title
import checkit.shared.generated.resources.reflect_reviews_written
import checkit.shared.generated.resources.reflect_review_status_complete
import checkit.shared.generated.resources.reflect_review_status_draft
import checkit.shared.generated.resources.reflect_zoom_in
import checkit.shared.generated.resources.reflect_zoom_out
import checkit.shared.generated.resources.reflect_review_card_title
import checkit.shared.generated.resources.reflect_review_content_label
import checkit.shared.generated.resources.reflect_review_content_placeholder
import checkit.shared.generated.resources.reflect_review_draft_note
import checkit.shared.generated.resources.reflect_review_edit
import checkit.shared.generated.resources.reflect_review_empty
import checkit.shared.generated.resources.reflect_review_generate_draft
import checkit.shared.generated.resources.reflect_review_intent_label
import checkit.shared.generated.resources.reflect_review_intent_placeholder
import checkit.shared.generated.resources.reflect_review_save
import checkit.shared.generated.resources.reflect_review_write
import checkit.shared.generated.resources.tab_reflect
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewPeriod
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.components.ReportPeriodHeader
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.localizedMonthTitle
import com.checkit.ui.localizedShortMonthName
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private val ReflectPeriods = listOf(
    ReportPeriod.Daily,
    ReportPeriod.Week,
    ReportPeriod.Month,
    ReportPeriod.Annual,
    ReportPeriod.Habit
)

@Composable
internal fun ReflectScreen(
    state: ReflectUiState,
    viewModel: ReflectViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.tab_reflect),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = viewModel::zoomIn) {
                        Icon(Icons.Default.ZoomIn, contentDescription = stringResource(Res.string.reflect_zoom_in))
                    }
                    IconButton(onClick = viewModel::zoomOut) {
                        Icon(Icons.Default.ZoomOut, contentDescription = stringResource(Res.string.reflect_zoom_out))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 12.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportPeriodHeader(
                selectedPeriod = state.selectedPeriod,
                selectedDate = state.selectedDate,
                onPeriodSelected = viewModel::selectPeriod,
                onPreviousPeriod = viewModel::previousPeriod,
                onNextPeriod = viewModel::nextPeriod,
                onCurrentPeriod = viewModel::resetToCurrentPeriod,
                onZoomOutTo = viewModel::zoomOutTo,
                periods = ReflectPeriods
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                when (state.selectedPeriod) {
                    ReportPeriod.Habit -> {
                        val checkins = state.habitCheckins
                        if (checkins.isEmpty()) {
                            EmptyHabitsCard()
                        } else {
                            HabitHeatmapSection(checkins = checkins, monthCount = 2)
                        }
                    }

                    ReportPeriod.Daily,
                    ReportPeriod.Week,
                    ReportPeriod.Month,
                    ReportPeriod.Annual -> {
                        val digest = remember(state.selectedPeriod, state.selectedDate, state.dailyPlans) {
                            state.digestReport
                        }
                        HeroSummaryCard(
                            totalMinutes = digest.totalMinutes,
                            previousTotalMinutes = digest.previousTotalMinutes,
                            selectedPeriod = state.selectedPeriod,
                            trendItems = digest.trendItems,
                            progressItems = digest.progressItems,
                            doneCount = digest.doneItemCount,
                            plannedCount = digest.plannedItemCount,
                            journalCount = digest.journalCount
                        )
                        ActivityChart(
                            items = digest.activityItems,
                            selectedDate = state.selectedDate,
                            selectedPeriod = state.selectedPeriod,
                            onZoomInTo = viewModel::zoomInTo
                        )
                        if (digest.topTags.isNotEmpty()) {
                            TopTagsCard(items = digest.topTags)
                        }
                        if (digest.highlights.isNotEmpty()) {
                            CompletedHighlightsCard(
                                highlights = digest.highlights,
                                selectedPeriod = state.selectedPeriod
                            )
                        }
                        ReviewCard(
                            state = state,
                            onOpenEditor = viewModel::openEditor,
                            onGenerateDraft = viewModel::generateDraft
                        )
                        ReviewsSection(
                            reviews = state.reviewsForSelectedPeriod,
                            selectedPeriod = state.selectedPeriod,
                            onOpenReview = viewModel::openReview
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    state: ReflectUiState,
    onOpenEditor: () -> Unit,
    onGenerateDraft: () -> Unit
) {
    val review = state.focusReview
    val periodLabel = state.focus.period.label()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.reflect_review_card_title, periodLabel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenEditor) {
                    Text(
                        text = stringResource(
                            if (review?.isComplete == true) {
                                Res.string.reflect_review_edit
                            } else {
                                Res.string.reflect_review_write
                            }
                        )
                    )
                }
            }
            if (review == null) {
                Text(
                    text = stringResource(Res.string.reflect_review_empty, periodLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onGenerateDraft) {
                    Text(stringResource(Res.string.reflect_review_generate_draft))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (review.content.isNotBlank()) {
                        Text(
                            text = review.content,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    review.intentNext?.takeIf { it.isNotBlank() }?.let { intent ->
                        Text(
                            text = intent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewsSection(
    reviews: List<PeriodReview>,
    selectedPeriod: ReportPeriod,
    onOpenReview: (PeriodReview) -> Unit
) {
    val periodLabel = selectedPeriod.toReviewPeriod().label()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.reflect_reviews_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.reflect_reviews_subtitle, periodLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (reviews.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = stringResource(Res.string.reflect_reviews_written, reviews.size),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (reviews.isEmpty()) {
                ReviewsEmptyState(periodLabel = periodLabel)
            } else {
                reviews.forEachIndexed { index, review ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    }
                    ReviewRow(review = review, onClick = { onOpenReview(review) })
                }
            }
        }
    }
}

@Composable
private fun ReviewsEmptyState(periodLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(Res.string.reflect_reviews_empty, periodLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReviewRow(
    review: PeriodReview,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = review.period.reviewIcon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = review.rangeLabel(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ReviewStatusPill(review = review)
            }
            if (review.content.isNotBlank()) {
                Text(
                    text = review.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            review.intentNext?.takeIf { it.isNotBlank() }?.let { intent ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = intent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ReviewStatusPill(review: PeriodReview) {
    val complete = review.isComplete
    Surface(
        color = if (complete) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        shape = CircleShape
    ) {
        Text(
            text = stringResource(
                if (complete) {
                    Res.string.reflect_review_status_complete
                } else {
                    Res.string.reflect_review_status_draft
                }
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (complete) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            }
        )
    }
}

@Composable
private fun ReviewPeriod.reviewIcon(): ImageVector = when (this) {
    ReviewPeriod.Day -> Icons.Default.Star
    ReviewPeriod.Week -> Icons.Default.DateRange
    ReviewPeriod.Month -> Icons.Default.CalendarMonth
    ReviewPeriod.Year -> Icons.Default.EmojiEvents
}

@Composable
private fun PeriodReview.rangeLabel(): String = when (period) {
    ReviewPeriod.Day -> periodStartDate.localizedCompactDateWithDayName()
    ReviewPeriod.Week -> "${periodStartDate.localizedShortMonthName()} ${periodStartDate.day}"
    ReviewPeriod.Month -> periodStartDate.localizedMonthTitle()
    ReviewPeriod.Year -> periodStartDate.year.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodReviewEditorSheet(
    editor: ReflectReviewEditorState,
    onContentChange: (String) -> Unit,
    onIntentNextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val periodLabel = editor.focus.period.label()
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        sheetGesturesEnabled = !editor.isSaving
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.reflect_review_card_title, periodLabel),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (editor.isDraft) {
                Text(
                    text = stringResource(Res.string.reflect_review_draft_note, periodLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedTextField(
                value = editor.content,
                onValueChange = onContentChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.reflect_review_content_label)) },
                placeholder = { Text(stringResource(Res.string.reflect_review_content_placeholder)) },
                minLines = 4,
                enabled = !editor.isSaving
            )
            OutlinedTextField(
                value = editor.intentNext,
                onValueChange = onIntentNextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.reflect_review_intent_label)) },
                placeholder = { Text(stringResource(Res.string.reflect_review_intent_placeholder)) },
                minLines = 2,
                enabled = !editor.isSaving
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onSave, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.reflect_review_save))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReviewPeriod.label(): String = when (this) {
    ReviewPeriod.Day -> stringResource(Res.string.reflect_period_day)
    ReviewPeriod.Week -> stringResource(Res.string.reflect_period_week)
    ReviewPeriod.Month -> stringResource(Res.string.reflect_period_month)
    ReviewPeriod.Year -> stringResource(Res.string.reflect_period_year)
}
