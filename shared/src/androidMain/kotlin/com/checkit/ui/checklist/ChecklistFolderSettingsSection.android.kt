package com.checkit.ui.checklist

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.checkit.data.AndroidBackupStorage
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.checkit.ui.settings.SettingsViewModel

/** Android checklist folder section: SAF folder picker, mirroring the backup folder row. */
@Composable
actual fun ChecklistFolderSettingsSection() {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val storage: AndroidBackupStorage = koinInject()
    val context = LocalContext.current

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setChecklistFolder(uri.toString(), storage.folderDisplayName(uri.toString()))
        }
    }

    ChecklistFolderSettingsContent(
        folderUri = state.checklistFolderUri,
        folderName = state.checklistFolderName,
        onSelectFolder = { folderLauncher.launch(null) },
        onClearFolder = viewModel::clearChecklistFolder
    )
}
