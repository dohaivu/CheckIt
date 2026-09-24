package com.checkit.ui.tasks

import com.checkit.data.SettingsRepository
import com.checkit.data.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initialSettings: UserSettings = UserSettings()) : SettingsRepository {
    private val settingsFlow = MutableStateFlow(initialSettings)
    override val settings: Flow<UserSettings> = settingsFlow

    fun currentSettings() = settingsFlow.value

    override suspend fun setLanguageCode(code: String) {
        settingsFlow.update { it.copy(languageCode = code) }
    }

    override suspend fun setThemeModeCode(code: String) {
        settingsFlow.update { it.copy(themeModeCode = code) }
    }

    override suspend fun setColorSchemeModeCode(code: String) {
        settingsFlow.update { it.copy(colorSchemeModeCode = code) }
    }

    override suspend fun setTaskWorkspaceViewCode(code: String) {
        settingsFlow.update { it.copy(taskWorkspaceViewCode = code) }
    }

    override suspend fun setTaskListDisplayTypeCode(code: String) {
        settingsFlow.update { it.copy(taskListDisplayTypeCode = code) }
    }

    override suspend fun setTaskShowCompleted(showCompleted: Boolean) {
        settingsFlow.update { it.copy(taskShowCompleted = showCompleted) }
    }

    override suspend fun setTaskSortOptionCode(code: String) {
        settingsFlow.update { it.copy(taskSortOptionCode = code) }
    }

    override suspend fun setPlanReminderEnabled(enabled: Boolean) {
        settingsFlow.update { it.copy(planReminderEnabled = enabled) }
    }

    override suspend fun setPlanReminderTimeMinutes(minutes: Int) {
        settingsFlow.update { it.copy(planReminderTimeMinutes = minutes) }
    }

    override suspend fun setReviewReminderEnabled(enabled: Boolean) {
        settingsFlow.update { it.copy(reviewReminderEnabled = enabled) }
    }

    override suspend fun setReviewReminderTimeMinutes(minutes: Int) {
        settingsFlow.update { it.copy(reviewReminderTimeMinutes = minutes) }
    }

    override suspend fun setCheckInReminderEnabled(enabled: Boolean) {
        settingsFlow.update { it.copy(checkInReminderEnabled = enabled) }
    }

    override suspend fun setIdleCheckInThresholdMinutes(minutes: Int) {
        settingsFlow.update { it.copy(idleCheckInThresholdMinutes = minutes) }
    }

    override suspend fun setScheduleReminderEnabled(enabled: Boolean) {
        settingsFlow.update { it.copy(scheduleReminderEnabled = enabled) }
    }

    override suspend fun setCheckInReminderLastShownAtMillis(millis: Long) {
        settingsFlow.update { it.copy(checkInReminderLastShownAtMillis = millis) }
    }

    override suspend fun setAutoMyDayLastRunEpochDay(epochDay: Int) {
        settingsFlow.update { it.copy(autoMyDayLastRunEpochDay = epochDay) }
    }

    override suspend fun setLastFabAction(type: String, id: String?) {
        settingsFlow.update { it.copy(lastFabActionType = type, lastFabActionId = id) }
    }

    override suspend fun setLastNestedDocumentId(id: String?) {
        settingsFlow.update { it.copy(lastNestedDocumentId = id) }
    }

    override suspend fun setLastSelectedListId(id: String?) {
        settingsFlow.update { it.copy(lastSelectedListId = id) }
    }

    override suspend fun addRecentLabel(label: String) {
        settingsFlow.update { it.copy(recentLabels = (listOf(label) + it.recentLabels).distinct().take(15)) }
    }

    override suspend fun setBackupFolder(uri: String?, name: String?) {
        settingsFlow.update { it.copy(backupFolderUri = uri, backupFolderName = name) }
    }

    override suspend fun setLastBackupAtMillis(millis: Long) {
        settingsFlow.update { it.copy(lastBackupAtMillis = millis) }
    }

    override suspend fun setGuidelinesFolder(uri: String?, name: String?) {
        settingsFlow.update { it.copy(guidelinesFolderUri = uri, guidelinesFolderName = name) }
    }
}
