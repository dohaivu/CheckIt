package com.checkit.domain

import kotlinx.datetime.LocalDate

/**
 * A single narrative document for one period (day | week | month | quarter | year).
 * At most one record per [period] + [startEpochDays] (unique index).
 * Numeric stats are not stored here; they are derived from the daily rollup tables.
 */
data class PeriodGoal(
    val id: Long = 0L,
    val period: Period = Period.Day,
    val startEpochDays: Int,
    val endEpochDays: Int,
    val review: String = "",
    val goal: String? = null,
    /** Satisfaction for this period (e.g. 0..5). */
    val rating: Float = 0f,
    val completedAtMillis: Long? = null,
    val editedAtMillis: Long? = null,
    /** Custom/manual metrics tracked for this period. */
    val metrics: List<PeriodMetric> = emptyList()
) {
    val startDate: LocalDate get() = LocalDate.fromEpochDays(startEpochDays)
    val endDateInclusive: LocalDate get() = LocalDate.fromEpochDays(endEpochDays - 1)
}

/**
 * A manually tracked metric attached to a [PeriodGoal], mirroring
 * [NestedManualMetric]: free-form name/value pairs with an optional unit.
 */
data class PeriodMetric(
    val name: String,
    val value: String,
    val targetValue: String? = null,
    val unit: MetricUnit = MetricUnit.None,
    val customUnit: String? = null,
    val sortOrder: Int = 0,
    val enabled: Boolean = true
)
