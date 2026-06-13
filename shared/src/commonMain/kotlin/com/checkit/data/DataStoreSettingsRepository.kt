package com.checkit.data

import kotlinx.coroutines.flow.Flow

class DataStoreSettingsRepository(
    private val dataStore: AppDataStore,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.settings

    override suspend fun setLanguageCode(code: String) {
        dataStore.setLanguageCode(code)
    }

    override suspend fun setThemeModeCode(code: String) {
        dataStore.setThemeModeCode(code)
    }

    override suspend fun setColorSchemeModeCode(code: String) {
        dataStore.setColorSchemeModeCode(code)
    }

    override suspend fun setTaskWorkspaceViewCode(code: String) {
        dataStore.setTaskWorkspaceViewCode(code)
    }

    override suspend fun setTaskListDisplayTypeCode(code: String) {
        dataStore.setTaskListDisplayTypeCode(code)
    }

    override suspend fun setTaskShowCompleted(showCompleted: Boolean) {
        dataStore.setTaskShowCompleted(showCompleted)
    }

    override suspend fun setTaskSortOptionCode(code: String) {
        dataStore.setTaskSortOptionCode(code)
    }

    override suspend fun setPlanReminderEnabled(enabled: Boolean) {
        dataStore.setPlanReminderEnabled(enabled)
    }

    override suspend fun setPlanReminderTimeMinutes(minutes: Int) {
        dataStore.setPlanReminderTimeMinutes(minutes)
    }

    override suspend fun setReviewReminderEnabled(enabled: Boolean) {
        dataStore.setReviewReminderEnabled(enabled)
    }

    override suspend fun setReviewReminderTimeMinutes(minutes: Int) {
        dataStore.setReviewReminderTimeMinutes(minutes)
    }

    override suspend fun setCheckInReminderEnabled(enabled: Boolean) {
        dataStore.setCheckInReminderEnabled(enabled)
    }

    override suspend fun setScheduleReminderEnabled(enabled: Boolean) {
        dataStore.setScheduleReminderEnabled(enabled)
    }

    override suspend fun setCheckInReminderLastShownAtMillis(millis: Long) {
        dataStore.setCheckInReminderLastShownAtMillis(millis)
    }
}
