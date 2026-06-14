package com.checkit.ui.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.tab_report
import com.checkit.ui.ReportUiState
import com.checkit.ui.components.TinyTopAppBar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReportScreen(
    state: ReportUiState,
    reportViewModel: ReportViewModel,
    modifier: Modifier = Modifier
) {

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                title = {
                    Text(stringResource(Res.string.tab_report), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                },
                actions = {

                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            TagsReport(
                state = state,
                onPeriodSelected = reportViewModel::selectPeriod,
                onPreviousPeriod = reportViewModel::previousPeriod,
                onNextPeriod = reportViewModel::nextPeriod,
                onCurrentPeriod = reportViewModel::resetToCurrentPeriod
            )
        }
    }
}
