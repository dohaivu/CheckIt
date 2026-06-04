package com.checkit.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
internal fun AppLocaleProvider(
    languageCode: String,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppLocale provides languageCode) {
        key(languageCode) {
            content()
        }
    }
}
