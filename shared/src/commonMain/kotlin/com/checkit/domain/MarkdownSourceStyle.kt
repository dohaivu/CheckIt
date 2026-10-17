package com.checkit.domain

/**
 * Display-style kinds for a *live* markdown editor, where the raw source
 * (markers included) stays visible but is styled as you type.
 *
 * This is the shared computation behind Android's `MarkdownVisualTransformation`
 * and the macOS `NSTextView` highlighter: each platform maps a kind to its own
 * text attributes and never re-parses markdown itself. Note this is distinct
 * from [parseRichText], which strips markers for read-only rendering.
 */
enum class MarkdownSourceStyleKind {
    Bold,
    Italic,
    Strikethrough,
    Highlight,
    Code,
    Header1,
    Header2,
    Header3,

    /** The `- `/`* ` list marker (first two characters of the line). */
    Bullet,

    /** The `1. ` list marker (digits, dot, space). */
    Numbered,

    /** The `>` blockquote indicator (`>` plus an optional following space). */
    Quote,

    /** The quoted body of a `>` line, rendered italic. */
    Quoted,
}

/** One styled range over the raw markdown source. Offsets are UTF-16 code units. */
data class MarkdownSourceStyleSpan(
    val kind: MarkdownSourceStyleKind,
    val start: Int,
    val end: Int,
    /** Resolved palette hex for a colored `=={cN}==` highlight; null means the caller's default. */
    val colorHex: String? = null,
)

private val BoldPattern = Regex("\\*\\*(.*?)\\*\\*")
private val ItalicPattern = Regex("(?:^|\\W)_([^_\\n]+?)_(?=$|\\W)")
private val StrikethroughPattern = Regex("~~(.*?)~~")
private val HighlightPattern = Regex("==(\\{c\\d+\\})?(.+?)==")
private val CodePattern = Regex("`([^`\\n]+?)`")
private val NumberedPattern = Regex("^\\d+\\.\\s")

/**
 * Computes the style spans for [text] while it is being edited. Line-based
 * constructs are matched first, then inline markers; ranges always cover the
 * raw markers so the caller can style the source in place. An unknown
 * `=={cN}==` token produces no highlight span (the text stays literal).
 */
fun parseMarkdownSourceStyles(text: String): List<MarkdownSourceStyleSpan> {
    if (text.isEmpty()) return emptyList()
    val spans = mutableListOf<MarkdownSourceStyleSpan>()

    // 1. Line-based constructs (headers, list markers, quotes).
    var lineStart = 0
    text.split('\n').forEach { line ->
        val lineEnd = lineStart + line.length
        when {
            line.startsWith("# ") ->
                spans.add(MarkdownSourceStyleSpan(MarkdownSourceStyleKind.Header1, lineStart, lineEnd))
            line.startsWith("## ") ->
                spans.add(MarkdownSourceStyleSpan(MarkdownSourceStyleKind.Header2, lineStart, lineEnd))
            line.startsWith("### ") ->
                spans.add(MarkdownSourceStyleSpan(MarkdownSourceStyleKind.Header3, lineStart, lineEnd))
            line.startsWith("- ") || line.startsWith("* ") ->
                spans.add(
                    MarkdownSourceStyleSpan(
                        MarkdownSourceStyleKind.Bullet,
                        lineStart,
                        (lineStart + 2).coerceAtMost(lineEnd)
                    )
                )
            NumberedPattern.find(line) != null -> {
                val markerLength = NumberedPattern.find(line)!!.value.length
                spans.add(
                    MarkdownSourceStyleSpan(
                        MarkdownSourceStyleKind.Numbered,
                        lineStart,
                        (lineStart + markerLength).coerceAtMost(lineEnd)
                    )
                )
            }
            line.startsWith("> ") || line.startsWith(">") -> {
                val prefixLen = if (line.startsWith("> ")) 2 else 1
                spans.add(
                    MarkdownSourceStyleSpan(
                        MarkdownSourceStyleKind.Quote,
                        lineStart,
                        (lineStart + prefixLen).coerceAtMost(lineEnd)
                    )
                )
                spans.add(MarkdownSourceStyleSpan(MarkdownSourceStyleKind.Quoted, lineStart, lineEnd))
            }
        }
        // Advance past the line and its '\n' separator.
        lineStart = lineEnd + 1
    }

    // 2. Inline constructs, matched over the raw source (markers included).
    BoldPattern.findAll(text).forEach {
        spans.add(MarkdownSourceStyleSpan(MarkdownSourceStyleKind.Bold, it.range.first, it.range.last + 1))
    }
    ItalicPattern.findAll(text).forEach {
        spans.add(MarkdownSourceStyleSpan(MarkdownSourceStyleKind.Italic, it.range.first, it.range.last + 1))
    }
    StrikethroughPattern.findAll(text).forEach {
        spans.add(
            MarkdownSourceStyleSpan(MarkdownSourceStyleKind.Strikethrough, it.range.first, it.range.last + 1)
        )
    }
    HighlightPattern.findAll(text).forEach { match ->
        val token = match.groupValues[1]
        // Unknown token: leave the whole match literal (no span), like the renderer.
        val colorHex = if (token.isEmpty()) null else highlightHexForToken(token) ?: return@forEach
        spans.add(
            MarkdownSourceStyleSpan(
                MarkdownSourceStyleKind.Highlight,
                match.range.first,
                match.range.last + 1,
                colorHex
            )
        )
    }
    CodePattern.findAll(text).forEach {
        spans.add(MarkdownSourceStyleSpan(MarkdownSourceStyleKind.Code, it.range.first, it.range.last + 1))
    }

    return spans
}
