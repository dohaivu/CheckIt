package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskStatus
import com.checkit.ui.TaskListDisplayType
import com.checkit.ui.TaskUiState
import com.checkit.ui.TaskWorkspaceView
import com.checkit.ui.today
import kotlinx.datetime.LocalDate

@Composable
internal fun TaskContent(
    state: TaskUiState,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onListDisplayTypeChange: (TaskListDisplayType) -> Unit,
    onTimelineCreateTask: (Int, Int) -> Unit,
    onTimelineTaskTimeChange: (TaskItem, Int, Int) -> Unit,
    onTimelineNoteTimeChange: (NoteItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val showListName = state.selectedList == null
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 0.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (state.selectedView == TaskWorkspaceView.List) {
                ListDisplayTypeMenu(selected = state.listDisplayType, onSelect = onListDisplayTypeChange)
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = "${state.visibleTasks.size} tasks · ${state.visibleNotes.size} notes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        when (state.selectedView) {
            TaskWorkspaceView.List -> TaskListView(
                tasks = state.visibleTasks,
                notes = state.visibleNotes,
                lists = state.board.lists,
                showListName = showListName,
                displayType = state.listDisplayType,
                onTaskClick = onTaskClick,
                onNoteClick = onNoteClick,
                modifier = Modifier.weight(1f)
            )
            TaskWorkspaceView.Agenda -> TaskAgendaView(
                tasks = state.visibleTasks,
                notes = state.visibleNotes,
                lists = state.board.lists,
                dayLimit = state.dayLimit,
                focusedDate = today(),
                showListName = showListName,
                onTaskClick = onTaskClick,
                onNoteClick = onNoteClick,
                modifier = Modifier.weight(1f)
            )
            TaskWorkspaceView.Timeline -> TaskTimelineView(
                tasks = state.visibleTasks,
                notes = state.visibleNotes,
                lists = state.board.lists,
                showListName = showListName,
                onTaskClick = onTaskClick,
                onNoteClick = onNoteClick,
                onTimelineCreateTask = onTimelineCreateTask,
                onTimelineTaskTimeChange = onTimelineTaskTimeChange,
                onTimelineNoteTimeChange = onTimelineNoteTimeChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun TaskAgendaView(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: List<TaskList>,
    dayLimit: Int?,
    focusedDate: LocalDate,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val agendaItems = remember(tasks, notes) {
        val taskItems = tasks.map { task ->
            TimelineItem(
                id = "task-${task.id}",
                type = TimelineItemType.Task,
                date = task.doDate,
                startTimeMinutes = task.startTimeMinutes,
                endTimeMinutes = task.endTimeMinutes,
                sortOrder = task.sortOrder,
                tag = task
            )
        }
        val noteItems = notes.map { note ->
            TimelineItem(
                id = "note-${note.id}",
                type = TimelineItemType.Note,
                date = note.date,
                startTimeMinutes = note.startTimeMinutes,
                endTimeMinutes = note.startTimeMinutes?.let { it + 30 },
                sortOrder = note.sortOrder,
                tag = note
            )
        }
        (taskItems + noteItems).sortedWith(compareBy<TimelineItem> { it.date }.thenBy { it.startTimeMinutes ?: -1 })
    }

    AgendaView(
        items = agendaItems,
        onItemClick = { item ->
            when (val tag = item.tag) {
                is TaskItem -> onTaskClick(tag)
                is NoteItem -> onNoteClick(tag)
            }
        },
        dayLimit = dayLimit,
        focusedDate = focusedDate,
        itemContent = { item ->
            val listId = (item.tag as? TaskItem)?.listId ?: (item.tag as? NoteItem)?.listId
            val list = lists.find { it.id == listId }

            when (val tag = item.tag) {
                is TaskItem -> TaskCard(
                    title = tag.name.ifBlank { "Untitled task" },
                    supportingText = if (showListName) list?.name else null,
                    leadingContent = { TaskStatusIcon(tag.status, tag.priority) },
                    color = taskCardColor(tag, list),
                    completed = tag.status == TaskStatus.Completed
                )
                is NoteItem -> TaskCard(
                    title = tag.content.ifBlank { "Empty note" },
                    supportingText = if (showListName) list?.name else null,
                    color = list?.color?.toColor() ?: MaterialTheme.colorScheme.secondary,
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    completed = tag.status == TaskStatus.Completed
                )
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun TaskTimelineView(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onTimelineCreateTask: (Int, Int) -> Unit,
    onTimelineTaskTimeChange: (TaskItem, Int, Int) -> Unit,
    onTimelineNoteTimeChange: (NoteItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val timelineItems = remember(tasks, notes) {
        val taskItems = tasks.map { task ->
            TimelineItem(
                id = "task-${task.id}",
                type = TimelineItemType.Task,
                startTimeMinutes = task.startTimeMinutes,
                endTimeMinutes = task.endTimeMinutes,
                sortOrder = task.sortOrder,
                isResizable = true,
                tag = task
            )
        }
        val noteItems = notes.map { note ->
            TimelineItem(
                id = "note-${note.id}",
                type = TimelineItemType.Note,
                startTimeMinutes = note.startTimeMinutes,
                endTimeMinutes = note.startTimeMinutes?.let { it + 30 },
                sortOrder = note.sortOrder,
                isResizable = false,
                tag = note
            )
        }
        (taskItems + noteItems).sortedBy { it.startTimeMinutes ?: -1 }
    }
    TimelineView(
        items = timelineItems,
        onItemClick = { item ->
            when (val tag = item.tag) {
                is TaskItem -> onTaskClick(tag)
                is NoteItem -> onNoteClick(tag)
            }
        },
        onCreateRequest = onTimelineCreateTask,
        onTimeChange = { item, start, end ->
            when (val tag = item.tag) {
                is TaskItem -> onTimelineTaskTimeChange(tag, start, end)
                is NoteItem -> onTimelineNoteTimeChange(tag, start)
            }
        },
        allDayItemContent = { item ->
            when (val tag = item.tag) {
                is TaskItem -> {
                    val list = lists.find { it.id == tag.listId }
                    AllDayItemRow(
                        label = tag.name.ifBlank { "Untitled task" },
                        icon = { Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        color = taskCardColor(tag, list),
                        supportingLabel = if (showListName) list?.name else null
                    )
                }
                is NoteItem -> {
                    val list = lists.find { it.id == tag.listId }
                    AllDayItemRow(
                        label = tag.content.ifBlank { "Empty note" },
                        icon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        color = list?.color?.toColor() ?: MaterialTheme.colorScheme.secondary,
                        supportingLabel = if (showListName) list?.name else null
                    )
                }
            }
        },
        timedItemContent = { item, isSelected ->
            when (val tag = item.tag) {
                is TaskItem -> {
                    val list = lists.find { it.id == tag.listId }
                    val start = tag.startTimeMinutes ?: 0
                    val end = tag.endTimeMinutes ?: (start + 60)
                    TaskCard(
                        title = tag.name.ifBlank { "Untitled task" },
                        timeLabel = "${start.toClockLabel()} - ${end.toClockLabel()}",
                        supportingText = if (showListName) list?.name else null,
                        leadingContent = { TaskStatusIcon(tag.status, tag.priority) },
                        color = taskCardColor(tag, list),
                        minHeight = 36.dp,
                        titleMaxLines = 1,
                        completed = tag.status == TaskStatus.Completed,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        containerAlpha = if (isSelected) SelectedTaskCardAlpha else DefaultTaskCardAlpha,
                        tonalElevation = if (isSelected) 3.dp else 1.dp,
                        modifier = Modifier.matchParentSize()
                    )
                }
                is NoteItem -> {
                    val list = lists.find { it.id == tag.listId }
                    val start = tag.startTimeMinutes ?: 0
                    TaskCard(
                        title = tag.content.ifBlank { "Empty note" },
                        timeLabel = start.toClockLabel(),
                        supportingText = if (showListName) list?.name else null,
                        color = list?.color?.toColor() ?: MaterialTheme.colorScheme.secondary,
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        minHeight = 36.dp,
                        titleMaxLines = 1,
                        completed = tag.status == TaskStatus.Completed,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        containerAlpha = DefaultTaskCardAlpha,
                        tonalElevation = 1.dp,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        },
        modifier = modifier
    )
}