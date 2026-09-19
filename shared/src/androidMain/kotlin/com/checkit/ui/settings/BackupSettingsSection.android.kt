package com.checkit.ui.settings

import androidx.compose.runtime.Composable
import com.checkit.data.BackupStorage
import com.checkit.domain.usecase.ExportBackupUseCase
import com.checkit.domain.usecase.ImportBackupUseCase
import org.koin.compose.koinInject

/** Android binds [BackupStorage] to the app-local backups directory. */
@Composable
actual fun BackupSettingsSection() {
    BackupSettingsContent(
        storage = koinInject(),
        exportBackup = koinInject(),
        importBackup = koinInject(),
    )
}
