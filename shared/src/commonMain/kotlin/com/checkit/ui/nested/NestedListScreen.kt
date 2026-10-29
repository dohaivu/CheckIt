package com.checkit.ui.nested

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BorderStyle
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.nested_add_child
import checkit.shared.generated.resources.nested_add_sibling
import checkit.shared.generated.resources.nested_batch_delete
import checkit.shared.generated.resources.nested_confirm_delete
import checkit.shared.generated.resources.nested_collapse_all
import checkit.shared.generated.resources.nested_delete_confirm
import checkit.shared.generated.resources.nested_delete_confirm_multiple
import checkit.shared.generated.resources.nested_edit_note
import checkit.shared.generated.resources.nested_expand_all
import checkit.shared.generated.resources.nested_focus_selection
import checkit.shared.generated.resources.nested_indent
import checkit.shared.generated.resources.nested_move_down
import checkit.shared.generated.resources.nested_move_up
import checkit.shared.generated.resources.nested_outdent
import checkit.shared.generated.resources.nested_root
import checkit.shared.generated.resources.nested_zoom_in
import checkit.shared.generated.resources.nested_zoom_out
import com.checkit.domain.FocusPeriod
import com.checkit.domain.MetricItem
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.MoveDestinations
import com.checkit.domain.NestedBorderShape
import com.checkit.domain.NestedBorderStyle
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedMetricSummary
import com.checkit.domain.NestedSortOrder
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.TagItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.filterNestedTreeWithContext
import com.checkit.domain.moveDestinations
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.tasks.PredefinedLabels
import com.checkit.ui.components.CompactFlatTextField
import com.checkit.ui.components.DatePicker
import com.checkit.ui.components.DateRangePill
import com.checkit.ui.components.EmojiPicker
import com.checkit.ui.components.FocusPeriodHeader
import com.checkit.ui.components.MarkdownTextField
import com.checkit.ui.components.MetricsSection
import com.checkit.ui.components.MetricChip
import com.checkit.ui.components.TagOptionMenu
import com.checkit.ui.components.TagPill
import com.checkit.ui.components.asMarkdownAnnotatedString
import com.checkit.ui.components.renderInlineMarkdown
import com.checkit.ui.isValidForSave
import com.checkit.ui.noRippleClickable
import com.checkit.ui.orderedRangeForEnd
import com.checkit.ui.orderedRangeForStart
import com.checkit.ui.tasks.views.ViewOptionChip
import com.checkit.ui.theme.parseHexColorOrNull
import com.checkit.ui.toDateTimeLabel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

// Palette shared with macOS NestedRowView (nestedDepthSolids). Guides and
// dots are quiet by default — the depth hue is a hint, not a statement. The
// active row promotes its own dot/ring and the spine running down to its
// children, so selection points at the branch instead of repainting the
// whole outline.
private val nestedDepthSolids: List<Color> = listOf(
    Color(0xFFD97F5F), // 0 terracotta
    Color(0xFF7EA38A), // 1 sage
    Color(0xFF7B9EBD), // 2 dusty blue
    Color(0xFFC09F5E), // 3 warm sand
)

private const val nestedGuideSubtleAlpha = 0.18f
private const val nestedGuideStrongAlpha = 0.92f
private const val nestedDotSubtleAlpha = 0.5f

private fun nestedDepthColor(depth: Int): Color =
    nestedDepthSolids[depth % nestedDepthSolids.size]

private fun nestedGuideColor(level: Int, emphasized: Boolean = false, focusAlpha: Float = 1f): Color =
    nestedDepthColor(level).copy(
        alpha = (if (emphasized) nestedGuideStrongAlpha else nestedGuideSubtleAlpha) * focusAlpha
    )

private fun nestedDotColor(depth: Int, emphasized: Boolean = false, focusAlpha: Float = 1f): Color =
    nestedDepthColor(depth).copy(
        alpha = (if (emphasized) 1f else nestedDotSubtleAlpha) * focusAlpha
    )

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            val addingItem = state.overlay as? NestedEditorOverlay.AddingItem
            val listState = rememberLazyListState()
            val haptics = LocalHapticFeedback.current
            val density = androidx.compose.ui.platform.LocalDensity.current
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
            // Focus selection is a render-time overlay on the filtered rows:
            // it never touches the filter, Working Mode, zoom, or collapse
            // state. A hidden branch root simply has no range, so focus
            // suspends until it is visible again.
            val focusActive = state.focusSelectionEnabled && !state.selection.isActive
            // Resolve the selection branch once per selection/projection
            // change: its effective root, contiguous pre-order range, and
            // guide depth. Focus dimming and the selection spine both use it,
            // so a leaf promotes to its visible parent (MindNode behavior)
            // without walking ancestors for every row.
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
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
                                        .graphicsLayer {
                                            translationY =
                                                if (isDragged) (drag?.totalDragY ?: 0f) else 0f
                                            scaleX = if (isDragged) 1.02f else 1f
                                            scaleY = if (isDragged) 1.02f else 1f
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
                                        isVisible = row.isVisible,
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

/**
 * Full-height note editor for one outline row. Long notes get the whole
 * sheet; Preview renders the draft as Markdown (read-only) so it can be
 * scanned without the editing chrome. The 5,000-character budget matches
 * the macOS note sheet.
 */
@Composable
private fun NestedNoteEditorSheet(
    itemId: String,
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember(itemId) { mutableStateOf(initialText) }
    var preview by remember(itemId) { mutableStateOf(initialText.isNotEmpty()) }
    val scrollState = rememberScrollState()
    LaunchedEffect(preview) { scrollState.scrollTo(0) }

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        sheetGesturesEnabled = false,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.nested_edit_note),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { preview = !preview }) {
                Text(if (preview) "Edit" else "Preview")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (preview) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (note.isBlank()) {
                        Text(
                            text = "Nothing to preview yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = note.asMarkdownAnnotatedString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    MarkdownTextField(
                        value = note,
                        onValueChange = { note = it.take(NESTED_NOTE_MAX_LENGTH) },
                        placeholder = "Add label or details",
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        minLines = 12,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${note.length}/5,000",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(note) }) {
                    Text("Save")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MoveToDialog(
    itemText: String,
    destinations: MoveDestinations,
    childCounts: Map<String, Int>,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var selectedId by remember(destinations) {
        mutableStateOf(
            destinations.siblings.firstOrNull()?.id
                ?: destinations.parentSiblings.firstOrNull()?.id
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = itemText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                MoveDestinationGroup(
                    title = "Siblings",
                    items = destinations.siblings,
                    childCounts = childCounts,
                    selectedId = selectedId,
                    onSelect = { selectedId = it }
                )
                MoveDestinationGroup(
                    title = "Parent's siblings",
                    items = destinations.parentSiblings,
                    childCounts = childCounts,
                    selectedId = selectedId,
                    onSelect = { selectedId = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedId?.let(onApply) },
                enabled = selectedId != null
            ) {
                Text("Move")
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
private fun MoveDestinationGroup(
    title: String,
    items: List<NestedListItem>,
    childCounts: Map<String, Int>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    if (items.isEmpty()) return
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp)
    )
    items.forEach { item ->
        val childCount = childCounts[item.id] ?: 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSelect(item.id) }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedId == item.id,
                onClick = { onSelect(item.id) }
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.text.ifBlank { "Untitled item" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (childCount > 0) "$childCount children" else "No children",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SortChildrenDialog(
    itemText: String,
    onDismiss: () -> Unit,
    onApply: (NestedSortOrder) -> Unit
) {
    var selected by remember { mutableStateOf(NestedSortOrder.NameAsc) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort children") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = itemText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                NestedSortOrder.entries.forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = order }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == order,
                            onClick = { selected = order }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = order.label(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selected) }) {
                Text("Sort")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

private fun NestedSortOrder.label(): String = when (this) {
    NestedSortOrder.NameAsc -> "Name (A–Z)"
    NestedSortOrder.AddedDesc -> "Newest first"
    NestedSortOrder.CompletedDesc -> "Recently completed"
    NestedSortOrder.UncheckedFirst -> "Unchecked first"
    NestedSortOrder.PriorityDesc -> "Priority (high first)"
}

@Composable
private fun NestedFormattingBar(
    item: NestedListItem,
    hasChildren: Boolean,
    onFormattingChange: (NestedTextStyle, NestedColorToken, NestedColorToken) -> Unit,
    onBorderChange: (NestedBorderShape, NestedBorderStyle, NestedColorToken) -> Unit,
    onLabelChange: (String?) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onGuideDotChange: (String?) -> Unit,
    onDateRangeChange: (kotlinx.datetime.LocalDate?, kotlinx.datetime.LocalDate?) -> Unit,
    availableTags: List<TagItem>,
    onTagsChange: (List<String>) -> Unit,
    onToggleNote: () -> Unit,
    onToggleCheckbox: () -> Unit,
    onEnableCheckboxForChildren: () -> Unit,
    onSetChecked: (Boolean) -> Unit,
    onManageDetails: () -> Unit
) {
    var priorityExpanded by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }
    var guideDotExpanded by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    val subtleInactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    Surface(
        modifier = Modifier
            .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
            .navigationBarsPadding(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
        ) {
            // Group 1: Typography & Style
            val hasCustomStyle = item.textStyle != NestedTextStyle.Body || item.checkboxEnabled
            Box {
                IconButton(
                    onClick = { styleExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = "Text style",
                        tint = if (hasCustomStyle) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = styleExpanded, onDismissRequest = { styleExpanded = false }) {
                    NestedTextStyle.entries.forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style.name) },
                            modifier = if (style == item.textStyle) {
                                Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                            } else {
                                Modifier
                            },
                            onClick = {
                                styleExpanded = false
                                onFormattingChange(style, item.textColor, item.backgroundColor)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Checkbox") },
                        modifier = if (item.checkboxEnabled) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            Modifier
                        },
                        onClick = {
                            styleExpanded = false
                            onToggleCheckbox()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add checkbox for children") },
                        enabled = hasChildren,
                        onClick = {
                            styleExpanded = false
                            onEnableCheckboxForChildren()
                        }
                    )
                }
            }

            ColorTokenMenu(
                icon = Icons.Default.FormatColorText,
                selected = item.textColor
            ) { token -> onFormattingChange(item.textStyle, token, item.backgroundColor) }

            ColorTokenMenu(
                icon = Icons.Default.FormatColorFill,
                selected = item.backgroundColor,
                filled = true
            ) { token -> onFormattingChange(item.textStyle, item.textColor, token) }

            BorderMenu(
                shape = item.borderShape,
                style = item.borderStyle,
                color = item.borderColor
            ) { shape, style, color -> onBorderChange(shape, style, color) }

            // Guide dot: an emoji replaces the row's dot (parents keep the ring).
            Box {
                IconButton(
                    onClick = { guideDotExpanded = true },
                    // The trigger shows the emoji itself, which carries no text
                    // label of its own, so name the control explicitly.
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { contentDescription = "Guide dot" }
                ) {
                    val emoji = item.guideDot
                    if (emoji != null) {
                        Text(text = emoji, fontSize = 15.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEmotions,
                            contentDescription = "Guide dot",
                            tint = subtleInactiveColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = guideDotExpanded,
                    onDismissRequest = { guideDotExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Default dot") },
                        trailingIcon = if (item.guideDot == null) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                        onClick = {
                            guideDotExpanded = false
                            onGuideDotChange(null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.guideDot == null) "Choose emoji…" else "Change emoji…") },
                        onClick = {
                            guideDotExpanded = false
                            showEmojiPicker = true
                        }
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(16.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Group 2: Metadata (Priority, Date Range, Tags)
            val hasPriority = item.priority != TaskPriority.None
            Box {
                IconButton(
                    onClick = { priorityExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Priority",
                        tint = if (hasPriority) priorityColor(item.priority) else subtleInactiveColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                    TaskPriority.entries.forEach { priority ->
                        DropdownMenuItem(
                            text = { Text(priority.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = priorityColor(priority),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                priorityExpanded = false
                                onPriorityChange(priority)
                            }
                        )
                    }
                }
            }

            val hasLabel = !item.label.isNullOrBlank()
            IconButton(
                onClick = { showLabelDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Label",
                    tint = if (hasLabel) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            val hasDateRange = item.startDate != null || item.endDate != null
            IconButton(
                onClick = { showDateDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Start and end dates",
                    tint = if (hasDateRange) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            TagOptionMenu(
                availableTags = availableTags,
                selectedTagIds = item.tags.map { it.id }.toSet(),
                onTagToggle = { tagId ->
                    val selected = item.tags.map { it.id }.toMutableSet()
                    if (!selected.add(tagId)) selected.remove(tagId)
                    onTagsChange(selected.toList())
                },
                icon = { hasSelected, onClick ->
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(modifier = Modifier.size(18.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = "Tags",
                                tint = if (hasSelected) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            if (hasSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            )

            VerticalDivider(
                modifier = Modifier
                    .height(16.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Group 3: Quick Actions (Note, Check off)
            val hasNote = !item.note.isNullOrEmpty()
            IconButton(
                onClick = onToggleNote,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Edit note",
                    tint = if (hasNote) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onSetChecked(!item.checked) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = if (item.checked) "Uncheck" else "Check off",
                    tint = if (item.checked) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onManageDetails,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Details",
                    tint = subtleInactiveColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showEmojiPicker) {
        EmojiPicker(
            onDismiss = { showEmojiPicker = false },
            onEmojiSelect = { emoji -> onGuideDotChange(emoji.details.string) }
        )
    }

    if (showDateDialog) {
        var startDate by remember { mutableStateOf(item.startDate) }
        var endDate by remember { mutableStateOf(item.endDate) }
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Text(
                    text = "Dates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DatePicker(
                        date = startDate,
                        onDateChange = { picked ->
                            // Inverted ranges are not allowed: a start after
                            // the end drops that end.
                            val (newStart, newEnd) = orderedRangeForStart(picked, endDate)
                            startDate = newStart
                            endDate = newEnd
                        },
                        modifier = Modifier.fillMaxWidth(),
                        emptyLabel = "Set start date"
                    )
                    DatePicker(
                        date = endDate,
                        onDateChange = { picked ->
                            // Mirror rule: an end before the start drops the
                            // start, so the latest pick always survives.
                            val (newStart, newEnd) = orderedRangeForEnd(startDate, picked)
                            startDate = newStart
                            endDate = newEnd
                        },
                        modifier = Modifier.fillMaxWidth(),
                        emptyLabel = "Set end date"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateRangeChange(startDate, endDate)
                        showDateDialog = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDateRangeChange(null, null)
                        showDateDialog = false
                    }
                ) {
                    Text("Clear both")
                }
            }
        )
    }

    if (showLabelDialog) {
        NestedLabelDialog(
            initialLabel = item.label,
            onDismiss = { showLabelDialog = false },
            onSave = { label ->
                onLabelChange(label)
                showLabelDialog = false
            }
        )
    }
}

@Composable
private fun NestedLabelDialog(
    initialLabel: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var value by remember(initialLabel) { mutableStateOf(initialLabel.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Label",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppOutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = "Add a label",
                    maxLines = 1
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PredefinedLabels.forEach { predefined ->
                        val selected = value.trim() == predefined
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { value = if (selected) "" else predefined }
                        ) {
                            Text(
                                text = predefined,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim().takeIf { it.isNotBlank() }) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    value = ""
                    onSave(null)
                }
            ) {
                Text("Clear")
            }
        }
    )
}

@Composable
private fun ColorTokenMenu(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: NestedColorToken,
    filled: Boolean = false,
    onSelected: (NestedColorToken) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isColorActive = selected != NestedColorToken.Default
    val subtleInactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (filled) "Background color" else "Text color",
                tint = if (isColorActive) nestedColor(selected) else subtleInactiveColor,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ColorTokenSwatches(selected = selected) {
                expanded = false
                onSelected(it)
            }
        }
    }
}

@Composable
private fun ColorTokenSwatches(
    selected: NestedColorToken,
    modifier: Modifier = Modifier,
    onSelected: (NestedColorToken) -> Unit
) {
    // Wrap at 7 swatches so the growing palette stays inside a phone-width
    // menu instead of running off screen.
    FlowRow(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        maxItemsInEachRow = 7,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NestedColorToken.entries.forEach { token ->
            val color = nestedColor(token)
            val isSelected = token == selected
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (token == NestedColorToken.Default) MaterialTheme.colorScheme.surfaceVariant else color
                    )
                    .clickable { onSelected(token) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = token.name,
                        tint = if (token == NestedColorToken.Default) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else if (token == NestedColorToken.Default) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Default",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BorderMenu(
    shape: NestedBorderShape,
    style: NestedBorderStyle,
    color: NestedColorToken,
    onSelected: (NestedBorderShape, NestedBorderStyle, NestedColorToken) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val active = style != NestedBorderStyle.None
    val subtleInactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BorderStyle,
                contentDescription = "Border",
                tint = if (active) {
                    if (color != NestedColorToken.Default) nestedColor(color)
                    else MaterialTheme.colorScheme.primary
                } else {
                    subtleInactiveColor
                },
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Border style",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NestedBorderStyle.entries.forEach { option ->
                        SelectableChip(
                            label = option.name,
                            selected = option == style
                        ) {
                            expanded = false
                            onSelected(shape, option, color)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Shape",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NestedBorderShape.entries.forEach { option ->
                        SelectableChip(
                            label = option.displayName(),
                            selected = option == shape
                        ) {
                            expanded = false
                            onSelected(option, style, color)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Border color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorTokenSwatches(selected = color) {
                    expanded = false
                    onSelected(shape, style, it)
                }
            }
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun priorityColor(priority: TaskPriority): Color =
    priority.priorityHex()?.parseHexColorOrNull()
        ?: MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun NestedItemMetadataPreview(
    item: NestedListItem,
    summary: NestedMetricSummary,
    isLeaf: Boolean
) {
    val hasNote = !item.note.isNullOrBlank()
    val hasTags = item.tags.isNotEmpty()
    val hasDateRange = item.startDate != null || item.endDate != null
    val progress = item.progressPercent
    val showTracked = isLeaf || item.showTrackedMinutes
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (progress != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { (progress.coerceIn(0, 100)) / 100f },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${progress.coerceIn(0, 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (summary.totalTaskCount > 0) {
                SummaryChip(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append("${summary.doneTaskCount}/${summary.totalTaskCount}")
                    }
                    append(" done")
                })
            }
            if (showTracked && summary.trackedMinutes > 0) {
                SummaryChip(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append("${summary.trackedMinutes}")
                    }
                    append(" min")
                })
            }
            item.manualMetrics.filter { it.enabled }.forEach { metric ->
                if (metric.isCompleted || metric.isValidForSave()) {
                    MetricChip(metric = metric)
                }
            }
        }
        if (!hasNote && !hasTags && !hasDateRange && progress == null) return@Column

        if (hasNote) {
            Text(
                text = remember(item.note) {
                    renderInlineMarkdown(item.note.orEmpty())
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hasTags || hasDateRange) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasDateRange) {
                    DateRangePill(
                        startDate = item.startDate,
                        endDate = item.endDate
                    )
                }
                item.tags.forEach { tag -> TagPill(tag, isCompact = true) }
            }
        }
    }
}

@Composable
private fun SummaryChip(content: AnnotatedString) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun NestedItemDetailsDialog(
    node: NestedItemNode,
    summary: NestedMetricSummary,
    onDismiss: () -> Unit,
    onSave: (Int, MetricRollupPolicy, Boolean, Int?, List<MetricItem>) -> Unit
) {
    val item = node.item
    val isLeaf = !node.hasChildren
    var actualMinutes by remember(item.id) { mutableStateOf(if (item.actualMinutes > 0) item.actualMinutes.toString() else "") }
    var policy by remember(item.id) { mutableStateOf(item.metricRollupPolicy) }
    var showTrackedMinutes by remember(item.id) { mutableStateOf(item.showTrackedMinutes) }
    var progress by remember(item.id) { mutableStateOf(item.progressPercent) }
    var metrics by remember(item.id) { mutableStateOf(item.manualMetrics) }
    var policyExpanded by remember { mutableStateOf(false) }

    val directChildCount = node.children.size
    val totalChildCount = remember(node) { countTotalDescendants(node) }
    val doneTaskCount = summary.doneTaskCount
    val totalTaskCount = summary.totalTaskCount
    val totalTrackedMinutes = summary.trackedMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Item details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = item.text.ifBlank { "Untitled item" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.completedAtMillis?.let { completedAt ->
                    Text(
                        text = "Completed · ${completedAt.toDateTimeLabel()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Overview stats cards (Compact flat design)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailMetricStatCard(
                        title = "Children",
                        value = if (directChildCount == totalChildCount) "$totalChildCount" else "$directChildCount ($totalChildCount)",
                        subtitle = if (totalChildCount > 0) {
                            if (totalTaskCount > 0) "$doneTaskCount/$totalTaskCount done" else "No checkboxes"
                        } else {
                            "Leaf item"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DetailMetricStatCard(
                        title = "Actual time",
                        value = "${item.actualMinutes}m",
                        subtitle = if (!isLeaf && totalTrackedMinutes != item.actualMinutes) "Total: ${totalTrackedMinutes}m" else "Direct time",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Section 2: Time tracking and rollup settings
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TIME & ROLLUP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactFlatTextField(
                            value = actualMinutes,
                            onValueChange = { value -> if (value.all { it.isDigit() }) actualMinutes = value },
                            placeholder = "0",
                            suffix = "min",
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        Box(modifier = Modifier.weight(1.3f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .clickable { policyExpanded = true }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = policyLabel(policy),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = policyExpanded,
                                onDismissRequest = { policyExpanded = false }
                            ) {
                                MetricRollupPolicy.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(policyLabel(option), style = MaterialTheme.typography.bodyMedium) },
                                        onClick = { policy = option; policyExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    if (!isLeaf) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show tracked minutes on row",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "$totalTrackedMinutes min total rollup",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = showTrackedMinutes,
                                onCheckedChange = { showTrackedMinutes = it },
                                modifier = Modifier.scale(0.75f)
                            )
                        }
                    }
                }

                // Section: Progress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val showing = progress != null
                        Icon(
                            imageVector = if (showing) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = if (showing) "Hide progress" else "Show progress",
                            tint = if (showing) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                            },
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    progress = if (showing) null else progress ?: 0
                                }
                        )
                        if (showing) {
                            Slider(
                                value = (progress ?: 0).toFloat(),
                                onValueChange = { value ->
                                    progress = value.roundToInt().coerceIn(0, 100)
                                },
                                valueRange = 0f..100f,
                                steps = 99,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    activeTickColor = Color.Transparent,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    inactiveTickColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f).height(22.dp)
                            )
                            Text(
                                text = "${progress}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(44.dp)
                            )
                        } else {
                            Text(
                                text = "Show progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { progress = 0 }
                            )
                        }
                    }
                }

                // Section 3: Custom/Manual Metrics
                MetricsSection(
                    metrics = metrics,
                    enabled = true,
                    onMetricsChange = { metrics = it },
                    emptyHint = "No custom metrics added."
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        actualMinutes.toIntOrNull() ?: 0,
                        policy,
                        showTrackedMinutes,
                        progress?.coerceIn(0, 100),
                        metrics.filter { it.isValidForSave() }
                    )
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
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
private fun DetailMetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun countTotalDescendants(node: NestedItemNode): Int {
    var count = node.children.size
    for (child in node.children) {
        count += countTotalDescendants(child)
    }
    return count
}

private fun policyLabel(policy: MetricRollupPolicy): String = when (policy) {
    MetricRollupPolicy.IncludeChildren -> "Include children"
    MetricRollupPolicy.OwnOnly -> "Own item only"
    MetricRollupPolicy.ExcludeFromParent -> "Exclude from parent"
}

// ---------------- breadcrumbs ----------------

private data class Breadcrumb(
    val label: String,
    val depth: Int,
    val isRoot: Boolean,
    val itemId: String? = null
)

private fun buildBreadcrumbs(state: NestedEditorState.Active, roots: List<NestedItemNode>): List<Breadcrumb> {
    val result = mutableListOf<Breadcrumb>()
    result.add(Breadcrumb(label = "", depth = 0, isRoot = true))
    val focusedId = state.zoomPath.lastOrNull() ?: return result
    val chain = findAncestorChain(roots, focusedId) ?: return result
    chain.forEach { node ->
        result.add(
            Breadcrumb(
                label = node.item.text,
                depth = result.size,
                isRoot = false,
                itemId = node.item.id
            )
        )
    }
    return result
}

/** Returns the node chain from a root down to [id], or null if not found. */
private fun findAncestorChain(nodes: List<NestedItemNode>, id: String): List<NestedItemNode>? {
    for (node in nodes) {
        if (node.item.id == id) return listOf(node)
        findAncestorChain(node.children, id)?.let { return listOf(node) + it }
    }
    return null
}

@Composable
private fun NestedListFilterBar(
    focus: FocusPeriod?,
    query: String,
    hideChecked: Boolean,
    isActive: Boolean,
    availableTags: List<TagItem>,
    selectedTagIds: Set<String>,
    onFocusChange: (FocusPeriod) -> Unit,
    onQueryChange: (String) -> Unit,
    onHideCheckedChange: (Boolean) -> Unit,
    onTagToggle: (String) -> Unit,
    onReset: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "FILTERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (isActive) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onReset() }
                )
            }
        }

        FocusPeriodHeader(
            focus = focus,
            onFocusSelected = onFocusChange,
            onPreviousPeriod = onPreviousPeriod,
            onNextPeriod = onNextPeriod,
            onCurrentPeriod = onCurrentPeriod
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            AppOutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search items...",
                clearEnabled = true,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            )

            TagOptionMenu(
                availableTags = availableTags,
                selectedTagIds = selectedTagIds,
                onTagToggle = onTagToggle
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onHideCheckedChange(!hideChecked) }
                    .background(
                        if (hideChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (hideChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = if (hideChecked) Icons.Default.Done else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (hideChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Hide checked",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hideChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<Breadcrumb>,
    onCrumbClick: (String) -> Unit,
    onRootClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.nested_root),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                .clickable { onRootClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
        breadcrumbs.drop(1).forEach { crumb ->
            val isCurrent = crumb.depth == breadcrumbs.lastOrNull()?.depth
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.surfaceVariant
                        else Color.Transparent
                    )
                    .clickable(enabled = !isCurrent) { crumb.itemId?.let(onCrumbClick) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = crumb.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------- display type ----------------

@Composable
private fun NestedDisplayTypeMenu(
    selected: NestedDisplayType,
    onSelect: (NestedDisplayType) -> Unit
) {
    var isPopupOpen by remember { mutableStateOf(false) }
    val visibleState = remember { MutableTransitionState(false) }

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(
            onClick = {
                isPopupOpen = true
                visibleState.targetState = true
            },
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = selected.icon(),
                contentDescription = "view options",
                tint = if (selected == NestedDisplayType.All) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(18.dp)
            )
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
                            .heightIn(min = 36.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                NestedDisplayType.entries.forEach { displayType ->
                                    ViewOptionChip(
                                        icon = displayType.icon(),
                                        label = displayType.label(),
                                        selected = selected == displayType,
                                        onClick = {
                                            onSelect(displayType)
                                        }
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

private fun NestedDisplayType.icon(): ImageVector =
    when (this) {
        NestedDisplayType.All -> Icons.AutoMirrored.Filled.ViewList
        NestedDisplayType.Working -> Icons.Default.Work
    }

private fun NestedDisplayType.label(): String =
    when (this) {
        NestedDisplayType.All -> "All"
        NestedDisplayType.Working -> "Working"
    }

// ---------------- toolbar ----------------

@Composable
private fun EditorToolbar(
    state: NestedEditorState.Active,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddChild: () -> Unit,
    onAddSibling: () -> Unit,
    onDelete: () -> Unit,
    onAddRoot: () -> Unit,
    onManageDetails: () -> Unit,
    onAddToDailyPlan: () -> Unit,
    onCopyToTask: () -> Unit,
    onEnterSelection: () -> Unit,
    onSortChildren: () -> Unit,
    onMoveTo: () -> Unit,
    focusSelectionEnabled: Boolean,
    onToggleFocusSelection: () -> Unit
) {
    val hasSelection = state.selectedItemId != null
    val selectedNode = state.selectedItemId?.let { id -> state.tree.nodeById[id] }
    val canZoomIn = hasSelection && (selectedNode?.hasChildren == true)
    val canSortChildren = selectedNode?.hasChildren == true
    val canMoveTo = state.selectedItemId?.let {
        !moveDestinations(state.tree.flatItems, it).isEmpty
    } == true
    val canZoomOut = state.zoomPath.isNotEmpty()
    val canAddSibling = hasSelection && (selectedNode?.item?.parentId != null)
    val hasCollapsible = state.tree.nodeById.values.any { it.hasChildren }
    var showMore by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start)
        ) {
            // Group 1: Zoom & Fold
            ToolbarButton(Icons.Default.ZoomIn, stringResource(Res.string.nested_zoom_in), canZoomIn, onZoomIn)
            ToolbarButton(Icons.Default.ZoomOut, stringResource(Res.string.nested_zoom_out), canZoomOut, onZoomOut)
            ToolbarToggleButton(
                icon = Icons.Default.CenterFocusStrong,
                label = stringResource(Res.string.nested_focus_selection),
                checked = focusSelectionEnabled,
                enabled = hasSelection || focusSelectionEnabled,
                onCheckedChange = { onToggleFocusSelection() }
            )
            ToolbarButton(Icons.Default.UnfoldMore, stringResource(Res.string.nested_expand_all), hasCollapsible, onExpandAll)
            ToolbarButton(Icons.Default.UnfoldLess, stringResource(Res.string.nested_collapse_all), hasCollapsible, onCollapseAll)

            VerticalDivider(
                modifier = Modifier
                    .height(14.dp)
                    .padding(horizontal = 1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Group 2: Indent & Move
            ToolbarButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(Res.string.nested_outdent), hasSelection, onOutdent)
            ToolbarButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(Res.string.nested_indent), hasSelection, onIndent)
            ToolbarButton(Icons.Default.KeyboardArrowUp, stringResource(Res.string.nested_move_up), hasSelection, onMoveUp)
            ToolbarButton(Icons.Default.KeyboardArrowDown, stringResource(Res.string.nested_move_down), hasSelection, onMoveDown)

            VerticalDivider(
                modifier = Modifier
                    .height(14.dp)
                    .padding(horizontal = 1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Group 3: Add Items
            ToolbarButton(Icons.Default.Add, stringResource(Res.string.nested_add_child), hasSelection, onAddChild)
            ToolbarButton(Icons.AutoMirrored.Filled.NoteAdd, stringResource(Res.string.nested_add_sibling), canAddSibling, onAddSibling)

            VerticalDivider(
                modifier = Modifier
                    .height(14.dp)
                    .padding(horizontal = 1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Group 4: More
            Box {
                ToolbarButton(Icons.Default.MoreVert, "More actions", true) { showMore = true }
                DropdownMenu(
                    expanded = showMore,
                    onDismissRequest = { showMore = false }
                ) {
                    ToolbarMenuItem("Details", Icons.Outlined.Info, hasSelection) {
                        showMore = false; onManageDetails()
                    }
                    ToolbarMenuItem("Add root item", Icons.Default.AddBox, true) {
                        showMore = false; onAddRoot()
                    }
                    ToolbarMenuItem("Select items", Icons.Default.SelectAll, true) {
                        showMore = false; onEnterSelection()
                    }
                    ToolbarMenuItem("Add to daily plan", Icons.Default.Today, hasSelection) {
                        showMore = false; onAddToDailyPlan()
                    }
                    ToolbarMenuItem("Copy to Task", Icons.Default.ContentCopy, hasSelection) {
                        showMore = false; onCopyToTask()
                    }
                    ToolbarMenuItem(stringResource(Res.string.nested_batch_delete), Icons.Default.Delete, hasSelection) {
                        showMore = false; onDelete()
                    }
                    ToolbarMenuItem("Sort children", Icons.Default.SortByAlpha, canSortChildren) {
                        showMore = false; onSortChildren()
                    }
                    ToolbarMenuItem("Move to…", Icons.AutoMirrored.Filled.DriveFileMove, canMoveTo) {
                        showMore = false; onMoveTo()
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarMenuItem(
    label: String,
    icon: ImageVector? = null,
    enabled: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        },
        onClick = onClick,
        enabled = enabled
    )
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(30.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ToolbarToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = Modifier
            .size(30.dp)
            .background(
                if (checked) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                } else {
                    Color.Transparent
                },
                CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                checked -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SelectionToolbar(
    state: NestedEditorState.Active,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onExit: () -> Unit
) {
    val count = state.selection.selectedIds.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = "${count} selected",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        ToolbarButton(Icons.Default.Done, "Mark done", count > 0, onToggleDone)
        ToolbarButton(Icons.Default.Delete, stringResource(Res.string.nested_batch_delete), count > 0, onDelete)
        Text(text = "Exit", modifier = Modifier.clickable {onExit()})
    }
}

// ---------------- tree ----------------

@Composable
private fun EmptyNestedList(onAddItem: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Start with a root item, then indent items to build your outline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.Button(onClick = onAddItem) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add item")
        }
    }
}

/**
 * Guide-emoji text style. Emoji fonts ship a tall line box (Android also adds
 * legacy font padding), which pushed the glyph low enough to be clipped by the
 * dot's circle; trimming both keeps the glyph optically centred.
 */
private fun nestedGuideEmojiStyle(size: TextUnit) = TextStyle(
    fontSize = size,
    lineHeight = size,
    textAlign = TextAlign.Center,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    ),
    platformStyle = guideEmojiPlatformStyle()
)

@Composable
private fun NestedItemRow(
    node: NestedItemNode,
    depth: Int,
    isVisible: Boolean,
    isInCheckedBranch: Boolean,
    continuingLevels: Set<Int>,
    state: NestedEditorState.Active,
    viewModel: NestedListsViewModel,
    spineLevel: Int?,
    isSpineRoot: Boolean,
    isContext: Boolean = false,
    focusTier: NestedFocusTier = NestedFocusTier.None,
    isDragActive: Boolean = false,
    isDragged: Boolean = false
) {
    val item = node.item
    val isSelected = item.id in state.selection.selectedIds ||
            (!state.selection.isActive && state.selectedItemId == item.id)
    val isEditing = state.editingTextItemId == item.id
    // Active row: solid dot/ring. The selection root (the active row, or its
    // visible parent for a leaf) carries the spine down through its children.
    // Ancestor guides never emphasize, so emphasis points down the branch.
    val isEmphasized = isSelected || isEditing
    // Focus selection dims everything outside the selection branch. Path rows
    // keep a little more ink than unrelated branches so the way back to the
    // root stays legible. Around 140ms keeps the change calm without feeling
    // laggy; only visible rows animate.
    val focusAlpha by animateFloatAsState(
        targetValue = when (focusTier) {
            NestedFocusTier.None, NestedFocusTier.Focused -> 1f
            NestedFocusTier.Path -> 0.72f
            NestedFocusTier.Outside -> 0.45f
        },
        animationSpec = tween(durationMillis = 140),
        label = "nestedFocusAlpha"
    )
    val baseTextColor = when {
        isContext && !isEmphasized -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        isInCheckedBranch -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
        item.textColor != NestedColorToken.Default -> nestedColor(item.textColor)
        else -> MaterialTheme.colorScheme.onSurface
    }
    // min(), not multiplication: a filtered context row outside focus should
    // not stack 0.6 * 0.45 into unreadable 0.27 text.
    val itemTextColor = baseTextColor.copy(alpha = minOf(baseTextColor.alpha, focusAlpha))
    // Guide geometry. An emoji needs a full cell even on leaves, and a parent
    // ring grows to hold one at the same glyph size without squeezing it
    // against the stroke, so size and offset are resolved once here and shared
    // by the spine drawing and the dot composable below; every variant keeps
    // the same 16.dp centre line.
    val guideEmoji = item.guideDot
    val dotSize = when {
        node.hasChildren && guideEmoji != null -> 22.dp
        node.hasChildren || guideEmoji != null -> 14.dp
        else -> 7.dp
    }
    val dotTop = 16.dp - dotSize / 2
    val cardShape = remember(item.borderShape) { NestedCardShape(item.borderShape) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val borderStroke = remember(item.borderStyle, density.density) {
        item.borderStyle.toStroke(with(density) { 1.5.dp.toPx() }, density)
    }
    val baseBorderColor = when (item.borderColor) {
        NestedColorToken.Default -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        else -> nestedColor(item.borderColor)
    }
    val borderColor = baseBorderColor.copy(alpha = baseBorderColor.alpha * focusAlpha)

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        if (!isDragged) {
                            // Draw in the unpadded container so every depth shares the
                            // same x-coordinate across all rows.
                            // Dot geometry must match the composable below: parents always
                            // use the 14.dp outer ring, leaves use the 7.dp solid dot, with
                            // the leaf pushed down so both centers share one line. Guides
                            // stay center-aligned and touch the outer edge.
                            val dotSizePx = dotSize.toPx()
                            val dotCenterY = dotTop.toPx() + dotSizePx / 2
                            val dotBottomY = dotTop.toPx() + dotSizePx
                            val strokeWidth = 1.dp.toPx()
                            val curveRadius = 6.dp.toPx()
                            val guideX: (Int) -> Float = { level -> level * 16.dp.toPx() + 8.dp.toPx() }

                            continuingLevels.forEach { level ->
                                val x = guideX(level)
                                drawLine(
                                    color = nestedGuideColor(level, emphasized = level == spineLevel, focusAlpha = focusAlpha),
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }

                            if (depth > 0) {
                                val x = guideX(depth - 1)
                                val xDot = guideX(depth)
                                val lineEnd = xDot - (dotSizePx / 2)
                                val path = Path().apply {
                                    if (depth - 1 in continuingLevels) {
                                        moveTo(x, dotCenterY - curveRadius)
                                    } else {
                                        moveTo(x, 0f)
                                        lineTo(x, dotCenterY - curveRadius)
                                    }
                                    quadraticTo(x, dotCenterY, x + curveRadius, dotCenterY)
                                    lineTo(lineEnd, dotCenterY)
                                }
                                drawPath(
                                    path = path,
                                    color = nestedGuideColor(
                                        depth - 1,
                                        emphasized = depth - 1 == spineLevel,
                                        focusAlpha = focusAlpha
                                    ),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            if (node.hasChildren && !item.collapsed) {
                                val x = guideX(depth)
                                drawLine(
                                    color = nestedGuideColor(depth, emphasized = isEmphasized || isSpineRoot, focusAlpha = focusAlpha),
                                    start = Offset(x, dotBottomY),
                                    end = Offset(x, size.height),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (depth * 16).dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(36.dp)
                            .noRippleClickable(
                                enabled = node.hasChildren,
                                onClick = { viewModel.toggleCollapsed(item.id) },
                                onDoubleClick = {viewModel.zoomToItem(item.id)}
                            ),
                        contentAlignment = Alignment.TopStart
                    ) {
                        val dotColor = nestedDotColor(
                            depth,
                            emphasized = isEmphasized || isSpineRoot,
                            focusAlpha = focusAlpha
                        )
                        val hasChildren = node.hasChildren
                        // A ring holding an emoji is larger, so it takes a
                        // thinner stroke to keep the plain ring's visual weight.
                        val ringStroke = when {
                            guideEmoji != null && (isEmphasized || isSpineRoot) -> 1.25.dp
                            guideEmoji != null -> 1.dp
                            isEmphasized || isSpineRoot -> 2.dp
                            else -> 1.5.dp
                        }
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(36.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = dotTop)
                                    // requiredSize: a parent ring holding an
                                    // emoji is wider than the 16.dp slot and
                                    // must overflow it evenly, not squeeze into
                                    // an oval.
                                    .requiredSize(dotSize)
                                    .clip(CircleShape)
                                    .then(
                                        when {
                                            // Parents keep the ring: it is also the
                                            // collapse cue, and an emoji sits inside it.
                                            hasChildren -> Modifier
                                                .border(ringStroke, dotColor, CircleShape)
                                                .then(
                                                    // Collapsed + emoji reads as a filled
                                                    // ring, replacing the centre dot.
                                                    if (guideEmoji != null && item.collapsed) {
                                                        Modifier.background(dotColor.copy(alpha = 0.22f))
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                            // Leaf emoji replaces the solid dot.
                                            guideEmoji != null -> Modifier
                                            else -> Modifier.background(dotColor)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    guideEmoji != null -> Text(
                                        text = guideEmoji,
                                        style = nestedGuideEmojiStyle(12.sp),
                                        modifier = Modifier.alpha(focusAlpha)
                                    )
                                    item.collapsed && hasChildren -> Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .offset(x = (-8).dp)
                            .clip(cardShape)
                            .background(
                                when {
                                    isEditing -> MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.5f
                                    )

                                    isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(
                                        alpha = 0.5f
                                    )
                                    item.backgroundColor != NestedColorToken.Default -> nestedColor(
                                        item.backgroundColor
                                    ).copy(alpha = 0.18f * focusAlpha)

                                    else -> Color.Transparent
                                }
                            )
                            .nestedCardBorder(cardShape, borderStroke, borderColor)
                            .combinedClickable(
                                onClick = {
                                    if (state.selection.isActive) {
                                        viewModel.toggleSelect(item.id)
                                    } else {
                                        viewModel.selectItem(item.id)
                                        if (state.overlay is NestedEditorOverlay.AddingItem) viewModel.cancelAddItem()
                                    }
                                },
                                onDoubleClick = {
                                    if (!state.selection.isActive) viewModel.startEditText(item.id)
                                }
                            )
                            .padding(
                                horizontal = 2.dp + item.borderShape.horizontalContentPadding(),
                                vertical = 6.dp + item.borderShape.verticalContentPadding()
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.checkboxEnabled) {
                            Checkbox(
                                checked = item.checked,
                                onCheckedChange = { viewModel.toggleChecked(item.id) },
                                modifier = Modifier.size(28.dp).scale(0.7f).align(Alignment.Top),
                                colors = CheckboxDefaults.colors(uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                            )
                        } else {
                            Spacer(Modifier.width(4.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            if (isEditing) {
                                var text by remember(item.id) { mutableStateOf(item.text) }
                                val focusRequester = remember(item.id) { FocusRequester() }
                                LaunchedEffect(item.id) {
                                    text = item.text
                                    focusRequester.requestFocus()
                                }
                                BasicTextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    singleLine = false,
                                    textStyle = nestedTextStyle(item.textStyle).copy(
                                        color = nestedTextColor(item.textColor).copy(
                                            alpha = minOf(1f, focusAlpha)
                                        )
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    // Default Enter inserts a newline (Shift+Return
                                    // equivalent); commit happens on focus loss,
                                    // tap-away, or hardware Back.
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                                    modifier = Modifier.fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { focusState ->
                                            // Tap-away / toolbar / Back commits;
                                            // explicit saves already cleared
                                            // editing, so this skips then.
                                            if (!focusState.isFocused && isEditing && text != item.text) {
                                                viewModel.saveItemText(item.id, text)
                                            }
                                        }
                                )
                            } else {
                                Text(
                                    text = remember(item.text) {
                                        renderInlineMarkdown(item.text)
                                    },
                                    style = nestedTextStyle(item.textStyle),
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = itemTextColor,
                                    fontWeight = if (depth == 0 && !node.hasChildren) FontWeight.Bold else null
                                )
                            }
                            Box(
                                modifier = if (focusAlpha < 1f) {
                                    Modifier.alpha(focusAlpha)
                                } else {
                                    Modifier
                                }
                            ) {
                                NestedItemMetadataPreview(
                                    item = item,
                                    summary = state.tree.metricSummaryById[item.id]
                                        ?: NestedMetricSummary(),
                                    isLeaf = !node.hasChildren
                                )
                            }
                        }

                        if (item.priority != TaskPriority.None) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = priorityColor(item.priority),
                                modifier = Modifier.size(18.dp).alpha(focusAlpha)
                            )
                        }

                        if (!item.label.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .widthIn(max = 140.dp)
                                    .heightIn(max = 22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(
                                            alpha = 0.85f * focusAlpha
                                        )
                                    )
                            ) {
                                Text(
                                    text = item.label,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (state.selection.isActive) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleSelect(item.id) },
                                modifier = Modifier.size(28.dp).scale(0.7f).align(Alignment.Top)
                            )
                        }
                    }
                }

                if (
                    isSelected &&
                    !state.selection.isActive &&
                    !isEditing &&
                    !isDragActive &&
                    state.overlay !is NestedEditorOverlay.AddingItem
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.50f))
                            .clickable { viewModel.startAddSibling(item.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add sibling below",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun nestedTextStyle(style: NestedTextStyle) = when (style) {
    NestedTextStyle.Body -> MaterialTheme.typography.bodyMedium
    NestedTextStyle.Header -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
    NestedTextStyle.Subheader -> MaterialTheme.typography.titleMedium
}

@Composable
private fun nestedTextColor(token: NestedColorToken) =
    if (token == NestedColorToken.Default) MaterialTheme.colorScheme.onSurface else nestedColor(token)

@Composable
private fun nestedColor(token: NestedColorToken): Color {
    // Palette lives on the shared enum so macOS renders identical RGB values.
    val hex = token.hex() ?: return MaterialTheme.colorScheme.onSurfaceVariant
    val rgb = hex.removePrefix("#").toInt(16)
    return Color(
        red = ((rgb shr 16) and 0xFF) / 255f,
        green = ((rgb shr 8) and 0xFF) / 255f,
        blue = (rgb and 0xFF) / 255f
    )
}

internal data class VisibleNestedRow(
    val node: NestedItemNode,
    val depth: Int,
    val isVisible: Boolean,
    val isInCheckedBranch: Boolean,
    val continuingLevels: Set<Int> = emptySet(),
    /** Kept only as ancestor scaffolding for a filtered match. */
    val isContext: Boolean = false
)

/** How far a row sits from the focused subtree. */
internal enum class NestedFocusTier { None, Focused, Path, Outside }

internal fun focusTier(
    index: Int,
    itemId: String,
    range: IntRange?,
    ancestorIds: Set<String>
): NestedFocusTier = when {
    range == null -> NestedFocusTier.None
    index in range -> NestedFocusTier.Focused
    itemId in ancestorIds -> NestedFocusTier.Path
    else -> NestedFocusTier.Outside
}

/**
 * Effective branch root for a selection. A selected parent is its own root;
 * a leaf promotes to its visible parent so focus and the selection spine keep
 * the surrounding sibling branch visible. If the parent is filtered out or
 * hidden, the leaf itself remains the root.
 */
internal fun selectionRootIdOf(
    rows: List<VisibleNestedRow>,
    tree: NestedDocumentTree,
    selectedId: String?
): String? {
    val id = selectedId ?: return null
    val node = tree.nodeById[id] ?: return null
    if (node.hasChildren) return id
    val parentId = node.item.parentId?.takeIf { it.isNotEmpty() } ?: return id
    return if (rows.any { it.node.item.id == parentId && it.isVisible }) parentId else id
}

/**
 * Pre-order rows make a subtree a contiguous index range: everything after
 * the root row whose depth is still deeper is a descendant. Returns null when
 * the root is missing or hidden in the current projection (filtered out,
 * collapsed under an ancestor, or outside the zoom root), so focus suspends
 * without touching filters.
 */
internal fun focusRowRange(rows: List<VisibleNestedRow>, rootId: String?): IntRange? {
    if (rootId == null) return null
    val start = rows.indexOfFirst { it.node.item.id == rootId }
    if (start < 0 || !rows[start].isVisible) return null
    val depth = rows[start].depth
    var end = start
    while (end + 1 < rows.size && rows[end + 1].depth > depth) end++
    return start..end
}

/**
 * Effective selection branch: the root row, its contiguous pre-order range,
 * and the guide column that carries its spine. Resolving this once lets rows
 * check membership in O(1) instead of walking ancestors one by one.
 */
internal data class NestedSelectionBranch(
    val rootId: String,
    val range: IntRange,
    val depth: Int
)

/** Resolve the selection branch for the current rows and selection. */
internal fun selectionBranchOf(
    rows: List<VisibleNestedRow>,
    tree: NestedDocumentTree,
    selectedId: String?
): NestedSelectionBranch? {
    val rootId = selectionRootIdOf(rows, tree, selectedId) ?: return null
    val range = focusRowRange(rows, rootId) ?: return null
    return NestedSelectionBranch(
        rootId = rootId,
        range = range,
        depth = rows[range.first].depth
    )
}

/** Ancestors of the selection root, used for the medium-emphasis path tier. */
internal fun focusAncestorIdsOf(tree: NestedDocumentTree, rootId: String?): Set<String> {
    if (rootId == null) return emptySet()
    val ancestors = LinkedHashSet<String>()
    var cursor: String? = tree.itemById[rootId]?.parentId
    while (true) {
        val id = cursor?.takeIf { it.isNotEmpty() } ?: break
        if (!ancestors.add(id)) break
        cursor = tree.itemById[id]?.parentId
    }
    return ancestors
}

private data class NestedTraversalEntry(
    val node: NestedItemNode,
    val depth: Int,
    val isVisible: Boolean,
    val ancestorChecked: Boolean,
    val continuingLevels: Set<Int>
)

/** Iterative traversal keeps stable rows available for expand/collapse animation. */
// ---------------- drag & drop ----------------

/** Mutable state for an in-progress row drag; lives across gesture callbacks. */
private class DragDropState(
    val itemId: String,
    val initialDepth: Int
) {
    // Observable so graphicsLayer/composition react to gesture mutations.
    var pointerViewportY by mutableStateOf(0)
    var totalDragY by mutableStateOf(0f)
    var totalDragX by mutableStateOf(0f)
    var targetParentId by mutableStateOf<String?>(null)
    var targetIndex by mutableStateOf(0)
    var indicatorRowId by mutableStateOf<String?>(null)
    var indicatorDepth by mutableStateOf(0)
    var below by mutableStateOf(false)
    var hasResolved by mutableStateOf(false)
}

private data class ResolvedDropTarget(
    val parentId: String?,
    val index: Int,
    val rowId: String,
    val below: Boolean,
    val depth: Int
)

/**
 * Resolves the current drop placement from the dragged item's pointer position.
 * Uses the vertical gap between visible rows to determine allowed nesting depth,
 * and horizontal drag delta to choose the exact indent/outdent level.
 */
private fun resolveDropTarget(
    tree: com.checkit.domain.NestedDocumentTree,
    rows: List<VisibleNestedRow>,
    layoutInfo: LazyListLayoutInfo,
    drag: DragDropState,
    density: androidx.compose.ui.unit.Density
): ResolvedDropTarget? {
    if (rows.isEmpty()) return null
    val entries = layoutInfo.visibleItemsInfo.filter { it.key is String }
    if (entries.isEmpty()) return null

    val pointerY = drag.pointerViewportY
    val hovered = entries.firstOrNull { pointerY in it.offset until (it.offset + it.size) }
        ?: if (pointerY < entries.first().offset) entries.first()
        else if (pointerY >= entries.last().offset + entries.last().size) entries.last()
        else entries.minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - pointerY) }
        ?: return null

    val hoveredKey = hovered.key as? String ?: return null
    val hoveredRowIdx = rows.indexOfFirst { it.node.item.id == hoveredKey }
    if (hoveredRowIdx < 0) return null
    val hoveredRow = rows[hoveredRowIdx]

    val rowCenterY = hovered.offset + hovered.size / 2
    val isBelow = pointerY > rowCenterY

    val aboveRow: VisibleNestedRow?
    val belowRow: VisibleNestedRow?
    if (!isBelow) {
        aboveRow = if (hoveredRowIdx > 0) rows[hoveredRowIdx - 1] else null
        belowRow = hoveredRow
    } else {
        aboveRow = hoveredRow
        belowRow = if (hoveredRowIdx < rows.lastIndex) rows[hoveredRowIdx + 1] else null
    }

    val minDepth = belowRow?.depth ?: 0
    val maxDepth = if (aboveRow != null) aboveRow.depth + 1 else 0
    val effectiveMinDepth = minDepth.coerceAtMost(maxDepth)

    val baseDepth = if (isBelow) aboveRow?.depth ?: 0 else belowRow?.depth ?: 0

    val stepPx = with(density) { 32.dp.toPx() }
    val depthDelta = (drag.totalDragX / stepPx).toInt()
    val targetDepth = (baseDepth + depthDelta).coerceIn(effectiveMinDepth, maxDepth)

    fun siblingsOf(parentId: String?): List<NestedListItem> {
        val nodes = if (parentId == null) tree.rootNodes else tree.nodeById[parentId]?.children.orEmpty()
        return nodes.mapNotNull { if (it.item.id != drag.itemId) it.item else null }
    }

    val targetParentId: String?
    val targetIndex: Int

    if (aboveRow == null) {
        targetParentId = belowRow?.node?.item?.parentId
        targetIndex = 0
    } else if (targetDepth == aboveRow.depth + 1) {
        targetParentId = aboveRow.node.item.id
        targetIndex = siblingsOf(aboveRow.node.item.id).size
    } else if (targetDepth == aboveRow.depth) {
        targetParentId = aboveRow.node.item.parentId
        val siblings = siblingsOf(targetParentId)
        val aboveIdx = siblings.indexOfFirst { it.id == aboveRow.node.item.id }
        targetIndex = if (aboveIdx >= 0) (aboveIdx + 1).coerceAtMost(siblings.size) else siblings.size
    } else {
        val levelsUp = aboveRow.depth - targetDepth
        var ancestorItem: NestedListItem? = aboveRow.node.item
        repeat(levelsUp) {
            ancestorItem = ancestorItem?.parentId?.let { tree.itemById[it] }
        }
        if (ancestorItem != null) {
            targetParentId = ancestorItem.parentId
            val siblings = siblingsOf(targetParentId)
            val ancIdx = siblings.indexOfFirst { it.id == ancestorItem.id }
            targetIndex = if (ancIdx >= 0) (ancIdx + 1).coerceAtMost(siblings.size) else siblings.size
        } else {
            targetParentId = null
            targetIndex = siblingsOf(null).size
        }
    }

    val displayRowId: String
    val displayBelow: Boolean
    if (belowRow != null) {
        displayRowId = belowRow.node.item.id
        displayBelow = false
    } else if (aboveRow != null) {
        displayRowId = aboveRow.node.item.id
        displayBelow = true
    } else {
        displayRowId = hoveredRow.node.item.id
        displayBelow = isBelow
    }

    return ResolvedDropTarget(
        parentId = targetParentId,
        index = targetIndex,
        rowId = displayRowId,
        below = displayBelow,
        depth = targetDepth
    )
}

@Composable
private fun DropIndicatorLine(depth: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16 + 8).dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun flattenVisibleNodes(
    roots: List<NestedItemNode>,
    contextIds: Set<String> = emptySet()
): List<VisibleNestedRow> {
    if (roots.isEmpty()) return emptyList()
    val result = ArrayList<VisibleNestedRow>()
    val stack = ArrayDeque<NestedTraversalEntry>()
    
    roots.asReversed().forEach { node ->
        stack.addLast(
            NestedTraversalEntry(
                node = node,
                depth = 0,
                isVisible = true,
                ancestorChecked = false,
                continuingLevels = emptySet()
            )
        )
    }
    
    while (stack.isNotEmpty()) {
        val entry = stack.removeLast()
        val node = entry.node
        val isInCheckedBranch = entry.ancestorChecked || node.item.checked
        result += VisibleNestedRow(
            node = node,
            depth = entry.depth,
            isVisible = entry.isVisible,
            isInCheckedBranch = isInCheckedBranch,
            continuingLevels = entry.continuingLevels,
            isContext = node.item.id in contextIds
        )
        
        val childrenVisible = entry.isVisible && !node.item.collapsed
        val children = node.children
        children.asReversed().forEachIndexed { index, child ->
            val isLast = (index == 0) // asReversed: index 0 is the last child
            val nextContinuing = entry.continuingLevels.toMutableSet()
            if (!isLast) {
                nextContinuing.add(entry.depth)
            } else {
                nextContinuing.remove(entry.depth)
            }
            
            stack.addLast(
                NestedTraversalEntry(
                    node = child,
                    depth = entry.depth + 1,
                    isVisible = childrenVisible,
                    ancestorChecked = isInCheckedBranch,
                    continuingLevels = nextContinuing
                )
            )
        }
    }
    return result
}

@Composable
private fun NewItemRow(
    depth: Int,
    text: String,
    onTextChange: (String) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    continuingLevels: Set<Int>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val dotSizePx = 7.dp.toPx() // Draft items use small dot
                val dotTop = 12.dp.toPx()
                val dotCenterY = dotTop + dotSizePx / 2
                val strokeWidth = 1.dp.toPx()
                val curveRadius = 6.dp.toPx()
                val guideX: (Int) -> Float = { level -> level * 16.dp.toPx() + 8.dp.toPx() }

                continuingLevels.forEach { level ->
                    val x = guideX(level)
                    drawLine(
                        color = nestedGuideColor(level),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                if (depth > 0) {
                    val x = guideX(depth - 1)
                    val xDot = guideX(depth)
                    val lineEnd = xDot - (dotSizePx / 2)
                    val path = Path().apply {
                        if (depth - 1 in continuingLevels) {
                            moveTo(x, dotCenterY - curveRadius)
                        } else {
                            moveTo(x, 0f)
                            lineTo(x, dotCenterY - curveRadius)
                        }
                        quadraticTo(x, dotCenterY, x + curveRadius, dotCenterY)
                        lineTo(lineEnd, dotCenterY)
                    }
                    drawPath(
                        path = path,
                        color = nestedGuideColor(depth - 1),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp),
            verticalAlignment = Alignment.Top
        ) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            // Spacer to match NestedTree dot area
            Spacer(Modifier.width(24.dp).height(36.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .offset(x = (-8).dp)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    singleLine = false,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    // Default Enter inserts a newline; commit via the
                    // check button (tap-away cancels drafts).
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            IconButton(
                onClick = onCommit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = "Save")
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
        }
    }
}
