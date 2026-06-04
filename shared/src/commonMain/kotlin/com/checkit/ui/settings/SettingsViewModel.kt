package com.checkit.ui.settings

import androidx.lifecycle.ViewModel
import com.checkit.data.CheckItRepository
import com.checkit.domain.AppConfig
import com.checkit.ui.AppColorSchemeMode
import com.checkit.ui.AppLanguage
import com.checkit.ui.AppThemeMode
import com.checkit.ui.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    private val repository: CheckItRepository,
    private val appConfig: AppConfig,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    val versionName: String = appConfig.versionName


    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
        persistSettings()
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = themeMode) }
        persistSettings()
    }

    fun setColorSchemeMode(colorSchemeMode: AppColorSchemeMode) {
        _uiState.update { it.copy(colorSchemeMode = colorSchemeMode) }
        persistSettings()
    }



    fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }



    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun persistSettings() {
        val state = _uiState.value
    }
}
