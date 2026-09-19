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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.data.BackupFileInfo
import com.checkit.data.BackupStorage
import com.checkit.domain.usecase.ExportBackupUseCase
import com.checkit.domain.usecase.ImportBackupUseCase
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
    storage: BackupStorage,
    exportBackup: ExportBackupUseCase,
    importBackup: ImportBackupUseCase,
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
    var backups by remember { mutableStateOf<List<BackupFileInfo>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<BackupFileInfo?>(null) }
    var pendingDelete by remember { mutableStateOf<BackupFileInfo?>(null) }

    fun refresh() {
        scope.launch {
            runCatching { storage.listBackups() }
                .onSuccess { backups = it }
                .onFailure { status = "Could not list backups: ${it.message ?: "unknown error"}" }
        }
    }

    LaunchedEffect(Unit) { refresh() }

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
                            ?: "Not set (backups stay in app storage)",
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Local backup", fontWeight = FontWeight.SemiBold)
                Text(
                    status ?: if (backups.isEmpty()) {
                        "No backups yet — JSON files stay in this app's folder"
                    } else {
                        "${backups.size} backup${if (backups.size == 1) "" else "s"} in app storage"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(
                onClick = {
                    scope.launch {
                        busy = true
                        runCatching {
                            val json = exportBackup()
                            if (backupFolderUri != null && onBackupToFolder != null) {
                                val fileName = onBackupToFolder(json)
                                onBackupRecorded?.invoke()
                                status = "Saved $fileName to backup folder"
                            } else {
                                val created = storage.createBackup(json)
                                status = "Saved ${created.name}"
                                refresh()
                            }
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
        backups.forEach { backup ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(backup.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "${formatBackupDate(backup.lastModifiedMillis)} • ${formatBackupSize(backup.sizeBytes)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(
                    onClick = { pendingRestore = backup },
                    enabled = !busy
                ) { Text("Restore") }
                TextButton(
                    onClick = { pendingDelete = backup },
                    enabled = !busy
                ) { Text("Delete") }
            }
        }
        AppHorizontalDivider()
    }

    pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup?") },
            text = { Text("This replaces all current data with ${backup.name}. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        busy = true
                        runCatching {
                            val json = storage.readBackup(backup.name)
                            importBackup(json)
                        }.onSuccess {
                            status = "Restored ${backup.name}"
                        }.onFailure { error ->
                            status = "Restore failed: ${error.message ?: "unknown error"}"
                        }
                        busy = false
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Cancel") }
            }
        )
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

    pendingDelete?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete backup?") },
            text = { Text("Delete ${backup.name}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch {
                        runCatching { storage.deleteBackup(backup.name) }
                            .onSuccess { refresh() }
                            .onFailure { error ->
                                status = "Delete failed: ${error.message ?: "unknown error"}"
                            }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
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

private fun formatBackupSize(bytes: Long): String = when {
    bytes < 1_024L -> "$bytes B"
    bytes < 1_024L * 1_024L -> "${bytes / 1_024L} KB"
    else -> {
        val tenths = bytes * 10 / (1_024L * 1_024L)
        "${tenths / 10}.${tenths % 10} MB"
    }
}
