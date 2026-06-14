package com.checkit.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.weekly_digest_active_days
import checkit.shared.generated.resources.weekly_digest_busiest_day
import checkit.shared.generated.resources.weekly_digest_done_items
import checkit.shared.generated.resources.weekly_digest_empty
import checkit.shared.generated.resources.weekly_digest_title
import checkit.shared.generated.resources.weekly_digest_top_tags
import checkit.shared.generated.resources.weekly_digest_total_time
import com.checkit.ui.ReportUiState
import com.checkit.ui.TagReportItem
import com.checkit.ui.TimeReportItem
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.components.WeekHeader
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.tasks.formatDuration
import com.checkit.ui.theme.toColor
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WeeklyDigestReport(
    state: ReportUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val digest = state.weeklyDigest
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = stringResource(Res.string.weekly_digest_title),
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            WeekHeader(
                week = state.selectedDate,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onCurrentWeek = onCurrentWeek
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(Res.string.weekly_digest_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (digest.totalMinutes == 0 && digest.doneItemCount == 0) {
                    Text(
                        text = stringResource(Res.string.weekly_digest_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    DigestMetric(
                        label = stringResource(Res.string.weekly_digest_total_time),
                        value = digest.totalMinutes.formatDuration()
                    )
                    DigestMetric(
                        label = stringResource(Res.string.weekly_digest_done_items),
                        value = digest.doneItemCount.toString()
                    )
                    DigestMetric(
                        label = stringResource(Res.string.weekly_digest_active_days),
                        value = "${digest.activeDayCount}/7"
                    )
                    DigestMetric(
                        label = stringResource(Res.string.weekly_digest_busiest_day),
                        value = digest.busiestDay?.busiestDayLabel() ?: "-"
                    )
                    if (digest.topTags.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Text(
                            text = stringResource(Res.string.weekly_digest_top_tags),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            digest.topTags.forEach { tag ->
                                TopTagRow(tag)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DigestMetric(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TopTagRow(tag: TagReportItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(tag.color.toColor(), CircleShape)
        )
        Text(
            text = tag.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = tag.totalMinutes.formatDuration(),
            modifier = Modifier.widthIn(min = 54.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TimeReportItem.busiestDayLabel(): String =
    "${startDate.localizedCompactDateWithDayName()} - ${totalMinutes.formatDuration()}"
