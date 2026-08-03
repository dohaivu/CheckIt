package com.checkit.ui.myday

import com.checkit.data.SettingsRepository
import com.checkit.domain.SprintManager
import com.checkit.domain.usecase.AddSuggestedTaskToMyDayUseCase
import com.checkit.domain.usecase.BuildDayReviewSummaryUseCase
import com.checkit.domain.usecase.CarryOverDailyPlanItemsUseCase
import com.checkit.domain.usecase.CompleteDayReviewUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveDayReviewsUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.SprintTransitionUseCase
import com.checkit.domain.usecase.SyncKeyResultFromDailyPlanUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpsertDailyPlanItemUseCase

/** Bundles the dependencies shared by the My Day feature controllers. */
internal class MyDayDependencies(
    val observeTaskBoard: ObserveTaskBoardUseCase,
    val observeDailyPlans: ObserveDailyPlansUseCase,
    val deleteDailyPlanItem: DeleteDailyPlanItemUseCase,
    val settingsRepository: SettingsRepository,
    val buildDayReviewSummary: BuildDayReviewSummaryUseCase,
    val completeDayReview: CompleteDayReviewUseCase,
    val carryOverDailyPlanItems: CarryOverDailyPlanItemsUseCase,
    val observeDayReviews: ObserveDayReviewsUseCase,
    val upsertDailyPlanItem: UpsertDailyPlanItemUseCase,
    val addSuggestedTaskToMyDay: AddSuggestedTaskToMyDayUseCase,
    val syncKeyResultFromDailyPlan: SyncKeyResultFromDailyPlanUseCase,
    val updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
    val sprintManager: SprintManager,
    val sprintTransition: SprintTransitionUseCase
)
