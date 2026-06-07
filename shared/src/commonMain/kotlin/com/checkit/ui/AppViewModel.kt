package com.checkit.ui

enum class CheckItTab {
    Task,
    MyDay,
    Calendar,
    Report,
    Settings
}

enum class AppLanguage(val label: String) {
    English("English"),
    Vietnamese("Tiếng Việt"),
    Chinese("中文");

    val code: String
        get() = when (this) {
            English -> "en"
            Vietnamese -> "vi"
            Chinese -> "zh"
        }

    companion object {
        fun fromCode(code: String): AppLanguage = when (code) {
            "vi" -> Vietnamese
            "zh" -> Chinese
            else -> English
        }
    }
}

enum class AppThemeMode {
    System,
    Light,
    Dark;

    val code: String
        get() = when (this) {
            System -> "system"
            Light -> "light"
            Dark -> "dark"
        }

    companion object {
        fun fromCode(code: String): AppThemeMode = when (code) {
            "light" -> Light
            "dark" -> Dark
            else -> System
        }
    }
}

enum class AppColorSchemeMode {
    Sunset,
    SkyBlue,
    SystemDefault;

    val code: String
        get() = when (this) {
            Sunset -> "sunset"
            SkyBlue -> "sky_blue"
            SystemDefault -> "system_default"
        }

    companion object {
        fun fromCode(code: String): AppColorSchemeMode = when (code) {
            "sky_blue" -> SkyBlue
            "sunset" -> Sunset
            else -> SystemDefault
        }
    }
}

