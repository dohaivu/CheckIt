package com.checkit.ui.reflect

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.DailyPlanItemSource
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.components.getMoodColorFromEmoji
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.localizedShortMonthName
import com.checkit.ui.shortName
import com.checkit.ui.toDurationLabel
import com.checkit.ui.theme.toColor
import kotlinx.datetime.LocalDate
import kotlin.math.abs

@Composable
internal fun HeroSummaryCard(
    totalMinutes: Int,
    previousTotalMinutes: Int,
    selectedPeriod: ReportPeriod,
    trendItems: List<TimeReportItem>,
    doneCount: Int,
    plannedCount: Int,
    journalCount: Int,
    modifier: Modifier = Modifier
) {
    val doneTotal = doneCount + plannedCount
    val trend = remember(totalMinutes, previousTotalMinutes, selectedPeriod) {
        totalMinutes.trendSummary(previousTotalMinutes, selectedPeriod)
    }
    val encouragement = remember(doneCount, plannedCount, selectedPeriod, journalCount) {
        heroEncouragement(doneCount, plannedCount, selectedPeriod, journalCount)
    }
    val progressSegments = remember(doneCount, plannedCount) {
        listOf(
            ProgressRingSegment(
                color = ReportGreenDark,
                count = doneCount,
                completed = true
            ),
            ProgressRingSegment(
                color = ReportBlue,
                count = plannedCount,
                completed = false
            )
        ).filter { it.count > 0 }
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
                        minLines = 3,
                        maxLines = 5,
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
internal fun ActivityChart(
    items: List<TimeReportItem>,
    selectedDate: LocalDate,
    selectedPeriod: ReportPeriod,
    onZoomInTo: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxMinutes = remember(items) { items.maxOfOrNull { it.totalMinutes } ?: 0 }
    val subtitle = remember(selectedPeriod) { activityChartSubtitle(selectedPeriod) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ReportPurple.copy(alpha = 0.08f),
                        ReportPurple.copy(alpha = 0.03f)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = sectionTitle(prefix = "YOUR ", emphasis = "RHYTHM", accent = ReportPurple),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = ReportPurple
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
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
                        label = item.startDate.activityLabel(selectedPeriod),
                        onClick = { onZoomInTo(item.startDate) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun activityChartSubtitle(selectedPeriod: ReportPeriod): AnnotatedString =
    buildAnnotatedString {
        when (selectedPeriod) {
            ReportPeriod.Daily -> {
                append("A gentle view of the ")
                highlight("week", ReportPurple)
                append(" around this day.")
            }
            ReportPeriod.Week -> {
                append("Days you ")
                highlight("showed up", ReportPurple)
                append(", even when it was ")
                softEmphasis("just a little")
                append(".")
            }
            ReportPeriod.Month -> {
                append("Weeks you ")
                highlight("showed up", ReportPurple)
                append(" this month.")
            }
            ReportPeriod.Annual -> {
                append("Months you ")
                highlight("showed up", ReportPurple)
                append(" this year.")
            }
            ReportPeriod.Habit -> Unit
        }
    }

@Composable
private fun LocalDate.activityLabel(selectedPeriod: ReportPeriod): String = when (selectedPeriod) {
    ReportPeriod.Daily,
    ReportPeriod.Week -> dayOfWeek.shortName()
    ReportPeriod.Month -> day.toString()
    ReportPeriod.Annual -> localizedShortMonthName()
    ReportPeriod.Habit -> ""
}

@Composable
private fun ActivityBar(
    item: TimeReportItem,
    maxMinutes: Int,
    selected: Boolean,
    showValue: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = if (maxMinutes == 0) 0f else item.totalMinutes.toFloat() / maxMinutes.toFloat()
    val fillHeight = if (item.totalMinutes == 0) 0.dp else 72.dp * fraction.coerceIn(0.22f, 1f)
    val barColor = if (selected) ReportBlue else ReportPurple
    val dayColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (showValue) {
            Text(
                text = item.totalMinutes.toDurationLabel(compact = true),
                modifier = Modifier
                    .padding(bottom = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = dayColor,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
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
            text = label,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = dayColor,
            maxLines = 1
        )
    }
}

@Composable
internal fun CompletedHighlightsCard(
    highlights: List<DigestHighlight>,
    selectedPeriod: ReportPeriod,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ReportBlue.copy(alpha = 0.08f),
                        ReportBlue.copy(alpha = 0.03f)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(vertical = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = sectionTitle(
                            prefix = "YOUR ",
                            emphasis = "WINS",
                            suffix = "",
                            accent = ReportBlue
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = ReportBlue
                    )
                    Text(
                        text = highlightsSubtitle(selectedPeriod),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                highlights.forEach { highlight ->
                    CompletedHighlightRow(
                        highlight = highlight,
                        selectedPeriod = selectedPeriod
                    )
                }
            }
        }
    }
}

private fun highlightsSubtitle(selectedPeriod: ReportPeriod): AnnotatedString =
    buildAnnotatedString {
        append("A few ")
        highlight("finished moments", ReportBlue)
        append(" from ${periodRangeLabel(selectedPeriod)}.")
    }

private fun periodRangeLabel(selectedPeriod: ReportPeriod): String = when (selectedPeriod) {
    ReportPeriod.Daily -> "today"
    ReportPeriod.Week -> "this week"
    ReportPeriod.Month -> "this month"
    ReportPeriod.Annual -> "this year"
    ReportPeriod.Habit -> "today"
}

@Composable
private fun CompletedHighlightRow(
    highlight: DigestHighlight,
    selectedPeriod: ReportPeriod,
    modifier: Modifier = Modifier
) {
    val accent = if (highlight.isJournal) {
        highlight.moodEmoji?.let { getMoodColorFromEmoji(it) } ?: MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            val emoji = highlight.moodEmoji
            if (emoji != null) {
                Text(text = emoji, fontSize = 18.sp)
            } else {
                Icon(
                    imageVector = highlight.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accent
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = highlight.title.ifBlank { if (highlight.isJournal) "Check-In" else "Done item" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            val detail = listOfNotNull(
                highlight.date.localizedCompactDateWithDayName().takeIf { selectedPeriod != ReportPeriod.Daily && selectedPeriod != ReportPeriod.Habit },
                highlight.note?.takeIf { it.isNotBlank() }?.takeIf { highlight.totalMinutes == 0 }
            ).joinToString(" - ")
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (highlight.totalMinutes > 0) {
            Text(
                text = highlight.totalMinutes.toDurationLabel(),
                modifier = Modifier.widthIn(min = 48.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                color = accent.copy(alpha = 0.8f)
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
internal fun TopTagsCard(
    items: List<TagReportItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val maxMinutes = items.maxOfOrNull { it.totalMinutes } ?: 0

    val gradientColors = remember(items) {
        val colors = items.map { it.color.toColor().copy(alpha = 0.12f) }
        if (colors.isEmpty()) listOf(Color.Transparent, Color.Transparent)
        else if (colors.size == 1) listOf(colors[0], colors[0].copy(alpha = 0.05f))
        else colors
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(gradientColors), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = sectionTitle(
                        prefix = "WHERE YOUR ",
                        emphasis = "ENERGY",
                        suffix = " WENT",
                        accent = ReportGreenDark
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = ReportGreenDark
                )
                Text(
                    text = energySubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { tag ->
                    TagReportBarRow(
                        item = tag,
                        fraction = if (maxMinutes == 0) 0f else tag.totalMinutes.toFloat() / maxMinutes.toFloat()
                    )
                }
            }
        }
    }
}

@Composable
internal fun TagReportBarRow(
    item: TagReportItem,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val tagColor = item.color.toColor()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = item.name,
            modifier = Modifier.widthIn(min = 64.dp, max = 110.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .background(tagColor.copy(alpha = 0.12f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                    .height(12.dp)
                    .background(tagColor, CircleShape)
            )
        }
        Text(
            text = item.totalMinutes.toDurationLabel(),
            modifier = Modifier.widthIn(min = 50.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}


private fun DigestHighlight.icon(): ImageVector = when (sourceName?.let { source ->
    runCatching { DailyPlanItemSource.valueOf(source) }.getOrNull()
}) {
    DailyPlanItemSource.MyDayTask -> Icons.Default.EventAvailable
    DailyPlanItemSource.MyDayNote -> Icons.AutoMirrored.Filled.EventNote
    DailyPlanItemSource.MyDayReminder -> Icons.Default.Schedule
    DailyPlanItemSource.ExistingTask -> Icons.Default.TaskAlt
    null -> Icons.AutoMirrored.Filled.Notes
}

private fun heroEncouragement(
    doneCount: Int,
    plannedCount: Int,
    selectedPeriod: ReportPeriod,
    journalCount: Int
): AnnotatedString {
    val period = when (selectedPeriod) {
        ReportPeriod.Daily -> "today"
        ReportPeriod.Week -> "this week"
        ReportPeriod.Month -> "this month"
        ReportPeriod.Annual -> "this year"
        ReportPeriod.Habit -> "today"
    }
    return buildAnnotatedString {
        when {
            doneCount > 0 -> {
                append("You finished ")
                highlight(doneCount.itemCountLabel(), ReportBlue)
                if (journalCount > 0) {
                    append(" and logged ")
                    highlight("$journalCount ${if (journalCount == 1) "check-in" else "check-ins"}", ReportPurple)
                }
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

private fun Int.trendSummary(
    previousTotalMinutes: Int,
    selectedPeriod: ReportPeriod
): TrendSummary {
    val comparisonLabel = when (selectedPeriod) {
        ReportPeriod.Daily -> "yesterday"
        ReportPeriod.Week -> "last week"
        ReportPeriod.Month -> "last month"
        ReportPeriod.Annual -> "last year"
        ReportPeriod.Habit -> "yesterday"
    }
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
