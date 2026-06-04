package com.checkit.infrastructure

import com.checkit.domain.SettingsRepository
import com.checkit.domain.UserSettings
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
}
