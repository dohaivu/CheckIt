package com.checkit.ui.tasks.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskType
import com.checkit.ui.components.CompactDetailChip
import com.checkit.ui.components.DateTimeRangeDetailChip
import com.checkit.ui.components.DetailChip
import com.checkit.ui.components.SupportingPills
import com.checkit.ui.components.asAnnotatedString
import com.checkit.ui.duration
import com.checkit.ui.tasks.HabitIcon
import com.checkit.ui.tasks.NoteIcon
import com.checkit.ui.tasks.SubtaskBriefList
import com.checkit.ui.tasks.TaskIcon
import com.checkit.ui.tasks.TaskListDisplayType
import com.checkit.ui.tasks.cardColor
import com.checkit.ui.tasks.compact
import com.checkit.ui.tasks.isOverdue
import com.checkit.ui.tasks.priorityColor
import com.checkit.ui.tasks.toDurationLabel

@Composable
internal fun TaskRow(
    task: TaskItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    showList: Boolean = true,
    displayType: TaskListDisplayType = TaskListDisplayType.Standard
) {
    BaseTaskRow(
        color = task.cardColor(),
        modifier = modifier,
        isCompleted = task.status == TaskStatus.Completed,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (displayType) {
                TaskListDisplayType.Brief -> BriefTaskRowContent(task)
                TaskListDisplayType.Standard -> StandardTaskRowContent(task, showList)
                TaskListDisplayType.Detail -> DetailTaskRowContent(task, showList)
            }
        }
    }
}

@Composable
internal fun NoteRow(
    note: NoteItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    showList: Boolean = true,
    displayType: TaskListDisplayType = TaskListDisplayType.Standard
) {
    BaseTaskRow(
        color = note.cardColor(),
        modifier = modifier,
        isCompleted = note.status == TaskStatus.Completed,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (displayType) {
                TaskListDisplayType.Brief -> BriefNoteRowContent(note)
                TaskListDisplayType.Standard -> StandardNoteRowContent(note, showList)
                TaskListDisplayType.Detail -> DetailNoteRowContent(note, showList)
            }
        }
    }
}

@Composable
internal fun BaseTaskRow(
    color: Color,
    modifier: Modifier = Modifier,
    isCompleted: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.width(4.dp))
            content()
        }

        Box(Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(color)
            )
        }

        if (isCompleted) {
            CompletedOverlay()
        }
    }
}

@Composable
internal fun BriefTaskRowContent(task: TaskItem) {
    Row(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (task.type == TaskType.Habit) {
            HabitIcon(task.status == TaskStatus.Completed, color = task.priority.priorityColor())
        } else {
            TaskIcon(
                completed = task.status == TaskStatus.Completed,
                color = task.priority.priorityColor()
            )
        }
        Text(
            text = task.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!task.label.isNullOrEmpty()) {
            Text(text = task.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        task.doDate?.let { CompactDetailChip(Icons.Default.Event, it.compact(), isHighlighted = task.isOverdue()) }
    }
}

@Composable
internal fun StandardTaskRowContent(task: TaskItem, showList: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TaskTitleRow(task, descriptionMaxLines = 0)
        task.subtasks.takeIf { it.isNotEmpty() }?.let { SubtaskProgressText(task) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            SupportingPills(
                date = { DateTimeRangeDetailChip(task.doDate, task.startTimeMinutes, task.endTimeMinutes, isOverdue = task.isOverdue()) },
                list = if (showList) task.list else null,
                tags = task.tags.take(2),
                overflowCount = (task.tags.size - 2).coerceAtLeast(0)
            )
        }
    }
}

@Composable
internal fun DetailTaskRowContent(task: TaskItem, showList: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TaskTitleRow(task, descriptionMaxLines = 3)
        SubtaskBriefList(task.subtasks)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SupportingPills(
                date = { DateTimeRangeDetailChip(task.doDate, task.startTimeMinutes, task.endTimeMinutes, isOverdue = task.isOverdue()) },
                list = if (showList) task.list else null,
                tags = task.tags
            )
        }
    }
}

@Composable
internal fun BriefNoteRowContent(note: NoteItem) {
    Row(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        if (!note.label.isNullOrEmpty()) {
            Text(text = note.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        note.date?.let { CompactDetailChip(Icons.Default.Event, it.compact(), isHighlighted = note.isOverdue()) }
    }
}

@Composable
internal fun StandardNoteRowContent(note: NoteItem, showList: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            NoteIcon(note.status)
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

                    if (!note.label.isNullOrEmpty()) {
                        Text(text = note.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content.asAnnotatedString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            note.date?.let { CompactDetailChip(Icons.Default.Event, it.compact(), isHighlighted = note.isOverdue()) }
            SupportingPills(
                list = if (showList) note.list else null,
                tags = note.tags,
                overflowCount = 0
            )
        }
    }
}

@Composable
internal fun DetailNoteRowContent(note: NoteItem, showList: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            NoteIcon(note.status)
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

                    if (!note.label.isNullOrEmpty()) {
                        Text(text = note.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content.asAnnotatedString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            note.date?.let {
                CompactDetailChip(
                    Icons.Default.Event,
                    it.compact(),
                    isHighlighted = note.isOverdue()
                )
            }

            SupportingPills(
                list = if (showList) note.list else null,
                tags = note.tags,
                overflowCount = 0
            )
        }
    }
}

@Composable
internal fun TaskTitleRow(
    task: TaskItem,
    descriptionMaxLines: Int,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (task.type == TaskType.Habit) {
            HabitIcon(task.status == TaskStatus.Completed, task.priority.priorityColor())
        } else {
            TaskIcon(
                completed = task.status == TaskStatus.Completed,
                color = task.priority.priorityColor()
            )
        }
        Column(Modifier.weight(1f)) {
            Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
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

        if (!task.label.isNullOrEmpty()) {
            Text(text = task.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
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

private const val DefaultNoteRowBackgroundAlpha = 0.55f
internal const val CompletedRowCoverAlpha = 0.62f
internal const val ContentContainerAlpha = 0.45f
internal const val ContentAlpha = 0.62f
