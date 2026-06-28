package com.checkit.infrastructure

import androidx.room.RoomDatabase
import com.checkit.data.AppDataStore
import com.checkit.data.CheckItRepository
import com.checkit.data.RoomCheckItRepository
import com.checkit.data.CheckItDatabase
import com.checkit.data.DataStoreSettingsRepository
import com.checkit.data.buildCheckItDatabase
import com.checkit.data.createPreferencesDataStore
import com.checkit.data.provideDatabaseBuilder
import com.checkit.data.SettingsRepository
import com.checkit.domain.CheckInReminderPolicy
import com.checkit.domain.DailyPlanScheduleReminderPolicy
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.notifications.AppReminderScheduler
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddDailyPlanItemUseCase
import com.checkit.domain.usecase.AddGoalUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.AddObjectiveUseCase
import com.checkit.domain.usecase.AddTagUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.AutoAddTodayTasksToMyDayUseCase
import com.checkit.domain.usecase.SyncKeyResultFromDailyPlanUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.CompleteNoteUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.DeleteGoalUseCase
import com.checkit.domain.usecase.DeleteObjectiveUseCase
import com.checkit.domain.usecase.DeleteTagUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.OpenNoteUseCase
import com.checkit.domain.usecase.OpenTaskUseCase
import com.checkit.domain.usecase.RestoreNoteUseCase
import com.checkit.domain.usecase.RestoreTaskUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemStatusUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateGoalUseCase
import com.checkit.domain.usecase.UpdateObjectiveUseCase
import com.checkit.domain.usecase.UpdateTagUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
import com.checkit.ui.calendar.CalendarViewModel
import com.checkit.ui.myday.MyDayViewModel
import com.checkit.ui.okr.GoalViewModel
import com.checkit.ui.okr.KeyResultViewModel
import com.checkit.ui.okr.ObjectiveViewModel
import com.checkit.ui.tasks.tag.TagViewModel
import com.checkit.ui.tasks.TaskViewModel
import com.checkit.ui.reports.ReportViewModel
import com.checkit.ui.settings.SettingsViewModel
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

expect fun platformModule(): Module

fun doInitKoin() {
    initKoin()
}

fun initKoin(config: KoinAppDeclaration? = null) =
    startKoin {
        config?.invoke(this)
        modules(
            platformModule(),
            provideDatabaseModule,
            provideInteractorModule,
            provideLocalServiceModule,
            provideViewModelModule
        )
    }

val provideInteractorModule = module {
    single { HttpClient() }
    single<CheckItRepository> { RoomCheckItRepository(get(), get(), get()) }
    single { ObserveTaskBoardUseCase(get()) }
    single { ObserveDailyPlansUseCase(get()) }
    single { EnsureDefaultTaskDataUseCase(get()) }
    single { AutoAddTodayTasksToMyDayUseCase(get(), get()) }
    single { AddGoalUseCase(get()) }
    single { UpdateGoalUseCase(get()) }
    single { DeleteGoalUseCase(get()) }
    single { AddObjectiveUseCase(get()) }
    single { UpdateObjectiveUseCase(get()) }
    single { DeleteObjectiveUseCase(get()) }
    single { AddTagUseCase(get()) }
    single { UpdateTagUseCase(get()) }
    single { DeleteTagUseCase(get()) }
    single { IsTagNameTakenUseCase(get()) }
    single { AddTaskUseCase(get()) }
    single { UpdateTaskUseCase(get()) }
    single { DeleteTaskUseCase(get()) }
    single { RestoreTaskUseCase(get()) }
    single { CompleteTaskUseCase(get()) }
    single { CompleteNoteUseCase(get()) }
    single { OpenTaskUseCase(get()) }
    single { OpenNoteUseCase(get()) }
    single { AddTaskToDailyPlanUseCase(get()) }
    single { AddDailyPlanItemUseCase(get()) }
    single { UpdateDailyPlanItemTimeUseCase(get()) }
    single { UpdateDailyPlanItemStatusUseCase(get()) }
    single { SyncKeyResultFromDailyPlanUseCase(get()) }
    single { UpdateDailyPlanItemUseCase(get()) }
    single { DeleteDailyPlanItemUseCase(get()) }
    single { AddNoteUseCase(get()) }
    single { UpdateNoteUseCase(get()) }
    single { DeleteNoteUseCase(get()) }
    single { RestoreNoteUseCase(get()) }
    single { SelectTaskBoardItemsUseCase() }
    single { CheckInReminderPolicy(get(), get()) }
    single { DailyPlanScheduleReminderPolicy(get(), get()) }
}

val provideDatabaseModule = module {
    single<RoomDatabase.Builder<CheckItDatabase>> { provideDatabaseBuilder() }
    single { buildCheckItDatabase(get()) }
    single { get<CheckItDatabase>().checkItDao() }
}

val provideLocalServiceModule = module {
    single { AppDataStore(createPreferencesDataStore()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
}

val provideViewModelModule = module {
    viewModel {
        TaskViewModel(
            observeTaskBoard = get(),
            ensureDefaultTaskData = get(),
            selectTaskBoardItems = get(),
            addTask = get(),
            addTaskToDailyPlan = get(),
            updateTask = get(),
            deleteTask = get(),
            restoreTask = get(),
            completeTask = get(),
            completeNote = get(),
            openTask = get(),
            openNote = get(),
            addNote = get(),
            updateNote = get(),
            deleteNote = get(),
            restoreNote = get(),
            updateDailyPlanItemTime = get(),
            updateDailyPlanItemStatus = get(),
            syncKeyResultFromDailyPlan = get(),
            settingsRepository = get()
        )
    }
    viewModel { GoalViewModel(get(), get(), get()) }
    viewModel { KeyResultViewModel(get()) }
    viewModel { ObjectiveViewModel(get(), get(), get()) }
    viewModel { TagViewModel(get(), get(), get(), get()) }
    viewModel { CalendarViewModel(get(), get(), get()) }
    viewModel {
        MyDayViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
    viewModel { ReportViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get<AppReminderScheduler>()) }
}
