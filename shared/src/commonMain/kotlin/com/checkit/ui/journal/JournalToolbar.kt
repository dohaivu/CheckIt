package com.checkit.ui.journal

enum class JournalToolbarAction {
    Bold,
    Italic,
    Strikethrough,
    Heading,
    Bullet,
    Numbered,
    Quote
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
fun applyJournalToolbarAction(
    text: String,
    selectionStart: Int,
    selectionEnd: Int,
    action: JournalToolbarAction
): JournalCursorEdit {
    val safeText = text
    val start = selectionStart.coerceIn(0, safeText.length)
    val end = selectionEnd.coerceIn(0, safeText.length)
    val selStart = minOf(start, end)
    val selEnd = maxOf(start, end)

    return when (action) {
        JournalToolbarAction.Bold -> wrapSelection(safeText, selStart, selEnd, "**", "**", "text")
        JournalToolbarAction.Italic -> wrapSelection(safeText, selStart, selEnd, "*", "*", "text")
        JournalToolbarAction.Strikethrough -> wrapSelection(safeText, selStart, selEnd, "~~", "~~", "text")
        JournalToolbarAction.Heading -> toggleLinePrefix(safeText, selStart, selEnd, "## ")
        JournalToolbarAction.Bullet -> toggleLinePrefix(safeText, selStart, selEnd, "- ")
        JournalToolbarAction.Quote -> toggleLinePrefix(safeText, selStart, selEnd, "> ")
        JournalToolbarAction.Numbered -> toggleNumberedPrefix(safeText, selStart, selEnd)
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
    // Include lines intersecting [selStart, selEnd). For collapsed selection, just current line.
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
    // Apply edits line by line from start to end.
    val sorted = starts.sorted()
    for (lineStart in sorted) {
        builder.append(text, cursor, lineStart)
        if (allHavePrefix) {
            // Remove prefix
            builder.append(text, lineStart + prefix.length, text.indexOf('\n', lineStart).let { if (it == -1) text.length else it })
            if (lineStart <= selStart) deltaBeforeStart -= prefix.length
            if (lineStart <= selEnd) deltaBeforeEnd -= prefix.length
            cursor = lineStart + prefix.length
            // Append rest of line (up to newline) already handled; move cursor to line end
            val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
            // We appended up to lineEnd above via range; now include newline later via next append
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
            if (existingMarkerLength(lineStart) == 0) {
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
