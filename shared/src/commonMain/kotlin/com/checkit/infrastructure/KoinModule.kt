package com.checkit.infrastructure

import androidx.room.RoomDatabase
import com.checkit.data.CheckItRepository
import com.checkit.data.RoomCheckItRepository
import com.checkit.data.CheckItDatabase
import com.checkit.data.buildCheckItDatabase
import com.checkit.data.provideDatabaseBuilder
import com.checkit.domain.SettingsRepository
import com.checkit.ui.calendar.CalendarViewModel
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
    single<CheckItRepository> { RoomCheckItRepository(get()) }
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
    viewModel { TaskViewModel(get() ) }
    viewModel { CalendarViewModel(get()) }
    viewModel { ReportViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
