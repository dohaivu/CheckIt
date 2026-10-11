package com.checkit.widget

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as UnitColorProvider
import com.checkit.domain.RichSpan
import com.checkit.domain.RichSpanKind
import com.checkit.domain.parseRichText
import com.checkit.ui.components.MarkdownHighlightColor
import com.checkit.ui.theme.parseHexColorOrNull

/** One drawable run of cleaned text plus the styles needed to paint it. */
data class WidgetTextSegment(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strikeThrough: Boolean = false,
    val highlight: Boolean = false,
    /** Hex for `=={cN}==` runs; null means the default highlight color. */
    val highlightHex: String? = null,
    val code: Boolean = false,
) {
    /** True when the run needs no overrides and can reuse the base style. */
    val isPlain: Boolean
        get() = !bold && !italic && !strikeThrough && !highlight && !code

    /** True when [other] would look identical, ignoring its text. */
    fun hasSameStyleAs(other: WidgetTextSegment): Boolean =
        bold == other.bold &&
            italic == other.italic &&
            strikeThrough == other.strikeThrough &&
            highlight == other.highlight &&
            highlightHex == other.highlightHex &&
            code == other.code
}

/** Header level classification for Markdown lines. */
enum class HeaderLevel {
    None,
    H1,
    H2,
    H3,
}

/** Parsed block structures representing Markdown line types for Glance widgets. */
sealed interface WidgetMarkdownLine {
    /** A line with text segments and line-level styling (header, list, quote, checkbox). */
    data class Content(
        val segments: List<WidgetTextSegment>,
        val headerLevel: HeaderLevel = HeaderLevel.None,
        val isQuote: Boolean = false,
        val isCheckedTask: Boolean = false,
        val isUncheckedTask: Boolean = false,
    ) : WidgetMarkdownLine

    /** Blank line separator between paragraphs. */
    object Blank : WidgetMarkdownLine

    /** Horizontal rule / divider (`---`, `***`, `___`). */
    object HorizontalRule : WidgetMarkdownLine
}

/**
 * Splits inline markdown into consecutive runs, mirroring `parseMarkdownInline`
 * for the in-app UI but returning segments instead of an AnnotatedString:
 * Glance `Text` only accepts a plain String, so each run has to be drawn by its own `Text`.
 *
 * Reuses the shared `parseRichText` parser, so markers, nesting rules and the
 * "unmatched markers stay literal" behavior match the app exactly.
 */
fun parseMarkdownWidgetSegments(markdown: String): List<WidgetTextSegment> {
    val parsed = parseRichText(markdown)
    val text = parsed.text
    if (text.isEmpty()) return emptyList()
    if (parsed.spans.isEmpty()) return listOf(WidgetTextSegment(text))

    val cuts: List<Int> = sortedSetOf(0, text.length)
        .apply {
            parsed.spans.forEach { span ->
                val start = span.start.coerceIn(0, text.length)
                val end = span.end.coerceIn(0, text.length)
                if (start < end) {
                    add(start)
                    add(end)
                }
            }
        }
        .toList()

    val segments = mutableListOf<WidgetTextSegment>()
    var pending: WidgetTextSegment? = null
    for (index in 0 until cuts.lastIndex) {
        val from = cuts[index]
        val to = cuts[index + 1]
        val run = styledRun(text, from, to, parsed.spans)

        val open = pending
        pending = if (open != null && open.hasSameStyleAs(run)) {
            open.copy(text = open.text + run.text)
        } else {
            open?.let(segments::add)
            run
        }
    }
    pending?.let(segments::add)
    return segments
}

/** The slice `[from, to)` with the flags of every span that covers it. */
private fun styledRun(
    text: String,
    from: Int,
    to: Int,
    spans: List<RichSpan>
): WidgetTextSegment {
    var bold = false
    var italic = false
    var strikeThrough = false
    var highlight = false
    var code = false

    spans.forEach { span ->
        if (span.start <= from && span.end >= to) {
            when (span.kind) {
                RichSpanKind.Bold -> bold = true
                RichSpanKind.Italic -> italic = true
                RichSpanKind.Strikethrough -> strikeThrough = true
                RichSpanKind.Highlight -> highlight = true
                RichSpanKind.Code -> code = true
            }
        }
    }

    return WidgetTextSegment(
        text = text.substring(from, to),
        bold = bold,
        italic = italic,
        strikeThrough = strikeThrough,
        highlight = highlight,
        highlightHex = spans.firstOrNull { it.kind == RichSpanKind.Highlight && it.start <= from && it.end >= to }?.colorHex,
        code = code
    )
}

/**
 * Parses full multiline Markdown into [WidgetMarkdownLine] blocks. Handles block structures
 * (headers, bullet lists, checkboxes, blockquotes, horizontal rules) and inline styles.
 */
fun parseMarkdownWidgetLines(markdown: String): List<WidgetMarkdownLine> {
    if (markdown.isEmpty()) return emptyList()

    val rawLines = markdown.split('\n')
    val result = mutableListOf<WidgetMarkdownLine>()

    for (rawLine in rawLines) {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) {
            result.add(WidgetMarkdownLine.Blank)
            continue
        }

        if (isHorizontalRule(trimmed)) {
            result.add(WidgetMarkdownLine.HorizontalRule)
            continue
        }

        var lineText = rawLine
        var isQuote = false
        var headerLevel = HeaderLevel.None
        var isCheckedTask = false
        var isUncheckedTask = false

        if (lineText.trimStart().startsWith(">")) {
            isQuote = true
            val contentStart = lineText.indexOf('>')
            lineText = "│  " + lineText.substring(contentStart + 1).let {
                if (it.startsWith(" ")) it.substring(1) else it
            }
        }

        val trimmedForPrefix = lineText.trimStart()
        when {
            trimmedForPrefix.startsWith("# ") -> {
                headerLevel = HeaderLevel.H1
                lineText = trimmedForPrefix.removePrefix("# ").trimStart()
            }
            trimmedForPrefix.startsWith("## ") -> {
                headerLevel = HeaderLevel.H2
                lineText = trimmedForPrefix.removePrefix("## ").trimStart()
            }
            trimmedForPrefix.startsWith("### ") -> {
                headerLevel = HeaderLevel.H3
                lineText = trimmedForPrefix.removePrefix("### ").trimStart()
            }
            trimmedForPrefix.startsWith("- [ ] ") || trimmedForPrefix.startsWith("* [ ] ") -> {
                isUncheckedTask = true
                lineText = "☐  " + trimmedForPrefix.substring(6)
            }
            trimmedForPrefix.startsWith("- [x] ") || trimmedForPrefix.startsWith("- [X] ") ||
                trimmedForPrefix.startsWith("* [x] ") || trimmedForPrefix.startsWith("* [X] ") -> {
                isCheckedTask = true
                lineText = "☑  " + trimmedForPrefix.substring(6)
            }
            trimmedForPrefix.startsWith("- ") || trimmedForPrefix.startsWith("* ") || trimmedForPrefix.startsWith("+ ") -> {
                lineText = "•  " + trimmedForPrefix.substring(2)
            }
            orderedListRegex.containsMatchIn(trimmedForPrefix) -> {
                val match = orderedListRegex.find(trimmedForPrefix)
                if (match != null) {
                    val numberGroup = match.groupValues[1]
                    lineText = "$numberGroup  " + trimmedForPrefix.substring(match.value.length)
                }
            }
        }

        var segments = parseMarkdownWidgetSegments(lineText)

        if (isQuote) {
            segments = segments.map { it.copy(italic = true) }
        }
        if (isCheckedTask) {
            segments = segments.map { it.copy(strikeThrough = true) }
        }

        result.add(
            WidgetMarkdownLine.Content(
                segments = segments,
                headerLevel = headerLevel,
                isQuote = isQuote,
                isCheckedTask = isCheckedTask,
                isUncheckedTask = isUncheckedTask
            )
        )
    }

    return result
}

private val orderedListRegex = Regex("^(\\d+\\.)\\s+")

private fun isHorizontalRule(text: String): Boolean {
    if (text.length < 3) return false
    val char = text[0]
    if (char != '-' && char != '*' && char != '_') return false
    return text.all { it == char }
}

/**
 * Markdown-aware replacement for Glance `Text` in widgets. Renders inline
 * markdown (including `=={cN}==` per-token colors) and block constructs
 * across single- and multi-line views; single-line content renders one
 * `Text` node directly to avoid extra RemoteViews containers.
 *
 * @param highlightColor default for `==highlight==` runs without a token.
 */
@Composable
fun GlanceMarkdownText(
    text: String,
    modifier: GlanceModifier = GlanceModifier,
    style: TextStyle = TextStyle(),
    highlightColor: UnitColorProvider = ColorProvider(
        day = MarkdownHighlightColor,
        night = MarkdownHighlightColor
    ),
    maxLines: Int = Int.MAX_VALUE
) {
    if (text.isEmpty()) {
        Text(text = text, style = style, maxLines = maxLines, modifier = modifier)
        return
    }

    val lines = remember(text) { parseMarkdownWidgetLines(text) }

    if (lines.isEmpty()) {
        Text(text = text, style = style, maxLines = maxLines, modifier = modifier)
        return
    }

    if (lines.size == 1 && lines[0] is WidgetMarkdownLine.Content) {
        GlanceMarkdownLineRow(
            line = lines[0] as WidgetMarkdownLine.Content,
            baseStyle = style,
            highlightColor = highlightColor,
            maxLines = maxLines,
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier) {
        var visibleCount = 0
        for (line in lines) {
            if (visibleCount >= maxLines) break

            when (line) {
                is WidgetMarkdownLine.Blank -> {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    visibleCount++
                }
                is WidgetMarkdownLine.HorizontalRule -> {
                    Text(
                        text = "──────────────",
                        style = style.copy(
                            color = style.color
                        ),
                        maxLines = 1
                    )
                    visibleCount++
                }
                is WidgetMarkdownLine.Content -> {
                    GlanceMarkdownLineRow(
                        line = line,
                        baseStyle = style,
                        highlightColor = highlightColor,
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                    visibleCount++
                }
            }
        }
    }
}

@Composable
private fun GlanceMarkdownLineRow(
    line: WidgetMarkdownLine.Content,
    baseStyle: TextStyle,
    highlightColor: UnitColorProvider,
    maxLines: Int,
    modifier: GlanceModifier = GlanceModifier
) {
    val lineTextStyle = computeLineTextStyle(baseStyle, line)

    if (line.segments.isEmpty()) {
        Text(text = "", style = lineTextStyle, maxLines = maxLines, modifier = modifier)
    } else if (line.segments.size == 1) {
        val segment = line.segments[0]
        Text(
            text = segment.text,
            style = segment.toGlanceTextStyle(lineTextStyle, highlightColor),
            maxLines = maxLines,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            line.segments.forEach { segment ->
                Text(
                    text = segment.text,
                    style = segment.toGlanceTextStyle(lineTextStyle, highlightColor),
                    maxLines = maxLines
                )
            }
        }
    }
}

private fun computeLineTextStyle(
    base: TextStyle,
    line: WidgetMarkdownLine.Content
): TextStyle {
    var result = base

    when (line.headerLevel) {
        HeaderLevel.H1 -> {
            val currentSize = base.fontSize
            val newSize = if (currentSize != null && currentSize != TextUnit.Unspecified) {
                (currentSize.value + 4).sp
            } else {
                20.sp
            }
            result = result.copy(fontSize = newSize, fontWeight = FontWeight.Bold)
        }
        HeaderLevel.H2 -> {
            val currentSize = base.fontSize
            val newSize = if (currentSize != null && currentSize != TextUnit.Unspecified) {
                (currentSize.value + 2).sp
            } else {
                18.sp
            }
            result = result.copy(fontSize = newSize, fontWeight = FontWeight.Bold)
        }
        HeaderLevel.H3 -> {
            result = result.copy(fontWeight = FontWeight.Bold)
        }
        HeaderLevel.None -> {}
    }

    if (line.isQuote) {
        result = result.copy(fontStyle = FontStyle.Italic)
    }

    if (line.isCheckedTask) {
        result = result.copy(textDecoration = TextDecoration.LineThrough)
    }

    return result
}

private fun WidgetTextSegment.toGlanceTextStyle(
    base: TextStyle,
    highlightColor: UnitColorProvider
): TextStyle {
    if (isPlain) return base
    var result = base
    if (bold) {
        result = result.copy(fontWeight = FontWeight.Bold)
    }
    if (italic) {
        result = result.copy(fontStyle = FontStyle.Italic)
    }
    if (strikeThrough) {
        result = result.copy(textDecoration = TextDecoration.LineThrough)
    }
    if (highlight) {
        val color = highlightHex?.parseHexColorOrNull()?.let {
            ColorProvider(day = it, night = it)
        } ?: highlightColor
        result = result.copy(color = color, fontWeight = FontWeight.Bold)
    }
    if (code) {
        result = result.copy(fontFamily = FontFamily.Monospace)
    }
    return result
}
