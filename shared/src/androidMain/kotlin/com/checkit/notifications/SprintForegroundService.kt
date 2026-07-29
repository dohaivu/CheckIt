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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

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
        
        sprintManager.state
            .distinctUntilChanged { old, new ->
                // Only update notification if meaningful visual properties change.
                // We ignore 'remainingSeconds' because the system chronometer handles it.
                if (old::class != new::class) return@distinctUntilChanged false
                when {
                    old is SprintState.Running && new is SprintState.Running -> {
                        old.description == new.description && 
                        old.endsAtEpochMillis == new.endsAtEpochMillis &&
                        old.isPaused() == new.isPaused() &&
                        old.isBreak == new.isBreak
                    }
                    old is SprintState.Paused && new is SprintState.Paused -> {
                        old.runningState.description == new.runningState.description &&
                        old.remainingSecondsAtPause == new.remainingSecondsAtPause
                    }
                    else -> true
                }
            }
            .onEach { state ->
                when (state) {
                    is SprintState.Running -> {
                        startForeground(NOTIFICATION_ID, createNotification(state, isPaused = false))
                    }
                    is SprintState.Paused -> {
                        startForeground(NOTIFICATION_ID, createNotification(state.runningState, isPaused = true))
                    }
                    is SprintState.Finished, SprintState.Idle -> {
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
        val headline = when {
            isPaused -> "Paused"
            state.isBreak -> "Short Break"
            else -> "Focusing"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon.takeIf { it != 0 } ?: R.mipmap.ic_launcher_round)
            .setContentTitle(headline)
            .setContentText(state.description)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (isPaused) {
            // Static text for frozen time when paused
            val minutes = state.remainingSeconds / 60
            val seconds = state.remainingSeconds % 60
            builder.setSubText(String.format(Locale.US, "%02d:%02d remaining", minutes, seconds))
            builder.setUsesChronometer(false)
        } else {
            // Live countdown using system Chronometer for best performance
            builder.setSubText(if (state.isBreak) "Time for a breather" else "In progress")
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setWhen(state.endsAtEpochMillis)
        }
        
        val intent = Intent().setClassName(packageName, "com.checkit.MainActivity").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        builder.setContentIntent(PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))

        return builder.build()
    }

    private fun SprintState.isPaused() = this is SprintState.Paused

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
