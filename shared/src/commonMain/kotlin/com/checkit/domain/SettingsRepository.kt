package com.checkit.domain

import kotlinx.coroutines.flow.Flow

data class UserSettings(
    val languageCode: String = "en",
    val themeModeCode: String = "system",
    val colorSchemeModeCode: String = "sunset"
)

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setLanguageCode(code: String)
    suspend fun setThemeModeCode(code: String)
    suspend fun setColorSchemeModeCode(code: String)
}
