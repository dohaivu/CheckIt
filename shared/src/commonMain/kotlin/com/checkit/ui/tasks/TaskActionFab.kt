package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.checkit.ui.components.icons.AppIcons
import com.checkit.ui.components.icons.Target

@Composable
internal fun TaskActionFab(
    onTaskClick: (() -> Unit)? = null,
    onNoteClick: (() -> Unit)? = null,
    onObjectiveClick: (() -> Unit)? = null,
    onKeyResultClick: (() -> Unit)? = null
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
            if (onTaskClick != null) {
                DropdownMenuItem(
                    text = { Text("Task") },
                    leadingIcon = { Icon(Icons.Default.TaskAlt, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onTaskClick()
                    }
                )
            }
            if (onNoteClick != null) {
                DropdownMenuItem(
                    text = { Text("Note") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onNoteClick()
                    }
                )
            }
            if (onObjectiveClick != null) {
                DropdownMenuItem(
                    text = { Text("Objective") },
                    leadingIcon = { Icon(AppIcons.Target, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onObjectiveClick()
                    }
                )
            }
            if (onKeyResultClick != null) {
                DropdownMenuItem(
                    text = { Text("Key result") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onKeyResultClick()
                    }
                )
            }
        }
    }
}
