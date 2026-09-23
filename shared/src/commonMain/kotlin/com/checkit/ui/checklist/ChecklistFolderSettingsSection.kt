package com.checkit.ui.checklist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.ui.components.AppHorizontalDivider

/**
 * Settings row for picking the local checklist folder.
 * Android shows the SAF folder picker; other targets are a no-op.
 */
@Composable
expect fun ChecklistFolderSettingsSection()

@Composable
internal fun ChecklistFolderSettingsContent(
    folderUri: String?,
    folderName: String?,
    onSelectFolder: (() -> Unit)? = null,
    onClearFolder: (() -> Unit)? = null
) {
    if (onSelectFolder == null) return
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Checklist folder", fontWeight = FontWeight.SemiBold)
                Text(
                    folderName?.let { "Reading markdown from \"$it\"" }
                        ?: folderUri?.let { "Reading markdown from selected folder" }
                        ?: "Not set (select a folder with markdown files)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (folderUri != null && onClearFolder != null) {
                TextButton(
                    onClick = onClearFolder,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(32.dp)
                ) { Text("Clear") }
            }
            TextButton(
                onClick = onSelectFolder,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(if (folderUri != null) "Change" else "Select")
            }
        }
        AppHorizontalDivider()
    }
}
