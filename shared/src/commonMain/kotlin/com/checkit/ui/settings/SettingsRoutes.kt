package com.checkit.ui.settings

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

internal sealed interface SettingsRoute : NavKey {
    @Serializable
    data object Home : SettingsRoute

    @Serializable
    data object Reminders : SettingsRoute
}
