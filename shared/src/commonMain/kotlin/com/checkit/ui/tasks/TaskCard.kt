package com.checkit.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.ui.components.priorityColor

@Composable
internal fun TaskCard(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    timeLabel: String? = null,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    minHeight: Dp = 64.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
    titleMaxLines: Int = 2,
    completed: Boolean = false,
    containerAlpha: Float = 0.11f,
    tonalElevation: Dp = 0.dp
) {
    val cardContent = @Composable {
        Box {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .heightIn(min = minHeight)
                    .background(color.copy(alpha = containerAlpha))
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(color)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (leadingContent != null) {
                        Box(
                            modifier = Modifier.padding(top = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            leadingContent()
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = titleMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (timeLabel != null) {
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (supportingText != null) {
                            Text(
                                text = supportingText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (completed) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = CompletedRowCoverAlpha))
                )
            }
        }
    }

    if (onClick != null) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = tonalElevation,
            onClick = onClick,
            content = cardContent
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = tonalElevation,
            content = cardContent
        )
    }
}