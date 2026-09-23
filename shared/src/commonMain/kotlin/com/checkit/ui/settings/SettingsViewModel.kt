package com.checkit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.auth.GoogleAccountManager
import com.checkit.data.CheckItRepository
import com.checkit.domain.AppConfig
import com.checkit.domain.CheckInReminderPolicy
import com.checkit.data.SettingsRepository
import com.checkit.data.UserSettings
import com.checkit.notifications.AppReminderScheduler
import com.checkit.platform.BackupScheduler
import com.checkit.ui.AppColorSchemeMode
import com.checkit.ui.AppLanguage
import com.checkit.ui.AppThemeMode
import com.checkit.ui.MinutesPerDay
import com.checkit.ui.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class SettingsViewModel(
    private val repository: CheckItRepository,
    private val appConfig: AppConfig,
    private val settingsRepository: SettingsRepository,
    private val appReminderScheduler: AppReminderScheduler,
    private val accountManager: GoogleAccountManager,
    private val backupScheduler: BackupScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    val versionName: String = appConfig.versionName

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            var lastBackupFolderUri: String? = null
            settingsRepository.settings.collect { stored ->
                _uiState.update { current ->
                    current.copy(
                        language = AppLanguage.fromCode(stored.languageCode),
                        themeMode = AppThemeMode.fromCode(stored.themeModeCode),
                        colorSchemeMode = AppColorSchemeMode.fromCode(stored.colorSchemeModeCode),
                        lastNestedDocumentId = stored.lastNestedDocumentId,
                        reminders = stored.toReminderSettingsUiState(),
                        backupFolderUri = stored.backupFolderUri,
                        backupFolderName = stored.backupFolderName,
                        lastBackupAtMillis = stored.lastBackupAtMillis,
                        checklistFolderUri = stored.checklistFolderUri,
                        checklistFolderName = stored.checklistFolderName,
                    )
                }
                appReminderScheduler.applySettings(stored)
                if (lastBackupFolderUri != stored.backupFolderUri) {
                    lastBackupFolderUri = stored.backupFolderUri
                    if (lastBackupFolderUri != null) {
                        backupScheduler.scheduleDailyBackup()
                    } else {
                        backupScheduler.cancelDailyBackup()
                    }
                }
            }
        }
        viewModelScope.launch {
            accountManager.accountState.collect { account ->
                _uiState.update { it.copy(account = account) }
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

    fun setIdleCheckInThresholdMinutes(minutes: Int) {
        val normalized = minutes.coerceIn(
            CheckInReminderPolicy.MinIdleThresholdMinutes,
            CheckInReminderPolicy.MaxIdleThresholdMinutes
        )
        _uiState.update { it.copy(reminders = it.reminders.copy(idleThresholdMinutes = normalized)) }
        viewModelScope.launch {
            settingsRepository.setIdleCheckInThresholdMinutes(normalized)
        }
    }

    fun setScheduleReminderEnabled(enabled: Boolean) {
        _uiState.update { it.copy(reminders = it.reminders.copy(scheduleEnabled = enabled)) }
        viewModelScope.launch {
            settingsRepository.setScheduleReminderEnabled(enabled)
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch { accountManager.signIn() }
    }

    fun setBackupFolderUri(uri: String?, name: String?) {
        viewModelScope.launch {
            settingsRepository.setBackupFolder(uri, name)
        }
    }

    fun clearBackupFolder() {
        setBackupFolderUri(null, null)
    }

    fun setChecklistFolder(uri: String?, name: String?) {
        viewModelScope.launch {
            settingsRepository.setChecklistFolder(uri, name)
        }
    }

    fun clearChecklistFolder() {
        setChecklistFolder(null, null)
    }

    fun markBackupCompleted() {
        viewModelScope.launch {
            settingsRepository.setLastBackupAtMillis(Clock.System.now().toEpochMilliseconds())
        }
    }

    fun restoreFromBackup(json: String, folderUri: String? = null, folderName: String? = null) {
        viewModelScope.launch {
            runCatching { repository.importBackupJson(json) }
                .onSuccess {
                    if (folderUri != null) {
                        settingsRepository.setBackupFolder(folderUri, folderName)
                    }
                    showMessage("Backup restored")
                }
                .onFailure { error ->
                    showMessage("Restore failed: ${error.message ?: "unknown error"}")
                }
        }
    }

    fun showMessage(message: String) {
        sendEvent(UiEvent.ShowSnackbar(message))
    }

    fun signOut() {
        viewModelScope.launch { accountManager.signOut() }
    }

    fun clearAccountError() {
        accountManager.clearError()
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

private fun UserSettings.toReminderSettingsUiState() = ReminderSettingsUiState(
    planEnabled = planReminderEnabled,
    planTimeMinutes = planReminderTimeMinutes,
    reviewEnabled = reviewReminderEnabled,
    reviewTimeMinutes = reviewReminderTimeMinutes,
    checkInEnabled = checkInReminderEnabled,
    idleThresholdMinutes = idleCheckInThresholdMinutes,
    scheduleEnabled = scheduleReminderEnabled,
    checkInLastShownAtMillis = checkInReminderLastShownAtMillis
)
