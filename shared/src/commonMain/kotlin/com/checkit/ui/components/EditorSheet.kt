package com.checkit.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppEditorBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetGesturesEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = LocalSnackbarHostState.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = modifier.fillMaxWidth(),
                content = content
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
internal fun EditorOverflowMenu(
    modifier: Modifier = Modifier,
    contentDescription: String = "Options",
    content: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val onDismiss = { menuExpanded = false }

    Box(modifier = modifier) {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = contentDescription)
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = onDismiss
        ) {
            content(onDismiss)
        }
    }
}

@Composable
internal fun DeleteOverflowMenu(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Options",
    label: String = "Delete"
) {
    EditorOverflowMenu(
        modifier = modifier,
        contentDescription = contentDescription
    ) { onDismiss ->
        DropdownMenuItem(
            text = { androidx.compose.material3.Text(label) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            onClick = {
                onDismiss()
                onDelete()
            }
        )
    }
}
