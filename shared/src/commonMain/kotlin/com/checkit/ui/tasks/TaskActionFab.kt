package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun TaskActionFab(
    onTaskClick: () -> Unit,
    onNoteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Task") },
                leadingIcon = { Icon(Icons.Default.TaskAlt, contentDescription = null) },
                onClick = {
                    expanded = false
                    onTaskClick()
                }
            )
            DropdownMenuItem(
                text = { Text("Note") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                onClick = {
                    expanded = false
                    onNoteClick()
                }
            )
        }
    }
}
