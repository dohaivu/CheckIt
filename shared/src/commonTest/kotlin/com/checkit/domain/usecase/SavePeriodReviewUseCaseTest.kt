package com.checkit.domain.usecase

import com.checkit.domain.PeriodFocus
import com.checkit.domain.ReviewPeriod
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavePeriodReviewUseCaseTest {
    private val date = LocalDate(2026, 7, 9)

    @Test
    fun savesCompletedManualReviewWithPeriodBounds() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodReviewUseCase(repository)
        val focus = PeriodFocus(ReviewPeriod.Week, date)

        save(focus, content = "  Solid week, shipped the PR.  ", intentNext = "  Start the refactor  ")

        val review = repository.observePeriodReviews().first().single()
        assertEquals(ReviewPeriod.Week, review.period)
        assertEquals(focus.start.toEpochDays().toInt(), review.periodStartEpochDays)
        assertEquals(focus.endExclusive.toEpochDays().toInt(), review.periodEndEpochDays)
        assertEquals("Solid week, shipped the PR.", review.content)
        assertEquals("Start the refactor", review.intentNext)
        assertEquals(ReviewSource.Manual, review.source)
        assertEquals(ReviewStatus.Complete, review.status)
        assertEquals(true, review.isComplete)
    }

    @Test
    fun savesWithoutIntentNextWhenBlank() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodReviewUseCase(repository)
        val focus = PeriodFocus(ReviewPeriod.Month, date)

        save(focus, content = "Good month", intentNext = "   ")

        val review = repository.observePeriodReviews().first().single()
        assertNull(review.intentNext)
    }

    @Test
    fun savingTwiceUpsertsTheSamePeriod() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodReviewUseCase(repository)
        val focus = PeriodFocus(ReviewPeriod.Day, date)

        save(focus, content = "First pass", intentNext = "")
        save(focus, content = "Second pass", intentNext = "Keep going")

        val reviews = repository.observePeriodReviews().first()
        assertEquals(1, reviews.size)
        assertEquals("Second pass", reviews.single().content)
        assertEquals("Keep going", reviews.single().intentNext)
    }
}
