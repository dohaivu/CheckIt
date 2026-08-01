package com.checkit.ui.myday

import com.checkit.domain.DayReviewConfirmInput
import com.checkit.domain.LeftoverAction
import com.checkit.domain.defaultLeftoverAction
import com.checkit.ui.UiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** Handles the end-of-day review flow: opening, editing, confirming. */
internal class DayReviewController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    fun open() {
        val current = state.uiState.value
        if (current.dayReview != null) return
        val date = current.today
        scope.launch {
            val summary = deps.buildDayReviewSummary(date, current.plan)
            val record = current.dayReviews.firstOrNull { it.date == date }
            val actions = summary.plannedItems.associate { item ->
                item.id to item.defaultLeftoverAction()
            }
            state.update {
                it.copy(
                    dayReview = DayReviewUiState(
                        summary = summary,
                        leftoverActions = actions,
                        winNote = record?.winNote.orEmpty(),
                        tomorrowGoal = record?.tomorrowGoal.orEmpty(),
                        streak = current.reviewStreak
                    ),
                    showDayReviewBanner = false,
                    showLeftoversSheet = false,
                    showSuggestions = false,
                    itemEditor = null
                )
            }
        }
    }

    fun dismiss() {
        state.update { it.copy(dayReview = null) }
    }

    fun setLeftoverAction(itemId: Long, action: LeftoverAction) {
        state.update { current ->
            val review = current.dayReview ?: return@update current
            current.copy(
                dayReview = review.copy(
                    leftoverActions = review.leftoverActions + (itemId to action)
                )
            )
        }
    }

    fun updateWinNote(note: String) {
        state.update { current ->
            val review = current.dayReview ?: return@update current
            current.copy(dayReview = review.copy(winNote = note))
        }
    }

    fun updateTomorrowGoal(goal: String) {
        state.update { current ->
            val review = current.dayReview ?: return@update current
            current.copy(dayReview = review.copy(tomorrowGoal = goal))
        }
    }

    fun confirm() {
        val current = state.uiState.value
        val review = current.dayReview ?: return
        if (review.isSubmitting) return
        state.update { it.copy(dayReview = review.copy(isSubmitting = true)) }
        scope.launch {
            deps.completeDayReview(
                plan = current.plan,
                input = DayReviewConfirmInput(
                    date = review.summary.date,
                    leftoverActions = review.leftoverActions,
                    winNote = review.winNote,
                    tomorrowGoal = review.tomorrowGoal
                )
            ).onSuccess { result ->
                state.update { it.copy(dayReview = null, showDayReviewBanner = false, showCelebration = true) }
                scope.launch {
                    delay(3000.milliseconds)
                    state.update { it.copy(showCelebration = false) }
                }
                val parts = buildList {
                    if (result.carriedCount > 0) add("${result.carriedCount} carried to tomorrow")
                    if (result.markedDoneCount > 0) add("${result.markedDoneCount} marked done")
                    if (result.droppedCount > 0) add("${result.droppedCount} left unfinished")
                    if (result.winNoteSaved) add("win saved")
                }
                state.sendEvent(
                    UiEvent.ShowSnackbar(
                        if (parts.isEmpty()) "Day reviewed" else parts.joinToString(" · ")
                    )
                )
            }.onFailure { error ->
                state.update { currentState ->
                    currentState.copy(
                        dayReview = currentState.dayReview?.copy(isSubmitting = false)
                    )
                }
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to finish review"))
            }
        }
    }
}
