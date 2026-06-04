package com.checkit.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.checkit.infrastructure.AndroidContextProvider

fun getPreferencesDataStorePath(appContext: Context): String =
    appContext.filesDir.resolve(dataStoreFileName).absolutePath

actual fun createPreferencesDataStore(): DataStore<Preferences> {
    val path = getPreferencesDataStorePath(AndroidContextProvider.context)
    return getPreferencesDataStore(path)
}
