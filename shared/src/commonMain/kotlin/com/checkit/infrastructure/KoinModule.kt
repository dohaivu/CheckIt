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
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddManualDoneToDailyPlanUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.AddTaskListUseCase
import com.checkit.domain.usecase.AddTaskTagUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.CompleteNoteUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.OpenNoteUseCase
import com.checkit.domain.usecase.OpenTaskUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateTaskListUseCase
import com.checkit.domain.usecase.UpdateTaskTagUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
import com.checkit.ui.calendar.CalendarViewModel
import com.checkit.ui.myday.MyDayViewModel
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
    single<CheckItRepository> { RoomCheckItRepository(get(), get()) }
    single { ObserveTaskBoardUseCase(get()) }
    single { ObserveDailyPlansUseCase(get()) }
    single { EnsureDefaultTaskDataUseCase(get()) }
    single { AddTaskListUseCase(get()) }
    single { UpdateTaskListUseCase(get()) }
    single { AddTaskTagUseCase(get()) }
    single { UpdateTaskTagUseCase(get()) }
    single { IsTagNameTakenUseCase(get()) }
    single { AddTaskUseCase(get()) }
    single { UpdateTaskUseCase(get()) }
    single { DeleteTaskUseCase(get()) }
    single { CompleteTaskUseCase(get()) }
    single { CompleteNoteUseCase(get()) }
    single { OpenTaskUseCase(get()) }
    single { OpenNoteUseCase(get()) }
    single { AddTaskToDailyPlanUseCase(get()) }
    single { AddManualDoneToDailyPlanUseCase(get()) }
    single { UpdateDailyPlanItemTimeUseCase(get()) }
    single { UpdateDailyPlanItemUseCase(get()) }
    single { DeleteDailyPlanItemUseCase(get()) }
    single { AddNoteUseCase(get()) }
    single { UpdateNoteUseCase(get()) }
    single { DeleteNoteUseCase(get()) }
    single { SelectTaskBoardItemsUseCase() }
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
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
    viewModel { CalendarViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel {
        MyDayViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
    viewModel { ReportViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
