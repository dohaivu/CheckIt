package com.checkit.ui.myday

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.sprint_action_continue_pomodoro
import checkit.shared.generated.resources.sprint_action_next_pomodoro
import checkit.shared.generated.resources.sprint_action_pomodoro
import checkit.shared.generated.resources.sprint_action_save
import checkit.shared.generated.resources.sprint_action_save_and_break
import checkit.shared.generated.resources.sprint_adhoc_placeholder
import checkit.shared.generated.resources.sprint_break_finish_subtitle
import checkit.shared.generated.resources.sprint_break_finish_title
import checkit.shared.generated.resources.sprint_finish_subtitle
import checkit.shared.generated.resources.sprint_finish_title
import checkit.shared.generated.resources.sprint_pomodoro_finish_subtitle
import checkit.shared.generated.resources.sprint_pomodoro_finish_title
import checkit.shared.generated.resources.sprint_start
import com.checkit.domain.SprintState
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskTag
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.TagPicker
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.myday.SprintChoice
import com.checkit.ui.tasks.views.DailyPlanTimelineCard
import com.checkit.ui.tasks.views.SprintButton
import com.checkit.ui.tasks.views.TaskTimelineCard
import org.jetbrains.compose.resources.stringResource

@Composable
fun SprintCompletionDialog(
    state: SprintState.Finished,
    onSaveWin: () -> Unit,
    onSaveAndBreak: () -> Unit,
    onContinueNewPomodoro: () -> Unit,
    onStartPomodoro: () -> Unit,
    onStartNextPomodoro: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when {
        state.isBreak -> stringResource(Res.string.sprint_break_finish_title)
        state.isPomodoro -> stringResource(Res.string.sprint_pomodoro_finish_title)
        else -> stringResource(Res.string.sprint_finish_title)
    }
    val subtitle = when {
        state.isBreak -> stringResource(Res.string.sprint_break_finish_subtitle)
        state.isPomodoro -> stringResource(Res.string.sprint_pomodoro_finish_subtitle)
        else -> stringResource(Res.string.sprint_finish_subtitle)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(subtitle)
                if (state.taskId == null && !state.isBreak) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Goal: ${state.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when {
                    state.isBreak -> {
                        Button(
                            onClick = onStartNextPomodoro,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.sprint_action_next_pomodoro))
                        }
                    }
                    state.isPomodoro -> {
                        Button(
                            onClick = onSaveAndBreak,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.sprint_action_save_and_break))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onContinueNewPomodoro,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.sprint_action_continue_pomodoro))
                        }
                    }
                    else -> {
                        Button(
                            onClick = onStartPomodoro,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.sprint_action_pomodoro))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onSaveWin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(if (state.isBreak) Res.string.cancel else Res.string.sprint_action_save))
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun SprintBar(
    state: SprintState.Running,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    val timeLabel = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    val progress = (state.totalSeconds - state.remainingSeconds).toFloat() / state.totalSeconds.toFloat()

    val containerColor = when {
        state.isBreak -> MaterialTheme.colorScheme.secondaryContainer
        state.isPomodoro -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val progressColor = when {
        state.isBreak -> MaterialTheme.colorScheme.secondary
        state.isPomodoro -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.widthIn(min = 320.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.description,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isPaused) {
                            PulsingDot(color = progressColor)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = when {
                                isPaused -> "Paused"
                                state.isBreak -> "Short Break"
                                state.isPomodoro -> "Deep Focus"
                                else -> "Quick Sprint"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(
                        onClick = if (isPaused) onResume else onPause,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onStop,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Stop",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        Modifier
            .size(8.dp)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}

@Composable
fun QuickSprintSheet(
    suggestedToday: List<SprintChoice>,
    suggestedYesterday: List<SprintChoice>,
    suggestedTasks: List<TaskItem>,
    availableTags: List<TaskTag>,
    continueItem: SprintChoice?,
    onStartSprint: (taskId: Long?, dailyPlanItemId: Long?, description: String, tagIds: List<Long>) -> Unit,
    onStartSprintWithChoice: (SprintChoice) -> Unit,
    onStartSprintWithTask: (TaskItem) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedTagIds by remember { mutableStateOf(emptySet<Long>()) }

    AppEditorBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.sprint_adhoc_placeholder),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            AppOutlinedTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                ),
                placeholder = "e.g., Focus on coding",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                contentPadding = PaddingValues(12.dp)
            )

            TagPicker(
                availableTags = availableTags,
                selectedTagIds = selectedTagIds,
                onTagToggle = { tagId ->
                    selectedTagIds = if (tagId in selectedTagIds) {
                        selectedTagIds - tagId
                    } else {
                        selectedTagIds + tagId
                    }
                },
                modifier = Modifier.align(Alignment.Start)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        onStartSprint(null, null, text, selectedTagIds.toList())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.sprint_start))
                }

                if (continueItem != null) {
                    OutlinedButton(
                        onClick = {
                            onStartSprintWithChoice(continueItem)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Continue Last")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (suggestedToday.isNotEmpty()) {
                item { SectionLabel("Today's Plan", modifier = Modifier.padding(horizontal = 16.dp)) }
                items(suggestedToday) { choice ->
                    SprintChoiceCard(
                        choice = choice,
                        onClick = {
                            onStartSprintWithChoice(choice)
                            onDismiss()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (suggestedYesterday.isNotEmpty()) {
                item { SectionLabel("Yesterday's Leftovers", modifier = Modifier.padding(horizontal = 16.dp)) }
                items(suggestedYesterday) { choice ->
                    SprintChoiceCard(
                        choice = choice,
                        onClick = {
                            onStartSprintWithChoice(choice)
                            onDismiss()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (suggestedTasks.isNotEmpty()) {
                item { SectionLabel("Or start with a task", modifier = Modifier.padding(horizontal = 16.dp)) }
                items(suggestedTasks, key = { it.id }) { task ->
                    TaskTimelineCard(
                        task = task,
                        timeLabel = task.doDate?.localizedCompactDateWithDayName() ?: task.objective.name,
                        trailingContent = {
                            SprintButton(onClick = {
                                onStartSprintWithTask(task)
                                onDismiss()
                            })
                        },
                        onClick = {
                            onStartSprintWithTask(task)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 4.dp, top = 8.dp)
    )
}

@Composable
private fun SprintChoiceCard(
    choice: SprintChoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (choice) {
        is SprintChoice.Task -> TaskTimelineCard(
            task = choice.task,
            trailingContent = { SprintButton(onClick = onClick) },
            onClick = onClick,
            modifier = modifier
        )
        is SprintChoice.PlanItem -> {
            val task = choice.task
            if (task != null) {
                TaskTimelineCard(
                    task = task,
                    timeLabel = choice.item.title,
                    trailingContent = { SprintButton(onClick = onClick) },
                    onClick = onClick,
                    modifier = modifier
                )
            } else {
                DailyPlanTimelineCard(
                    item = choice.item,
                    isOverdue = false,
                    trailingContent = { SprintButton(onClick = onClick) },
                    onClick = onClick,
                    modifier = modifier
                )
            }
        }
    }
}
