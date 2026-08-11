package com.checkit.ui.plan

import com.checkit.domain.PeriodPlan
import com.checkit.domain.PlanFocus
import com.checkit.domain.PlanPeriod
import com.checkit.domain.PlanPriority
import com.checkit.domain.PlanPriorityNode
import com.checkit.domain.PlanWorkspace
import com.checkit.ui.today

enum class PlanEditorMode {
    Add,
    Edit
}

data class PlanPeriodUiState(
    val focus: PlanFocus = PlanFocus(PlanPeriod.Week, today()),
    val workspace: PlanWorkspace? = null,
    val isLoading: Boolean = true,
    val editor: PlanPriorityEditorState? = null
) {
    val rootNodes: List<PlanPriorityNode> get() = workspace?.rootNodes.orEmpty()
    val parentCandidates: List<PlanPriority> get() = workspace?.parentCandidates.orEmpty()
    val plan: PeriodPlan? get() = workspace?.plan
}

data class PlanPriorityEditorState(
    val mode: PlanEditorMode,
    val priorityId: Long? = null,
    val parentId: Long? = null,
    val title: String = "",
    val note: String = ""
)
