package com.checkit.ui.nested

import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import checkit.shared.generated.resources.nested_selection_mode
import checkit.shared.generated.resources.nested_untitled_document
import checkit.shared.generated.resources.nested_zoom_in
import checkit.shared.generated.resources.nested_zoom_out
import com.checkit.domain.NestedItemNode
import com.checkit.ui.components.TinyTopAppBar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NestedListEditorScreen(
    state: NestedListEditorUiState,
    viewModel: NestedListsViewModel,
    onNavigateBack: () -> Unit
) {
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
                onToggleNote = { state.editingItemId?.let(viewModel::startEditNote) },
                onToggleCheckbox = { state.editingItemId?.let(viewModel::toggleCheckboxEnabled) },
                onDelete = { state.editingItemId?.let(viewModel::requestDeleteItem) },
                onAddRoot = viewModel::startAddRoot
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
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
                        state = state,
                        viewModel = viewModel
                    )
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
                    onValueChange = { note = it },
                    minLines = 3,
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
    onToggleNote: () -> Unit,
    onToggleCheckbox: () -> Unit,
    onDelete: () -> Unit,
    onAddRoot: () -> Unit
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
        ToolbarButton(Icons.Default.KeyboardArrowRight, stringResource(Res.string.nested_indent), hasSelection, onIndent)
        ToolbarButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(Res.string.nested_outdent), hasSelection, onOutdent)
        ToolbarButton(Icons.Default.KeyboardArrowUp, stringResource(Res.string.nested_move_up), hasSelection, onMoveUp)
        ToolbarButton(Icons.Default.KeyboardArrowDown, stringResource(Res.string.nested_move_down), hasSelection, onMoveDown)
        Box {
            ToolbarButton(Icons.Default.MoreVert, "More actions", true) { showMore = true }
            androidx.compose.material3.DropdownMenu(
                expanded = showMore,
                onDismissRequest = { showMore = false }
            ) {
                ToolbarMenuItem(stringResource(Res.string.nested_add_child), hasSelection) {
                    showMore = false; onAddChild()
                }
                ToolbarMenuItem(stringResource(Res.string.nested_add_sibling), canAddSibling) {
                    showMore = false; onAddSibling()
                }
                ToolbarMenuItem("Add root item", true) {
                    showMore = false; onAddRoot()
                }
                ToolbarMenuItem(stringResource(Res.string.nested_edit_note), hasSelection) {
                    showMore = false; onToggleNote()
                }
                ToolbarMenuItem("Checkbox", hasSelection) {
                    showMore = false; onToggleCheckbox()
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
    val isSelected = item.id in state.selectedItemIds
    val isEditing = state.editingItemId == item.id
    val showNewItemRow = state.isAddingItem && state.addingItemAnchorId == item.id

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
                    repeat(depth) { level ->
                        val x = level * 16.dp.toPx() + 8.dp.toPx()
                        drawLine(
                            color = guideColors[level % guideColors.size],
                            start = androidx.compose.ui.geometry.Offset(x, 0f),
                            end = androidx.compose.ui.geometry.Offset(x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        isEditing -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else -> androidx.compose.ui.graphics.Color.Transparent
                    }
                )
                .clickable {
                    if (state.selectionMode) {
                        viewModel.toggleSelect(item.id)
                    } else {
                        viewModel.startEditText(item.id)
                    }
                }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.hasChildren) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.collapsed) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                androidx.compose.ui.graphics.Color.Transparent
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { viewModel.toggleCollapsed(item.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Crossfade(targetState = item.collapsed, label = "collapseToggle") { collapsed ->
                            Icon(
                                imageVector = if (collapsed) Icons.Default.Add else Icons.Default.Remove,
                                contentDescription = if (collapsed) "Expand children" else "Collapse children",
                                tint = if (collapsed) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.width(28.dp))
            }

            if (item.checkboxEnabled) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = { viewModel.toggleChecked(item.id) },
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Spacer(Modifier.width(8.dp))
            }

            if (isEditing) {
                var text by remember(item.id) { mutableStateOf(item.text) }
                LaunchedEffect(item.id) { text = item.text }
                val focusManager = LocalFocusManager.current
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.saveItemText(item.id, text)
                    }),
                    modifier = Modifier.weight(1f)
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (depth == 0 && !node.hasChildren) FontWeight.Bold else FontWeight.Normal
                    )
                    if (item.note.isNullOrBlank().not()) {
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2
                        )
                    }
                }
            }

            if (state.selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { viewModel.toggleSelect(item.id) }
                )
            }
        }
        }

        if (showNewItemRow) {
            val isChild = state.newItemParentId == item.id
            NewItemRow(
                depth = if (isChild) depth + 1 else depth,
                text = state.newItemText,
                onTextChange = viewModel::updateNewItemText,
                onCommit = viewModel::commitNewItem,
                onCancel = viewModel::cancelAddItem
            )
        }

    }
    }
}

private data class VisibleNestedRow(
    val node: NestedItemNode,
    val depth: Int,
    val isVisible: Boolean
)

/** Iterative traversal keeps stable rows available for expand/collapse animation. */
private fun flattenVisibleNodes(roots: List<NestedItemNode>): List<VisibleNestedRow> {
    if (roots.isEmpty()) return emptyList()
    val result = ArrayList<VisibleNestedRow>()
    val stack = ArrayDeque<Triple<NestedItemNode, Int, Boolean>>()
    roots.asReversed().forEach { stack.addLast(Triple(it, 0, true)) }
    while (stack.isNotEmpty()) {
        val (node, depth, isVisible) = stack.removeLast()
        result += VisibleNestedRow(node, depth, isVisible)
        val childrenVisible = isVisible && !node.item.collapsed
        node.children.asReversed().forEach { child ->
            stack.addLast(Triple(child, depth + 1, childrenVisible))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val focusManager = LocalFocusManager.current
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
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
                modifier = Modifier.fillMaxWidth()
            )
        }
        IconButton(
            onClick = onCancel,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel")
        }
    }
}
