package com.checkit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.Goal
import com.checkit.domain.KeyResult
import com.checkit.domain.ListItem
import com.checkit.domain.PlanPriority
import com.checkit.domain.TagItem
import com.checkit.domain.TwelveWeekGoal
import com.checkit.ui.components.icons.AppIcons
import com.checkit.ui.components.icons.Target
import com.checkit.ui.plan.PlanPriorityPill
import com.checkit.ui.theme.materialIcon
import com.checkit.ui.theme.toColor

@Composable
internal fun SupportingPills(
    list: ListItem? = null,
    planPriority: PlanPriority? = null,
    keyResult: KeyResult? = null,
    tags: List<TagItem> = emptyList(),
    overflowCount: Int = 0,
    modifier: Modifier = Modifier
) {
    if (list == null && planPriority == null && keyResult == null && tags.isEmpty() && overflowCount == 0) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        list?.let {
            DetailChip(
                icon = materialIcon(it.icon),
                label = it.title,
                iconTint = it.color.toColor()
            )
        }
        planPriority?.let { PlanPriorityPill(it) }
        keyResult?.let { KeyResultPill(it) }

        tags.forEach { tag -> TagPill(tag = tag) }
        if (overflowCount > 0) {
            Text(
                text = "+$overflowCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
internal fun KeyResultPill(keyResult: KeyResult) {
    DetailChip(
        icon = Icons.Default.Bolt,
        label = keyResult.title,
        iconTint = MaterialTheme.colorScheme.primary
    )
}

@Composable
internal fun TwelveWeekGoalPill(goal: TwelveWeekGoal) {
    DetailChip(
        icon = AppIcons.Target,
        label = goal.title,
        iconTint = MaterialTheme.colorScheme.primary
    )
}
