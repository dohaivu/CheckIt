package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.FocusPeriod
import com.checkit.domain.PeriodGoal
import kotlin.time.Clock

/**
 * Persists a period goal (upsert) for [focus]. [goal] is stored on this
 * period's own record.
 */
class SavePeriodGoalUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        focus: FocusPeriod,
        review: String,
        goal: String? = null,
        ratings: Float = 0f
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        repository.savePeriodGoal(
            PeriodGoal(
                period = focus.period,
                startEpochDays = focus.start.toEpochDays().toInt(),
                endEpochDays = focus.endExclusive.toEpochDays().toInt(),
                review = review.trim(),
                goal = goal?.trim()?.takeIf { it.isNotEmpty() },
                ratings = ratings,
                completedAtMillis = if (review.isNotBlank()) now else null,
                editedAtMillis = now
            )
        )
    }
}
