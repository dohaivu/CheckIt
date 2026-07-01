package com.checkit.ui.okr

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.ColorPicker
import com.checkit.ui.components.DeleteOverflowMenu
import com.checkit.ui.components.PeriodPicker
import com.checkit.ui.components.SectionLabel
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.tasks.ObjectiveEditorState
import com.checkit.ui.theme.AppIconColorDefaults
import kotlinx.datetime.LocalDate

@Composable
internal fun ObjectiveEditorSheet(
    editor: ObjectiveEditorState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit,
    onColorChange: (String) -> Unit,
    onIconChange: (String) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
                        text = if (editor.mode == EditorMode.Add) "New objective" else "Edit objective",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onSave) {
                        Text("Save")
                    }
                    if (editor.mode == EditorMode.Edit) {
                        DeleteOverflowMenu(
                            onDelete = { showDeleteConfirmation = true },
                            contentDescription = "Objective options",
                            label = "Delete objective"
                        )
                    }
                }
            }
            item {
                AppOutlinedTextField(
                    value = editor.name,
                    onValueChange = onTitleChange,
                    placeholder = "Title",
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                PeriodPicker(
                    startDate = editor.startDate,
                    endDate = editor.endDate,
                    onRangeChange = onDateRangeChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                SectionLabel("Color")
                ColorPicker(
                    colors = AppIconColorDefaults.ListColors,
                    selected = editor.color,
                    onSelect = onColorChange
                )
            }
//            item {
//                SectionLabel("Icon")
//                IconPicker(
//                    icons = AppIconColorDefaults.ListIcons,
//                    selected = editor.icon,
//                    tint = editor.color.toColor(),
//                    onSelect = onIconChange
//                )
//            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete objective?") },
            text = { Text("All key results and tasks in this objective will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text("Delete")
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
