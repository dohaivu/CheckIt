package com.checkit.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
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
        Box {
            when (displayType) {
                TaskListDisplayType.Brief -> BriefTaskRowContent(task)
                TaskListDisplayType.Standard -> StandardTaskRowContent(task, list)
                TaskListDisplayType.Detail -> DetailTaskRowContent(task, list)
            }
            if (task.status == TaskStatus.Completed) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = CompletedRowCoverAlpha))
                )
            }
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = DefaultNoteRowBackgroundAlpha)
    ) {
        Box {
            when (displayType) {
                TaskListDisplayType.Brief -> BriefNoteRowContent(note)
                TaskListDisplayType.Standard -> StandardNoteRowContent(note, list)
                TaskListDisplayType.Detail -> DetailNoteRowContent(note, list)
            }
            if (note.status == TaskStatus.Completed) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = CompletedRowCoverAlpha))
                )
            }
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
        TaskStatusIcon(task.status, task.priority)
        Text(
            text = task.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        task.doDate?.let { DetailChip(Icons.Default.Event, it.compact()) }
        if (task.priority != TaskPriority.None) PriorityPill(priority = task.priority)
    }
}

@Composable
private fun StandardTaskRowContent(task: TaskItem, list: TaskList?) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskTitleRow(task, descriptionMaxLines = 1)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DateTimeRangeDetailChip(task.doDate, task.startTimeMinutes, task.endTimeMinutes)
            task.durationMinutes?.let { DetailChip(Icons.Default.Schedule, it.formatDuration()) }
        }
        task.subtasks.takeIf { it.isNotEmpty() }?.let { SubtaskProgressText(task) }
        SupportingPills(list = list, tags = task.tags.take(2), overflowCount = (task.tags.size - 2).coerceAtLeast(0))
    }
}

@Composable
private fun DetailTaskRowContent(task: TaskItem, list: TaskList?) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TaskTitleRow(task, descriptionMaxLines = 3)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DateTimeRangeDetailChip(task.doDate, task.startTimeMinutes, task.endTimeMinutes)
            task.durationMinutes?.let { DetailChip(Icons.Default.Schedule, it.formatDuration()) }
            RepeatPill(task.repeatRRule)
            if (task.reminders.isNotEmpty()) DetailChip(Icons.Default.Notifications, "${task.reminders.size} reminders")
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
        NoteStatusIcon(note.status)
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
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TaskStatusIcon(task.status, task.priority)
        Column(Modifier.weight(1f)) {
            Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (task.description.isNotBlank()) {
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = descriptionMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun TaskStatusIcon(status: TaskStatus, priority: TaskPriority) {
    Icon(
        imageVector = if (status == TaskStatus.Completed) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
        contentDescription = null,
        tint = if (status == TaskStatus.Completed) MaterialTheme.colorScheme.onSurfaceVariant else priority.priorityColor(),
        modifier = Modifier.size(20.dp)
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            NoteStatusIcon(note.status)
            Text(note.content, style = MaterialTheme.typography.bodyMedium, maxLines = contentMaxLines, overflow = TextOverflow.Ellipsis)
        }
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

@Composable
private fun NoteStatusIcon(status: TaskStatus) {
    Icon(
        imageVector = if (status == TaskStatus.Completed) Icons.Default.CheckCircle else Icons.Default.Notes,
        contentDescription = null,
        tint = if (status == TaskStatus.Completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(22.dp)
    )
}

private const val DefaultNoteRowBackgroundAlpha = 0.55f
internal const val CompletedRowCoverAlpha = 0.62f

