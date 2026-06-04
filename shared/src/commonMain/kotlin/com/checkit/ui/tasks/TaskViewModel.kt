package com.checkit.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.TaskBoard
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.TaskBoardSelection
import com.checkit.ui.TaskUiState
import com.checkit.ui.TaskWorkspaceView
import com.checkit.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskViewModel(
    private val observeTaskBoard: ObserveTaskBoardUseCase,
    private val ensureDefaultTaskData: EnsureDefaultTaskDataUseCase,
    private val selectTaskBoardItems: SelectTaskBoardItemsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensureDefaultTaskData()
            observeTaskBoard()
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = error.message ?: "Unable to load tasks")
                    }
                }
                .collect { board ->
                    _uiState.update { current -> current.withBoard(board) }
                }
        }
    }

    fun selectList(listId: Long) {
        _uiState.update {
            it.copy(selectedListId = listId, selectedFilterId = null).refreshVisibleItems()
        }
    }

    fun selectFilter(filterId: Long) {
        _uiState.update {
            it.copy(selectedListId = null, selectedFilterId = filterId).refreshVisibleItems()
        }
    }

    fun selectView(view: TaskWorkspaceView) {
        _uiState.update { it.copy(selectedView = view) }
    }

    private fun TaskUiState.withBoard(board: TaskBoard): TaskUiState {
        val nextListId = selectedListId?.takeIf { selectedId -> board.lists.any { it.id == selectedId } }
            ?: board.lists.firstOrNull()?.id
        val nextFilterId = selectedFilterId?.takeIf { selectedId -> board.filters.any { it.id == selectedId } }
        return copy(
            board = board,
            selectedListId = if (nextFilterId == null) nextListId else null,
            selectedFilterId = nextFilterId,
            isLoading = false
        ).refreshVisibleItems()
    }

    private fun TaskUiState.refreshVisibleItems(): TaskUiState {
        val selection = selectedFilter?.let { TaskBoardSelection.FilterSelection(it) }
            ?: selectedListId?.let { TaskBoardSelection.ListSelection(it) }
            ?: return copy(visibleTasks = emptyList(), visibleNotes = emptyList())
        val items = selectTaskBoardItems(board, selection, today())
        return copy(visibleTasks = items.tasks, visibleNotes = items.notes)
    }
}
