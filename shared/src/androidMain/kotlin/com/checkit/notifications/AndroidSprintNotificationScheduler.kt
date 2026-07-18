package com.checkit.notifications

import android.content.Context
import com.checkit.domain.SprintState

class AndroidSprintNotificationScheduler(
    private val context: Context
) : SprintNotificationScheduler {

    override fun startPersistentNotification(running: SprintState.Running) {
        SprintForegroundService.start(context)
    }

    override fun updatePersistentNotification(running: SprintState.Running, isPaused: Boolean) {
        // Foreground service handles its own updates by observing the SprintManager flow
    }

    override fun cancelNotification() {
        SprintForegroundService.stop(context)
    }
}
