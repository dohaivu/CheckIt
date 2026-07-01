package com.checkit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.tab_calendar
import checkit.shared.generated.resources.tab_my_day
import checkit.shared.generated.resources.tab_report
import checkit.shared.generated.resources.tab_settings
import checkit.shared.generated.resources.tab_tasks
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.ui.tasks.TaskUiState
import com.checkit.ui.myday.MyDayUiState
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable data object Task : AppRoute
    @Serializable data object Calendar : AppRoute
    @Serializable data object MyDay : AppRoute
    @Serializable data object Report : AppRoute
    @Serializable data object TimeReport : AppRoute
    @Serializable data object TagsReport : AppRoute
    @Serializable data object Settings : AppRoute
}

enum class CheckItTab {
    Task, MyDay, Calendar, Report, Settings;

    fun route(): AppRoute = when (this) {
        MyDay -> AppRoute.MyDay
        Task -> AppRoute.Task
        Calendar -> AppRoute.Calendar
        Report -> AppRoute.Report
        Settings -> AppRoute.Settings
    }

    fun icon(): ImageVector = when (this) {
        MyDay -> Icons.Default.Today
        Task -> Icons.AutoMirrored.Filled.ListAlt
        Calendar -> Icons.Default.CalendarMonth
        Report -> Icons.Default.BarChart
        Settings -> Icons.Default.MoreHoriz
    }

    @Composable
    fun label(): String = when (this) {
        MyDay -> stringResource(Res.string.tab_my_day)
        Task -> stringResource(Res.string.tab_tasks)
        Calendar -> stringResource(Res.string.tab_calendar)
        Report -> stringResource(Res.string.tab_report)
        Settings -> stringResource(Res.string.tab_settings)
    }

    companion object {
        fun fromRoute(route: NavKey): CheckItTab? = when (route) {
            AppRoute.MyDay -> MyDay
            AppRoute.Task -> Task
            AppRoute.Calendar -> Calendar
            AppRoute.Report, AppRoute.TimeReport, AppRoute.TagsReport -> Report
            AppRoute.Settings -> Settings
            else -> null
        }
    }
}

class AppNavigationState(
    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(AppRoute.MyDay)
) {
    val currentRoute: NavKey
        get() = backStack.lastOrNull() ?: AppRoute.MyDay

    fun resetTo(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }

    fun push(route: NavKey) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else if (backStack.lastOrNull() != AppRoute.MyDay) {
            resetTo(AppRoute.MyDay)
        }
    }
}

@Composable
fun rememberAppNavigationState(): AppNavigationState {
    return remember { AppNavigationState() }
}

sealed interface WidgetLaunchTarget {
    data class Task(val task: TaskItem, val dailyPlanItem: DailyPlanItem?) : WidgetLaunchTarget
    data class Note(val note: NoteItem) : WidgetLaunchTarget
    data class DailyPlan(val item: DailyPlanItem, val date: LocalDate) : WidgetLaunchTarget

    companion object {
        fun from(
            dailyPlanItemId: Long?,
            taskId: Long?,
            noteId: Long?,
            taskUiState: TaskUiState,
            myDayUiState: MyDayUiState
        ): WidgetLaunchTarget? {
            val dailyPlanTarget = dailyPlanItemId?.let { itemId ->
                myDayUiState.dailyPlans.firstNotNullOfOrNull { plan ->
                    plan.items.firstOrNull { it.id == itemId }?.let { item -> item to plan.date }
                }
            }
            if (dailyPlanItemId != null && dailyPlanTarget == null) return null
            if (taskId != null) {
                val task = taskUiState.board.tasksById[taskId] ?: return null
                return Task(task = task, dailyPlanItem = dailyPlanTarget?.first)
            }
            if (noteId != null) {
                val note = taskUiState.board.notesById[noteId] ?: return null
                return Note(note)
            }
            return dailyPlanTarget?.let { (item, date) -> DailyPlan(item, date) }
        }
    }
}
