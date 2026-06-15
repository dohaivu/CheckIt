package com.checkit.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.weekly_digest_active_days
import checkit.shared.generated.resources.weekly_digest_busiest_day
import checkit.shared.generated.resources.weekly_digest_empty
import checkit.shared.generated.resources.weekly_digest_highlights
import checkit.shared.generated.resources.weekly_digest_top_tags
import checkit.shared.generated.resources.weekly_digest_total_items
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.ui.DigestHighlight
import com.checkit.ui.ReportUiState
import com.checkit.ui.TagReportItem
import com.checkit.ui.TimeReportItem
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.components.ReportPeriodHeader
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.shortName
import com.checkit.ui.tasks.cardColor
import com.checkit.ui.tasks.formatDuration
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

@Composable
internal fun DigestReport(
    state: ReportUiState,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPeriod = when (state.selectedPeriod) {
        ReportPeriod.Daily,
        ReportPeriod.Week -> state.selectedPeriod
        ReportPeriod.Month,
        ReportPeriod.Annual -> ReportPeriod.Week
    }
    val reportState = if (state.selectedPeriod == selectedPeriod) {
        state
    } else {
        state.copy(selectedPeriod = selectedPeriod)
    }
    val digest = reportState.digestReport
    LaunchedEffect(state.selectedPeriod) {
        if (state.selectedPeriod != selectedPeriod) {
            onPeriodSelected(selectedPeriod)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ReportPeriodHeader(
            selectedPeriod = selectedPeriod,
            selectedDate = state.selectedDate,
            onPeriodSelected = onPeriodSelected,
            onPreviousPeriod = onPreviousPeriod,
            onNextPeriod = onNextPeriod,
            onCurrentPeriod = onCurrentPeriod,
            periods = listOf(ReportPeriod.Daily, ReportPeriod.Week)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (digest.totalItemCount == 0) {
                EmptyDigestCard()
                if (selectedPeriod == ReportPeriod.Daily) {
                    WeeklyActivityChart(
                        items = digest.weekActivityItems,
                        selectedDate = state.selectedDate,
                        selectedPeriod = selectedPeriod
                    )
                }
            } else {
                HeroSummaryCard(
                    totalMinutes = digest.totalMinutes,
                    previousTotalMinutes = digest.previousTotalMinutes,
                    selectedPeriod = selectedPeriod,
                    trendItems = digest.trendItems,
                    progressItems = digest.progressItems,
                    doneCount = digest.doneItemCount,
                    plannedCount = digest.plannedItemCount
                )
                MetricGrid(
                    totalItemCount = digest.totalItemCount,
                    selectedPeriod = selectedPeriod,
                    activeDayCount = digest.activeDayCount,
                    busiestDay = digest.busiestDay
                )
                WeeklyActivityChart(
                    items = digest.weekActivityItems,
                    selectedDate = state.selectedDate,
                    selectedPeriod = selectedPeriod
                )
                if (digest.highlights.isNotEmpty()) {
                    CompletedHighlightsCard(
                        highlights = digest.highlights,
                        selectedPeriod = selectedPeriod
                    )
                }
                if (digest.topTags.isNotEmpty()) {
                    TopTagsCard(items = digest.topTags)
                }
            }
        }
    }
}

@Composable
private fun HeroSummaryCard(
    totalMinutes: Int,
    previousTotalMinutes: Int,
    selectedPeriod: ReportPeriod,
    trendItems: List<TimeReportItem>,
    progressItems: List<DailyPlanItem>,
    doneCount: Int,
    plannedCount: Int,
    modifier: Modifier = Modifier
) {
    val doneTotal = doneCount + plannedCount
    val trend = totalMinutes.trendSummary(previousTotalMinutes, selectedPeriod)
    val progressSegments = remember(progressItems) {
        progressItems.toProgressRingSegments()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ReportBlue.copy(alpha = 0.13f),
                        ReportPink.copy(alpha = 0.11f),
                        ReportGreen.copy(alpha = 0.13f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 142.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Total Focus Time",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalMinutes.formatDuration(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = trend.color.copy(alpha = 0.13f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = trend.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = trend.color
                        )
                    }
                }
                MiniTrendLine(
                    items = trendItems,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                )
            }
            ProgressRing(
                segments = progressSegments,
                totalCount = doneTotal,
                centerText = "$doneCount/$doneTotal",
                modifier = Modifier.size(118.dp)
            )
        }
    }
}

@Composable
private fun MetricGrid(
    totalItemCount: Int,
    selectedPeriod: ReportPeriod,
    activeDayCount: Int,
    busiestDay: TimeReportItem?,
    modifier: Modifier = Modifier
) {
    val metrics = buildList {
        add(
            DashboardMetric(
                value = totalItemCount.toString(),
                label = stringResource(Res.string.weekly_digest_total_items),
                icon = Icons.Default.EventAvailable,
                accent = ReportBlue
            )
        )
        if (selectedPeriod == ReportPeriod.Week) {
            add(
                DashboardMetric(
                    value = "$activeDayCount/7",
                    label = stringResource(Res.string.weekly_digest_active_days),
                    icon = Icons.Default.CalendarMonth,
                    accent = ReportOrange
                )
            )
            add(
                DashboardMetric(
                    value = busiestDay?.startDate?.dayOfWeek?.shortName() ?: "-",
                    label = stringResource(Res.string.weekly_digest_busiest_day),
                    icon = Icons.Default.LocalFireDepartment,
                    accent = ReportPurple
                )
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowMetrics.forEach { metric ->
                    MetricCard(
                        metric = metric,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowMetrics.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    metric: DashboardMetric,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .heightIn(min = 116.dp)
            .aspectRatio(1.18f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = metric.accent.copy(alpha = 0.055f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    metric.accent.copy(alpha = 0.42f),
                    metric.accent.copy(alpha = 0.16f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(metric.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                    tint = Color.White
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WeeklyActivityChart(
    items: List<TimeReportItem>,
    selectedDate: LocalDate,
    selectedPeriod: ReportPeriod,
    modifier: Modifier = Modifier
) {
    val maxMinutes = items.maxOfOrNull { it.totalMinutes } ?: 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(154.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { item ->
                val selected = selectedPeriod == ReportPeriod.Daily && item.startDate == selectedDate
                ActivityBar(
                    item = item,
                    maxMinutes = maxMinutes,
                    selected = selected,
                    showValue = when (selectedPeriod) {
                        ReportPeriod.Daily -> selected
                        else -> item.totalMinutes == maxMinutes && maxMinutes > 0
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActivityBar(
    item: TimeReportItem,
    maxMinutes: Int,
    selected: Boolean,
    showValue: Boolean,
    modifier: Modifier = Modifier
) {
    val fraction = if (maxMinutes == 0) 0f else item.totalMinutes.toFloat() / maxMinutes.toFloat()
    val fillHeight = if (item.totalMinutes == 0) 8.dp else 88.dp * fraction.coerceIn(0.22f, 1f)
    val barColor = if (selected) ReportBlue else ReportPurple
    val dayColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier.height(108.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (showValue) {
                Text(
                    text = item.totalMinutes.formatDuration(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = dayColor,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fillHeight)
                        .clip(CircleShape)
                        .background(barColor)
                )
            }
        }
        Text(
            text = item.startDate.dayOfWeek.shortName(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = dayColor,
            maxLines = 1
        )
    }
}

@Composable
private fun CompletedHighlightsCard(
    highlights: List<DigestHighlight>,
    selectedPeriod: ReportPeriod,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ReportBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
                Text(
                    text = stringResource(Res.string.weekly_digest_highlights),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            highlights.forEachIndexed { index, highlight ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
                CompletedHighlightRow(
                    highlight = highlight,
                    selectedPeriod = selectedPeriod
                )
            }
        }
    }
}

@Composable
private fun CompletedHighlightRow(
    highlight: DigestHighlight,
    selectedPeriod: ReportPeriod,
    modifier: Modifier = Modifier
) {
    val accent = highlight.item.cardColor()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = highlight.icon(),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = highlight.title.ifBlank { "Done item" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            val detail = listOfNotNull(
                highlight.date.localizedCompactDateWithDayName().takeIf { selectedPeriod == ReportPeriod.Week },
                highlight.note?.takeIf { it.isNotBlank() }?.takeIf { highlight.totalMinutes == 0 }
            ).joinToString(" - ")
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (highlight.totalMinutes > 0) {
            Text(
                text = highlight.totalMinutes.formatDuration(),
                modifier = Modifier.widthIn(min = 58.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                color = accent
            )
        }
    }
}

@Composable
private fun ProgressRing(
    segments: List<ProgressRingSegment>,
    totalCount: Int,
    centerText: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 15.dp
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    val centerColor = segments.firstOrNull { it.completed }?.color ?: MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (totalCount > 0) {
                var startAngle = -90f
                val gapDegrees = if (segments.size > 1) 4f else 0f
                segments.forEach { segment ->
                    val sweepAngle = 360f * segment.count.toFloat() / totalCount.toFloat()
                    val visibleSweep = (sweepAngle - gapDegrees).coerceAtLeast(1f)
                    drawArc(
                        color = if (segment.completed) segment.color else segment.color.copy(alpha = 0.28f),
                        startAngle = startAngle,
                        sweepAngle = visibleSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = centerColor
            )
            Text(
                text = "Done",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniTrendLine(
    items: List<TimeReportItem>,
    modifier: Modifier = Modifier
) {
    val totals = remember(items) {
        items.map { it.totalMinutes }
    }
    val maxMinutes = remember(totals) {
        totals.maxOrNull() ?: 0
    }
    val minMinutes = remember(totals) {
        totals.minOrNull() ?: 0
    }
    val range = remember(maxMinutes, minMinutes) {
        (maxMinutes - minMinutes).coerceAtLeast(1)
    }

    Canvas(modifier = modifier) {
        if (totals.isEmpty()) return@Canvas

        val horizontalStep = if (totals.size == 1) 0f else size.width / (totals.lastIndex).toFloat()
        val points = totals.mapIndexed { index, minutes ->
            val normalized = if (maxMinutes == minMinutes) {
                0.5f
            } else {
                (minutes - minMinutes).toFloat() / range.toFloat()
            }
            Offset(
                x = horizontalStep * index,
                y = size.height * (0.82f - normalized * 0.66f)
            )
        }
        for (index in 0 until points.lastIndex) {
            drawLine(
                color = ReportBlue,
                start = points[index],
                end = points[index + 1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCircle(
            color = ReportBlue,
            radius = 7.dp.toPx(),
            center = points.last()
        )
    }
}

@Composable
private fun TopTagsCard(
    items: List<TagReportItem>,
    modifier: Modifier = Modifier
) {
    val maxMinutes = items.maxOfOrNull { it.totalMinutes } ?: 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.weekly_digest_top_tags),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            items.forEach { tag ->
                TagReportBarRow(
                    item = tag,
                    fraction = if (maxMinutes == 0) 0f else tag.totalMinutes.toFloat() / maxMinutes.toFloat()
                )
            }
        }
    }
}

@Composable
private fun EmptyDigestCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Text(
            text = stringResource(Res.string.weekly_digest_empty),
            modifier = Modifier.padding(22.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DigestHighlight.icon(): ImageVector = when {
    title.contains("code", ignoreCase = true) -> Icons.Default.Code
    title.contains("anki", ignoreCase = true) ||
        title.contains("spanish", ignoreCase = true) -> Icons.AutoMirrored.Filled.MenuBook
    item.source == DailyPlanItemSource.ExistingTask -> Icons.Default.TaskAlt
    else -> Icons.Default.EventAvailable
}

private fun List<DailyPlanItem>.toProgressRingSegments(): List<ProgressRingSegment> =
    progressSegmentsFor(DailyPlanItemStatus.Done, completed = true) +
        progressSegmentsFor(DailyPlanItemStatus.Planned, completed = false)

private fun List<DailyPlanItem>.progressSegmentsFor(
    status: DailyPlanItemStatus,
    completed: Boolean
): List<ProgressRingSegment> =
    filter { it.status == status }
        .groupBy { it.cardColor() }
        .map { (color, items) ->
            ProgressRingSegment(
                color = color,
                count = items.size,
                completed = completed
            )
        }
        .sortedByDescending { it.count }

private fun Int.trendSummary(
    previousTotalMinutes: Int,
    selectedPeriod: ReportPeriod
): TrendSummary {
    val comparisonLabel = if (selectedPeriod == ReportPeriod.Week) "last week" else "yesterday"
    return when {
        previousTotalMinutes == 0 && this == 0 -> TrendSummary(
            label = "No change vs $comparisonLabel",
            color = ReportMuted
        )
        previousTotalMinutes == 0 -> TrendSummary(
            label = "New focus vs $comparisonLabel",
            color = ReportGreenDark
        )
        else -> {
            val change = ((this - previousTotalMinutes).toFloat() / previousTotalMinutes.toFloat() * 100f).toInt()
            val prefix = when {
                change > 0 -> "Up"
                change < 0 -> "Down"
                else -> "Even"
            }
            TrendSummary(
                label = "$prefix ${abs(change)}% vs $comparisonLabel",
                color = when {
                    change > 0 -> ReportGreenDark
                    change < 0 -> ReportPink
                    else -> ReportMuted
                }
            )
        }
    }
}

private data class TrendSummary(
    val label: String,
    val color: Color
)

private data class ProgressRingSegment(
    val color: Color,
    val count: Int,
    val completed: Boolean
)

private data class DashboardMetric(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

private val ReportBlue = Color(0xFF3E72F2)
private val ReportPurple = Color(0xFF7B5CF0)
private val ReportGreen = Color(0xFF2EC995)
private val ReportGreenDark = Color(0xFF0E9F73)
private val ReportOrange = Color(0xFFFF8A24)
private val ReportPink = Color(0xFFF05AA6)
private val ReportMuted = Color(0xFF667085)
