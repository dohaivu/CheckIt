package com.checkit.notifications

import com.checkit.domain.SprintState

interface SprintNotificationScheduler {
    fun startPersistentNotification(running: SprintState.Running)
    fun updatePersistentNotification(running: SprintState.Running, isPaused: Boolean)
    fun cancelNotification()
    fun showFinishedNotification(description: String, isPomodoro: Boolean)
}

class NoOpSprintNotificationScheduler : SprintNotificationScheduler {
    override fun startPersistentNotification(running: SprintState.Running) {}
    override fun updatePersistentNotification(running: SprintState.Running, isPaused: Boolean) {}
    override fun cancelNotification() {}
    override fun showFinishedNotification(description: String, isPomodoro: Boolean) {}
}
