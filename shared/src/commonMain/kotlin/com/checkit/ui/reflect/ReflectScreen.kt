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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.calendar_open_review
import checkit.shared.generated.resources.reflect_period_day
import checkit.shared.generated.resources.reflect_period_month
import checkit.shared.generated.resources.reflect_period_week
import checkit.shared.generated.resources.reflect_period_year
import checkit.shared.generated.resources.reflect_review_card_title
import checkit.shared.generated.resources.reflect_review_empty
import checkit.shared.generated.resources.reflect_review_status_draft
import checkit.shared.generated.resources.reflect_reviews_empty
import checkit.shared.generated.resources.reflect_reviews_subtitle
import checkit.shared.generated.resources.reflect_reviews_title
import checkit.shared.generated.resources.reflect_reviews_written
import checkit.shared.generated.resources.tab_reflect
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewPeriod
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.components.ReportPeriodHeader
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.components.parseMarkdownToAnnotatedString
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.localizedMonthTitle
import com.checkit.ui.localizedShortMonthName
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
                        val digest = state.digestReport
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

                        ReviewCard(
                            state = state,
                            onOpenEditor = viewModel::openEditor
                        )

                        if (digest.topTags.isNotEmpty()) {
                            TopTagsCard(items = digest.topTags)
                        }

                        ActivityChart(
                            items = digest.activityItems,
                            selectedDate = state.selectedDate,
                            selectedPeriod = state.selectedPeriod,
                            onZoomInTo = viewModel::zoomInTo
                        )

                        if (digest.highlights.isNotEmpty()) {
                            CompletedHighlightsCard(
                                highlights = digest.highlights,
                                selectedPeriod = state.selectedPeriod
                            )
                        }

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
    onOpenEditor: () -> Unit
) {
    val review = state.focusReview
    val periodLabel = state.focus.period.label()

    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient, RoundedCornerShape(24.dp))
            .clickable(onClick = onOpenEditor)
            .padding(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = state.focus.period.reviewIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.reflect_review_card_title, periodLabel).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = stringResource(Res.string.calendar_open_review),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }

            if (review == null || review.content.isBlank()) {
                Text(
                    text = stringResource(Res.string.reflect_review_empty, periodLabel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            } else {
                Text(
                    text = review.annotatedContent,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                review.intentNext?.takeIf { it.isNotBlank() }?.let { intent ->
                    Column (
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "NEXT FOCUS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = remember { parseMarkdownToAnnotatedString(intent) },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = stringResource(Res.string.reflect_reviews_title).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                Text(
                    text = stringResource(Res.string.reflect_reviews_subtitle, periodLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            if (reviews.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.reflect_reviews_written, reviews.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        if (reviews.isEmpty()) {
            ReviewsEmptyState(periodLabel = selectedPeriod.childReviewPeriod().label())
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                reviews.forEach { review ->
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
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Notes,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        Text(
            text = stringResource(Res.string.reflect_reviews_empty, periodLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
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
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = review.period.reviewIcon(),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = review.rangeLabel(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ReviewStatusPill(review = review)
            }
            if (review.content.isNotBlank()) {
                Text(
                    text = review.annotatedContent,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ReviewStatusPill(review: PeriodReview) {
    if (review.status == com.checkit.domain.ReviewStatus.Draft) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            shape = CircleShape
        ) {
            Text(
                text = stringResource(Res.string.reflect_review_status_draft),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
internal fun ReviewPeriod.reviewIcon(): ImageVector = when (this) {
    ReviewPeriod.Day -> Icons.Default.Star
    ReviewPeriod.Week -> Icons.Default.DateRange
    ReviewPeriod.Month -> Icons.Default.CalendarMonth
    ReviewPeriod.Year -> Icons.Default.EmojiEvents
}

@Composable
internal fun PeriodReview.rangeLabel(): String = when (period) {
    ReviewPeriod.Day -> periodStartDate.localizedCompactDateWithDayName()
    ReviewPeriod.Week -> "${periodStartDate.localizedShortMonthName()} ${periodStartDate.day}"
    ReviewPeriod.Month -> periodStartDate.localizedMonthTitle()
    ReviewPeriod.Year -> periodStartDate.year.toString()
}

@Composable
internal fun ReviewPeriod.label(): String = when (this) {
    ReviewPeriod.Day -> stringResource(Res.string.reflect_period_day)
    ReviewPeriod.Week -> stringResource(Res.string.reflect_period_week)
    ReviewPeriod.Month -> stringResource(Res.string.reflect_period_month)
    ReviewPeriod.Year -> stringResource(Res.string.reflect_period_year)
}
