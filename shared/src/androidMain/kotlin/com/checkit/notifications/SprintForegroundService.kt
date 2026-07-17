package com.checkit.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.checkit.MainActivity
import com.checkit.domain.SprintManager
import com.checkit.domain.SprintState
import com.checkit.shared.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SprintForegroundService : Service(), KoinComponent {
    private val sprintManager: SprintManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val CHANNEL_ID = "sprint_timer_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, SprintForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SprintForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        sprintManager.state.onEach { state ->
            when (state) {
                is SprintState.Running -> {
                    startForeground(NOTIFICATION_ID, createNotification(state, isPaused = false))
                }
                is SprintState.Paused -> {
                    startForeground(NOTIFICATION_ID, createNotification(state.runningState, isPaused = true))
                }
                is SprintState.Finished -> {
                    stopForeground(true)
                    stopSelf()
                }
                SprintState.Idle -> {
                    stopForeground(true)
                    stopSelf()
                }
            }
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotification(state: SprintState.Running, isPaused: Boolean): Notification {
        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        val timeLabel = String.format("%02d:%02d", minutes, seconds)
        val title = if (isPaused) "Paused: ${state.description}" else "Focusing: ${state.description}"
        
        // We assume MainActivity is in the androidApp module and available via its full name or it will be merged.
        // If shared doesn't know about MainActivity, we might need a different way to start it.
        // But usually shared can reference classes that will be present in the final app.
        val intent = Intent().setClassName(packageName, "com.checkit.MainActivity").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = applicationInfo.icon
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (iconRes != 0) iconRes else R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText("$timeLabel remaining")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sprint Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active focus timer"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
