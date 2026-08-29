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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.checkit.data.SettingsRepository
import com.checkit.domain.usecase.AutoAddTodayTasksToMyDayUseCase
import com.checkit.domain.usecase.RebuildReflectStatsUseCase
import com.checkit.ui.calendar.CalendarScreen
import com.checkit.ui.components.LocalSnackbarHostState
import com.checkit.ui.localization.AppLocaleProvider
import com.checkit.ui.myday.DailyPlanItemEditorSheet
import com.checkit.ui.journal.JournalEntryEditorSheet
import com.checkit.ui.journal.JournalHistorySheet
import com.checkit.ui.myday.MyDayScreen
import com.checkit.ui.nested.NestedListScreen
import com.checkit.ui.reflect.PeriodGoalEditorSheet
import com.checkit.ui.reflect.ReflectScreen
import com.checkit.ui.settings.SettingsScreen
import com.checkit.ui.tasks.TaskEditorActions
import com.checkit.ui.tasks.TaskEditorSheet
import com.checkit.ui.tasks.TaskEditorState
import com.checkit.ui.tasks.TaskScreen
import com.checkit.ui.tasks.list.ListSectionScreen
import com.checkit.ui.tasks.tag.TagEditorSheet
import com.checkit.ui.tasks.tag.TagScreen
import com.checkit.ui.theme.AppTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun CheckItApp(
    viewModels: CheckItViewModels = koinCheckItViewModels(),
    autoAddTodayTasksToMyDayUseCase: AutoAddTodayTasksToMyDayUseCase = koinInject(),
    rebuildReflectStatsUseCase: RebuildReflectStatsUseCase = koinInject(),
    settingsRepository: SettingsRepository = koinInject(),
    dailyPlanItemLaunchId: Long? = null,
    taskLaunchId: Long? = null,
    noteLaunchId: Long? = null,
    openMyDaySuggestionsLaunch: Boolean = false,
    openDayCloseLaunch: Boolean = false,
    openPlanAssistLaunch: Boolean = false,
    openCheckInLaunch: Boolean = false,
    openNewJournalEntryLaunch: Boolean = false,
    openQuickSprintLaunch: Boolean = false,
    openNewTaskLaunch: Boolean = false,
    startSprintItemIdLaunch: Long? = null,
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
    }.collectAsState(AppColorSchemeMode.SkyBlue)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        merge(
            viewModels.task.events,
            viewModels.tag.events,
            viewModels.myDay.events,
            viewModels.settings.events,
            viewModels.reflect.events,
            viewModels.nested.events
        ).collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.OpenReflect -> navState.resetTo(AppRoute.Reflect)
            }
        }
    }

    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    val selectedTab = remember(navState.currentRoute) { CheckItTab.fromRoute(navState.currentRoute) }

    val taskUiState by viewModels.task.uiState.collectAsState()
    val tagUiState by viewModels.tag.uiState.collectAsState()
    val myDayUiState by viewModels.myDay.uiState.collectAsState()
    val calendarUiState by viewModels.calendar.uiState.collectAsState()
    val nestedUiState by viewModels.nested.uiState.collectAsState()
    val reflectEditorState by viewModels.reflect.editor.collectAsState()
    val journalHistoryUiState by viewModels.journalHistory.uiState.collectAsState()
    var showJournalHistory by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val appScope = rememberCoroutineScope()

    fun runAutoTodayTasks() {
        appScope.launch {
            runCatching {
                // Once-a-day gate shared by both maintenance tasks.
                val todayEpochDay = today().toEpochDays().toInt()
                if (settingsRepository.settings.first().autoMyDayLastRunEpochDay == todayEpochDay) {
                    return@runCatching
                }
                autoAddTodayTasksToMyDayUseCase()
                rebuildReflectStatsUseCase()
                settingsRepository.setAutoMyDayLastRunEpochDay(todayEpochDay)
            }
        }
    }

    LaunchedEffect(Unit) {
        runAutoTodayTasks()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                runAutoTodayTasks()
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

    LaunchedEffect(openDayCloseLaunch) {
        if (!openDayCloseLaunch) return@LaunchedEffect
        navState.resetTo(AppRoute.MyDay)
        viewModels.myDay.openDayClose()
        onWidgetLaunchConsumed()
    }

    LaunchedEffect(openPlanAssistLaunch) {
        if (!openPlanAssistLaunch) return@LaunchedEffect
        navState.resetTo(AppRoute.MyDay)
        viewModels.myDay.openSuggestions()
        onWidgetLaunchConsumed()
    }

    LaunchedEffect(openCheckInLaunch) {
        if (!openCheckInLaunch) return@LaunchedEffect
        navState.resetTo(AppRoute.MyDay)
        viewModels.myDay.openQuickSprint()
        onWidgetLaunchConsumed()
    }

    LaunchedEffect(openNewJournalEntryLaunch) {
        if (!openNewJournalEntryLaunch) return@LaunchedEffect
        navState.resetTo(AppRoute.MyDay)
        viewModels.myDay.openNewJournalEntry()
        onWidgetLaunchConsumed()
    }

    LaunchedEffect(openQuickSprintLaunch) {
        if (!openQuickSprintLaunch) return@LaunchedEffect
        navState.resetTo(AppRoute.MyDay)
        viewModels.myDay.openQuickSprint()
        onWidgetLaunchConsumed()
    }

    LaunchedEffect(openNewTaskLaunch) {
        if (!openNewTaskLaunch) return@LaunchedEffect
        viewModels.task.openNewTask()
        onWidgetLaunchConsumed()
    }

    LaunchedEffect(startSprintItemIdLaunch, myDayUiState.dailyPlans) {
        if (startSprintItemIdLaunch == null) return@LaunchedEffect
        if (myDayUiState.dailyPlans.isEmpty()) return@LaunchedEffect
        navState.resetTo(AppRoute.MyDay)
        viewModels.myDay.startSprintByItemId(startSprintItemIdLaunch)
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
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
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
                                            listViewModel = viewModels.list,
                                            onOpenTags = { navState.push(AppRoute.Tags) },
                                            onOpenSections = { listId -> navState.push(AppRoute.ListSections(listId)) }
                                        )
                                    }
                                    AppRoute.Tags -> {
                                        TagScreen(
                                            tags = taskUiState.board.tags,
                                            selectedTagId = taskUiState.selectedTagId,
                                            tagViewModel = viewModels.tag,
                                            onTagClick = { tagId ->
                                                viewModels.task.selectTag(tagId)
                                                navState.pop()
                                            },
                                            onNavigateBack = { navState.pop() }
                                        )
                                    }
                                    AppRoute.MyDay -> {
                                        MyDayScreen(
                                            viewModel = viewModels.myDay,
                                            onTaskClick = viewModels.task::openTask,
                                            onNoteClick = viewModels.task::openNote,
                                            onNoteTimeChange = viewModels.task::updateNoteTime,
                                            onCreateTask = { addToMyDayOnSave ->
                                                viewModels.task.openNewTask(today(), addToMyDayOnSave)
                                            },
                                            onNewTagClick = viewModels.tag::openNewTag,
                                            onOpenNewGoalEditor = { date, period, mode ->
                                                viewModels.reflect.openNewGoalEditor(date, period, mode)
                                            },
                                            onOpenGoalEditor = { goal, mode ->
                                                viewModels.reflect.openGoal(goal, mode)
                                            }
                                        )
                                    }
                                    AppRoute.Calendar -> {
                                        CalendarScreen(
                                            state = calendarUiState,
                                            board = taskUiState.board,
                                            calendarViewModel = viewModels.calendar,
                                            onDateDoubleClick = { date -> viewModels.task.openNewTaskOnDate(date) },
                                            onDailyPlanItemClick = viewModels.myDay::openItemEditor,
                                            onOpenJournalHistory = { showJournalHistory = true },
                                            onAddDailyPlanItem = { date -> viewModels.myDay.openDailyPlan(date = date) },
                                            onTaskClick = viewModels.task::openTask,
                                            onNoteClick = viewModels.task::openNote,
                                            onNewTagClick = viewModels.tag::openNewTag,
                                            onOpenReflect = { date ->
                                                viewModels.reflect.focusDay(date)
                                                navState.resetTo(AppRoute.Reflect)
                                            }
                                        )
                                    }
                                    AppRoute.Reflect -> {
                                        val reflectState by viewModels.reflect.uiState.collectAsState()
                                        ReflectScreen(
                                            state = reflectState,
                                            viewModel = viewModels.reflect
                                        )
                                    }

                                    AppRoute.NestedLists -> {
                                        val settingsUiState by viewModels.settings.uiState.collectAsState()
                                        LaunchedEffect(Unit) {
                                            val lastId = settingsUiState.lastNestedDocumentId
                                            if (lastId != null && nestedUiState.editor == null) {
                                                viewModels.nested.openDocument(lastId)
                                            }
                                        }
                                        NestedListScreen(
                                            state = nestedUiState,
                                            viewModel = viewModels.nested,
                                            onAddToDailyPlan = { title, tagIds, nestedListItemId ->
                                                navState.resetTo(AppRoute.MyDay)
                                                viewModels.myDay.openDailyPlan(title, tagIds, nestedListItemId)
                                            },
                                            onCopyToTask = { title, note, subtaskTexts ->
                                                viewModels.task.openNewTaskFromNestedItem(title, note, subtaskTexts)
                                            }
                                        )
                                    }
                                    is AppRoute.ListSections -> {
                                        val listId = (key as AppRoute.ListSections).listId
                                        val list = taskUiState.board.lists.find { it.id == listId }
                                        LaunchedEffect(list) {
                                            if (list != null) {
                                                viewModels.listSection.loadList(list.id, list.sections)
                                            }
                                        }
                                        ListSectionScreen(
                                            viewModel = viewModels.listSection,
                                            onNavigateBack = { navState.pop() }
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
                            availableLists = taskUiState.board.lists,
                            availableTags = taskUiState.board.tags,
                            recentLabels = taskUiState.recentLabels,
                            actions = TaskEditorActions(
                                onDismiss = viewModels.task::dismissEditor,
                                onSave = viewModels.task::saveEditor,
                                onDelete = viewModels.task::deleteEditorItem,
                                onRestore = viewModels.task::restoreCurrentItem,
                                onComplete = viewModels.task::completeCurrentItem,
                                onReopen = viewModels.task::reopenCurrentItem,
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
                                onTaskTimeChange = viewModels.task::updateTaskTime,
                                onDailyPlanTimeChange = viewModels.task::updateDailyPlanTime,
                                onDailyPlanStatus = viewModels.task::updateDailyPlanStatus,
                                onDailyPlanDelete = { itemId ->
                                    viewModels.myDay.deleteDailyPlanItem(itemId)
                                    viewModels.task.removeDailyPlanItemFromEditor(itemId)
                                },
                                onDailyPlanStartSprint = { item ->
                                    viewModels.myDay.startSprintForItem(item)
                                    viewModels.task.dismissEditor()
                                },
                                onDailyPlanStartOngoingSprint = { item ->
                                    viewModels.myDay.startOngoingSprintForItem(item)
                                    viewModels.task.dismissEditor()
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
                                onPinToggle = viewModels.task::togglePin,
                                onTaskLabelChange = viewModels.task::updateTaskLabel,
                                onNoteLabelChange = viewModels.task::updateNoteLabel,
                                onNewTagClick = viewModels.tag::openNewTag,
                            )
                        )
                    }
                    myDayUiState.itemEditor?.let { editor ->
                        DailyPlanItemEditorSheet(
                            state = editor,
                            availableTags = myDayUiState.tags,
                            recentLabels = myDayUiState.recentLabels,
                            suggestions = myDayUiState.goalSuggestions,
                            onDismiss = viewModels.myDay::dismissDailyPlanEditor,
                            onTitleChange = viewModels.myDay::updateTitle,
                            onNoteChange = viewModels.myDay::updateNote,
                            onLabelChange = viewModels.myDay::updateLabel,
                            onStatusChange = viewModels.myDay::updateStatus,
                            onSourceChange = viewModels.myDay::updateEditorSource,
                            onDateChange = viewModels.myDay::updateDate,
                            onTimeChange = viewModels.myDay::updateTime,
                            onTagToggle = viewModels.myDay::toggleTag,
                            onNewTagClick = viewModels.tag::openNewTag,
                            onAdd = viewModels.myDay::addDailyPlan,
                            onDelete = viewModels.myDay::deleteDailyPlan,
                            onDuplicate = viewModels.myDay::duplicateDailyPlanItem,
                            onStartSprint = viewModels.myDay::startNewSprintFromEditor,
                            onStartOngoingSprint = viewModels.myDay::startOngoingSprintFromEditor,
                            onUpgradeToTask = {
                                val current = myDayUiState.itemEditor
                                viewModels.myDay.dismissDailyPlanEditor()
                                if (current != null && current.itemId != null) {
                                    viewModels.task.openNewTaskFromDailyPlan(
                                        planItemId = current.itemId,
                                        title = current.title,
                                        note = current.note,
                                        label = current.label,
                                        tagIds = current.selectedTagIds
                                    )
                                }
                            }
                        )
                    }
                    myDayUiState.journalEditor?.let { editor ->
                        JournalEntryEditorSheet(
                            state = editor,
                            availableTags = myDayUiState.tags,
                            onDismiss = viewModels.myDay::dismissJournalEditor,
                            onLabelChange = viewModels.myDay::updateJournalEditorLabel,
                            onContentChange = viewModels.myDay::updateJournalEditorContent,
                            onPresetSelected = viewModels.myDay::applyJournalLabelPreset,
                            onMoodToggle = viewModels.myDay::toggleJournalEditorMood,
                            onTagToggle = viewModels.myDay::toggleJournalEditorTag,
                            onNewTagClick = viewModels.tag::openNewTag,
                            onSave = viewModels.myDay::saveJournalEditor,
                            onDelete = { editor.entryId?.let { viewModels.myDay.deleteJournalEntry(it) } }
                        )
                    }
                    if (showJournalHistory) {
                        JournalHistorySheet(
                            state = journalHistoryUiState,
                            onMoodToggle = viewModels.journalHistory::toggleMood,
                            onSearchTextChange = viewModels.journalHistory::updateSearchText,
                            onTagToggle = viewModels.journalHistory::toggleTag,
                            onEntryClick = { entry ->
                                showJournalHistory = false
                                viewModels.myDay.openJournalEditor(entry)
                            },
                            onGoalClick = { goal ->
                                showJournalHistory = false
                                viewModels.reflect.focusDay(goal.startDate)
                                navState.resetTo(AppRoute.Reflect)
                            },
                            onLoadMore = viewModels.journalHistory::loadOlder,
                            onDismiss = { showJournalHistory = false }
                        )
                    }
                    tagUiState.editor?.let { tagEditor ->
                        TagEditorSheet(
                            editor = tagEditor,
                            onDismiss = viewModels.tag::dismissEditor,
                            onSave = { viewModels.tag.saveEditor() },
                            onDelete = { viewModels.tag.deleteEditorTag() },
                            onNameChange = viewModels.tag::updateName,
                            onColorChange = viewModels.tag::updateColor
                        )
                    }
                    reflectEditorState?.let { editor ->
                        PeriodGoalEditorSheet(
                            editor = editor,
                            onReviewChange = viewModels.reflect::updateEditorReview,
                            onGoalChange = viewModels.reflect::updateEditorGoal,
                            onRatingChange = viewModels.reflect::updateEditorRating,
                            onMetricsChange = viewModels.reflect::updateEditorMetrics,
                            onSave = viewModels.reflect::saveEditor,
                            onDismiss = viewModels.reflect::dismissEditor
                        )
                    }
                }
            }
        }
    }
}
