package com.checkit.notifications

interface CheckInReminderForceRunner {
    /**
     * Runs the check-in evaluation immediately, bypassing quiet hours and
     * the repeat cooldown. Returns a human-readable outcome for dev UI.
     */
    suspend fun forceRun(): String
}

class NoOpCheckInReminderForceRunner : CheckInReminderForceRunner {
    override suspend fun forceRun(): String = "Check-in reminders are not supported on this platform"
}
