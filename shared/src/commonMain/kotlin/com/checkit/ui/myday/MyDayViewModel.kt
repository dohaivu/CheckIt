package com.checkit.ui.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.SettingsRepository
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.JournalEntry
import com.checkit.domain.LeftoverAction
import com.checkit.domain.RoutineStepTemplate
import com.checkit.domain.SprintManager
import kotlinx.datetime.DayOfWeek
import com.checkit.domain.TaskItem
import com.checkit.domain.usecase.AddJournalEntryUseCase
import com.checkit.domain.usecase.AddSuggestedTaskToMyDayUseCase
import com.checkit.domain.usecase.BuildDayCloseSummaryUseCase
import com.checkit.domain.usecase.CarryOverDailyPlanItemsUseCase
import com.checkit.domain.usecase.CompleteDayCloseUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.DeleteJournalEntryUseCase
import com.checkit.domain.usecase.DeleteRoutineUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveJournalEntriesUseCase
import com.checkit.domain.usecase.ObserveNotesForDateUseCase
import com.checkit.domain.usecase.ObservePeriodGoalsUseCase
import com.checkit.domain.usecase.ObserveRoutineTodayUseCase
import com.checkit.domain.usecase.ObserveRoutinesUseCase
import com.checkit.domain.usecase.ResetStaleRoutineTodayUseCase
import com.checkit.domain.usecase.ObserveTagsUseCase
import com.checkit.domain.usecase.ObserveWorkingTasksUseCase
import com.checkit.domain.usecase.SaveRoutineUseCase
import com.checkit.domain.usecase.SmartScheduleDailyPlanUseCase
import com.checkit.domain.usecase.SprintTransitionUseCase
import com.checkit.domain.usecase.ToggleRoutineStepUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateJournalEntryUseCase
import com.checkit.domain.usecase.UpsertDailyPlanItemUseCase
import com.checkit.ui.UiEvent
import com.checkit.ui.currentMyDayTimeMinutes
import com.checkit.ui.journal.JournalController
import com.checkit.ui.journal.JournalPrompt
import com.checkit.ui.today
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/**
 * Orchestrates the My Day feature. Heavy lifting is split into small controllers:
 * [MyDayDataLoader], [DayCloseController], [PlanAssistController],
 * [DailyPlanEditorController], [SprintController], and [com.checkit.ui.journal.JournalController].
 */
class MyDayViewModel(
    observeDailyPlans: ObserveDailyPlansUseCase,
    observeJournalEntries: ObserveJournalEntriesUseCase,
    observePeriodGoals: ObservePeriodGoalsUseCase,
    observeTags: ObserveTagsUseCase,
    observeWorkingTasks: ObserveWorkingTasksUseCase,
    observeNotesForDate: ObserveNotesForDateUseCase,
    addJournalEntry: AddJournalEntryUseCase,
    updateJournalEntry: UpdateJournalEntryUseCase,
    deleteJournalEntry: DeleteJournalEntryUseCase,
    deleteDailyPlanItemUseCase: DeleteDailyPlanItemUseCase,
    settingsRepository: SettingsRepository,
    buildDayCloseSummary: BuildDayCloseSummaryUseCase,
    completeDayClose: CompleteDayCloseUseCase,
    carryOverDailyPlanItems: CarryOverDailyPlanItemsUseCase,
    upsertDailyPlanItem: UpsertDailyPlanItemUseCase,
    addSuggestedTaskToMyDay: AddSuggestedTaskToMyDayUseCase,
    updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
    smartSchedule: SmartScheduleDailyPlanUseCase,
    observeRoutines: ObserveRoutinesUseCase,
    observeRoutineToday: ObserveRoutineTodayUseCase,
    resetStaleRoutineToday: ResetStaleRoutineTodayUseCase,
    saveRoutine: SaveRoutineUseCase,
    deleteRoutine: DeleteRoutineUseCase,
    toggleRoutineStep: ToggleRoutineStepUseCase,
    val sprintManager: SprintManager,
    sprintTransition: SprintTransitionUseCase
) : ViewModel() {
    private val deps = MyDayDependencies(
        observeDailyPlans = observeDailyPlans,
        observeJournalEntries = observeJournalEntries,
        observePeriodGoals = observePeriodGoals,
        observeTags = observeTags,
        observeWorkingTasks = observeWorkingTasks,
        observeNotesForDate = observeNotesForDate,
        addJournalEntry = addJournalEntry,
        updateJournalEntry = updateJournalEntry,
        deleteJournalEntry = deleteJournalEntry,
        deleteDailyPlanItem = deleteDailyPlanItemUseCase,
        settingsRepository = settingsRepository,
        buildDayCloseSummary = buildDayCloseSummary,
        completeDayClose = completeDayClose,
        carryOverDailyPlanItems = carryOverDailyPlanItems,
        upsertDailyPlanItem = upsertDailyPlanItem,
        addSuggestedTaskToMyDay = addSuggestedTaskToMyDay,
        updateDailyPlanItemTime = updateDailyPlanItemTime,
        smartSchedule = smartSchedule,
        sprintManager = sprintManager,
        sprintTransition = sprintTransition,
        observeRoutines = observeRoutines,
        observeRoutineToday = observeRoutineToday,
        resetStaleRoutineToday = resetStaleRoutineToday,
        saveRoutine = saveRoutine,
        deleteRoutine = deleteRoutine,
        toggleRoutineStep = toggleRoutineStep
    )

    private val state = MyDayStateHolder(viewModelScope)
    val uiState: StateFlow<MyDayUiState> = state.uiState
    val events: Flow<UiEvent> = state.events

    private val loader = MyDayDataLoader(deps, state, viewModelScope)
    private val routines = RoutineController(deps, state, viewModelScope)
    private val dayClose = DayCloseController(deps, state, viewModelScope)
    private val planAssist = PlanAssistController(deps, state, viewModelScope)
    private val dailyPlanEditor = DailyPlanEditorController(deps, state, viewModelScope)
    private val sprints = SprintController(deps, state, viewModelScope)
    private val smartScheduler = SmartSchedulerController(deps, state, viewModelScope)
    private val journal = JournalController(deps, state, viewModelScope)

    init {
        loader.start()
        routines.start()
    }

    fun selectView(view: MyDayView) {
        state.update { it.copy(selectedView = view) }
    }

    // Routines
    fun toggleRoutineStep(routineId: String, stepId: String) = routines.toggleStep(routineId, stepId)
    /**
     * Re-pins all day-scoped queries to the current day; called on
     * foreground return so a new day loads without an app restart.
     */
    fun refreshToday() {
        loader.start()
        routines.refreshToday()
    }
    fun saveRoutine(
        id: String?,
        title: String,
        description: String,
        reminderMinutes: Int?,
        activeWeekdays: Set<DayOfWeek>,
        steps: List<RoutineStepTemplate>
    ) = routines.saveRoutine(id, title, description, reminderMinutes, activeWeekdays, steps)
    fun deleteRoutine(id: String) = routines.deleteRoutine(id)

    // Day review
    fun openDayClose() = dayClose.open()
    fun dismissDayClose() = dayClose.dismiss()
    fun setLeftoverAction(itemId: String, action: LeftoverAction) = dayClose.setLeftoverAction(itemId, action)
    fun updateWinNote(note: String) = dayClose.updateWinNote(note)
    fun updateDayRating(rating: Float) = dayClose.updateRating(rating)
    fun updateTomorrowGoal(goal: String) = dayClose.updateTomorrowGoal(goal)
    fun confirmDayClose() = dayClose.confirm()

    // Plan assist / suggestions
    fun openSuggestions(startTimeMinutes: Int? = null, endTimeMinutes: Int? = null) = planAssist.openSuggestions(startTimeMinutes, endTimeMinutes)
    fun dismissSuggestions() = planAssist.dismissSuggestions()
    fun addTaskFromSuggestion(task: TaskItem) = planAssist.addTaskFromSuggestion(task)
    fun addTaskToMyDay(task: TaskItem) = planAssist.addTaskToMyDay(task)
    fun addDailyPlanItem(title: String, tagIds: List<String>, nestedListItemId: String? = null) = planAssist.addDailyPlanItem(title, tagIds, nestedListItemId)
    fun carryAllYesterdayLeftovers() = planAssist.carryAllYesterdayLeftovers()
    fun carryYesterdayLeftover(item: DailyPlanItem) = planAssist.carryYesterdayLeftover(item)

    // Smart scheduler
    fun smartSchedule() = smartScheduler.scheduleAll()

    // Journal
    fun openJournalList() = journal.openJournalList()
    fun dismissJournalList() = journal.dismissJournalList()
    fun openNewJournalEntry() = journal.openNewJournalEntry()
    fun openNewJournalEntryWithPrompt(promptId: String) = journal.openNewJournalEntryWithPrompt(promptId)
    fun openJournalEditor(entry: JournalEntry) = journal.openJournalEditor(entry)
    fun dismissJournalEditor() = journal.dismissJournalEditor()
    fun updateJournalEditorLabel(value: String) = journal.updateJournalEditorLabel(value)
    fun updateJournalEditorContent(value: String) = journal.updateJournalEditorContent(value)
    fun applyJournalLabel(label: String) = journal.applyJournalLabel(label)
    fun applyJournalPrompt(prompt: JournalPrompt) = journal.applyJournalPrompt(prompt)
    fun clearJournalPrompt() = journal.clearJournalPrompt()
    fun discardJournalDraft() = journal.discardJournalDraft()
    fun toggleJournalEditorMood(mood: String) = journal.toggleJournalEditorMood(mood)
    fun toggleJournalEditorTag(tagId: String) = journal.toggleJournalEditorTag(tagId)
    fun saveJournalEditor() = journal.saveJournalEditor()
    fun deleteJournalEntry(entryId: String) = journal.deleteJournalEntry(entryId)

    // Daily plan item editor
    fun updateItemTime(item: DailyPlanItem, startTimeMinutes: Int, endTimeMinutes: Int) = dailyPlanEditor.updateItemTime(item, startTimeMinutes, endTimeMinutes)
    fun openNewDailyPlan(title: String, tagIds: List<String>, nestedListItemId: String? = null) = dailyPlanEditor.openNewDailyPlan(title, tagIds, nestedListItemId)
    fun openNewDailyPlan(startTimeMinutes: Int? = null, endTimeMinutes: Int? = null, date: LocalDate = today()) = dailyPlanEditor.openNewDailyPlan(startTimeMinutes, endTimeMinutes, date)
    fun dismissDailyPlanEditor() = dailyPlanEditor.dismissDailyPlanEditor()
    fun addDailyPlan() = dailyPlanEditor.addDailyPlan()
    fun saveDailyPlan(editor: DailyPlanItemEditorState): Boolean = dailyPlanEditor.saveDailyPlan(editor)
    fun openItemEditor(item: DailyPlanItem, date: LocalDate) = dailyPlanEditor.openItemEditor(item, date)
    fun updateTitle(title: String) = dailyPlanEditor.updateTitle(title)
    fun updateNote(note: String) = dailyPlanEditor.updateNote(note)
    fun updateLabel(label: String) = dailyPlanEditor.updateLabel(label)
    fun updateStatus(isDone: Boolean) = dailyPlanEditor.updateStatus(isDone)
    fun updateEditorSource(source: DailyPlanItemSource) = dailyPlanEditor.updateEditorSource(source)
    fun updateDate(date: LocalDate?) = dailyPlanEditor.updateDate(date)
    fun updateTime(startTimeMinutes: Int?, endTimeMinutes: Int?) = dailyPlanEditor.updateTime(startTimeMinutes, endTimeMinutes)
    fun toggleTag(tagId: String) = dailyPlanEditor.toggleTag(tagId)
    fun deleteDailyPlan() = dailyPlanEditor.deleteDailyPlan()
    fun deleteDailyPlanItem(itemId: String) = dailyPlanEditor.deleteDailyPlanItem(itemId)
    fun duplicateDailyPlanItem() = dailyPlanEditor.duplicateDailyPlanItem()

    // Sprints
    fun executeFabAction(action: FabAction) = sprints.executeFabAction(action)
    fun startSprint(taskId: String? = null, dailyPlanItemId: String? = null, description: String = "", tagIds: List<String> = emptyList()) = sprints.startSprint(taskId, dailyPlanItemId, description, tagIds)
    fun startSprintByItemId(itemId: String? = null) = sprints.startSprintByItemId(itemId)
    fun startSprintWithTask(task: TaskItem) = sprints.startSprintWithTask(task)
    fun startSprintWithChoice(choice: SprintChoice) = sprints.startSprintWithChoice(choice)
    fun startSprintForItem(item: DailyPlanItem) = sprints.startSprintForItem(item)
    fun startOngoingSprintForItem(item: DailyPlanItem) = sprints.startOngoingSprintForItem(item)
    fun startNewSprintFromEditor() = sprints.startNewSprintFromEditor()
    fun startOngoingSprintFromEditor() = sprints.startOngoingSprintFromEditor()
    fun openQuickSprint() = sprints.openQuickSprint()
    fun dismissQuickSprint() = sprints.dismissQuickSprint()
    fun pauseSprint() = sprints.pauseSprint()
    fun resumeSprint() = sprints.resumeSprint()
    fun completeSprint() = sprints.completeSprint()
    fun upgradeToPomodoro() = sprints.upgradeToPomodoro()
    fun saveSprintAsWin() = sprints.saveSprintAsWin()
    fun saveAndBreak() = sprints.saveAndBreak()
    fun continueNewPomodoro() = sprints.continueNewPomodoro()
    fun startNextPomodoro() = sprints.startNextPomodoro()
    fun dismissFinishedSprint() = sprints.dismissFinishedSprint()

    fun createFromTimelineRange(startTimeMinutes: Int, endTimeMinutes: Int) {
        if (startTimeMinutes < currentMyDayTimeMinutes()) {
            dailyPlanEditor.openNewDailyPlan(startTimeMinutes, endTimeMinutes)
        } else {
            planAssist.openSuggestions(startTimeMinutes, endTimeMinutes)
        }
    }
}
