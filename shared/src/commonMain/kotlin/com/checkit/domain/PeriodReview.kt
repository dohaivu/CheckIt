package com.checkit.domain

import kotlinx.datetime.LocalDate

enum class ReviewSource {
    Auto,
    Manual
}

enum class ReviewStatus {
    Draft,
    Complete
}

/**
 * A single narrative document for one period (day | week | month | quarter | year).
 * At most one record per [period] + [periodStartEpochDays] (unique index).
 * Numeric stats are not stored here; they are derived from the daily rollup tables.
 */
data class PeriodReview(
    val id: Long = 0L,
    val period: Period = Period.Day,
    val periodStartEpochDays: Int,
    val periodEndEpochDays: Int,
    val content: String = "",
    val intentNext: String? = null,
    val source: ReviewSource = ReviewSource.Manual,
    val status: ReviewStatus = ReviewStatus.Draft,
    val completedAtMillis: Long? = null,
    val generatedAtMillis: Long? = null,
    val editedAtMillis: Long? = null
) {
    val periodStartDate: LocalDate get() = LocalDate.fromEpochDays(periodStartEpochDays)
    val isComplete: Boolean get() = status == ReviewStatus.Complete
}
