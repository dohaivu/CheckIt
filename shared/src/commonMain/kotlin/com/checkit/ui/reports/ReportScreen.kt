package com.checkit.ui.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.tab_report
import checkit.shared.generated.resources.time_report_title
import checkit.shared.generated.resources.weekly_digest_title
import com.checkit.ui.ReportUiState
import com.checkit.ui.components.TinyTopAppBar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReportScreen(
    state: ReportUiState,
    reportViewModel: ReportViewModel,
    onShowTimeReport: () -> Unit,
    onShowWeeklyDigest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.tab_report),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Report options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        ReportMenuItem(
                            text = stringResource(Res.string.time_report_title),
                            onClick = {
                                menuExpanded = false
                                onShowTimeReport()
                            }
                        )
                        ReportMenuItem(
                            text = stringResource(Res.string.weekly_digest_title),
                            onClick = {
                                menuExpanded = false
                                onShowWeeklyDigest()
                            }
                        )
                    }
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

@Composable
private fun ReportMenuItem(
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontWeight = FontWeight.Normal
            )
        },
        onClick = onClick
    )
}
