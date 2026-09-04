package com.checkit.ui.reflect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.theme.toColor
import com.checkit.ui.toDurationLabel
import kotlinx.datetime.LocalDate

@Composable
internal fun ActivityAndTagsSection(
    totalMinutes: Int,
    activityItems: List<TimeReportItem>,
    topTags: List<TagReportItem>,
    selectedDate: LocalDate,
    selectedPeriod: ReportPeriod,
    onZoomInTo: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Total Time Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "TOTAL TIME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = totalMinutes.toDurationLabel(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Activity Rhythm Chart
        ActivityChart(
            items = activityItems,
            selectedDate = selectedDate,
            selectedPeriod = selectedPeriod,
            onZoomInTo = onZoomInTo
        )

        // Tag Hours Meters
        if (topTags.isNotEmpty()) {
            TagHoursList(tags = topTags)
        }
    }
}

@Composable
private fun TagHoursList(
    tags: List<TagReportItem>,
    modifier: Modifier = Modifier
) {
    val maxMinutes = remember(tags) {
        tags.maxOfOrNull { it.totalMinutes }?.coerceAtLeast(1) ?: 1
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "TAG HOURS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            tags.forEach { tagItem ->
                val tagColor = tagItem.color.toColor()
                val fraction = (tagItem.totalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.05f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(tagColor, CircleShape)
                    )
                    Text(
                        text = tagItem.name,
                        modifier = Modifier.widthIn(min = 60.dp, max = 110.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .background(tagColor.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(10.dp)
                                .background(tagColor, CircleShape)
                        )
                    }
                    Text(
                        text = tagItem.totalMinutes.toDurationLabel(),
                        modifier = Modifier.widthIn(min = 55.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
