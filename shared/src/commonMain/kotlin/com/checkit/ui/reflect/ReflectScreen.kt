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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.reflect_children_checkins
import checkit.shared.generated.resources.reflect_children_day_plan
import checkit.shared.generated.resources.reflect_day_empty_plan
import checkit.shared.generated.resources.reflect_period_day
import checkit.shared.generated.resources.reflect_period_month
import checkit.shared.generated.resources.reflect_period_week
import checkit.shared.generated.resources.reflect_period_year
import checkit.shared.generated.resources.reflect_history_empty
import checkit.shared.generated.resources.reflect_history_title
import checkit.shared.generated.resources.reflect_stats_done
import checkit.shared.generated.resources.reflect_stats_entries
import checkit.shared.generated.resources.reflect_stats_minutes
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
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewPeriod
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.components.ReportPeriodHeader
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.localizedMonthTitle
import com.checkit.ui.localizedShortMonthName
import com.checkit.ui.myday.doneWorkMinutes
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
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
                    StatsStrip(stats = state.stats)
                    ChildrenStrip(
                        state = state,
                        onZoomInTo = viewModel::zoomInTo
                    )
                    ReviewCard(
                        state = state,
                        onOpenEditor = viewModel::openEditor,
                        onGenerateDraft = viewModel::generateDraft
                    )
                    DigestCards(
                        digest = digest,
                        selectedDate = state.selectedDate,
                        selectedPeriod = state.selectedPeriod
                    )
                    HistorySection(
                        history = state.history,
                        onOpenReview = viewModel::openReview
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsStrip(stats: PeriodStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatsChip(stringResource(Res.string.reflect_stats_done, stats.doneCount))
        StatsChip(stringResource(Res.string.reflect_stats_minutes, stats.totalMinutes))
        StatsChip(stringResource(Res.string.reflect_stats_entries, stats.journalCount))
    }
}

@Composable
private fun StatsChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ChildrenStrip(
    state: ReflectUiState,
    onZoomInTo: (LocalDate) -> Unit
) {
    when (state.focus.period) {
        ReviewPeriod.Day -> DayHighlights(state)
        ReviewPeriod.Week -> WeekChildren(state, onZoomInTo)
        ReviewPeriod.Month -> MonthChildren(state, onZoomInTo)
        ReviewPeriod.Year -> YearChildren(state, onZoomInTo)
    }
}

@Composable
private fun WeekChildren(
    state: ReflectUiState,
    onZoomInTo: (LocalDate) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        state.children.forEach { child ->
            val hasReview = state.hasReview(child)
            val minutes = state.dailyPlans
                .firstOrNull { it.date.toEpochDays().toInt() == child.start.toEpochDays().toInt() }
                ?.doneWorkMinutes() ?: 0
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { onZoomInTo(child.start) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = child.start.dayOfWeek.name.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = child.start.day.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (hasReview) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                    )
                } else if (minutes > 0) {
                    Text(
                        text = "${minutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthChildren(
    state: ReflectUiState,
    onZoomInTo: (LocalDate) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.children) { child ->
            ChildChip(
                label = "${child.start.localizedShortMonthName()} ${child.start.day}",
                hasReview = state.hasReview(child),
                onClick = { onZoomInTo(child.start) }
            )
        }
    }
}

@Composable
private fun YearChildren(
    state: ReflectUiState,
    onZoomInTo: (LocalDate) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.children) { child ->
            ChildChip(
                label = child.start.localizedShortMonthName(),
                hasReview = state.hasReview(child),
                onClick = { onZoomInTo(child.start) }
            )
        }
    }
}

@Composable
private fun ChildChip(
    label: String,
    hasReview: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        if (hasReview) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun DayHighlights(state: ReflectUiState) {
    val dayStart = state.focus.start
    val dayEpoch = dayStart.toEpochDays().toInt()
    val plan = state.dailyPlans.firstOrNull { it.date.toEpochDays().toInt() == dayEpoch }
    val doneItems = plan?.items.orEmpty().filter { it.status == DailyPlanItemStatus.Done }
    val dayJournals = state.journalEntries.filter { it.dateEpochDays == dayEpoch }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.reflect_children_day_plan),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (doneItems.isEmpty()) {
                Text(
                    text = stringResource(Res.string.reflect_day_empty_plan),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                doneItems.take(5).forEach { item ->
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (dayJournals.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.reflect_children_checkins, dayJournals.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                dayJournals.take(3).forEach { entry ->
                    Text(
                        text = entry.content,
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
private fun HistorySection(
    history: List<PeriodReview>,
    onOpenReview: (PeriodReview) -> Unit
) {
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
            Text(
                text = stringResource(Res.string.reflect_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (history.isEmpty()) {
                Text(
                    text = stringResource(Res.string.reflect_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                history.forEachIndexed { index, review ->
                    if (index > 0) HorizontalDivider()
                    HistoryRow(review = review, onClick = { onOpenReview(review) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    review: PeriodReview,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Notes,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${review.period.label()} · ${review.rangeLabel()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (review.content.isNotBlank()) {
                Text(
                    text = review.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
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
