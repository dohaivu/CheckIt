package com.checkit.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.plan_add_task
import checkit.shared.generated.resources.plan_period_day
import checkit.shared.generated.resources.plan_period_month
import checkit.shared.generated.resources.plan_period_quarter
import checkit.shared.generated.resources.plan_period_week
import checkit.shared.generated.resources.plan_period_year
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.PeriodPlan
import com.checkit.domain.PlanFocus
import com.checkit.domain.PlanPeriod
import com.checkit.domain.PlanPriority
import com.checkit.domain.PlanPriorityNode
import com.checkit.domain.TaskItem
import com.checkit.ui.components.DateTimeRangeDetailChip
import com.checkit.ui.components.DetailChip
import com.checkit.ui.components.TagPill
import com.checkit.ui.tasks.cardColor
import com.checkit.ui.tasks.isOverdue
import com.checkit.ui.tasks.views.TaskTitleRow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PlanPriorityList(
    nodes: List<PlanPriorityNode>,
    focus: PlanPeriod,
    onEditPriority: (PlanPriority) -> Unit,
    onAddTaskClick: (PlanPriority) -> Unit,
    onOpenTask: (TaskItem) -> Unit,
    onZoomIntoPriority: (PlanPriority) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        nodes.forEach { node ->
            PriorityItem(
                node = node,
                depth = 0,
                focus = focus,
                onEditPriority = onEditPriority,
                onAddTaskClick = onAddTaskClick,
                onOpenTask = onOpenTask,
                onZoomIntoPriority = onZoomIntoPriority
            )
        }
    }
}

@Composable
private fun PriorityItem(
    node: PlanPriorityNode,
    depth: Int,
    focus: PlanPeriod,
    onEditPriority: (PlanPriority) -> Unit,
    onAddTaskClick: (PlanPriority) -> Unit,
    onOpenTask: (TaskItem) -> Unit,
    onZoomIntoPriority: (PlanPriority) -> Unit
) {
    val priority = node.priority
    val containerModifier = if (depth == 0) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(8.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    }

    Column(
        modifier = containerModifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PriorityHeader(
            priority = priority,
            isRoot = depth == 0,
            showAddTask = focus == PlanPeriod.Week || focus == PlanPeriod.Day,
            onClick = if (depth > 0) {
                { onZoomIntoPriority(priority) }
            } else null,
            onEditPriority = onEditPriority,
            onAddTaskClick = onAddTaskClick
        )
        if (depth > 0 && priority.periodPlan.period != focus) {
            PeriodPlanChip(
                periodPlan = priority.periodPlan,
                modifier = Modifier
            )
        }
        
        if (node.tasks.isNotEmpty() || (focus == PlanPeriod.Day && node.dailyPlanItems.isNotEmpty())) {
            Column(
                modifier = Modifier.padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                node.tasks.forEach { task ->
                    PriorityTaskRow(
                        task = task,
                        onClick = { onOpenTask(task) }
                    )
                }
                if (focus == PlanPeriod.Day) {
                    node.dailyPlanItems.forEach { item ->
                        PriorityDailyPlanRow(item = item)
                    }
                }
            }
        }

        if (node.children.isNotEmpty()) {
            if (depth == 0 && (node.tasks.isNotEmpty() || (focus == PlanPeriod.Day && node.dailyPlanItems.isNotEmpty()))) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                node.children.forEach { child ->
                    PriorityItem(
                        node = child,
                        depth = depth + 1,
                        focus = focus,
                        onEditPriority = onEditPriority,
                        onAddTaskClick = onAddTaskClick,
                        onOpenTask = onOpenTask,
                        onZoomIntoPriority = onZoomIntoPriority
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityHeader(
    priority: PlanPriority,
    isRoot: Boolean,
    showAddTask: Boolean,
    onClick: (() -> Unit)?,
    onEditPriority: (PlanPriority) -> Unit,
    onAddTaskClick: (PlanPriority) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(priority.id) {
                detectTapGestures(
                    onTap = { onClick?.invoke() },
                    onLongPress = { onEditPriority(priority) }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = priority.title,
                style = if (isRoot) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                fontWeight = if (isRoot) FontWeight.ExtraBold else FontWeight.Bold,
                textDecoration = if (priority.isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (priority.isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (priority.note.isNotBlank()) {
                Text(
                    text = priority.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showAddTask) {
            IconButton(
                onClick = { onAddTaskClick(priority) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.plan_add_task),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PeriodPlanChip(periodPlan: PeriodPlan, modifier: Modifier = Modifier) {
    DetailChip(
        icon = periodPlan.period.periodIcon(),
        label = PlanFocus(periodPlan.period, periodPlan.startDate).crumbLabel(),
        iconTint = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
internal fun PlanPriorityPill(priority: PlanPriority) {
    DetailChip(
        icon = priority.periodPlan.period.periodIcon(),
        label = "${PlanFocus(priority.periodPlan.period, priority.periodPlan.startDate).crumbLabel()}: ${priority.title}",
        iconTint = MaterialTheme.colorScheme.tertiary
    )
}


@Composable
private fun PriorityTaskRow(
    task: TaskItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { onClick() }
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(task.cardColor())
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TaskTitleRow(task, descriptionMaxLines = 0)
                DateTimeRangeDetailChip(
                    task.doDate,
                    task.startTimeMinutes,
                    task.endTimeMinutes,
                    isOverdue = task.isOverdue()
                )
            }
        }
    }
}

@Composable
private fun PriorityDailyPlanRow(item: DailyPlanItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (item.startTimeMinutes != null) {
            Text(
                text = item.startTimeMinutes.toTimeLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Int.toTimeLabel(): String {
    val hour = this / 60
    val minute = this % 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private fun PlanPeriod.periodIcon(): ImageVector = when (this) {
    PlanPeriod.Year -> Icons.Default.DateRange
    PlanPeriod.Quarter -> Icons.Default.GridView
    PlanPeriod.Month -> Icons.Default.CalendarMonth
    PlanPeriod.Week -> Icons.Default.CalendarViewWeek
    PlanPeriod.Day -> Icons.Default.Today
}

private fun PlanPeriod.periodLabelRes(): StringResource = when (this) {
    PlanPeriod.Year -> Res.string.plan_period_year
    PlanPeriod.Quarter -> Res.string.plan_period_quarter
    PlanPeriod.Month -> Res.string.plan_period_month
    PlanPeriod.Week -> Res.string.plan_period_week
    PlanPeriod.Day -> Res.string.plan_period_day
}
