package com.checkit.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.TaskListDisplayType

@Composable
internal fun TaskRow(
    task: TaskItem,
    onClick: () -> Unit,
    list: TaskList? = null,
    displayType: TaskListDisplayType = TaskListDisplayType.Standard
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        when (displayType) {
            TaskListDisplayType.Brief -> BriefTaskRowContent(task)
            TaskListDisplayType.Standard -> StandardTaskRowContent(task, list)
            TaskListDisplayType.Detail -> DetailTaskRowContent(task, list)
        }
    }
}

@Composable
internal fun NoteRow(
    note: NoteItem,
    onClick: () -> Unit,
    list: TaskList? = null,
    displayType: TaskListDisplayType = TaskListDisplayType.Standard
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        when (displayType) {
            TaskListDisplayType.Brief -> BriefNoteRowContent(note)
            TaskListDisplayType.Standard -> StandardNoteRowContent(note, list)
            TaskListDisplayType.Detail -> DetailNoteRowContent(note, list)
        }
    }
}

@Composable
private fun BriefTaskRowContent(task: TaskItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaskStatusIcon(task.status)
        Text(
            text = task.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        task.startTimeMinutes?.let { DetailChip(Icons.Default.Schedule, it.toClockLabel()) }
            ?: task.dueDate?.let { DetailChip(Icons.Default.Event, it.compact()) }
        if (task.priority != TaskPriority.None) PriorityPill(priority = task.priority)
    }
}

@Composable
private fun StandardTaskRowContent(task: TaskItem, list: TaskList?) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskTitleRow(task, descriptionMaxLines = 1, showStatusText = false)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            task.dueDate?.let { DetailChip(Icons.Default.Event, it.compact()) }
            task.durationMinutes?.let { DetailChip(Icons.Default.Schedule, it.formatDuration()) }
            if (task.repeatRRule != null) DetailChip(Icons.Default.MoreTime, "Repeats")
            if (task.priority != TaskPriority.None) PriorityPill(priority = task.priority)
        }
        task.subtasks.takeIf { it.isNotEmpty() }?.let { SubtaskProgressText(task) }
        SupportingPills(list = list, tags = task.tags.take(2), overflowCount = (task.tags.size - 2).coerceAtLeast(0))
    }
}

@Composable
private fun DetailTaskRowContent(task: TaskItem, list: TaskList?) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TaskTitleRow(task, descriptionMaxLines = 3, showStatusText = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            task.dueDate?.let { DetailChip(Icons.Default.Event, it.compact()) }
            task.startTimeMinutes?.let { DetailChip(Icons.Default.Schedule, it.toClockLabel()) }
            task.endTimeMinutes?.let { DetailChip(Icons.Default.Schedule, it.toClockLabel()) }
            task.durationMinutes?.let { DetailChip(Icons.Default.Schedule, it.formatDuration()) }
            if (task.repeatRRule != null) DetailChip(Icons.Default.MoreTime, "Repeats")
            if (task.reminders.isNotEmpty()) DetailChip(Icons.Default.Notifications, "${task.reminders.size} reminders")
            if (task.priority != TaskPriority.None) PriorityPill(priority = task.priority)
        }
        task.subtasks.takeIf { it.isNotEmpty() }?.let { SubtaskProgressText(task) }
        SupportingPills(list = list, tags = task.tags)
    }
}

@Composable
private fun BriefNoteRowContent(note: NoteItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Text(
            text = note.content,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        DetailChip(Icons.Default.Event, note.date.compact())
    }
}

@Composable
private fun StandardNoteRowContent(note: NoteItem, list: TaskList?) {
    NoteRowScaffold(note = note, list = list, contentMaxLines = 2, visibleTagCount = 2)
}

@Composable
private fun DetailNoteRowContent(note: NoteItem, list: TaskList?) {
    NoteRowScaffold(note = note, list = list, contentMaxLines = 5, visibleTagCount = Int.MAX_VALUE)
}

@Composable
private fun TaskTitleRow(
    task: TaskItem,
    descriptionMaxLines: Int,
    showStatusText: Boolean
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TaskStatusIcon(task.status)
        Column(Modifier.weight(1f)) {
            Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (task.description.isNotBlank()) {
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = descriptionMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showStatusText) {
            Text(task.status.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TaskStatusIcon(status: TaskStatus) {
    Icon(
        imageVector = if (status == TaskStatus.Completed) Icons.Default.CheckCircle else Icons.Default.TaskAlt,
        contentDescription = null,
        tint = if (status == TaskStatus.Completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp)
    )
}

@Composable
private fun SubtaskProgressText(task: TaskItem) {
    Text(
        text = "${task.subtasks.count { it.isCompleted }}/${task.subtasks.size} subtasks",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NoteRowScaffold(
    note: NoteItem,
    list: TaskList?,
    contentMaxLines: Int,
    visibleTagCount: Int
) {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(note.content, style = MaterialTheme.typography.bodyMedium, maxLines = contentMaxLines, overflow = TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailChip(Icons.Default.Event, note.date.compact())
            }
            SupportingPills(
                list = list,
                tags = note.tags.take(visibleTagCount),
                overflowCount = (note.tags.size - visibleTagCount).coerceAtLeast(0)
            )
        }
    }
}

@Composable
private fun SupportingPills(
    list: TaskList?,
    tags: List<com.checkit.domain.TaskTag>,
    overflowCount: Int = 0
) {
    if (list == null && tags.isEmpty() && overflowCount == 0) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        list?.let {
            DetailChip(
                icon = materialIcon(it.icon),
                label = it.name,
                iconTint = it.color.toColor()
            )
        }
        tags.forEach { tag -> TaskTagPill(tag = tag) }
        if (overflowCount > 0) {
            Text(
                text = "+$overflowCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }
    }
}
