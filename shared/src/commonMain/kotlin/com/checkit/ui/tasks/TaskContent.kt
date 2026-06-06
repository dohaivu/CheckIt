package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.ui.TaskListDisplayType
import com.checkit.ui.TaskUiState
import com.checkit.ui.TaskWorkspaceView

@Composable
internal fun TaskContent(
    state: TaskUiState,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onListDisplayTypeChange: (TaskListDisplayType) -> Unit,
    onTimelineCreateTask: (Int, Int) -> Unit,
    onTimelineTaskTimeChange: (TaskItem, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val showListName = state.selectedList == null
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
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
                onDisplayTypeChange = onListDisplayTypeChange,
                onTaskClick = onTaskClick,
                onNoteClick = onNoteClick,
                modifier = Modifier.weight(1f)
            )
            TaskWorkspaceView.Agenda -> TaskAgendaView(
                tasks = state.visibleTasks,
                notes = state.visibleNotes,
                lists = state.board.lists,
                showListName = showListName,
                onTaskClick = onTaskClick,
                onNoteClick = onNoteClick,
                dayLimit = state.dayLimit,
                modifier = Modifier.weight(1f)
            )
            TaskWorkspaceView.Timeline -> TaskTimelineView(
                tasks = state.visibleTasks,
                notes = state.visibleNotes,
                lists = state.board.lists,
                showListName = showListName,
                onTaskClick = onTaskClick,
                onNoteClick = onNoteClick,
                onCreateTask = onTimelineCreateTask,
                onTaskTimeChange = onTimelineTaskTimeChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
