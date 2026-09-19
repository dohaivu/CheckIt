package com.checkit.platform

/** Schedules the daily JSON backup to the user-selected folder. Android only; no-op elsewhere. */
interface BackupScheduler {
    fun scheduleDailyBackup()
    fun cancelDailyBackup()
}

class NoOpBackupScheduler : BackupScheduler {
    override fun scheduleDailyBackup() = Unit
    override fun cancelDailyBackup() = Unit
}
