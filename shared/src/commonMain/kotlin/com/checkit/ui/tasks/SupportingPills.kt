package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskList
import com.checkit.domain.TaskTag
import com.checkit.ui.components.DetailChip
import com.checkit.ui.components.TaskTagPill
import com.checkit.ui.components.icons.AppIcons
import com.checkit.ui.components.icons.Target
import com.checkit.ui.theme.materialIcon
import com.checkit.ui.theme.toColor

@Composable
internal fun SupportingPills(
    list: TaskList?,
    tags: List<TaskTag>,
    overflowCount: Int = 0
) {
    if (list == null && tags.isEmpty() && overflowCount == 0) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        list?.let {
            val isObjective = remember {  (list.goalId != null)}
            DetailChip(
                icon = if (isObjective) AppIcons.Target else materialIcon(it.icon),
                label = it.name,
                iconTint = it.color.toColor()
            )
        }
        tags.forEach { tag -> TaskTagPill(tag = tag) }
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
