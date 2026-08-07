package com.checkit.ui.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.SettingsRepository
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.JournalEntry
import com.checkit.domain.LeftoverAction
import com.checkit.domain.SprintManager
import com.checkit.domain.TaskItem
import com.checkit.domain.usecase.AddJournalEntryUseCase
import com.checkit.domain.usecase.AddSuggestedTaskToMyDayUseCase
import com.checkit.domain.usecase.BuildDayReviewSummaryUseCase
import com.checkit.domain.usecase.CarryOverDailyPlanItemsUseCase
import com.checkit.domain.usecase.CompleteDayReviewUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.DeleteJournalEntryUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveDayReviewsUseCase
import com.checkit.domain.usecase.ObserveJournalEntriesUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.SmartScheduleDailyPlanUseCase
import com.checkit.domain.usecase.SprintTransitionUseCase
import com.checkit.domain.usecase.SyncKeyResultFromDailyPlanUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateJournalEntryUseCase
import com.checkit.domain.usecase.UpsertDailyPlanItemUseCase
import com.checkit.ui.UiEvent
import com.checkit.ui.currentMyDayTimeMinutes
import com.checkit.ui.today
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/**
 * Orchestrates the My Day feature. Heavy lifting is split into small controllers:
 * [MyDayDataLoader], [DayReviewController], [LeftoversController], [PlanAssistController],
 * [DailyPlanEditorController], [SprintController], and [JournalController].
 */
class MyDayViewModel(
    observeTaskBoard: ObserveTaskBoardUseCase,
    observeDailyPlans: ObserveDailyPlansUseCase,
    observeJournalEntries: ObserveJournalEntriesUseCase,
    addJournalEntry: AddJournalEntryUseCase,
    updateJournalEntry: UpdateJournalEntryUseCase,
    deleteJournalEntry: DeleteJournalEntryUseCase,
    deleteDailyPlanItemUseCase: DeleteDailyPlanItemUseCase,
    settingsRepository: SettingsRepository,
    buildDayReviewSummary: BuildDayReviewSummaryUseCase,
    completeDayReview: CompleteDayReviewUseCase,
    carryOverDailyPlanItems: CarryOverDailyPlanItemsUseCase,
    observeDayReviews: ObserveDayReviewsUseCase,
    upsertDailyPlanItem: UpsertDailyPlanItemUseCase,
    addSuggestedTaskToMyDay: AddSuggestedTaskToMyDayUseCase,
    syncKeyResultFromDailyPlan: SyncKeyResultFromDailyPlanUseCase,
    updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
    smartSchedule: SmartScheduleDailyPlanUseCase,
    val sprintManager: SprintManager,
    sprintTransition: SprintTransitionUseCase
) : ViewModel() {
    private val deps = MyDayDependencies(
        observeTaskBoard = observeTaskBoard,
        observeDailyPlans = observeDailyPlans,
        observeJournalEntries = observeJournalEntries,
        addJournalEntry = addJournalEntry,
        updateJournalEntry = updateJournalEntry,
        deleteJournalEntry = deleteJournalEntry,
        deleteDailyPlanItem = deleteDailyPlanItemUseCase,
        settingsRepository = settingsRepository,
        buildDayReviewSummary = buildDayReviewSummary,
        completeDayReview = completeDayReview,
        carryOverDailyPlanItems = carryOverDailyPlanItems,
        observeDayReviews = observeDayReviews,
        upsertDailyPlanItem = upsertDailyPlanItem,
        addSuggestedTaskToMyDay = addSuggestedTaskToMyDay,
        syncKeyResultFromDailyPlan = syncKeyResultFromDailyPlan,
        updateDailyPlanItemTime = updateDailyPlanItemTime,
        smartSchedule = smartSchedule,
        sprintManager = sprintManager,
        sprintTransition = sprintTransition
    )

    private val state = MyDayStateHolder(viewModelScope)
    val uiState: StateFlow<MyDayUiState> = state.uiState
    val events: Flow<UiEvent> = state.events

    private val loader = MyDayDataLoader(deps, state, viewModelScope)
    private val dayReview = DayReviewController(deps, state, viewModelScope)
    private val leftovers = LeftoversController(deps, state, viewModelScope)
    private val planAssist = PlanAssistController(deps, state, viewModelScope)
    private val dailyPlanEditor = DailyPlanEditorController(deps, state, viewModelScope)
    private val sprints = SprintController(deps, state, viewModelScope)
    private val smartScheduler = SmartSchedulerController(deps, state, viewModelScope)
    private val journal = JournalController(deps, state, viewModelScope)

    init {
        loader.start()
    }

    fun selectView(view: MyDayView) {
        state.update { it.copy(selectedView = view) }
    }

    // Day review
    fun openDayReview() = dayReview.open()
    fun dismissDayReview() = dayReview.dismiss()
    fun setLeftoverAction(itemId: Long, action: LeftoverAction) = dayReview.setLeftoverAction(itemId, action)
    fun updateWinNote(note: String) = dayReview.updateWinNote(note)
    fun updateTomorrowGoal(goal: String) = dayReview.updateTomorrowGoal(goal)
    fun confirmDayReview() = dayReview.confirm()

    // Leftovers
    fun openLeftoversSheet() = leftovers.openSheet()
    fun dismissLeftoversSheet() = leftovers.dismissSheet()
    fun dismissLeftoversBanner() = leftovers.dismissBanner()
    fun carryAllYesterdayLeftovers() = leftovers.carryAll()
    fun carryYesterdayLeftover(item: DailyPlanItem) = leftovers.carryItem(item)

    // Plan assist / suggestions
    fun openPlanAssist() = planAssist.openPlanAssist()
    fun dismissPlanAssist() = planAssist.dismissPlanAssist()
    fun openSuggestions(
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null
    ) = planAssist.openSuggestions(startTimeMinutes, endTimeMinutes)
    fun dismissSuggestions() = planAssist.dismissSuggestions()
    fun addTaskFromSuggestion(task: TaskItem) = planAssist.addTaskFromSuggestion(task)
    fun addTaskToMyDay(task: TaskItem) = planAssist.addTaskToMyDay(task)
    fun quickAddDailyPlanItem(title: String, tagIds: List<Long>) = planAssist.quickAddDailyPlanItem(title, tagIds)

    // Smart scheduler
    fun smartSchedule() = smartScheduler.scheduleAll()

    // Journal
    fun openJournalList(date: LocalDate? = null) = journal.openJournalList(date)
    fun dismissJournalList() = journal.dismissJournalList()
    fun openNewJournalEntry() = journal.openNewJournalEntry()
    fun openJournalEditor(entry: JournalEntry) = journal.openJournalEditor(entry)
    fun dismissJournalEditor() = journal.dismissJournalEditor()
    fun updateJournalEditorContext(value: String) = journal.updateJournalEditorContext(value)
    fun updateJournalEditorContent(value: String) = journal.updateJournalEditorContent(value)
    fun toggleJournalEditorMood(mood: String) = journal.toggleJournalEditorMood(mood)
    fun toggleJournalEditorTag(tagId: Long) = journal.toggleJournalEditorTag(tagId)
    fun saveJournalEditor() = journal.saveJournalEditor()
    fun deleteJournalEntry(entryId: Long) = journal.deleteJournalEntry(entryId)

    // Daily plan item editor
    fun updateItemTime(item: DailyPlanItem, startTimeMinutes: Int, endTimeMinutes: Int) =
        dailyPlanEditor.updateItemTime(item, startTimeMinutes, endTimeMinutes)
    fun openDailyPlan(
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null,
        date: LocalDate = today()
    ) = dailyPlanEditor.openDailyPlan(startTimeMinutes, endTimeMinutes, date)
    fun dismissDailyPlanEditor() = dailyPlanEditor.dismissDailyPlanEditor()
    fun addDailyPlan() = dailyPlanEditor.addDailyPlan()
    fun saveDailyPlan(editor: DailyPlanItemEditorState): Boolean = dailyPlanEditor.saveDailyPlan(editor)
    fun openItemEditor(item: DailyPlanItem, date: LocalDate) = dailyPlanEditor.openItemEditor(item, date)
    fun updateTitle(title: String) = dailyPlanEditor.updateTitle(title)
    fun updateNote(note: String) = dailyPlanEditor.updateNote(note)
    fun updateStatus(isDone: Boolean) = dailyPlanEditor.updateStatus(isDone)
    fun updateEditorSource(source: DailyPlanItemSource) = dailyPlanEditor.updateEditorSource(source)
    fun updateTime(startTimeMinutes: Int?, endTimeMinutes: Int?) = dailyPlanEditor.updateTime(startTimeMinutes, endTimeMinutes)
    fun toggleTag(tagId: Long) = dailyPlanEditor.toggleTag(tagId)
    fun deleteDailyPlan() = dailyPlanEditor.deleteDailyPlan()
    fun deleteDailyPlanItem(itemId: Long) = dailyPlanEditor.deleteDailyPlanItem(itemId)
    fun duplicateDailyPlanItem() = dailyPlanEditor.duplicateDailyPlanItem()

    // Sprints
    fun executeFabAction(action: FabAction) = sprints.executeFabAction(action)
    fun startSprint(taskId: Long? = null, dailyPlanItemId: Long? = null, description: String = "", tagIds: List<Long> = emptyList()) =
        sprints.startSprint(taskId, dailyPlanItemId, description, tagIds)
    fun startSprintByItemId(itemId: Long) = sprints.startSprintByItemId(itemId)
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
            dailyPlanEditor.openDailyPlan(startTimeMinutes, endTimeMinutes)
        } else {
            planAssist.openSuggestions(startTimeMinutes, endTimeMinutes)
        }
    }
}
