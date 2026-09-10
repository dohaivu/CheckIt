package com.checkit.ui.quicknote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.QuickNoteSyncManager
import com.checkit.data.QuickNoteSyncState
import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteRules
import com.checkit.domain.usecase.CreateQuickNoteUseCase
import com.checkit.domain.usecase.DeleteQuickNotePermanentlyUseCase
import com.checkit.domain.usecase.MaintainQuickNotesUseCase
import com.checkit.domain.usecase.MoveQuickNoteToBeDeletedUseCase
import com.checkit.domain.usecase.MoveQuickNoteUseCase
import com.checkit.domain.usecase.ObserveQuickNextUseCase
import com.checkit.domain.usecase.ObserveQuickToBeDeletedUseCase
import com.checkit.domain.usecase.RestoreQuickNoteUseCase
import com.checkit.domain.usecase.SetQuickNoteReminderUseCase
import com.checkit.ui.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickNoteUiState(
    val next: List<QuickNote> = emptyList(),
    val toBeDeleted: List<QuickNote> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = true,
    val reminderPickerId: String? = null,
    val syncState: QuickNoteSyncState = QuickNoteSyncState(),
    /** In-drag visual order of NEXT ids; null when not dragging. */
    val dragOrder: List<String>? = null,
) {
    val visibleNext: List<QuickNote> = run {
        val order = dragOrder ?: return@run next
        val byId = next.associateBy { it.id }
        order.mapNotNull { byId[it] } + next.filter { it.id !in order.toSet() }
    }
}

class QuickNoteViewModel(
    private val observeNext: ObserveQuickNextUseCase,
    private val observeToBeDeleted: ObserveQuickToBeDeletedUseCase,
    private val createNote: CreateQuickNoteUseCase,
    private val moveToBeDeleted: MoveQuickNoteToBeDeletedUseCase,
    private val setReminder: SetQuickNoteReminderUseCase,
    private val moveNote: MoveQuickNoteUseCase,
    private val deletePermanentlyUseCase: DeleteQuickNotePermanentlyUseCase,
    private val restoreUseCase: RestoreQuickNoteUseCase,
    private val maintain: MaintainQuickNotesUseCase,
    private val syncManager: QuickNoteSyncManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickNoteUiState())
    val uiState: StateFlow<QuickNoteUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(observeNext(), observeToBeDeleted()) { next, deleted -> next to deleted }
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to load quick notes"))
                }
                .collect { (next, deleted) ->
                    _uiState.update { it.copy(next = next, toBeDeleted = deleted, isLoading = false) }
                }
        }
        viewModelScope.launch {
            syncManager.syncState.collect { syncState ->
                _uiState.update { it.copy(syncState = syncState) }
            }
        }
        refresh()
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun submitInput() {
        val text = _uiState.value.input
        if (text.isBlank()) return
        _uiState.update { it.copy(input = "") }
        viewModelScope.launch {
            runCatching { createNote(text) }
                .onFailure { error ->
                    _uiState.update { state -> state.copy(input = text) }
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to save note"))
                }
        }
    }

    fun swipeRight(id: String) {
        viewModelScope.launch {
            runCatching { moveToBeDeleted(id) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to move note"))
                }
        }
    }

    fun deletePermanently(id: String) {
        viewModelScope.launch {
            runCatching { deletePermanentlyUseCase(id) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to delete note"))
                }
        }
    }

    fun restore(id: String) {
        viewModelScope.launch {
            runCatching { restoreUseCase(id) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to restore note"))
                }
        }
    }

    fun openReminderPicker(id: String) {
        _uiState.update { it.copy(reminderPickerId = id) }
    }

    fun dismissReminderPicker() {
        _uiState.update { it.copy(reminderPickerId = null) }
    }

    fun setReminder30Min() = setReminderWithDuration(QuickNoteRules.REMINDER_30_MIN_MILLIS)

    fun setReminder1Hour() = setReminderWithDuration(QuickNoteRules.REMINDER_1_HOUR_MILLIS)

    private fun setReminderWithDuration(durationMillis: Long) {
        val id = _uiState.value.reminderPickerId ?: return
        _uiState.update { it.copy(reminderPickerId = null) }
        viewModelScope.launch {
            runCatching { setReminder(id, durationMillis) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to set reminder"))
                }
        }
    }

    // --- Drag-and-drop ordering (NEXT only) ---

    fun moveDragging(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val visible = state.visibleNext
            if (fromIndex !in visible.indices || toIndex !in visible.indices) return@update state
            if (fromIndex == toIndex) return@update state
            val reordered = visible.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            state.copy(dragOrder = reordered.map { it.id })
        }
    }

    fun commitDrag() {
        val state = _uiState.value
        val order = state.dragOrder ?: return
        _uiState.update { it.copy(dragOrder = null) }
        val current = state.next.sortedBy { it.sortOrder }
        if (order == current.map { it.id }) return
        viewModelScope.launch {
            runCatching {
                val indexOf = order.withIndex().associate { (idx, id) -> id to idx }
                // Persist as a single semantic placement of the moved item is complex after
                // multi-step visual drags; fall back to sequential adjacent placements.
                var working = current.map { it.id }.toMutableList()
                order.forEachIndexed { targetIndex, id ->
                    val from = working.indexOf(id)
                    if (from != targetIndex) {
                        working.add(targetIndex, working.removeAt(from))
                        val beforeId = working.getOrNull(targetIndex - 1)?.takeIf { it != id }
                        val afterId = working.getOrNull(targetIndex + 1)?.takeIf { it != id }
                        moveNote(id, beforeId, afterId)
                    }
                }
                // Keep a local copy of the intended order for logging parity; repository is source of truth.
                indexOf
            }.onFailure { error ->
                _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to reorder"))
            }
        }
    }

    fun cancelDrag() {
        _uiState.update { it.copy(dragOrder = null) }
    }

    /**
     * Full maintenance: expire 24h items, reconcile alarms, and sync.
     * Called on screen show, app resume, and manual refresh.
     */
    fun refresh() {
        viewModelScope.launch {
            runCatching { maintain() }
        }
    }
}
