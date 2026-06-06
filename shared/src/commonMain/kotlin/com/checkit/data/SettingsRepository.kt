package com.checkit.data

import kotlinx.coroutines.flow.Flow

data class UserSettings(
    val languageCode: String = "en",
    val themeModeCode: String = "system",
    val colorSchemeModeCode: String = "sunset",
    val planReminderEnabled: Boolean = true,
    val planReminderTimeMinutes: Int = 7 * 60,
    val reviewReminderEnabled: Boolean = true,
    val reviewReminderTimeMinutes: Int = 21 * 60,
    val checkInReminderEnabled: Boolean = true,
    val checkInReminderLastShownAtMillis: Long? = null
)

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setLanguageCode(code: String)
    suspend fun setThemeModeCode(code: String)
    suspend fun setColorSchemeModeCode(code: String)
    suspend fun setPlanReminderEnabled(enabled: Boolean)
    suspend fun setPlanReminderTimeMinutes(minutes: Int)
    suspend fun setReviewReminderEnabled(enabled: Boolean)
    suspend fun setReviewReminderTimeMinutes(minutes: Int)
    suspend fun setCheckInReminderEnabled(enabled: Boolean)
    suspend fun setCheckInReminderLastShownAtMillis(millis: Long)
}
