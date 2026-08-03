package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.UndoBehavior
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlin.time.Duration.Companion.milliseconds

private enum class ComposerTab(val label: String) {
    Write("Write"),
    Preview("Preview"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalRichTextApi::class, FlowPreview::class)
@Composable
internal fun RichTextComposer(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    modifier: Modifier = Modifier,
    showPreview: Boolean = false,
    enabled: Boolean = true,
) {
    val state = rememberRichTextState(historyLimit = 0, coalesceWindowMs = 0L)
    state.config.listIndent = 15

    var isEditing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Automatically focus when switching to editing mode
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    // Track the last markdown to prevent infinite loops and redundant state.setMarkdown calls
    var lastMarkdown by remember { mutableStateOf<String?>(null) }

    // Sync external value to internal state
    LaunchedEffect(value) {
        if (value != lastMarkdown) {
            state.setMarkdown(value)
            lastMarkdown = value
        }
    }

    // Sync internal state to external onValueChange with debouncing
    LaunchedEffect(state) {
        snapshotFlow { state.annotatedString }
            .drop(1)
            .debounce(300.milliseconds)
            .collectLatest {
                val markdown = state.toMarkdown().replace(Regex("\n{2,}"), "\n")
                if (lastMarkdown != null && markdown != lastMarkdown) {
                    lastMarkdown = markdown
                    onValueChange(markdown)
                }
            }
    }

    var tab by remember { mutableStateOf(ComposerTab.Write) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (isEditing) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .background(if (isEditing) MaterialTheme.colorScheme.surface else Color.Transparent)
        ,
    ) {
        if (isEditing) {
            if (showPreview) {
                TabRow(selected = tab, onSelect = { tab = it })
            }

            val isWriteTab = tab == ComposerTab.Write || !showPreview
            if (isWriteTab) {
                ComposerToolbar(
                    state = state,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 130.dp)
                    .padding(0.dp),
            ) {
                if (isWriteTab) {
                    OutlinedRichTextEditor(
                        state = state,
                        readOnly = !enabled,
                        placeholder = if (placeholder != null) {
                            {
                                Text(
                                    text = placeholder,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else null,
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.SansSerif,
                        ),
                        colors = RichTextEditorDefaults.richTextEditorColors(
                            textColor = MaterialTheme.colorScheme.onSurface,
                            containerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .defaultMinSize(minHeight = 120.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, keyboardType = KeyboardType.Unspecified, imeAction = ImeAction.Unspecified, platformImeOptions = null, showKeyboardOnFocus = null,hintLocales = null),
                        undoBehavior = UndoBehavior.Disabled
                    )
                } else {
                    if (state.annotatedString.text.isBlank()) {
                        Text(
                            text = "Nothing to preview",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        RichText(
                            state = state,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                    }
                }
            }
        } else {
            // Read Mode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .clickable(enabled = enabled) { isEditing = true }
                    .padding(vertical = 8.dp)
            ) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder ?: "Add notes...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    RichText(
                        state = state,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TabRow(
    selected: ComposerTab,
    onSelect: (ComposerTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 8.dp, top = 8.dp, end = 8.dp),
    ) {
        ComposerTab.entries.forEach { entry ->
            Tab(
                label = entry.label,
                isSelected = entry == selected,
                onClick = { onSelect(entry) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Tab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.outlineVariant else Color.Transparent
    val background = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
            )
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

private val HeadingSpan = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
private val BoldSpan = SpanStyle(fontWeight = FontWeight.Bold)
private val ItalicSpan = SpanStyle(fontStyle = FontStyle.Italic)
private val StrikeSpan = SpanStyle(textDecoration = TextDecoration.LineThrough)

@Composable
private fun ComposerToolbar(
    state: RichTextState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier,
    ) {
        item {
            ToolbarButton(
                icon = Icons.Outlined.Title,
                isSelected = state.currentSpanStyle.fontSize == HeadingSpan.fontSize,
                contentDescription = "Heading",
                enabled = enabled,
                onClick = { state.toggleSpanStyle(HeadingSpan) },
            )
        }
        item {
            ToolbarButton(
                icon = Icons.Outlined.FormatBold,
                isSelected = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                contentDescription = "Bold",
                enabled = enabled,
                onClick = { state.toggleSpanStyle(BoldSpan) },
            )
        }
        item {
            ToolbarButton(
                icon = Icons.Outlined.FormatItalic,
                isSelected = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                contentDescription = "Italic",
                enabled = enabled,
                onClick = { state.toggleSpanStyle(ItalicSpan) },
            )
        }
        item {
            ToolbarButton(
                icon = Icons.Outlined.FormatStrikethrough,
                isSelected = state.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
                contentDescription = "Strikethrough",
                enabled = enabled,
                onClick = { state.toggleSpanStyle(StrikeSpan) },
            )
        }

        item { Divider() }

        item {
            ToolbarButton(
                icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                isSelected = state.isUnorderedList,
                contentDescription = "Bulleted list",
                enabled = enabled,
                onClick = { state.toggleUnorderedList() },
            )
        }
        item {
            ToolbarButton(
                icon = Icons.Outlined.FormatListNumbered,
                isSelected = state.isOrderedList,
                contentDescription = "Numbered list",
                enabled = enabled,
                onClick = { state.toggleOrderedList() },
            )
        }

        item {
            ToolbarButton(
                icon = Icons.AutoMirrored.Outlined.FormatIndentDecrease,
                isSelected = false,
                enabled = enabled && state.canDecreaseListLevel,
                contentDescription = "Outdent",
                onClick = { state.decreaseListLevel() },
            )
        }

        item {
            ToolbarButton(
                icon = Icons.AutoMirrored.Outlined.FormatIndentIncrease,
                isSelected = false,
                enabled = enabled && state.canIncreaseListLevel,
                contentDescription = "Indent",
                onClick = { state.increaseListLevel() },
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val contentAlpha = if (enabled) 1f else 0.38f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            // Workaround: prevent the rich editor from losing focus when a toolbar button
            // is clicked (Desktop quirk). Without this the cursor jumps and toggleSpanStyle
            // applies to the wrong place.
            .focusProperties { canFocus = false }
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = (if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                .copy(alpha = contentAlpha),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .height(20.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
