package com.checkit.ui.myday

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import com.checkit.domain.TagItem
import com.checkit.ui.theme.toColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedSprintFab(
    lastAction: FabAction,
    recentTags: List<TagItem>,
    onExecuteAction: (FabAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 45f else 0f)
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recentTags.fastForEachReversed { tag ->
                    FabMenuItem(
                        label = tag.name,
                        icon = Icons.AutoMirrored.Filled.Label,
                        color = tag.color.toColor(),
                        onClick = {
                            expanded = false
                            onExecuteAction(FabAction.TagSprint(tag))
                        }
                    )
                }
                FabMenuItem(
                    label = "Quick Sprint",
                    icon = Icons.Default.Bolt,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        expanded = false
                        onExecuteAction(FabAction.QuickSprint)
                    }
                )
            }
        }

        val fabColor = when (lastAction) {
            is FabAction.QuickSprint -> MaterialTheme.colorScheme.primary
            is FabAction.TagSprint -> lastAction.tag.color.toColor()
        }

        Surface(
            shape = FloatingActionButtonDefaults.smallShape,
            color = fabColor,
            contentColor = if (lastAction is FabAction.TagSprint) Color.White else MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(56.dp)
                .clip(FloatingActionButtonDefaults.smallShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = {
                        if (expanded) {
                            expanded = false
                        } else {
                            onExecuteAction(lastAction)
                        }
                    },
                    onLongClick = {
                        expanded = !expanded
                    }
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Sprint Actions",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun FabMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.clip(MaterialTheme.shapes.small)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = color,
            contentColor = if (color == MaterialTheme.colorScheme.tertiary) MaterialTheme.colorScheme.onTertiary else Color.White,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
