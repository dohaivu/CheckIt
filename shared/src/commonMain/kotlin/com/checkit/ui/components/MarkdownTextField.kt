package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.checkit.ui.tasks.views.ContentContainerAlpha
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class MarkdownTemplate(
    val name: String,
    val content: String,
    val description: String? = null
)

data class AppenderCommandItem(
    val name: String,
    val label: String,
    val description: String? = null,
    val action: (text: String, slashIndex: Int, cursorIndex: Int) -> JournalCursorEdit
)

internal data class SlashQuery(
    val slashIndex: Int,
    val query: String
)

internal fun detectSlashQuery(text: String, cursorIndex: Int): SlashQuery? {
    if (cursorIndex <= 0 || cursorIndex > text.length) return null
    val textBeforeCursor = text.substring(0, cursorIndex)
    val lastSlashIndex = textBeforeCursor.lastIndexOf('/')
    if (lastSlashIndex == -1) return null

    // Ensure '/' is at start of line or preceded by whitespace
    val isPrecededBySpaceOrNewline = lastSlashIndex == 0 || textBeforeCursor[lastSlashIndex - 1].isWhitespace()
    if (!isPrecededBySpaceOrNewline) return null

    val query = textBeforeCursor.substring(lastSlashIndex + 1)
    // Avoid triggering if space or newline was typed after '/'
    if (query.contains(' ') || query.contains('\n')) return null

    return SlashQuery(slashIndex = lastSlashIndex, query = query)
}

internal fun applySlashReplacement(
    text: String,
    slashIndex: Int,
    cursorIndex: Int,
    replacement: String
): JournalCursorEdit {
    val prefix = text.substring(0, slashIndex)
    val suffix = text.substring(cursorIndex.coerceAtMost(text.length))
    val newText = prefix + replacement + suffix
    val newCursor = slashIndex + replacement.length
    return JournalCursorEdit(
        text = newText,
        selectionStart = newCursor,
        selectionEnd = newCursor
    )
}

internal fun currentFormattedDate(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${now.year}-${now.month.number.toString().padStart(2, '0')}-${now.day.toString().padStart(2, '0')}"
}

internal fun currentFormattedTime(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
}

internal fun currentFormattedNow(): String {
    return "${currentFormattedDate()} ${currentFormattedTime()}"
}

val commonAppenderCommands =
    listOf(
        AppenderCommandItem(
            name = "3-points",
            label = "3 Points",
            action = { text, slashIndex, cursorIndex ->
                applySlashReplacement(text, slashIndex, cursorIndex, "1. \n2. \n3. \n")
            }
        )
    )

val markdownAppenderCommands = listOf(
    AppenderCommandItem(
        name = "heading",
        label = "Heading",
        description = "Insert heading ## ",
        action = { text, slashIndex, cursorIndex ->
            applySlashReplacement(text, slashIndex, cursorIndex, "## ")
        }
    ),
    AppenderCommandItem(
        name = "bullet",
        label = "Bullet List",
        description = "Insert bullet list - ",
        action = { text, slashIndex, cursorIndex ->
            applySlashReplacement(text, slashIndex, cursorIndex, "- ")
        }
    ),
    AppenderCommandItem(
        name = "number",
        label = "Numbered List",
        description = "Insert numbered list 1. ",
        action = { text, slashIndex, cursorIndex ->
            applySlashReplacement(text, slashIndex, cursorIndex, "1. ")
        }
    ),
    AppenderCommandItem(
        name = "quote",
        label = "Quote",
        description = "Insert blockquote > ",
        action = { text, slashIndex, cursorIndex ->
            applySlashReplacement(text, slashIndex, cursorIndex, "> ")
        }
    ),
    AppenderCommandItem(
        name = "todo",
        label = "Task Checklist",
        description = "Insert checkbox - [ ] ",
        action = { text, slashIndex, cursorIndex ->
            applySlashReplacement(text, slashIndex, cursorIndex, "- [ ] ")
        }
    )
)

internal fun defaultAppenderCommands(templates: List<MarkdownTemplate> = emptyList()): List<AppenderCommandItem> {
    val items = mutableListOf<AppenderCommandItem>()

    // Custom templates
    templates.forEach { template ->
        items.add(
            AppenderCommandItem(
                name = template.name.lowercase().replace(" ", "-"),
                label = template.name,
                description = template.description ?: "Custom template",
                action = { text, slashIndex, cursorIndex ->
                    applySlashReplacement(text, slashIndex, cursorIndex, template.content)
                }
            )
        )
    }

    // Dynamic Date & Time appenders
    items.add(
        AppenderCommandItem(
            name = "date",
            label = "Date",
            description = "Current date (${currentFormattedDate()})",
            action = { text, slashIndex, cursorIndex ->
                applySlashReplacement(text, slashIndex, cursorIndex, currentFormattedDate())
            }
        )
    )
    items.add(
        AppenderCommandItem(
            name = "time",
            label = "Time",
            description = "Current time (${currentFormattedTime()})",
            action = { text, slashIndex, cursorIndex ->
                applySlashReplacement(text, slashIndex, cursorIndex, currentFormattedTime())
            }
        )
    )
    items.add(
        AppenderCommandItem(
            name = "now",
            label = "Date & Time",
            description = "Current date and time (${currentFormattedNow()})",
            action = { text, slashIndex, cursorIndex ->
                applySlashReplacement(text, slashIndex, cursorIndex, currentFormattedNow())
            }
        )
    )

    return items
}

@Composable
fun AppenderPopup(
    items: List<AppenderCommandItem>,
    onSelect: (AppenderCommandItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .heightIn(max = 160.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 2.dp)
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "/${item.name}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )

                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MarkdownToolbar(
    onAction: (ToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        listOf(
            "B" to ToolbarAction.Bold,
            "I" to ToolbarAction.Italic,
            "==" to ToolbarAction.Highlight,
            "~~" to ToolbarAction.Strikethrough,
            "H" to ToolbarAction.Heading,
            "• " to ToolbarAction.Bullet,
            "1. " to ToolbarAction.Numbered,
            "Quote" to ToolbarAction.Quote,
            "/" to ToolbarAction.SlashCommand
        ).forEach { (label, action) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onAction(action) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun MarkdownTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    ),
    placeholder: String? = null,
    placeholderStyle: TextStyle = textStyle.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentContainerAlpha)
    ),
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    showToolbar: Boolean = true,
    templates: List<MarkdownTemplate> = emptyList(),
    customAppenderCommands: List<AppenderCommandItem> = emptyList(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
    textFieldModifier: Modifier = Modifier.fillMaxWidth()
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = value)) }
    var lastExternalText by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    if (value != lastExternalText) {
        lastExternalText = value
        textFieldValue = TextFieldValue(
            text = value,
            selection = TextRange(value.length)
        )
    }

    val availableSlashCommands = remember(templates, customAppenderCommands) {
        defaultAppenderCommands(templates) + customAppenderCommands + commonAppenderCommands + markdownAppenderCommands
    }

    val slashQuery = remember(textFieldValue.text, textFieldValue.selection.start, isFocused) {
        if (isFocused) detectSlashQuery(textFieldValue.text, textFieldValue.selection.start) else null
    }

    val matchingSlashItems = remember(slashQuery, availableSlashCommands) {
        if (slashQuery == null) emptyList()
        else {
            val q = slashQuery.query.lowercase()
            availableSlashCommands.filter {
                it.name.lowercase().contains(q) || it.label.lowercase().contains(q)
            }
        }
    }

    val density = LocalDensity.current
    val lineIndex = remember(textFieldValue.text, slashQuery?.slashIndex) {
        if (slashQuery != null) {
            textFieldValue.text.substring(0, slashQuery.slashIndex.coerceAtMost(textFieldValue.text.length)).count { it == '\n' }
        } else 0
    }
    val yOffsetPx = with(density) { (20.dp * (lineIndex + 1)).roundToPx() }
    val xOffsetPx = with(density) { 10.dp.roundToPx() }

    Box(modifier = modifier) {
        Column {
            AppOutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    if (newValue.text != lastExternalText) {
                        lastExternalText = newValue.text
                        onValueChange(newValue.text)
                    }
                },
                textStyle = textStyle,
                placeholder = placeholder,
                placeholderStyle = placeholderStyle,
                minLines = minLines,
                maxLines = maxLines,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                enabled = enabled,
                readOnly = readOnly,
                interactionSource = interactionSource,
                visualTransformation = remember { MarkdownVisualTransformation() },
                contentPadding = contentPadding,
                modifier = textFieldModifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
            )

            if (showToolbar && isFocused && enabled && !readOnly && slashQuery == null) {
                Spacer(Modifier.height(6.dp))
                MarkdownToolbar(
                    onAction = { action ->
                        val edit = applyToolbarAction(
                            text = textFieldValue.text,
                            selectionStart = textFieldValue.selection.start,
                            selectionEnd = textFieldValue.selection.end,
                            action = action
                        )
                        val newTextFieldValue = textFieldValue.copy(
                            text = edit.text,
                            selection = TextRange(edit.selectionStart, edit.selectionEnd)
                        )
                        textFieldValue = newTextFieldValue
                        lastExternalText = edit.text
                        onValueChange(edit.text)
                        focusRequester.requestFocus()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (slashQuery != null && matchingSlashItems.isNotEmpty() && enabled && !readOnly) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(x = xOffsetPx, y = yOffsetPx),
                properties = PopupProperties(focusable = false)
            ) {
                AppenderPopup(
                    items = matchingSlashItems,
                    onSelect = { item ->
                        val edit = item.action(
                            textFieldValue.text,
                            slashQuery.slashIndex,
                            textFieldValue.selection.start
                        )
                        val newTextFieldValue = textFieldValue.copy(
                            text = edit.text,
                            selection = TextRange(edit.selectionStart, edit.selectionEnd)
                        )
                        textFieldValue = newTextFieldValue
                        lastExternalText = edit.text
                        onValueChange(edit.text)
                        focusRequester.requestFocus()
                    },
                    modifier = Modifier.widthIn(max = 280.dp)
                )
            }
        }
    }
}

enum class ToolbarAction {
    Bold,
    Italic,
    Highlight,
    Strikethrough,
    Heading,
    Bullet,
    Numbered,
    Quote,
    SlashCommand
}

data class JournalCursorEdit(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int
)

/**
 * Applies a markdown toolbar action at the given cursor/selection.
 * Selection inputs are coerced into range and normalized.
 */
fun applyToolbarAction(
    text: String,
    selectionStart: Int,
    selectionEnd: Int,
    action: ToolbarAction
): JournalCursorEdit {
    val safeText = text
    val start = selectionStart.coerceIn(0, safeText.length)
    val end = selectionEnd.coerceIn(0, safeText.length)
    val selStart = minOf(start, end)
    val selEnd = maxOf(start, end)

    return when (action) {
        ToolbarAction.Bold -> wrapSelection(safeText, selStart, selEnd, "**", "**", "text")
        ToolbarAction.Italic -> wrapSelection(safeText, selStart, selEnd, "*", "*", "text")
        ToolbarAction.Highlight -> wrapSelection(safeText, selStart, selEnd, "==", "==", "text")
        ToolbarAction.Strikethrough -> wrapSelection(safeText, selStart, selEnd, "~~", "~~", "text")
        ToolbarAction.Heading -> toggleLinePrefix(safeText, selStart, selEnd, "## ")
        ToolbarAction.Bullet -> toggleLinePrefix(safeText, selStart, selEnd, "- ")
        ToolbarAction.Quote -> toggleLinePrefix(safeText, selStart, selEnd, "> ")
        ToolbarAction.Numbered -> toggleNumberedPrefix(safeText, selStart, selEnd)
        ToolbarAction.SlashCommand -> {
            val insert = "/"
            val newText = safeText.substring(0, selStart) + insert + safeText.substring(selStart)
            JournalCursorEdit(newText, selStart + 1, selStart + 1)
        }
    }
}

private fun wrapSelection(
    safeText: String,
    selStart: Int,
    selEnd: Int,
    prefix: String,
    suffix: String,
    defaultPlaceholder: String
): JournalCursorEdit {
    val pLen = prefix.length
    return if (selStart == selEnd) {
        val insert = "$prefix$defaultPlaceholder$suffix"
        val newText = safeText.substring(0, selStart) + insert + safeText.substring(selStart)
        JournalCursorEdit(newText, selStart + pLen, selStart + pLen + defaultPlaceholder.length)
    } else {
        val selected = safeText.substring(selStart, selEnd)
        val newText = safeText.substring(0, selStart) + prefix + selected + suffix + safeText.substring(selEnd)
        JournalCursorEdit(newText, selStart + pLen, selEnd + pLen)
    }
}

private fun lineStartOffsets(text: String): List<Int> {
    if (text.isEmpty()) return listOf(0)
    val starts = mutableListOf(0)
    text.forEachIndexed { index, c ->
        if (c == '\n' && index + 1 <= text.length) starts.add(index + 1)
    }
    return starts
}

private fun lineStartForOffset(text: String, offset: Int): Int {
    val clamped = offset.coerceIn(0, text.length)
    return text.lastIndexOf('\n', clamped - 1).let { if (it == -1) 0 else it + 1 }
}

private fun toggleLinePrefix(
    text: String,
    selStart: Int,
    selEnd: Int,
    prefix: String
): JournalCursorEdit {
    if (text.isEmpty()) {
        return JournalCursorEdit(prefix, prefix.length, prefix.length)
    }
    val fromLineStart = lineStartForOffset(text, selStart)
    val starts = lineStartOffsets(text).filter { it <= selEnd && (it >= fromLineStart) }
        .filter { lineStart ->
            val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
            lineStart <= selEnd && lineEnd >= selStart
        }
    if (starts.isEmpty()) return JournalCursorEdit(text, selStart, selEnd)

    val allHavePrefix = starts.all { lineStart ->
        text.regionMatches(lineStart, prefix, 0, prefix.length)
    }

    var deltaBeforeStart = 0
    var deltaBeforeEnd = 0
    val builder = StringBuilder()
    var cursor = 0
    val sorted = starts.sorted()
    for (lineStart in sorted) {
        builder.append(text, cursor, lineStart)
        if (allHavePrefix) {
            builder.append(text, lineStart + prefix.length, text.indexOf('\n', lineStart).let { if (it == -1) text.length else it })
            if (lineStart <= selStart) deltaBeforeStart -= prefix.length
            if (lineStart <= selEnd) deltaBeforeEnd -= prefix.length
            cursor = lineStart + prefix.length
            val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
            cursor = lineEnd
        } else {
            val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
            val alreadyHas = text.regionMatches(lineStart, prefix, 0, prefix.length)
            if (!alreadyHas) {
                builder.append(prefix)
                builder.append(text, lineStart, lineEnd)
                if (lineStart <= selStart) deltaBeforeStart += prefix.length
                if (lineStart <= selEnd) deltaBeforeEnd += prefix.length
            } else {
                builder.append(text, lineStart, lineEnd)
            }
            cursor = lineEnd
        }
    }
    builder.append(text, cursor, text.length)
    return JournalCursorEdit(
        builder.toString(),
        (selStart + deltaBeforeStart).coerceIn(0, builder.length),
        (selEnd + deltaBeforeEnd).coerceIn(0, builder.length)
    )
}

private val numberedPattern = Regex("^\\d+\\.\\s")

private fun toggleNumberedPrefix(text: String, selStart: Int, selEnd: Int): JournalCursorEdit {
    if (text.isEmpty()) {
        return JournalCursorEdit("1. ", 3, 3)
    }
    val starts = lineStartOffsets(text).filter { lineStart ->
        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        lineStart <= selEnd && lineEnd >= selStart
    }.sorted()
    if (starts.isEmpty()) return JournalCursorEdit(text, selStart, selEnd)

    fun existingMarkerLength(lineStart: Int): Int {
        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        return numberedPattern.find(line)?.value?.length ?: 0
    }

    val allNumbered = starts.all { existingMarkerLength(it) > 0 }

    var deltaBeforeStart = 0
    var deltaBeforeEnd = 0
    val builder = StringBuilder()
    var cursor = 0
    for (lineStart in starts) {
        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        builder.append(text, cursor, lineStart)
        if (allNumbered) {
            val len = existingMarkerLength(lineStart)
            builder.append(text, lineStart + len, lineEnd)
            if (lineStart <= selStart) deltaBeforeStart -= len
            if (lineStart <= selEnd) deltaBeforeEnd -= len
            cursor = lineEnd
        } else {
            val existingLen = existingMarkerLength(lineStart)
            if (existingLen == 0) {
                builder.append("1. ")
                builder.append(text, lineStart, lineEnd)
                if (lineStart <= selStart) deltaBeforeStart += 3
                if (lineStart <= selEnd) deltaBeforeEnd += 3
            } else {
                builder.append(text, lineStart, lineEnd)
            }
            cursor = lineEnd
        }
    }
    builder.append(text, cursor, text.length)
    return JournalCursorEdit(
        builder.toString(),
        (selStart + deltaBeforeStart).coerceIn(0, builder.length),
        (selEnd + deltaBeforeEnd).coerceIn(0, builder.length)
    )
}
