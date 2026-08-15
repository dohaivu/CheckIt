package com.checkit.ui.nested

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.nested_delete_document
import checkit.shared.generated.resources.nested_delete_confirm
import checkit.shared.generated.resources.nested_documents_empty
import checkit.shared.generated.resources.nested_lists_title
import checkit.shared.generated.resources.nested_new_document
import checkit.shared.generated.resources.nested_untitled_document
import com.checkit.domain.NestedDocument
import com.checkit.ui.components.TinyTopAppBar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NestedListScreen(
    state: NestedListsUiState,
    viewModel: NestedListsViewModel,
    onOpenDocument: (Long) -> Unit
) {
    var showNewDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<NestedDocument?>(null) }
    var newTitle by remember { mutableStateOf("") }

    ScaffoldWithFab(
        onAdd = { showNewDialog = true }
    ) { contentModifier ->
        when {
            state.isLoading -> Box(contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.documents.isEmpty() -> Box(contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.nested_documents_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(
                modifier = contentModifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.documents, key = { it.id }) { document ->
                    DocumentCard(
                        document = document,
                        onClick = { onOpenDocument(document.id) },
                        onDelete = { deleting = document }
                    )
                }
            }
        }
    }

    if (showNewDialog) {
        NewDocumentDialog(
            title = newTitle,
            onTitleChange = { newTitle = it },
            onConfirm = {
                viewModel.addDocument(newTitle) { id ->
                    onOpenDocument(id)
                }
                newTitle = ""
                showNewDialog = false
            },
            onDismiss = { showNewDialog = false }
        )
    }

    deleting?.let { document ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(Res.string.nested_delete_document)) },
            text = { Text(stringResource(Res.string.nested_delete_confirm, document.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDocument(document.id)
                    deleting = null
                }) {
                    Text(stringResource(Res.string.nested_delete_document))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ScaffoldWithFab(
    onAdd: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TinyTopAppBar(
            title = { Text(stringResource(Res.string.nested_lists_title), fontWeight = FontWeight.Bold) }
        )
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            content(Modifier)
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.nested_new_document))
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: NestedDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = document.title.ifBlank { stringResource(Res.string.nested_untitled_document) },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.nested_delete_document),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
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