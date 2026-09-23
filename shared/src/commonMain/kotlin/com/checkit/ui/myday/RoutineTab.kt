package com.checkit.ui.myday

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.checkit.domain.Routine
import com.checkit.domain.RoutineLog
import com.checkit.domain.RoutineStepTemplate
import com.checkit.domain.routinePercent
import com.checkit.domain.usecase.toClockLabel
import com.checkit.ui.components.TimePicker
import com.checkit.ui.reflect.RoutineHeatmapSection
import com.checkit.ui.reflect.buildRoutineSeries
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

private data class RoutineEditorState(
    val id: String?,
    val title: String,
    val reminderMinutes: Int?,
    val steps: List<RoutineStepTemplate>
)

@Composable
internal fun RoutineTab(
    routines: List<Routine>,
    checks: Map<String, Set<String>>,
    logs: List<RoutineLog>,
    today: LocalDate,
    onToggleStep: (String, String) -> Unit,
    onSaveRoutine: (String?, String, Int?, List<RoutineStepTemplate>) -> Unit,
    onDeleteRoutine: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val series = remember(routines, logs, today) { buildRoutineSeries(routines, logs, today) }
    var editor by remember { mutableStateOf<RoutineEditorState?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's routines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = {
                        editor = RoutineEditorState(id = null, title = "", reminderMinutes = null, steps = emptyList())
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(text = "New routine")
                }
            }
        }
        if (routines.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Text(
                        text = "No routines yet. Create one to start a daily checklist that resets tomorrow.",
                        modifier = Modifier.padding(22.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(routines, key = { it.id }) { routine ->
            RoutineCard(
                routine = routine,
                checkedStepIds = checks[routine.id].orEmpty(),
                onToggleStep = { stepId -> onToggleStep(routine.id, stepId) },
                onEdit = {
                    editor = RoutineEditorState(
                        id = routine.id,
                        title = routine.title,
                        reminderMinutes = routine.reminderMinutes,
                        steps = routine.steps
                    )
                },
                onDelete = { onDeleteRoutine(routine.id) }
            )
        }
        item {
            RoutineHeatmapSection(series = series)
        }
    }

    editor?.let { state ->
        RoutineEditorDialog(
            state = state,
            onDismiss = { editor = null },
            onSave = { id, title, reminderMinutes, steps ->
                onSaveRoutine(id, title, reminderMinutes, steps)
                editor = null
            }
        )
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    checkedStepIds: Set<String>,
    onToggleStep: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validStepIds = remember(routine.steps) { routine.steps.map { it.id }.toSet() }
    val percent = remember(routine.steps, checkedStepIds) {
        routinePercent(routine.steps.size, checkedStepIds, validStepIds)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = routine.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit routine")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete routine")
                }
            }
            routine.reminderMinutes?.let { minutes ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = minutes.toClockLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (routine.steps.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            routine.steps.forEach { step ->
                val checked = step.id in checkedStepIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleStep(step.id) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleStep(step.id) }
                    )
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (checked) TextDecoration.LineThrough else null
                        ),
                        color = if (checked) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineEditorDialog(
    state: RoutineEditorState,
    onDismiss: () -> Unit,
    onSave: (String?, String, Int?, List<RoutineStepTemplate>) -> Unit
) {
    var title by remember(state) { mutableStateOf(state.title) }
    var reminderMinutes by remember(state) { mutableStateOf(state.reminderMinutes) }
    var steps by remember(state) { mutableStateOf(state.steps) }
    var newStepTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (state.id == null) "New routine" else "Edit routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TimePicker(
                    label = "reminder",
                    timeMinutes = reminderMinutes,
                    initialTimeMinutes = 8 * 60,
                    onTimeChange = { reminderMinutes = it }
                )
                steps.forEach { step ->
                    var stepTitle by remember(step.id) { mutableStateOf(step.title) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = stepTitle,
                            onValueChange = { value ->
                                stepTitle = value
                                steps = steps.map { row ->
                                    if (row.id == step.id) row.copy(title = value) else row
                                }
                            },
                            label = { Text("Step") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { steps = steps.filterNot { row -> row.id == step.id } }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove step")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = newStepTitle,
                        onValueChange = { newStepTitle = it },
                        label = { Text("New step") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val trimmed = newStepTitle.trim()
                            if (trimmed.isNotEmpty()) {
                                steps = steps + RoutineStepTemplate(
                                    id = Uuid.random().toString(),
                                    title = trimmed,
                                    sortOrder = steps.size
                                )
                                newStepTitle = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(state.id, title.trim(), reminderMinutes, steps.filter { it.title.isNotBlank() })
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
