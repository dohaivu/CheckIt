package com.checkit.ui.settings

import androidx.compose.runtime.Composable

/** No local backup storage on iOS: settings renders without this section. */
@Composable
actual fun BackupSettingsSection() {
}
