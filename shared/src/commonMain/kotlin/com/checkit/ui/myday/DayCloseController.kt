package com.checkit.ui.myday

import com.checkit.domain.DayCloseConfirmInput
import com.checkit.domain.LeftoverAction
import com.checkit.domain.defaultReviewAction
import com.checkit.ui.UiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.milliseconds

/** Handles the end-of-day review flow: opening, editing, confirming. */
internal class DayCloseController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    fun open() {
        if (state.uiState.value.dayClose != null) return
        scope.launch {
            // Wait for the initial data load so a cold-start tap (e.g. review reminder
            // notification) does not build the summary from an empty plan.
            val loaded = state.uiState.first { !it.isLoading }
            if (loaded.dayClose != null) return@launch
            val date = loaded.today
            val summary = deps.buildDayCloseSummary(date, loaded.plan)
            val record = loaded.dayReviews.firstOrNull { it.periodStartDate == date }
            // The tomorrow goal is stored as the next day's period intent.
            val tomorrowRecord = loaded.dayReviews.firstOrNull {
                it.periodStartDate == date.plus(1, DateTimeUnit.DAY)
            }
            val allItems = summary.plannedItems + summary.alreadyCarriedItems
            val actions = allItems.associate { item ->
                item.id to item.defaultReviewAction(loaded.dailyPlans)
            }
            state.update {
                it.copy(
                    dayClose = DayCloseUiState(
                        summary = summary,
                        leftoverActions = actions,
                        winNote = record?.content.orEmpty(),
                        tomorrowGoal = tomorrowRecord?.periodIntent.orEmpty(),
                        streak = loaded.reviewStreak
                    ),
                    showDayCloseBanner = false,
                    showLeftoversSheet = false,
                    showSuggestions = false,
                    itemEditor = null
                )
            }
        }
    }

    fun dismiss() {
        state.update { it.copy(dayClose = null) }
    }

    fun setLeftoverAction(itemId: Long, action: LeftoverAction) {
        state.update { current ->
            val review = current.dayClose ?: return@update current
            current.copy(
                dayClose = review.copy(
                    leftoverActions = review.leftoverActions + (itemId to action)
                )
            )
        }
    }

    fun updateWinNote(note: String) {
        state.update { current ->
            val review = current.dayClose ?: return@update current
            current.copy(dayClose = review.copy(winNote = note))
        }
    }

    fun updateTomorrowGoal(goal: String) {
        state.update { current ->
            val review = current.dayClose ?: return@update current
            current.copy(dayClose = review.copy(tomorrowGoal = goal))
        }
    }

    fun confirm() {
        val current = state.uiState.value
        val review = current.dayClose ?: return
        if (review.isSubmitting) return
        state.update { it.copy(dayClose = review.copy(isSubmitting = true)) }
        scope.launch {
            deps.completeDayClose(
                plan = current.plan,
                input = DayCloseConfirmInput(
                    date = review.summary.date,
                    leftoverActions = review.leftoverActions,
                    winNote = review.winNote,
                    tomorrowGoal = review.tomorrowGoal
                )
            ).onSuccess { result ->
                state.update { it.copy(dayClose = null, showDayCloseBanner = false, showCelebration = true) }
                scope.launch {
                    delay(3000.milliseconds)
                    state.update { it.copy(showCelebration = false) }
                }

                val parts = buildList {
                    if (result.carriedCount > 0) add("${result.carriedCount} carried to tomorrow")
                    if (result.markedDoneCount > 0) add("${result.markedDoneCount} marked done")
                    if (result.droppedCount > 0) add("${result.droppedCount} left unfinished")
                    if (result.winNoteSaved) add("day close saved")
                }
                state.sendEvent(
                    UiEvent.ShowSnackbar(
                        if (parts.isEmpty()) "Day reviewed" else parts.joinToString(" · ")
                    )
                )
            }.onFailure { error ->
                state.update { currentState ->
                    currentState.copy(
                        dayClose = currentState.dayClose?.copy(isSubmitting = false)
                    )
                }
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to finish review"))
            }
        }
    }
}
