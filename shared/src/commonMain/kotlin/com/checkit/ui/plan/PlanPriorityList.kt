package com.checkit.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.PlanPeriod
import com.checkit.domain.PlanPriority
import com.checkit.domain.PlanPriorityNode
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.plan_add_task
import checkit.shared.generated.resources.plan_edit_priority
import com.checkit.ui.tasks.views.TaskRow
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PlanPriorityList(
    nodes: List<PlanPriorityNode>,
    focus: PlanPeriod,
    onToggleDone: (Long, Boolean) -> Unit,
    onEditPriority: (PlanPriority) -> Unit,
    onAddTaskClick: (PlanPriority) -> Unit,
    onOpenTask: (TaskItem) -> Unit,
    onZoomIntoPriority: (PlanPriority) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        nodes.forEach { node ->
            PriorityItem(
                node = node,
                depth = 0,
                focus = focus,
                onToggleDone = onToggleDone,
                onEditPriority = onEditPriority,
                onAddTaskClick = onAddTaskClick,
                onOpenTask = onOpenTask,
                onZoomIntoPriority = null
            )
            node.children.forEach { child ->
                PriorityItem(
                    node = child,
                    depth = 1,
                    focus = focus,
                    onToggleDone = onToggleDone,
                    onEditPriority = onEditPriority,
                    onAddTaskClick = onAddTaskClick,
                    onOpenTask = onOpenTask,
                    onZoomIntoPriority = { onZoomIntoPriority(child.priority) }
                )
            }
        }
    }
}

@Composable
private fun PriorityItem(
    node: PlanPriorityNode,
    depth: Int,
    focus: PlanPeriod,
    onToggleDone: (Long, Boolean) -> Unit,
    onEditPriority: (PlanPriority) -> Unit,
    onAddTaskClick: (PlanPriority) -> Unit,
    onOpenTask: (TaskItem) -> Unit,
    onZoomIntoPriority: (() -> Unit)?
) {
    val priority = node.priority
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PriorityHeader(
            priority = priority,
            showAddTask = focus == PlanPeriod.Week || focus == PlanPeriod.Day,
            onClick = onZoomIntoPriority,
            onToggleDone = onToggleDone,
            onEditPriority = onEditPriority,
            onAddTaskClick = onAddTaskClick
        )
        Column(
            modifier = Modifier.padding(start = 24.dp)
        ) {
            node.tasks.forEach { task ->
                LinkedTaskRow(
                    task = task,
                    onClick = { onOpenTask(task) }
                )
            }
        }
        if (focus == PlanPeriod.Day) {
            node.dailyPlanItems.forEach { item ->
                DailyPlanItemRow(item = item)
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun PriorityHeader(
    priority: PlanPriority,
    showAddTask: Boolean,
    onClick: (() -> Unit)?,
    onToggleDone: (Long, Boolean) -> Unit,
    onEditPriority: (PlanPriority) -> Unit,
    onAddTaskClick: (PlanPriority) -> Unit
) {
    Row(
        modifier = Modifier
            .pointerInput(priority.id) {
                detectTapGestures(
                    onTap = { onClick?.invoke() },
                    onLongPress = { onEditPriority(priority) }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = priority.isDone,
            onCheckedChange = { checked -> onToggleDone(priority.id, checked) }
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = priority.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (priority.isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (priority.isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (priority.note.isNotBlank()) {
                Text(
                    text = priority.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showAddTask) {
            IconButton(
                onClick = { onAddTaskClick(priority) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.plan_add_task),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LinkedTaskRow(
    task: TaskItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = task.name,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (task.status == TaskStatus.Completed) {
                TextDecoration.LineThrough
            } else {
                TextDecoration.None
            },
            color = if (task.status == TaskStatus.Completed) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DailyPlanItemRow(item: DailyPlanItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
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
