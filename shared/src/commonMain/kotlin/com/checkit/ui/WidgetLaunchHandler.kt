package com.checkit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.checkit.ui.myday.MyDayUiState
import com.checkit.ui.myday.MyDayViewModel
import com.checkit.ui.tasks.TaskUiState
import com.checkit.ui.tasks.TaskViewModel

@Composable
internal fun WidgetLaunchHandler(
    dailyPlanItemLaunchId: Long?,
    taskLaunchId: Long?,
    noteLaunchId: Long?,
    taskUiState: TaskUiState,
    myDayUiState: MyDayUiState,
    taskViewModel: TaskViewModel,
    myDayViewModel: MyDayViewModel,
    navState: AppNavigationState,
    onWidgetLaunchConsumed: () -> Unit
) {
    LaunchedEffect(
        dailyPlanItemLaunchId,
        taskLaunchId,
        noteLaunchId,
        taskUiState.board,
        myDayUiState.dailyPlans
    ) {
        val launchTarget = WidgetLaunchTarget.from(
            dailyPlanItemId = dailyPlanItemLaunchId,
            taskId = taskLaunchId,
            noteId = noteLaunchId,
            taskUiState = taskUiState,
            myDayUiState = myDayUiState
        ) ?: return@LaunchedEffect

        navState.resetTo(AppRoute.MyDay)
        when (launchTarget) {
            is WidgetLaunchTarget.Task -> taskViewModel.openTask(launchTarget.task, launchTarget.dailyPlanItem)
            is WidgetLaunchTarget.Note -> taskViewModel.openNote(launchTarget.note)
            is WidgetLaunchTarget.DailyPlan -> myDayViewModel.openItemEditor(launchTarget.item, launchTarget.date)
        }
        onWidgetLaunchConsumed()
    }
}
