package com.checkit.ui

import com.checkit.ui.calendar.CalendarViewModel
import com.checkit.ui.myday.MyDayViewModel
import com.checkit.ui.okr.GoalViewModel
import com.checkit.ui.okr.KeyResultViewModel
import com.checkit.ui.okr.ObjectiveViewModel
import com.checkit.ui.plan.PeriodPlanViewModel
import com.checkit.ui.reflect.ReflectViewModel
import com.checkit.ui.settings.SettingsViewModel
import com.checkit.ui.tasks.TaskViewModel
import com.checkit.ui.tasks.list.ListViewModel
import com.checkit.ui.tasks.tag.TagViewModel
import com.checkit.ui.twelveweek.TwelveWeekViewModel
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

data class CheckItViewModels(
    val task: TaskViewModel,
    val goal: GoalViewModel,
    val keyResult: KeyResultViewModel,
    val objective: ObjectiveViewModel,
    val list: ListViewModel,
    val tag: TagViewModel,
    val myDay: MyDayViewModel,
    val calendar: CalendarViewModel,
    val reflect: ReflectViewModel,
    val plan: PeriodPlanViewModel,
    val twelveWeek: TwelveWeekViewModel,
    val settings: SettingsViewModel
)

@Composable
fun koinCheckItViewModels(): CheckItViewModels = CheckItViewModels(
    task = koinViewModel(),
    goal = koinViewModel(),
    keyResult = koinViewModel(),
    objective = koinViewModel(),
    list = koinViewModel(),
    tag = koinViewModel(),
    myDay = koinViewModel(),
    calendar = koinViewModel(),
    reflect = koinViewModel(),
    plan = koinViewModel(),
    twelveWeek = koinViewModel(),
    settings = koinViewModel()
)
