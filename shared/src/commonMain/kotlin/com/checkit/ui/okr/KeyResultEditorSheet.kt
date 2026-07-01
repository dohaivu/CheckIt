package com.checkit.ui.okr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.checkit.domain.KeyResultUnit
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppHorizontalDivider
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DeleteOverflowMenu
import com.checkit.ui.components.StepHeader
import com.checkit.ui.tasks.EditorMode

@Composable
internal fun KeyResultEditorSheet(
    editor: KeyResultEditorState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTargetValueChange: (Double) -> Unit,
    onCurrentValueChange: (Double) -> Unit,
    onUnitChange: (KeyResultUnit) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        if (editor.mode == EditorMode.Add && editor.title.isEmpty()) {
            focusRequester.requestFocus()
        }
    }

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        text = if (editor.mode == EditorMode.Add) "New Key Result" else "Edit Key Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onSave,
                        enabled = editor.title.isNotBlank() && (editor.unit == KeyResultUnit.Binary || editor.targetValue > 0)
                    ) {
                        Text("Save")
                    }
                    if (editor.mode == EditorMode.Edit) {
                        DeleteOverflowMenu(
                            onDelete = { showDeleteConfirmation = true },
                            contentDescription = "Options",
                            label = "Delete key result"
                        )
                    }
                }
            }

            // STEP 1
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepHeader("STEP 1: What are you doing?")
                    AppOutlinedTextField(
                        value = editor.title,
                        onValueChange = onTitleChange,
                        placeholder = "e.g. Learn Spanish vocabulary",
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                }
            }

            // STEP 2
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepHeader("STEP 2: How are you measuring progress?")
                    MeasurementCategorySelector(
                        selectedUnit = editor.unit,
                        onUnitSelect = onUnitChange
                    )
                }
            }

            // STEP 3
            if (editor.unit != KeyResultUnit.Binary) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StepHeader("STEP 3: Enter your target number")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppOutlinedTextField(
                                value = if (editor.targetValue == 0.0) "" else editor.targetValue.toString(),
                                onValueChange = { onTargetValueChange(it.toDoubleOrNull() ?: 0.0) },
                                modifier = Modifier.weight(1f),
                                placeholder = "Target",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                            Text(
                                text = editor.unit.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Optional: Current progress
            item {
                AppHorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (editor.unit != KeyResultUnit.Binary) {
                            AppOutlinedTextField(
                                value = if (editor.currentValue == 0.0) "" else editor.currentValue.toString(),
                                onValueChange = { onCurrentValueChange(it.toDoubleOrNull() ?: 0.0) },
                                modifier = Modifier.size(width = 80.dp, height = 40.dp),
                                placeholder = "0",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )
                        } else {
                            TextButton(
                                onClick = { onCurrentValueChange(if (editor.currentValue >= 1.0) 0.0 else 1.0) }
                            ) {
                                Text(if (editor.currentValue >= 1.0) "Completed" else "Not started")
                            }
                        }
                    }

                    val progress = if (editor.unit == KeyResultUnit.Binary) {
                        if (editor.currentValue >= 1.0) 1f else 0f
                    } else if (editor.targetValue > 0) {
                        (editor.currentValue / editor.targetValue).toFloat()
                    } else 0f

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete key result?") },
            text = { Text("This action cannot be undone. Are you sure you want to delete \"${editor.title}\"?") },
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

private enum class MeasurementCategory(
    val title: String,
    val description: String,
    val unit: KeyResultUnit
) {
    Items("By counting items", "number, things, pages, words", KeyResultUnit.Number),
    Time("By tracking time", "hours, minutes", KeyResultUnit.Hours),
    Consistency("By counting successful days", "consistency", KeyResultUnit.Days),
    Milestone("By a binary milestone", "Yes/No", KeyResultUnit.Binary),
    Percentage("By percentage", "% completion", KeyResultUnit.Percentage),
    Money("By money", "revenue, cost", KeyResultUnit.Currency),
    Points("By points", "scores, weight", KeyResultUnit.Points)
}

@Composable
private fun MeasurementCategorySelector(
    selectedUnit: KeyResultUnit,
    onUnitSelect: (KeyResultUnit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = MeasurementCategory.entries.find { it.unit == selectedUnit } ?: MeasurementCategory.Items
    
    Box {
        AppOutlinedTextField(
            value = selectedCategory.title,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select category",
                    modifier = Modifier.size(24.dp)
                )
            }
        )
        // Overlay box to capture clicks
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            MeasurementCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(category.title, style = MaterialTheme.typography.bodyLarge)
                            Text(category.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = {
                        onUnitSelect(category.unit)
                        expanded = false
                    }
                )
            }
        }
    }
}
