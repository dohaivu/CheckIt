package com.checkit.infrastructure

import androidx.room3.RoomDatabase
import com.checkit.data.AppDataStore
import com.checkit.data.CheckItDatabase
import com.checkit.data.CheckItRepository
import com.checkit.data.DataStoreSettingsRepository
import com.checkit.data.RoomCheckItRepository
import com.checkit.data.SettingsRepository
import com.checkit.data.buildCheckItDatabase
import com.checkit.data.createPreferencesDataStore
import com.checkit.data.provideDatabaseBuilder
import com.checkit.domain.CheckInReminderPolicy
import com.checkit.domain.DailyPlanScheduleReminderPolicy
import com.checkit.domain.SprintManager
import com.checkit.domain.usecase.AddDailyPlanItemUseCase
import com.checkit.domain.usecase.AddJournalEntryUseCase
import com.checkit.domain.usecase.AddListUseCase
import com.checkit.domain.usecase.AddNestedDocumentUseCase
import com.checkit.domain.usecase.AddNestedItemUseCase
import com.checkit.domain.usecase.AddNoteUseCase
import com.checkit.domain.usecase.AddSuggestedTaskToMyDayUseCase
import com.checkit.domain.usecase.AddTagUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.AutoAddTodayTasksToMyDayUseCase
import com.checkit.domain.usecase.BuildDayCloseSummaryUseCase
import com.checkit.domain.usecase.BuildPeriodReviewDraftUseCase
import com.checkit.domain.usecase.CarryOverDailyPlanItemsUseCase
import com.checkit.domain.usecase.CompleteDayCloseUseCase
import com.checkit.domain.usecase.CompleteNoteUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.DeleteJournalEntryUseCase
import com.checkit.domain.usecase.DeleteListUseCase
import com.checkit.domain.usecase.DeleteNestedDocumentUseCase
import com.checkit.domain.usecase.DeleteNestedItemsUseCase
import com.checkit.domain.usecase.DeleteNoteUseCase
import com.checkit.domain.usecase.DeleteTagUseCase
import com.checkit.domain.usecase.DeleteTaskUseCase
import com.checkit.domain.usecase.IsTagNameTakenUseCase
import com.checkit.domain.usecase.MoveNestedItemsUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveJournalEntriesUseCase
import com.checkit.domain.usecase.ObserveNestedDocumentTreeUseCase
import com.checkit.domain.usecase.ObserveNestedDocumentsUseCase
import com.checkit.domain.usecase.ObserveNestedTagsUseCase
import com.checkit.domain.usecase.ObservePeriodReviewsUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.OpenNoteUseCase
import com.checkit.domain.usecase.OpenTaskUseCase
import com.checkit.domain.usecase.RenameNestedDocumentUseCase
import com.checkit.domain.usecase.ReplaceNestedManualMetricsUseCase
import com.checkit.domain.usecase.RestoreNoteUseCase
import com.checkit.domain.usecase.RestoreTaskUseCase
import com.checkit.domain.usecase.SavePeriodReviewUseCase
import com.checkit.domain.usecase.SaveSprintAsWinUseCase
import com.checkit.domain.usecase.SelectTaskBoardItemsUseCase
import com.checkit.domain.usecase.SetNestedItemCheckboxEnabledUseCase
import com.checkit.domain.usecase.SetNestedItemsCheckedUseCase
import com.checkit.domain.usecase.SmartScheduleDailyPlanUseCase
import com.checkit.domain.usecase.SprintTransitionUseCase
import com.checkit.domain.usecase.ToggleNestedItemCollapsedUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemStatusUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTagUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateJournalEntryUseCase
import com.checkit.domain.usecase.UpdateListUseCase
import com.checkit.domain.usecase.UpdateNestedItemDateRangeUseCase
import com.checkit.domain.usecase.UpdateNestedItemFormattingUseCase
import com.checkit.domain.usecase.UpdateNestedItemMetricSettingsUseCase
import com.checkit.domain.usecase.UpdateNestedItemNoteUseCase
import com.checkit.domain.usecase.UpdateNestedItemPriorityUseCase
import com.checkit.domain.usecase.UpdateNestedItemTagsUseCase
import com.checkit.domain.usecase.UpdateNestedItemTextUseCase
import com.checkit.domain.usecase.UpdateNoteUseCase
import com.checkit.domain.usecase.UpdateTagSortOrderUseCase
import com.checkit.domain.usecase.UpdateTagUseCase
import com.checkit.domain.usecase.UpdateTaskUseCase
import com.checkit.domain.usecase.UpsertDailyPlanItemUseCase
import com.checkit.notifications.AppReminderScheduler
import com.checkit.ui.calendar.CalendarViewModel
import com.checkit.ui.myday.MyDayViewModel
import com.checkit.ui.nested.NestedListsViewModel
import com.checkit.ui.reflect.ReflectViewModel
import com.checkit.ui.settings.SettingsViewModel
import com.checkit.ui.tasks.TaskViewModel
import com.checkit.ui.tasks.list.ListViewModel
import com.checkit.ui.tasks.tag.TagViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
            provideCommonModule,
            provideDatabaseModule,
            provideInteractorModule,
            provideLocalServiceModule,
            provideViewModelModule
        )
    }

val provideCommonModule = module {
    single<CoroutineDispatcher> { Dispatchers.Default }
}

val provideInteractorModule = module {
    single { HttpClient() }
    single { SprintManager(get()) }
    single { SaveSprintAsWinUseCase(get(), get(), get(), get(), get()) }
    single { SprintTransitionUseCase(get(), get(), get()) }
    single { UpsertDailyPlanItemUseCase(get()) }
    single { AddSuggestedTaskToMyDayUseCase(get(), get(), get()) }
    single<CheckItRepository> { RoomCheckItRepository(get(), get(), get()) }
    single { ObserveTaskBoardUseCase(get()) }
    single { ObserveDailyPlansUseCase(get()) }
    single { ObserveJournalEntriesUseCase(get()) }
    single { AddJournalEntryUseCase(get()) }
    single { UpdateJournalEntryUseCase(get()) }
    single { DeleteJournalEntryUseCase(get()) }
    single { AutoAddTodayTasksToMyDayUseCase(get(), get(), get(), get()) }
    single { AddListUseCase(get()) }
    single { UpdateListUseCase(get()) }
    single { DeleteListUseCase(get()) }
    single { AddTagUseCase(get()) }
    single { UpdateTagUseCase(get()) }
    single { UpdateTagSortOrderUseCase(get()) }
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
    single { SmartScheduleDailyPlanUseCase(get()) }
    single { UpdateDailyPlanItemStatusUseCase(get()) }
    single { UpdateDailyPlanItemTagUseCase(get()) }
    single { DeleteDailyPlanItemUseCase(get()) }
    single { BuildDayCloseSummaryUseCase(get()) }
    single { CarryOverDailyPlanItemsUseCase(get(), get()) }
    single { ObservePeriodReviewsUseCase(get()) }
    single { SavePeriodReviewUseCase(get()) }
    single { BuildPeriodReviewDraftUseCase() }
    single { CompleteDayCloseUseCase(get(), get(), get(), get()) }
    single { AddNoteUseCase(get()) }
    single { UpdateNoteUseCase(get()) }
    single { DeleteNoteUseCase(get()) }
    single { RestoreNoteUseCase(get()) }
    single { SelectTaskBoardItemsUseCase() }
    single { CheckInReminderPolicy(get(), get()) }
    single { DailyPlanScheduleReminderPolicy(get(), get()) }
    single { ObserveNestedDocumentsUseCase(get()) }
    single { ObserveNestedTagsUseCase(get()) }
    single { ObserveNestedDocumentTreeUseCase(get()) }
    single { AddNestedDocumentUseCase(get()) }
    single { RenameNestedDocumentUseCase(get()) }
    single { DeleteNestedDocumentUseCase(get()) }
    single { AddNestedItemUseCase(get()) }
    single { UpdateNestedItemTextUseCase(get()) }
    single { UpdateNestedItemNoteUseCase(get()) }
    single { UpdateNestedItemFormattingUseCase(get()) }
    single { UpdateNestedItemPriorityUseCase(get()) }
    single { UpdateNestedItemDateRangeUseCase(get()) }
    single { UpdateNestedItemTagsUseCase(get()) }
    single { UpdateNestedItemMetricSettingsUseCase(get()) }
    single { ReplaceNestedManualMetricsUseCase(get()) }
    single { SetNestedItemCheckboxEnabledUseCase(get()) }
    single { SetNestedItemsCheckedUseCase(get()) }
    single { ToggleNestedItemCollapsedUseCase(get()) }
    single { MoveNestedItemsUseCase(get()) }
    single { DeleteNestedItemsUseCase(get()) }
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
            updateDailyPlanItemTag = get(),
            settingsRepository = get()
        )
    }
    viewModel { ListViewModel(get(), get(), get()) }
    viewModel { TagViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { CalendarViewModel(get(), get(), get(), get()) }
    viewModel {
        MyDayViewModel(
            observeTaskBoard = get(),
            observeDailyPlans = get(),
            observeJournalEntries = get(),
            addJournalEntry = get(),
            updateJournalEntry = get(),
            deleteJournalEntry = get(),
            deleteDailyPlanItemUseCase = get(),
            settingsRepository = get(),
            buildDayCloseSummary = get(),
            completeDayClose = get(),
            carryOverDailyPlanItems = get(),
            observePeriodReviews = get(),
            upsertDailyPlanItem = get(),
            addSuggestedTaskToMyDay = get(),
            updateDailyPlanItemTime = get(),
            smartSchedule = get(),
            sprintManager = get(),
            sprintTransition = get()
        )
    }
    viewModel {
        ReflectViewModel(
            repository = get(),
            observePeriodReviews = get(),
            savePeriodReview = get(),
            buildDraft = get()
        )
    }
    viewModel { SettingsViewModel(get(), get(), get(), get<AppReminderScheduler>()) }
    viewModel {
        NestedListsViewModel(
            observeDocumentsUseCase = get(),
            observeTagsUseCase = get(),
            observeTreeUseCase = get(),
            addDocumentUseCase = get(),
            renameDocumentUseCase = get(),
            deleteDocumentUseCase = get(),
            addItemUseCase = get(),
            updateItemTextUseCase = get(),
            updateItemNoteUseCase = get(),
            updateItemFormattingUseCase = get(),
            updateItemDateRangeUseCase = get(),
            updateItemPriorityUseCase = get(),
            updateItemTagsUseCase = get(),
            updateItemMetricSettingsUseCase = get(),
            replaceNestedManualMetricsUseCase = get(),
            setCheckboxEnabledUseCase = get(),
            setItemsCheckedUseCase = get(),
            toggleCollapsedUseCase = get(),
            moveItemsUseCase = get(),
            deleteItemsUseCase = get(),
            settingsRepository = get()
        )
    }
}
