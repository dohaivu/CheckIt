package com.checkit.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp

object RatingBarDefaults {
    /** 5 Stars: Excellent */
    val ExcellentColor = Color(0xFF16A34A)
    /** 4 Stars: Above expectations */
    val AboveExpectationsColor = Color(0xFF65A30D)
    /** 3 Stars: Meets basic expectations */
    val MeetsExpectationsColor = Color(0xFFCA8A04)
    /** 2 Stars: Needs improvement */
    val NeedsImprovementColor = Color(0xFFEA580C)
    /** 1 Star: Failed expectations */
    val FailedExpectationsColor = Color(0xFFDC2626)

    /**
     * Returns a color based on the rating value.
     */
    fun getRatingColor(rating: Float): Color = when {
        rating >= 4.5f -> ExcellentColor
        rating >= 3.5f -> AboveExpectationsColor
        rating >= 2.5f -> MeetsExpectationsColor
        rating >= 1.5f -> NeedsImprovementColor
        rating > 0f -> FailedExpectationsColor
        else -> Color.Unspecified
    }
}

@Composable
fun RatingBar(
    rating: Float,
    onRatingChange: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier.Companion,
    enabled: Boolean = true,
    useRatingColors: Boolean = true,
    iconTint: Color = Color.Unspecified
) {
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val activeColor = when {
        iconTint != Color.Unspecified -> iconTint
        useRatingColors -> RatingBarDefaults.getRatingColor(rating).takeOrElse { themeSecondary }
        else -> themeSecondary
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val starValue = index + 1f
            val isFull = rating >= starValue
            val isHalf = rating >= starValue - 0.5f && !isFull

            val icon = when {
                isFull -> Icons.Filled.Star
                isHalf -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }

            val tint = if (isFull || isHalf) {
                activeColor
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .then(
                        if (onRatingChange != null) {
                            Modifier.pointerInput(enabled, rating) {
                                if (enabled) {
                                    detectTapGestures { offset ->
                                        val isLeft = offset.x < size.width / 2
                                        val newRating = if (isLeft) starValue - 0.5f else starValue
                                        // Toggle logic: if tapping 0.5 and it's already 0.5, set to 0
                                        val finalRating =
                                            if (newRating == 0.5f && rating == 0.5f) 0f else newRating
                                        onRatingChange(finalRating)
                                    }
                                }
                            }
                        } else Modifier.Companion
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Rate $starValue stars",
                    tint = tint,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}