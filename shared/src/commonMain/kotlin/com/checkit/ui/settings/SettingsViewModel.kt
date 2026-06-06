package com.checkit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.CheckItRepository
import com.checkit.domain.AppConfig
import com.checkit.data.SettingsRepository
import com.checkit.data.UserSettings
import com.checkit.notifications.AppReminderScheduler
import com.checkit.ui.AppColorSchemeMode
import com.checkit.ui.AppLanguage
import com.checkit.ui.AppThemeMode
import com.checkit.ui.ReminderSettingsUiState
import com.checkit.ui.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    @Suppress("unused") private val repository: CheckItRepository,
    private val appConfig: AppConfig,
    private val settingsRepository: SettingsRepository,
    private val appReminderScheduler: AppReminderScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    val versionName: String = appConfig.versionName

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { stored ->
                _uiState.update { current ->
                    current.copy(
                        language = AppLanguage.fromCode(stored.languageCode),
                        themeMode = AppThemeMode.fromCode(stored.themeModeCode),
                        colorSchemeMode = AppColorSchemeMode.fromCode(stored.colorSchemeModeCode),
                        reminders = stored.toReminderSettingsUiState(),
                    )
                }
                appReminderScheduler.applySettings(stored)
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
        viewModelScope.launch {
            settingsRepository.setLanguageCode(language.code)
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = themeMode) }
        viewModelScope.launch {
            settingsRepository.setThemeModeCode(themeMode.code)
        }
    }

    fun setColorSchemeMode(colorSchemeMode: AppColorSchemeMode) {
        _uiState.update { it.copy(colorSchemeMode = colorSchemeMode) }
        viewModelScope.launch {
            settingsRepository.setColorSchemeModeCode(colorSchemeMode.code)
        }
    }

    fun setPlanReminderEnabled(enabled: Boolean) {
        _uiState.update { it.copy(reminders = it.reminders.copy(planEnabled = enabled)) }
        viewModelScope.launch {
            settingsRepository.setPlanReminderEnabled(enabled)
        }
    }

    fun setPlanReminderTimeMinutes(minutes: Int) {
        val normalized = minutes.coerceIn(0, MinutesPerDay - 1)
        _uiState.update { it.copy(reminders = it.reminders.copy(planTimeMinutes = normalized)) }
        viewModelScope.launch {
            settingsRepository.setPlanReminderTimeMinutes(normalized)
        }
    }

    fun setReviewReminderEnabled(enabled: Boolean) {
        _uiState.update { it.copy(reminders = it.reminders.copy(reviewEnabled = enabled)) }
        viewModelScope.launch {
            settingsRepository.setReviewReminderEnabled(enabled)
        }
    }

    fun setReviewReminderTimeMinutes(minutes: Int) {
        val normalized = minutes.coerceIn(0, MinutesPerDay - 1)
        _uiState.update { it.copy(reminders = it.reminders.copy(reviewTimeMinutes = normalized)) }
        viewModelScope.launch {
            settingsRepository.setReviewReminderTimeMinutes(normalized)
        }
    }

    fun setCheckInReminderEnabled(enabled: Boolean) {
        _uiState.update { it.copy(reminders = it.reminders.copy(checkInEnabled = enabled)) }
        viewModelScope.launch {
            settingsRepository.setCheckInReminderEnabled(enabled)
        }
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private companion object {
        const val MinutesPerDay = 24 * 60
    }
}

private fun UserSettings.toReminderSettingsUiState() = ReminderSettingsUiState(
    planEnabled = planReminderEnabled,
    planTimeMinutes = planReminderTimeMinutes,
    reviewEnabled = reviewReminderEnabled,
    reviewTimeMinutes = reviewReminderTimeMinutes,
    checkInEnabled = checkInReminderEnabled,
    checkInLastShownAtMillis = checkInReminderLastShownAtMillis
)
