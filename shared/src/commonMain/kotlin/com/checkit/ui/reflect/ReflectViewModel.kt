package com.checkit.ui.reflect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.CheckItRepository
import com.checkit.domain.JournalEntry
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewSource
import com.checkit.domain.usecase.BuildPeriodReviewDraftUseCase
import com.checkit.domain.usecase.ObservePeriodReviewsUseCase
import com.checkit.domain.usecase.SavePeriodReviewUseCase
import com.checkit.ui.UiEvent
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.today
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class ReflectViewModel(
    private val repository: CheckItRepository,
    private val observePeriodReviews: ObservePeriodReviewsUseCase,
    private val savePeriodReview: SavePeriodReviewUseCase,
    private val buildDraft: BuildPeriodReviewDraftUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReflectUiState())
    val uiState: StateFlow<ReflectUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeDailyPlans(),
                repository.observeJournalEntries(),
                observePeriodReviews()
            ) { dailyPlans, journalEntries, reviews ->
                Triple(dailyPlans, journalEntries, reviews)
            }
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to load Reflect"))
                }
                .collect { (dailyPlans, journalEntries, reviews) ->
                    _uiState.update {
                        it.copy(
                            dailyPlans = dailyPlans,
                            journalEntries = journalEntries,
                            reviews = reviews,
                            isLoading = false
                        )
                    }
                }
        }
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
        openEditor()
    }

    fun openEditor() {
        val state = _uiState.value
        val review = state.focusReview
        _uiState.update {
            it.copy(
                editor = ReflectReviewEditorState(
                    focus = it.focus,
                    review = review,
                    content = review?.content.orEmpty(),
                    intentNext = review?.intentNext.orEmpty(),
                    source = review?.source ?: ReviewSource.Manual,
                    statsJson = review?.statsJson,
                    highlightsJson = review?.highlightsJson,
                    isDraft = review?.source == ReviewSource.Hybrid
                )
            )
        }
    }

    fun generateDraft() {
        val current = _uiState.value
        if (current.editor != null) return
        val focus = current.focus
        viewModelScope.launch {
            val draft = buildDraft(focus, current.dailyPlans)
            _uiState.update {
                it.copy(
                    editor = ReflectReviewEditorState(
                        focus = focus,
                        review = it.focusReview,
                        content = draft?.content.orEmpty(),
                        source = if (draft != null) ReviewSource.Hybrid else ReviewSource.Manual,
                        statsJson = draft?.statsJson,
                        highlightsJson = draft?.highlightsJson,
                        isDraft = draft != null
                    )
                )
            }
            if (draft == null) {
                sendEvent(UiEvent.ShowSnackbar("No activity in this period to draft from"))
            }
        }
    }

    fun updateEditorContent(value: String) {
        _uiState.update { current ->
            val editor = current.editor ?: return@update current
            current.copy(editor = editor.copy(content = value))
        }
    }

    fun updateEditorIntentNext(value: String) {
        _uiState.update { current ->
            val editor = current.editor ?: return@update current
            current.copy(editor = editor.copy(intentNext = value))
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun saveEditor() {
        val current = _uiState.value
        val editor = current.editor ?: return
        if (editor.isSaving) return
        _uiState.update { it.copy(editor = editor.copy(isSaving = true)) }
        viewModelScope.launch {
            runCatching {
                savePeriodReview(
                    focus = editor.focus,
                    content = editor.content,
                    intentNext = editor.intentNext,
                    source = editor.source,
                    statsJson = editor.statsJson,
                    highlightsJson = editor.highlightsJson
                )
            }.onSuccess {
                _uiState.update { it.copy(editor = null) }
                sendEvent(UiEvent.ShowSnackbar("Review saved"))
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(editor = state.editor?.copy(isSaving = false))
                }
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save review"))
            }
        }
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

private fun ReportPeriod.move(date: LocalDate, amount: Int): LocalDate = when (this) {
    ReportPeriod.Daily -> date.plus(amount, DateTimeUnit.DAY)
    ReportPeriod.Week -> date.plus(amount * 7, DateTimeUnit.DAY)
    ReportPeriod.Month -> date.plus(amount, DateTimeUnit.MONTH).firstDayOfMonth()
    ReportPeriod.Annual -> date.plus(amount, DateTimeUnit.YEAR)
    ReportPeriod.Habit -> date
}
