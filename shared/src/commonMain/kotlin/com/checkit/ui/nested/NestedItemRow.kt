package com.checkit.ui.nested

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedMetricSummary
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.TaskPriority
import com.checkit.ui.components.DateRangePill
import com.checkit.ui.components.MetricChip
import com.checkit.ui.components.TagPill
import com.checkit.ui.components.renderInlineMarkdown
import com.checkit.ui.isValidForSave
import com.checkit.ui.noRippleClickable
import com.checkit.ui.theme.parseHexColorOrNull

// Palette shared with macOS NestedRowView (nestedDepthSolids).
private val nestedDepthSolids: List<Color> = listOf(
    Color(0xFFD97F5F), // 0 terracotta
    Color(0xFF7EA38A), // 1 sage
    Color(0xFF7B9EBD), // 2 dusty blue
    Color(0xFFC09F5E), // 3 warm sand
)

private const val nestedGuideSubtleAlpha = 0.18f
private const val nestedGuideStrongAlpha = 0.92f

internal fun nestedDepthColor(depth: Int): Color =
    nestedDepthSolids[depth % nestedDepthSolids.size]

internal fun nestedGuideColor(level: Int, emphasized: Boolean = false, focusAlpha: Float = 1f): Color =
    nestedDepthColor(level).copy(
        alpha = (if (emphasized) nestedGuideStrongAlpha else nestedGuideSubtleAlpha) * focusAlpha
    )

internal fun nestedDotColor(depth: Int, emphasized: Boolean = false, focusAlpha: Float = 1f): Color =
    nestedDepthColor(depth).copy(
        alpha = (if (emphasized) 1f else 0.5f) * focusAlpha
    )

@Composable
internal fun priorityColor(priority: TaskPriority): Color =
    priority.priorityHex()?.parseHexColorOrNull()
        ?: MaterialTheme.colorScheme.onSurfaceVariant

@Composable
internal fun nestedTextStyle(style: NestedTextStyle) = when (style) {
    NestedTextStyle.Body -> MaterialTheme.typography.bodyMedium
    NestedTextStyle.Header -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
    NestedTextStyle.Subheader -> MaterialTheme.typography.titleMedium
}

@Composable
internal fun nestedTextColor(token: NestedColorToken) =
    if (token == NestedColorToken.Default) MaterialTheme.colorScheme.onSurface else nestedColor(token)

@Composable
internal fun nestedColor(token: NestedColorToken): Color {
    val hex = token.hex() ?: return MaterialTheme.colorScheme.onSurfaceVariant
    val rgb = hex.removePrefix("#").toInt(16)
    return Color(
        red = ((rgb shr 16) and 0xFF) / 255f,
        green = ((rgb shr 8) and 0xFF) / 255f,
        blue = (rgb and 0xFF) / 255f
    )
}

/**
 * Guide-emoji text style. Emoji fonts ship a tall line box; trimming keeps the glyph optically centred.
 */
internal val nestedGuideEmojiStyle = TextStyle(
    fontSize = 12.sp,
    lineHeight = 12.sp,
    textAlign = TextAlign.Center,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    ),
    platformStyle = guideEmojiPlatformStyle()
)

@Composable
internal fun NestedItemRow(
    node: NestedItemNode,
    depth: Int,
    isInCheckedBranch: Boolean,
    continuingLevels: Set<Int>,
    state: NestedEditorState.Active,
    viewModel: NestedListsViewModel,
    spineLevel: Int?,
    isSpineRoot: Boolean,
    isContext: Boolean = false,
    focusTier: NestedFocusTier = NestedFocusTier.None,
    isDragActive: Boolean = false,
    isDragged: Boolean = false
) {
    val item = node.item
    val isSelected = item.id in state.selection.selectedIds ||
            (!state.selection.isActive && state.selectedItemId == item.id)
    val isEditing = state.editingTextItemId == item.id
    val isEmphasized = isSelected || isEditing

    val focusAlpha by animateFloatAsState(
        targetValue = when (focusTier) {
            NestedFocusTier.None, NestedFocusTier.Focused -> 1f
            NestedFocusTier.Path -> 0.72f
            NestedFocusTier.Outside -> 0.45f
        },
        animationSpec = tween(durationMillis = 140),
        label = "nestedFocusAlpha"
    )
    val baseTextColor = when {
        isContext && !isEmphasized -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        isInCheckedBranch -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
        item.textColor != NestedColorToken.Default -> nestedColor(item.textColor)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val itemTextColor = baseTextColor.copy(alpha = minOf(baseTextColor.alpha, focusAlpha))

    val guideEmoji = item.guideDot
    val dotSize = when {
        node.hasChildren && guideEmoji != null -> 22.dp
        node.hasChildren || guideEmoji != null -> 14.dp
        else -> 7.dp
    }
    val dotTop = 16.dp - dotSize / 2
    val cardShape = remember(item.borderShape) { NestedCardShape(item.borderShape) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val borderStroke = remember(item.borderStyle, density.density) {
        item.borderStyle.toStroke(with(density) { 1.5.dp.toPx() }, density)
    }
    val baseBorderColor = when (item.borderColor) {
        NestedColorToken.Default -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        else -> nestedColor(item.borderColor)
    }
    val borderColor = baseBorderColor.copy(alpha = baseBorderColor.alpha * focusAlpha)

    // Reusable Path to avoid allocating a new Path instance on every draw frame
    val branchPath = remember { Path() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (!isDragged) {
                        val dotSizePx = dotSize.toPx()
                        val dotCenterY = dotTop.toPx() + dotSizePx / 2
                        val dotBottomY = dotTop.toPx() + dotSizePx
                        val strokeWidth = 1.dp.toPx()
                        val curveRadius = 6.dp.toPx()
                        val guideX: (Int) -> Float = { level -> level * 16.dp.toPx() + 8.dp.toPx() }

                        continuingLevels.forEach { level ->
                            val x = guideX(level)
                            drawLine(
                                color = nestedGuideColor(level, emphasized = level == spineLevel, focusAlpha = focusAlpha),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }

                        if (depth > 0) {
                            val x = guideX(depth - 1)
                            val xDot = guideX(depth)
                            val lineEnd = xDot - (dotSizePx / 2)
                            branchPath.reset()
                            if (depth - 1 in continuingLevels) {
                                branchPath.moveTo(x, dotCenterY - curveRadius)
                            } else {
                                branchPath.moveTo(x, 0f)
                                branchPath.lineTo(x, dotCenterY - curveRadius)
                            }
                            branchPath.quadraticTo(x, dotCenterY, x + curveRadius, dotCenterY)
                            branchPath.lineTo(lineEnd, dotCenterY)
                            drawPath(
                                path = branchPath,
                                color = nestedGuideColor(
                                    depth - 1,
                                    emphasized = depth - 1 == spineLevel,
                                    focusAlpha = focusAlpha
                                ),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        if (node.hasChildren && !item.collapsed) {
                            val x = guideX(depth)
                            drawLine(
                                color = nestedGuideColor(depth, emphasized = isEmphasized || isSpineRoot, focusAlpha = focusAlpha),
                                start = Offset(x, dotBottomY),
                                end = Offset(x, size.height),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 16).dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(36.dp)
                        .noRippleClickable(
                            enabled = node.hasChildren,
                            onClick = { viewModel.toggleCollapsed(item.id) },
                            onDoubleClick = { viewModel.zoomToItem(item.id) }
                        ),
                    contentAlignment = Alignment.TopStart
                ) {
                    val dotColor = nestedDotColor(
                        depth,
                        emphasized = isEmphasized || isSpineRoot,
                        focusAlpha = focusAlpha
                    )
                    val hasChildren = node.hasChildren
                    val ringStroke = when {
                        guideEmoji != null && (isEmphasized || isSpineRoot) -> 1.25.dp
                        guideEmoji != null -> 1.dp
                        isEmphasized || isSpineRoot -> 2.dp
                        else -> 1.5.dp
                    }
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(36.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = dotTop)
                                .requiredSize(dotSize)
                                .then(
                                    when {
                                        hasChildren -> Modifier
                                            .then(
                                                if (guideEmoji != null && item.collapsed) {
                                                    Modifier.background(
                                                        dotColor.copy(alpha = 0.22f),
                                                        CircleShape
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .border(ringStroke, dotColor, CircleShape)
                                        guideEmoji != null -> Modifier
                                        else -> Modifier.background(dotColor, CircleShape)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                guideEmoji != null -> Text(
                                    text = guideEmoji,
                                    style = nestedGuideEmojiStyle,
                                    modifier = Modifier.alpha(focusAlpha)
                                )
                                item.collapsed && hasChildren -> Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-8).dp)
                        .clip(cardShape)
                        .background(
                            when {
                                isEditing -> MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.5f
                                )
                                isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(
                                    alpha = 0.5f
                                )
                                item.backgroundColor != NestedColorToken.Default -> nestedColor(
                                    item.backgroundColor
                                ).copy(alpha = 0.18f * focusAlpha)
                                else -> Color.Transparent
                            }
                        )
                        .nestedCardBorder(cardShape, borderStroke, borderColor)
                        .combinedClickable(
                            onClick = {
                                if (state.selection.isActive) {
                                    viewModel.toggleSelect(item.id)
                                } else {
                                    viewModel.selectItem(item.id)
                                    if (state.overlay is NestedEditorOverlay.AddingItem) viewModel.cancelAddItem()
                                }
                            },
                            onDoubleClick = {
                                if (!state.selection.isActive) viewModel.startEditText(item.id)
                            }
                        )
                        .padding(
                            horizontal = 2.dp + item.borderShape.horizontalContentPadding(),
                            vertical = 6.dp + item.borderShape.verticalContentPadding()
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.checkboxEnabled) {
                        Checkbox(
                            checked = item.checked,
                            onCheckedChange = { viewModel.toggleChecked(item.id) },
                            modifier = Modifier.size(28.dp).scale(0.7f).align(Alignment.Top),
                            colors = CheckboxDefaults.colors(uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                        )
                    } else {
                        Spacer(Modifier.width(4.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        if (isEditing) {
                            var text by remember(item.id) { mutableStateOf(item.text) }
                            val focusRequester = remember(item.id) { FocusRequester() }
                            LaunchedEffect(item.id) {
                                text = item.text
                                focusRequester.requestFocus()
                            }
                            DisposableEffect(item.id) {
                                onDispose {
                                    if (text != item.text) {
                                        viewModel.saveItemText(item.id, text)
                                    }
                                }
                            }
                            BasicTextField(
                                value = text,
                                onValueChange = { text = it },
                                singleLine = false,
                                textStyle = nestedTextStyle(item.textStyle).copy(
                                    color = nestedTextColor(item.textColor).copy(
                                        alpha = minOf(1f, focusAlpha)
                                    )
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                                modifier = Modifier.fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused && isEditing && text != item.text) {
                                            viewModel.saveItemText(item.id, text)
                                        }
                                    }
                            )
                        } else {
                            Text(
                                text = remember(item.text) {
                                    renderInlineMarkdown(item.text)
                                },
                                style = nestedTextStyle(item.textStyle),
                                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                color = itemTextColor,
                                fontWeight = if (depth == 0 && !node.hasChildren) FontWeight.Bold else null
                            )
                        }
                        Box(
                            modifier = if (focusAlpha < 1f) {
                                Modifier.alpha(focusAlpha)
                            } else {
                                Modifier
                            }
                        ) {
                            NestedItemMetadataPreview(
                                item = item,
                                summary = state.tree.metricSummaryById[item.id]
                                    ?: NestedMetricSummary(),
                                isLeaf = !node.hasChildren
                            )
                        }
                    }

                    if (item.priority != TaskPriority.None) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = priorityColor(item.priority),
                            modifier = Modifier.size(18.dp).alpha(focusAlpha)
                        )
                    }

                    if (!item.label.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .widthIn(max = 140.dp)
                                .heightIn(max = 22.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(
                                        alpha = 0.85f * focusAlpha
                                    )
                                )
                        ) {
                            Text(
                                text = item.label,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (state.selection.isActive) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { viewModel.toggleSelect(item.id) },
                            modifier = Modifier.size(28.dp).scale(0.7f).align(Alignment.Top)
                        )
                    }
                }
            }

            if (
                isSelected &&
                !state.selection.isActive &&
                !isEditing &&
                !isDragActive &&
                state.overlay !is NestedEditorOverlay.AddingItem
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.50f))
                        .clickable { viewModel.startAddSibling(item.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add sibling below",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun NestedItemMetadataPreview(
    item: NestedListItem,
    summary: NestedMetricSummary,
    isLeaf: Boolean
) {
    val hasNote = !item.note.isNullOrBlank()
    val hasTags = item.tags.isNotEmpty()
    val hasDateRange = item.startDate != null || item.endDate != null
    val progress = item.progressPercent
    val showTracked = isLeaf || item.showTrackedMinutes
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (progress != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { (progress.coerceIn(0, 100)) / 100f },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${progress.coerceIn(0, 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (summary.totalTaskCount > 0) {
                SummaryChip(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append("${summary.doneTaskCount}/${summary.totalTaskCount}")
                    }
                    append(" done")
                })
            }
            if (showTracked && summary.trackedMinutes > 0) {
                SummaryChip(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append("${summary.trackedMinutes}")
                    }
                    append(" min")
                })
            }
            item.manualMetrics.filter { it.enabled }.forEach { metric ->
                if (metric.isCompleted || metric.isValidForSave()) {
                    MetricChip(metric = metric)
                }
            }
        }
        if (!hasNote && !hasTags && !hasDateRange && progress == null) return@Column

        if (hasNote) {
            Text(
                text = remember(item.note) {
                    renderInlineMarkdown(item.note.orEmpty())
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hasTags || hasDateRange) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasDateRange) {
                    DateRangePill(
                        startDate = item.startDate,
                        endDate = item.endDate
                    )
                }
                item.tags.forEach { tag -> TagPill(tag, isCompact = true) }
            }
        }
    }
}

@Composable
internal fun SummaryChip(content: AnnotatedString) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
internal fun DropIndicatorLine(depth: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16 + 8).dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
internal fun NewItemRow(
    depth: Int,
    text: String,
    onTextChange: (String) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    continuingLevels: Set<Int>
) {
    val branchPath = remember { Path() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val dotSizePx = 7.dp.toPx()
                val dotTop = 12.dp.toPx()
                val dotCenterY = dotTop + dotSizePx / 2
                val strokeWidth = 1.dp.toPx()
                val curveRadius = 6.dp.toPx()
                val guideX: (Int) -> Float = { level -> level * 16.dp.toPx() + 8.dp.toPx() }

                continuingLevels.forEach { level ->
                    val x = guideX(level)
                    drawLine(
                        color = nestedGuideColor(level),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                if (depth > 0) {
                    val x = guideX(depth - 1)
                    val xDot = guideX(depth)
                    val lineEnd = xDot - (dotSizePx / 2)
                    branchPath.reset()
                    if (depth - 1 in continuingLevels) {
                        branchPath.moveTo(x, dotCenterY - curveRadius)
                    } else {
                        branchPath.moveTo(x, 0f)
                        branchPath.lineTo(x, dotCenterY - curveRadius)
                    }
                    branchPath.quadraticTo(x, dotCenterY, x + curveRadius, dotCenterY)
                    branchPath.lineTo(lineEnd, dotCenterY)
                    drawPath(
                        path = branchPath,
                        color = nestedGuideColor(depth - 1),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp),
            verticalAlignment = Alignment.Top
        ) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            Spacer(Modifier.width(24.dp).height(36.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .offset(x = (-8).dp)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    singleLine = false,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            IconButton(
                onClick = onCommit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = "Save")
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
        }
    }
}
