package com.checkit.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.usecase.AutoAddTodayTasksToMyDayUseCase
import com.checkit.ui.calendar.CalendarScreen
import com.checkit.ui.calendar.CalendarViewModel
import com.checkit.ui.myday.MyDayScreen
import com.checkit.ui.myday.MyDayViewModel
import com.checkit.ui.tasks.TaskScreen
import com.checkit.ui.tasks.TaskListViewModel
import com.checkit.ui.tasks.TaskTagViewModel
import com.checkit.ui.tasks.TaskViewModel
import com.checkit.ui.localization.AppLocaleProvider
import com.checkit.ui.reports.ReportScreen
import com.checkit.ui.reports.ReportViewModel
import com.checkit.ui.reports.TagsReport
import com.checkit.ui.reports.TimeReport
import com.checkit.ui.settings.SettingsScreen
import com.checkit.ui.settings.SettingsViewModel
import com.checkit.ui.myday.DailyPlanItemEditorSheet
import com.checkit.ui.tasks.TaskEditorSheet
import com.checkit.ui.theme.AppTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
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
    data object TimeReport : NavKey

    @Serializable
    data object TagsReport : NavKey

    @Serializable
    data object Settings : NavKey
}

@Composable
fun CheckItApp(
    taskViewModel: TaskViewModel = koinViewModel(),
    taskListViewModel: TaskListViewModel = koinViewModel(),
    taskTagViewModel: TaskTagViewModel = koinViewModel(),
    myDayViewModel: MyDayViewModel = koinViewModel(),
    calendarViewModel: CalendarViewModel = koinViewModel(),
    reportViewModel: ReportViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    autoAddTodayTasksToMyDayUseCase: AutoAddTodayTasksToMyDayUseCase = koinInject(),
    dailyPlanItemLaunchId: Long? = null,
    taskLaunchId: Long? = null,
    noteLaunchId: Long? = null,
    openMyDaySuggestionsLaunch: Boolean = false,
    onWidgetLaunchConsumed: () -> Unit = {}
) {
    val backStack = remember { mutableStateListOf<NavKey>(Routes.MyDay) }
    val taskMessage by remember(taskViewModel) {
        taskViewModel.uiState.map { it.message }.distinctUntilChanged()
    }.collectAsState(null)
    val settingsMessage by remember(settingsViewModel) {
        settingsViewModel.uiState.map { it.message }.distinctUntilChanged()
    }.collectAsState(null)
    val taskListMessage by remember(taskListViewModel) {
        taskListViewModel.uiState.map { it.message }.distinctUntilChanged()
    }.collectAsState(null)
    val taskTagMessage by remember(taskTagViewModel) {
        taskTagViewModel.uiState.map { it.message }.distinctUntilChanged()
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
    val currentRoute = backStack.lastOrNull() ?: Routes.MyDay
    val selectedTab = currentRoute.asTab()

    val taskUiState by taskViewModel.uiState.collectAsState()
    val myDayUiState by myDayViewModel.uiState.collectAsState()
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val appScope = rememberCoroutineScope()

    fun runAutoAddTodayTasksToMyDay() {
        appScope.launch {
            runCatching { autoAddTodayTasksToMyDayUseCase() }
        }
    }

    LaunchedEffect(Unit) {
        runAutoAddTodayTasksToMyDay()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                runAutoAddTodayTasksToMyDay()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(taskMessage, myDayMessage, settingsMessage, taskListMessage, taskTagMessage) {
        val message = taskMessage ?: myDayMessage ?: settingsMessage ?: taskListMessage ?: taskTagMessage
            ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)

        if (myDayMessage != null) {
            myDayViewModel.consumeMessage()
        }
        if (settingsMessage != null) {
            settingsViewModel.consumeMessage()
        }
        if (taskListMessage != null) {
            taskListViewModel.consumeMessage()
        }
        if (taskTagMessage != null) {
            taskTagViewModel.consumeMessage()
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
        } else if (backStack.lastOrNull() != Routes.MyDay) {
            resetTo(Routes.MyDay)
        }
    }

    LaunchedEffect(openMyDaySuggestionsLaunch) {
        if (!openMyDaySuggestionsLaunch) return@LaunchedEffect
        resetTo(Routes.MyDay)
        myDayViewModel.openSuggestions()
        onWidgetLaunchConsumed()
    }

    LaunchedEffect(
        dailyPlanItemLaunchId,
        taskLaunchId,
        noteLaunchId,
        taskUiState.board,
        myDayUiState.dailyPlans
    ) {
        val launchTarget = WidgetLaunchTarget.from(
            dailyPlanItemId = dailyPlanItemLaunchId,
            taskId = taskLaunchId,
            noteId = noteLaunchId,
            taskUiState = taskUiState,
            myDayUiState = myDayUiState
        ) ?: return@LaunchedEffect

        resetTo(Routes.MyDay)
        when (launchTarget) {
            is WidgetLaunchTarget.Task -> taskViewModel.openTask(launchTarget.task, launchTarget.dailyPlanItem)
            is WidgetLaunchTarget.Note -> taskViewModel.openNote(launchTarget.note)
            is WidgetLaunchTarget.DailyPlan -> myDayViewModel.openItemEditor(launchTarget.item, launchTarget.date)
        }
        onWidgetLaunchConsumed()
    }

    NavigationBackHandler(
        state = backState,
        isBackEnabled = backStack.size > 1 || currentRoute != Routes.MyDay,
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
                    modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
                    backStack = backStack,
                    onBack = { onBack() },
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    entryProvider = { key ->
                        NavEntry(key) {
                            when (key) {
                                Routes.Task -> {
                                    TaskScreen(
                                        state = taskUiState,
                                        viewModel = taskViewModel,
                                        listViewModel = taskListViewModel,
                                        tagViewModel = taskTagViewModel
                                    )
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
                                    CalendarScreen(
                                        state = calendarUiState,
                                        calendarViewModel = calendarViewModel,
                                        onDateDoubleClick = { date -> taskViewModel.openNewTaskOnDate(date) },
                                        onDailyPlanItemClick = myDayViewModel::openItemEditor,
                                        onAddDailyPlanItem = { date -> myDayViewModel.openCheckIn(date = date) },
                                        onTaskClick = taskViewModel::openTask,
                                        onNoteClick = taskViewModel::openNote
                                    )
                                }
                                Routes.Report -> {
                                    val reportState by reportViewModel.uiState.collectAsState()
                                    ReportScreen(
                                        state = reportState,
                                        reportViewModel = reportViewModel,
                                        onShowTagsReport = { push(Routes.TagsReport) },
                                        onShowTimeReport = { push(Routes.TimeReport) },
                                    )
                                }
                                Routes.TagsReport -> {
                                    val reportState by reportViewModel.uiState.collectAsState()
                                    TagsReport(
                                        state = reportState,
                                        onPeriodSelected = reportViewModel::selectPeriod,
                                        onPreviousPeriod = reportViewModel::previousPeriod,
                                        onNextPeriod = reportViewModel::nextPeriod,
                                        onCurrentPeriod = reportViewModel::resetToCurrentPeriod,
                                        onNavigateBack = { onBack() },
                                    )
                                }
                                Routes.TimeReport -> {
                                    val reportState by reportViewModel.uiState.collectAsState()
                                    TimeReport(
                                        state = reportState,
                                        onPeriodSelected = reportViewModel::selectPeriod,
                                        onPreviousPeriod = reportViewModel::previousPeriod,
                                        onNextPeriod = reportViewModel::nextPeriod,
                                        onCurrentPeriod = reportViewModel::resetToCurrentPeriod,
                                        onNavigateBack = { onBack() },
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
                        onSave = taskViewModel::saveEditor,
                        onDelete = taskViewModel::deleteEditorItem,
                        onRestore = taskViewModel::restoreCurrentItem,
                        onComplete = taskViewModel::completeCurrentItem,
                        onOpen = taskViewModel::openCurrentItem,
                        onAddToMyDay = {
                            val taskId = (editor as? TaskEditorState.TaskForm)?.taskId
                            val task = taskUiState.board.tasks.firstOrNull { it.id == taskId }
                            task?.let { selectedTask ->
                                myDayViewModel.addTaskToMyDay(selectedTask)
                                taskViewModel.dismissEditor()
                            }
                        },
                        onTaskNameChange = taskViewModel::updateTaskName,
                        onTaskListChange = taskViewModel::updateTaskListId,
                        onTaskDescriptionChange = taskViewModel::updateTaskDescription,
                        onTaskDoDateChange = taskViewModel::updateTaskDoDate,
                        onTaskStartTimeChange = taskViewModel::updateTaskStartTime,
                        onTaskEndTimeChange = taskViewModel::updateTaskEndTime,
                        onDailyPlanStartTimeChange = taskViewModel::updateDailyPlanStartTime,
                        onDailyPlanEndTimeChange = taskViewModel::updateDailyPlanEndTime,
                        onDailyPlanStatus = taskViewModel::updateDailyPlanStatus,
                        onDailyPlanDelete = { itemId ->
                            myDayViewModel.deleteDailyPlanItem(itemId)
                            taskViewModel.removeDailyPlanItemFromEditor(itemId)
                        },
                        onTaskRepeatChange = taskViewModel::updateTaskRepeat,
                        onTaskPriorityChange = taskViewModel::updateTaskPriority,
                        onTaskReminderToggle = taskViewModel::toggleTaskReminder,
                        onSubTaskToggle = taskViewModel::toggleSubTask,
                        onSubTaskAdd = taskViewModel::addSubTask,
                        onSubTaskNameChange = taskViewModel::updateSubTaskName,
                        onSubTaskRemove = taskViewModel::removeSubTask,
                        onSubTaskMove = taskViewModel::moveSubTask,
                        onTaskTagToggle = taskViewModel::toggleTaskTag,
                        onNoteTitleChange = taskViewModel::updateNoteTitle,
                        onNoteContentChange = taskViewModel::updateNoteContent,
                        onNoteListChange = taskViewModel::updateNoteListId,
                        onNoteDateChange = taskViewModel::updateNoteDate,
                        onNoteStartTimeChange = taskViewModel::updateNoteStartTime,
                        onNoteTagToggle = taskViewModel::toggleNoteTag,
                        onSwitchAddModeToTask = taskViewModel::switchAddEditorToTask,
                        onSwitchAddModeToNote = taskViewModel::switchAddEditorToNote
                    )
                }
                myDayUiState.itemEditor?.let { editor ->
                    DailyPlanItemEditorSheet(
                        state = editor,
                        availableTags = myDayUiState.board.tags,
                        onDismiss = myDayViewModel::dismissCheckIn,
                        onDoneTitleChange = myDayViewModel::updateDoneTitle,
                        onDoneNoteChange = myDayViewModel::updateDoneNote,
                        onStatusChange = myDayViewModel::updateStatus,
                        onSourceChange = myDayViewModel::updateEditorSource,
                        onStartTimeChange = myDayViewModel::updateStartTime,
                        onEndTimeChange = myDayViewModel::updateEndTime,
                        onTagToggle = myDayViewModel::toggleTag,
                        onAdd = myDayViewModel::addCheckIn,
                        onDelete = myDayViewModel::deleteEditorItem
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
    Routes.TimeReport -> CheckItTab.Report
    Routes.TagsReport -> CheckItTab.Report
    Routes.Settings -> CheckItTab.Settings
    else -> null
}

private sealed interface WidgetLaunchTarget {
    data class Task(val task: TaskItem, val dailyPlanItem: DailyPlanItem?) : WidgetLaunchTarget
    data class Note(val note: NoteItem) : WidgetLaunchTarget
    data class DailyPlan(val item: DailyPlanItem, val date: LocalDate) : WidgetLaunchTarget

    companion object {
        fun from(
            dailyPlanItemId: Long?,
            taskId: Long?,
            noteId: Long?,
            taskUiState: TaskUiState,
            myDayUiState: MyDayUiState
        ): WidgetLaunchTarget? {
            val dailyPlanTarget = dailyPlanItemId?.let { itemId ->
                myDayUiState.dailyPlans.firstNotNullOfOrNull { plan ->
                    plan.items.firstOrNull { it.id == itemId }?.let { item -> item to plan.date }
                }
            }
            if (dailyPlanItemId != null && dailyPlanTarget == null) return null
            if (taskId != null) {
                val task = taskUiState.board.tasksById[taskId] ?: return null
                return Task(task = task, dailyPlanItem = dailyPlanTarget?.first)
            }
            if (noteId != null) {
                val note = taskUiState.board.notesById[noteId] ?: return null
                return Note(note)
            }
            return dailyPlanTarget?.let { (item, date) -> DailyPlan(item, date) }
        }
    }
}

private fun CheckItTab.route(): NavKey = when (this) {
    CheckItTab.Task -> Routes.Task
    CheckItTab.MyDay -> Routes.MyDay
    CheckItTab.Calendar -> Routes.Calendar
    CheckItTab.Report -> Routes.Report
    CheckItTab.Settings -> Routes.Settings
}

private fun CheckItTab.icon() = when (this) {
    CheckItTab.Task -> Icons.AutoMirrored.Filled.ListAlt
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
