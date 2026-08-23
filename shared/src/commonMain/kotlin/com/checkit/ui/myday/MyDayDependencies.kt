package com.checkit.ui.myday

import com.checkit.data.SettingsRepository
import com.checkit.domain.SprintManager
import com.checkit.domain.TagItem
import com.checkit.domain.usecase.AddJournalEntryUseCase
import com.checkit.domain.usecase.AddSuggestedTaskToMyDayUseCase
import com.checkit.domain.usecase.BuildDayCloseSummaryUseCase
import com.checkit.domain.usecase.CarryOverDailyPlanItemsUseCase
import com.checkit.domain.usecase.CompleteDayCloseUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.DeleteJournalEntryUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveJournalEntriesUseCase
import com.checkit.domain.usecase.ObserveNotesForDateUseCase
import com.checkit.domain.usecase.ObservePeriodReviewsUseCase
import com.checkit.domain.usecase.ObserveTagsUseCase
import com.checkit.domain.usecase.ObserveTasksForDateUseCase
import com.checkit.domain.usecase.ObserveWorkingTasksUseCase
import com.checkit.domain.usecase.SmartScheduleDailyPlanUseCase
import com.checkit.domain.usecase.SprintTransitionUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateJournalEntryUseCase
import com.checkit.domain.usecase.UpsertDailyPlanItemUseCase

/** Bundles the dependencies shared by the My Day feature controllers. */
internal class MyDayDependencies(
    val observeDailyPlans: ObserveDailyPlansUseCase,
    val observeJournalEntries: ObserveJournalEntriesUseCase,
    val observePeriodReviews: ObservePeriodReviewsUseCase,
    val observeTags: ObserveTagsUseCase,
    val observeWorkingTasks: ObserveWorkingTasksUseCase,
    val observeNotesForDate: ObserveNotesForDateUseCase,
    val addJournalEntry: AddJournalEntryUseCase,
    val updateJournalEntry: UpdateJournalEntryUseCase,
    val deleteJournalEntry: DeleteJournalEntryUseCase,
    val deleteDailyPlanItem: DeleteDailyPlanItemUseCase,
    val settingsRepository: SettingsRepository,
    val buildDayCloseSummary: BuildDayCloseSummaryUseCase,
    val completeDayClose: CompleteDayCloseUseCase,
    val carryOverDailyPlanItems: CarryOverDailyPlanItemsUseCase,
    val upsertDailyPlanItem: UpsertDailyPlanItemUseCase,
    val addSuggestedTaskToMyDay: AddSuggestedTaskToMyDayUseCase,
    val updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
    val smartSchedule: SmartScheduleDailyPlanUseCase,
    val sprintManager: SprintManager,
    val sprintTransition: SprintTransitionUseCase
)
