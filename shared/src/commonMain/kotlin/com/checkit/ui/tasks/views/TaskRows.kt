package com.checkit.ui.tasks.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import com.checkit.ui.components.DateTimeRangeDetailChip
import com.checkit.ui.components.DetailChip
import com.checkit.ui.components.RepeatPill
import com.checkit.ui.components.SupportingPills
import com.checkit.ui.duration
import com.checkit.ui.tasks.NoteIcon
import com.checkit.ui.tasks.TaskIcon
import com.checkit.ui.tasks.TaskListDisplayType
import com.checkit.ui.tasks.compact
import com.checkit.ui.tasks.isOverdue
import com.checkit.ui.tasks.priorityColor
import com.checkit.ui.tasks.toDurationLabel

@Composable
internal fun TaskRow(
    task: TaskItem,
    onClick: () -> Unit,
    list: Objective? = null,
    displayType: TaskListDisplayType = TaskListDisplayType.Standard
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.status == TaskStatus.Completed) 0.dp else 2.dp,
            pressedElevation = 8.dp,
            focusedElevation = 4.dp
        )
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
    list: Objective? = null,
    displayType: TaskListDisplayType = TaskListDisplayType.Standard
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp,
            focusedElevation = 4.dp
        )
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
internal fun OKRTaskContent(task: TaskItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaskIcon(
            completed = task.status == TaskStatus.Completed,
            color = task.priority.priorityColor()
        )
        Text(
            text = task.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        task.doDate?.let { DetailChip(Icons.Default.Event, it.compact(), isHighlighted = task.isOverdue()) }
    }
}

@Composable
internal fun OKRNoteContent(note: NoteItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NoteIcon(note.status)
        Text(
            text = note.title.ifBlank { note.content },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        DetailChip(Icons.Default.Event, note.date.compact())
    }
}

@Composable
internal fun BriefTaskRowContent(task: TaskItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaskIcon(
            completed = task.status == TaskStatus.Completed,
            color = task.priority.priorityColor()
        )
        Text(
            text = task.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        task.doDate?.let { DetailChip(Icons.Default.Event, it.compact(), isHighlighted = task.isOverdue()) }
    }
}

@Composable
internal fun StandardTaskRowContent(task: TaskItem, list: Objective?) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskTitleRow(task, descriptionMaxLines = 0)
        DateTimeRangeDetailChip(task.doDate, task.startTimeMinutes, task.endTimeMinutes, isOverdue = task.isOverdue())
        task.subtasks.takeIf { it.isNotEmpty() }?.let { SubtaskProgressText(task) }
        SupportingPills(list = list, tags = task.tags.take(2), overflowCount = (task.tags.size - 2).coerceAtLeast(0))
    }
}

@Composable
internal fun DetailTaskRowContent(task: TaskItem, list: Objective?) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TaskTitleRow(task, descriptionMaxLines = 3)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DateTimeRangeDetailChip(task.doDate, task.startTimeMinutes, task.endTimeMinutes, isOverdue = task.isOverdue())
            duration(task.startTimeMinutes, task.endTimeMinutes)?.let { DetailChip(Icons.Default.Schedule, it.toDurationLabel()) }
            RepeatPill(task.repeatRRule)
            if (task.reminders.isNotEmpty()) DetailChip(Icons.Default.Notifications, "${task.reminders.size} reminders")
        }
        task.subtasks.takeIf { it.isNotEmpty() }?.let { SubtaskProgressText(task) }
        SupportingPills(list = list, tags = task.tags)
    }
}

@Composable
internal fun BriefNoteRowContent(note: NoteItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NoteIcon(note.status)
        Text(
            text = note.title.ifBlank { note.content },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (note.title.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        DetailChip(Icons.Default.Event, note.date.compact())
    }
}

@Composable
internal fun StandardNoteRowContent(note: NoteItem, list: Objective?) {
    NoteRowScaffold(note = note, list = list, contentMaxLines = 2, visibleTagCount = 2)
}

@Composable
internal fun DetailNoteRowContent(note: NoteItem, list: Objective?) {
    NoteRowScaffold(note = note, list = list, contentMaxLines = 5, visibleTagCount = Int.MAX_VALUE)
}

@Composable
internal fun TaskTitleRow(
    task: TaskItem,
    descriptionMaxLines: Int,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TaskIcon(
            completed = task.status == TaskStatus.Completed,
            color = task.priority.priorityColor()
        )
        Column(Modifier.weight(1f)) {
            Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (descriptionMaxLines > 0 && task.description.isNotBlank()) {
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
internal fun SubtaskProgressText(task: TaskItem) {
    Text(
        text = "${task.subtasks.count { it.isCompleted }}/${task.subtasks.size} subtasks",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
internal fun NoteRowScaffold(
    note: NoteItem,
    list: Objective?,
    contentMaxLines: Int,
    visibleTagCount: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            NoteIcon(note.status)
            Column(Modifier.weight(1f)) {
                if (note.title.isNotBlank()) {
                    Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                if (note.content.isNotBlank()) {
                    Text(
                        note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = contentMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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

private const val DefaultNoteRowBackgroundAlpha = 0.55f
internal const val CompletedRowCoverAlpha = 0.62f
internal const val ContentContainerAlpha = 0.45f
internal const val ContentAlpha = 0.62f
