package com.checkit.ui.twelveweek

import com.checkit.domain.TwelveWeekCycleCard
import com.checkit.domain.TwelveWeekGoalFinalStatus
import com.checkit.domain.TwelveWeekWorkspace

data class TwelveWeekViewOptionsState(
    val showCompletedTactic: Boolean = false,
    val showCompletedCycle: Boolean = false
)

data class TwelveWeekUiState(
    val workspace: TwelveWeekWorkspace = TwelveWeekWorkspace(),
    val isLoading: Boolean = true,
    val viewOptions: TwelveWeekViewOptionsState = TwelveWeekViewOptionsState(),
    val visibleCycleCards: List<TwelveWeekCycleCard> = emptyList(),
    val cycleEditor: TwelveWeekCycleEditorState? = null,
    val goalEditor: TwelveWeekGoalEditorState? = null,
    val checkInSheet: TwelveWeekCheckInSheetState? = null,
    val completeSheet: TwelveWeekCompleteSheetState? = null,
    val checkInHistory: TwelveWeekCheckInHistoryState? = null
)

data class TwelveWeekCycleEditorState(
    val cycleId: Long? = null,
    val title: String = "",
    val startEpochDays: Int = 0,
    val isSaving: Boolean = false
)

data class TwelveWeekGoalEditorState(
    val goalId: Long?,
    val cycleId: Long,
    val title: String = "",
    val note: String = "",
    val isSaving: Boolean = false
)

data class TwelveWeekCheckInSheetState(
    val cycleId: Long,
    val weekIndex: Int,
    val note: String = "",
    val scores: List<TwelveWeekScoreField> = emptyList(),
    val isSaving: Boolean = false
) {
    val canSave: Boolean get() = scores.isNotEmpty()
}

data class TwelveWeekScoreField(
    val goalId: Long,
    val goalTitle: String,
    val score: String = ""
)

data class TwelveWeekCompleteSheetState(
    val cycleId: Long,
    val goalTitles: Map<Long, String>,
    val finalStatuses: MutableMap<Long, TwelveWeekGoalFinalStatus>,
    val reviewNote: String = "",
    val isSaving: Boolean = false
)

data class TwelveWeekCheckInHistoryState(
    val cycleId: Long,
    val cycleTitle: String,
    val startEpochDays: Int,
    val weeks: List<TwelveWeekHistoryEntry>
)

data class TwelveWeekHistoryEntry(
    val weekIndex: Int,
    val note: String,
    val scores: List<TwelveWeekHistoryScore>
)

data class TwelveWeekHistoryScore(
    val goalTitle: String,
    val score: Int
)