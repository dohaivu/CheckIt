package com.checkit.ui.myday

import com.checkit.domain.CarryOverTimePolicy
import com.checkit.domain.DailyPlanItem
import com.checkit.ui.UiEvent
import com.checkit.ui.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Handles yesterday's leftovers: sheet, banner, and carry-over actions. */
internal class LeftoversController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    fun openSheet() {
        state.update {
            it.copy(
                showLeftoversSheet = true,
                showLeftoversBanner = false,
                showSuggestions = false,
                itemEditor = null
            )
        }
    }

    fun dismissSheet() {
        state.update { it.copy(showLeftoversSheet = false) }
    }

    fun dismissBanner() {
        val todayEpoch = today().toEpochDays().toInt()
        scope.launch {
            deps.settingsRepository.setLeftoversBannerDismissedEpochDay(todayEpoch)
        }
        state.update {
            it.copy(
                showLeftoversBanner = false,
                leftoversBannerDismissedEpochDay = todayEpoch
            )
        }
    }

    fun carryAll() {
        val current = state.uiState.value
        val items = current.pendingYesterdayLeftovers
        if (items.isEmpty()) return
        scope.launch {
            runCatching {
                deps.carryOverDailyPlanItems.carryAll(
                    items = items,
                    toDate = current.today,
                    timePolicy = CarryOverTimePolicy.ClearTimes
                )
            }.onSuccess { result ->
                val todayEpoch = current.today.toEpochDays().toInt()
                deps.settingsRepository.setLeftoversBannerDismissedEpochDay(todayEpoch)
                deps.settingsRepository.setAutoCarryOverLastRunEpochDay(todayEpoch)
                state.update {
                    it.copy(
                        showLeftoversBanner = false,
                        showLeftoversSheet = false
                    )
                }
                state.sendEvent(
                    UiEvent.ShowSnackbar(
                        when {
                            result.carriedCount > 0 && result.skippedCount > 0 ->
                                "${result.carriedCount} carried · ${result.skippedCount} already on today"
                            result.carriedCount > 0 ->
                                "${result.carriedCount} carried from yesterday"
                            else -> "Nothing new to carry"
                        }
                    )
                )
            }.onFailure { error ->
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to carry leftovers"))
            }
        }
    }

    fun carryItem(item: DailyPlanItem) {
        scope.launch {
            runCatching {
                deps.carryOverDailyPlanItems(
                    items = listOf(item),
                    itemIds = setOf(item.id),
                    toDate = today(),
                    timePolicy = CarryOverTimePolicy.ClearTimes
                )
            }.onSuccess { result ->
                state.sendEvent(
                    UiEvent.ShowSnackbar(
                        if (result.carriedCount > 0) "Carried to today" else "Already on today"
                    )
                )
            }.onFailure { error ->
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to carry item"))
            }
        }
    }
}
