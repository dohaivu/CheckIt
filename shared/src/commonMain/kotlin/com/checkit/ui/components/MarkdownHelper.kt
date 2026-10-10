package com.checkit.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.checkit.domain.RichSpan
import com.checkit.domain.RichSpanKind
import com.checkit.domain.highlightHexForToken
import com.checkit.domain.parseRichText
import com.checkit.ui.theme.parseHexColorOrNull

@Composable
fun String?.asMarkdownAnnotatedString(): AnnotatedString {
    val text = this ?: ""
    return remember(text) {
        parseMarkdownToAnnotatedString(text)
    }
}

class MarkdownVisualTransformation : VisualTransformation {
    private val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    private val italicRegex = Regex("(?:^|\\W)_([^_\\n]+?)_(?=$|\\W)")
    private val strikethroughRegex = Regex("~~(.*?)~~")
    private val highlightPattern = Regex("==(\\{c\\d+\\})?(.+?)==")
    private val codePattern = Regex("`([^`\\n]+?)`")
    // Matches any digit followed by a period and a space (e.g., "1. ", "12. ")
    private val numberedListRegex = Regex("^\\d+\\.\\s")

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text

        val transformed = buildAnnotatedString {
            // 1. Put down the raw text first
            append(rawText)

            // 2. Format Line-Based Elements: Headers & Lists
            var currentLineStart = 0
            val lines = rawText.split('\n')

            lines.forEach { line ->
                val lineLength = line.length

                when {
                    // Header 1: "# "
                    line.startsWith("# ") -> {
                        addStyle(
                            style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            start = currentLineStart,
                            end = currentLineStart + lineLength
                        )
                    }
                    // Header 2: "## "
                    line.startsWith("## ") -> {
                        addStyle(
                            style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            start = currentLineStart,
                            end = currentLineStart + lineLength
                        )
                    }
                    // Header 3: "### "
                    line.startsWith("### ") -> {
                        addStyle(
                            style = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                            start = currentLineStart,
                            end = currentLineStart + lineLength
                        )
                    }
                    // Bullet Lists: Starts with "- " or "* " (followed by space, not bolding)
                    line.startsWith("- ") || line.startsWith("* ") -> {
                        // Style just the marker symbol to make it look clean
                        addStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            start = currentLineStart,
                            end = currentLineStart + 2
                        )
                    }
                    // Numbered Lists: Starts with digits like "1. " or "2. "
                    numberedListRegex.find(line) != null -> {
                        val match = numberedListRegex.find(line)!!
                        val markerLength = match.value.length
                        // Style the number prefix to keep spacing consistent
                        addStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            ),
                            start = currentLineStart,
                            end = currentLineStart + markerLength
                        )
                    }
                    // Blockquotes: Starts with "> " or ">"
                    line.startsWith("> ") || line.startsWith(">") -> {
                        val prefixLen = if (line.startsWith("> ")) 2 else 1
                        addStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            start = currentLineStart,
                            end = currentLineStart + prefixLen
                        )
                        addStyle(
                            style = SpanStyle(fontStyle = FontStyle.Italic),
                            start = currentLineStart,
                            end = currentLineStart + lineLength
                        )
                    }
                }
                // Move index tracker past the current line and its \n newline token
                currentLineStart += lineLength + 1
            }

            // 3. Format Inline Elements: Bold (**text**)
            boldRegex.findAll(rawText).forEach { matchResult ->
                val range = matchResult.range
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = range.first,
                    end = range.last + 1
                )
            }

            // 4. Format Inline Elements: Italic (_text_)
            italicRegex.findAll(rawText).forEach { matchResult ->
                val range = matchResult.range
                addStyle(
                    style = SpanStyle(fontStyle = FontStyle.Italic),
                    start = range.first,
                    end = range.last + 1
                )
            }

            // 5. Format Inline Elements: Strikethrough (~~text~~)
            strikethroughRegex.findAll(rawText).forEach { matchResult ->
                val range = matchResult.range
                addStyle(
                    style = SpanStyle(textDecoration = TextDecoration.LineThrough),
                    start = range.first,
                    end = range.last + 1
                )
            }

            // 6. Format Inline Elements: Highlight (==text== / =={cN}text==,
            // markers included like the rest). Unknown tokens stay unstyled.
            highlightPattern.findAll(rawText).forEach { matchResult ->
                val range = matchResult.range
                val token = matchResult.groupValues[1]
                val color = if (token.isEmpty()) {
                    MarkdownHighlightColor
                } else {
                    highlightHexForToken(token)?.parseHexColorOrNull() ?: return@forEach
                }
                addStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = color
                    ),
                    start = range.first,
                    end = range.last + 1
                )
            }

            // 7. Format Inline Elements: Code (`text`)
            codePattern.findAll(rawText).forEach { matchResult ->
                val range = matchResult.range
                addStyle(
                    style = SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = MarkdownCodeBackgroundColor
                    ),
                    start = range.first,
                    end = range.last + 1
                )
            }
        }

        return TransformedText(transformed, OffsetMapping.Identity)
    }
}

fun parseMarkdownToAnnotatedString(
    markdown: String?,
    highlightColor: Color = MarkdownHighlightColor
): AnnotatedString {
    if (markdown.isNullOrEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        val lines = markdown.split('\n')

        lines.forEachIndexed { index, line ->
            var cleanLine = line
            var isHeader = false
            var isQuote = false
            var headerStyle: SpanStyle? = null
            var quoteStyle: SpanStyle? = null

            // 1. Process Line-Based Elements (Headers, Lists & Quotes)
            when {
                cleanLine.startsWith("# ") -> {
                    cleanLine = cleanLine.removePrefix("# ")
                    isHeader = true
                    headerStyle = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                cleanLine.startsWith("## ") -> {
                    cleanLine = cleanLine.removePrefix("## ")
                    isHeader = true
                    headerStyle = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                cleanLine.startsWith("### ") -> {
                    cleanLine = cleanLine.removePrefix("### ")
                    isHeader = true
                    headerStyle = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                cleanLine.startsWith("- ") || cleanLine.startsWith("* ") -> {
                    cleanLine = "•  " + cleanLine.substring(2)
                }
                cleanLine.startsWith("> ") -> {
                    cleanLine = "│  " + cleanLine.substring(2)
                    isQuote = true
                    quoteStyle = SpanStyle(fontStyle = FontStyle.Italic)
                }
                cleanLine.startsWith(">") -> {
                    cleanLine = "│  " + cleanLine.substring(1).trimStart()
                    isQuote = true
                    quoteStyle = SpanStyle(fontStyle = FontStyle.Italic)
                }
            }

            // Track exactly where this line starts in our main builder string
            val lineStartIndex = this.length

            // 2. Clear markdown inline symbols and calculate exact styling positions
            val (finalLineText, stylesToApply) = processInlineStyles(cleanLine, highlightColor)

            // Append only the clean text without structural markers
            append(finalLineText)
            val lineEndIndex = this.length

            // Apply header styling if matched
            if (isHeader && headerStyle != null) {
                addStyle(headerStyle, lineStartIndex, lineEndIndex)
            }

            // Apply quote styling and indicator prefix style if matched
            if (isQuote) {
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    start = lineStartIndex,
                    end = (lineStartIndex + 2).coerceAtMost(lineEndIndex)
                )
                if (quoteStyle != null) {
                    addStyle(quoteStyle, lineStartIndex, lineEndIndex)
                }
            }

            // Apply all saved bold, italic, and strikethrough styles using their adjusted positions
            stylesToApply.forEach { styleMarker ->
                addStyle(
                    style = styleMarker.style,
                    start = lineStartIndex + styleMarker.start,
                    end = lineStartIndex + styleMarker.end
                )
            }

            // Add a newline character for all lines except the last one
            if (index < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

// Default marker color for ==highlight== when callers don't pass a theme color.
// Deep orange reads on both light and dark surfaces.
internal val MarkdownHighlightColor: Color = Color(0xFFE65100)
internal val MarkdownCodeBackgroundColor: Color = Color(0x20808080)

// A simple helper data class to store layout positions
private data class StyleMarker(val style: SpanStyle, val start: Int, val end: Int)

private fun RichSpan.toSpanStyle(defaultHighlight: Color): SpanStyle {
    val highlight = colorHex?.parseHexColorOrNull() ?: defaultHighlight
    return when (kind) {
        RichSpanKind.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
        RichSpanKind.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
        RichSpanKind.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
        RichSpanKind.Highlight -> SpanStyle(fontWeight = FontWeight.Bold, color = highlight)
        RichSpanKind.Code -> SpanStyle(fontFamily = FontFamily.Monospace, background = MarkdownCodeBackgroundColor)
    }
}

private fun processInlineStyles(
    inputLine: String,
    highlightColor: Color = MarkdownHighlightColor
): Pair<String, List<StyleMarker>> {
    val parsed = parseRichText(inputLine)
    return parsed.text to parsed.spans.map { span ->
        StyleMarker(
            style = span.toSpanStyle(highlightColor),
            start = span.start,
            end = span.end
        )
    }
}

/**
 * Inline-only markdown for single-line rows: strips markers and returns a
 * styled AnnotatedString. Unlike [parseMarkdownToAnnotatedString], line-based
 * constructs (`# ` headers, `> ` quotes, list bullets) are left untouched so a
 * row starting with `#tag` never blows up into a header.
 */
fun parseMarkdownInline(
    text: String,
    highlightColor: Color = MarkdownHighlightColor
): AnnotatedString {
    val parsed = parseRichText(text)
    return AnnotatedString(
        text = parsed.text,
        spanStyles = parsed.spans.map { span ->
            AnnotatedString.Range(
                span.toSpanStyle(highlightColor),
                span.start,
                span.end
            )
        }
    )
}
