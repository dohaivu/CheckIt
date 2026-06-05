package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList

@Composable
internal fun TaskListView(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks, key = { "task-${it.id}" }) { task ->
            TaskRow(
                task = task,
                onClick = { onTaskClick(task) },
                list = if (showListName) lists.firstOrNull { it.id == task.listId } else null
            )
        }
        items(notes, key = { "note-${it.id}" }) { note ->
            NoteRow(
                note = note,
                onClick = { onNoteClick(note) },
                list = if (showListName) lists.firstOrNull { it.id == note.listId } else null
            )
        }
    }
}
