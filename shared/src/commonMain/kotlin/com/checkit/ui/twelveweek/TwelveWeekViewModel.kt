package com.checkit.ui.twelveweek

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.TwelveWeekGoalScoreWriteInput
import com.checkit.domain.TwelveWeekGoalFinalStatus
import com.checkit.domain.mondayOfWeek
import com.checkit.domain.usecase.AbandonTwelveWeekCycleUseCase
import com.checkit.domain.usecase.AddTwelveWeekGoalUseCase
import com.checkit.domain.usecase.CompleteTwelveWeekCycleUseCase
import com.checkit.domain.usecase.DeleteTwelveWeekGoalUseCase
import com.checkit.domain.usecase.ObserveTwelveWeekWorkspaceUseCase
import com.checkit.domain.usecase.StartTwelveWeekCycleUseCase
import com.checkit.domain.usecase.UpdateTwelveWeekCycleUseCase
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
    private val updateCycle: UpdateTwelveWeekCycleUseCase,
    private val addGoal: AddTwelveWeekGoalUseCase,
    private val updateGoal: UpdateTwelveWeekGoalUseCase,
    private val deleteGoal: DeleteTwelveWeekGoalUseCase,
    private val upsertCheckIn: UpsertTwelveWeekCheckInUseCase,
    private val completeCycle: CompleteTwelveWeekCycleUseCase,
    private val abandonCycleUseCase: AbandonTwelveWeekCycleUseCase
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

    fun openCycleEditor(cycleId: Long? = null) {
        val cycle = cycleId?.let { id ->
            _uiState.value.workspace.cycleCards
                .firstOrNull { it.cycle.id == id }
                ?.cycle
        }
        _uiState.update {
            it.copy(
                cycleEditor = TwelveWeekCycleEditorState(
                    cycleId = cycleId,
                    title = cycle?.title.orEmpty(),
                    startEpochDays = cycle?.startEpochDays
                        ?: mondayOfWeek(today().toEpochDays().toInt())
                )
            )
        }
    }

    fun dismissCycleEditor() {
        _uiState.update { it.copy(cycleEditor = null) }
    }

    fun updateCycleEditorTitle(value: String) {
        _uiState.update { state -> state.copy(cycleEditor = state.cycleEditor?.copy(title = value)) }
    }

    fun saveCycleEditor() {
        val editor = _uiState.value.cycleEditor ?: return
        if (editor.isSaving) return
        _uiState.update { it.copy(cycleEditor = editor.copy(isSaving = true)) }
        viewModelScope.launch {
            runCatching {
                if (editor.cycleId == null) {
                    startCycle(
                        title = editor.title,
                        startEpochDays = today().toEpochDays().toInt()
                    )
                } else {
                    updateCycle(editor.cycleId, editor.title)
                }
            }.onSuccess {
                _uiState.update { state -> state.copy(cycleEditor = null) }
                if (editor.cycleId == null) {
                    sendEvent(UiEvent.ShowSnackbar("12-week cycle started"))
                }
            }.onFailure { error ->
                _uiState.update { state -> state.copy(cycleEditor = state.cycleEditor?.copy(isSaving = false)) }
                sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save cycle"))
            }
        }
    }

    fun openAddGoalEditor(cycleId: Long) {
        _uiState.update {
            it.copy(goalEditor = TwelveWeekGoalEditorState(goalId = null, cycleId = cycleId))
        }
    }

    fun openEditGoalEditor(goalId: Long) {
        val goal = _uiState.value.workspace.cycleCards
            .flatMap { it.goals }
            .firstOrNull { it.goal.id == goalId }?.goal ?: return
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

    fun openCheckInSheet(cycleId: Long, weekIndex: Int) {
        val workspace = _uiState.value.workspace
        val cycleCard = workspace.cycleCards.firstOrNull { it.cycle.id == cycleId } ?: return
        val existing = workspace.checkIns.firstOrNull { it.cycleId == cycleId && it.weekIndex == weekIndex }
        val savedScores = existing?.let { checkIn ->
            workspace.scores.filter { it.checkInId == checkIn.id }
        }.orEmpty()
        _uiState.update {
            it.copy(
                checkInSheet = TwelveWeekCheckInSheetState(
                    cycleId = cycleId,
                    weekIndex = weekIndex,
                    note = existing?.note.orEmpty(),
                    scores = cycleCard.goals.map { card ->
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
        _uiState.update { it.copy(checkInSheet = sheet.copy(isSaving = true)) }
        viewModelScope.launch {
            runCatching {
                upsertCheckIn(
                    cycleId = sheet.cycleId,
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

    fun openCompleteSheet(cycleId: Long) {
        val workspace = _uiState.value.workspace
        val cycleCard = workspace.cycleCards.firstOrNull { it.cycle.id == cycleId } ?: return
        val goals = cycleCard.goals
        _uiState.update {
            it.copy(
                completeSheet = TwelveWeekCompleteSheetState(
                    cycleId = cycleId,
                    goalTitles = goals.associate { card -> card.goal.id to card.goal.title },
                    finalStatuses = goals.associateTo(mutableMapOf()) { card ->
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

    fun abandonCycle(cycleId: Long) {
        viewModelScope.launch {
            runCatching { abandonCycleUseCase(cycleId) }
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