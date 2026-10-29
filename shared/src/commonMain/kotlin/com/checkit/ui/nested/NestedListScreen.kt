package com.checkit.ui.nested

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.nested_batch_delete
import checkit.shared.generated.resources.nested_confirm_delete
import checkit.shared.generated.resources.nested_delete_confirm
import checkit.shared.generated.resources.nested_delete_confirm_multiple
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedMetricSummary
import com.checkit.domain.filterNestedTreeWithContext
import com.checkit.domain.moveDestinations
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun NestedListScreen(
    state: NestedEditorState.Active,
    viewModel: NestedListsViewModel,
    modifier: Modifier = Modifier,
    onAddToDailyPlan: (title: String, tagIds: List<String>, nestedListItemId: String?) -> Unit = { _, _, _ -> },
    onCopyToTask: (title: String, note: String?, subtaskTexts: List<String>) -> Unit = { _, _, _ -> }
) {
    var detailsItemId by remember { mutableStateOf<String?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    val tree = state.tree
    val focusedNode = state.focusedItem
    val unfilteredRoots = remember(focusedNode, tree.rootNodes) {
        focusedNode?.let { listOf(it) } ?: tree.rootNodes
    }
    // Typing shouldn't re-filter the whole outline on every keystroke (macOS
    // debounces the same way). The text field keeps the immediate query; the
    // outline follows the settled one. Clearing applies immediately.
    var settledQuery by remember(state.documentId) { mutableStateOf(state.filters.query) }
    LaunchedEffect(state.filters.query) {
        val query = state.filters.query
        if (query.isEmpty()) {
            settledQuery = ""
        } else {
            delay(150)
            settledQuery = query
        }
    }
    val effectiveFilters = state.filters.copy(query = settledQuery)
    val outlineProjection = remember(unfilteredRoots, effectiveFilters) {
        if (effectiveFilters.isVisible || effectiveFilters.displayType != NestedDisplayType.All) {
            filterNestedTreeWithContext(
                roots = unfilteredRoots,
                start = effectiveFilters.focus?.start,
                end = effectiveFilters.focus?.endInclusive,
                query = effectiveFilters.query,
                hideChecked = effectiveFilters.hideChecked,
                selectedTagIds = effectiveFilters.selectedTagIds,
                workingOnly = effectiveFilters.displayType == NestedDisplayType.Working
            )
        } else {
            null
        }
    }
    val visibleRoots = outlineProjection?.roots ?: unfilteredRoots
    // Ancestors kept only to hold matches under them recede visually.
    val contextIds = remember(outlineProjection) { outlineProjection?.contextIds?.toSet().orEmpty() }
    val visibleRows = remember(visibleRoots, contextIds) {
        flattenVisibleNodes(visibleRoots, contextIds)
    }
    val breadcrumbs = remember(state.zoomPath, tree.rootNodes) { buildBreadcrumbs(state, tree.rootNodes) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
            if (state.filters.isVisible) {
                NestedListFilterBar(
                    focus = state.filters.focus,
                    query = state.filters.query,
                    hideChecked = state.filters.hideChecked,
                    isActive = state.filters.isActive,
                    availableTags = state.availableTags,
                    selectedTagIds = state.filters.selectedTagIds,
                    onFocusChange = viewModel::updateFilterFocus,
                    onQueryChange = viewModel::updateFilterQuery,
                    onHideCheckedChange = { viewModel.toggleHideChecked() },
                    onTagToggle = viewModel::updateFilterTags,
                    onReset = viewModel::resetFilters,
                    onPreviousPeriod = viewModel::previousFilterPeriod,
                    onNextPeriod = viewModel::nextFilterPeriod,
                    onCurrentPeriod = viewModel::currentFilterPeriod
                )
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BreadcrumbBar(
                    breadcrumbs = breadcrumbs,
                    onCrumbClick = { itemId -> viewModel.zoomToItem(itemId) },
                    onRootClick = viewModel::zoomToRoot
                )

                NestedDisplayTypeMenu(
                    selected = state.filters.displayType,
                    onSelect = viewModel::updateDisplayType
                )
            }

            if (state.selection.isActive) {
                SelectionToolbar(
                    state = state,
                    onToggleDone = { viewModel.batchSetChecked(true) },
                    onDelete = viewModel::requestDeleteSelected,
                    onExit = viewModel::exitSelectionMode
                )
            } else {
                EditorToolbar(
                    state = state,
                    onZoomIn = viewModel::zoomInSelected,
                    onZoomOut = viewModel::zoomOut,
                    onExpandAll = viewModel::expandAll,
                    onCollapseAll = viewModel::collapseAll,
                    onIndent = { state.selectedItemId?.let(viewModel::indent) },
                    onOutdent = { state.selectedItemId?.let(viewModel::outdent) },
                    onMoveUp = { state.selectedItemId?.let(viewModel::moveUp) },
                    onMoveDown = { state.selectedItemId?.let(viewModel::moveDown) },
                    onAddChild = { state.selectedItemId?.let(viewModel::startAddChild) },
                    onAddSibling = { state.selectedItemId?.let(viewModel::startAddSibling) },
                    onDelete = viewModel::requestDeleteSelected,
                    onAddRoot = viewModel::startAddRoot,
                    onManageDetails = { state.selectedItemId?.let { detailsItemId = it } },
                    onEnterSelection = viewModel::enterSelectionMode,
                    onSortChildren = { showSortDialog = true },
                    onMoveTo = { showMoveDialog = true },
                    focusSelectionEnabled = state.focusSelectionEnabled,
                    onToggleFocusSelection = viewModel::toggleFocusSelection,
                    onAddToDailyPlan = {
                        state.selectedItemId?.let { id ->
                            state.tree.nodeById[id]?.item?.let { item ->
                                val parentTitle = state.tree.nodeById[item.parentId]?.item?.text
                                val title = parentTitle?.let { "$it - ${item.text}" } ?: item.text
                                onAddToDailyPlan(title, item.tags.map { it.id }, item.id)
                            }
                        }
                    },
                    onCopyToTask = {
                        state.selectedItemId?.let { id ->
                            state.tree.nodeById[id]?.let { node ->
                                onCopyToTask(
                                    node.item.text,
                                    node.item.note,
                                    node.children.sortedBy { it.item.position }.map { it.item.text }
                                )
                            }
                        }
                    }
                )
            }
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            val addingItem = state.overlay as? NestedEditorOverlay.AddingItem
            val listState = rememberLazyListState()
            val haptics = LocalHapticFeedback.current
            val density = LocalDensity.current
            var drag by remember { mutableStateOf<DragDropState?>(null) }

            val draggedDescendantIds = remember<Set<String>>(drag?.itemId, state.tree) {
                val id = drag?.itemId
                val node = if (id != null) state.tree.nodeById[id] else null
                if (node != null && node.hasChildren) {
                    val result = HashSet<String>()
                    val stack = ArrayDeque<NestedItemNode>()
                    stack.addAll(node.children)
                    while (stack.isNotEmpty()) {
                        val curr = stack.removeLast()
                        result.add(curr.item.id)
                        stack.addAll(curr.children)
                    }
                    result
                } else {
                    emptySet()
                }
            }
            val effectiveVisibleRows = remember(visibleRows, draggedDescendantIds) {
                if (draggedDescendantIds.isNotEmpty()) {
                    visibleRows.filterNot { it.node.item.id in draggedDescendantIds }
                } else {
                    visibleRows
                }
            }
            val focusActive = state.focusSelectionEnabled && !state.selection.isActive
            val selectionBranch = remember(
                state.tree,
                state.selectedItemId,
                effectiveVisibleRows,
                state.selection.isActive
            ) {
                if (state.selection.isActive) {
                    null
                } else {
                    selectionBranchOf(effectiveVisibleRows, state.tree, state.selectedItemId)
                }
            }
            val focusRange = if (focusActive) selectionBranch?.range else null
            val focusAncestors = remember(state.tree, selectionBranch, focusActive) {
                if (focusActive) {
                    focusAncestorIdsOf(state.tree, selectionBranch?.rootId)
                } else {
                    emptySet()
                }
            }

            fun updateDropTarget(current: DragDropState) {
                val resolved = resolveDropTarget(
                    tree = state.tree,
                    rows = effectiveVisibleRows,
                    layoutInfo = listState.layoutInfo,
                    drag = current,
                    density = density
                )
                if (resolved != null) {
                    current.targetParentId = resolved.parentId
                    current.targetIndex = resolved.index
                    current.indicatorRowId = resolved.rowId
                    current.below = resolved.below
                    current.indicatorDepth = resolved.depth
                    current.hasResolved = true
                }
            }

            // Edge auto-scroll while dragging with proportional velocity and real-time drop target updates.
            LaunchedEffect(drag?.itemId) {
                val active = drag ?: return@LaunchedEffect
                while (true) {
                    val info = listState.layoutInfo
                    val edge = with(density) { 56.dp.toPx() }
                    val maxVelocity = with(density) { 16.dp.toPx() }
                    val viewportY = active.pointerViewportY
                    val scrolled = when {
                        viewportY < edge && listState.canScrollBackward -> {
                            val factor = ((edge - viewportY) / edge).coerceIn(0.1f, 1f)
                            listState.scrollBy(-maxVelocity * factor)
                            true
                        }
                        viewportY > (info.viewportEndOffset - edge) && listState.canScrollForward -> {
                            val factor = ((viewportY - (info.viewportEndOffset - edge)) / edge).coerceIn(0.1f, 1f)
                            listState.scrollBy(maxVelocity * factor)
                            true
                        }
                        else -> false
                    }
                    if (scrolled) {
                        updateDropTarget(active)
                    }
                    delay(16)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (addingItem != null && addingItem.draft.anchorId == null) {
                    item(key = "new-root") {
                        NewItemRow(
                            depth = 0,
                            text = addingItem.draft.text,
                            onTextChange = viewModel::updateNewItemText,
                            onCommit = { viewModel.commitNewItem(thenContinue = false) },
                            onCancel = viewModel::cancelAddItem,
                            continuingLevels = emptySet()
                        )
                    }
                }
                if (effectiveVisibleRows.isEmpty()) {
                    if (addingItem == null || addingItem.draft.anchorId != null) {
                        item(key = "empty-list") {
                            EmptyNestedList(onAddItem = viewModel::startAddRoot)
                        }
                    }
                } else {
                    itemsIndexed(
                        effectiveVisibleRows,
                        key = { _, row -> row.node.item.id }
                    ) { index, row ->
                        val isDragged = drag?.itemId == row.node.item.id
                        val indicator = drag?.takeIf { it.indicatorRowId == row.node.item.id }
                        Box(
                            modifier = Modifier
                                .animateItem()
                                .zIndex(if (isDragged) 2f else 0f)
                        ) {
                            Column {
                                if (indicator != null && !indicator.below) {
                                    DropIndicatorLine(indicator.indicatorDepth)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            translationY = if (isDragged) drag?.totalDragY ?: 0f else 0f
                                            translationX = if (isDragged) drag?.totalDragX ?: 0f else 0f
                                            shadowElevation =
                                                if (isDragged) with(density) { 8.dp.toPx() } else 0f
                                            alpha = if (isDragged) 0.92f else 1f
                                        }
                                        .then(
                                            if (isDragged) {
                                                Modifier.background(
                                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    RoundedCornerShape(8.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .pointerInput(row.node.item.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { startOffset ->
                                                    if (!viewModel.canStartDrag()) return@detectDragGesturesAfterLongPress
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val entry = listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.key == row.node.item.id }
                                                    val initialY = (entry?.offset ?: 0) + startOffset.y.roundToInt()
                                                    val newState = DragDropState(
                                                        itemId = row.node.item.id,
                                                        initialDepth = row.depth
                                                    ).apply {
                                                        pointerViewportY = initialY
                                                        indicatorDepth = row.depth
                                                    }
                                                    drag = newState
                                                    updateDropTarget(newState)
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    val current = drag ?: return@detectDragGesturesAfterLongPress
                                                    current.totalDragY += amount.y
                                                    current.totalDragX += amount.x
                                                    current.pointerViewportY += amount.y.roundToInt()
                                                    updateDropTarget(current)
                                                },
                                                onDragEnd = {
                                                    val finished = drag
                                                    drag = null
                                                    if (finished != null && finished.hasResolved) {
                                                        viewModel.moveItemTo(
                                                            finished.itemId,
                                                            finished.targetParentId,
                                                            finished.targetIndex
                                                        )
                                                    }
                                                },
                                                onDragCancel = { drag = null }
                                            )
                                        }
                                ) {
                                    NestedItemRow(
                                        node = row.node,
                                        depth = row.depth,
                                        isInCheckedBranch = row.isInCheckedBranch,
                                        continuingLevels = row.continuingLevels,
                                        isContext = row.isContext,
                                        spineLevel = selectionBranch
                                            ?.takeIf { branch ->
                                                index in branch.range &&
                                                    branch.rootId != row.node.item.id
                                            }
                                            ?.depth,
                                        isSpineRoot = selectionBranch?.rootId == row.node.item.id,
                                        focusTier = focusTier(
                                            index = index,
                                            itemId = row.node.item.id,
                                            range = focusRange,
                                            ancestorIds = focusAncestors
                                        ),
                                        state = state,
                                        viewModel = viewModel,
                                        isDragActive = drag != null,
                                        isDragged = isDragged
                                    )
                                }
                                if (indicator != null && indicator.below) {
                                    DropIndicatorLine(indicator.indicatorDepth)
                                }
                            }
                        }
                        if (addingItem != null && addingItem.draft.anchorId == row.node.item.id) {
                            NewItemRow(
                                depth = addingItem.draft.depth,
                                text = addingItem.draft.text,
                                onTextChange = viewModel::updateNewItemText,
                                onCommit = { viewModel.commitNewItem(thenContinue = true) },
                                onCancel = viewModel::cancelAddItem,
                                continuingLevels = row.continuingLevels
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
            if (!state.selection.isActive) {
                val selectedItem = state.selectedItemId?.let { tree.itemById[it] }
                if (selectedItem != null) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        NestedFormattingBar(
                            item = selectedItem,
                            hasChildren = state.tree.nodeById[selectedItem.id]?.hasChildren == true,
                            onFormattingChange = { style, textColor, backgroundColor ->
                                viewModel.updateItemFormatting(selectedItem.id, style, textColor, backgroundColor)
                            },
                            onBorderChange = { shape, style, color ->
                                viewModel.updateItemBorder(selectedItem.id, shape, style, color)
                            },
                            onLabelChange = { label ->
                                viewModel.updateItemLabel(selectedItem.id, label)
                            },
                            onPriorityChange = { priority ->
                                viewModel.updateItemPriority(selectedItem.id, priority)
                            },
                            onGuideDotChange = { emoji ->
                                viewModel.updateItemGuideDot(selectedItem.id, emoji)
                            },
                            onDateRangeChange = { startDate, endDate ->
                                viewModel.updateItemDateRange(selectedItem.id, startDate, endDate)
                            },
                            availableTags = state.availableTags,
                            onTagsChange = { viewModel.updateItemTags(selectedItem.id, it) },
                            onToggleNote = { viewModel.startEditNote(selectedItem.id) },
                            onToggleCheckbox = { viewModel.toggleCheckboxEnabled(selectedItem.id) },
                            onEnableCheckboxForChildren = { viewModel.enableCheckboxForChildren(selectedItem.id) },
                            onSetChecked = { checked -> viewModel.setChecked(selectedItem.id, checked) },
                            onManageDetails = { detailsItemId = selectedItem.id }
                        )
                    }
                }
            }
        }
    }

    when (val overlay = state.overlay) {
        is NestedEditorOverlay.ConfirmDelete -> {
            val deleteCount = overlay.itemIds.size
            val singleItemText = if (deleteCount == 1) {
                overlay.itemIds.firstOrNull()?.let { state.tree.itemById[it]?.text } ?: ""
            } else {
                ""
            }
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteConfirm,
                title = { Text(stringResource(Res.string.nested_confirm_delete)) },
                text = {
                    if (deleteCount <= 1) {
                        Text(stringResource(Res.string.nested_delete_confirm, singleItemText))
                    } else {
                        Text(stringResource(Res.string.nested_delete_confirm_multiple, deleteCount))
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteSelected) {
                        Text(stringResource(Res.string.nested_batch_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDeleteConfirm) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }
        is NestedEditorOverlay.EditingNote -> {
            NestedNoteEditorSheet(
                itemId = overlay.itemId,
                initialText = overlay.initialText,
                onDismiss = viewModel::stopEditNote,
                onSave = { note -> viewModel.saveItemNote(overlay.itemId, note) }
            )
        }
        else -> {}
    }

    detailsItemId?.let { itemId ->
        state.tree.nodeById[itemId]?.let { node ->
            NestedItemDetailsDialog(
                node = node,
                summary = state.tree.metricSummaryById[itemId] ?: NestedMetricSummary(),
                onDismiss = { detailsItemId = null },
                onSave = { mins, pol, show, progress, metrics ->
                    viewModel.updateItemMetricSettings(itemId, mins, pol, show)
                    viewModel.updateItemProgress(itemId, progress)
                    viewModel.replaceManualMetrics(itemId, metrics)
                    detailsItemId = null
                }
            )
        }
    }

    if (showMoveDialog) {
        val selectedId = state.selectedItemId
        val node = selectedId?.let { state.tree.nodeById[it] }
        if (selectedId != null && node != null) {
            val destinations = moveDestinations(state.tree.flatItems, selectedId)
            MoveToDialog(
                itemText = node.item.text.ifBlank { "Untitled item" },
                destinations = destinations,
                childCounts = state.tree.nodeById.mapValues { it.value.children.size },
                onDismiss = { showMoveDialog = false },
                onApply = { destinationId ->
                    viewModel.moveUnderSelected(destinationId)
                    showMoveDialog = false
                }
            )
        }
    }

    if (showSortDialog) {
        state.selectedItemId?.let { state.tree.nodeById[it] }?.let { node ->
            SortChildrenDialog(
                itemText = node.item.text.ifBlank { "Untitled item" },
                onDismiss = { showSortDialog = false },
                onApply = { order ->
                    viewModel.sortChildrenOfSelected(order)
                    showSortDialog = false
                }
            )
        }
    }
}
