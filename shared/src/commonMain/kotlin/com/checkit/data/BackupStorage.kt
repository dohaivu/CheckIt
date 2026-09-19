package com.checkit.data

/** A JSON backup file stored in the app-local backups directory. */
data class BackupFileInfo(
    val name: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
)

/**
 * Platform file storage for JSON backups.
 *
 * Only Android provides an implementation (app-local `backups` directory);
 * other targets have no backup UI wired in.
 */
interface BackupStorage {
    suspend fun listBackups(): List<BackupFileInfo>
    suspend fun createBackup(json: String): BackupFileInfo
    suspend fun readBackup(name: String): String
    suspend fun deleteBackup(name: String): Boolean
}
