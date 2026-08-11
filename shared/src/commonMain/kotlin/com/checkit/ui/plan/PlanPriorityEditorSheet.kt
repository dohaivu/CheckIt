package com.checkit.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.checkit.domain.PlanPriority
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DeleteOverflowMenu
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.plan_add
import checkit.shared.generated.resources.plan_close
import checkit.shared.generated.resources.plan_complete
import checkit.shared.generated.resources.plan_delete_priority
import checkit.shared.generated.resources.plan_edit_priority_title
import checkit.shared.generated.resources.plan_new_priority
import checkit.shared.generated.resources.plan_note_label
import checkit.shared.generated.resources.plan_parent_label
import checkit.shared.generated.resources.plan_parent_none
import checkit.shared.generated.resources.plan_priority_title_label
import checkit.shared.generated.resources.plan_reopen
import checkit.shared.generated.resources.plan_save
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PlanPriorityEditorSheet(
    editor: PlanPriorityEditorState,
    parentCandidates: List<PlanPriority>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onParentChange: (Long?) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editor.mode == PlanEditorMode.Add) {
                        stringResource(Res.string.plan_new_priority)
                    } else {
                        stringResource(Res.string.plan_edit_priority_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (editor.mode == PlanEditorMode.Edit) {
                    DeleteOverflowMenu(
                        onDelete = onDelete,
                        label = stringResource(Res.string.plan_delete_priority)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.plan_close))
                }
            }

            AppOutlinedTextField(
                value = editor.title,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = stringResource(Res.string.plan_priority_title_label),
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions.Default
            )

            AppOutlinedTextField(
                value = editor.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.plan_note_label),
                minLines = 2,
                maxLines = 4
            )

            val availableParents = parentCandidates.filter { it.id != editor.priorityId }
            if (availableParents.isNotEmpty()) {
                ParentPicker(
                    selectedParentId = editor.parentId,
                    candidates = availableParents,
                    onParentChange = onParentChange
                )
            }


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }

                if (editor.mode == PlanEditorMode.Edit && editor.priorityId != null) {
                    Button(
                        onClick = { onToggleDone(editor.priorityId, !editor.isDone) },
                    ) {
                        Text(
                            stringResource(
                                if (editor.isDone) Res.string.plan_reopen else Res.string.plan_complete
                            )
                        )
                    }
                }

                Button(
                    onClick = onSave,
                    enabled = editor.title.isNotBlank()
                ) {
                    Text(
                        if (editor.mode == PlanEditorMode.Add) {
                            stringResource(Res.string.plan_add)
                        } else {
                            stringResource(Res.string.plan_save)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentPicker(
    selectedParentId: Long?,
    candidates: List<PlanPriority>,
    onParentChange: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = candidates.firstOrNull { it.id == selectedParentId }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.plan_parent_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected?.title ?: stringResource(Res.string.plan_parent_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.plan_parent_none)) },
                    onClick = {
                        onParentChange(null)
                        expanded = false
                    }
                )
                candidates.forEach { candidate ->
                    DropdownMenuItem(
                        text = { Text(candidate.title) },
                        onClick = {
                            onParentChange(candidate.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
