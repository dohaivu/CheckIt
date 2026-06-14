package com.checkit.ui

data class ReminderSettingsUiState(
    val planEnabled: Boolean = true,
    val planTimeMinutes: Int = 7 * 60,
    val reviewEnabled: Boolean = true,
    val reviewTimeMinutes: Int = 21 * 60,
    val checkInEnabled: Boolean = true,
    val scheduleEnabled: Boolean = true,
    val checkInLastShownAtMillis: Long? = null,
)

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.English,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val colorSchemeMode: AppColorSchemeMode = AppColorSchemeMode.Sunset,
    val reminders: ReminderSettingsUiState = ReminderSettingsUiState(),
    val tagUsageSort: TagUsageSort = TagUsageSort.MostUsed,
    val message: String? = null
)

enum class TagUsageSort {
    MostUsed,
    HighestSpending,
    RecentlyUsed,
    Alphabetical
}
