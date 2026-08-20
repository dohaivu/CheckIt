package com.checkit.ui.nested

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.nested_add_child
import checkit.shared.generated.resources.nested_add_sibling
import checkit.shared.generated.resources.nested_batch_delete
import checkit.shared.generated.resources.nested_confirm_delete
import checkit.shared.generated.resources.nested_delete_confirm
import checkit.shared.generated.resources.nested_edit_note
import checkit.shared.generated.resources.nested_indent
import checkit.shared.generated.resources.nested_move_down
import checkit.shared.generated.resources.nested_move_up
import checkit.shared.generated.resources.nested_outdent
import checkit.shared.generated.resources.nested_root
import checkit.shared.generated.resources.nested_zoom_in
import checkit.shared.generated.resources.nested_zoom_out
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedManualMetric
import com.checkit.domain.NestedMetricSummary
import com.checkit.domain.NestedMetricUnit
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.FocusPeriod
import com.checkit.domain.TaskPriority
import com.checkit.domain.filterNestedTree
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DateRangePill
import com.checkit.ui.components.FocusPeriodHeader
import com.checkit.ui.components.PeriodPicker
import com.checkit.ui.components.TagOptionMenu
import com.checkit.ui.components.TagPlain
import com.checkit.ui.tasks.noRippleClickable
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NestedListEditorScreen(
    state: NestedEditorState.Active,
    viewModel: NestedListsViewModel,
    modifier: Modifier = Modifier,
    onAddToDailyPlan: (title: String, tagIds: List<Long>, nestedListItemId: Long?) -> Unit = { _, _, _ -> }
) {
    var detailsItemId by remember { mutableStateOf<Long?>(null) }
    val tree = state.tree
    val focusedNode = state.focusedItem
    val unfilteredRoots = focusedNode?.let { listOf(it) } ?: tree.rootNodes
    val visibleRoots = if (state.filters.isVisible) {
        filterNestedTree(
            roots = unfilteredRoots,
            start = state.filters.focus?.start,
            end = state.filters.focus?.endInclusive,
            query = state.filters.query,
            hideChecked = state.filters.hideChecked
        )
    } else {
        unfilteredRoots
    }
    val visibleRows = flattenVisibleNodes(visibleRoots)
    val breadcrumbs = buildBreadcrumbs(state, tree.rootNodes)

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
            if (state.filters.isVisible) {
                NestedListFilterBar(
                    focus = state.filters.focus,
                    query = state.filters.query,
                    hideChecked = state.filters.hideChecked,
                    isActive = state.filters.isActive,
                    onFocusChange = viewModel::updateFilterFocus,
                    onQueryChange = viewModel::updateFilterQuery,
                    onHideCheckedChange = { viewModel.toggleHideChecked() },
                    onReset = viewModel::resetFilters,
                    onPreviousPeriod = viewModel::previousFilterPeriod,
                    onNextPeriod = viewModel::nextFilterPeriod,
                    onCurrentPeriod = viewModel::currentFilterPeriod
                )
            }
            BreadcrumbBar(
                breadcrumbs = breadcrumbs,
                onCrumbClick = { itemId -> viewModel.zoomToItem(itemId) },
                onRootClick = viewModel::zoomToRoot
            )

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
                    onIndent = { state.selectedItemId?.let(viewModel::indent) },
                    onOutdent = { state.selectedItemId?.let(viewModel::outdent) },
                    onMoveUp = { state.selectedItemId?.let(viewModel::moveUp) },
                    onMoveDown = { state.selectedItemId?.let(viewModel::moveDown) },
                    onAddChild = { state.selectedItemId?.let(viewModel::startAddChild) },
                    onAddSibling = { state.selectedItemId?.let(viewModel::startAddSibling) },
                    onDelete = viewModel::requestDeleteSelected,
                    onAddRoot = viewModel::startAddRoot,
                    onManageDetails = { state.selectedItemId?.let { detailsItemId = it } },
                    onAddToDailyPlan = {
                        state.selectedItemId?.let { id ->
                            state.tree.nodeById[id]?.item?.let { item ->
                                onAddToDailyPlan(item.text, item.tags.map { it.id }, item.id)
                            }
                        }
                    }
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            val addingItem = state.overlay as? NestedEditorOverlay.AddingItem
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (addingItem != null && addingItem.draft.anchorId == null) {
                    item(key = "new-root") {
                        NewItemRow(
                            depth = 0,
                            text = addingItem.draft.text,
                            onTextChange = viewModel::updateNewItemText,
                            onCommit = viewModel::commitNewItem,
                            onCancel = viewModel::cancelAddItem,
                            continuingLevels = emptySet()
                        )
                    }
                }
                if (visibleRows.isEmpty()) {
                    if (addingItem == null || addingItem.draft.anchorId != null) {
                        item(key = "empty-list") {
                            EmptyNestedList(onAddItem = viewModel::startAddRoot)
                        }
                    }
                } else {
                    items(visibleRows, key = { it.node.item.id }) { row ->
                        NestedTree(
                            node = row.node,
                            depth = row.depth,
                            isVisible = row.isVisible,
                            isInCheckedBranch = row.isInCheckedBranch,
                            continuingLevels = row.continuingLevels,
                            state = state,
                            viewModel = viewModel
                        )
                        if (addingItem != null && addingItem.draft.anchorId == row.node.item.id) {
                            NewItemRow(
                                depth = addingItem.draft.depth,
                                text = addingItem.draft.text,
                                onTextChange = viewModel::updateNewItemText,
                                onCommit = viewModel::commitNewItem,
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
                        NestedFormattingBottomBar(
                            item = selectedItem,
                            onFormattingChange = { style, textColor, backgroundColor ->
                                viewModel.updateItemFormatting(selectedItem.id, style, textColor, backgroundColor)
                            },
                            onPriorityChange = { priority ->
                                viewModel.updateItemPriority(selectedItem.id, priority)
                            },
                            onDateRangeChange = { startDate, endDate ->
                                viewModel.updateItemDateRange(selectedItem.id, startDate, endDate)
                            },
                            availableTags = state.availableTags,
                            onTagsChange = { viewModel.updateItemTags(selectedItem.id, it) },
                            onToggleNote = { viewModel.startEditNote(selectedItem.id) },
                            onToggleCheckbox = { viewModel.toggleCheckboxEnabled(selectedItem.id) },
                            onSetChecked = { checked -> viewModel.setChecked(selectedItem.id, checked) }
                        )
                    }
                }
            }
        }
    }

    when (val overlay = state.overlay) {
        is NestedEditorOverlay.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteConfirm,
                title = { Text(stringResource(Res.string.nested_confirm_delete)) },
                text = { Text(stringResource(Res.string.nested_delete_confirm)) },
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
            var note by remember(overlay.itemId) { mutableStateOf(overlay.initialText) }
            AlertDialog(
                onDismissRequest = viewModel::stopEditNote,
                title = { Text(stringResource(Res.string.nested_edit_note)) },
                text = {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(2_000) },
                        label = { Text("Note") },
                        placeholder = { Text("Add label or details") },
                        minLines = 4,
                        maxLines = 8,
                        supportingText = { Text("${note.length}/2,000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.saveItemNote(overlay.itemId, note) }) {
                        Text(stringResource(Res.string.nested_edit_note))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::stopEditNote) { Text(stringResource(Res.string.cancel)) }
                }
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
                onSave = { mins, pol, show, metrics ->
                    viewModel.updateItemMetricSettings(itemId, mins, pol, show)
                    viewModel.replaceManualMetrics(itemId, metrics)
                    detailsItemId = null
                }
            )
        }
    }
}

@Composable
private fun NestedFormattingBottomBar(
    item: com.checkit.domain.NestedListItem,
    onFormattingChange: (NestedTextStyle, NestedColorToken, NestedColorToken) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onDateRangeChange: (kotlinx.datetime.LocalDate?, kotlinx.datetime.LocalDate?) -> Unit,
    availableTags: List<com.checkit.domain.TagItem>,
    onTagsChange: (List<Long>) -> Unit,
    onToggleNote: () -> Unit,
    onToggleCheckbox: () -> Unit,
    onSetChecked: (Boolean) -> Unit
) {
    var priorityExpanded by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }
    var showPeriodPicker by remember { mutableStateOf(false) }
    val colorTokens = listOf(
        NestedColorToken.Default, NestedColorToken.Red, NestedColorToken.Orange,
        NestedColorToken.Yellow, NestedColorToken.Green, NestedColorToken.Blue,
        NestedColorToken.Purple, NestedColorToken.Pink
    )
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .background(
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 8.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        Box {
            IconButton(onClick = { styleExpanded = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.FormatSize,
                    contentDescription = "Text style",
                    tint = if (item.textStyle != NestedTextStyle.Body) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
            }
        }
        ColorTokenMenu(
            icon = Icons.Default.FormatColorText,
            selected = item.textColor,
            tokens = colorTokens
        ) { token -> onFormattingChange(item.textStyle, token, item.backgroundColor) }
        ColorTokenMenu(
            icon = Icons.Default.FormatColorFill,
            selected = item.backgroundColor,
            tokens = colorTokens,
            filled = true
        ) { token -> onFormattingChange(item.textStyle, item.textColor, token) }

        // Priority button (separated)
        Box {
            IconButton(onClick = { priorityExpanded = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Priority",
                    tint = priorityColor(item.priority)
                )
            }
            DropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                TaskPriority.entries.forEach { priority ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${priorityMarker(priority)}  ${priority.name}",
                                color = priorityColor(priority)
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

        // Date Range button (separated, opens PeriodPicker dialog)
        val hasDateRange = item.startDate != null || item.endDate != null
        IconButton(
            onClick = { showPeriodPicker = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Date range",
                tint = if (hasDateRange) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TagOptionMenu(
            availableTags = availableTags,
            selectedTagIds = item.tags.map { it.id }.toSet(),
            onTagToggle = { tagId ->
                val selected = item.tags.map { it.id }.toMutableSet()
                if (!selected.add(tagId)) selected.remove(tagId)
                onTagsChange(selected.toList())
            }
        )
        IconButton(onClick = onToggleNote, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.EditNote,
                contentDescription = "Edit note",
                tint = if (!item.note.isNullOrEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { onSetChecked(!item.checked) }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = if (item.checked) "Uncheck" else "Check off",
                tint = if (item.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPeriodPicker) {
        AlertDialog(
            onDismissRequest = { showPeriodPicker = false },
            title = {
                Text(
                    text = "Timeframe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                PeriodPicker(
                    startDate = item.startDate,
                    endDate = item.endDate,
                    onRangeChange = { start, end ->
                        onDateRangeChange(start, end)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showPeriodPicker = false }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDateRangeChange(null, null)
                        showPeriodPicker = false
                    }
                ) {
                    Text("Clear")
                }
            }
        )
    }
}

@Composable
private fun ColorTokenMenu(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: NestedColorToken,
    tokens: List<NestedColorToken>,
    filled: Boolean = false,
    onSelected: (NestedColorToken) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = if (filled) "Background color" else "Text color",
                tint = nestedColor(selected)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tokens.forEach { token ->
                DropdownMenuItem(
                    leadingIcon = { ColorTokenSwatch(token, filled = filled) },
                    text = { Text(token.name) },
                    modifier = if (token == selected) {
                        Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                    } else {
                        Modifier
                    },
                    onClick = {
                        expanded = false
                        onSelected(token)
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorTokenSwatch(
    token: NestedColorToken,
    filled: Boolean = false
) {
    val color = when (token) {
        NestedColorToken.Default -> MaterialTheme.colorScheme.onSurfaceVariant
        NestedColorToken.Red -> androidx.compose.ui.graphics.Color(0xFFE57373)
        NestedColorToken.Orange -> androidx.compose.ui.graphics.Color(0xFFFFB74D)
        NestedColorToken.Yellow -> androidx.compose.ui.graphics.Color(0xFFFFD54F)
        NestedColorToken.Green -> androidx.compose.ui.graphics.Color(0xFF81C784)
        NestedColorToken.Blue -> androidx.compose.ui.graphics.Color(0xFF64B5F6)
        NestedColorToken.Purple -> androidx.compose.ui.graphics.Color(0xFFBA68C8)
        NestedColorToken.Pink -> androidx.compose.ui.graphics.Color(0xFFF06292)
    }
    Box(modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (filled && token != NestedColorToken.Default) color.copy(alpha = 0.28f) else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, color.copy(alpha = 0.75f), CircleShape), contentAlignment = Alignment.Center) {
        if (!filled && token != NestedColorToken.Default) Box(Modifier.size(10.dp).clip(CircleShape).background(color))
    }
}

private fun priorityMarker(priority: TaskPriority): String = when (priority) {
    TaskPriority.None -> ""
    TaskPriority.Low -> "!"
    TaskPriority.Medium -> "!!"
    TaskPriority.High -> "!!!"
}

@Composable
private fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.None -> MaterialTheme.colorScheme.onSurfaceVariant
    TaskPriority.Low -> Color(0xFF4CAF50)
    TaskPriority.Medium -> Color(0xFFFF9800)
    TaskPriority.High -> Color(0xFFE53935)
}

@Composable
private fun NestedItemMetadataPreview(
    item: com.checkit.domain.NestedListItem,
    summary: NestedMetricSummary,
    isLeaf: Boolean
) {
    val hasNote = !item.note.isNullOrBlank()
    val hasTags = item.tags.isNotEmpty()
    val hasDateRange = item.startDate != null || item.endDate != null
    val showTracked = isLeaf || item.showTrackedMinutes
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (summary.doneItemCount > 0) {
                MetricChip(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append("${summary.doneItemCount}")
                    }
                    append(" done")
                })
            }
            if (showTracked && summary.trackedMinutes > 0) {
                MetricChip(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append("${summary.trackedMinutes}")
                    }
                    append(" min")
                })
            }
            item.manualMetrics.filter { it.enabled }.forEach { metric ->
                if (metric.value.isNotBlank()) {
                    MetricChip(
                        content = buildAnnotatedString {
                            if (metric.name.isNotBlank()) {
                                append(metric.name)
                                append(" ")
                            }
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                append(metric.value)
                            }
                            if (!metric.targetValue.isNullOrBlank()) {
                                append("/")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(metric.targetValue)
                                }
                            }
                            val unit = metric.displayUnit()
                            if (unit != null) {
                                append(" ")
                                append(unit)
                            }
                        },
                        manual = true
                    )
                }
            }
        }
        if (!hasNote && !hasTags && !hasDateRange) return@Column

        if (hasNote) {
            Text(
                text = item.note.orEmpty(),
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
                item.tags.forEach { tag -> TagPlain(tag) }
                if (hasDateRange) {
                    DateRangePill(
                        startDate = item.startDate,
                        endDate = item.endDate
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(content: AnnotatedString, manual: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (manual) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.labelSmall,
            color = if (manual) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private fun NestedManualMetric.displayUnit(): String? = when (unit) {
    NestedMetricUnit.None -> null
    NestedMetricUnit.Custom -> customUnit?.takeIf { it.isNotBlank() }
    else -> unitLabel(unit)
}

private fun NestedMetricUnit.displayName(customUnit: String? = null): String = when (this) {
    NestedMetricUnit.None -> "None"
    NestedMetricUnit.Custom -> customUnit?.takeIf { it.isNotBlank() } ?: "Custom"
    else -> unitLabel(this)
}

private fun unitLabel(unit: NestedMetricUnit): String = when (unit) {
    NestedMetricUnit.None -> ""
    NestedMetricUnit.Percentage -> "%"
    NestedMetricUnit.Points -> "points"
    NestedMetricUnit.Count -> "count"
    NestedMetricUnit.Items -> "items"
    NestedMetricUnit.Hours -> "hours"
    NestedMetricUnit.Days -> "days"
    NestedMetricUnit.Currency -> "currency"
    NestedMetricUnit.Rating -> "rating"
    NestedMetricUnit.Custom -> ""
}

@Composable
private fun NestedItemDetailsDialog(
    node: NestedItemNode,
    summary: NestedMetricSummary,
    onDismiss: () -> Unit,
    onSave: (Int, MetricRollupPolicy, Boolean, List<NestedManualMetric>) -> Unit
) {
    val item = node.item
    val isLeaf = !node.hasChildren
    var actualMinutes by remember(item.id) { mutableStateOf(if (item.actualMinutes > 0) item.actualMinutes.toString() else "") }
    var policy by remember(item.id) { mutableStateOf(item.metricRollupPolicy) }
    var showTrackedMinutes by remember(item.id) { mutableStateOf(item.showTrackedMinutes) }
    var metrics by remember(item.id) { mutableStateOf(item.manualMetrics) }
    var policyExpanded by remember { mutableStateOf(false) }
    var unitExpandedIndex by remember { mutableStateOf<Int?>(null) }

    val directChildCount = node.children.size
    val totalChildCount = remember(node) { countTotalDescendants(node) }
    val doneChildCount = summary.doneItemCount
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
                        subtitle = if (totalChildCount > 0) "$doneChildCount done" else "Leaf item",
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

                // Section 3: Custom/Manual Metrics
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CUSTOM METRICS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                metrics = metrics + NestedManualMetric(
                                    itemId = item.id,
                                    name = "",
                                    value = "",
                                    sortOrder = metrics.size
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Add", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (metrics.isEmpty()) {
                        Text(
                            text = "No custom metrics added.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    metrics.forEachIndexed { index, metric ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CompactFlatTextField(
                                    value = metric.name,
                                    onValueChange = { value ->
                                        metrics = metrics.toMutableList().also { it[index] = metric.copy(name = value) }
                                    },
                                    placeholder = "Metric name",
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { metrics = metrics.filterIndexed { metricIndex, _ -> metricIndex != index } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete metric",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CompactFlatTextField(
                                    value = metric.value,
                                    onValueChange = { value ->
                                        metrics = metrics.toMutableList().also { it[index] = metric.copy(value = value) }
                                    },
                                    placeholder = "Value",
                                    modifier = Modifier.weight(1f)
                                )
                                CompactFlatTextField(
                                    value = metric.targetValue.orEmpty(),
                                    onValueChange = { value ->
                                        metrics = metrics.toMutableList().also { it[index] = metric.copy(targetValue = value) }
                                    },
                                    placeholder = "Target",
                                    modifier = Modifier.weight(1f)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .clickable { unitExpandedIndex = index }
                                            .padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = metric.unit.displayName(metric.customUnit),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = unitExpandedIndex == index,
                                        onDismissRequest = { unitExpandedIndex = null }
                                    ) {
                                        NestedMetricUnit.entries.forEach { unit ->
                                            DropdownMenuItem(
                                                text = { Text(unit.displayName(), style = MaterialTheme.typography.bodySmall) },
                                                onClick = {
                                                    metrics = metrics.toMutableList().also {
                                                        it[index] = metric.copy(
                                                            unit = unit,
                                                            customUnit = if (unit == NestedMetricUnit.Custom) metric.customUnit else null
                                                        )
                                                    }
                                                    unitExpandedIndex = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (metric.unit == NestedMetricUnit.Custom) {
                                CompactFlatTextField(
                                    value = metric.customUnit.orEmpty(),
                                    onValueChange = { value ->
                                        metrics = metrics.toMutableList().also { it[index] = metric.copy(customUnit = value) }
                                    },
                                    placeholder = "Custom unit (e.g. kg, pts)",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        actualMinutes.toIntOrNull() ?: 0,
                        policy,
                        showTrackedMinutes,
                        metrics.filter { it.name.isNotBlank() && it.value.isNotBlank() }
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

@Composable
private fun CompactFlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                keyboardOptions = keyboardOptions,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (suffix != null) {
            Text(
                text = suffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
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
    val itemId: Long? = null
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
private fun findAncestorChain(nodes: List<NestedItemNode>, id: Long): List<NestedItemNode>? {
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
    onFocusChange: (FocusPeriod) -> Unit,
    onQueryChange: (String) -> Unit,
    onHideCheckedChange: (Boolean) -> Unit,
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
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<Breadcrumb>,
    onCrumbClick: (Long) -> Unit,
    onRootClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                imageVector = Icons.Default.KeyboardArrowRight,
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
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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

// ---------------- toolbar ----------------

@Composable
private fun EditorToolbar(
    state: NestedEditorState.Active,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddChild: () -> Unit,
    onAddSibling: () -> Unit,
    onDelete: () -> Unit,
    onAddRoot: () -> Unit,
    onManageDetails: () -> Unit,
    onAddToDailyPlan: () -> Unit
) {
    val hasSelection = state.selectedItemId != null
    val selectedNode = state.selectedItemId?.let { id -> state.tree.nodeById[id] }
    val canZoomIn = hasSelection && (selectedNode?.hasChildren == true)
    val canZoomOut = state.zoomPath.isNotEmpty()
    val canAddSibling = hasSelection && (selectedNode?.item?.parentId != null)
    var showMore by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(Icons.Default.ZoomIn, stringResource(Res.string.nested_zoom_in), canZoomIn, onZoomIn)
        ToolbarButton(Icons.Default.ZoomOut, stringResource(Res.string.nested_zoom_out), canZoomOut, onZoomOut)
        ToolbarButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(Res.string.nested_indent), hasSelection, onIndent)
        ToolbarButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(Res.string.nested_outdent), hasSelection, onOutdent)
        ToolbarButton(Icons.Default.KeyboardArrowUp, stringResource(Res.string.nested_move_up), hasSelection, onMoveUp)
        ToolbarButton(Icons.Default.KeyboardArrowDown, stringResource(Res.string.nested_move_down), hasSelection, onMoveDown)
        ToolbarButton(Icons.Default.Add, stringResource(Res.string.nested_add_child), hasSelection, onAddChild)
        ToolbarButton(Icons.AutoMirrored.Filled.NoteAdd, stringResource(Res.string.nested_add_sibling), canAddSibling, onAddSibling)
        Box {
            ToolbarButton(Icons.Default.MoreVert, "More actions", true) { showMore = true }
            DropdownMenu(
                expanded = showMore,
                onDismissRequest = { showMore = false }
            ) {
                ToolbarMenuItem("Details", hasSelection, onManageDetails)
                ToolbarMenuItem("Add to daily plan", hasSelection) {
                    showMore = false; onAddToDailyPlan()
                }
                ToolbarMenuItem("Add root item", true) {
                    showMore = false; onAddRoot()
                }
                ToolbarMenuItem(stringResource(Res.string.nested_batch_delete), hasSelection) {
                    showMore = false; onDelete()
                }
            }
        }
    }
}

@Composable
private fun ToolbarMenuItem(label: String, enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.DropdownMenuItem(
        text = { Text(label) },
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
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
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
        ToolbarButton(Icons.Default.ArrowBack, stringResource(Res.string.cancel), true, onExit)
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

@Composable
private fun NestedTree(
    node: NestedItemNode,
    depth: Int,
    isVisible: Boolean,
    isInCheckedBranch: Boolean,
    continuingLevels: Set<Int>,
    state: NestedEditorState.Active,
    viewModel: NestedListsViewModel
) {
    val item = node.item
    val guideColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.34f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.34f),
        MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
    )
    val isSelected = item.id in state.selection.selectedIds ||
        (!state.selection.isActive && state.selectedItemId == item.id)
    val isEditing = state.editingTextItemId == item.id

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
                    // Draw in the unpadded container so every depth shares the
                    // same x-coordinate across all rows.
                    val dotSize = if (item.collapsed && node.hasChildren) 14.dp else 7.dp
                    val dotTop = 12.dp.toPx()
                    val dotSizePx = dotSize.toPx()
                    val dotCenterY = dotTop + dotSizePx / 2
                    val dotBottomY = dotTop + dotSizePx
                    val strokeWidth = 1.dp.toPx()
                    val curveRadius = 6.dp.toPx()
                    val guideX: (Int) -> Float = { level -> level * 16.dp.toPx() + 8.dp.toPx() }

                    continuingLevels.forEach { level ->
                        val x = guideX(level)
                        drawLine(
                            color = guideColors[level % guideColors.size],
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
                            color = guideColors[(depth - 1) % guideColors.size],
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    if (node.hasChildren && !item.collapsed) {
                        val x = guideX(depth)
                        drawLine(
                            color = guideColors[depth % guideColors.size],
                            start = Offset(x, dotBottomY),
                            end = Offset(x, size.height),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
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
                            onClick = { viewModel.toggleCollapsed(item.id) }
                        ),
                    contentAlignment = Alignment.TopStart
                ) {
                    val dotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(36.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .size(if (item.collapsed && node.hasChildren) 14.dp else 7.dp)
                                .clip(CircleShape)
                                .then(
                                    if (item.collapsed && node.hasChildren) {
                                        Modifier.border(2.dp, dotColor, CircleShape)
                                    } else {
                                        Modifier.background(dotColor)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.collapsed && node.hasChildren) {
                                Box(
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                isEditing -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                item.backgroundColor != NestedColorToken.Default -> nestedColor(item.backgroundColor).copy(alpha = 0.18f)
                                else -> Color.Transparent
                            }
                        )
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
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                if (item.checkboxEnabled) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { viewModel.toggleChecked(item.id) },
                        modifier = Modifier.size(28.dp).scale(0.7f).align(Alignment.Top)
                    )
                } else {
                    Spacer(Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        var text by remember(item.id) { mutableStateOf(item.text) }
                        val focusRequester = remember(item.id) { FocusRequester() }
                        LaunchedEffect(item.id) {
                            text = item.text
                            focusRequester.requestFocus()
                        }
                        val focusManager = LocalFocusManager.current
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            textStyle = nestedTextStyle(item.textStyle).copy(color = nestedTextColor(item.textColor)),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                viewModel.saveItemText(item.id, text)
                            }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    } else {
                        Text(
                            text = item.text,
                            style = nestedTextStyle(item.textStyle),
                            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                            color = when {
                                isInCheckedBranch -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
                                item.textColor != NestedColorToken.Default -> nestedColor(item.textColor)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (depth == 0 && !node.hasChildren) FontWeight.Bold else null
                        )
                    }
                    NestedItemMetadataPreview(
                        item = item,
                        summary = state.tree.metricSummaryById[item.id] ?: NestedMetricSummary(),
                        isLeaf = !node.hasChildren
                    )
                }

                if (item.priority != TaskPriority.None) {
                    Text(
                        text = priorityMarker(item.priority),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor(item.priority),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }

                if (state.selection.isActive) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { viewModel.toggleSelect(item.id) },
                        modifier = Modifier.size(28.dp).align(Alignment.Top)
                    )
                }
            }
        }

    }
    }
}
}

@Composable
private fun nestedTextStyle(style: NestedTextStyle) = when (style) {
    NestedTextStyle.Body -> MaterialTheme.typography.bodyLarge
    NestedTextStyle.Header -> MaterialTheme.typography.titleLarge
    NestedTextStyle.Subheader -> MaterialTheme.typography.titleMedium
}

@Composable
private fun nestedTextColor(token: NestedColorToken) =
    if (token == NestedColorToken.Default) MaterialTheme.colorScheme.onSurface else nestedColor(token)

@Composable
private fun nestedColor(token: NestedColorToken) = when (token) {
    NestedColorToken.Default -> MaterialTheme.colorScheme.onSurfaceVariant
    NestedColorToken.Red -> Color(0xFFE57373)
    NestedColorToken.Orange -> Color(0xFFFFB74D)
    NestedColorToken.Yellow -> Color(0xFFFFD54F)
    NestedColorToken.Green -> Color(0xFF66BB6A)
    NestedColorToken.Blue -> Color(0xFF42A5F5)
    NestedColorToken.Purple -> Color(0xFFAB47BC)
    NestedColorToken.Pink -> Color(0xFFEC407A)
}

private data class VisibleNestedRow(
    val node: NestedItemNode,
    val depth: Int,
    val isVisible: Boolean,
    val isInCheckedBranch: Boolean,
    val continuingLevels: Set<Int> = emptySet()
)

private data class NestedTraversalEntry(
    val node: NestedItemNode,
    val depth: Int,
    val isVisible: Boolean,
    val ancestorChecked: Boolean,
    val continuingLevels: Set<Int>
)

/** Iterative traversal keeps stable rows available for expand/collapse animation. */
private fun flattenVisibleNodes(roots: List<NestedItemNode>): List<VisibleNestedRow> {
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
            continuingLevels = entry.continuingLevels
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
    val guideColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.34f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.34f),
        MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
    )
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
                        color = guideColors[level % guideColors.size],
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
                        color = guideColors[(depth - 1) % guideColors.size],
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
            val focusManager = LocalFocusManager.current
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
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onCommit()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
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
