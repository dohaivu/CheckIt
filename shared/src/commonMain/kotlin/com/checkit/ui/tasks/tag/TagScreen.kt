package com.checkit.ui.tasks.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.TagItem
import com.checkit.ui.components.ReorderDragState
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.components.findReorderTarget
import com.checkit.ui.components.reorderableRowGraphics
import com.checkit.ui.theme.toColor

@Composable
internal fun TagScreen(
    tags: List<TagItem>,
    selectedTagId: String?,
    tagViewModel: TagViewModel,
    onTagClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by tagViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var orderedTags by remember(tags) { mutableStateOf(tags) }
    val dragState = remember { ReorderDragState(scope) }
    val currentTags by rememberUpdatedState(orderedTags)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = tagViewModel::openNewTag) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }
            )
        }
    ) { padding ->
        if (tags.isEmpty()) {
            TagEmptyState(
                onAddClick = tagViewModel::openNewTag,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(orderedTags, key = { it.id }) { tag ->
                    val isDragging = dragState.draggingKey == tag.id || dragState.previousKey == tag.id
                    TagRow(
                        tag = tag,
                        usageCount = state.tagUsageCounts[tag.id] ?: 0,
                        selected = selectedTagId == tag.id,
                        isDragging = isDragging,
                        onClick = { onTagClick(tag.id) },
                        onLongClick = { tagViewModel.openEditTag(tag) },
                        onDragStart = {
                            dragState.onDragStart(tag.id)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { delta ->
                            dragState.onDrag(delta)
                            val latest = currentTags
                            val key = dragState.draggingKey
                            if (key != null) {
                                val fromIndex = latest.indexOfFirst { it.id == key }
                                if (fromIndex >= 0) {
                                    val targetIndex = findReorderTarget(
                                        count = latest.size,
                                        keyAt = { index -> latest[index].id },
                                        bounds = dragState.bounds,
                                        draggedKey = key,
                                        draggedDelta = dragState.draggingOffset,
                                        fromIndex = fromIndex
                                    )
                                    if (targetIndex != null && targetIndex != fromIndex) {
                                        orderedTags = latest.toMutableList().apply {
                                            add(targetIndex, removeAt(fromIndex))
                                        }
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            val wasDragging = dragState.draggingKey != null
                            dragState.onDragEnd()
                            if (wasDragging) {
                                tagViewModel.updateTagSortOrders(orderedTags)
                            }
                        },
                        modifier = Modifier.reorderableRowGraphics(tag.id, dragState)
                    )
                }
            }
        }
    }
}

@Composable
private fun TagRow(
    tag: TagItem,
    usageCount: Int,
    selected: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val rowShape = RoundedCornerShape(12.dp)
    val backgroundColor = when {
        isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = when {
        isDragging -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, rowShape)
            .border(
                width = if (isDragging) 1.5.dp else 1.dp,
                color = borderColor,
                shape = rowShape
            )
            .clip(rowShape)
            .clickable(onClick = onClick)
            .pointerInput(tag.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(tag.color.toColor(), CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tag.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        if (usageCount > 0) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = usageCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Reorder tag",
            modifier = Modifier.pointerInput(tag.id) {
                detectDragGestures(
                    onDragStart = { currentOnDragStart() },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount.y)
                    }
                )
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TagEmptyState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalOffer,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No tags yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tags help you organize and filter your tasks. Tap the button below to create your first one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create tag",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
