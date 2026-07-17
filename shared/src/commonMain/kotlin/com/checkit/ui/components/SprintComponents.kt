package com.checkit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.sprint_action_log_task
import checkit.shared.generated.resources.sprint_action_pomodoro
import checkit.shared.generated.resources.sprint_action_save
import checkit.shared.generated.resources.sprint_finish_subtitle
import checkit.shared.generated.resources.sprint_finish_title
import com.checkit.domain.SprintState
import org.jetbrains.compose.resources.stringResource

@Composable
fun SprintTimerOverlay(
    state: SprintState.Running,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    val timeLabel = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    val backgroundColor = if (state.isPomodoro) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.description,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Light
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
                
                Spacer(modifier = Modifier.width(32.dp))
                
                IconButton(
                    onClick = if (isPaused) onResume else onPause,
                    modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SprintCompletionDialog(
    state: SprintState.Finished,
    onSaveWin: () -> Unit,
    onStartPomodoro: () -> Unit,
    onLogTask: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.sprint_finish_title))
        },
        text = {
            Column {
                Text(stringResource(Res.string.sprint_finish_subtitle))
                if (state.dailyPlanItemId == null) {
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
                if (!state.isPomodoro) {
                    Button(
                        onClick = onStartPomodoro,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.sprint_action_pomodoro))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedButton(
                    onClick = if (state.dailyPlanItemId != null) onSaveWin else onLogTask,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(if (state.dailyPlanItemId != null) Res.string.sprint_action_save else Res.string.sprint_action_log_task))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
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

    val backgroundColor = if (state.isPomodoro) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                Text(
                    text = if (isPaused) "Paused" else if (state.isPomodoro) "Deep Focus" else "Quick Sprint",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = timeLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = if (isPaused) onResume else onPause,
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Stop", 
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
