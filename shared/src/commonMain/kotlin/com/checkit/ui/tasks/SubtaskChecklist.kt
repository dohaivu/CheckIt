package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.checkit.ui.EditorMode
import com.checkit.ui.SubTaskEditorState

@Composable
internal fun SubtaskChecklist(
    subtasks: List<SubTaskEditorState>,
    mode: EditorMode,
    onToggle: (Int) -> Unit,
    onAdd: () -> Unit,
    onNameChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (subtasks.isEmpty() && mode == EditorMode.View) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        subtasks.forEachIndexed { index, subtask ->
            SubtaskRow(
                subtask = subtask,
                mode = mode,
                onToggle = { onToggle(index) },
                onNameChange = { onNameChange(index, it) },
                onRemove = { onRemove(index) }
            )
        }
        if (mode != EditorMode.View) {
            TextButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Add subtask")
            }
        }
    }
}

@Composable
private fun SubtaskRow(
    subtask: SubTaskEditorState,
    mode: EditorMode,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (subtask.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (subtask.isCompleted) "Mark incomplete" else "Mark complete",
                tint = if (subtask.isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        if (mode == EditorMode.View) {
            Text(
                text = subtask.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (subtask.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
        } else {
            OutlinedTextField(
                value = subtask.name,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Subtask") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = editorTextFieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { }),
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete subtask")
            }
        }
    }
}
