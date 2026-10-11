package com.checkit.domain

/** Inline span kinds parsed from markdown markers. */
enum class RichSpanKind {
    Bold,
    Italic,
    Strikethrough,
    Highlight,
    Code,
}

/** One styled run. Offsets are UTF-16 code units into [RichText.text]. */
data class RichSpan(
    val kind: RichSpanKind,
    val start: Int,
    val end: Int,
    /** Hex color for [RichSpanKind.Highlight] with an explicit `{cN}` token; null means the default. */
    val colorHex: String? = null
)

/**
 * Shared highlight palette for `=={cN}==` (1-based `c1`..`c13`). Single
 * source of truth, also surfaced to pickers as
 * `AppIconColorDefaults.CommonColors`. Lowercase only; anything else is not
 * a token.
 */
val HighlightPalette: List<String> = listOf(
    "#2563EB", // c1 Blue
    "#7C3AED", // c2 Violet
    "#C026D3", // c3 Fuchsia
    "#DB2777", // c4 Pink
    "#DC2626", // c5 Red
    "#EA580C", // c6 Orange
    "#CA8A04", // c7 Amber
    "#65A30D", // c8 Lime
    "#16A34A", // c9 Green
    "#0D9488", // c10 Teal
    "#0891B2", // c11 Cyan
    "#7C2D12", // c12 Brown
    "#64748B", // c13 Slate
)

/** Resolves a `{cN}` token (braces included) to its hex, or null when unknown. */
fun highlightHexForToken(token: String): String? =
    token.removePrefix("{c").removeSuffix("}")
        .toIntOrNull()
        ?.takeIf { it in 1..HighlightPalette.size }
        ?.let { HighlightPalette[it - 1] }

/** Clean text (markers stripped) plus its inline spans. */
data class RichText(
    val text: String,
    val spans: List<RichSpan>
)

/**
 * Parses inline markdown spans (`**bold**`, `_italic_`, `~~strike~~`,
 * `==highlight==`, `` `code` ``), stripping markers. Unmatched markers stay literal.
 * Nesting recurses (e.g. `**==bold highlight==**`).
 *
 * Highlight takes an optional color token: `=={c1}text==` colors the span
 * with that palette entry (see [HighlightPalette]); an unknown token leaves
 * the whole match literal, while a malformed token (`{C1}`, `{cx}`) degrades
 * to a default highlight with the braces visible.
 *
 * Italic uses `_` (not `*`) so `**bold**` and `*italic*` never collide the
 * way `***both***` does — and because `NSRegularExpression` (which backs
 * Kotlin Regex on Apple targets) has no lookbehind, the ambiguity can't be
 * fixed with lookarounds either. The opening `_` must start the string or
 * follow a non-word char, and the closing `_` must end the string or precede
 * a non-word char, so `snake_case` stays literal.
 *
 * Pure and platform-neutral: Compose builds an AnnotatedString from this,
 * SwiftUI builds an AttributedString. Offsets are UTF-16 code units, so
 * Swift must convert via `String.Index(utf16Offset:in:)`, never integer
 * subscripting.
 */
fun parseRichText(input: String): RichText {
    val spans = mutableListOf<RichSpan>()
    // Bold (**), strikethrough (~~), italic (_), highlight (==, optional {cN} token), and inline code (`).
    val combinedRegex = Regex("(\\*\\*(.*?)\\*\\*)|(~~(.*?)~~)|((?:^|\\W)_([^_\\n]+?)_(?=$|\\W))|(==(\\{c\\d+\\})?(.+?)==)|(`([^`\\n]+?)`)")

    val resultText = StringBuilder()
    var lastIndex = 0

    combinedRegex.findAll(input).forEach { match ->
        // groupValues[1/2] bold, [3/4] strikethrough, [5/6] italic,
        // [7] highlight (with [8] token and [9] inner), [10/11] code.
        val kind = when {
            match.groupValues[1].isNotEmpty() -> RichSpanKind.Bold
            match.groupValues[3].isNotEmpty() -> RichSpanKind.Strikethrough
            match.groupValues[5].isNotEmpty() -> RichSpanKind.Italic
            match.groupValues[7].isNotEmpty() -> RichSpanKind.Highlight
            else -> RichSpanKind.Code
        }
        // The italic alternative consumes one leading boundary char (a
        // non-word char matched by (?:^|\W)); re-emit it literally. At string
        // start there is no boundary char: the match then starts with '_'
        // itself ('_' is a word char, so \W can never consume it).
        val leadLen =
            if (kind == RichSpanKind.Italic && input[match.range.first] != '_') 1 else 0
        resultText.append(input.substring(lastIndex, match.range.first + leadLen))

        // Highlight resolves its optional color token now: an unknown token
        // leaves the whole match literal (markers visible, no span).
        val highlightHex = if (kind == RichSpanKind.Highlight) {
            val token = match.groupValues[8]
            if (token.isNotEmpty()) {
                highlightHexForToken(token) ?: run {
                    resultText.append(input.substring(match.range.first + leadLen, match.range.last + 1))
                    lastIndex = match.range.last + 1
                    return@forEach
                }
            } else null
        } else null

        val innerTextRaw = when (kind) {
            RichSpanKind.Bold -> match.groupValues[2]
            RichSpanKind.Strikethrough -> match.groupValues[4]
            RichSpanKind.Italic -> match.groupValues[6]
            RichSpanKind.Highlight -> match.groupValues[9]
            RichSpanKind.Code -> match.groupValues[11]
        }

        val start = resultText.length
        // Recurse to handle nested styles, reusing the same markers.
        val inner = parseRichText(innerTextRaw)

        resultText.append(inner.text)
        val end = resultText.length

        spans.add(RichSpan(kind = kind, start = start, end = end, colorHex = highlightHex))
        inner.spans.forEach { innerSpan ->
            spans.add(innerSpan.copy(start = start + innerSpan.start, end = start + innerSpan.end))
        }

        lastIndex = match.range.last + 1
    }

    resultText.append(input.substring(lastIndex))

    return RichText(text = resultText.toString(), spans = spans)
}
