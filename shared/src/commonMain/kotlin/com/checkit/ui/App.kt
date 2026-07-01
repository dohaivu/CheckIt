package com.checkit.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.checkit.domain.usecase.AutoAddTodayTasksToMyDayUseCase
import com.checkit.ui.calendar.CalendarScreen
import com.checkit.ui.components.LocalSnackbarHostState
import com.checkit.ui.localization.AppLocaleProvider
import com.checkit.ui.myday.DailyPlanItemEditorSheet
import com.checkit.ui.myday.MyDayScreen
import com.checkit.ui.reports.ReportScreen
import com.checkit.ui.reports.TagsReport
import com.checkit.ui.reports.TimeReport
import com.checkit.ui.settings.SettingsScreen
import com.checkit.ui.tasks.TaskEditorActions
import com.checkit.ui.tasks.TaskEditorSheet
import com.checkit.ui.tasks.TaskEditorState
import com.checkit.ui.tasks.TaskScreen
import com.checkit.ui.theme.AppTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun CheckItApp(
    viewModels: CheckItViewModels = koinCheckItViewModels(),
    autoAddTodayTasksToMyDayUseCase: AutoAddTodayTasksToMyDayUseCase = koinInject(),
    dailyPlanItemLaunchId: Long? = null,
    taskLaunchId: Long? = null,
    noteLaunchId: Long? = null,
    openMyDaySuggestionsLaunch: Boolean = false,
    onWidgetLaunchConsumed: () -> Unit = {}
) {
    val navState = rememberAppNavigationState()
    val appLanguage by remember(viewModels.settings) {
        viewModels.settings.uiState.map { it.language }.distinctUntilChanged()
    }.collectAsState(AppLanguage.English)
    val appThemeMode by remember(viewModels.settings) {
        viewModels.settings.uiState.map { it.themeMode }.distinctUntilChanged()
    }.collectAsState(AppThemeMode.System)
    val appColorSchemeMode by remember(viewModels.settings) {
        viewModels.settings.uiState.map { it.colorSchemeMode }.distinctUntilChanged()
    }.collectAsState(AppColorSchemeMode.Sunset)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        merge(
            viewModels.task.events,
            viewModels.goal.events,
            viewModels.keyResult.events,
            viewModels.objective.events,
            viewModels.tag.events,
            viewModels.myDay.events,
            viewModels.settings.events,
            viewModels.report.events
        ).collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    val selectedTab = remember(navState.currentRoute) { CheckItTab.fromRoute(navState.currentRoute) }

    val taskUiState by viewModels.task.uiState.collectAsState()
    val myDayUiState by viewModels.myDay.uiState.collectAsState()
    val calendarUiState by viewModels.calendar.uiState.collectAsState()
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

    LaunchedEffect(openMyDaySuggestionsLaunch) {
        if (!openMyDaySuggestionsLaunch) return@LaunchedEffect
        navState.resetTo(AppRoute.MyDay)
        viewModels.myDay.openSuggestions()
        onWidgetLaunchConsumed()
    }

    WidgetLaunchHandler(
        dailyPlanItemLaunchId = dailyPlanItemLaunchId,
        taskLaunchId = taskLaunchId,
        noteLaunchId = noteLaunchId,
        taskUiState = taskUiState,
        myDayUiState = myDayUiState,
        taskViewModel = viewModels.task,
        myDayViewModel = viewModels.myDay,
        navState = navState,
        onWidgetLaunchConsumed = onWidgetLaunchConsumed
    )

    NavigationBackHandler(
        state = backState,
        isBackEnabled = navState.backStack.size > 1 || navState.currentRoute != AppRoute.MyDay,
        onBackCompleted = { navState.pop() }
    )

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        AppLocaleProvider(appLanguage.code) {
            AppTheme(themeMode = appThemeMode, colorSchemeMode = appColorSchemeMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = NavigationBarDefaults.Elevation
                        ) {
                            CheckItTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { navState.resetTo(tab.route()) },
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
                        backStack = navState.backStack,
                        onBack = { navState.pop() },
                        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                        predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                        entryProvider = { key ->
                            NavEntry(key) {
                                when (key) {
                                    AppRoute.Task -> {
                                        TaskScreen(
                                            state = taskUiState,
                                            viewModel = viewModels.task,
                                            goalViewModel = viewModels.goal,
                                            keyResultViewModel = viewModels.keyResult,
                                            objectiveViewModel = viewModels.objective,
                                            tagViewModel = viewModels.tag
                                        )
                                    }
                                    AppRoute.MyDay -> {
                                        MyDayScreen(
                                            viewModel = viewModels.myDay,
                                            onTaskClick = viewModels.task::openTask,
                                            onNoteClick = viewModels.task::openNote,
                                            onNoteTimeChange = viewModels.task::updateNoteTime,
                                            onCreateTask = viewModels.task::openNewTask
                                        )
                                    }
                                    AppRoute.Calendar -> {
                                        CalendarScreen(
                                            state = calendarUiState,
                                            calendarViewModel = viewModels.calendar,
                                            onDateDoubleClick = { date -> viewModels.task.openNewTaskOnDate(date) },
                                            onDailyPlanItemClick = viewModels.myDay::openItemEditor,
                                            onAddDailyPlanItem = { date -> viewModels.myDay.openCheckIn(date = date) },
                                            onTaskClick = viewModels.task::openTask,
                                            onNoteClick = viewModels.task::openNote
                                        )
                                    }
                                    AppRoute.Report -> {
                                        val reportState by viewModels.report.uiState.collectAsState()
                                        ReportScreen(
                                            state = reportState,
                                            reportViewModel = viewModels.report,
                                            onShowTagsReport = { navState.push(AppRoute.TagsReport) },
                                            onShowTimeReport = { navState.push(AppRoute.TimeReport) },
                                        )
                                    }
                                    AppRoute.TagsReport -> {
                                        val reportState by viewModels.report.uiState.collectAsState()
                                        TagsReport(
                                            state = reportState,
                                            onPeriodSelected = viewModels.report::selectPeriod,
                                            onPreviousPeriod = viewModels.report::previousPeriod,
                                            onNextPeriod = viewModels.report::nextPeriod,
                                            onCurrentPeriod = viewModels.report::resetToCurrentPeriod,
                                            onNavigateBack = { navState.pop() },
                                        )
                                    }
                                    AppRoute.TimeReport -> {
                                        val reportState by viewModels.report.uiState.collectAsState()
                                        TimeReport(
                                            state = reportState,
                                            onPeriodSelected = viewModels.report::selectPeriod,
                                            onPreviousPeriod = viewModels.report::previousPeriod,
                                            onNextPeriod = viewModels.report::nextPeriod,
                                            onCurrentPeriod = viewModels.report::resetToCurrentPeriod,
                                            onNavigateBack = { navState.pop() },
                                        )
                                    }

                                    AppRoute.Settings -> SettingsScreen(
                                        settingsViewModel = viewModels.settings
                                    )
                                }
                            }
                        }
                    )
                    taskUiState.editor?.let { editor ->
                        TaskEditorSheet(
                            editor = editor,
                            availableLists = taskUiState.board.objectives,
                            availableTags = taskUiState.board.tags,
                            actions = TaskEditorActions(
                                onDismiss = viewModels.task::dismissEditor,
                                onSave = viewModels.task::saveEditor,
                                onDelete = viewModels.task::deleteEditorItem,
                                onRestore = viewModels.task::restoreCurrentItem,
                                onComplete = viewModels.task::completeCurrentItem,
                                onOpen = viewModels.task::openCurrentItem,
                                onAddToMyDay = {
                                    val taskId = (editor as? TaskEditorState.TaskForm)?.taskId
                                    val task = taskUiState.board.tasks.firstOrNull { it.id == taskId }
                                    task?.let { selectedTask ->
                                        viewModels.myDay.addTaskToMyDay(selectedTask)
                                        viewModels.task.dismissEditor()
                                    }
                                },
                                onTaskNameChange = viewModels.task::updateTaskName,
                                onTaskListChange = viewModels.task::updateTaskListId,
                                onTaskDescriptionChange = viewModels.task::updateTaskDescription,
                                onTaskDoDateChange = viewModels.task::updateTaskDoDate,
                                onTaskStartTimeChange = viewModels.task::updateTaskStartTime,
                                onTaskEndTimeChange = viewModels.task::updateTaskEndTime,
                                onDailyPlanStartTimeChange = viewModels.task::updateDailyPlanStartTime,
                                onDailyPlanEndTimeChange = viewModels.task::updateDailyPlanEndTime,
                                onDailyPlanStatus = viewModels.task::updateDailyPlanStatus,
                                onDailyPlanDelete = { itemId ->
                                    viewModels.myDay.deleteDailyPlanItem(itemId)
                                    viewModels.task.removeDailyPlanItemFromEditor(itemId)
                                },
                                onTaskRepeatChange = viewModels.task::updateTaskRepeat,
                                onTaskPriorityChange = viewModels.task::updateTaskPriority,
                                onTaskReminderToggle = viewModels.task::toggleTaskReminder,
                                onSubTaskToggle = viewModels.task::toggleSubTask,
                                onSubTaskAdd = viewModels.task::addSubTask,
                                onSubTaskNameChange = viewModels.task::updateSubTaskName,
                                onSubTaskRemove = viewModels.task::removeSubTask,
                                onSubTaskMove = viewModels.task::moveSubTask,
                                onTaskTagToggle = viewModels.task::toggleTaskTag,
                                onNoteTitleChange = viewModels.task::updateNoteTitle,
                                onNoteContentChange = viewModels.task::updateNoteContent,
                                onNoteListChange = viewModels.task::updateNoteListId,
                                onNoteDateChange = viewModels.task::updateNoteDate,
                                onNoteStartTimeChange = viewModels.task::updateNoteStartTime,
                                onNoteTagToggle = viewModels.task::toggleNoteTag,
                                onSwitchAddModeToTask = viewModels.task::switchAddEditorToTask,
                                onSwitchAddModeToNote = viewModels.task::switchAddEditorToNote
                            )
                        )
                    }
                    myDayUiState.itemEditor?.let { editor ->
                        DailyPlanItemEditorSheet(
                            state = editor,
                            availableTags = myDayUiState.board.tags,
                            onDismiss = viewModels.myDay::dismissCheckIn,
                            onTitleChange = viewModels.myDay::updateTitle,
                            onNoteChange = viewModels.myDay::updateNote,
                            onStatusChange = viewModels.myDay::updateStatus,
                            onSourceChange = viewModels.myDay::updateEditorSource,
                            onStartTimeChange = viewModels.myDay::updateStartTime,
                            onEndTimeChange = viewModels.myDay::updateEndTime,
                            onTagToggle = viewModels.myDay::toggleTag,
                            onAdd = viewModels.myDay::addCheckIn,
                            onDelete = viewModels.myDay::deleteEditorItem
                        )
                    }
                }
            }
        }
    }
}
