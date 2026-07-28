package com.checkit.notifications

import com.checkit.domain.SprintState

interface SprintNotificationScheduler {
    fun startPersistentNotification(running: SprintState.Running)
    fun updatePersistentNotification(running: SprintState.Running, isPaused: Boolean)
    fun cancelNotification()
    suspend fun showFinishedNotification(finished: SprintState.Finished)
}

class NoOpSprintNotificationScheduler : SprintNotificationScheduler {
    override fun startPersistentNotification(running: SprintState.Running) {}
    override fun updatePersistentNotification(running: SprintState.Running, isPaused: Boolean) {}
    override fun cancelNotification() {}
    override suspend fun showFinishedNotification(finished: SprintState.Finished) {}
}
