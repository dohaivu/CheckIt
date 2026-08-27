package com.checkit.ui.reflect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.CheckItRepository
import com.checkit.domain.MetricItem
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.DoneItemSummary
import com.checkit.domain.FocusPeriod
import com.checkit.domain.HabitDailyRollup
import com.checkit.domain.JournalEntry
import com.checkit.domain.PeriodGoal

import com.checkit.domain.usecase.ObservePeriodGoalsUseCase
import com.checkit.domain.usecase.SavePeriodGoalUseCase
import com.checkit.ui.UiEvent
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.today
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@OptIn(ExperimentalCoroutinesApi::class)
class ReflectViewModel(
    private val repository: CheckItRepository,
    private val observePeriodGoals: ObservePeriodGoalsUseCase,
    private val savePeriodGoal: SavePeriodGoalUseCase,
    private val dataDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReflectUiState())
    val uiState: StateFlow<ReflectUiState> = _uiState.asStateFlow()

    private val _editor = MutableStateFlow<ReflectGoalEditorState?>(null)
    val editor: StateFlow<ReflectGoalEditorState?> = _editor.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { ReflectSelection(it.selectedPeriod, it.selectedDate) }
                .distinctUntilChanged()
                .flatMapLatest { selection ->
                    observeSelection(selection)
                }
                .flowOn(dataDispatcher)
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to load Reflect"))
                }
                .collect { data ->
                _uiState.update {
                    it.copy(
                        dailyStats = data.dailyStats,
                        doneItems = data.doneItems,
                        journalEntries = data.journalEntries,
                        habitRollups = data.habitRollups,
                        goals = data.goals,
                        isLoading = false
                    )
                }
                }
        }
    }

    /**
     * Subscribes only to the rollup windows needed for [selection]:
     * daily stats over the selection window, tag/done/journal windows scoped to
     * the focused period, and a fixed trailing window for habits.
     */
    private fun observeSelection(selection: ReflectSelection) =
        combine(
            repository.observeDailyReflectStats(
                startDate = selection.period.statsWindow(selection.date).first,
                endDateInclusive = selection.period.statsWindow(selection.date).second
            ),
            repository.observeDoneItemSummaries(
                startDate = selection.period.focusWindow(selection.date).first,
                endDateInclusive = selection.period.focusWindow(selection.date).second
            ),
            repository.observeJournalEntriesInRange(
                startDate = selection.period.focusWindow(selection.date).first,
                endDateInclusive = selection.period.focusWindow(selection.date).second
            ),
            combine(
                repository.observeHabitDailyRollups(
                    startDate = habitRollupWindow(today()).first,
                    endDateInclusive = habitRollupWindow(today()).second
                ),
                observePeriodGoals(
                    // Goals shown are the child-period ones within the current
                    // window (plus the focus goal itself), so only that span
                    // needs to be observed.
                    startDate = selection.goalsWindow().first,
                    endDateInclusive = selection.goalsWindow().second
                )
            ) { habits, goals -> habits to goals }
        ) { stats, doneItems, journals, (habits, goals) ->
            ReflectData(
                dailyStats = stats,
                doneItems = doneItems,
                journalEntries = journals,
                habitRollups = habits,
                goals = goals
            )
        }

    /**
     * Inclusive date span covering every goal the UI can display for this
     * selection: the child-period goals of the current window and the focus
     * goal itself.
     */
    private fun ReflectSelection.goalsWindow(): Pair<LocalDate, LocalDate> {
        val focus = FocusPeriod(period.toPeriod(), date)
        val rangeFocus = if (period == ReportPeriod.Daily) focus.zoomOut() else focus
        return rangeFocus.start to rangeFocus.endInclusive
    }

    fun selectPeriod(period: ReportPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun previousPeriod() {
        _uiState.update { state ->
            state.copy(selectedDate = state.selectedPeriod.move(state.selectedDate, -1))
        }
    }

    fun nextPeriod() {
        _uiState.update { state ->
            state.copy(selectedDate = state.selectedPeriod.move(state.selectedDate, 1))
        }
    }

    fun resetToCurrentPeriod() {
        _uiState.update { it.copy(selectedDate = today()) }
    }

    fun zoomIn() {
        _uiState.update { it.copy(selectedPeriod = it.selectedPeriod.zoomInPeriod()) }
    }

    fun zoomOut() {
        _uiState.update { it.copy(selectedPeriod = it.selectedPeriod.zoomOutPeriod()) }
    }

    /** Zoom into the child period anchored at [date] (tap-to-zoom-in). */
    fun zoomInTo(date: LocalDate) {
        _uiState.update {
            it.copy(selectedPeriod = it.selectedPeriod.zoomInPeriod(), selectedDate = date)
        }
    }

    /** Jump straight to a specific day (used by deep links from Calendar/agenda). */
    fun focusDay(date: LocalDate) {
        _uiState.update {
            it.copy(selectedPeriod = ReportPeriod.Daily, selectedDate = date)
        }
    }

    /** Breadcrumb zoom-out: jump to a broader level keeping the anchor. */
    fun zoomOutTo(period: ReportPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun openGoal(goal: PeriodGoal) {
        _uiState.update {
            it.copy(
                selectedPeriod = goal.period.toReportPeriod(),
                selectedDate = goal.startDate
            )
        }
        // Seed the editor from the tapped goal itself; the matching window
        // loads asynchronously and openEditor() would otherwise miss it.
        val focus = FocusPeriod(goal.period, goal.startDate)
        _editor.value = ReflectGoalEditorState(
            focus = focus,
            existing = goal,
            review = goal.review,
            goal = goal.goal.orEmpty(),
            rating = goal.rating,
            metrics = goal.metrics
        )
    }

    fun openNewGoalEditor(date: LocalDate, period: com.checkit.domain.Period) {
        val focus = FocusPeriod(period, date)
        _editor.value = ReflectGoalEditorState(
            focus = focus,
            review = "",
            goal = "",
            rating = 0f,
            metrics = emptyList()
        )
    }

    fun openEditor() {
        val state = _uiState.value
        _editor.value = ReflectGoalEditorState(
            focus = state.focus,
            existing = state.focusGoal,
            review = state.focusGoal?.review.orEmpty(),
            goal = state.focusGoal?.goal.orEmpty(),
            rating = state.focusGoal?.rating ?: 0f,
            metrics = state.focusGoal?.metrics.orEmpty()
        )
    }

    fun updateEditorReview(value: String) {
        _editor.update { editor -> editor?.copy(review = value) }
    }

    fun updateEditorGoal(value: String) {
        _editor.update { editor -> editor?.copy(goal = value) }
    }

    fun updateEditorRating(value: Float) {
        _editor.update { editor -> editor?.copy(rating = value) }
    }

    fun updateEditorMetrics(value: List<MetricItem>) {
        _editor.update { editor -> editor?.copy(metrics = value) }
    }

    fun dismissEditor() {
        _editor.value = null
    }

    fun saveEditor() {
        val editor = _editor.value ?: return
        if (editor.isSaving) return
        _editor.value = editor.copy(isSaving = true)
        viewModelScope.launch {
            runCatching {
                savePeriodGoal(
                    focus = editor.focus,
                    review = editor.review,
                    goal = editor.goal,
                    rating = editor.rating,
                    metrics = editor.metrics
                )
            }.onSuccess {
                _editor.value = null
                sendEvent(UiEvent.ShowSnackbar("Goal saved"))
            }.onFailure { error ->
                _editor.update { it?.copy(isSaving = false) }
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save goal"))
            }
        }
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

private data class ReflectSelection(
    val period: ReportPeriod,
    val date: LocalDate
)

private data class ReflectData(
    val dailyStats: List<DailyReflectStat>,
    val doneItems: List<DoneItemSummary>,
    val journalEntries: List<JournalEntry>,
    val habitRollups: List<HabitDailyRollup>,
    val goals: List<PeriodGoal>
)

private fun ReportPeriod.move(date: LocalDate, amount: Int): LocalDate = when (this) {
    ReportPeriod.Daily -> date.plus(amount, DateTimeUnit.DAY)
    ReportPeriod.Week -> date.plus(amount * 7, DateTimeUnit.DAY)
    ReportPeriod.Month -> date.plus(amount, DateTimeUnit.MONTH).firstDayOfMonth()
    ReportPeriod.Annual -> date.plus(amount, DateTimeUnit.YEAR)
    ReportPeriod.Habit -> date
}
