package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
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
                items = state.visibleListItems,
                showListName = showListName,
                displayType = state.listDisplayType,
                onTaskClick = onTaskClick,
                onNoteClick = onNoteClick,
                modifier = Modifier.weight(1f)
            )
            TaskWorkspaceView.Agenda -> TaskAgendaView(
                tasks = state.visibleTasks,
                notes = state.visibleNotes,
                dayLimit = state.dayLimit,
                focusedDate = today(),
                onTaskClick = onTaskClick,
                onNoteClick = onNoteClick,
                modifier = Modifier.weight(1f)
            )
            TaskWorkspaceView.Timeline -> TaskTimelineView(
                tasks = state.visibleTasks,
                notes = state.visibleNotes,
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
    dayLimit: Int?,
    focusedDate: LocalDate,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val agendaItems = remember(tasks, notes) { buildAgendaTimelineItems(tasks, notes) }

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
            when (val tag = item.tag) {
                is TaskItem -> if (item.startTimeMinutes == null) {
                    TaskAllDayCard(tag, completedOverlay = tag.status == TaskStatus.Completed)
                } else {
                    TaskTimelineCard(tag, completedOverlay = tag.status == TaskStatus.Completed)
                }
                is NoteItem -> if (item.startTimeMinutes == null) {
                    NoteAllDayCard(tag, completedOverlay = tag.status == TaskStatus.Completed)
                } else {
                    NoteTimelineCard(tag, completedOverlay = tag.status == TaskStatus.Completed)
                }
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun TaskTimelineView(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onTimelineCreateTask: (Int, Int) -> Unit,
    onTimelineTaskTimeChange: (TaskItem, Int, Int) -> Unit,
    onTimelineNoteTimeChange: (NoteItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val timelineItems = remember(tasks, notes) { buildTimelineItems(tasks, notes) }
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
                is TaskItem -> TaskAllDayCard(tag, completedOverlay = tag.status == TaskStatus.Completed)
                is NoteItem -> NoteAllDayCard(tag, completedOverlay = tag.status == TaskStatus.Completed)
            }
        },
        timedItemContent = { item, isSelected, displayMode ->
            when (val tag = item.tag) {
                is TaskItem -> TaskTimelineCard(
                    task = tag,
                    selected = isSelected,
                    completedOverlay = tag.status == TaskStatus.Completed,
                    modifier = Modifier.matchParentSize(),
                    displayMode = displayMode
                )
                is NoteItem -> NoteTimelineCard(
                    note = tag,
                    selected = isSelected,
                    completedOverlay = tag.status == TaskStatus.Completed,
                    modifier = Modifier.matchParentSize()
                )
            }
        },
        modifier = modifier
    )
}

private fun buildAgendaTimelineItems(
    tasks: List<TaskItem>,
    notes: List<NoteItem>
): List<TimelineItem> {
    val items = ArrayList<TimelineItem>(tasks.size + notes.size)
    tasks.forEach { task ->
        items += TimelineItem(
            id = "task-${task.id}",
            type = TimelineItemType.Task,
            date = task.doDate,
            startTimeMinutes = task.startTimeMinutes,
            endTimeMinutes = task.endTimeMinutes,
            sortOrder = task.sortOrder,
            tag = task
        )
    }
    notes.forEach { note ->
        items += TimelineItem(
            id = "note-${note.id}",
            type = TimelineItemType.Note,
            date = note.date,
            startTimeMinutes = note.startTimeMinutes,
            endTimeMinutes = note.startTimeMinutes?.let { it + 30 },
            sortOrder = note.sortOrder,
            tag = note
        )
    }
    return items.sortedWith(AgendaTimelineItemComparator)
}

private fun buildTimelineItems(
    tasks: List<TaskItem>,
    notes: List<NoteItem>
): List<TimelineItem> {
    val items = ArrayList<TimelineItem>(tasks.size + notes.size)
    tasks.forEach { task ->
        items += TimelineItem(
            id = "task-${task.id}",
            type = TimelineItemType.Task,
            startTimeMinutes = task.startTimeMinutes,
            endTimeMinutes = task.endTimeMinutes,
            sortOrder = task.sortOrder,
            isResizable = true,
            tag = task
        )
    }
    notes.forEach { note ->
        items += TimelineItem(
            id = "note-${note.id}",
            type = TimelineItemType.Note,
            startTimeMinutes = note.startTimeMinutes,
            endTimeMinutes = note.startTimeMinutes?.let { it + 30 },
            sortOrder = note.sortOrder,
            isResizable = false,
            tag = note
        )
    }
    return items.sortedWith(TimelineItemComparator)
}

private val AgendaTimelineItemComparator: Comparator<TimelineItem> =
    compareBy<TimelineItem> { it.date }.thenBy { it.startTimeMinutes ?: -1 }

private val TimelineItemComparator: Comparator<TimelineItem> =
    compareBy<TimelineItem> { it.startTimeMinutes ?: -1 }
