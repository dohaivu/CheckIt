package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.checkit.ui.TaskUiState
import com.checkit.ui.TaskWorkspaceView

@Composable
internal fun TaskContent(
    state: TaskUiState,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
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
        Spacer(Modifier.height(12.dp))
        when (state.selectedView) {
            TaskWorkspaceView.List -> TaskListView(state.visibleTasks, state.visibleNotes, onTaskClick, onNoteClick)
            TaskWorkspaceView.Agenda -> TaskAgendaView(state.visibleTasks, onTaskClick)
            TaskWorkspaceView.Timeline -> TaskTimelineView(state.visibleTasks, onTaskClick)
        }
    }
}
