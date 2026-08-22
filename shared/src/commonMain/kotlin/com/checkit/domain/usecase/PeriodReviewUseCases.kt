package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.FocusPeriod
import com.checkit.domain.Period
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import kotlin.time.Clock

/** Persists a period review (upsert) for the given focus. */
class SavePeriodReviewUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        focus: FocusPeriod,
        content: String,
        intentNext: String,
        source: ReviewSource = ReviewSource.Manual
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        repository.savePeriodReview(
            PeriodReview(
                period = focus.period,
                periodStartEpochDays = focus.start.toEpochDays().toInt(),
                periodEndEpochDays = focus.endExclusive.toEpochDays().toInt(),
                content = content.trim(),
                intentNext = intentNext.trim().takeIf { it.isNotEmpty() },
                source = source,
                status = ReviewStatus.Complete,
                completedAtMillis = if (content.isNotBlank()) now else null,
                generatedAtMillis = if (source == ReviewSource.Manual) null else now,
                editedAtMillis = now
            )
        )
    }
}
