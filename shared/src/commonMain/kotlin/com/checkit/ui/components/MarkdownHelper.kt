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
import com.checkit.domain.MarkdownSourceStyleKind
import com.checkit.domain.MarkdownSourceStyleSpan
import com.checkit.domain.RichSpan
import com.checkit.domain.RichSpanKind
import com.checkit.domain.parseMarkdownSourceStyles
import com.checkit.domain.parseRichText
import com.checkit.ui.theme.parseHexColorOrNull

@Composable
fun String?.asMarkdownAnnotatedString(): AnnotatedString {
    val text = this ?: ""
    return remember(text) {
        renderFullMarkdown(text)
    }
}

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val transformed = buildAnnotatedString {
            // Keep the raw source; only add display styles over it.
            append(rawText)
            parseMarkdownSourceStyles(rawText).forEach { span ->
                addStyle(
                    style = span.toSpanStyle(),
                    start = span.start.coerceIn(0, rawText.length),
                    end = span.end.coerceIn(0, rawText.length)
                )
            }
        }
        return TransformedText(transformed, OffsetMapping.Identity)
    }
}

/** Maps a shared source-style kind to its Compose display style. */
private fun MarkdownSourceStyleSpan.toSpanStyle(): SpanStyle = when (kind) {
    MarkdownSourceStyleKind.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
    MarkdownSourceStyleKind.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
    MarkdownSourceStyleKind.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    MarkdownSourceStyleKind.Highlight -> SpanStyle(
        fontWeight = FontWeight.Bold,
        color = colorHex?.parseHexColorOrNull() ?: MarkdownHighlightColor
    )
    MarkdownSourceStyleKind.Code -> SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = MarkdownCodeBackgroundColor
    )
    MarkdownSourceStyleKind.Header1 -> SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    MarkdownSourceStyleKind.Header2 -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    MarkdownSourceStyleKind.Header3 -> SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    MarkdownSourceStyleKind.Bullet -> SpanStyle(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    MarkdownSourceStyleKind.Numbered -> SpanStyle(fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    MarkdownSourceStyleKind.Quote -> SpanStyle(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    MarkdownSourceStyleKind.Quoted -> SpanStyle(fontStyle = FontStyle.Italic)
}

fun renderFullMarkdown(
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
                    cleanLine = QuoteIndicator + cleanLine.substring(2)
                    isQuote = true
                }
                cleanLine.startsWith(">") -> {
                    cleanLine = QuoteIndicator + cleanLine.substring(1).trimStart()
                    isQuote = true
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
                // Upright bold monospace bar/indent so the spine reads cleanly.
                val prefixEnd = (lineStartIndex + QuoteIndicator.length).coerceAtMost(lineEndIndex)
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    start = lineStartIndex,
                    end = prefixEnd
                )
                // Italic applies to the quoted content only, leaving the bar upright.
                addStyle(
                    style = SpanStyle(fontStyle = FontStyle.Italic),
                    start = prefixEnd,
                    end = lineEndIndex
                )
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

/**
 * Inline-only markdown for single-line rows: strips markers and returns a
 * styled AnnotatedString. Unlike [renderFullMarkdown], line-based
 * constructs (`# ` headers, `> ` quotes, list bullets) are left untouched so a
 * row starting with `#tag` never blows up into a header.
 */
fun renderInlineMarkdown(
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


// Default marker color for ==highlight== when callers don't pass a theme color.
// Deep orange reads on both light and dark surfaces.
internal val MarkdownHighlightColor: Color = Color(0xFFE65100)
internal val MarkdownCodeBackgroundColor: Color = Color(0x20808080)

// Leading indicator for `>` blockquote lines: a vertical bar plus two spaces.
// Kept monospace-bold and upright so it reads as a spine and the italic
// content starts at a fixed column.
private const val QuoteIndicator = "│  "

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
