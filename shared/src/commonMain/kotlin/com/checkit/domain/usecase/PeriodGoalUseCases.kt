package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.MetricItem
import com.checkit.domain.FocusPeriod
import com.checkit.domain.PeriodGoal

import kotlin.time.Clock

/**
 * Persists a period goal (upsert) for [focus]. [goal] is stored on this
 * period's own record; [metrics] replaces the goal's custom metrics.
 */
class SavePeriodGoalUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        focus: FocusPeriod,
        review: String,
        goal: String? = null,
        rating: Float = 0f,
        metrics: List<MetricItem> = emptyList()
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        repository.savePeriodGoal(
            PeriodGoal(
                period = focus.period,
                startEpochDays = focus.start.toEpochDays().toInt(),
                endEpochDays = focus.endExclusive.toEpochDays().toInt(),
                review = review.trim(),
                goal = goal?.trim()?.takeIf { it.isNotEmpty() },
                rating = rating,
                completedAtMillis = if (review.isNotBlank()) now else null,
                editedAtMillis = now,
                metrics = metrics
            )
        )
    }
}
