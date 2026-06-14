package com.checkit.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.time_report_empty
import checkit.shared.generated.resources.time_report_title
import com.checkit.ui.ReportUiState
import com.checkit.ui.TimeReportItem
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.components.ReportPeriodHeader
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.localizedShortMonthName
import com.checkit.ui.tasks.formatDuration
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TimeReport(
    state: ReportUiState,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        text = stringResource(Res.string.time_report_title),
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
                    text = stringResource(Res.string.time_report_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.timeReports.all { it.totalMinutes == 0 }) {
                    Text(
                        text = stringResource(Res.string.time_report_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    TimeBarChart(
                        items = state.timeReports,
                        period = state.selectedPeriod
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeBarChart(
    items: List<TimeReportItem>,
    period: ReportPeriod
) {
    val maxMinutes = items.maxOfOrNull { it.totalMinutes } ?: 0
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            TimeBarRow(
                item = item,
                label = item.label(period),
                fraction = if (maxMinutes == 0) 0f else item.totalMinutes.toFloat() / maxMinutes.toFloat()
            )
        }
    }
}

@Composable
private fun TimeBarRow(
    item: TimeReportItem,
    label: String,
    fraction: Float
) {
    val barColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.widthIn(min = 84.dp, max = 128.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f))
        ) {
            if (item.totalMinutes > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(barColor)
                )
            }
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

@Composable
private fun TimeReportItem.label(period: ReportPeriod): String =
    if (period == ReportPeriod.Week || startDate == endDate) {
        startDate.localizedCompactDateWithDayName()
    } else {
        "${startDate.localizedShortMonthName()} ${startDate.day} - ${endDate.localizedShortMonthName()} ${endDate.day}"
    }
