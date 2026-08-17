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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
import checkit.shared.generated.resources.nested_untitled_document
import checkit.shared.generated.resources.nested_zoom_in
import checkit.shared.generated.resources.nested_zoom_out
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedManualMetric
import com.checkit.domain.NestedMetricSummary
import com.checkit.domain.NestedMetricUnit
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.TaskPriority
import com.checkit.ui.components.DatePicker
import com.checkit.ui.components.TagOptionMenu
import com.checkit.ui.components.TagPlain
import com.checkit.ui.components.TinyTopAppBar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NestedListEditorScreen(
    state: NestedListEditorUiState,
    viewModel: NestedListsViewModel,
    onAddToDailyPlan: (title: String, tagIds: List<Long>) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit
) {
    var detailsItemId by remember { mutableStateOf<Long?>(null) }
    val tree = state.tree
    if (tree == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val focusedNode = state.focusedItem
    val visibleRoots = focusedNode?.let { listOf(it) } ?: tree.rootNodes
    val visibleRows = flattenVisibleNodes(visibleRoots)
    val breadcrumbs = buildBreadcrumbs(state, tree.rootNodes)

    Column(modifier = Modifier.fillMaxSize()) {
        TinyTopAppBar(
            title = {
                Text(
                    text = tree.document.title.ifBlank { stringResource(Res.string.nested_untitled_document) },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                if (state.selectionMode) {
                    TextButton(onClick = viewModel::selectAll) { Text("Select all") }
                    TextButton(onClick = viewModel::exitSelectionMode) { Text(stringResource(Res.string.cancel)) }
                }
            }
        )
        BreadcrumbBar(
            breadcrumbs = breadcrumbs,
            onCrumbClick = { itemId -> viewModel.zoomToItem(itemId) },
            onRootClick = viewModel::zoomToRoot
        )

        if (state.selectionMode) {
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
                onIndent = { state.editingItemId?.let(viewModel::indent) },
                onOutdent = { state.editingItemId?.let(viewModel::outdent) },
                onMoveUp = { state.editingItemId?.let(viewModel::moveUp) },
                onMoveDown = { state.editingItemId?.let(viewModel::moveDown) },
                onAddChild = { state.editingItemId?.let(viewModel::startAddChild) },
                onAddSibling = { state.editingItemId?.let(viewModel::startAddSibling) },
                onDelete = { state.editingItemId?.let(viewModel::requestDeleteItem) },
                onAddRoot = viewModel::startAddRoot,
                onManageDetails = { state.editingItemId?.let { detailsItemId = it } },
                onAddToDailyPlan = {
                    state.editingItemId?.let { id ->
                        state.tree?.nodeById?.get(id)?.item?.let { item ->
                            onAddToDailyPlan(item.text, item.tags.map { it.id })
                        }
                    }
                }
            )
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (state.isAddingItem && state.addingItemAnchorId == null) {
                    item(key = "new-root") {
                        NewItemRow(
                            depth = 0,
                            text = state.newItemText,
                            onTextChange = viewModel::updateNewItemText,
                            onCommit = viewModel::commitNewItem,
                            onCancel = viewModel::cancelAddItem
                        )
                    }
                }
                if (visibleRows.isEmpty()) {
                    if (!state.isAddingItem || state.addingItemAnchorId != null) {
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
                            state = state,
                            viewModel = viewModel
                        )
                        if (state.isAddingItem && state.addingItemAnchorId == row.node.item.id) {
                            NewItemRow(
                                depth = state.newItemDepth,
                                text = state.newItemText,
                                onTextChange = viewModel::updateNewItemText,
                                onCommit = viewModel::commitNewItem,
                                onCancel = viewModel::cancelAddItem
                            )
                        }
                    }
                }
            }
            if (!state.selectionMode) {
                val selectedItem = state.editingItemId?.let { tree.itemById[it] }
                if (selectedItem != null) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        NestedFormattingBottomBar(
                            item = selectedItem,
                            onFormattingChange = { style, textColor, backgroundColor ->
                                viewModel.updateItemFormatting(selectedItem.id, style, textColor, backgroundColor)
                            },
                            onMetadataChange = { date, priority ->
                                viewModel.updateItemMetadata(selectedItem.id, date, priority)
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

    if (state.showDeleteConfirm) {
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

    state.editingNoteItemId?.let { noteItemId ->
        val noteItem = state.tree?.nodeById?.get(noteItemId)
        val initialNote = noteItem?.item?.note.orEmpty()
        var note by remember(noteItemId) { mutableStateOf(initialNote) }
        AlertDialog(
            onDismissRequest = viewModel::stopEditNote,
            title = { Text(stringResource(Res.string.nested_edit_note)) },
            text = {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(2_000) },
                    label = { Text("Note") },
                    placeholder = { Text("Add context or details") },
                    minLines = 4,
                    maxLines = 8,
                    supportingText = { Text("${note.length}/2,000") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveItemNote(noteItemId, note) }) {
                    Text(stringResource(Res.string.nested_edit_note))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::stopEditNote) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }

    detailsItemId?.let { itemId ->
        state.tree.itemById.get(itemId)?.let { item ->
            NestedItemDetailsDialog(
                item = item,
                summary = state.tree.metricSummaryById[itemId] ?: NestedMetricSummary(),
                isLeaf = state.tree.nodeById[itemId]?.hasChildren != true,
                onDismiss = { detailsItemId = null },
                onSave = { actualMinutes, policy, showTrackedMinutes, manualMetrics ->
                    viewModel.updateItemMetricSettings(itemId, actualMinutes, policy, showTrackedMinutes)
                    viewModel.replaceManualMetrics(itemId, manualMetrics)
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
    onMetadataChange: (kotlinx.datetime.LocalDate?, TaskPriority) -> Unit,
    availableTags: List<com.checkit.domain.TagItem>,
    onTagsChange: (List<Long>) -> Unit,
    onToggleNote: () -> Unit,
    onToggleCheckbox: () -> Unit,
    onSetChecked: (Boolean) -> Unit
) {
    var priorityExpanded by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }
    val colorTokens = listOf(
        NestedColorToken.Default, NestedColorToken.Red, NestedColorToken.Orange,
        NestedColorToken.Yellow, NestedColorToken.Green, NestedColorToken.Blue,
        NestedColorToken.Purple, NestedColorToken.Pink
    )
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .background(
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
            Box {
                IconButton(onClick = { styleExpanded = true }, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.FormatSize,
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
            Box {
                IconButton(onClick = { priorityExpanded = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Flag, contentDescription = "Priority", tint = priorityColor(item.priority))
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
                                onMetadataChange(item.doDate, priority)
                            }
                        )
                    }
                }
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
            DatePicker(
                date = item.doDate,
                startTimeMinutes = null,
                endTimeMinutes = null,
                onDateChange = { onMetadataChange(it, item.priority) },
                onTimeChange = { _, _ -> },
                supportsEndTime = false,
                iconOnly = true
            )
            IconButton(onClick = onToggleNote, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.EditNote,
                    contentDescription = "Edit note",
                    tint = if (!item.note.isNullOrEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onSetChecked(!item.checked) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Done,
                    contentDescription = if (item.checked) "Uncheck" else "Check off",
                    tint = if (item.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    val hasDate = item.doDate != null
    val showTracked = isLeaf || item.showTrackedMinutes
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (summary.doneItemCount > 0) MetricChip("${summary.doneItemCount} done")
            if (showTracked && summary.trackedMinutes > 0) MetricChip("${summary.trackedMinutes} min")
            item.manualMetrics.filter { it.enabled }.forEach { metric ->
                if (metric.value.isNotBlank()) {
                    val target = metric.targetValue?.takeIf { it.isNotBlank() }?.let { " / $it" }.orEmpty()
                    val unit = metric.displayUnit()
                    MetricChip(
                        listOfNotNull(metric.name.takeIf { it.isNotBlank() }, "${metric.value}$target", unit)
                            .joinToString(" "),
                        manual = true
                    )
                }
            }
        }
        if (!hasNote && !hasTags && !hasDate) return@Column

        if (hasNote) {
            Text(
                text = item.note.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hasTags || hasDate) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.tags.forEach { tag -> TagPlain(tag) }
                item.doDate?.let { date ->
                    Text(
                        text = "$date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(text: String, manual: Boolean = false) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (manual) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.88f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (manual) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
    item: com.checkit.domain.NestedListItem,
    summary: NestedMetricSummary,
    isLeaf: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, MetricRollupPolicy, Boolean, List<NestedManualMetric>) -> Unit
) {
    var actualMinutes by remember(item.id) { mutableStateOf(item.actualMinutes.toString()) }
    var policy by remember(item.id) { mutableStateOf(item.metricRollupPolicy) }
    var showTrackedMinutes by remember(item.id) { mutableStateOf(item.showTrackedMinutes) }
    var metrics by remember(item.id) { mutableStateOf(item.manualMetrics) }
    var policyExpanded by remember { mutableStateOf(false) }
    var unitExpandedIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Details", style = MaterialTheme.typography.titleLarge)
                Text(item.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = actualMinutes,
                        onValueChange = { value -> if (value.all { it.isDigit() }) actualMinutes = value },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(0.8f)
                    )
                    Box(modifier = Modifier.weight(1.2f)) {
                        TextButton(onClick = { policyExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(policyLabel(policy), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = policyExpanded, onDismissRequest = { policyExpanded = false }) {
                            MetricRollupPolicy.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(policyLabel(option)) },
                                    onClick = { policy = option; policyExpanded = false }
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tracked minutes", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${summary.doneItemCount} children done · ${summary.trackedMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = showTrackedMinutes || isLeaf,
                        onCheckedChange = { if (!isLeaf) showTrackedMinutes = it },
                        modifier = Modifier.scale(0.8f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Manual", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        metrics = metrics + NestedManualMetric(itemId = item.id, name = "", value = "", sortOrder = metrics.size)
                    }) { Text("Add") }
                }
                metrics.forEachIndexed { index, metric ->
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = metric.name,
                                    onValueChange = { value -> metrics = metrics.toMutableList().also { it[index] = metric.copy(name = value) } },
                                    placeholder = { Text("Metric name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { metrics = metrics.filterIndexed { metricIndex, _ -> metricIndex != index } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete metric")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = metric.value,
                                    onValueChange = { value -> metrics = metrics.toMutableList().also { it[index] = metric.copy(value = value) } },
                                    placeholder = { Text("Value") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = metric.targetValue.orEmpty(),
                                    onValueChange = { value -> metrics = metrics.toMutableList().also { it[index] = metric.copy(targetValue = value) } },
                                    placeholder = { Text("Target") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Box {
                                TextButton(onClick = { unitExpandedIndex = index }) {
                                    Text(metric.unit.displayName(metric.customUnit))
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(
                                    expanded = unitExpandedIndex == index,
                                    onDismissRequest = { unitExpandedIndex = null }
                                ) {
                                    NestedMetricUnit.entries.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit.displayName()) },
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
                            if (metric.unit == NestedMetricUnit.Custom) {
                                OutlinedTextField(
                                    value = metric.customUnit.orEmpty(),
                                    onValueChange = { value -> metrics = metrics.toMutableList().also { it[index] = metric.copy(customUnit = value) } },
                                    placeholder = { Text("Custom unit") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(actualMinutes.toIntOrNull() ?: 0, policy, showTrackedMinutes, metrics.filter { it.name.isNotBlank() && it.value.isNotBlank() })
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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

private fun buildBreadcrumbs(state: NestedListEditorUiState, roots: List<NestedItemNode>): List<Breadcrumb> {
    val result = mutableListOf<Breadcrumb>()
    result.add(Breadcrumb(label = "", depth = 0, isRoot = true))
    val focusedId = state.focusItemIds.lastOrNull() ?: return result
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
private fun BreadcrumbBar(
    breadcrumbs: List<Breadcrumb>,
    onCrumbClick: (Long) -> Unit,
    onRootClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        else androidx.compose.ui.graphics.Color.Transparent
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
    state: NestedListEditorUiState,
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
    val hasSelection = state.editingItemId != null
    val selectedNode = state.editingItemId?.let { id -> state.tree?.nodeById?.get(id) }
    val canZoomIn = hasSelection && (selectedNode?.hasChildren == true)
    val canZoomOut = state.focusItemIds.isNotEmpty()
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
    state: NestedListEditorUiState,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onExit: () -> Unit
) {
    val count = state.selectedItemIds.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
    state: NestedListEditorUiState,
    viewModel: NestedListsViewModel
) {
    val item = node.item
    val guideColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.34f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.34f),
        MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
    )
    val isSelected = item.id in state.selectedItemIds ||
        (!state.selectionMode && state.editingItemId == item.id)
    val isEditing = state.isEditingText && state.editingItemId == item.id

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
                    repeat(depth + if (node.hasChildren) 1 else 0) { level ->
                            val x = level * 16.dp.toPx() + 8.dp.toPx()
                            drawLine(
                                color = guideColors[level % guideColors.size],
                            start = androidx.compose.ui.geometry.Offset(
                                x,
                                if (level == depth) {
                                    (12.dp + if (item.collapsed && node.hasChildren) 14.dp else 7.dp).toPx()
                                } else {
                                    0f
                                }
                            ),
                                end = androidx.compose.ui.geometry.Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
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
                        .clickable(
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
                                else -> androidx.compose.ui.graphics.Color.Transparent
                            }
                        )
                        .combinedClickable(
                            onClick = {
                                if (state.selectionMode) {
                                    viewModel.toggleSelect(item.id)
                                } else {
                                    viewModel.selectItem(item.id)
                                    if (state.isAddingItem) viewModel.cancelAddItem()
                                }
                            },
                            onDoubleClick = {
                                if (!state.selectionMode) viewModel.startEditText(item.id)
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
                        summary = state.tree?.metricSummaryById?.get(item.id) ?: NestedMetricSummary(),
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

                if (state.selectionMode) {
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
    NestedColorToken.Red -> androidx.compose.ui.graphics.Color(0xFFE57373)
    NestedColorToken.Orange -> androidx.compose.ui.graphics.Color(0xFFFFB74D)
    NestedColorToken.Yellow -> androidx.compose.ui.graphics.Color(0xFFFFD54F)
    NestedColorToken.Green -> androidx.compose.ui.graphics.Color(0xFF66BB6A)
    NestedColorToken.Blue -> androidx.compose.ui.graphics.Color(0xFF42A5F5)
    NestedColorToken.Purple -> androidx.compose.ui.graphics.Color(0xFFAB47BC)
    NestedColorToken.Pink -> androidx.compose.ui.graphics.Color(0xFFEC407A)
}

private data class VisibleNestedRow(
    val node: NestedItemNode,
    val depth: Int,
    val isVisible: Boolean,
    val isInCheckedBranch: Boolean
)

private data class NestedTraversalEntry(
    val node: NestedItemNode,
    val depth: Int,
    val isVisible: Boolean,
    val ancestorChecked: Boolean
)

/** Iterative traversal keeps stable rows available for expand/collapse animation. */
private fun flattenVisibleNodes(roots: List<NestedItemNode>): List<VisibleNestedRow> {
    if (roots.isEmpty()) return emptyList()
    val result = ArrayList<VisibleNestedRow>()
    val stack = ArrayDeque<NestedTraversalEntry>()
    roots.asReversed().forEach { stack.addLast(NestedTraversalEntry(it, 0, true, false)) }
    while (stack.isNotEmpty()) {
        val entry = stack.removeLast()
        val node = entry.node
        val isInCheckedBranch = entry.ancestorChecked || node.item.checked
        result += VisibleNestedRow(node, entry.depth, entry.isVisible, isInCheckedBranch)
        val childrenVisible = entry.isVisible && !node.item.collapsed
        node.children.asReversed().forEach { child ->
            stack.addLast(
                NestedTraversalEntry(
                    node = child,
                    depth = entry.depth + 1,
                    isVisible = childrenVisible,
                    ancestorChecked = isInCheckedBranch
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
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp, top = 0.dp, bottom = 0.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val focusManager = LocalFocusManager.current
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
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
