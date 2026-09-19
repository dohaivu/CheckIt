package com.checkit.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.usecase.ExportBackupUseCase
import com.checkit.ui.components.AppHorizontalDivider
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Platform backup/restore section, rendered directly inside Settings.
 * Android shows the local JSON backup UI; other targets are a no-op.
 */
@Composable
expect fun BackupSettingsSection()

@Composable
internal fun BackupSettingsContent(
    exportBackup: ExportBackupUseCase,
    backupFolderUri: String? = null,
    backupFolderName: String? = null,
    lastBackupAtMillis: Long? = null,
    onSelectBackupFolder: (() -> Unit)? = null,
    onClearBackupFolder: (() -> Unit)? = null,
    onBackupToFolder: (suspend (json: String) -> String)? = null,
    onBackupRecorded: (() -> Unit)? = null,
    onRestoreFromFolder: (() -> Unit)? = null,
    folderRestore: FolderRestoreCandidate? = null,
    onConfirmFolderRestore: (() -> Unit)? = null,
    onDismissFolderRestore: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Column {
        if (onSelectBackupFolder != null) {
            val lastBackup = lastBackupAtMillis?.let {
                "Last success: ${formatBackupDate(it)}"
            } ?: "Never run"
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Backup folder", fontWeight = FontWeight.SemiBold)
                    Text(
                        backupFolderName?.let { "Syncing to \"$it\"\n$lastBackup" }
                            ?: backupFolderUri?.let { "Syncing to selected folder\n$lastBackup" }
                            ?: "Not set (select a folder to enable backups)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (backupFolderUri != null && onClearBackupFolder != null) {
                    TextButton(onClick = onClearBackupFolder, enabled = !busy) { Text("Clear") }
                }
                TextButton(onClick = onSelectBackupFolder, enabled = !busy) {
                    Text(if (backupFolderUri != null) "Change" else "Select")
                }
            }
            AppHorizontalDivider()
        }
        if (onRestoreFromFolder != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Restore from folder", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pick a folder to restore its newest backup",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = onRestoreFromFolder, enabled = !busy) { Text("Restore") }
            }
            AppHorizontalDivider()
        }
        if (backupFolderUri != null && onBackupToFolder != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Manual backup", fontWeight = FontWeight.SemiBold)
                    Text(
                        status ?: "Save a JSON backup to the folder now",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching {
                                val fileName = onBackupToFolder(exportBackup())
                                onBackupRecorded?.invoke()
                                status = "Saved $fileName to backup folder"
                            }.onFailure { error ->
                                status = "Backup failed: ${error.message ?: "unknown error"}"
                            }
                            busy = false
                        }
                    },
                    enabled = !busy
                ) {
                    Text(if (busy) "Working…" else "Back up now")
                }
            }
            AppHorizontalDivider()
        }
    }

    folderRestore?.let { candidate ->
        AlertDialog(
            onDismissRequest = { onDismissFolderRestore?.invoke() },
            title = { Text("Restore backup?") },
            text = { Text("This replaces all current data with ${candidate.fileName}. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onConfirmFolderRestore?.invoke() }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { onDismissFolderRestore?.invoke() }) { Text("Cancel") }
            }
        )
    }
}

/** A backup JSON read from a user-selected folder, awaiting restore confirmation. */
data class FolderRestoreCandidate(
    val folderUri: String,
    val folderName: String?,
    val fileName: String,
    val json: String,
)

private fun formatBackupDate(millis: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val hour12 = when (val hour = dateTime.hour % 12) {
        0 -> 12
        else -> hour
    }
    val suffix = if (dateTime.hour < 12) "AM" else "PM"
    return "$month ${dateTime.day}, ${dateTime.year} $hour12:${dateTime.minute.toString().padStart(2, '0')} $suffix"
}
