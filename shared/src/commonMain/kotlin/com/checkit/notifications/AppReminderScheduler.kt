package com.checkit.notifications

import com.checkit.data.UserSettings

interface AppReminderScheduler {
    suspend fun applySettings(settings: UserSettings)
}

class NoOpAppReminderScheduler : AppReminderScheduler {
    override suspend fun applySettings(settings: UserSettings) = Unit
}
