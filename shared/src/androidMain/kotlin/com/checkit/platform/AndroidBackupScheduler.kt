package com.checkit.platform

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AndroidBackupScheduler(
    private val context: Context
) : BackupScheduler {

    override fun scheduleDailyBackup() {
        Log.d(TAG, "Scheduling daily backup...")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .addTag(BACKUP_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        Log.d(TAG, "Daily backup scheduled")
    }

    override fun cancelDailyBackup() {
        Log.d(TAG, "Canceling daily backup...")
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME)
    }

    companion object {
        private const val TAG = "BackupScheduler"
        private const val BACKUP_WORK_TAG = "checkit_backup"
        private const val BACKUP_WORK_NAME = "checkit_daily_backup"
    }
}
