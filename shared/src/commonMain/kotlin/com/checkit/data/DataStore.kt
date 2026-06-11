package com.checkit.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

class AppDataStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<UserSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            UserSettings(
                languageCode = prefs[KEY_LANGUAGE] ?: UserSettings().languageCode,
                themeModeCode = prefs[KEY_THEME_MODE] ?: UserSettings().themeModeCode,
                colorSchemeModeCode = prefs[KEY_COLOR_SCHEME] ?: UserSettings().colorSchemeModeCode,
                taskWorkspaceViewCode = prefs[KEY_TASK_WORKSPACE_VIEW] ?: UserSettings().taskWorkspaceViewCode,
                taskListDisplayTypeCode = prefs[KEY_TASK_LIST_DISPLAY_TYPE] ?: UserSettings().taskListDisplayTypeCode,
                taskShowCompleted = prefs[KEY_TASK_SHOW_COMPLETED] ?: UserSettings().taskShowCompleted,
                taskSortOptionCode = prefs[KEY_TASK_SORT_OPTION] ?: UserSettings().taskSortOptionCode,
                planReminderEnabled = prefs[KEY_PLAN_REMINDER_ENABLED] ?: UserSettings().planReminderEnabled,
                planReminderTimeMinutes = prefs[KEY_PLAN_REMINDER_TIME] ?: UserSettings().planReminderTimeMinutes,
                reviewReminderEnabled = prefs[KEY_REVIEW_REMINDER_ENABLED] ?: UserSettings().reviewReminderEnabled,
                reviewReminderTimeMinutes = prefs[KEY_REVIEW_REMINDER_TIME] ?: UserSettings().reviewReminderTimeMinutes,
                checkInReminderEnabled = prefs[KEY_CHECK_IN_REMINDER_ENABLED] ?: UserSettings().checkInReminderEnabled,
                checkInReminderLastShownAtMillis = prefs[KEY_CHECK_IN_REMINDER_LAST_SHOWN],
            )
        }

    suspend fun setLanguageCode(code: String) {
        dataStore.edit { it[KEY_LANGUAGE] = code }
    }

    suspend fun setThemeModeCode(code: String) {
        dataStore.edit { it[KEY_THEME_MODE] = code }
    }

    suspend fun setColorSchemeModeCode(code: String) {
        dataStore.edit { it[KEY_COLOR_SCHEME] = code }
    }

    suspend fun setTaskWorkspaceViewCode(code: String) {
        dataStore.edit { it[KEY_TASK_WORKSPACE_VIEW] = code }
    }

    suspend fun setTaskListDisplayTypeCode(code: String) {
        dataStore.edit { it[KEY_TASK_LIST_DISPLAY_TYPE] = code }
    }

    suspend fun setTaskShowCompleted(showCompleted: Boolean) {
        dataStore.edit { it[KEY_TASK_SHOW_COMPLETED] = showCompleted }
    }

    suspend fun setTaskSortOptionCode(code: String) {
        dataStore.edit { it[KEY_TASK_SORT_OPTION] = code }
    }

    suspend fun setPlanReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAN_REMINDER_ENABLED] = enabled }
    }

    suspend fun setPlanReminderTimeMinutes(minutes: Int) {
        dataStore.edit { it[KEY_PLAN_REMINDER_TIME] = minutes.coerceIn(0, MinutesPerDay - 1) }
    }

    suspend fun setReviewReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_REVIEW_REMINDER_ENABLED] = enabled }
    }

    suspend fun setReviewReminderTimeMinutes(minutes: Int) {
        dataStore.edit { it[KEY_REVIEW_REMINDER_TIME] = minutes.coerceIn(0, MinutesPerDay - 1) }
    }

    suspend fun setCheckInReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_CHECK_IN_REMINDER_ENABLED] = enabled }
    }

    suspend fun setCheckInReminderLastShownAtMillis(millis: Long) {
        dataStore.edit { it[KEY_CHECK_IN_REMINDER_LAST_SHOWN] = millis }
    }

    private companion object {
        const val MinutesPerDay = 24 * 60
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_COLOR_SCHEME = stringPreferencesKey("color_scheme_mode")
        val KEY_TASK_WORKSPACE_VIEW = stringPreferencesKey("task_workspace_view")
        val KEY_TASK_LIST_DISPLAY_TYPE = stringPreferencesKey("task_list_display_type")
        val KEY_TASK_SHOW_COMPLETED = booleanPreferencesKey("task_show_completed")
        val KEY_TASK_SORT_OPTION = stringPreferencesKey("task_sort_option")
        val KEY_PLAN_REMINDER_ENABLED = booleanPreferencesKey("plan_reminder_enabled")
        val KEY_PLAN_REMINDER_TIME = intPreferencesKey("plan_reminder_time_minutes")
        val KEY_REVIEW_REMINDER_ENABLED = booleanPreferencesKey("review_reminder_enabled")
        val KEY_REVIEW_REMINDER_TIME = intPreferencesKey("review_reminder_time_minutes")
        val KEY_CHECK_IN_REMINDER_ENABLED = booleanPreferencesKey("check_in_reminder_enabled")
        val KEY_CHECK_IN_REMINDER_LAST_SHOWN = longPreferencesKey("check_in_reminder_last_shown_at_millis")
    }
}

internal const val dataStoreFileName = "settings.preferences_pb"

fun getPreferencesDataStore(path: String) = PreferenceDataStoreFactory.createWithPath {
    path.toPath()
}

expect fun createPreferencesDataStore(): DataStore<Preferences>
