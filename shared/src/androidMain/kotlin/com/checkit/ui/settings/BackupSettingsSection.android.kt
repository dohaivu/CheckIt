package com.checkit.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.checkit.data.AndroidBackupStorage
import com.checkit.domain.usecase.ExportBackupUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android backup section: user-selected SAF folder backup/restore
 * (mirroring SpendWise's backup folder / restore rows).
 */
@Composable
actual fun BackupSettingsSection() {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val storage: AndroidBackupStorage = koinInject()
    val exportBackup: ExportBackupUseCase = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var folderRestore by remember { mutableStateOf<FolderRestoreCandidate?>(null) }

    fun persistFolderAccess(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            persistFolderAccess(uri)
            viewModel.setBackupFolderUri(uri.toString(), storage.folderDisplayName(uri.toString()))
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            persistFolderAccess(uri)
            scope.launch {
                val folderUri = uri.toString()
                val backup = runCatching { storage.readBackupFromFolder(folderUri) }.getOrNull()
                if (backup != null) {
                    folderRestore = FolderRestoreCandidate(
                        folderUri = folderUri,
                        folderName = storage.folderDisplayName(folderUri),
                        fileName = backup.first,
                        json = backup.second
                    )
                } else {
                    viewModel.showMessage("No CheckIt backup found in selected folder")
                }
            }
        }
    }

    BackupSettingsContent(
        exportBackup = exportBackup,
        backupFolderUri = state.backupFolderUri,
        backupFolderName = state.backupFolderName,
        lastBackupAtMillis = state.lastBackupAtMillis,
        onSelectBackupFolder = { folderLauncher.launch(null) },
        onClearBackupFolder = viewModel::clearBackupFolder,
        onBackupToFolder = { json ->
            val folderUri = state.backupFolderUri ?: error("No backup folder selected")
            storage.writeToFolder(folderUri, json)
        },
        onBackupRecorded = viewModel::markBackupCompleted,
        onRestoreFromFolder = { restoreLauncher.launch(null) },
        folderRestore = folderRestore,
        onConfirmFolderRestore = {
            val candidate = folderRestore
            folderRestore = null
            if (candidate != null) {
                viewModel.restoreFromBackup(candidate.json, candidate.folderUri, candidate.folderName)
            }
        },
        onDismissFolderRestore = { folderRestore = null }
    )
}
