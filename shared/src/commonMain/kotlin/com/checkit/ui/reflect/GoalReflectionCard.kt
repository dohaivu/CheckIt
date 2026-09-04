package com.checkit.ui.reflect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.calendar_open_review
import checkit.shared.generated.resources.reflect_review_card_title
import checkit.shared.generated.resources.reflect_review_empty
import com.checkit.ui.color
import com.checkit.ui.components.MetricChip
import com.checkit.ui.components.RatingBar
import com.checkit.ui.components.asAnnotatedString
import com.checkit.ui.gradient
import com.checkit.ui.periodDetail
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GoalReflectionCard(
    state: ReflectUiState,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goal = state.focusGoal
    val periodLabel = state.focus.periodDetail()
    val color = state.focus.period.color()
    val gradient = state.focus.period.gradient()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.14f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onOpenEditor)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = state.focus.period.reviewIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = color
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = periodLabel.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (goal != null && goal.rating > 0) {
                        RatingBar(
                            rating = goal.rating,
                            modifier = Modifier.width(80.dp).height(16.dp),
                            iconTint = color
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(Res.string.calendar_open_review),
                        modifier = Modifier.size(16.dp),
                        tint = color.copy(alpha = 0.7f)
                    )
                }
            }

            // Target goal if set (elegant, quiet blockquote accent)
            goal?.goal?.takeIf { it.isNotBlank() }?.let { periodGoal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(
                                color = color.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(1.5.dp)
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = periodGoal,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Review text
            if (goal == null || goal.review.isBlank()) {
                Text(
                    text = stringResource(Res.string.reflect_review_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = goal.review.asAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Metric chips
            if (goal != null && goal.metrics.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    goal.metrics.forEach { metric ->
                        MetricChip(metric)
                    }
                }
            }
        }
    }
}
