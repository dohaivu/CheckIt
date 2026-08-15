package com.checkit.ui.nested

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
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
            onCrumbClick = { depth -> viewModel.zoomOutTo(depth) },
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
                onDelete = { state.editingItemId?.let(viewModel::requestDeleteItem) }
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(visibleRows, key = { it.node.item.id }) { row ->
                NestedTree(
                    node = row.node,
                    depth = row.depth,
                    state = state,
                    viewModel = viewModel
                )
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
        val noteItem = state.tree?.let { findNode(flattenEditorNodes(visibleRoots), noteItemId) }
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
    val isRoot: Boolean
)

private fun buildBreadcrumbs(state: NestedListEditorUiState, roots: List<NestedItemNode>): List<Breadcrumb> {
    val result = mutableListOf<Breadcrumb>()
    result.add(Breadcrumb(label = "", depth = 0, isRoot = true))
    val focusedId = state.focusItemIds.lastOrNull() ?: return result
    val chain = findAncestorChain(roots, focusedId) ?: return result
    chain.forEach { node ->
        result.add(Breadcrumb(label = node.item.text, depth = result.size, isRoot = false))
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

private fun findNode(nodes: List<NestedItemNode>, id: Long): NestedItemNode? {
    for (node in nodes) {
        if (node.item.id == id) return node
        findNode(node.children, id)?.let { return it }
    }
    return null
}

private fun flattenEditorNodes(nodes: List<NestedItemNode>): List<NestedItemNode> =
    nodes.flatMap { node -> listOf(node) + flattenEditorNodes(node.children) }

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<Breadcrumb>,
    onCrumbClick: (Int) -> Unit,
    onRootClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.nested_root),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onRootClick() }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
        breadcrumbs.drop(1).forEach { crumb ->
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = crumb.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onCrumbClick(crumb.depth) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
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
    onDelete: () -> Unit
) {
    val hasSelection = state.editingItemId != null
    val selectedNode = state.editingItemId?.let { id -> findNode(state.tree?.rootNodes.orEmpty(), id) }
    val canZoomIn = hasSelection && (selectedNode?.hasChildren == true)
    val canZoomOut = state.focusItemIds.isNotEmpty()
    val canAddSibling = hasSelection && (selectedNode?.item?.parentId != null)
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
        ToolbarButton(Icons.Default.Add, stringResource(Res.string.nested_add_child), hasSelection, onAddChild)
        ToolbarButton(Icons.Default.NoteAdd, stringResource(Res.string.nested_add_sibling), canAddSibling, onAddSibling)
        ToolbarButton(Icons.Default.Flag, stringResource(Res.string.nested_edit_note), hasSelection, onToggleNote)
        ToolbarButton(Icons.Default.Done, "Checkbox", hasSelection, onToggleCheckbox)
        ToolbarButton(Icons.Default.Delete, stringResource(Res.string.nested_batch_delete), hasSelection, onDelete)
    }
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
private fun NestedTree(
    node: NestedItemNode,
    depth: Int,
    state: NestedListEditorUiState,
    viewModel: NestedListsViewModel
) {
    val item = node.item
    val isSelected = item.id in state.selectedItemIds
    val isEditing = state.editingItemId == item.id
    val showNewItemRow = state.isAddingItem && state.addingItemAnchorId == item.id

    Column(
        modifier = Modifier.fillMaxWidth()
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
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.hasChildren) {
                IconButton(
                    onClick = { viewModel.toggleCollapsed(item.id) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (item.collapsed) Icons.Default.Add else Icons.Default.Remove,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(20.dp))
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

        if (item.collapsed && node.hasChildren) {
            Text(
                text = "${countVisibleChildren(node)} items hidden",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = (depth * 16 + 40).dp, top = 2.dp, bottom = 2.dp)
            )
        }
    }
}

private data class VisibleNestedRow(
    val node: NestedItemNode,
    val depth: Int
)

/** Iterative pre-order traversal avoids composing a recursive tree for every row. */
private fun flattenVisibleNodes(roots: List<NestedItemNode>): List<VisibleNestedRow> {
    if (roots.isEmpty()) return emptyList()
    val result = ArrayList<VisibleNestedRow>()
    val stack = ArrayDeque<Pair<NestedItemNode, Int>>()
    roots.asReversed().forEach { stack.addLast(it to 0) }
    while (stack.isNotEmpty()) {
        val (node, depth) = stack.removeLast()
        result += VisibleNestedRow(node, depth)
        if (!node.item.collapsed) {
            node.children.asReversed().forEach { stack.addLast(it to depth + 1) }
        }
    }
    return result
}

private fun countVisibleChildren(node: NestedItemNode): Int {
    var count = 0
    val stack = ArrayDeque<NestedItemNode>()
    node.children.forEach(stack::addLast)
    while (stack.isNotEmpty()) {
        val current = stack.removeLast()
        count++
        current.children.forEach(stack::addLast)
    }
    return count
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
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 2.dp)
        )
        IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, contentDescription = "Cancel") }
    }
}
