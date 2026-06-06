package com.checkit.ui.settings

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.color_scheme
import checkit.shared.generated.resources.color_scheme_sky_blue
import checkit.shared.generated.resources.color_scheme_sunset
import checkit.shared.generated.resources.color_scheme_system_default
import checkit.shared.generated.resources.language
import checkit.shared.generated.resources.settings_title
import checkit.shared.generated.resources.theme
import checkit.shared.generated.resources.theme_dark
import checkit.shared.generated.resources.theme_light
import checkit.shared.generated.resources.theme_system
import checkit.shared.generated.resources.version
import com.checkit.ui.AppColorSchemeMode
import com.checkit.ui.AppLanguage
import com.checkit.ui.AppThemeMode
import com.checkit.ui.ReminderSettingsUiState
import com.checkit.ui.SettingsUiState
import com.checkit.ui.components.AppHorizontalDivider
import com.checkit.ui.components.TinyTopAppBar
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
internal fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val backStack = remember { mutableStateListOf<NavKey>(SettingsRoute.Home) }
    val backState = rememberNavigationEventState(NavigationEventInfo.None)

    fun push(route: NavKey) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    NavigationBackHandler(
        state = backState,
        isBackEnabled = backStack.size > 1,
        onBackCompleted = { pop() }
    )

    NavDisplay(
        modifier = modifier.fillMaxSize(),
        backStack = backStack,
        onBack = { pop() },
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        entryProvider = { route ->
            NavEntry(route) {
                when (route) {
                    SettingsRoute.Home -> {
                        val currentState by settingsViewModel.uiState.collectAsState()
                        SettingsHomeScreen(
                            state = currentState,
                            viewModel = settingsViewModel,
                            onOpenReminders = { push(SettingsRoute.Reminders) }
                        )
                    }
                    SettingsRoute.Reminders -> {
                        val currentState by settingsViewModel.uiState.collectAsState()
                        ReminderSettingsScreen(
                            state = currentState.reminders,
                            viewModel = settingsViewModel,
                            onBack = { pop() }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                title = {
                    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = navigationIcon,
                actions = { actions() }
            )
        }
    ) { padding ->
        content(Modifier.padding(top = padding.calculateTopPadding()))
    }
}

@Composable
private fun SettingsHomeScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onOpenReminders: () -> Unit
) {
    SettingsScaffold(title = stringResource(Res.string.settings_title)) { contentModifier ->
        Column(
            modifier = contentModifier.fillMaxSize().padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                item { LanguageSettings(state, viewModel) }
                item { ThemeSettings(state, viewModel) }
                item { ColorSchemeSettings(state, viewModel) }
                item {
                    SettingsRow(
                        title = "Reminders",
                        subtitle = reminderSummary(state.reminders),
                        onClick = onOpenReminders
                    )
                }
            }
            Text(
                text = stringResource(Res.string.version, viewModel.versionName),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReminderSettingsScreen(
    state: ReminderSettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    SettingsScaffold(
        title = "Reminders",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            item {
                ReminderRow(
                    title = "Plan reminder",
                    subtitle = "Start the day by planning My Day",
                    enabled = state.planEnabled,
                    timeMinutes = state.planTimeMinutes,
                    onEnabledChange = viewModel::setPlanReminderEnabled,
                    onTimeChange = viewModel::setPlanReminderTimeMinutes
                )
            }
            item {
                ReminderRow(
                    title = "Review reminder",
                    subtitle = "Close the day with a quick review",
                    enabled = state.reviewEnabled,
                    timeMinutes = state.reviewTimeMinutes,
                    onEnabledChange = viewModel::setReviewReminderEnabled,
                    onTimeChange = viewModel::setReviewReminderTimeMinutes
                )
            }
            item {
                CheckInReminderRow(
                    enabled = state.checkInEnabled,
                    lastShownAtMillis = state.checkInLastShownAtMillis,
                    onEnabledChange = viewModel::setCheckInReminderEnabled
                )
            }
        }
    }
}

@Composable
private fun ReminderRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    timeMinutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(
                modifier = Modifier.weight(1f).clickable(enabled = enabled) { showTimePicker = true }
            ) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = formatTime(timeMinutes),
                    modifier = Modifier.padding(top = 4.dp),
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        AppHorizontalDivider()
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            title = title,
            initialMinutes = timeMinutes,
            onDismiss = { showTimePicker = false },
            onConfirm = { minutes ->
                onTimeChange(minutes)
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun CheckInReminderRow(
    enabled: Boolean,
    lastShownAtMillis: Long?,
    onEnabledChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("CheckIn reminder", fontWeight = FontWeight.SemiBold)
                Text(
                    "Checks every 30 minutes when My Day has nothing near now",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = lastShownAtMillis?.let { "Last shown ${formatLastShown(it)}" } ?: "Last shown never",
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        AppHorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour * 60 + timePickerState.minute) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
internal fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
        AppHorizontalDivider()
    }
}

@Composable
private fun LanguageSettings(state: SettingsUiState, viewModel: SettingsViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    SettingValueRow(
        title = stringResource(Res.string.language),
        value = state.language.label,
        onClick = { showDialog = true }
    )
    if (showDialog) {
        LanguageSelectionDialog(
            selected = state.language,
            onDismiss = { showDialog = false },
            onSelected = { language ->
                viewModel.setLanguage(language)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ThemeSettings(state: SettingsUiState, viewModel: SettingsViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    SettingValueRow(
        title = stringResource(Res.string.theme),
        value = state.themeMode.label(),
        onClick = { showDialog = true }
    )
    if (showDialog) {
        ThemeSelectionDialog(
            selected = state.themeMode,
            onDismiss = { showDialog = false },
            onSelected = { themeMode ->
                viewModel.setThemeMode(themeMode)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ColorSchemeSettings(state: SettingsUiState, viewModel: SettingsViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    SettingValueRow(
        title = stringResource(Res.string.color_scheme),
        value = state.colorSchemeMode.label(),
        onClick = { showDialog = true }
    )
    if (showDialog) {
        ColorSchemeSelectionDialog(
            selected = state.colorSchemeMode,
            onDismiss = { showDialog = false },
            onSelected = { colorSchemeMode ->
                viewModel.setColorSchemeMode(colorSchemeMode)
                showDialog = false
            }
        )
    }
}

@Composable
private fun SettingValueRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
        AppHorizontalDivider()
    }
}

@Composable
private fun LanguageSelectionDialog(
    selected: AppLanguage,
    onDismiss: () -> Unit,
    onSelected: (AppLanguage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.language)) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    SelectionRow(
                        text = language.label,
                        selected = selected == language,
                        onClick = { onSelected(language) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    selected: AppThemeMode,
    onDismiss: () -> Unit,
    onSelected: (AppThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.theme)) },
        text = {
            Column {
                AppThemeMode.entries.forEach { themeMode ->
                    SelectionRow(
                        text = themeMode.label(),
                        selected = selected == themeMode,
                        onClick = { onSelected(themeMode) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun ColorSchemeSelectionDialog(
    selected: AppColorSchemeMode,
    onDismiss: () -> Unit,
    onSelected: (AppColorSchemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.color_scheme)) },
        text = {
            Column {
                AppColorSchemeMode.entries.forEach { colorSchemeMode ->
                    SelectionRow(
                        text = colorSchemeMode.label(),
                        selected = selected == colorSchemeMode,
                        onClick = { onSelected(colorSchemeMode) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun AppThemeMode.label(): String = when (this) {
    AppThemeMode.System -> stringResource(Res.string.theme_system)
    AppThemeMode.Light -> stringResource(Res.string.theme_light)
    AppThemeMode.Dark -> stringResource(Res.string.theme_dark)
}

@Composable
private fun AppColorSchemeMode.label(): String = when (this) {
    AppColorSchemeMode.Sunset -> stringResource(Res.string.color_scheme_sunset)
    AppColorSchemeMode.SkyBlue -> stringResource(Res.string.color_scheme_sky_blue)
    AppColorSchemeMode.SystemDefault -> stringResource(Res.string.color_scheme_system_default)
}

private fun reminderSummary(state: ReminderSettingsUiState): String {
    val enabledCount = listOf(state.planEnabled, state.reviewEnabled, state.checkInEnabled).count { it }
    return when (enabledCount) {
        0 -> "All reminders off"
        3 -> "Plan ${formatTime(state.planTimeMinutes)}, Review ${formatTime(state.reviewTimeMinutes)}, CheckIn on"
        else -> "$enabledCount reminders on"
    }
}

private fun formatTime(minutes: Int): String {
    val normalized = minutes.coerceIn(0, 24 * 60 - 1)
    val hour = normalized / 60
    val minute = normalized % 60
    val displayHour = when (val hour12 = hour % 12) {
        0 -> 12
        else -> hour12
    }
    val suffix = if (hour < 12) "AM" else "PM"
    return "$displayHour:${minute.toString().padStart(2, '0')} $suffix"
}

private fun formatLastShown(millis: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} " +
        "${dateTime.day}, ${dateTime.year} at ${formatTime(dateTime.hour * 60 + dateTime.minute)}"
}

@Composable
private fun SelectionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
