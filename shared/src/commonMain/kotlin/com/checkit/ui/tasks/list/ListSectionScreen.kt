package com.checkit.ui.tasks.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.checkit.domain.ListSection
import com.checkit.ui.theme.toColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListSectionScreen(
    viewModel: ListSectionViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    
    val rowBounds = remember { mutableStateMapOf<Long, SectionRowBounds>() }
    val draggedIndex = remember { mutableIntStateOf(-1) }
    val draggedCenterY = remember { mutableFloatStateOf(0f) }

    val draggedSectionId = remember(draggedIndex.intValue, state.sections) {
        state.sections.getOrNull(draggedIndex.intValue)?.id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("List Sections") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openNewSection) {
                Icon(Icons.Default.Add, contentDescription = "Add Section")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.sections.forEachIndexed { index, section ->
                val sectionId = section.id
                val isDragging = draggedSectionId == sectionId
                
                key(sectionId) {
                    SectionItem(
                        section = section,
                        isDragging = isDragging,
                        onEditClick = { viewModel.openEditSection(section) },
                        onMove = { dragAmountY ->
                            val currentDraggedIndex = state.sections.indexOfFirst { it.id == draggedSectionId }
                            if (currentDraggedIndex == -1) return@SectionItem
                            
                            draggedCenterY.floatValue += dragAmountY
                            
                            val targetIndex = state.sections.indices.firstOrNull { i ->
                                if (i == currentDraggedIndex) return@firstOrNull false
                                val key = state.sections[i].id
                                val bounds = rowBounds[key] ?: return@firstOrNull false
                                draggedCenterY.floatValue in bounds.top..bounds.bottom
                            } ?: return@SectionItem
                            
                            val newSections = state.sections.toMutableList()
                            val item = newSections.removeAt(currentDraggedIndex)
                            newSections.add(targetIndex, item)
                            viewModel.reorderSections(newSections)
                            draggedIndex.intValue = targetIndex
                        },
                        onDragStart = {
                            rowBounds[sectionId]?.let { bounds ->
                                draggedIndex.intValue = index
                                draggedCenterY.floatValue = bounds.center
                            }
                        },
                        onDragEnd = {
                            draggedIndex.intValue = -1
                            draggedCenterY.floatValue = 0f
                        },
                        modifier = Modifier
                            .animateSectionPlacement(sectionId, isDragging) { baseTop, height ->
                                rowBounds[sectionId] = SectionRowBounds(
                                    top = baseTop,
                                    bottom = baseTop + height
                                )
                            }
                            .graphicsLayer {
                                val center = rowBounds[sectionId]?.center ?: 0f
                                translationY = if (isDragging) draggedCenterY.floatValue - center else 0f
                                scaleX = if (isDragging) 1.02f else 1f
                                scaleY = if (isDragging) 1.02f else 1f
                                shadowElevation = if (isDragging) 8f else 0f
                                shape = RoundedCornerShape(12.dp)
                            }
                    )
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    state.editor?.let { editor ->
        ListSectionEditorSheet(
            editor = editor,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveEditor,
            onDelete = viewModel::deleteEditorSection,
            onTitleChange = viewModel::updateTitle,
            onColorChange = viewModel::updateColor
        )
    }
}

@Composable
private fun SectionItem(
    section: ListSection,
    isDragging: Boolean,
    onEditClick: () -> Unit,
    onMove: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnMove by rememberUpdatedState(onMove)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(section.color.toColor())
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = section.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { currentOnDragStart() },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnMove(dragAmount.y)
                            }
                        )
                    }
            )
        }
    }
}

private data class SectionRowBounds(
    val top: Float,
    val bottom: Float
) {
    val center: Float get() = (top + bottom) / 2f
}

private fun Modifier.animateSectionPlacement(
    key: Any,
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
    }.offset { IntOffset(x = 0, y = offsetY.value.roundToInt()) }
}
