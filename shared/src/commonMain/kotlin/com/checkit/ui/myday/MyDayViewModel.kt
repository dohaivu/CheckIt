package com.checkit.ui.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.*
import com.checkit.domain.DefaultTaskDurationMinutes
import com.checkit.domain.usecase.*
import com.checkit.data.*
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.UiEvent
import com.checkit.ui.duration
import com.checkit.ui.today
import com.checkit.ui.currentMyDayTimeMinutes
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import com.checkit.domain.SprintManager
import com.checkit.domain.usecase.AddTaskUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class MyDayViewModel(
    private val observeTaskBoard: ObserveTaskBoardUseCase,
    private val observeDailyPlans: ObserveDailyPlansUseCase,
    private val ensureDefaultTaskData: EnsureDefaultTaskDataUseCase,
    private val deleteDailyPlanItemUseCase: DeleteDailyPlanItemUseCase,
    private val settingsRepository: SettingsRepository,
    private val buildDayReviewSummary: BuildDayReviewSummaryUseCase,
    private val completeDayReview: CompleteDayReviewUseCase,
    private val carryOverDailyPlanItems: CarryOverDailyPlanItemsUseCase,
    private val upsertDailyPlanItem: UpsertDailyPlanItemUseCase,
    private val addSuggestedTaskToMyDay: AddSuggestedTaskToMyDayUseCase,
    private val syncKeyResultFromDailyPlan: SyncKeyResultFromDailyPlanUseCase,
    private val updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
    val sprintManager: SprintManager,
    private val sprintTransition: SprintTransitionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyDayUiState())
    val uiState: StateFlow<MyDayUiState> = _uiState.asStateFlow()
    private var pendingEditorTextSaveJob: Job? = null
    private val autoCarryMutex = Mutex()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            ensureDefaultTaskData()
            combine(
                observeTaskBoard(),
                observeDailyPlans(),
                settingsRepository.settings
            ) { board, dailyPlans, settings ->
                Triple(board, dailyPlans, settings)
            }
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                    sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to load My Day"))
                }
                .collect { (board, dailyPlans, settings) ->
                    val date = today()
                    val todayEpoch = date.toEpochDays().toInt()
                    val nowMinutes = currentMyDayTimeMinutes()
                    val plan = dailyPlans.firstOrNull { it.date == date }
                    val leftovers = YesterdayLeftovers.items(dailyPlans, date)
                    val pendingLeftovers = YesterdayLeftovers.pendingForToday(leftovers, plan)
                    val showReviewBanner = DayReviewBannerPolicy.shouldShow(
                        hasPlanItems = plan?.items?.isNotEmpty() == true,
                        reviewReminderEnabled = settings.reviewReminderEnabled,
                        reviewReminderTimeMinutes = settings.reviewReminderTimeMinutes,
                        lastDayReviewEpochDay = settings.lastDayReviewEpochDay,
                        todayEpochDay = todayEpoch,
                        nowMinutes = nowMinutes
                    )
                    val showLeftoversBanner = LeftoversBannerPolicy.shouldShow(
                        pendingCount = pendingLeftovers.size,
                        leftoversBannerDismissedEpochDay = settings.leftoversBannerDismissedEpochDay,
                        todayEpochDay = todayEpoch
                    )
                    val showPlanAssist = PlanAssistBannerPolicy.shouldShow(
                        todayPlanItemCount = plan?.items?.size ?: 0,
                        planReminderEnabled = settings.planReminderEnabled,
                        planReminderTimeMinutes = settings.planReminderTimeMinutes,
                        reviewReminderTimeMinutes = settings.reviewReminderTimeMinutes,
                        lastDayPlanDismissedEpochDay = settings.lastDayPlanDismissedEpochDay,
                        todayEpochDay = todayEpoch,
                        nowMinutes = nowMinutes
                    )
                    maybeAutoCarryOver(settings, pendingLeftovers, date)
                    val review = _uiState.value.dayReview?.let { existing ->
                        val summary = buildDayReviewSummary(date, plan)
                        val validIds = summary.plannedItems.map { it.id }.toSet()
                        existing.copy(
                            summary = summary,
                            leftoverActions = existing.leftoverActions.filterKeys { it in validIds },
                            winNoteItemId = existing.winNoteItemId ?: summary.winNoteItemId
                        )
                    }
                    _uiState.update { state ->
                        state.copy(
                            board = board,
                            dailyPlans = dailyPlans,
                            dayReview = review,
                            showDayReviewBanner = showReviewBanner && review == null,
                            reviewReminderEnabled = settings.reviewReminderEnabled,
                            reviewReminderTimeMinutes = settings.reviewReminderTimeMinutes,
                            planReminderEnabled = settings.planReminderEnabled,
                            planReminderTimeMinutes = settings.planReminderTimeMinutes,
                            lastDayReviewEpochDay = settings.lastDayReviewEpochDay,
                            lastDayPlanDismissedEpochDay = settings.lastDayPlanDismissedEpochDay,
                            leftoversBannerDismissedEpochDay = settings.leftoversBannerDismissedEpochDay,
                            autoCarryOverLeftovers = settings.autoCarryOverLeftovers,
                            yesterdayLeftovers = leftovers,
                            pendingYesterdayLeftovers = pendingLeftovers,
                            showLeftoversBanner = showLeftoversBanner &&
                                review == null &&
                                !state.showLeftoversSheet,
                            showPlanAssistBanner = showPlanAssist &&
                                review == null &&
                                !state.showSuggestions,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun maybeAutoCarryOver(
        settings: UserSettings,
        pendingLeftovers: List<DailyPlanItem>,
        today: LocalDate
    ) {
        if (!settings.autoCarryOverLeftovers) return
        if (pendingLeftovers.isEmpty()) return
        val todayEpoch = today.toEpochDays().toInt()
        if (settings.autoCarryOverLastRunEpochDay == todayEpoch) return
        viewModelScope.launch {
            autoCarryMutex.withLock {
                runCatching {
                    val result = carryOverDailyPlanItems.carryAll(
                        items = pendingLeftovers,
                        toDate = today,
                        timePolicy = CarryOverTimePolicy.ClearTimes
                    )
                    settingsRepository.setAutoCarryOverLastRunEpochDay(todayEpoch)
                    settingsRepository.setLeftoversBannerDismissedEpochDay(todayEpoch)
                    if (result.carriedCount > 0) {
                        sendEvent(UiEvent.ShowSnackbar("${result.carriedCount} carried from yesterday"))
                    }
                }
            }
        }
    }

    fun openDayReview() {
        val state = _uiState.value
        val date = state.today
        viewModelScope.launch {
            val summary = buildDayReviewSummary(date, state.plan)
            val defaults = summary.plannedItems.associate { it.id to LeftoverAction.CarryOver }
            _uiState.update {
                it.copy(
                    dayReview = DayReviewUiState(
                        summary = summary,
                        leftoverActions = defaults,
                        winNote = summary.winNote,
                        winNoteItemId = summary.winNoteItemId
                    ),
                    showDayReviewBanner = false,
                    showLeftoversSheet = false,
                    showSuggestions = false,
                    itemEditor = null
                )
            }
        }
    }

    fun dismissDayReview() {
        _uiState.update { it.copy(dayReview = null) }
    }

    fun openLeftoversSheet() {
        _uiState.update {
            it.copy(
                showLeftoversSheet = true,
                showLeftoversBanner = false,
                showSuggestions = false,
                itemEditor = null
            )
        }
    }

    fun dismissLeftoversSheet() {
        _uiState.update { it.copy(showLeftoversSheet = false) }
    }

    fun dismissLeftoversBanner() {
        val todayEpoch = today().toEpochDays().toInt()
        viewModelScope.launch {
            settingsRepository.setLeftoversBannerDismissedEpochDay(todayEpoch)
        }
        _uiState.update {
            it.copy(
                showLeftoversBanner = false,
                leftoversBannerDismissedEpochDay = todayEpoch
            )
        }
    }

    fun carryAllYesterdayLeftovers() {
        val state = _uiState.value
        val items = state.pendingYesterdayLeftovers
        if (items.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                carryOverDailyPlanItems.carryAll(
                    items = items,
                    toDate = state.today,
                    timePolicy = CarryOverTimePolicy.ClearTimes
                )
            }.onSuccess { result ->
                val todayEpoch = state.today.toEpochDays().toInt()
                settingsRepository.setLeftoversBannerDismissedEpochDay(todayEpoch)
                settingsRepository.setAutoCarryOverLastRunEpochDay(todayEpoch)
                _uiState.update {
                    it.copy(
                        showLeftoversBanner = false,
                        showLeftoversSheet = false
                    )
                }
                sendEvent(
                    UiEvent.ShowSnackbar(
                        when {
                            result.carriedCount > 0 && result.skippedCount > 0 ->
                                "${result.carriedCount} carried · ${result.skippedCount} already on today"
                            result.carriedCount > 0 ->
                                "${result.carriedCount} carried from yesterday"
                            else -> "Nothing new to carry"
                        }
                    )
                )
            }.onFailure { error ->
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to carry leftovers"))
            }
        }
    }

    fun carryYesterdayLeftover(item: DailyPlanItem) {
        viewModelScope.launch {
            runCatching {
                carryOverDailyPlanItems(
                    items = listOf(item),
                    itemIds = setOf(item.id),
                    toDate = today(),
                    timePolicy = CarryOverTimePolicy.ClearTimes
                )
            }.onSuccess { result ->
                sendEvent(
                    UiEvent.ShowSnackbar(
                        if (result.carriedCount > 0) "Carried to today" else "Already on today"
                    )
                )
            }.onFailure { error ->
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to carry item"))
            }
        }
    }

    fun openPlanAssist() {
        _uiState.update {
            it.copy(
                showPlanAssistBanner = false,
                showSuggestions = true,
                showLeftoversSheet = false,
                suggestionStartTimeMinutes = null,
                suggestionEndTimeMinutes = null,
                itemEditor = null,
                dayReview = null
            )
        }
    }

    fun dismissPlanAssist() {
        val todayEpoch = today().toEpochDays().toInt()
        viewModelScope.launch {
            settingsRepository.setLastDayPlanDismissedEpochDay(todayEpoch)
        }
        _uiState.update {
            it.copy(
                showPlanAssistBanner = false,
                lastDayPlanDismissedEpochDay = todayEpoch
            )
        }
    }

    fun setLeftoverAction(itemId: Long, action: LeftoverAction) {
        _uiState.update { state ->
            val review = state.dayReview ?: return@update state
            state.copy(
                dayReview = review.copy(
                    leftoverActions = review.leftoverActions + (itemId to action)
                )
            )
        }
    }

    fun updateWinNote(note: String) {
        _uiState.update { state ->
            val review = state.dayReview ?: return@update state
            state.copy(dayReview = review.copy(winNote = note))
        }
    }

    fun updateTomorrowGoal(goal: String) {
        _uiState.update { state ->
            val review = state.dayReview ?: return@update state
            state.copy(dayReview = review.copy(tomorrowGoal = goal))
        }
    }

    fun confirmDayReview() {
        val state = _uiState.value
        val review = state.dayReview ?: return
        if (review.isSubmitting) return
        _uiState.update { it.copy(dayReview = review.copy(isSubmitting = true)) }
        viewModelScope.launch {
            completeDayReview(
                plan = state.plan,
                input = DayReviewConfirmInput(
                    date = review.summary.date,
                    leftoverActions = review.leftoverActions,
                    winNote = review.winNote,
                    winNoteItemId = review.winNoteItemId,
                    tomorrowGoal = review.tomorrowGoal
                )
            ).onSuccess { result ->
                _uiState.update { it.copy(dayReview = null, showDayReviewBanner = false, showCelebration = true) }
                viewModelScope.launch {
                    delay(3000.milliseconds)
                    _uiState.update { it.copy(showCelebration = false) }
                }
                val parts = buildList {
                    if (result.carriedCount > 0) add("${result.carriedCount} carried to tomorrow")
                    if (result.markedDoneCount > 0) add("${result.markedDoneCount} marked done")
                    if (result.droppedCount > 0) add("${result.droppedCount} left unfinished")
                    if (result.winNoteSaved) add("win saved")
                }
                sendEvent(
                    UiEvent.ShowSnackbar(
                        if (parts.isEmpty()) "Day reviewed" else parts.joinToString(" · ")
                    )
                )
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        dayReview = current.dayReview?.copy(isSubmitting = false)
                    )
                }
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to finish review"))
            }
        }
    }

    fun selectView(view: MyDayView) {
        _uiState.update { it.copy(selectedView = view) }
    }

    fun updateItemTime(item: DailyPlanItem, startTimeMinutes: Int, endTimeMinutes: Int) {
        val nextEndTime = if (item.source.hasEndTime()) endTimeMinutes else null
        viewModelScope.launch {
            syncKeyResultFromDailyPlan(itemId = item.id, proposedStartTime = startTimeMinutes, proposedEndTime = nextEndTime)
            updateDailyPlanItemTime(item.id, startTimeMinutes, nextEndTime)
        }
    }

    /** PR3: open check-in with a free time slot around now (notification deep link). */
    fun openCheckInAtFreeSlot() {
        val state = _uiState.value
        val duration = DefaultTaskDurationMinutes
        val preferredStart = currentMyDayTimeMinutes()
        val (start, end) = nextAvailableTimeRange(preferredStart, duration, state.items)
        openCheckIn(startTimeMinutes = start, endTimeMinutes = end)
    }

    fun openCheckIn(
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null,
        date: LocalDate = today()
    ) {
        cancelPendingEditorTextSave()
        _uiState.update {
            it.copy(
                itemEditor = DailyPlanItemEditorState(
                    date = date,
                    source = DailyPlanItemSource.MyDayNote,
                    status = DailyPlanItemStatus.Planned,
                    startTimeMinutes = startTimeMinutes,
                    endTimeMinutes = endTimeMinutes
                )
            )
        }
    }
    fun dismissCheckIn() {
        flushPendingEditorTextSave()
        _uiState.update { it.copy(itemEditor = null) }
    }

    fun addCheckIn() {
        val editor = _uiState.value.itemEditor ?: return

        if (!saveCheckIn(editor)) return

        _uiState.update { it.copy(itemEditor = null) }
        sendEvent(UiEvent.ShowSnackbar("Saved"))
    }

    fun saveCheckIn(editor: DailyPlanItemEditorState): Boolean {
        viewModelScope.launch {
            upsertDailyPlanItem(editor).onFailure { error ->
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save"))
            }
        }
        return true
    }

    fun openSuggestions(
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null
    ) {
        _uiState.update {
            it.copy(
                showSuggestions = true,
                showPlanAssistBanner = false,
                suggestionStartTimeMinutes = startTimeMinutes,
                suggestionEndTimeMinutes = endTimeMinutes
            )
        }
    }
    fun dismissSuggestions() {
        _uiState.update {
            it.copy(
                showSuggestions = false,
                suggestionStartTimeMinutes = null,
                suggestionEndTimeMinutes = null
            )
        }
    }
    fun addTaskFromSuggestion(task: TaskItem) {
        addTaskToMyDay(task, clearSuggestions = true)
    }

    fun addTaskToMyDay(task: TaskItem) {
        addTaskToMyDay(task, clearSuggestions = false)
    }

    private fun addTaskToMyDay(
        task: TaskItem,
        clearSuggestions: Boolean
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            addSuggestedTaskToMyDay(
                task = task,
                suggestionStart = state.suggestionStartTimeMinutes,
                suggestionEnd = state.suggestionEndTimeMinutes
            ).onSuccess {
                _uiState.update { current ->
                    if (clearSuggestions) {
                        current.copy(
                            showSuggestions = false,
                            suggestionStartTimeMinutes = null,
                            suggestionEndTimeMinutes = null
                        )
                    } else {
                        current
                    }
                }
                sendEvent(UiEvent.ShowSnackbar("Added to My Day"))
            }.onFailure { error ->
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to add task"))
            }
        }
    }

    fun createFromTimelineRange(startTimeMinutes: Int, endTimeMinutes: Int) {
        if (startTimeMinutes < currentMyDayTimeMinutes()) {
            openCheckIn(startTimeMinutes, endTimeMinutes)
        } else {
            openSuggestions(startTimeMinutes, endTimeMinutes)
        }
    }

    fun openItemEditor(item: DailyPlanItem, date: LocalDate) {
        cancelPendingEditorTextSave()
        _uiState.update {
            it.copy(
                itemEditor = DailyPlanItemEditorState(
                    mode = EditorMode.Edit,
                    itemId = item.id,
                    taskId = item.taskId,
                    date = date,
                    source = item.source,
                    title = item.title,
                    note = item.note.orEmpty(),
                    status = item.status,
                    startTimeMinutes = item.startTimeMinutes,
                    endTimeMinutes = item.endTimeMinutes,
                    selectedTagIds = item.tags.map { it.id }.toSet()
                )
            )
        }
    }
    fun updateTitle(title: String) = updateItemEditor(saveImmediately = false) { it.copy(title = title) }
    fun updateNote(note: String) = updateItemEditor(saveImmediately = false) { it.copy(note = note) }
    fun updateStatus(isDone: Boolean) = updateItemEditor {
        it.copy(status = if (isDone) DailyPlanItemStatus.Done else DailyPlanItemStatus.Planned)
    }
    fun updateEditorSource(source: DailyPlanItemSource) = updateItemEditor {
        it.copy(
            source = source,
            status = if (it.isAddMode) source.inferredAddStatus(it.startTimeMinutes) else source.defaultStatus(),
            endTimeMinutes = if (source.hasEndTime()) it.endTimeMinutes else null
        )
    }
    fun updateStartTime(timeMinutes: Int?) = updateItemEditor {
        it.copy(
            startTimeMinutes = timeMinutes,
            status = if (it.isAddMode) it.source.inferredAddStatus(timeMinutes) else it.status
        )
    }
    fun updateEndTime(timeMinutes: Int?) = updateItemEditor { it.copy(endTimeMinutes = timeMinutes) }
    fun toggleTag(tagId: Long) = updateItemEditor {
        val newTagIds = if (it.selectedTagIds.contains(tagId)) {
            it.selectedTagIds - tagId
        } else {
            it.selectedTagIds + tagId
        }
        it.copy(selectedTagIds = newTagIds)
    }

    fun deleteEditorItem() {
        cancelPendingEditorTextSave()
        val itemId = _uiState.value.itemEditor?.itemId ?: return
        deleteDailyPlanItem(itemId) {
            it.copy(itemEditor = null)
        }
        sendEvent(UiEvent.ShowSnackbar("Deleted"))
    }

    fun deleteDailyPlanItem(itemId: Long) {
        deleteDailyPlanItem(itemId) {
            it
        }
        sendEvent(UiEvent.ShowSnackbar("Removed from My Day"))
    }

    private fun deleteDailyPlanItem(
        itemId: Long,
        updateState: (MyDayUiState) -> MyDayUiState
    ) {
        viewModelScope.launch {
            deleteDailyPlanItemUseCase(itemId)
            _uiState.update(updateState)
        }
    }

    fun startSprint(taskId: Long? = null, dailyPlanItemId: Long? = null, description: String = "", tagIds: List<Long> = emptyList()) {
        dismissQuickSprint()
        if (!sprintManager.startSprint(taskId, dailyPlanItemId, description, tagIds = tagIds)) {
            sendEvent(UiEvent.ShowSnackbar("A sprint is already in progress"))
        }
    }

    fun startSprintWithTask(task: TaskItem) {
        dismissQuickSprint()
        if (!sprintManager.startSprint(task.id, null, task.name, tagIds = task.tags.map { it.id })) {
            sendEvent(UiEvent.ShowSnackbar("A sprint is already in progress"))
        }
    }

    fun startSprintWithChoice(choice: SprintChoice) {
        dismissQuickSprint()
        val success = when (choice) {
            is SprintChoice.Task -> sprintManager.startSprint(
                choice.task.id,
                null,
                choice.task.name,
                tagIds = choice.task.tags.map { it.id }
            )
            is SprintChoice.PlanItem -> {
                // If the item is already Done, we start a NEW session (new daily plan item) on finish.
                val itemId = if (choice.item.status == DailyPlanItemStatus.Done) null else choice.item.id
                sprintManager.startSprint(
                    choice.item.taskId,
                    itemId,
                    choice.item.title,
                    tagIds = choice.item.tags.map { it.id }
                )
            }
        }
        if (!success) {
            sendEvent(UiEvent.ShowSnackbar("A sprint is already in progress"))
        }
    }

    fun openQuickSprint() {
        _uiState.update { it.copy(showQuickSprintSheet = true) }
    }

    fun dismissQuickSprint() {
        _uiState.update { it.copy(showQuickSprintSheet = false) }
    }

    fun pauseSprint() = sprintManager.pauseSprint()
    fun resumeSprint() = sprintManager.resumeSprint()
    fun completeSprint() = sprintManager.completeSprintManually()

    fun upgradeToPomodoro() {
        viewModelScope.launch { sprintTransition.upgradeToPomodoro() }
    }

    fun saveSprintAsWin() {
        viewModelScope.launch { sprintTransition.saveWin() }
    }

    fun saveAndBreak() {
        viewModelScope.launch { sprintTransition.saveAndBreak() }
    }

    fun continueNewPomodoro() {
        viewModelScope.launch { sprintTransition.saveAndContinue() }
    }

    fun startNextPomodoro() {
        viewModelScope.launch { sprintTransition.startNext() }
    }

    fun dismissFinishedSprint() = sprintManager.dismissFinished()

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun updateItemEditor(
        saveImmediately: Boolean = true,
        transform: (DailyPlanItemEditorState) -> DailyPlanItemEditorState
    ) {
        var updatedEditor: DailyPlanItemEditorState? = null
        _uiState.update { state ->
            state.itemEditor?.let {
                updatedEditor = transform(it)
                state.copy(itemEditor = updatedEditor)
            } ?: state
        }
        val editor = updatedEditor ?: return
        if (!editor.isEditMode) return
        if (saveImmediately) {
            cancelPendingEditorTextSave()
            saveCheckIn(editor)
        } else {
            scheduleEditorTextSave()
        }
    }

    private fun scheduleEditorTextSave() {
        pendingEditorTextSaveJob?.cancel()
        pendingEditorTextSaveJob = viewModelScope.launch {
            delay(EditorTextSaveDebounceMillis.milliseconds)
            pendingEditorTextSaveJob = null
            saveCurrentEditor()
        }
    }

    private fun flushPendingEditorTextSave() {
        val pendingSave = pendingEditorTextSaveJob ?: return
        pendingSave.cancel()
        pendingEditorTextSaveJob = null
        saveCurrentEditor()
    }

    private fun cancelPendingEditorTextSave() {
        pendingEditorTextSaveJob?.cancel()
        pendingEditorTextSaveJob = null
    }

    private fun saveCurrentEditor() {
        val editor = _uiState.value.itemEditor?.takeIf { it.isEditMode } ?: return
        saveCheckIn(editor)
    }
    
    companion object {
        private const val EditorTextSaveDebounceMillis = 600L
    }
}
