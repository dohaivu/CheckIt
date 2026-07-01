package com.checkit.ui.okr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppHorizontalDivider
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.ColorPicker
import com.checkit.ui.components.DeleteOverflowMenu
import com.checkit.ui.components.IconPicker
import com.checkit.ui.components.SectionLabel
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.theme.AppIconColorDefaults
import com.checkit.ui.theme.toColor

@Composable
internal fun GoalEditorSheet(
    editor: GoalEditorState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTitleChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onIconChange: (String) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight(0.7f)
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        text = if (editor.mode == EditorMode.Add) "New goal" else "Edit goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onSave,
                        enabled = editor.title.isNotBlank()
                    ) {
                        Text("Save")
                    }
                    if (editor.mode == EditorMode.Edit) {
                        DeleteOverflowMenu(
                            onDelete = { showDeleteConfirmation = true },
                            contentDescription = "Goal options",
                            label = "Delete goal"
                        )
                    }
                }
            }
            item {
                Column {
                    Text(
                        text = "Title",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    AppOutlinedTextField(
                        value = editor.title,
                        onValueChange = onTitleChange,
                        placeholder = "e.g. Health & Fitness",
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                }
            }
            item {
                AppHorizontalDivider()
                SectionLabel("Color")
                ColorPicker(
                    colors = AppIconColorDefaults.ListColors,
                    selected = editor.color,
                    onSelect = onColorChange
                )
            }
            item {
                SectionLabel("Icon")
                IconPicker(
                    icons = AppIconColorDefaults.ListIcons,
                    selected = editor.icon,
                    tint = editor.color.toColor(),
                    onSelect = onIconChange
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete goal?") },
            text = { Text("Objectives in \"${editor.title}\" will become normal lists.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
