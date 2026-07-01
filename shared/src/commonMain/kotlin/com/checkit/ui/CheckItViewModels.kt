package com.checkit.ui

import com.checkit.ui.calendar.CalendarViewModel
import com.checkit.ui.myday.MyDayViewModel
import com.checkit.ui.okr.GoalViewModel
import com.checkit.ui.okr.KeyResultViewModel
import com.checkit.ui.okr.ObjectiveViewModel
import com.checkit.ui.reports.ReportViewModel
import com.checkit.ui.settings.SettingsViewModel
import com.checkit.ui.tasks.TaskViewModel
import com.checkit.ui.tasks.tag.TagViewModel
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

data class CheckItViewModels(
    val task: TaskViewModel,
    val goal: GoalViewModel,
    val keyResult: KeyResultViewModel,
    val objective: ObjectiveViewModel,
    val tag: TagViewModel,
    val myDay: MyDayViewModel,
    val calendar: CalendarViewModel,
    val report: ReportViewModel,
    val settings: SettingsViewModel
)

@Composable
fun koinCheckItViewModels(): CheckItViewModels = CheckItViewModels(
    task = koinViewModel(),
    goal = koinViewModel(),
    keyResult = koinViewModel(),
    objective = koinViewModel(),
    tag = koinViewModel(),
    myDay = koinViewModel(),
    calendar = koinViewModel(),
    report = koinViewModel(),
    settings = koinViewModel()
)
