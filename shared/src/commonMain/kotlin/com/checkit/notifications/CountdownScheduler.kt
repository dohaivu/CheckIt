package com.checkit.notifications

import com.checkit.domain.CountdownState

interface CountdownScheduler {
    fun startPersistentNotification(running: CountdownState.Running)
    fun updatePersistentNotification(running: CountdownState.Running)
    fun cancelNotification()
    suspend fun showFinishedNotification(finished: CountdownState.Finished)
}

class NoOpCountdownScheduler : CountdownScheduler {
    override fun startPersistentNotification(running: CountdownState.Running) {}
    override fun updatePersistentNotification(running: CountdownState.Running) {}
    override fun cancelNotification() {}
    override suspend fun showFinishedNotification(finished: CountdownState.Finished) {}
}
