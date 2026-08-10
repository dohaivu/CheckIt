package com.checkit.ui.myday

import com.checkit.data.UserSettings
import com.checkit.domain.CarryOverTimePolicy
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DayCloseBannerPolicy
import com.checkit.domain.JournalEntry
import com.checkit.domain.LeftoversBannerPolicy
import com.checkit.domain.PeriodReview
import com.checkit.domain.PlanAssistBannerPolicy
import com.checkit.domain.ReviewPeriod
import com.checkit.domain.ReviewStreakPolicy
import com.checkit.domain.TaskBoard
import com.checkit.domain.YesterdayLeftovers
import com.checkit.domain.defaultReviewAction
import com.checkit.ui.UiEvent
import com.checkit.ui.currentMyDayTimeMinutes
import com.checkit.ui.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

/** Observes the underlying data sources and derives the My Day UI state. */
internal class MyDayDataLoader(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    private val autoCarryMutex = Mutex()

    fun start() {
        scope.launch {
            combine(
                deps.observeTaskBoard(),
                deps.observeDailyPlans(),
                deps.settingsRepository.settings,
                deps.observePeriodReviews(),
                deps.observeJournalEntries()
            ) { board, dailyPlans, settings, dayReviews, journalEntries ->
                ReviewCombined(board, dailyPlans, settings, dayReviews, journalEntries)
            }
                .catch { error ->
                    state.update { it.copy(isLoading = false) }
                    state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to load My Day"))
                }
                .collect { (board, dailyPlans, settings, periodReviews, journalEntries) ->
                    val date = today()
                    val todayEpoch = date.toEpochDays().toInt()
                    val nowMinutes = currentMyDayTimeMinutes()
                    val plan = dailyPlans.firstOrNull { it.date == date }
                    val dayReviews = periodReviews.filter { it.period == ReviewPeriod.Day }
                    val leftovers = YesterdayLeftovers.items(dailyPlans, date)
                    val pendingLeftovers = YesterdayLeftovers.pendingForToday(leftovers, plan)
                    val reviewStreak = ReviewStreakPolicy.currentStreak(dayReviews, date)
                    val showReviewBanner = DayCloseBannerPolicy.shouldShow(
                        hasPlanItems = plan?.items?.isNotEmpty() == true,
                        reviewReminderEnabled = settings.reviewReminderEnabled,
                        reviewReminderTimeMinutes = settings.reviewReminderTimeMinutes,
                        lastDayCloseEpochDay = settings.lastDayCloseEpochDay,
                        todayEpochDay = todayEpoch,
                        nowMinutes = nowMinutes
                    )
                    val showLeftoversBanner = LeftoversBannerPolicy.shouldShow(
                        pendingCount = pendingLeftovers.size,
                        leftoversBannerDismissedEpochDay = settings.leftoversBannerDismissedEpochDay,
                        todayEpochDay = todayEpoch
                    )
                    val showPlanAssist = PlanAssistBannerPolicy.shouldShow(
                        todayPlanItemCount = plan?.items?.size ?: 0,
                        planReminderEnabled = settings.planReminderEnabled,
                        planReminderTimeMinutes = settings.planReminderTimeMinutes,
                        reviewReminderTimeMinutes = settings.reviewReminderTimeMinutes,
                        lastDayPlanDismissedEpochDay = settings.lastDayPlanDismissedEpochDay,
                        todayEpochDay = todayEpoch,
                        nowMinutes = nowMinutes
                    )
                    maybeAutoCarryOver(settings, pendingLeftovers, date)

                    val summary = deps.buildDayCloseSummary(date, plan)
                    state.update { current ->
                        val updatedReview = current.dayClose?.let { existing ->
                            val validItems = summary.plannedItems + summary.alreadyCarriedItems
                            val validIds = validItems.map { it.id }.toSet()
                            existing.copy(
                                summary = summary,
                                leftoverActions = existing.leftoverActions.filterKeys { it in validIds } +
                                    validItems
                                        .filter { it.id !in existing.leftoverActions }
                                        .associate { it.id to it.defaultReviewAction(dailyPlans) },
                                streak = reviewStreak
                            )
                        }

                        val lastFabAction = when (settings.lastFabActionType) {
                            "TagSprint" -> board.tags.find { it.id == settings.lastFabActionId }?.let { FabAction.TagSprint(it) } ?: FabAction.QuickSprint
                            else -> FabAction.QuickSprint
                        }
                        current.copy(
                            board = board,
                            dailyPlans = dailyPlans,
                            dayClose = updatedReview,
                            journalEntries = journalEntries,
                            showDayCloseBanner = showReviewBanner && updatedReview == null,
                            reviewReminderEnabled = settings.reviewReminderEnabled,
                            reviewReminderTimeMinutes = settings.reviewReminderTimeMinutes,
                            planReminderEnabled = settings.planReminderEnabled,
                            planReminderTimeMinutes = settings.planReminderTimeMinutes,
                            lastDayCloseEpochDay = settings.lastDayCloseEpochDay,
                            lastDayPlanDismissedEpochDay = settings.lastDayPlanDismissedEpochDay,
                            leftoversBannerDismissedEpochDay = settings.leftoversBannerDismissedEpochDay,
                            autoCarryOverLeftovers = settings.autoCarryOverLeftovers,
                            yesterdayLeftovers = leftovers,
                            pendingYesterdayLeftovers = pendingLeftovers,
                            recentTags = board.tags.sortedByDescending { it.lastUsedAtMillis }.take(5),
                            lastFabAction = lastFabAction,
                            dayReviews = dayReviews,
                            reviewStreak = reviewStreak,
                            showLeftoversBanner = showLeftoversBanner &&
                                updatedReview == null &&
                                !current.showLeftoversSheet,
                            showPlanAssistBanner = showPlanAssist &&
                                updatedReview == null &&
                                !current.showSuggestions,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun maybeAutoCarryOver(
        settings: UserSettings,
        pendingLeftovers: List<DailyPlanItem>,
        today: LocalDate
    ) {
        if (!settings.autoCarryOverLeftovers) return
        if (pendingLeftovers.isEmpty()) return
        val todayEpoch = today.toEpochDays().toInt()
        if (settings.autoCarryOverLastRunEpochDay == todayEpoch) return
        scope.launch {
            autoCarryMutex.withLock {
                runCatching {
                    val result = deps.carryOverDailyPlanItems.carryAll(
                        items = pendingLeftovers,
                        toDate = today,
                        timePolicy = CarryOverTimePolicy.ClearTimes
                    )
                    deps.settingsRepository.setAutoCarryOverLastRunEpochDay(todayEpoch)
                    deps.settingsRepository.setLeftoversBannerDismissedEpochDay(todayEpoch)
                    if (result.carriedCount > 0) {
                        state.sendEvent(UiEvent.ShowSnackbar("${result.carriedCount} carried from yesterday"))
                    }
                }
            }
        }
    }
}

private data class ReviewCombined(
    val board: TaskBoard,
    val dailyPlans: List<DailyPlan>,
    val settings: UserSettings,
    val dayReviews: List<PeriodReview>,
    val journalEntries: List<JournalEntry>
)
