package com.checkit.platform

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.checkit.data.AndroidBackupStorage
import com.checkit.data.CheckItRepository
import com.checkit.data.SettingsRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Daily JSON backup to the user-selected folder, mirroring SpendWise's
 * BackupWorker. Skips when the last successful backup is under 23h old.
 */
class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: CheckItRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val storage: AndroidBackupStorage by inject()

    override suspend fun doWork(): Result {
        Log.d(TAG, "BackupWorker starting...")
        val settings = settingsRepository.settings.first()
        val backupFolderUri = settings.backupFolderUri

        if (backupFolderUri == null) {
            Log.w(TAG, "Backup failed: No backup folder selected")
            return Result.failure()
        }

        val lastBackup = settings.lastBackupAtMillis ?: 0L
        val now = System.currentTimeMillis()

        // Skip if last backup was successful within the last 23 hours to avoid redundancy
        // (using 23h instead of 24h to allow for small scheduling drifts)
        if (settings.lastBackupAtMillis != null && (now - lastBackup) < 23 * 60 * 60 * 1000L) {
            Log.d(TAG, "Backup skipped: Last backup was less than 23h ago")
            return Result.success()
        }

        return try {
            Log.d(TAG, "Generating backup JSON...")
            val json = repository.exportBackupJson()
            val fileName = storage.writeToFolder(backupFolderUri, json)

            settingsRepository.setLastBackupAtMillis(System.currentTimeMillis())
            Log.i(TAG, "Backup successful: $fileName")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed with exception", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "BackupWorker"
    }
}
