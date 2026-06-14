package com.checkit.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.checkit.domain.TaskFilter
import com.checkit.ui.TaskSortOption
import com.checkit.ui.TaskWorkspaceView
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.theme.materialIcon


@Composable
internal fun ViewOptionsMenu(
    showCompleted: Boolean,
    onShowCompletedChange: (Boolean) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    filters: List<TaskFilter>,
    selectedFilterId: Long?,
    selectFilter: (Long) -> Unit,
    availableViews: List<TaskWorkspaceView>,
    selectedView: TaskWorkspaceView,
    selectView: (view: TaskWorkspaceView) -> Unit,
    sortOption: TaskSortOption,
    selectSortOption: (TaskSortOption) -> Unit
) {
    var isPopupOpen by remember { mutableStateOf(false) }
    val visibleState = remember { MutableTransitionState(false) }
    val hasActiveItemOptions = showCompleted || searchText.isNotBlank() || selectedFilterId != null
    val scopeFilters = remember(filters) { filters.filterNot { it.isAllTasksFilter() } }

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
                            OptionSectionLabel("View")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableViews.forEach { view ->
                                    ViewOptionChip(
                                        icon = view.icon(),
                                        label = view.name,
                                        selected = selectedView == view,
                                        onClick = { selectView(view) }
                                    )
                                }
                            }

                            if (scopeFilters.isNotEmpty()) {
                                OptionSectionLabel("Scope")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    scopeFilters.forEach { filter ->
                                        ViewOptionChip(
                                            icon = materialIcon(filter.icon),
                                            label = filter.name,
                                            selected = selectedFilterId == filter.id,
                                            onClick = { selectFilter(filter.id) }
                                        )
                                    }
                                }
                            }

                            OptionSectionLabel("Items")
                            AppOutlinedTextField(
                                value = searchText,
                                onValueChange = onSearchTextChange,
                                placeholder = "Search tasks and notes",
                                maxLines = 1,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().widthIn(min = 220.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            )
                            ViewOptionChip(
                                icon = if (showCompleted) Icons.Default.CheckCircle else Icons.Default.TaskAlt,
                                label = if (showCompleted) "Hide completed" else "Show completed",
                                selected = showCompleted,
                                onClick = { onShowCompletedChange(!showCompleted) }
                            )

                            if (selectedView == TaskWorkspaceView.List) {
                                OptionSectionLabel("Sort")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TaskSortOption.entries.forEach { option ->
                                        ViewOptionChip(
                                            icon = option.icon(),
                                            label = option.label(),
                                            selected = sortOption == option,
                                            onClick = { selectSortOption(option) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun TaskFilter.isAllTasksFilter(): Boolean =
    tagId == null &&
        dueDatePreset == null &&
        status == null &&
        priority == null &&
        !includeTrashed

@Composable
private fun OptionSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
internal fun ViewOptionChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val backgroundColor = remember(selected, primaryContainer, surfaceVariant) {
        if (selected) primaryContainer else surfaceVariant.copy(alpha = ContentContainerAlpha)
    }

    val contentColor = remember(selected, onPrimaryContainer, onSurface) {
        if (selected) onPrimaryContainer else onSurface
    }

    val iconTint = remember(selected, primary, onSurfaceVariant) {
        if (selected) primary else onSurfaceVariant
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .background(backgroundColor)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun TaskSortOption.icon(): ImageVector =
    when (this) {
        TaskSortOption.Custom -> Icons.AutoMirrored.Filled.ViewList
        TaskSortOption.Priority -> Icons.Default.PriorityHigh
        TaskSortOption.Title -> Icons.Default.SortByAlpha
        TaskSortOption.Date -> Icons.Default.Event
    }

private fun TaskSortOption.label(): String =
    when (this) {
        TaskSortOption.Custom -> "Custom"
        TaskSortOption.Priority -> "Priority"
        TaskSortOption.Title -> "Title"
        TaskSortOption.Date -> "Date"
    }
