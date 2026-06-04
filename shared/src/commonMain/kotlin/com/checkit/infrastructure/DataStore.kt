package com.checkit.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.checkit.domain.UserSettings
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

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_COLOR_SCHEME = stringPreferencesKey("color_scheme_mode")
    }
}

internal const val dataStoreFileName = "settings.preferences_pb"

fun getPreferencesDataStore(path: String) = PreferenceDataStoreFactory.createWithPath {
    path.toPath()
}

expect fun createPreferencesDataStore(): DataStore<Preferences>
