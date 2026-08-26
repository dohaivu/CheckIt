package com.checkit.domain.usecase

import com.checkit.domain.FocusPeriod
import com.checkit.domain.Period
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
        val focus = FocusPeriod(Period.Week, date)

        save(focus, content = "  Solid week, shipped the PR.  ")

        val review = repository.observePeriodReviews().first().single()
        assertEquals(Period.Week, review.period)
        assertEquals(focus.start.toEpochDays().toInt(), review.periodStartEpochDays)
        assertEquals(focus.endExclusive.toEpochDays().toInt(), review.periodEndEpochDays)
        assertEquals("Solid week, shipped the PR.", review.content)
        assertNull(review.periodIntent)
        assertEquals(ReviewSource.Manual, review.source)
        assertEquals(ReviewStatus.Complete, review.status)
        assertEquals(true, review.isComplete)
    }

    @Test
    fun savesPeriodIntentOnCurrentPeriodReview() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodReviewUseCase(repository)
        val focus = FocusPeriod(Period.Week, date)

        save(focus, content = "Solid week", periodIntent = "  Ship the refactor  ")

        val review = repository.observePeriodReviews().first().single()
        assertEquals("Ship the refactor", review.periodIntent)
    }

    @Test
    fun savesWithoutIntentWhenBlank() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodReviewUseCase(repository)
        val focus = FocusPeriod(Period.Month, date)

        save(focus, content = "Good month", periodIntent = "   ")

        val review = repository.observePeriodReviews().first().single()
        assertNull(review.periodIntent)
    }

    @Test
    fun savingTwiceUpsertsTheSamePeriod() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodReviewUseCase(repository)
        val focus = FocusPeriod(Period.Day, date)

        save(focus, content = "First pass")
        save(focus, content = "Second pass", periodIntent = "Keep going")

        val reviews = repository.observePeriodReviews().first()
        assertEquals(1, reviews.size)
        assertEquals("Second pass", reviews.single().content)
        assertEquals("Keep going", reviews.single().periodIntent)
    }
}
