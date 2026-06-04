package com.checkit.domain

data class UserSettings(
    val languageCode: String = "en",
    val themeModeCode: String = "system",
    val colorSchemeModeCode: String = "sunset"
)


data class AppConfig(val versionName: String)
