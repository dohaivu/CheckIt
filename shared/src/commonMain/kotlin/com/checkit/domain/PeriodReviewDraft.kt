package com.checkit.domain

import kotlinx.serialization.Serializable

/** Structured stats snapshot stored on a generated (Hybrid/Auto) review. */
@Serializable
data class PeriodReviewDraftStats(
    val doneCount: Int,
    val plannedCount: Int,
    val totalMinutes: Int,
    val topTags: List<PeriodReviewDraftTag>
)

@Serializable
data class PeriodReviewDraftTag(
    val name: String,
    val color: String?,
    val totalMinutes: Int
)

/** A single completed item highlighted in a generated review. */
@Serializable
data class PeriodReviewDraftHighlight(
    val dateEpochDays: Int,
    val title: String,
    val minutes: Int
)

/** In-memory draft produced for a period focus, ready to seed the review editor. */
data class PeriodReviewDraft(
    val content: String,
    val statsJson: String,
    val highlightsJson: String
)
