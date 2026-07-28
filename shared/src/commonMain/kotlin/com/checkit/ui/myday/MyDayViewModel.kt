package com.checkit.ui.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.*
import com.checkit.domain.usecase.*
import com.checkit.data.*
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.UiEvent
import com.checkit.ui.duration
import com.checkit.ui.today
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
    private val addTaskToDailyPlan: AddTaskToDailyPlanUseCase,
    private val addDailyPlanItem: AddDailyPlanItemUseCase,
    private val updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
    private val updateDailyPlanItemStatus: UpdateDailyPlanItemStatusUseCase,
    private val updateDailyPlanItem: UpdateDailyPlanItemUseCase,
    private val syncKeyResultFromDailyPlan: SyncKeyResultFromDailyPlanUseCase,
    private val deleteDailyPlanItemUseCase: DeleteDailyPlanItemUseCase,
    private val settingsRepository: SettingsRepository,
    private val buildDayReviewSummary: BuildDayReviewSummaryUseCase,
    private val completeDayReview: CompleteDayReviewUseCase,
    private val carryOverDailyPlanItems: CarryOverDailyPlanItemsUseCase,
    val sprintManager: SprintManager
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
        val (start, end) = state.nextAvailableTimeRange(preferredStart, duration)
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
        val title = editor.title.trim()
        val note = editor.note.trim()
        val source = editor.saveSource()
        val status = editor.saveStatus()
        when (source) {
            DailyPlanItemSource.MyDayNote -> {
                if (title.isBlank() && note.isBlank()) {
                    sendEvent(UiEvent.ShowSnackbar("Add a note"))
                    return false
                }
            }
            DailyPlanItemSource.MyDayReminder -> {
                when {
                    title.isBlank() -> {
                        sendEvent(UiEvent.ShowSnackbar("Add a reminder"))
                        return false
                    }
                    editor.startTimeMinutes == null -> {
                        sendEvent(UiEvent.ShowSnackbar("Add reminder time"))
                        return false
                    }
                }
            }
            DailyPlanItemSource.MyDayTask -> {
                val start = editor.startTimeMinutes
                val end = editor.endTimeMinutes
                when {
                    title.isBlank() -> {
                        sendEvent(UiEvent.ShowSnackbar("Add a done item"))
                        return false
                    }
                    start == null || end == null -> {
                        sendEvent(UiEvent.ShowSnackbar("Add start and end time"))
                        return false
                    }
                    end <= start -> {
                        sendEvent(UiEvent.ShowSnackbar("End time must be after start"))
                        return false
                    }
                }
            }
            DailyPlanItemSource.ExistingTask -> Unit
        }
        viewModelScope.launch {
            if (editor.itemId == null) {
                addDailyPlanItem(
                    editor.date,
                    title,
                    note.takeIf { it.isNotBlank() },
                    editor.startTimeMinutes,
                    if (source.hasEndTime()) editor.endTimeMinutes else null,
                    source,
                    status = status,
                    tagIds = editor.selectedTagIds.toList()
                )
            } else {
                syncKeyResultFromDailyPlan(
                    itemId = editor.itemId,
                    proposedStatus = status,
                    proposedStartTime = editor.startTimeMinutes,
                    proposedEndTime = if (source.hasEndTime()) editor.endTimeMinutes else null
                )
                updateDailyPlanItem(
                    editor.itemId,
                    editor.toWriteInput(status, source)
                )
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
            val (startTimeMinutes, endTimeMinutes) = state.selectedSuggestionTimeRangeFor(task)
            val itemId = addTaskToDailyPlan(today(), task)
            if (startTimeMinutes != task.startTimeMinutes || endTimeMinutes != task.endTimeMinutes) {
                updateDailyPlanItemTime(itemId, startTimeMinutes, endTimeMinutes)
            }
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
        val current = sprintManager.state.value
        if (current is SprintState.Finished) {
            val originalStartTime = current.startTimeEpochMillis
            
            if (!sprintManager.startSprint(
                    taskId = current.taskId,
                    dailyPlanItemId = current.dailyPlanItemId,
                    description = current.description,
                    durationSeconds = 1500, // 25 minutes
                    isPomodoro = true,
                    startTimeEpochMillis = originalStartTime
                )
            ) {
                sendEvent(UiEvent.ShowSnackbar("A focus session is already in progress"))
            }
        }
    }

    fun saveSprintAsWin() {
        val current = sprintManager.takeFinished() ?: return
        viewModelScope.launch {
            performSaveSprintAsWin(current)
        }
    }

    private suspend fun performSaveSprintAsWin(current: SprintState.Finished): Long? {
        if (current.isBreak) return null
        return try {
            val todayDate = today()
            val startInstant = Instant.fromEpochMilliseconds(current.startTimeEpochMillis)
            val startDateTime = startInstant.toLocalDateTime(TimeZone.currentSystemDefault())
            val startMinutes = startDateTime.hour * 60 + startDateTime.minute
            val durationMinutes = (current.elapsedSeconds / 60).coerceAtLeast(1)
            val endMinutes = startMinutes + durationMinutes

            val taskId = current.taskId
            val dailyPlanItemId = current.dailyPlanItemId

            if (dailyPlanItemId != null) {
                if (_uiState.value.items.none { it.id == dailyPlanItemId }) {
                    sendEvent(UiEvent.ShowSnackbar("Could not save sprint: plan item no longer exists"))
                    return null
                }

                syncKeyResultFromDailyPlan(
                    itemId = dailyPlanItemId,
                    proposedStatus = DailyPlanItemStatus.Done,
                    proposedStartTime = startMinutes,
                    proposedEndTime = endMinutes
                )
                updateDailyPlanItemTime(dailyPlanItemId, startMinutes, endMinutes)
                updateDailyPlanItemStatus(dailyPlanItemId, DailyPlanItemStatus.Done)
                dailyPlanItemId
            } else if (taskId != null) {
                val task = _uiState.value.board.tasksById[taskId]
                if (task == null) {
                    sendEvent(UiEvent.ShowSnackbar("Could not save sprint: task no longer exists"))
                    return null
                }
                val itemId = addTaskToDailyPlan(todayDate, task)

                syncKeyResultFromDailyPlan(
                    itemId = itemId,
                    proposedStatus = DailyPlanItemStatus.Done,
                    proposedStartTime = startMinutes,
                    proposedEndTime = endMinutes
                )
                updateDailyPlanItemTime(itemId, startMinutes, endMinutes)
                updateDailyPlanItemStatus(itemId, DailyPlanItemStatus.Done)
                itemId
            } else {
                addDailyPlanItem(
                    date = todayDate,
                    title = current.description,
                    note = "Sprint session (${durationMinutes}m)",
                    startTimeMinutes = startMinutes,
                    endTimeMinutes = startMinutes + durationMinutes,
                    source = DailyPlanItemSource.MyDayTask,
                    status = DailyPlanItemStatus.Done,
                    tagIds = current.tagIds
                )
            }
        } catch (error: Exception) {
            sendEvent(UiEvent.ShowSnackbar(error.message ?: "Could not save sprint"))
            null
        }
    }

    fun saveAndBreak() {
        val current = sprintManager.takeFinished() ?: return
        viewModelScope.launch {
            performSaveSprintAsWin(current)
            sprintManager.startSprint(
                taskId = current.taskId,
                dailyPlanItemId = current.dailyPlanItemId,
                description = "Short Break",
                durationSeconds = 300,
                isPomodoro = false,
                isBreak = true
            )
        }
    }

    fun continueNewPomodoro() {
        val current = sprintManager.takeFinished() ?: return
        viewModelScope.launch {
            val savedItemId = performSaveSprintAsWin(current)
            val task = current.taskId?.let { _uiState.value.board.tasksById[it] }
            sprintManager.startSprint(
                taskId = current.taskId,
                dailyPlanItemId = savedItemId ?: current.dailyPlanItemId,
                description = task?.name ?: current.description,
                durationSeconds = current.durationSeconds + 1500, // Expand total duration
                isPomodoro = true,
                isBreak = false,
                startTimeEpochMillis = current.startTimeEpochMillis // Keep original start
            )
        }
    }

    fun startNextPomodoro() {
        val current = sprintManager.takeFinished() ?: return
        val task = current.taskId?.let { _uiState.value.board.tasksById[it] }
        sprintManager.startSprint(
            taskId = current.taskId,
            dailyPlanItemId = current.dailyPlanItemId,
            description = task?.name ?: current.description,
            durationSeconds = 1500,
            isPomodoro = true,
            isBreak = false
        )
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
            delay(EditorTextSaveDebounceMillis)
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
}

private fun MyDayUiState.selectedSuggestionTimeRangeFor(task: TaskItem): Pair<Int?, Int?> {
    val selectedDuration = suggestionStartTimeMinutes?.let { selectedStart ->
        suggestionEndTimeMinutes?.let { selectedEnd ->
            (selectedEnd - selectedStart).takeIf { it > 0 }
        }
    }
    val durationMinutes = selectedDuration
        ?: duration(task.startTimeMinutes, task.endTimeMinutes)
        ?: DefaultTaskDurationMinutes
    val preferredStart = suggestionStartTimeMinutes ?: task.preferredMyDayStartTime()
    return nextAvailableTimeRange(preferredStart, durationMinutes)
}

private fun TaskItem.preferredMyDayStartTime(): Int {
    val now = currentMyDayTimeMinutes()
    val start = startTimeMinutes
    return if (start == null || start < now) {
        now
    } else {
        start
    }
}

private fun MyDayUiState.nextAvailableTimeRange(
    preferredStartTimeMinutes: Int,
    durationMinutes: Int
): Pair<Int?, Int?> {
    val duration = durationMinutes.coerceIn(MinimumPlanDurationMinutes, MyDayMinutesPerDay)
    val lastStart = MyDayMinutesPerDay - duration
    val preferredStart = preferredStartTimeMinutes.coerceIn(0, lastStart)
    val occupiedRanges = items
        .mapNotNull { item -> item.occupiedTimeRange() }
        .sortedBy { it.first }

    findAvailableStart(preferredStart, duration, occupiedRanges)?.let { start ->
        return start to start + duration
    }
    findAvailableStart(0, duration, occupiedRanges)?.let { start ->
        return start to start + duration
    }
    return null to null
}

private fun DailyPlanItem.occupiedTimeRange(): Pair<Int, Int>? {
    val start = startTimeMinutes ?: return null
    val end = (endTimeMinutes ?: (start + DefaultTaskDurationMinutes)).coerceAtMost(MyDayMinutesPerDay)
    return if (end > start) start.coerceIn(0, MyDayMinutesPerDay) to end else null
}

private fun findAvailableStart(
    preferredStart: Int,
    durationMinutes: Int,
    occupiedRanges: List<Pair<Int, Int>>
): Int? {
    val lastStart = MyDayMinutesPerDay - durationMinutes
    var candidate = preferredStart.coerceIn(0, lastStart)
    occupiedRanges.forEach { (occupiedStart, occupiedEnd) ->
        if (candidate + durationMinutes <= occupiedStart) return candidate
        if (candidate < occupiedEnd && candidate + durationMinutes > occupiedStart) {
            candidate = occupiedEnd.coerceAtMost(lastStart)
        }
    }
    return candidate.takeIf { candidate + durationMinutes <= MyDayMinutesPerDay && !it.overlapsAny(durationMinutes, occupiedRanges) }
}

private fun Int.overlapsAny(durationMinutes: Int, occupiedRanges: List<Pair<Int, Int>>): Boolean =
    occupiedRanges.any { (occupiedStart, occupiedEnd) ->
        this < occupiedEnd && this + durationMinutes > occupiedStart
    }

private fun DailyPlanItemEditorState.toWriteInput(
    status: DailyPlanItemStatus,
    source: DailyPlanItemSource = this.source
) = DailyPlanItemWriteInput(
    title = title,
    note = note,
    source = source,
    status = status,
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = if (source.hasEndTime()) endTimeMinutes else null,
    tagIds = selectedTagIds.toList()
)

private fun DailyPlanItemEditorState.saveSource(): DailyPlanItemSource =
    source

private fun DailyPlanItemEditorState.saveStatus(): DailyPlanItemStatus =
    if (isAddMode) {
        if (source == DailyPlanItemSource.MyDayNote) DailyPlanItemStatus.Done
        else source.inferredAddStatus(startTimeMinutes)
    } else status

private fun DailyPlanItemSource.inferredAddStatus(startTimeMinutes: Int?): DailyPlanItemStatus =
    if (infersAddStatusFromStartTime() && startTimeMinutes != null && startTimeMinutes < currentMyDayTimeMinutes()) {
        DailyPlanItemStatus.Done
    } else {
        DailyPlanItemStatus.Planned
    }

private fun DailyPlanItemSource.infersAddStatusFromStartTime(): Boolean =
    this == DailyPlanItemSource.MyDayTask || this == DailyPlanItemSource.MyDayReminder

private fun DailyPlanItemSource.defaultStatus(): DailyPlanItemStatus = when (this) {
    DailyPlanItemSource.MyDayNote,
    DailyPlanItemSource.MyDayReminder -> DailyPlanItemStatus.Planned
    else -> DailyPlanItemStatus.Done
}

private fun currentMyDayTimeMinutes(): Int {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return now.hour * 60 + now.minute
}

private const val DefaultTaskDurationMinutes = 45
private const val MinimumPlanDurationMinutes = 15
private const val MyDayMinutesPerDay = 24 * 60
private const val EditorTextSaveDebounceMillis = 600L
