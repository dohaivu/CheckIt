package com.checkit.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.checkit.domain.CountdownManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CountdownActionReceiver : BroadcastReceiver(), KoinComponent {
    private val countdownManager: CountdownManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_STOP = "com.checkit.ACTION_COUNTDOWN_STOP"
        const val ACTION_DISMISS_FINISHED = "com.checkit.ACTION_COUNTDOWN_DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        countdownManager.stop()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_DISMISS_FINISHED -> {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(NotificationIds.CountdownFinished)
                countdownManager.dismissFinished()
            }
        }
    }
}
