package com.checkit.ui.reflect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.CheckItRepository
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.DoneItemSummary
import com.checkit.domain.FocusPeriod
import com.checkit.domain.HabitDailyRollup
import com.checkit.domain.JournalEntry
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewSource
import com.checkit.domain.usecase.ObservePeriodReviewsUseCase
import com.checkit.domain.usecase.SavePeriodReviewUseCase
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
    private val observePeriodReviews: ObservePeriodReviewsUseCase,
    private val savePeriodReview: SavePeriodReviewUseCase,
    private val dataDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReflectUiState())
    val uiState: StateFlow<ReflectUiState> = _uiState.asStateFlow()

    private val _editor = MutableStateFlow<ReflectReviewEditorState?>(null)
    val editor: StateFlow<ReflectReviewEditorState?> = _editor.asStateFlow()

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
                            reviews = data.reviews,
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
                observePeriodReviews(
                    // Reviews shown are the child-period ones within the current
                    // window (plus the focus review itself), so only that span
                    // needs to be observed.
                    startDate = selection.reviewsWindow().first,
                    endDateInclusive = selection.reviewsWindow().second
                )
            ) { habits, reviews -> habits to reviews }
        ) { stats, doneItems, journals, (habits, reviews) ->
            ReflectData(
                dailyStats = stats,
                doneItems = doneItems,
                journalEntries = journals,
                habitRollups = habits,
                reviews = reviews
            )
        }

    /**
     * Inclusive date span covering every review the UI can display for this
     * selection: the child-period reviews of the current window, the focus
     * review itself, and the following period (the editor prefills its intent
     * field from that period's own review).
     */
    private fun ReflectSelection.reviewsWindow(): Pair<LocalDate, LocalDate> {
        val focus = FocusPeriod(period.toPeriod(), date)
        val rangeFocus = if (period == ReportPeriod.Daily) focus.zoomOut() else focus
        return rangeFocus.start to maxOf(rangeFocus.endInclusive, rangeFocus.shift(1).endInclusive)
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

    fun openReview(review: PeriodReview) {
        _uiState.update {
            it.copy(
                selectedPeriod = review.period.toReportPeriod(),
                selectedDate = review.periodStartDate
            )
        }
        // Seed the editor from the tapped review itself; the matching window
        // loads asynchronously and openEditor() would otherwise miss it.
        val focus = FocusPeriod(review.period, review.periodStartDate)
        _editor.value = ReflectReviewEditorState(
            focus = focus,
            review = review,
            content = review.content,
            periodIntent = review.periodIntent.orEmpty(),
            source = review.source
        )
        loadNextPeriodIntent(focus)
    }

    fun openEditor() {
        val state = _uiState.value
        _editor.value = ReflectReviewEditorState(
            focus = state.focus,
            review = state.focusReview,
            content = state.focusReview?.content.orEmpty(),
            periodIntent = state.focusReview?.periodIntent.orEmpty(),
            source = state.focusReview?.source ?: ReviewSource.Manual
        )
        loadNextPeriodIntent(state.focus)
    }

    /**
     * The editor's intent field describes the next focus period, so it is
     * prefilled from that period's own review in the already-observed reviews.
     */
    private fun loadNextPeriodIntent(focus: FocusPeriod) {
        val nextFocus = focus.shift(1)
        _uiState.value.reviews
            .firstOrNull {
                it.period == nextFocus.period && it.periodStartEpochDays == nextFocus.startEpochDays
            }
            ?.periodIntent
            ?.takeIf { it.isNotBlank() }
            ?.let { intent ->
                _editor.update { editor ->
                    editor?.takeIf { it.focus == focus }?.copy(nextPeriodIntent = intent)
                }
            }
    }

    fun updateEditorContent(value: String) {
        _editor.update { editor -> editor?.copy(content = value) }
    }

    fun updateEditorNextPeriodIntent(value: String) {
        _editor.update { editor -> editor?.copy(nextPeriodIntent = value) }
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
                savePeriodReview(
                    focus = editor.focus,
                    content = editor.content,
                    periodIntent = editor.periodIntent,
                    nextPeriodIntent = editor.nextPeriodIntent,
                    source = editor.source
                )
            }.onSuccess {
                _editor.value = null
                sendEvent(UiEvent.ShowSnackbar("Review saved"))
            }.onFailure { error ->
                _editor.update { it?.copy(isSaving = false) }
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save review"))
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
    val reviews: List<PeriodReview>
)

private fun ReportPeriod.move(date: LocalDate, amount: Int): LocalDate = when (this) {
    ReportPeriod.Daily -> date.plus(amount, DateTimeUnit.DAY)
    ReportPeriod.Week -> date.plus(amount * 7, DateTimeUnit.DAY)
    ReportPeriod.Month -> date.plus(amount, DateTimeUnit.MONTH).firstDayOfMonth()
    ReportPeriod.Annual -> date.plus(amount, DateTimeUnit.YEAR)
    ReportPeriod.Habit -> date
}
