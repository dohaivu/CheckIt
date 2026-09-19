package com.checkit.ui.settings

import android.content.Intent
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
import com.checkit.domain.usecase.ImportBackupUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android backup section: local app-storage backups plus a user-selected
 * SAF folder (mirroring SpendWise's backup folder / restore rows).
 */
@Composable
actual fun BackupSettingsSection() {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val storage: AndroidBackupStorage = koinInject()
    val exportBackup: ExportBackupUseCase = koinInject()
    val importBackup: ImportBackupUseCase = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var folderRestore by remember { mutableStateOf<FolderRestoreCandidate?>(null) }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setBackupFolderUri(uri.toString(), storage.folderDisplayName(uri.toString()))
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scope.launch {
                val folderUri = uri.toString()
                val newest = runCatching { storage.readNewestFromFolder(folderUri) }.getOrNull()
                if (newest != null) {
                    folderRestore = FolderRestoreCandidate(
                        folderUri = folderUri,
                        folderName = storage.folderDisplayName(folderUri),
                        fileName = newest.first,
                        json = newest.second
                    )
                } else {
                    viewModel.showMessage("No CheckIt backup found in selected folder")
                }
            }
        }
    }

    BackupSettingsContent(
        storage = storage,
        exportBackup = exportBackup,
        importBackup = importBackup,
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
                scope.launch {
                    runCatching { importBackup(candidate.json) }
                        .onSuccess {
                            viewModel.setBackupFolderUri(candidate.folderUri, candidate.folderName)
                            viewModel.showMessage("Restored ${candidate.fileName}")
                        }
                        .onFailure { error ->
                            viewModel.showMessage("Restore failed: ${error.message ?: "unknown error"}")
                        }
                }
            }
        },
        onDismissFolderRestore = { folderRestore = null }
    )
}
