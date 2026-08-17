package com.checkit.ui.nested

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.nested_delete_confirm
import checkit.shared.generated.resources.nested_delete_document
import checkit.shared.generated.resources.nested_documents_empty
import checkit.shared.generated.resources.nested_lists_title
import checkit.shared.generated.resources.nested_new_document
import checkit.shared.generated.resources.nested_untitled_document
import com.checkit.ui.components.TinyTopAppBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NestedListScreen(
    state: NestedUiState,
    viewModel: NestedListsViewModel,
    onAddToDailyPlan: (title: String, tagIds: List<Long>) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val activeEditor = state.editor as? NestedEditorState.Active
    val documentTitle = activeEditor?.tree?.document?.title?.ifBlank { stringResource(Res.string.nested_untitled_document) }
        ?: stringResource(Res.string.nested_lists_title)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.nested_lists_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.documents, key = { it.id }) { document ->
                        val isSelected = activeEditor?.documentId == document.id
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = document.title.ifBlank { stringResource(Res.string.nested_untitled_document) },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.openDocument(document.id)
                            },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                            badge = {
                                IconButton(onClick = { viewModel.requestDeleteDocument(document) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                    label = { Text(stringResource(Res.string.nested_new_document)) },
                    selected = false,
                    onClick = { viewModel.startNewDocument() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp).height(48.dp)
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TinyTopAppBar(
                    title = {
                        Text(
                            text = documentTitle,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        val isEditing = activeEditor != null
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = if (isEditing) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                                contentDescription = if (isEditing) "Back to menu" else "Menu"
                            )
                        }
                    },
                    actions = {
                        if (activeEditor?.selection?.isActive == true) {
                            TextButton(onClick = viewModel::selectAll) { Text("Select all") }
                            TextButton(onClick = viewModel::exitSelectionMode) { Text(stringResource(Res.string.cancel)) }
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                when (val editor = state.editor) {
                    is NestedEditorState.Active -> {
                        NestedListEditorScreen(
                            state = editor,
                            viewModel = viewModel,
                            onAddToDailyPlan = onAddToDailyPlan
                        )
                    }
                    NestedEditorState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is NestedEditorState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(editor.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (state.isListLoading) {
                                CircularProgressIndicator()
                            } else if (state.documents.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.nested_documents_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Select a document from the drawer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showNewDocumentDialog) {
        NewDocumentDialog(
            title = state.newDocumentTitle,
            onTitleChange = viewModel::updateNewDocumentTitle,
            onConfirm = { viewModel.addDocument(state.newDocumentTitle) },
            onDismiss = viewModel::cancelNewDocument
        )
    }

    state.documentDeleting?.let { document ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteDocument,
            title = { Text(stringResource(Res.string.nested_delete_document)) },
            text = { Text(stringResource(Res.string.nested_delete_confirm, document.title)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteDocument(document.id) }) {
                    Text(stringResource(Res.string.nested_delete_document))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteDocument) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun NewDocumentDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nested_new_document)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = title.isNotBlank()) {
                Text(stringResource(Res.string.nested_new_document))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
