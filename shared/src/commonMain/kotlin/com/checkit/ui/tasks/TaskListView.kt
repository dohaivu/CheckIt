package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.ui.TaskListDisplayType

@Composable
internal fun TaskListView(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    displayType: TaskListDisplayType = TaskListDisplayType.Standard,
    onDisplayTypeChange: ((TaskListDisplayType) -> Unit)? = null,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        onDisplayTypeChange?.let { onSelect ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ListDisplayTypeMenu(selected = displayType, onSelect = onSelect)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { "task-${it.id}" }) { task ->
                TaskRow(
                    task = task,
                    onClick = { onTaskClick(task) },
                    list = if (showListName) lists.firstOrNull { it.id == task.listId } else null,
                    displayType = displayType
                )
            }
            items(notes, key = { "note-${it.id}" }) { note ->
                NoteRow(
                    note = note,
                    onClick = { onNoteClick(note) },
                    list = if (showListName) lists.firstOrNull { it.id == note.listId } else null,
                    displayType = displayType
                )
            }
        }
    }
}

@Composable
private fun ListDisplayTypeMenu(
    selected: TaskListDisplayType,
    onSelect: (TaskListDisplayType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) {
        Text(selected.name)
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        TaskListDisplayType.entries.forEach { displayType ->
            DropdownMenuItem(
                text = { Text(displayType.name) },
                onClick = {
                    onSelect(displayType)
                    expanded = false
                }
            )
        }
    }
}
