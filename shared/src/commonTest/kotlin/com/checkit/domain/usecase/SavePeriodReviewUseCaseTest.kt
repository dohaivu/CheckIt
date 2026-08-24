package com.checkit.domain.usecase

import com.checkit.domain.FocusPeriod
import com.checkit.domain.Period
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun savesNextPeriodIntentOnNextFocusPeriodReview() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodReviewUseCase(repository)
        val focus = FocusPeriod(Period.Week, date)

        save(focus, content = "Solid week", nextPeriodIntent = "  Start the refactor  ")

        val reviews = repository.observePeriodReviews().first()
        val nextFocus = focus.shift(1)
        val nextReview = assertNotNull(
            repository.periodReviewFor(nextFocus.period, nextFocus.start),
            "Intent should be stored on the next focus period's review"
        )
        assertEquals("Start the refactor", nextReview.periodIntent)
        assertEquals(2, reviews.size)

        // The reviewed period itself carries no intent.
        val review = reviews.single { it.periodStartEpochDays == focus.start.toEpochDays().toInt() }
        assertNull(review.periodIntent)
    }

    @Test
    fun nextPeriodIntentMergesIntoExistingNextPeriodReview() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodReviewUseCase(repository)
        val focus = FocusPeriod(Period.Day, date)
        val nextFocus = focus.shift(1)
        repository.savePeriodReview(
            PeriodReview(
                period = Period.Day,
                periodStartEpochDays = nextFocus.startEpochDays,
                periodEndEpochDays = nextFocus.endInclusiveEpochDays + 1,
                content = "Already written"
            )
        )

        save(focus, content = "Done", nextPeriodIntent = "Rest")

        val nextReview = assertNotNull(repository.periodReviewFor(Period.Day, nextFocus.start))
        assertEquals("Already written", nextReview.content)
        assertEquals(ReviewSource.Manual, nextReview.source)
        assertEquals("Rest", nextReview.periodIntent)
    }

    @Test
    fun savesWithoutIntentsWhenBlank() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodReviewUseCase(repository)
        val focus = FocusPeriod(Period.Month, date)

        save(focus, content = "Good month", periodIntent = "   ", nextPeriodIntent = "   ")

        val review = repository.observePeriodReviews().first().single()
        assertNull(review.periodIntent)
    }

    @Test
    fun savingTwiceUpsertsTheSamePeriods() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodReviewUseCase(repository)
        val focus = FocusPeriod(Period.Day, date)

        save(focus, content = "First pass", nextPeriodIntent = "")
        save(focus, content = "Second pass", nextPeriodIntent = "Keep going")

        val reviews = repository.observePeriodReviews().first()
        assertEquals(2, reviews.size)
        val review = reviews.single { it.periodStartEpochDays == focus.start.toEpochDays().toInt() }
        assertEquals("Second pass", review.content)
        val nextReview = reviews.single { it.periodStartEpochDays == focus.shift(1).start.toEpochDays().toInt() }
        assertEquals("Keep going", nextReview.periodIntent)
    }
}
