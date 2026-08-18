package com.checkit.ui

import com.checkit.ui.calendar.CalendarViewModel
import com.checkit.ui.myday.MyDayViewModel
import com.checkit.ui.nested.NestedListsViewModel
import com.checkit.ui.reflect.ReflectViewModel
import com.checkit.ui.settings.SettingsViewModel
import com.checkit.ui.tasks.TaskViewModel
import com.checkit.ui.tasks.list.ListViewModel
import com.checkit.ui.tasks.tag.TagViewModel
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

data class CheckItViewModels(
    val task: TaskViewModel,
    val list: ListViewModel,
    val tag: TagViewModel,
    val myDay: MyDayViewModel,
    val calendar: CalendarViewModel,
    val reflect: ReflectViewModel,
    val nested: NestedListsViewModel,
    val settings: SettingsViewModel
)

@Composable
fun koinCheckItViewModels(): CheckItViewModels = CheckItViewModels(
    task = koinViewModel(),
    list = koinViewModel(),
    tag = koinViewModel(),
    myDay = koinViewModel(),
    calendar = koinViewModel(),
    reflect = koinViewModel(),
    nested = koinViewModel(),
    settings = koinViewModel()
)
