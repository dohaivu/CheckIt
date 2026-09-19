package com.checkit.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android-only file storage for JSON backups.
 *
 * Files live under `<filesDir>/backups` (no extra permissions required).
 * Mirrors the SpendWise `BackupWorker` JSON approach, but targets a local
 * app directory instead of a user-picked cloud folder.
 */
class AndroidBackupStorage(
    context: Context,
) : BackupStorage {
    private val backupsDir: File = File(context.filesDir, BACKUPS_DIR_NAME).apply { mkdirs() }

    override suspend fun listBackups(): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        backupsDir.mkdirs()
        backupsDir.listFiles { file -> file.isFile && file.name.endsWith(BACKUP_EXTENSION) }
            .orEmpty()
            .map { file ->
                BackupFileInfo(
                    name = file.name,
                    sizeBytes = file.length(),
                    lastModifiedMillis = file.lastModified(),
                )
            }
            .sortedByDescending { it.lastModifiedMillis }
    }

    override suspend fun createBackup(json: String): BackupFileInfo = withContext(Dispatchers.IO) {
        backupsDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = uniqueBackupFile(timestamp)
        file.writeText(json)
        BackupFileInfo(
            name = file.name,
            sizeBytes = file.length(),
            lastModifiedMillis = file.lastModified(),
        )
    }

    override suspend fun readBackup(name: String): String = withContext(Dispatchers.IO) {
        resolve(name).readText()
    }

    override suspend fun deleteBackup(name: String): Boolean = withContext(Dispatchers.IO) {
        resolve(name).delete()
    }

    private fun uniqueBackupFile(timestamp: String): File {
        var candidate = File(backupsDir, "$BACKUP_PREFIX$timestamp$BACKUP_EXTENSION")
        var attempt = 1
        while (candidate.exists()) {
            candidate = File(backupsDir, "$BACKUP_PREFIX${timestamp}_$attempt$BACKUP_EXTENSION")
            attempt += 1
        }
        return candidate
    }

    private fun resolve(name: String): File {
        require(name.isNotBlank()) { "Backup name must not be blank" }
        require(name.endsWith(BACKUP_EXTENSION)) { "Not a backup file: $name" }
        require(name.all { it.isLetterOrDigit() || it in "._-" }) { "Invalid backup name: $name" }
        require(!name.contains("..")) { "Invalid backup name: $name" }
        val file = File(backupsDir, name)
        require(file.canonicalPath.startsWith(backupsDir.canonicalPath)) { "Invalid backup name: $name" }
        require(file.exists()) { "Backup not found: $name" }
        return file
    }

    companion object {
        const val BACKUPS_DIR_NAME = "backups"
        const val BACKUP_PREFIX = "CheckIt_Backup_"
        const val BACKUP_EXTENSION = ".json"
    }
}
