package com.checkit.ui.myday

import com.checkit.data.UserSettings
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.JournalEntry
import com.checkit.domain.LeftoversBannerPolicy
import com.checkit.domain.NoteItem
import com.checkit.domain.PeriodGoal
import com.checkit.domain.Period
import com.checkit.domain.startOf
import com.checkit.domain.TagItem
import com.checkit.domain.TaskItem
import com.checkit.domain.YesterdayLeftovers
import com.checkit.domain.defaultReviewAction
import com.checkit.ui.UiEvent
import com.checkit.ui.currentMyDayTimeMinutes
import com.checkit.ui.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Observes the underlying data sources and derives the My Day UI state. */
internal class MyDayDataLoader(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    fun start() {
        val today = today()
        scope.launch {
            combine(
                deps.observeWorkingTasks(today),
                deps.observeNotesForDate(today),
                deps.observeTags(),
                deps.observeDailyPlans(startDate = today.minus(1, DateTimeUnit.DAY), endDate = today),
                deps.settingsRepository.settings,
                deps.observePeriodGoals(
                    startDate = minOf(Period.Month.startOf(today), Period.Week.startOf(today)),
                    endDateInclusive = today
                ),
                deps.observeJournalEntries(startDate = today, endDateInclusive = today)
            ) { array ->
                ReviewCombined(
                    tasks = array[0] as List<TaskItem>,
                    notes = array[1] as List<NoteItem>,
                    tags = array[2] as List<TagItem>,
                    dailyPlans = array[3] as List<DailyPlan>,
                    settings = array[4] as UserSettings,
                    periodGoals = array[5] as List<PeriodGoal>,
                    journalEntries = array[6] as List<JournalEntry>
                )
            }
                .catch { error ->
                    state.update { it.copy(isLoading = false) }
                    state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to load My Day"))
                }
                .collect { combined ->
                    val date = today()
                    val todayEpoch = date.toEpochDays().toInt()
                    val nowMinutes = currentMyDayTimeMinutes()
                    val plan = combined.dailyPlans.firstOrNull { it.date == date }
                    val periodGoals = combined.periodGoals
                    val leftovers = YesterdayLeftovers.items(combined.dailyPlans, date)
                    val pendingLeftovers = YesterdayLeftovers.pendingForToday(leftovers, plan)
                    val showLeftoversBanner = LeftoversBannerPolicy.shouldShow(
                        pendingCount = pendingLeftovers.size,
                        leftoversBannerDismissedEpochDay = combined.settings.leftoversBannerDismissedEpochDay,
                        todayEpochDay = todayEpoch
                    )

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
                                        .associate { it.id to it.defaultReviewAction(combined.dailyPlans) }
                            )
                        }

                        val lastFabAction = when (combined.settings.lastFabActionType) {
                            "TagSprint" -> combined.tags.find { it.id == combined.settings.lastFabActionId }?.let { FabAction.TagSprint(it) } ?: FabAction.QuickSprint
                            else -> FabAction.QuickSprint
                        }
                        current.copy(
                            tasks = combined.tasks,
                            notes = combined.notes,
                            tags = combined.tags,
                            dailyPlans = combined.dailyPlans,
                            dayClose = updatedReview,
                            journalEntries = combined.journalEntries,
                            reviewReminderEnabled = combined.settings.reviewReminderEnabled,
                            reviewReminderTimeMinutes = combined.settings.reviewReminderTimeMinutes,
                            planReminderEnabled = combined.settings.planReminderEnabled,
                            planReminderTimeMinutes = combined.settings.planReminderTimeMinutes,
                            leftoversBannerDismissedEpochDay = combined.settings.leftoversBannerDismissedEpochDay,
                            yesterdayLeftovers = leftovers,
                            pendingYesterdayLeftovers = pendingLeftovers,
                            recentTags = combined.tags.sortedByDescending { it.lastUsedAtMillis }.take(5),
                            lastFabAction = lastFabAction,
                            periodGoals = periodGoals,
                            recentLabels = combined.settings.recentLabels,
                            nowMinutes = nowMinutes,
                            showLeftoversBanner = showLeftoversBanner &&
                                updatedReview == null &&
                                !current.showLeftoversSheet,
                            isLoading = false
                        )
                    }
                }
        }
    }
}

private data class ReviewCombined(
    val tasks: List<TaskItem>,
    val notes: List<NoteItem>,
    val tags: List<TagItem>,
    val dailyPlans: List<DailyPlan>,
    val settings: UserSettings,
    val periodGoals: List<PeriodGoal>,
    val journalEntries: List<JournalEntry>
)
