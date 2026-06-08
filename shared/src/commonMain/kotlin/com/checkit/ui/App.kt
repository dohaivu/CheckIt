package com.checkit.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.tab_calendar
import checkit.shared.generated.resources.tab_my_day
import checkit.shared.generated.resources.tab_tasks
import checkit.shared.generated.resources.tab_report
import checkit.shared.generated.resources.tab_settings
import com.checkit.domain.NoteItem
import com.checkit.ui.calendar.CalendarScreen
import com.checkit.ui.calendar.CalendarViewModel
import com.checkit.ui.myday.MyDayScreen
import com.checkit.ui.myday.MyDayViewModel
import com.checkit.ui.tasks.TaskScreen
import com.checkit.ui.tasks.TaskViewModel
import com.checkit.ui.localization.AppLocaleProvider
import com.checkit.ui.reports.ReportScreen
import com.checkit.ui.reports.ReportViewModel
import com.checkit.ui.settings.SettingsScreen
import com.checkit.ui.settings.SettingsViewModel
import com.checkit.ui.tasks.TaskEditorSheet
import com.checkit.ui.theme.AppTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private data object Routes {
    @Serializable
    data object Task : NavKey

    @Serializable
    data object Calendar : NavKey

    @Serializable
    data object MyDay : NavKey

    @Serializable
    data object Report : NavKey

    @Serializable
    data object Settings : NavKey
}

@Composable
fun CheckItApp(
    taskViewModel: TaskViewModel = koinViewModel(),
    myDayViewModel: MyDayViewModel = koinViewModel(),
    calendarViewModel: CalendarViewModel = koinViewModel(),
    reportViewModel: ReportViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val backStack = remember { mutableStateListOf<NavKey>(Routes.Task) }
    val taskMessage by remember(taskViewModel) {
        taskViewModel.uiState.map { it.message }.distinctUntilChanged()
    }.collectAsState(null)
    val settingsMessage by remember(settingsViewModel) {
        settingsViewModel.uiState.map { it.message }.distinctUntilChanged()
    }.collectAsState(null)
    val myDayMessage by remember(myDayViewModel) {
        myDayViewModel.uiState.map { it.message }.distinctUntilChanged()
    }.collectAsState(null)
    val appLanguage by remember(settingsViewModel) {
        settingsViewModel.uiState.map { it.language }.distinctUntilChanged()
    }.collectAsState(AppLanguage.English)
    val appThemeMode by remember(settingsViewModel) {
        settingsViewModel.uiState.map { it.themeMode }.distinctUntilChanged()
    }.collectAsState(AppThemeMode.System)
    val appColorSchemeMode by remember(settingsViewModel) {
        settingsViewModel.uiState.map { it.colorSchemeMode }.distinctUntilChanged()
    }.collectAsState(AppColorSchemeMode.Sunset)
    val snackbarHostState = remember { SnackbarHostState() }
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    val currentRoute = backStack.lastOrNull() ?: Routes.Task
    val selectedTab = currentRoute.asTab()
    val taskUiState by taskViewModel.uiState.collectAsState()

    LaunchedEffect(taskMessage, myDayMessage, settingsMessage) {
        val message = taskMessage ?: myDayMessage ?: settingsMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)

        if (myDayMessage != null) {
            myDayViewModel.consumeMessage()
        }
        if (settingsMessage != null) {
            settingsViewModel.consumeMessage()
        }
    }

    fun resetTo(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }

    fun push(route: NavKey) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun onBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else if (backStack.lastOrNull() != Routes.Task) {
            resetTo(Routes.Task)
        }
    }

    NavigationBackHandler(
        state = backState,
        isBackEnabled = backStack.size > 1 || currentRoute != Routes.Task,
        onBackCompleted = { onBack() }
    )

    AppLocaleProvider(appLanguage.code) {
        AppTheme(themeMode = appThemeMode, colorSchemeMode = appColorSchemeMode) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = NavigationBarDefaults.Elevation
                    ) {
                        CheckItTab.entries.forEach { tab ->
                            val route = tab.route()
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = {
                                    resetTo(route)
                                },
                                icon = { Icon(tab.icon(), contentDescription = tab.label()) },
                                label = { Text(tab.label()) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                NavDisplay(
                    modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                    backStack = backStack,
                    onBack = { onBack() },
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    entryProvider = { key ->
                        NavEntry(key) {
                            when (key) {
                                Routes.Task -> {
                                    TaskScreen(taskUiState, taskViewModel)
                                }
                                Routes.MyDay -> {
                                    MyDayScreen(
                                        viewModel = myDayViewModel,
                                        onTaskClick = taskViewModel::openTask,
                                        onNoteClick = taskViewModel::openNote,
                                        onNoteTimeChange = taskViewModel::updateNoteTime,
                                        onCreateTask = taskViewModel::openNewTask
                                    )
                                }
                                Routes.Calendar -> {
                                    val calendarState by calendarViewModel.uiState.collectAsState()
                                    CalendarScreen(
                                        state = calendarState,
                                        calendarViewModel = calendarViewModel,
                                        onDateDoubleClick = taskViewModel::openNewTaskOnDate,
                                        onTaskClick = taskViewModel::openTask,
                                        onNoteClick = taskViewModel::openNote
                                    )
                                }
                                Routes.Report -> {
                                    val reportState by reportViewModel.uiState.collectAsState()
                                    ReportScreen(
                                        state = reportState,
                                        reportViewModel = reportViewModel,
                                    )
                                }

                                Routes.Settings -> SettingsScreen(
                                    settingsViewModel = settingsViewModel
                                )
                            }
                        }
                    }
                )
                taskUiState.editor?.let { editor ->
                    TaskEditorSheet(
                        editor = editor,
                        availableLists = taskUiState.board.lists,
                        availableTags = taskUiState.board.tags,
                        onDismiss = taskViewModel::dismissEditor,
                        onEdit = taskViewModel::editCurrentItem,
                        onSave = taskViewModel::saveEditor,
                        onDelete = taskViewModel::deleteEditorItem,
                        onComplete = taskViewModel::completeCurrentItem,
                        onOpen = taskViewModel::openCurrentItem,
                        onTaskNameChange = taskViewModel::updateTaskName,
                        onTaskListChange = taskViewModel::updateTaskListId,
                        onTaskDescriptionChange = taskViewModel::updateTaskDescription,
                        onTaskDueDateChange = taskViewModel::updateTaskDueDate,
                        onTaskStartTimeChange = taskViewModel::updateTaskStartTime,
                        onTaskEndTimeChange = taskViewModel::updateTaskEndTime,
                        onTaskRepeatChange = taskViewModel::updateTaskRepeat,
                        onTaskPriorityChange = taskViewModel::updateTaskPriority,
                        onTaskRemindersEnabledChange = taskViewModel::setTaskRemindersEnabled,
                        onTaskReminderToggle = taskViewModel::toggleTaskReminder,
                        onSubTaskToggle = taskViewModel::toggleSubTask,
                        onSubTaskAdd = taskViewModel::addSubTask,
                        onSubTaskNameChange = taskViewModel::updateSubTaskName,
                        onSubTaskRemove = taskViewModel::removeSubTask,
                        onTaskTagToggle = taskViewModel::toggleTaskTag,
                        onNoteContentChange = taskViewModel::updateNoteContent,
                        onNoteListChange = taskViewModel::updateNoteListId,
                        onNoteDateChange = taskViewModel::updateNoteDate,
                        onNoteStartTimeChange = taskViewModel::updateNoteStartTime,
                        onNoteTagToggle = taskViewModel::toggleNoteTag,
                        onSwitchAddModeToTask = taskViewModel::switchAddEditorToTask,
                        onSwitchAddModeToNote = taskViewModel::switchAddEditorToNote
                    )
                }
            }
        }
    }
    }
}

private fun NavKey.asTab(): CheckItTab? = when (this) {
    Routes.Task -> CheckItTab.Task
    Routes.MyDay -> CheckItTab.MyDay
    Routes.Calendar -> CheckItTab.Calendar
    Routes.Report -> CheckItTab.Report
    Routes.Settings -> CheckItTab.Settings
    else -> null
}

private fun CheckItTab.route(): NavKey = when (this) {
    CheckItTab.Task -> Routes.Task
    CheckItTab.MyDay -> Routes.MyDay
    CheckItTab.Calendar -> Routes.Calendar
    CheckItTab.Report -> Routes.Report
    CheckItTab.Settings -> Routes.Settings
}

private fun CheckItTab.icon() = when (this) {
    CheckItTab.Task -> Icons.Default.Add
    CheckItTab.MyDay -> Icons.Default.Today
    CheckItTab.Calendar -> Icons.Default.CalendarMonth
    CheckItTab.Report -> Icons.Default.BarChart
    CheckItTab.Settings -> Icons.Default.MoreHoriz
}

@Composable
private fun CheckItTab.label(): String = when (this) {
    CheckItTab.Task -> stringResource(Res.string.tab_tasks)
    CheckItTab.MyDay -> stringResource(Res.string.tab_my_day)
    CheckItTab.Calendar -> stringResource(Res.string.tab_calendar)
    CheckItTab.Report -> stringResource(Res.string.tab_report)
    CheckItTab.Settings -> stringResource(Res.string.tab_settings)
}
