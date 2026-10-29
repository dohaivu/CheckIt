package com.checkit.ui.nested

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.nested_add_child
import checkit.shared.generated.resources.nested_add_sibling
import checkit.shared.generated.resources.nested_batch_delete
import checkit.shared.generated.resources.nested_collapse_all
import checkit.shared.generated.resources.nested_expand_all
import checkit.shared.generated.resources.nested_focus_selection
import checkit.shared.generated.resources.nested_indent
import checkit.shared.generated.resources.nested_move_down
import checkit.shared.generated.resources.nested_move_up
import checkit.shared.generated.resources.nested_outdent
import checkit.shared.generated.resources.nested_root
import checkit.shared.generated.resources.nested_zoom_in
import checkit.shared.generated.resources.nested_zoom_out
import com.checkit.domain.FocusPeriod
import com.checkit.domain.NestedBorderShape
import com.checkit.domain.NestedBorderStyle
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.TagItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.moveDestinations
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DatePicker
import com.checkit.ui.components.EmojiPicker
import com.checkit.ui.components.FocusPeriodHeader
import com.checkit.ui.components.TagOptionMenu
import com.checkit.ui.orderedRangeForEnd
import com.checkit.ui.orderedRangeForStart
import com.checkit.ui.tasks.views.ViewOptionChip
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

// ---------------- breadcrumbs ----------------

internal data class Breadcrumb(
    val label: String,
    val depth: Int,
    val isRoot: Boolean,
    val itemId: String? = null
)

internal fun buildBreadcrumbs(state: NestedEditorState.Active, roots: List<NestedItemNode>): List<Breadcrumb> {
    val result = mutableListOf<Breadcrumb>()
    result.add(Breadcrumb(label = "", depth = 0, isRoot = true))
    val focusedId = state.zoomPath.lastOrNull() ?: return result
    val chain = findAncestorChain(roots, focusedId) ?: return result
    chain.forEach { node ->
        result.add(
            Breadcrumb(
                label = node.item.text,
                depth = result.size,
                isRoot = false,
                itemId = node.item.id
            )
        )
    }
    return result
}

/** Returns the node chain from a root down to [id], or null if not found. */
internal fun findAncestorChain(nodes: List<NestedItemNode>, id: String): List<NestedItemNode>? {
    for (node in nodes) {
        if (node.item.id == id) return listOf(node)
        findAncestorChain(node.children, id)?.let { return listOf(node) + it }
    }
    return null
}

@Composable
internal fun BreadcrumbBar(
    breadcrumbs: List<Breadcrumb>,
    onCrumbClick: (String) -> Unit,
    onRootClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.nested_root),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                .clickable { onRootClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
        breadcrumbs.drop(1).forEach { crumb ->
            val isCurrent = crumb.depth == breadcrumbs.lastOrNull()?.depth
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.surfaceVariant
                        else Color.Transparent
                    )
                    .clickable(enabled = !isCurrent) { crumb.itemId?.let(onCrumbClick) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = crumb.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------- formatting bar ----------------

@Composable
internal fun NestedFormattingBar(
    item: NestedListItem,
    hasChildren: Boolean,
    onFormattingChange: (NestedTextStyle, NestedColorToken, NestedColorToken) -> Unit,
    onBorderChange: (NestedBorderShape, NestedBorderStyle, NestedColorToken) -> Unit,
    onLabelChange: (String?) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onGuideDotChange: (String?) -> Unit,
    onGuideDotColorChange: (NestedColorToken?) -> Unit,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit,
    availableTags: List<TagItem>,
    onTagsChange: (List<String>) -> Unit,
    onToggleNote: () -> Unit,
    onToggleCheckbox: () -> Unit,
    onEnableCheckboxForChildren: () -> Unit,
    onSetChecked: (Boolean) -> Unit,
    onManageDetails: () -> Unit
) {
    var priorityExpanded by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }
    var guideDotExpanded by remember { mutableStateOf(false) }
    var guideDotColorExpanded by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    val subtleInactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    Surface(
        modifier = Modifier
            .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
            .navigationBarsPadding(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
        ) {
            // Group 1: Typography & Style
            val hasCustomStyle = item.textStyle != NestedTextStyle.Body || item.checkboxEnabled
            Box {
                IconButton(
                    onClick = { styleExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = "Text style",
                        tint = if (hasCustomStyle) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = styleExpanded, onDismissRequest = { styleExpanded = false }) {
                    NestedTextStyle.entries.forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style.name) },
                            modifier = if (style == item.textStyle) {
                                Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                            } else {
                                Modifier
                            },
                            onClick = {
                                styleExpanded = false
                                onFormattingChange(style, item.textColor, item.backgroundColor)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Checkbox") },
                        modifier = if (item.checkboxEnabled) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            Modifier
                        },
                        onClick = {
                            styleExpanded = false
                            onToggleCheckbox()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add checkbox for children") },
                        enabled = hasChildren,
                        onClick = {
                            styleExpanded = false
                            onEnableCheckboxForChildren()
                        }
                    )
                }
            }

            ColorTokenMenu(
                icon = Icons.Default.FormatColorText,
                selected = item.textColor
            ) { token -> onFormattingChange(item.textStyle, token, item.backgroundColor) }

            ColorTokenMenu(
                icon = Icons.Default.FormatColorFill,
                selected = item.backgroundColor,
                filled = true
            ) { token -> onFormattingChange(item.textStyle, item.textColor, token) }

            BorderMenu(
                shape = item.borderShape,
                style = item.borderStyle,
                color = item.borderColor
            ) { shape, style, color -> onBorderChange(shape, style, color) }

            // Dot color also owns the connector into this row and its descendant spine.
            Box {
                IconButton(
                    onClick = { guideDotColorExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.InvertColors,
                        contentDescription = "Dot and guide color",
                        tint = item.guideDotColor?.let { nestedColor(it) } ?: subtleInactiveColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = guideDotColorExpanded,
                    onDismissRequest = { guideDotColorExpanded = false }
                ) {
                    ColorTokenSwatches(selected = item.guideDotColor ?: NestedColorToken.Default) { token ->
                        guideDotColorExpanded = false
                        onGuideDotColorChange(token.takeIf { it != NestedColorToken.Default })
                    }
                }
            }

            // Guide dot: an emoji replaces the row's dot (parents keep the ring).
            Box {
                IconButton(
                    onClick = { guideDotExpanded = true },
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { contentDescription = "Guide dot" }
                ) {
                    val emoji = item.guideDot
                    if (emoji != null) {
                        Text(text = emoji, fontSize = 15.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEmotions,
                            contentDescription = "Guide dot",
                            tint = subtleInactiveColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = guideDotExpanded,
                    onDismissRequest = { guideDotExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Default dot") },
                        trailingIcon = if (item.guideDot == null) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                        onClick = {
                            guideDotExpanded = false
                            onGuideDotChange(null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.guideDot == null) "Choose emoji…" else "Change emoji…") },
                        onClick = {
                            guideDotExpanded = false
                            showEmojiPicker = true
                        }
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(16.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Group 2: Metadata (Priority, Date Range, Tags)
            val hasPriority = item.priority != TaskPriority.None
            Box {
                IconButton(
                    onClick = { priorityExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Priority",
                        tint = if (hasPriority) priorityColor(item.priority) else subtleInactiveColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                    TaskPriority.entries.forEach { priority ->
                        DropdownMenuItem(
                            text = { Text(priority.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = priorityColor(priority),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                priorityExpanded = false
                                onPriorityChange(priority)
                            }
                        )
                    }
                }
            }

            val hasLabel = !item.label.isNullOrBlank()
            IconButton(
                onClick = { showLabelDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Label",
                    tint = if (hasLabel) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            val hasDateRange = item.startDate != null || item.endDate != null
            IconButton(
                onClick = { showDateDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Start and end dates",
                    tint = if (hasDateRange) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            TagOptionMenu(
                availableTags = availableTags,
                selectedTagIds = item.tags.map { it.id }.toSet(),
                onTagToggle = { tagId ->
                    val selected = item.tags.map { it.id }.toMutableSet()
                    if (!selected.add(tagId)) selected.remove(tagId)
                    onTagsChange(selected.toList())
                },
                icon = { hasSelected, onClick ->
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(modifier = Modifier.size(18.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = "Tags",
                                tint = if (hasSelected) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            if (hasSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            )

            VerticalDivider(
                modifier = Modifier
                    .height(16.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Group 3: Quick Actions (Note, Check off, Details)
            val hasNote = !item.note.isNullOrEmpty()
            IconButton(
                onClick = onToggleNote,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Edit note",
                    tint = if (hasNote) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onSetChecked(!item.checked) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = if (item.checked) "Uncheck" else "Check off",
                    tint = if (item.checked) MaterialTheme.colorScheme.primary else subtleInactiveColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onManageDetails,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Details",
                    tint = subtleInactiveColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showEmojiPicker) {
        EmojiPicker(
            onDismiss = { showEmojiPicker = false },
            onEmojiSelect = { emoji -> onGuideDotChange(emoji.details.string) }
        )
    }

    if (showDateDialog) {
        var startDate by remember { mutableStateOf(item.startDate) }
        var endDate by remember { mutableStateOf(item.endDate) }
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Text(
                    text = "Dates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DatePicker(
                        date = startDate,
                        onDateChange = { picked ->
                            val (newStart, newEnd) = orderedRangeForStart(picked, endDate)
                            startDate = newStart
                            endDate = newEnd
                        },
                        modifier = Modifier.fillMaxWidth(),
                        emptyLabel = "Set start date"
                    )
                    DatePicker(
                        date = endDate,
                        onDateChange = { picked ->
                            val (newStart, newEnd) = orderedRangeForEnd(startDate, picked)
                            startDate = newStart
                            endDate = newEnd
                        },
                        modifier = Modifier.fillMaxWidth(),
                        emptyLabel = "Set end date"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateRangeChange(startDate, endDate)
                        showDateDialog = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDateRangeChange(null, null)
                        showDateDialog = false
                    }
                ) {
                    Text("Clear both")
                }
            }
        )
    }

    if (showLabelDialog) {
        NestedLabelDialog(
            initialLabel = item.label,
            onDismiss = { showLabelDialog = false },
            onSave = { label ->
                onLabelChange(label)
                showLabelDialog = false
            }
        )
    }
}

// ---------------- filter bar ----------------

@Composable
internal fun NestedListFilterBar(
    focus: FocusPeriod?,
    query: String,
    hideChecked: Boolean,
    isActive: Boolean,
    availableTags: List<TagItem>,
    selectedTagIds: Set<String>,
    onFocusChange: (FocusPeriod) -> Unit,
    onQueryChange: (String) -> Unit,
    onHideCheckedChange: (Boolean) -> Unit,
    onTagToggle: (String) -> Unit,
    onReset: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "FILTERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (isActive) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onReset() }
                )
            }
        }

        FocusPeriodHeader(
            focus = focus,
            onFocusSelected = onFocusChange,
            onPreviousPeriod = onPreviousPeriod,
            onNextPeriod = onNextPeriod,
            onCurrentPeriod = onCurrentPeriod
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            AppOutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search items...",
                clearEnabled = true,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            )

            TagOptionMenu(
                availableTags = availableTags,
                selectedTagIds = selectedTagIds,
                onTagToggle = onTagToggle
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onHideCheckedChange(!hideChecked) }
                    .background(
                        if (hideChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (hideChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = if (hideChecked) Icons.Default.Done else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (hideChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Hide checked",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hideChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

// ---------------- display type ----------------

@Composable
internal fun NestedDisplayTypeMenu(
    selected: NestedDisplayType,
    onSelect: (NestedDisplayType) -> Unit
) {
    var isPopupOpen by remember { mutableStateOf(false) }
    val visibleState = remember { MutableTransitionState(false) }

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(
            onClick = {
                isPopupOpen = true
                visibleState.targetState = true
            },
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = selected.icon(),
                contentDescription = "view options",
                tint = if (selected == NestedDisplayType.All) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(18.dp)
            )
        }

        if (isPopupOpen) {
            if (visibleState.isIdle && !visibleState.targetState) {
                isPopupOpen = false
            }

            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(x = 0, y = 130),
                onDismissRequest = { visibleState.targetState = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = scaleIn(
                        initialScale = 0.7f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(200)
                    ) + fadeIn(),
                    exit = scaleOut(
                        targetScale = 0.7f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(150)
                    ) + fadeOut()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .heightIn(min = 36.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                NestedDisplayType.entries.forEach { displayType ->
                                    ViewOptionChip(
                                        icon = displayType.icon(),
                                        label = displayType.label(),
                                        selected = selected == displayType,
                                        onClick = {
                                            onSelect(displayType)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun NestedDisplayType.icon(): ImageVector =
    when (this) {
        NestedDisplayType.All -> Icons.AutoMirrored.Filled.ViewList
        NestedDisplayType.Working -> Icons.Default.Work
    }

internal fun NestedDisplayType.label(): String =
    when (this) {
        NestedDisplayType.All -> "All"
        NestedDisplayType.Working -> "Working"
    }

// ---------------- toolbar ----------------

@Composable
internal fun EditorToolbar(
    state: NestedEditorState.Active,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddChild: () -> Unit,
    onAddSibling: () -> Unit,
    onDelete: () -> Unit,
    onAddRoot: () -> Unit,
    onManageDetails: () -> Unit,
    onAddToDailyPlan: () -> Unit,
    onCopyToTask: () -> Unit,
    onEnterSelection: () -> Unit,
    onSortChildren: () -> Unit,
    onMoveTo: () -> Unit,
    focusSelectionEnabled: Boolean,
    onToggleFocusSelection: () -> Unit
) {
    val hasSelection = state.selectedItemId != null
    val selectedNode = state.selectedItemId?.let { id -> state.tree.nodeById[id] }
    val canZoomIn = hasSelection && (selectedNode?.hasChildren == true)
    val canSortChildren = selectedNode?.hasChildren == true
    val canMoveTo = state.selectedItemId?.let {
        !moveDestinations(state.tree.flatItems, it).isEmpty
    } == true
    val canZoomOut = state.zoomPath.isNotEmpty()
    val canAddSibling = hasSelection && (selectedNode?.item?.parentId != null)
    val hasCollapsible = state.tree.nodeById.values.any { it.hasChildren }
    var showMore by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start)
        ) {
            // Group 1: Zoom & Fold
            ToolbarButton(Icons.Default.ZoomIn, stringResource(Res.string.nested_zoom_in), canZoomIn, onZoomIn)
            ToolbarButton(Icons.Default.ZoomOut, stringResource(Res.string.nested_zoom_out), canZoomOut, onZoomOut)
            ToolbarToggleButton(
                icon = Icons.Default.CenterFocusStrong,
                label = stringResource(Res.string.nested_focus_selection),
                checked = focusSelectionEnabled,
                enabled = hasSelection || focusSelectionEnabled,
                onCheckedChange = { onToggleFocusSelection() }
            )
            ToolbarButton(Icons.Default.UnfoldMore, stringResource(Res.string.nested_expand_all), hasCollapsible, onExpandAll)
            ToolbarButton(Icons.Default.UnfoldLess, stringResource(Res.string.nested_collapse_all), hasCollapsible, onCollapseAll)

            VerticalDivider(
                modifier = Modifier
                    .height(14.dp)
                    .padding(horizontal = 1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Group 2: Indent & Move
            ToolbarButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(Res.string.nested_outdent), hasSelection, onOutdent)
            ToolbarButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(Res.string.nested_indent), hasSelection, onIndent)
            ToolbarButton(Icons.Default.KeyboardArrowUp, stringResource(Res.string.nested_move_up), hasSelection, onMoveUp)
            ToolbarButton(Icons.Default.KeyboardArrowDown, stringResource(Res.string.nested_move_down), hasSelection, onMoveDown)

            VerticalDivider(
                modifier = Modifier
                    .height(14.dp)
                    .padding(horizontal = 1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Group 3: Add Items
            ToolbarButton(Icons.Default.Add, stringResource(Res.string.nested_add_child), hasSelection, onAddChild)
            ToolbarButton(Icons.AutoMirrored.Filled.NoteAdd, stringResource(Res.string.nested_add_sibling), canAddSibling, onAddSibling)

            VerticalDivider(
                modifier = Modifier
                    .height(14.dp)
                    .padding(horizontal = 1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Group 4: More
            Box {
                ToolbarButton(Icons.Default.MoreVert, "More actions", true) { showMore = true }
                DropdownMenu(
                    expanded = showMore,
                    onDismissRequest = { showMore = false }
                ) {
                    ToolbarMenuItem("Details", Icons.Outlined.Info, hasSelection) {
                        showMore = false; onManageDetails()
                    }
                    ToolbarMenuItem("Add root item", Icons.Default.AddBox, true) {
                        showMore = false; onAddRoot()
                    }
                    ToolbarMenuItem("Select items", Icons.Default.SelectAll, true) {
                        showMore = false; onEnterSelection()
                    }
                    ToolbarMenuItem("Add to daily plan", Icons.Default.Today, hasSelection) {
                        showMore = false; onAddToDailyPlan()
                    }
                    ToolbarMenuItem("Copy to Task", Icons.Default.ContentCopy, hasSelection) {
                        showMore = false; onCopyToTask()
                    }
                    ToolbarMenuItem(stringResource(Res.string.nested_batch_delete), Icons.Default.Delete, hasSelection) {
                        showMore = false; onDelete()
                    }
                    ToolbarMenuItem("Sort children", Icons.Default.SortByAlpha, canSortChildren) {
                        showMore = false; onSortChildren()
                    }
                    ToolbarMenuItem("Move to…", Icons.AutoMirrored.Filled.DriveFileMove, canMoveTo) {
                        showMore = false; onMoveTo()
                    }
                }
            }
        }
    }
}

@Composable
internal fun ToolbarMenuItem(
    label: String,
    icon: ImageVector? = null,
    enabled: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        },
        onClick = onClick,
        enabled = enabled
    )
}

@Composable
internal fun ToolbarButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(30.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun ToolbarToggleButton(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = Modifier
            .size(30.dp)
            .background(
                if (checked) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                } else {
                    Color.Transparent
                },
                CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                checked -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun SelectionToolbar(
    state: NestedEditorState.Active,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onExit: () -> Unit
) {
    val count = state.selection.selectedIds.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = "$count selected",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        ToolbarButton(Icons.Default.Done, "Mark done", count > 0, onToggleDone)
        ToolbarButton(Icons.Default.Delete, stringResource(Res.string.nested_batch_delete), count > 0, onDelete)
        Text(text = "Exit", modifier = Modifier.clickable { onExit() })
    }
}

@Composable
internal fun EmptyNestedList(onAddItem: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Start with a root item, then indent items to build your outline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onAddItem) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add item")
        }
    }
}
