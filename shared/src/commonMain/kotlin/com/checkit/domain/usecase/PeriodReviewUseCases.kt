package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.FocusPeriod
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import kotlin.time.Clock

/**
 * Persists a period review (upsert) for [focus]. [periodIntent] is stored on
 * this period's own review; [nextPeriodIntent] describes the next period, so
 * it is merged into that period's review instead.
 */
class SavePeriodReviewUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        focus: FocusPeriod,
        content: String,
        periodIntent: String? = null,
        nextPeriodIntent: String = "",
        source: ReviewSource = ReviewSource.Manual
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        repository.savePeriodReview(
            PeriodReview(
                period = focus.period,
                periodStartEpochDays = focus.start.toEpochDays().toInt(),
                periodEndEpochDays = focus.endExclusive.toEpochDays().toInt(),
                content = content.trim(),
                periodIntent = periodIntent?.trim()?.takeIf { it.isNotEmpty() },
                source = source,
                status = ReviewStatus.Complete,
                completedAtMillis = if (content.isNotBlank()) now else null,
                generatedAtMillis = if (source == ReviewSource.Manual) null else now,
                editedAtMillis = now
            )
        )
        saveIntentForNextPeriod(focus, nextPeriodIntent, now)
    }

    /** Merges [periodIntent] into the next focus period's review (creating a draft if missing). */
    private suspend fun saveIntentForNextPeriod(focus: FocusPeriod, periodIntent: String, nowMillis: Long) {
        val intent = periodIntent.trim().takeIf { it.isNotEmpty() } ?: return
        val nextFocus = focus.shift(1)
        val existing = repository.periodReviewFor(nextFocus.period, nextFocus.start)
        repository.savePeriodReview(
            (existing ?: PeriodReview(
                period = nextFocus.period,
                periodStartEpochDays = nextFocus.startEpochDays,
                periodEndEpochDays = nextFocus.endExclusive.toEpochDays().toInt()
            )).copy(periodIntent = intent, editedAtMillis = nowMillis)
        )
    }
}
