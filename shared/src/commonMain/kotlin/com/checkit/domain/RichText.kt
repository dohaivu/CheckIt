package com.checkit.domain

/** Inline span kinds parsed from markdown markers. */
enum class RichSpanKind {
    Bold,
    Italic,
    Strikethrough,
    Highlight,
}

/** One styled run. Offsets are UTF-16 code units into [RichText.text]. */
data class RichSpan(
    val kind: RichSpanKind,
    val start: Int,
    val end: Int
)

/** Clean text (markers stripped) plus its inline spans. */
data class RichText(
    val text: String,
    val spans: List<RichSpan>
)

/**
 * Parses inline markdown spans (`**bold**`, `*italic*`, `~~strike~~`,
 * `==highlight==`), stripping markers. Unmatched markers stay literal.
 * Nesting recurses (e.g. `**==bold highlight==**`).
 *
 * Pure and platform-neutral: Compose builds an AnnotatedString from this,
 * SwiftUI builds an AttributedString. Offsets are UTF-16 code units, so
 * Swift must convert via `String.Index(utf16Offset:in:)`, never integer
 * subscripting.
 */
fun parseRichText(input: String): RichText {
    val spans = mutableListOf<RichSpan>()
    // Bold (**) and strikethrough (~~) first, then italic (*), then highlight (==).
    val combinedRegex = Regex("(\\*\\*(.*?)\\*\\*)|(~~(.*?)~~)|(\\*(.*?)\\*)|(==(.+?)==)")

    val resultText = StringBuilder()
    var lastIndex = 0

    combinedRegex.findAll(input).forEach { match ->
        resultText.append(input.substring(lastIndex, match.range.first))

        // groupValues[1/2] bold, [3/4] strikethrough, [5/6] italic, [7/8] highlight.
        val kind = when {
            match.groupValues[1].isNotEmpty() -> RichSpanKind.Bold
            match.groupValues[3].isNotEmpty() -> RichSpanKind.Strikethrough
            match.groupValues[5].isNotEmpty() -> RichSpanKind.Italic
            else -> RichSpanKind.Highlight
        }
        val innerTextRaw = when (kind) {
            RichSpanKind.Bold -> match.groupValues[2]
            RichSpanKind.Strikethrough -> match.groupValues[4]
            RichSpanKind.Italic -> match.groupValues[6]
            RichSpanKind.Highlight -> match.groupValues[8]
        }

        val start = resultText.length
        // Recurse to handle nested styles, reusing the same markers.
        val inner = parseRichText(innerTextRaw)

        resultText.append(inner.text)
        val end = resultText.length

        spans.add(RichSpan(kind = kind, start = start, end = end))
        inner.spans.forEach { innerSpan ->
            spans.add(innerSpan.copy(start = start + innerSpan.start, end = start + innerSpan.end))
        }

        lastIndex = match.range.last + 1
    }

    resultText.append(input.substring(lastIndex))

    return RichText(text = resultText.toString(), spans = spans)
}
