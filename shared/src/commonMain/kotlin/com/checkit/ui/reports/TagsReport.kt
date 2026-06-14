package com.checkit.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.tags_report_empty
import checkit.shared.generated.resources.tags_report_title
import com.checkit.ui.ReportUiState
import com.checkit.ui.TagReportItem
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.components.ReportPeriodHeader
import com.checkit.ui.tasks.formatDuration
import com.checkit.ui.theme.toColor
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TagsReport(
    state: ReportUiState,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ReportPeriodHeader(
            selectedPeriod = state.selectedPeriod,
            selectedDate = state.selectedDate,
            onPeriodSelected = onPeriodSelected,
            onPreviousPeriod = onPreviousPeriod,
            onNextPeriod = onNextPeriod,
            onCurrentPeriod = onCurrentPeriod
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.tags_report_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (state.tagReports.isEmpty()) {
                Text(
                    text = stringResource(Res.string.tags_report_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TagBarChart(items = state.tagReports)
            }
        }
    }
}

@Composable
private fun TagBarChart(items: List<TagReportItem>) {
    val maxMinutes = items.maxOfOrNull { it.totalMinutes } ?: 0
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            TagBarRow(
                item = item,
                fraction = if (maxMinutes == 0) 0f else item.totalMinutes.toFloat() / maxMinutes.toFloat()
            )
        }
    }
}

@Composable
private fun TagBarRow(
    item: TagReportItem,
    fraction: Float
) {
    val tagColor = item.color.toColor()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = item.name,
            modifier = Modifier.widthIn(min = 72.dp, max = 118.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(tagColor.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(tagColor)
            )
        }
        Text(
            text = item.totalMinutes.formatDuration(),
            modifier = Modifier.widthIn(min = 54.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
