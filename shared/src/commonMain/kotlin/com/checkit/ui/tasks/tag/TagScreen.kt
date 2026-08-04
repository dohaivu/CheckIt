package com.checkit.ui.tasks.tag

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.checkit.domain.TagItem
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.theme.toColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun TagScreen(
    tags: List<TagItem>,
    selectedTagId: Long?,
    tagViewModel: TagViewModel,
    onTagClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by tagViewModel.uiState.collectAsState()
    var orderedTags by remember(tags) { mutableStateOf(tags) }
    var draggedTagId by remember { mutableStateOf<Long?>(null) }
    val draggedCenterY = remember { mutableFloatStateOf(0f) }
    val rowBounds = remember { mutableStateMapOf<Long, TagRowBounds>() }

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
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(orderedTags, key = { it.id }) { tag ->
                    val isDragging = draggedTagId == tag.id
                    TagRow(
                        tag = tag,
                        usageCount = state.tagUsageCounts[tag.id] ?: 0,
                        selected = selectedTagId == tag.id,
                        isDragging = isDragging,
                        draggedCenterY = draggedCenterY.floatValue,
                        draggedRowCenterY = rowBounds[tag.id]?.center,
                        onClick = { onTagClick(tag.id) },
                        onLongClick = { tagViewModel.openEditTag(tag) },
                        onDragStart = {
                            draggedTagId = tag.id
                            rowBounds[tag.id]?.let { draggedCenterY.floatValue = it.center }
                        },
                        onDrag = { delta ->
                            draggedCenterY.floatValue += delta
                            val fromIndex = orderedTags.indexOfFirst { it.id == tag.id }
                            val toIndex = orderedTags.indices.firstOrNull { index ->
                                if (index == fromIndex) return@firstOrNull false
                                val bounds = rowBounds[orderedTags[index].id] ?: return@firstOrNull false
                                draggedCenterY.floatValue in bounds.top..bounds.bottom
                            }
                            if (fromIndex >= 0 && toIndex != null) {
                                orderedTags = orderedTags.toMutableList().apply {
                                    add(toIndex, removeAt(fromIndex))
                                }
                            }
                        },
                        onDragEnd = {
                            if (draggedTagId != null) tagViewModel.updateTagSortOrders(orderedTags)
                            draggedTagId = null
                            draggedCenterY.floatValue = 0f
                        },
                        onBoundsChanged = { top, height ->
                            rowBounds[tag.id] = TagRowBounds(top, top + height)
                        }
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
    draggedCenterY: Float,
    draggedRowCenterY: Float?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onBoundsChanged: (Float, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .pointerInput(tag.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .animateTagPlacement(tag.id, isDragging) { top, height ->
                // The bounds exclude the temporary placement animation offset.
                onBoundsChanged(top, height)
            }
            .graphicsLayer {
                if (isDragging) {
                    translationY = draggedCenterY - (draggedRowCenterY ?: draggedCenterY)
                    scaleX = 1.03f
                    scaleY = 1.03f
                    shadowElevation = 10f
                    alpha = 0.98f
                } else {
                    scaleX = 1f
                    scaleY = 1f
                    shadowElevation = 0f
                    alpha = 1f
                }
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

private data class TagRowBounds(
    val top: Float,
    val bottom: Float
) {
    val center: Float get() = (top + bottom) / 2f
}

private fun Modifier.animateTagPlacement(
    key: Long,
    isDragging: Boolean,
    onPositioned: (Float, Int) -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val offsetY = remember(key) { Animatable(0f) }
    var previousTop by remember(key) { mutableStateOf<Float?>(null) }

    onGloballyPositioned { coordinates ->
        val nextTop = coordinates.positionInParent().y
        onPositioned(nextTop - offsetY.value, coordinates.size.height)

        if (isDragging) {
            previousTop = nextTop
            scope.launch { offsetY.snapTo(0f) }
            return@onGloballyPositioned
        }

        val lastTop = previousTop
        if (lastTop != null && lastTop != nextTop) {
            scope.launch {
                offsetY.snapTo(lastTop - nextTop)
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
        previousTop = nextTop
    }.offset { IntOffset(0, offsetY.value.roundToInt()) }
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
