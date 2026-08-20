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
import com.checkit.ui.MinutesPerDay
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
                scheduleReminderEnabled = prefs[KEY_SCHEDULE_REMINDER_ENABLED] ?: UserSettings().scheduleReminderEnabled,
                checkInReminderLastShownAtMillis = prefs[KEY_CHECK_IN_REMINDER_LAST_SHOWN],
                autoMyDayLastRunEpochDay = prefs[KEY_AUTO_MY_DAY_LAST_RUN_EPOCH_DAY],
                lastDayCloseEpochDay = prefs[KEY_LAST_DAY_CLOSE_EPOCH_DAY],
                autoCarryOverLeftovers = prefs[KEY_AUTO_CARRY_OVER_LEFTOVERS]
                    ?: UserSettings().autoCarryOverLeftovers,
                autoCarryOverLastRunEpochDay = prefs[KEY_AUTO_CARRY_OVER_LAST_RUN_EPOCH_DAY],
                leftoversBannerDismissedEpochDay = prefs[KEY_LEFTOVERS_BANNER_DISMISSED_EPOCH_DAY],
                lastDayPlanDismissedEpochDay = prefs[KEY_LAST_DAY_PLAN_DISMISSED_EPOCH_DAY],
                lastFabActionType = prefs[KEY_LAST_FAB_ACTION_TYPE] ?: UserSettings().lastFabActionType,
                lastFabActionId = prefs[KEY_LAST_FAB_ACTION_ID],
                lastNestedDocumentId = prefs[KEY_LAST_NESTED_DOCUMENT_ID],
                recentLabels = prefs[KEY_RECENT_LABELS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
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

    suspend fun setScheduleReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SCHEDULE_REMINDER_ENABLED] = enabled }
    }

    suspend fun setCheckInReminderLastShownAtMillis(millis: Long) {
        dataStore.edit { it[KEY_CHECK_IN_REMINDER_LAST_SHOWN] = millis }
    }

    suspend fun setAutoMyDayLastRunEpochDay(epochDay: Int) {
        dataStore.edit { it[KEY_AUTO_MY_DAY_LAST_RUN_EPOCH_DAY] = epochDay }
    }

    suspend fun setLastDayCloseEpochDay(epochDay: Int) {
        dataStore.edit { it[KEY_LAST_DAY_CLOSE_EPOCH_DAY] = epochDay }
    }

    suspend fun setAutoCarryOverLeftovers(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_CARRY_OVER_LEFTOVERS] = enabled }
    }

    suspend fun setAutoCarryOverLastRunEpochDay(epochDay: Int) {
        dataStore.edit { it[KEY_AUTO_CARRY_OVER_LAST_RUN_EPOCH_DAY] = epochDay }
    }

    suspend fun setLeftoversBannerDismissedEpochDay(epochDay: Int) {
        dataStore.edit { it[KEY_LEFTOVERS_BANNER_DISMISSED_EPOCH_DAY] = epochDay }
    }

    suspend fun setLastDayPlanDismissedEpochDay(epochDay: Int) {
        dataStore.edit { it[KEY_LAST_DAY_PLAN_DISMISSED_EPOCH_DAY] = epochDay }
    }

    suspend fun setLastFabAction(type: String, id: Long?) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_FAB_ACTION_TYPE] = type
            if (id != null) {
                prefs[KEY_LAST_FAB_ACTION_ID] = id
            } else {
                prefs.remove(KEY_LAST_FAB_ACTION_ID)
            }
        }
    }

    suspend fun setLastNestedDocumentId(id: Long?) {
        dataStore.edit { prefs ->
            if (id != null) {
                prefs[KEY_LAST_NESTED_DOCUMENT_ID] = id
            } else {
                prefs.remove(KEY_LAST_NESTED_DOCUMENT_ID)
            }
        }
    }

    suspend fun addRecentLabel(label: String) {
        if (label.isBlank()) return
        dataStore.edit { prefs ->
            val current = prefs[KEY_RECENT_LABELS]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            current.remove(label)
            current.add(0, label)
            prefs[KEY_RECENT_LABELS] = current.take(15).joinToString(",")
        }
    }

    private companion object {
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
        val KEY_SCHEDULE_REMINDER_ENABLED = booleanPreferencesKey("schedule_reminder_enabled")
        val KEY_CHECK_IN_REMINDER_LAST_SHOWN = longPreferencesKey("check_in_reminder_last_shown_at_millis")
        val KEY_AUTO_MY_DAY_LAST_RUN_EPOCH_DAY = intPreferencesKey("auto_my_day_last_run_epoch_day")
        val KEY_LAST_DAY_CLOSE_EPOCH_DAY = intPreferencesKey("last_day_close_epoch_day")
        val KEY_AUTO_CARRY_OVER_LEFTOVERS = booleanPreferencesKey("auto_carry_over_leftovers")
        val KEY_AUTO_CARRY_OVER_LAST_RUN_EPOCH_DAY = intPreferencesKey("auto_carry_over_last_run_epoch_day")
        val KEY_LEFTOVERS_BANNER_DISMISSED_EPOCH_DAY = intPreferencesKey("leftovers_banner_dismissed_epoch_day")
        val KEY_LAST_DAY_PLAN_DISMISSED_EPOCH_DAY = intPreferencesKey("last_day_plan_dismissed_epoch_day")
        val KEY_LAST_FAB_ACTION_TYPE = stringPreferencesKey("last_fab_action_type")
        val KEY_LAST_FAB_ACTION_ID = longPreferencesKey("last_fab_action_id")
        val KEY_LAST_NESTED_DOCUMENT_ID = longPreferencesKey("last_nested_document_id")
        val KEY_RECENT_LABELS = stringPreferencesKey("recent_labels")
    }
}

internal const val dataStoreFileName = "settings.preferences_pb"

fun getPreferencesDataStore(path: String) = PreferenceDataStoreFactory.createWithPath {
    path.toPath()
}

expect fun createPreferencesDataStore(): DataStore<Preferences>
