package com.checkit.ui.twelveweek

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.TwelveWeekGoalScoreWriteInput
import com.checkit.domain.TwelveWeekGoalFinalStatus
import com.checkit.domain.usecase.AbandonTwelveWeekCycleUseCase
import com.checkit.domain.usecase.AddTwelveWeekGoalUseCase
import com.checkit.domain.usecase.CompleteTwelveWeekCycleUseCase
import com.checkit.domain.usecase.DeleteTwelveWeekGoalUseCase
import com.checkit.domain.usecase.ObserveTwelveWeekWorkspaceUseCase
import com.checkit.domain.usecase.StartTwelveWeekCycleUseCase
import com.checkit.domain.usecase.UpdateTwelveWeekGoalUseCase
import com.checkit.domain.usecase.UpsertTwelveWeekCheckInUseCase
import com.checkit.ui.UiEvent
import com.checkit.ui.today
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TwelveWeekViewModel(
    private val observeWorkspace: ObserveTwelveWeekWorkspaceUseCase,
    private val startCycle: StartTwelveWeekCycleUseCase,
    private val addGoal: AddTwelveWeekGoalUseCase,
    private val updateGoal: UpdateTwelveWeekGoalUseCase,
    private val deleteGoal: DeleteTwelveWeekGoalUseCase,
    private val upsertCheckIn: UpsertTwelveWeekCheckInUseCase,
    private val completeCycle: CompleteTwelveWeekCycleUseCase,
    private val abandonCycle: AbandonTwelveWeekCycleUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(TwelveWeekUiState())
    val uiState: StateFlow<TwelveWeekUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val todayEpochDays = today().toEpochDays().toInt()
        viewModelScope.launch {
            observeWorkspace(todayEpochDays)
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to load 12-week goals"))
                }
                .collect { workspace ->
                    _uiState.update {
                        it.copy(workspace = workspace, isLoading = false)
                    }
                }
        }
    }

    fun openStartSheet() {
        _uiState.update { it.copy(startSheet = TwelveWeekStartSheetState()) }
    }

    fun dismissStartSheet() {
        _uiState.update { it.copy(startSheet = null) }
    }

    fun updateStartTitle(value: String) {
        _uiState.update { state ->
            state.copy(startSheet = state.startSheet?.copy(title = value))
        }
    }

    fun updateStartGoalTitle(index: Int, value: String) {
        _uiState.update { state ->
            state.startSheet?.let { sheet ->
                state.copy(
                    startSheet = sheet.copy(
                        goalTitles = sheet.goalTitles.mapIndexed { i, current ->
                            if (i == index) value else current
                        }
                    )
                )
            } ?: state
        }
    }

    fun saveStartCycle() {
        val sheet = _uiState.value.startSheet ?: return
        if (sheet.isSaving) return
        _uiState.update { it.copy(startSheet = sheet.copy(isSaving = true)) }
        viewModelScope.launch {
            runCatching {
                startCycle(
                    title = sheet.title,
                    startEpochDays = today().toEpochDays().toInt(),
                    goalTitles = sheet.goalTitles
                )
            }.onSuccess {
                _uiState.update { state -> state.copy(startSheet = null) }
                sendEvent(UiEvent.ShowSnackbar("12-week cycle started"))
            }.onFailure { error ->
                _uiState.update { state -> state.copy(startSheet = state.startSheet?.copy(isSaving = false)) }
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to start cycle"))
            }
        }
    }

    fun openAddGoalEditor() {
        val cycle = _uiState.value.workspace.cycle ?: return
        _uiState.update {
            it.copy(goalEditor = TwelveWeekGoalEditorState(goalId = null, cycleId = cycle.id))
        }
    }

    fun openEditGoalEditor(goalId: Long) {
        val goal = _uiState.value.workspace.goals.firstOrNull { it.goal.id == goalId }?.goal ?: return
        _uiState.update {
            it.copy(
                goalEditor = TwelveWeekGoalEditorState(
                    goalId = goalId,
                    cycleId = goal.cycleId,
                    title = goal.title,
                    note = goal.note
                )
            )
        }
    }

    fun dismissGoalEditor() {
        _uiState.update { it.copy(goalEditor = null) }
    }

    fun updateGoalEditorTitle(value: String) {
        _uiState.update { state -> state.copy(goalEditor = state.goalEditor?.copy(title = value)) }
    }

    fun updateGoalEditorNote(value: String) {
        _uiState.update { state -> state.copy(goalEditor = state.goalEditor?.copy(note = value)) }
    }

    fun saveGoalEditor() {
        val editor = _uiState.value.goalEditor ?: return
        if (editor.isSaving) return
        _uiState.update { it.copy(goalEditor = editor.copy(isSaving = true)) }
        viewModelScope.launch {
            runCatching {
                if (editor.goalId == null) {
                    addGoal(editor.cycleId, editor.title, editor.note)
                } else {
                    updateGoal(editor.goalId, editor.title, editor.note)
                }
            }.onSuccess {
                _uiState.update { state -> state.copy(goalEditor = null) }
            }.onFailure { error ->
                _uiState.update { state -> state.copy(goalEditor = state.goalEditor?.copy(isSaving = false)) }
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save goal"))
            }
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            runCatching { deleteGoal(goalId) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(goalEditor = state.goalEditor?.takeUnless { it.goalId == goalId })
                    }
                }
                .onFailure { error ->
                    sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to delete goal"))
                }
        }
    }

    fun openCheckInSheet(weekIndex: Int) {
        val workspace = _uiState.value.workspace
        val existing = workspace.checkIns.firstOrNull { it.weekIndex == weekIndex }
        val savedScores = existing?.let { checkIn ->
            workspace.scores.filter { it.checkInId == checkIn.id }
        }.orEmpty()
        _uiState.update {
            it.copy(
                checkInSheet = TwelveWeekCheckInSheetState(
                    weekIndex = weekIndex,
                    note = existing?.note.orEmpty(),
                    scores = workspace.goals.map { card ->
                        TwelveWeekScoreField(
                            goalId = card.goal.id,
                            goalTitle = card.goal.title,
                            score = savedScores.firstOrNull { s -> s.goalId == card.goal.id }?.score?.toString().orEmpty()
                        )
                    }
                )
            )
        }
    }

    fun dismissCheckInSheet() {
        _uiState.update { it.copy(checkInSheet = null) }
    }

    fun updateCheckInNote(value: String) {
        _uiState.update { state -> state.copy(checkInSheet = state.checkInSheet?.copy(note = value)) }
    }

    fun updateScore(goalId: Long, value: String) {
        _uiState.update { state ->
            state.copy(
                checkInSheet = state.checkInSheet?.copy(
                    scores = state.checkInSheet.scores.map { field ->
                        if (field.goalId == goalId) field.copy(score = value) else field
                    }
                )
            )
        }
    }

    fun saveCheckIn() {
        val sheet = _uiState.value.checkInSheet ?: return
        if (sheet.isSaving) return
        val cycleId = _uiState.value.workspace.cycle?.id ?: return
        _uiState.update { it.copy(checkInSheet = sheet.copy(isSaving = true)) }
        viewModelScope.launch {
            runCatching {
                upsertCheckIn(
                    cycleId = cycleId,
                    weekIndex = sheet.weekIndex,
                    note = sheet.note,
                    scores = sheet.scores.mapNotNull { field ->
                        field.score.trim().toIntOrNull()?.let { score ->
                            TwelveWeekGoalScoreWriteInput(goalId = field.goalId, score = score, note = "")
                        }
                    }
                )
            }.onSuccess {
                _uiState.update { state -> state.copy(checkInSheet = null) }
                sendEvent(UiEvent.ShowSnackbar("Week ${sheet.weekIndex + 1} check-in saved"))
            }.onFailure { error ->
                _uiState.update { state -> state.copy(checkInSheet = state.checkInSheet?.copy(isSaving = false)) }
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save check-in"))
            }
        }
    }

    fun openCompleteSheet() {
        val workspace = _uiState.value.workspace
        val cycle = workspace.cycle ?: return
        _uiState.update {
            it.copy(
                completeSheet = TwelveWeekCompleteSheetState(
                    cycleId = cycle.id,
                    goalTitles = workspace.goals.associate { card -> card.goal.id to card.goal.title },
                    finalStatuses = workspace.goals.associateTo(mutableMapOf()) { card ->
                        card.goal.id to (card.goal.finalStatus ?: TwelveWeekGoalFinalStatus.Partial)
                    }
                )
            )
        }
    }

    fun dismissCompleteSheet() {
        _uiState.update { it.copy(completeSheet = null) }
    }

    fun setFinalStatus(goalId: Long, status: TwelveWeekGoalFinalStatus) {
        _uiState.update { state ->
            state.completeSheet?.let { sheet ->
                state.copy(completeSheet = sheet.copy(finalStatuses = sheet.finalStatuses.toMutableMap().apply { put(goalId, status) }))
            } ?: state
        }
    }

    fun updateCompleteNote(value: String) {
        _uiState.update { state -> state.copy(completeSheet = state.completeSheet?.copy(reviewNote = value)) }
    }

    fun saveCompleteCycle() {
        val sheet = _uiState.value.completeSheet ?: return
        if (sheet.isSaving) return
        _uiState.update { it.copy(completeSheet = sheet.copy(isSaving = true)) }
        viewModelScope.launch {
            runCatching {
                completeCycle(sheet.cycleId, sheet.finalStatuses.toMap(), sheet.reviewNote)
            }.onSuccess {
                _uiState.update { state -> state.copy(completeSheet = null) }
                sendEvent(UiEvent.ShowSnackbar("Cycle completed"))
            }.onFailure { error ->
                _uiState.update { state -> state.copy(completeSheet = state.completeSheet?.copy(isSaving = false)) }
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to complete cycle"))
            }
        }
    }

    fun abandonCycle() {
        val cycle = _uiState.value.workspace.cycle ?: return
        viewModelScope.launch {
            runCatching { abandonCycle(cycle.id) }
                .onSuccess { sendEvent(UiEvent.ShowSnackbar("Cycle abandoned")) }
                .onFailure { error ->
                    sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to abandon cycle"))
                }
        }
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}