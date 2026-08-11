package com.checkit.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.PlanFocus
import com.checkit.domain.PlanPeriod
import com.checkit.domain.PlanPriority
import com.checkit.domain.TaskItem
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.plan_add_priority
import checkit.shared.generated.resources.plan_empty_subtitle
import checkit.shared.generated.resources.plan_empty_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PeriodPlanScreen(
    state: PlanPeriodUiState,
    onFocusSelected: (PlanFocus) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
    onAddPriority: () -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onEditPriority: (PlanPriority) -> Unit,
    onAddTask: (Long, String) -> Unit,
    onUnlinkTask: (Long, Long) -> Unit,
    onOpenTask: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        item {
            PlanPeriodHeader(
                focus = state.focus,
                onFocusSelected = onFocusSelected,
                onPreviousPeriod = onPreviousPeriod,
                onNextPeriod = onNextPeriod,
                onCurrentPeriod = onCurrentPeriod
            )
        }
        when {
            state.isLoading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            state.rootNodes.isEmpty() -> {
                item {
                    EmptyPlanState(
                        focus = state.focus,
                        onAddPriority = onAddPriority
                    )
                }
            }
            else -> {
                item {
                    PlanPriorityList(
                        nodes = state.rootNodes,
                        focus = state.focus.period,
                        onToggleDone = onToggleDone,
                        onEditPriority = onEditPriority,
                        onAddTask = onAddTask,
                        onUnlinkTask = onUnlinkTask,
                        onOpenTask = onOpenTask,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyPlanState(
    focus: PlanFocus,
    onAddPriority: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (focus.period.ordinal >= PlanPeriod.Week.ordinal) {
                    stringResource(Res.string.plan_empty_title)
                } else {
                    "No priorities for this ${focus.period.name.lowercase()}"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(Res.string.plan_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onAddPriority) {
                Text(stringResource(Res.string.plan_add_priority))
            }
        }
    }
}
