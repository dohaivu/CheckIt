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
import com.checkit.domain.CountdownManager
import com.checkit.domain.CountdownState
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

class CountdownService : Service(), KoinComponent {
    private val countdownManager: CountdownManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val CHANNEL_ID = "countdown_timer_channel"
        private const val NOTIFICATION_ID = NotificationIds.CountdownOngoing
        private const val EXTRA_CONTENT = "countdown_content"
        private const val EXTRA_DURATION_SECONDS = "countdown_duration_seconds"

        fun start(context: Context, content: String, durationSeconds: Int) {
            val intent = Intent(context, CountdownService::class.java).apply {
                putExtra(EXTRA_CONTENT, content)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CountdownService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        countdownManager.state
            .distinctUntilChanged { old, new ->
                // The system chronometer renders the ticking; only re-post on
                // identity or deadline/content changes.
                if (old::class != new::class) return@distinctUntilChanged false
                if (old is CountdownState.Running && new is CountdownState.Running) {
                    old.content == new.content && old.endsAtEpochMillis == new.endsAtEpochMillis
                } else {
                    true
                }
            }
            .onEach { state ->
                when (state) {
                    is CountdownState.Running -> {
                        startForeground(NOTIFICATION_ID, createNotification(state))
                    }
                    is CountdownState.Finished, CountdownState.Idle -> {
                        stopForeground(true)
                        stopSelf()
                    }
                }
            }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val content = intent?.getStringExtra(EXTRA_CONTENT).orEmpty()
        val durationSeconds = intent?.getIntExtra(EXTRA_DURATION_SECONDS, 0) ?: 0
        if (content.isNotBlank() && durationSeconds > 0) {
            // Single active timer: a new start replaces the previous one.
            countdownManager.start(content, durationSeconds)
        } else if (countdownManager.state.value !is CountdownState.Running) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotification(state: CountdownState.Running): Notification {
        val intent = Intent().setClassName(packageName, "com.checkit.MainActivity").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.schedule_24px)
            .setContentTitle("Countdown")
            .setContentText(state.content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(state.content))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(state.endsAtEpochMillis)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    null, "Stop",
                    getPendingActionIntent(CountdownActionReceiver.ACTION_STOP)
                ).build()
            )
            .build()
    }

    private fun getPendingActionIntent(action: String): PendingIntent {
        val intent = Intent(this, CountdownActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Countdown Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows active countdown timer"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
