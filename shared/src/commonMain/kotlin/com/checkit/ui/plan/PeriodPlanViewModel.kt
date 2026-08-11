package com.checkit.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.PlanFocus
import com.checkit.domain.PlanPeriod
import com.checkit.domain.PlanPriority
import com.checkit.domain.startOf
import com.checkit.domain.usecase.AddPlanPriorityUseCase
import com.checkit.domain.usecase.DeletePlanPriorityUseCase
import com.checkit.domain.usecase.LinkTaskToPlanPriorityUseCase
import com.checkit.domain.usecase.ObservePlanWorkspaceUseCase
import com.checkit.domain.usecase.TogglePlanPriorityDoneUseCase
import com.checkit.domain.usecase.UpdatePlanPriorityUseCase
import com.checkit.ui.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeriodPlanViewModel(
    private val observePlanWorkspace: ObservePlanWorkspaceUseCase,
    private val addPlanPriority: AddPlanPriorityUseCase,
    private val updatePlanPriority: UpdatePlanPriorityUseCase,
    private val deletePlanPriority: DeletePlanPriorityUseCase,
    private val togglePlanPriorityDone: TogglePlanPriorityDoneUseCase,
    private val linkTaskToPlanPriority: LinkTaskToPlanPriorityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanPeriodUiState())
    val uiState: StateFlow<PlanPeriodUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        observeWorkspace()
    }

    private fun observeWorkspace() {
        viewModelScope.launch {
            _uiState.map { it.focus }
                .distinctUntilChanged()
                .collectLatest { focus ->
                    observePlanWorkspace(focus).collect { workspace ->
                        _uiState.update {
                            it.copy(workspace = workspace, isLoading = false)
                        }
                    }
                }
        }
    }

    fun selectFocus(focus: PlanFocus) {
        val normalized = focus.copy(anchorDate = focus.period.startOf(focus.anchorDate))
        _uiState.update { state ->
            if (state.focus == normalized && state.workspace != null) {
                state
            } else {
                state.copy(focus = normalized, isLoading = true, editor = null)
            }
        }
    }

    fun shiftPeriod(delta: Int) {
        selectFocus(_uiState.value.focus.shift(delta))
    }

    fun zoomOut() {
        selectFocus(_uiState.value.focus.zoomOut())
    }

    /** Zooms the workspace into the focus period of a nested (child) priority. */
    fun zoomIntoPriority(priority: PlanPriority) {
        val state = _uiState.value
        val plan = priority.periodPlan
        val target = PlanFocus(plan.period, plan.startDate)
        if (target.period.ordinal >= state.focus.period.ordinal) {
            selectFocus(target)
        }
    }

    fun startAddPriority() {
        _uiState.update {
            it.copy(
                editor = PlanPriorityEditorState(
                    mode = PlanEditorMode.Add,
                    parentId = it.parentCandidates.firstOrNull()?.id
                )
            )
        }
    }

    fun startEditPriority(priority: PlanPriority) {
        _uiState.update {
            it.copy(
                editor = PlanPriorityEditorState(
                    mode = PlanEditorMode.Edit,
                    priorityId = priority.id,
                    parentId = priority.parentId,
                    title = priority.title,
                    note = priority.note,
                    isDone = priority.isDone
                )
            )
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun updateEditorTitle(title: String) {
        _uiState.update {
            it.copy(editor = it.editor?.copy(title = title))
        }
    }

    fun updateEditorNote(note: String) {
        _uiState.update {
            it.copy(editor = it.editor?.copy(note = note))
        }
    }

    fun updateEditorParent(parentId: Long?) {
        _uiState.update {
            it.copy(editor = it.editor?.copy(parentId = parentId))
        }
    }

    fun savePriority() {
        val state = _uiState.value
        val editor = state.editor ?: return
        val focus = state.focus.let { current ->
            if (current.period == PlanPeriod.Day) current.zoomOut() else current
        }
        viewModelScope.launch {
            runCatching {
                when (editor.mode) {
                    PlanEditorMode.Add -> addPlanPriority(focus, editor.title, editor.parentId, editor.note)
                    PlanEditorMode.Edit -> editor.priorityId?.let {
                        updatePlanPriority(it, focus, editor.title, editor.parentId, editor.note)
                    }
                }
            }.onSuccess {
                _uiState.update { current -> current.copy(editor = null) }
                _events.tryEmit(
                    UiEvent.ShowSnackbar(
                        when (editor.mode) {
                            PlanEditorMode.Add -> "Priority added"
                            PlanEditorMode.Edit -> "Priority saved"
                        }
                    )
                )
            }.onFailure { error ->
                _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to save priority"))
            }
        }
    }

    fun deletePriority(priorityId: Long) {
        viewModelScope.launch {
            deletePlanPriority(priorityId)
            _uiState.update { it.copy(editor = null) }
            _events.tryEmit(UiEvent.ShowSnackbar("Priority deleted"))
        }
    }

    fun toggleDone(priorityId: Long, isDone: Boolean) {
        viewModelScope.launch {
            togglePlanPriorityDone(priorityId, isDone)
            _uiState.update { state ->
                state.copy(
                    editor = state.editor?.takeIf { it.priorityId == priorityId }?.copy(isDone = isDone)
                )
            }
        }
    }

    fun linkTask(priorityId: Long, taskId: Long) {
        viewModelScope.launch {
            runCatching { linkTaskToPlanPriority(priorityId, taskId) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to link task"))
                }
        }
    }
}
