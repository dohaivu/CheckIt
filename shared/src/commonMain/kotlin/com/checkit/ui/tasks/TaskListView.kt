package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem

@Composable
internal fun TaskListView(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tasks, key = { "task-${it.id}" }) { task -> TaskRow(task, onClick = { onTaskClick(task) }) }
        items(notes, key = { "note-${it.id}" }) { note -> NoteRow(note, onClick = { onNoteClick(note) }) }
    }
}
