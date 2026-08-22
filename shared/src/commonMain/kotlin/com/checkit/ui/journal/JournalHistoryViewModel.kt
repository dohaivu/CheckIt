package com.checkit.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.CheckItRepository
import com.checkit.domain.JournalEntry
import com.checkit.domain.MoodFilter
import com.checkit.domain.TagItem
import com.checkit.domain.usecase.ObserveTagsUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JournalHistoryFilters(
    val mood: MoodFilter? = null,
    val searchText: String = "",
    val tagId: Long? = null
)

data class JournalHistoryUiState(
    val filters: JournalHistoryFilters = JournalHistoryFilters(),
    val entries: List<JournalEntry> = emptyList(),
    val tags: List<TagItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/** Owns journal-history state independently of any host screen (Calendar, My Day). */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class JournalHistoryViewModel(
    private val repository: CheckItRepository,
    observeTags: ObserveTagsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(JournalHistoryUiState())
    val uiState: StateFlow<JournalHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeTags().collect { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }

        viewModelScope.launch {
            _uiState
                .map { it.filters }
                .debounce(250)
                .distinctUntilChanged()
                .flatMapLatest { filters ->
                    repository.observeJournalEntriesFiltered(
                        moodEmojis = filters.mood?.emojis.orEmpty(),
                        searchText = filters.searchText.trim().takeIf { it.isNotEmpty() },
                        tagId = filters.tagId
                    )
                }
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Unable to load journals")
                    }
                }
                .collect { entries ->
                    _uiState.update { it.copy(entries = entries, isLoading = false) }
                }
        }
    }

    fun toggleMood(mood: MoodFilter) {
        _uiState.update { state ->
            state.copy(filters = state.filters.copy(mood = if (state.filters.mood == mood) null else mood))
        }
    }

    fun updateSearchText(value: String) {
        _uiState.update { it.copy(filters = it.filters.copy(searchText = value)) }
    }

    fun toggleTag(tagId: Long) {
        _uiState.update { state ->
            state.copy(
                filters = state.filters.copy(tagId = if (state.filters.tagId == tagId) null else tagId)
            )
        }
    }

    fun clearFilters() {
        _uiState.update { it.copy(filters = JournalHistoryFilters()) }
    }
}
