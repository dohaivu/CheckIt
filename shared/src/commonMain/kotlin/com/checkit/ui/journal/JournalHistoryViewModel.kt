package com.checkit.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.CheckItRepository
import com.checkit.domain.JournalEntry
import com.checkit.domain.MoodFilter
import com.checkit.domain.PeriodGoal
import com.checkit.domain.TagItem
import com.checkit.domain.usecase.ObservePeriodGoalsUseCase
import com.checkit.domain.usecase.ObserveTagsUseCase
import com.checkit.ui.today
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

data class JournalHistoryFilters(
    val mood: MoodFilter? = null,
    val searchText: String = "",
    val tagId: Long? = null
)

private fun JournalHistoryFilters.hasActiveFilters(): Boolean =
    mood != null || searchText.isNotBlank() || tagId != null

data class JournalHistoryUiState(
    val filters: JournalHistoryFilters = JournalHistoryFilters(),
    val entries: List<JournalEntry> = emptyList(),
    val tags: List<TagItem> = emptyList(),
    /** Day goals shown inline in the history agenda. */
    val dayGoals: List<PeriodGoal> = emptyList(),
    /** Whether older history exists outside the loaded window (browse mode only). */
    val hasOlder: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

/** Owns journal-history state independently of any host screen (Calendar, My Day). */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class JournalHistoryViewModel(
    private val repository: CheckItRepository,
    observeTags: ObserveTagsUseCase,
    private val observePeriodGoals: ObservePeriodGoalsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(JournalHistoryUiState())
    val uiState: StateFlow<JournalHistoryUiState> = _uiState.asStateFlow()

    private val windowStart = MutableStateFlow(today().minus(WindowDays - 1, DateTimeUnit.DAY))

    init {
        viewModelScope.launch {
            observeTags().collect { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }

        viewModelScope.launch {
            combine(
                _uiState.map { it.filters }.debounce(250).distinctUntilChanged(),
                windowStart
            ) { filters, start -> filters to start }
                .flatMapLatest { (filters, start) ->
                    // Hybrid windowing: browse mode loads a recent window that
                    // expands on demand; active filters search all history.
                    val hasFilters = filters.hasActiveFilters()
                    val effectiveStart = if (hasFilters) null else start

                    val reviewsFlow = if (hasFilters) {
                        kotlinx.coroutines.flow.flowOf(emptyList<PeriodGoal>())
                    } else {
                        observePeriodGoals(startDate = effectiveStart, endDateInclusive = null)
                    }

                    combine(
                        repository.observeJournalEntriesFiltered(
                            moodEmojis = filters.mood?.emojis.orEmpty(),
                            searchText = filters.searchText.trim().takeIf { it.isNotEmpty() },
                            tagId = filters.tagId,
                            startDate = effectiveStart
                        ),
                        reviewsFlow
                    ) { entries, reviews ->
                        entries to reviews.filter { it.review.isNotBlank() }
                    }
                }
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Unable to load journals")
                    }
                }
                .collect { (entries, dayGoals) ->
                    _uiState.update {
                        it.copy(entries = entries, dayGoals = dayGoals, isLoading = false)
                    }
                }
        }

        viewModelScope.launch {
            combine(
                _uiState.map { it.filters }.distinctUntilChanged(),
                windowStart
            ) { filters, start -> if (filters.hasActiveFilters()) null else start }
                .distinctUntilChanged()
                .flatMapLatest { start ->
                    if (start == null) {
                        kotlinx.coroutines.flow.flowOf(false)
                    } else {
                        repository.observeOlderJournalHistoryExists(start)
                    }
                }
                .collect { hasOlder ->
                    _uiState.update { it.copy(hasOlder = hasOlder) }
                }
        }
    }

    /** Expands the browse-mode history window by [WindowDays]. */
    fun loadOlder() {
        windowStart.update { it.minus(WindowDays, DateTimeUnit.DAY) }
    }

    fun toggleMood(mood: MoodFilter) {
        _uiState.update { state ->
            state.copy(filters = state.filters.copy(mood = if (state.filters.mood == mood) null else mood))
        }
        resetWindow()
    }

    fun updateSearchText(value: String) {
        _uiState.update { it.copy(filters = it.filters.copy(searchText = value)) }
        resetWindow()
    }

    fun toggleTag(tagId: Long) {
        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(tagId = if (state.filters.tagId == tagId) null else tagId)
            )
        }
        resetWindow()
    }

    fun clearFilters() {
        _uiState.update { it.copy(filters = JournalHistoryFilters()) }
        resetWindow()
    }

    private fun resetWindow() {
        windowStart.value = today().minus(WindowDays - 1, DateTimeUnit.DAY)
    }

    companion object {
        /** Size of the initially loaded history window and of each expansion. */
        const val WindowDays: Int = 7
    }
}
