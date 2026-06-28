package com.checkit.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.weekly_digest_empty
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.components.ReportPeriodHeader
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.shortName
import com.checkit.ui.tasks.cardColor
import com.checkit.ui.tasks.toDurationLabel
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
            periods = DigestReportPeriods
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
                WeeklyActivityChart(
                    items = digest.weekActivityItems,
                    selectedDate = state.selectedDate,
                    selectedPeriod = selectedPeriod
                )
                if (digest.topTags.isNotEmpty()) {
                    TopTagsCard(items = digest.topTags)
                }
                if (digest.highlights.isNotEmpty()) {
                    CompletedHighlightsCard(
                        highlights = digest.highlights,
                        selectedPeriod = selectedPeriod
                    )
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
    val trend = remember(totalMinutes, previousTotalMinutes, selectedPeriod) {
        totalMinutes.trendSummary(previousTotalMinutes, selectedPeriod)
    }
    val encouragement = remember(doneCount, plannedCount, selectedPeriod) {
        heroEncouragement(doneCount, plannedCount, selectedPeriod)
    }
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
                        text = "You invested",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalMinutes.toDurationLabel(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = encouragement,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
private fun WeeklyActivityChart(
    items: List<TimeReportItem>,
    selectedDate: LocalDate,
    selectedPeriod: ReportPeriod,
    modifier: Modifier = Modifier
) {
    val maxMinutes = remember(items) { items.maxOfOrNull { it.totalMinutes } ?: 0 }
    val subtitle = remember(selectedPeriod) { activityChartSubtitle(selectedPeriod) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = sectionTitle(prefix = "Your ", emphasis = "rhythm", accent = ReportPurple),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp),
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
}

private fun activityChartSubtitle(selectedPeriod: ReportPeriod): AnnotatedString =
    buildAnnotatedString {
        if (selectedPeriod == ReportPeriod.Daily) {
            append("A gentle view of the ")
            highlight("week", ReportPurple)
            append(" around this day.")
        } else {
            append("Days you ")
            highlight("showed up", ReportPurple)
            append(", even when it was ")
            softEmphasis("just a little")
            append(".")
        }
    }

private val DigestReportPeriods = listOf(ReportPeriod.Daily, ReportPeriod.Week)

@Composable
private fun ActivityBar(
    item: TimeReportItem,
    maxMinutes: Int,
    selected: Boolean,
    showValue: Boolean,
    modifier: Modifier = Modifier
) {
    val fraction = if (maxMinutes == 0) 0f else item.totalMinutes.toFloat() / maxMinutes.toFloat()
    val fillHeight = if (item.totalMinutes == 0) 0.dp else 88.dp * fraction.coerceIn(0.22f, 1f)
    val barColor = if (selected) ReportBlue else ReportPurple
    val dayColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (showValue) {
            Text(
                text = item.totalMinutes.toDurationLabel(compact = true),
                modifier = Modifier
                    .padding(bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium,
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = sectionTitle(
                            prefix = "",
                            emphasis = "Wins",
                            suffix = " you can feel good about",
                            accent = ReportBlue
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = highlightsSubtitle(selectedPeriod),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

private fun highlightsSubtitle(selectedPeriod: ReportPeriod): AnnotatedString =
    buildAnnotatedString {
        append("A few ")
        highlight("finished moments", ReportBlue)
        append(if (selectedPeriod == ReportPeriod.Week) " from this week." else " from today.")
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
                text = highlight.totalMinutes.toDurationLabel(),
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = sectionTitle(
                        prefix = "Where your ",
                        emphasis = "energy",
                        suffix = " went",
                        accent = ReportGreenDark
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = energySubtitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

private fun DigestHighlight.icon(): ImageVector = when (item.source) {
    DailyPlanItemSource.MyDayTask -> Icons.Default.EventAvailable
    DailyPlanItemSource.MyDayNote -> Icons.AutoMirrored.Filled.EventNote
    DailyPlanItemSource.MyDayReminder -> Icons.Default.Schedule
    DailyPlanItemSource.ExistingTask -> Icons.Default.TaskAlt
}

private fun heroEncouragement(
    doneCount: Int,
    plannedCount: Int,
    selectedPeriod: ReportPeriod
): AnnotatedString {
    val period = if (selectedPeriod == ReportPeriod.Week) "this week" else "today"
    return buildAnnotatedString {
        when {
            doneCount > 0 -> {
                append("You finished ")
                highlight(doneCount.itemCountLabel(), ReportBlue)
                append(" $period. ")
                highlight("That is real progress.", ReportGreenDark, fontStyle = FontStyle.Italic)
            }
            plannedCount > 0 -> {
                append("You made a ")
                highlight("plan", ReportBlue)
                append(" $period. ")
                highlight("That is the first move.", ReportGreenDark, fontStyle = FontStyle.Italic)
            }
            else -> {
                append("You gave your day ")
                highlight("some shape", ReportBlue)
                append(". ")
                highlight("Keep going.", ReportGreenDark, fontStyle = FontStyle.Italic)
            }
        }
    }
}

private fun sectionTitle(
    prefix: String,
    emphasis: String,
    accent: Color,
    suffix: String = ""
): AnnotatedString =
    buildAnnotatedString {
        append(prefix)
        highlight(emphasis, accent)
        append(suffix)
    }

private fun energySubtitle(): AnnotatedString =
    buildAnnotatedString {
        append("The areas you ")
        highlight("gave time to", ReportGreenDark)
        append(".")
    }

private fun AnnotatedString.Builder.highlight(
    text: String,
    color: Color,
    fontWeight: FontWeight = FontWeight.Bold,
    fontStyle: FontStyle? = null
) {
    withStyle(
        SpanStyle(
            color = color,
            fontWeight = fontWeight,
            fontStyle = fontStyle
        )
    ) {
        append(text)
    }
}

private fun AnnotatedString.Builder.softEmphasis(text: String) {
    withStyle(
        SpanStyle(
            color = ReportMuted,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic
        )
    ) {
        append(text)
    }
}

private fun Int.itemCountLabel(): String =
    "$this ${if (this == 1) "thing" else "things"}"

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

private val ReportBlue = Color(0xFF3E72F2)
private val ReportPurple = Color(0xFF7B5CF0)
private val ReportGreen = Color(0xFF2EC995)
private val ReportGreenDark = Color(0xFF0E9F73)
private val ReportPink = Color(0xFFF05AA6)
private val ReportMuted = Color(0xFF667085)
