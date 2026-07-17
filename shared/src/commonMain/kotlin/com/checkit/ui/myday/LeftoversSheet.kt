package com.checkit.ui.myday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.leftovers_banner_carry_all
import checkit.shared.generated.resources.leftovers_item_carry
import checkit.shared.generated.resources.leftovers_sheet_empty
import checkit.shared.generated.resources.leftovers_sheet_title
import com.checkit.domain.DailyPlanItem
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.tasks.isOverdue
import com.checkit.ui.tasks.views.DailyPlanTimelineCard
import com.checkit.ui.today
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LeftoversSheet(
    items: List<DailyPlanItem>,
    onDismiss: () -> Unit,
    onCarry: (DailyPlanItem) -> Unit,
    onCarryAll: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.leftovers_sheet_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (items.isNotEmpty()) {
                    TextButton(onClick = onCarryAll) {
                        Text(stringResource(Res.string.leftovers_banner_carry_all))
                    }
                }
            }
            if (items.isEmpty()) {
                Text(
                    text = stringResource(Res.string.leftovers_sheet_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        LeftoverItemRow(
                            item = item,
                            onCarry = { onCarry(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftoverItemRow(
    item: DailyPlanItem,
    onCarry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            DailyPlanTimelineCard(
                item = item,
                isOverdue = item.isOverdue(today())
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCarry) {
                    Text(stringResource(Res.string.leftovers_item_carry))
                }
            }
        }
    }
}
