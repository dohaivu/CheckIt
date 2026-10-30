package com.checkit.ui.nested

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderStyle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.nested_edit_note
import com.checkit.domain.MetricItem
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.MoveDestinations
import com.checkit.domain.NestedBorderShape
import com.checkit.domain.NestedBorderStyle
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedMetricSummary
import com.checkit.domain.NestedSortOrder
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.CompactFlatTextField
import com.checkit.ui.components.MarkdownTextField
import com.checkit.ui.components.MetricsSection
import com.checkit.ui.components.asMarkdownAnnotatedString
import com.checkit.ui.isValidForSave
import com.checkit.ui.tasks.PredefinedLabels
import com.checkit.ui.toDateTimeLabel
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun NestedNoteEditorSheet(
    itemId: String,
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember(itemId) { mutableStateOf(initialText) }
    var preview by remember(itemId) { mutableStateOf(initialText.isNotEmpty()) }
    val scrollState = rememberScrollState()
    LaunchedEffect(preview) { scrollState.scrollTo(0) }

    AppEditorBottomSheet(
        onDismiss = onDismiss,
        sheetGesturesEnabled = false,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.nested_edit_note),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { preview = !preview }) {
                Text(if (preview) "Edit" else "Preview")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (preview) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (note.isBlank()) {
                        Text(
                            text = "Nothing to preview yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = note.asMarkdownAnnotatedString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    MarkdownTextField(
                        value = note,
                        onValueChange = { note = it.take(NESTED_NOTE_MAX_LENGTH) },
                        placeholder = "Add label or details",
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        minLines = 12,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${note.length}/5,000",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(note) }) {
                    Text("Save")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun MoveToDialog(
    itemText: String,
    destinations: MoveDestinations,
    childCounts: Map<String, Int>,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var selectedId by remember(destinations) {
        mutableStateOf(
            destinations.siblings.firstOrNull()?.id
                ?: destinations.parentSiblings.firstOrNull()?.id
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = itemText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                MoveDestinationGroup(
                    title = "Siblings",
                    items = destinations.siblings,
                    childCounts = childCounts,
                    selectedId = selectedId,
                    onSelect = { selectedId = it }
                )
                MoveDestinationGroup(
                    title = "Parent's siblings",
                    items = destinations.parentSiblings,
                    childCounts = childCounts,
                    selectedId = selectedId,
                    onSelect = { selectedId = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedId?.let(onApply) },
                enabled = selectedId != null
            ) {
                Text("Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
internal fun MoveDestinationGroup(
    title: String,
    items: List<NestedListItem>,
    childCounts: Map<String, Int>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    if (items.isEmpty()) return
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp)
    )
    items.forEach { item ->
        val childCount = childCounts[item.id] ?: 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSelect(item.id) }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedId == item.id,
                onClick = { onSelect(item.id) }
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.text.ifBlank { "Untitled item" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (childCount > 0) "$childCount children" else "No children",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun SortChildrenDialog(
    itemText: String,
    onDismiss: () -> Unit,
    onApply: (NestedSortOrder) -> Unit
) {
    var selected by remember { mutableStateOf(NestedSortOrder.NameAsc) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort children") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = itemText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                NestedSortOrder.entries.forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = order }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == order,
                            onClick = { selected = order }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = order.label(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selected) }) {
                Text("Sort")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

internal fun NestedSortOrder.label(): String = when (this) {
    NestedSortOrder.NameAsc -> "Name (A–Z)"
    NestedSortOrder.AddedDesc -> "Newest first"
    NestedSortOrder.CompletedDesc -> "Recently completed"
    NestedSortOrder.UncheckedFirst -> "Unchecked first"
    NestedSortOrder.PriorityDesc -> "Priority (high first)"
}

@Composable
internal fun NestedLabelDialog(
    initialLabel: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var value by remember(initialLabel) { mutableStateOf(initialLabel.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Label",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppOutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = "Add a label",
                    maxLines = 1
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PredefinedLabels.forEach { predefined ->
                        val selected = value.trim() == predefined
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { value = if (selected) "" else predefined }
                        ) {
                            Text(
                                text = predefined,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim().takeIf { it.isNotBlank() }) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    value = ""
                    onSave(null)
                }
            ) {
                Text("Clear")
            }
        }
    )
}

@Composable
internal fun ColorTokenMenu(
    icon: ImageVector,
    selected: NestedColorToken,
    filled: Boolean = false,
    onSelected: (NestedColorToken) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isColorActive = selected != NestedColorToken.Default
    val subtleInactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (filled) "Background color" else "Text color",
                tint = if (isColorActive) nestedColor(selected) else subtleInactiveColor,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ColorTokenSwatches(selected = selected) {
                expanded = false
                onSelected(it)
            }
        }
    }
}

@Composable
internal fun ColorTokenSwatches(
    selected: NestedColorToken,
    modifier: Modifier = Modifier,
    onSelected: (NestedColorToken) -> Unit
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        maxItemsInEachRow = 7,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NestedColorToken.entries.forEach { token ->
            val color = nestedColor(token)
            val isSelected = token == selected
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (token == NestedColorToken.Default) MaterialTheme.colorScheme.surfaceVariant else color
                    )
                    .clickable { onSelected(token) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = token.name,
                        tint = if (token == NestedColorToken.Default) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else if (token == NestedColorToken.Default) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Default",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun BorderMenu(
    shape: NestedBorderShape,
    style: NestedBorderStyle,
    color: NestedColorToken,
    onSelected: (NestedBorderShape, NestedBorderStyle, NestedColorToken) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val active = style != NestedBorderStyle.None
    val subtleInactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BorderStyle,
                contentDescription = "Border",
                tint = if (active) {
                    if (color != NestedColorToken.Default) nestedColor(color)
                    else MaterialTheme.colorScheme.primary
                } else {
                    subtleInactiveColor
                },
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Border style",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NestedBorderStyle.entries.forEach { option ->
                        SelectableChip(
                            label = option.name,
                            selected = option == style
                        ) {
                            expanded = false
                            onSelected(shape, option, color)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Shape",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NestedBorderShape.entries.forEach { option ->
                        SelectableChip(
                            label = option.displayName(),
                            selected = option == shape
                        ) {
                            expanded = false
                            onSelected(option, style, color)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Border color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorTokenSwatches(selected = color) {
                    expanded = false
                    onSelected(shape, style, it)
                }
            }
        }
    }
}

@Composable
internal fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun NestedItemDetailsDialog(
    node: NestedItemNode,
    summary: NestedMetricSummary,
    onDismiss: () -> Unit,
    onSave: (Int, MetricRollupPolicy, Boolean, Int?, List<MetricItem>) -> Unit
) {
    val item = node.item
    val isLeaf = !node.hasChildren
    var actualMinutes by remember(item.id) { mutableStateOf(if (item.actualMinutes > 0) item.actualMinutes.toString() else "") }
    var policy by remember(item.id) { mutableStateOf(item.metricRollupPolicy) }
    var showTrackedMinutes by remember(item.id) { mutableStateOf(item.showTrackedMinutes) }
    var progress by remember(item.id) { mutableStateOf(item.progressPercent) }
    var metrics by remember(item.id) { mutableStateOf(item.manualMetrics) }
    var policyExpanded by remember { mutableStateOf(false) }

    val directChildCount = node.children.size
    val totalChildCount = remember(node) { countTotalDescendants(node) }
    val doneTaskCount = summary.doneTaskCount
    val totalTaskCount = summary.totalTaskCount
    val totalTrackedMinutes = summary.trackedMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Item details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = item.text.ifBlank { "Untitled item" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.completedAtMillis?.let { completedAt ->
                    Text(
                        text = "Completed · ${completedAt.toDateTimeLabel()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Overview stats cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailMetricStatCard(
                        title = "Children",
                        value = if (directChildCount == totalChildCount) "$totalChildCount" else "$directChildCount ($totalChildCount)",
                        subtitle = if (totalChildCount > 0) {
                            if (totalTaskCount > 0) "$doneTaskCount/$totalTaskCount done" else "No checkboxes"
                        } else {
                            "Leaf item"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DetailMetricStatCard(
                        title = "Actual time",
                        value = "${item.actualMinutes}m",
                        subtitle = if (!isLeaf && totalTrackedMinutes != item.actualMinutes) "Total: ${totalTrackedMinutes}m" else "Direct time",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Section 2: Time tracking and rollup settings
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TIME & ROLLUP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactFlatTextField(
                            value = actualMinutes,
                            onValueChange = { value -> if (value.all { it.isDigit() }) actualMinutes = value },
                            placeholder = "0",
                            suffix = "min",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        Box(modifier = Modifier.weight(1.3f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .clickable { policyExpanded = true }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = policyLabel(policy),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = policyExpanded,
                                onDismissRequest = { policyExpanded = false }
                            ) {
                                MetricRollupPolicy.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(policyLabel(option), style = MaterialTheme.typography.bodyMedium) },
                                        onClick = { policy = option; policyExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    if (!isLeaf) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show tracked minutes on row",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "$totalTrackedMinutes min total rollup",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showTrackedMinutes,
                                onCheckedChange = { showTrackedMinutes = it },
                                modifier = Modifier.scale(0.75f)
                            )
                        }
                    }
                }

                // Section: Progress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val showing = progress != null
                        Icon(
                            imageVector = if (showing) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = if (showing) "Hide progress" else "Show progress",
                            tint = if (showing) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                            },
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    progress = if (showing) null else progress ?: 0
                                }
                        )
                        if (showing) {
                            Slider(
                                value = (progress ?: 0).toFloat(),
                                onValueChange = { value ->
                                    progress = value.roundToInt().coerceIn(0, 100)
                                },
                                valueRange = 0f..100f,
                                steps = 99,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    activeTickColor = Color.Transparent,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    inactiveTickColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f).height(22.dp)
                            )
                            Text(
                                text = "${progress}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(44.dp)
                            )
                        } else {
                            Text(
                                text = "Show progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { progress = 0 }
                            )
                        }
                    }
                }

                // Section 3: Custom/Manual Metrics
                MetricsSection(
                    metrics = metrics,
                    enabled = true,
                    onMetricsChange = { metrics = it },
                    emptyHint = "No custom metrics added."
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        actualMinutes.toIntOrNull() ?: 0,
                        policy,
                        showTrackedMinutes,
                        progress?.coerceIn(0, 100),
                        metrics.filter { it.isValidForSave() }
                    )
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
internal fun DetailMetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun countTotalDescendants(node: NestedItemNode): Int {
    var count = node.children.size
    for (child in node.children) {
        count += countTotalDescendants(child)
    }
    return count
}

internal fun policyLabel(policy: MetricRollupPolicy): String = when (policy) {
    MetricRollupPolicy.IncludeChildren -> "Include children"
    MetricRollupPolicy.OwnOnly -> "Own item only"
    MetricRollupPolicy.ExcludeFromParent -> "Exclude from parent"
}
