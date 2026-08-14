package com.checkit.ui.twelveweek

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.checkit.ui.tasks.views.ViewOptionChip

@Composable
internal fun ViewOptionsMenu(
    showCompletedTactic: Boolean,
    showCompletedCycle: Boolean,
    onShowCompletedTacticChange: (Boolean) -> Unit,
    onShowCompletedCycleChange: (Boolean) -> Unit
) {
    var isPopupOpen by remember { mutableStateOf(false) }
    val visibleState = remember { MutableTransitionState(false) }
    val hasActiveItemOptions = showCompletedTactic || showCompletedCycle

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(
            onClick = {
                isPopupOpen = true
                visibleState.targetState = true
            }
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = if (hasActiveItemOptions) "View options active" else "View options",
                    tint = if (hasActiveItemOptions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
                if (hasActiveItemOptions) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }

        if (isPopupOpen) {
            if (visibleState.isIdle && !visibleState.targetState) {
                isPopupOpen = false
            }

            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(x = 0, y = 130),
                onDismissRequest = { visibleState.targetState = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = scaleIn(
                        initialScale = 0.7f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(200)
                    ) + fadeIn(),
                    exit = scaleOut(
                        targetScale = 0.7f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(150)
                    ) + fadeOut()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .heightIn(min = 100.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            ViewOptionChip(
                                icon = if (showCompletedTactic) Icons.Default.CheckCircle else Icons.Default.TaskAlt,
                                label = if (showCompletedTactic) "Hide completed tasks" else "Show completed tasks",
                                selected = showCompletedTactic,
                                onClick = { onShowCompletedTacticChange(!showCompletedTactic) }
                            )

                            ViewOptionChip(
                                icon = if (showCompletedCycle) Icons.Default.CheckCircle else Icons.Default.TaskAlt,
                                label = if (showCompletedCycle) "Hide completed cycles" else "Show completed cycles",
                                selected = showCompletedCycle,
                                onClick = { onShowCompletedCycleChange(!showCompletedCycle) }
                            )
                        }
                    }
                }
            }
        }
    }
}