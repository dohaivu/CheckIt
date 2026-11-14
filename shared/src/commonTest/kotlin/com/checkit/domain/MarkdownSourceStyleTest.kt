package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownSourceStyleTest {

    private fun List<MarkdownSourceStyleSpan>.of(kind: MarkdownSourceStyleKind) =
        filter { it.kind == kind }

    @Test
    fun emptyInputProducesNoSpans() {
        assertTrue(parseMarkdownSourceStyles("").isEmpty())
    }

    @Test
    fun headerSpansCoverWholeLine() {
        val spans = parseMarkdownSourceStyles("# Title")
        val header = spans.of(MarkdownSourceStyleKind.Header1).single()
        assertEquals(0, header.start)
        assertEquals(7, header.end)
    }

    @Test
    fun bulletSpanCoversOnlyMarker() {
        val spans = parseMarkdownSourceStyles("- item")
        val bullet = spans.of(MarkdownSourceStyleKind.Bullet).single()
        assertEquals(0, bullet.start)
        assertEquals(2, bullet.end)
    }

    @Test
    fun numberedSpanCoversDigitsDotSpace() {
        val spans = parseMarkdownSourceStyles("12. item")
        val numbered = spans.of(MarkdownSourceStyleKind.Numbered).single()
        assertEquals(0, numbered.start)
        assertEquals(4, numbered.end)
    }

    @Test
    fun quoteSpansSplitIndicatorAndBody() {
        val spans = parseMarkdownSourceStyles("> hello")
        val quote = spans.of(MarkdownSourceStyleKind.Quote).single()
        assertEquals(0, quote.start)
        assertEquals(2, quote.end)
        val body = spans.of(MarkdownSourceStyleKind.Quoted).single()
        assertEquals(0, body.start)
        assertEquals(7, body.end)
    }

    @Test
    fun inlineSpansKeepRawMarkers() {
        val spans = parseMarkdownSourceStyles("a **b** c `d`")
        val bold = spans.of(MarkdownSourceStyleKind.Bold).single()
        assertEquals(2, bold.start)
        assertEquals(7, bold.end)
        val code = spans.of(MarkdownSourceStyleKind.Code).single()
        assertEquals(10, code.start)
        assertEquals(13, code.end)
    }

    @Test
    fun coloredHighlightCarriesResolvedHex() {
        val span = parseMarkdownSourceStyles("=={c5}hi==").of(MarkdownSourceStyleKind.Highlight).single()
        assertEquals("#16A34A", span.colorHex)
    }

    @Test
    fun defaultHighlightHasNoColor() {
        val span = parseMarkdownSourceStyles("==hi==").of(MarkdownSourceStyleKind.Highlight).single()
        assertEquals(null, span.colorHex)
    }

    @Test
    fun unknownHighlightTokenProducesNoSpan() {
        assertTrue(parseMarkdownSourceStyles("=={c99}hi==").of(MarkdownSourceStyleKind.Highlight).isEmpty())
    }

    @Test
    fun italicSpanIncludesLeadingBoundary() {
        val span = parseMarkdownSourceStyles("This is _italic_ text").of(MarkdownSourceStyleKind.Italic).single()
        assertEquals(7, span.start)
        assertEquals(16, span.end)
    }

    @Test
    fun nestedBoldItalicSpansOverlap() {
        // Raw-source styling keeps the markers, and nesting produces an inner
        // span inside the outer one. Platforms must merge font traits instead
        // of overwriting `.font` (see macOS `MarkdownSourceStyling.apply`).
        val spans = parseMarkdownSourceStyles("**_both_**")
        val bold = spans.of(MarkdownSourceStyleKind.Bold).single()
        val italic = spans.of(MarkdownSourceStyleKind.Italic).single()
        assertEquals(0, bold.start)
        assertEquals(10, bold.end)
        assertTrue(italic.start >= bold.start && italic.end <= bold.end)
    }

    @Test
    fun fencedCodeIsNotStyledAsMarkdown() {
        val text = "```\n# not a header\n**not bold**\n```"
        val spans = parseMarkdownSourceStyles(text)

        assertTrue(spans.of(MarkdownSourceStyleKind.Header1).isEmpty())
        assertTrue(spans.of(MarkdownSourceStyleKind.Bold).isEmpty())
        val code = spans.of(MarkdownSourceStyleKind.Code).single()
        assertEquals("```\n".length, code.start)
        assertEquals(text.length - "```".length - 1, code.end)
    }

    @Test
    fun stylesOutsideFenceStillApply() {
        val text = "```\nx\n```\n# header"
        val spans = parseMarkdownSourceStyles(text)

        val header = spans.of(MarkdownSourceStyleKind.Header1).single()
        assertEquals(text.indexOf("# header"), header.start)
        assertEquals(text.length, header.end)
    }

    @Test
    fun unclosedFenceContentIsCode() {
        val text = "# before\n```\n# inside"
        val spans = parseMarkdownSourceStyles(text)

        assertEquals(0, spans.of(MarkdownSourceStyleKind.Header1).single().start)
        val code = spans.of(MarkdownSourceStyleKind.Code).single()
        assertEquals(text.indexOf("# inside"), code.start)
        assertEquals(text.length, code.end)
    }

    @Test
    fun inlineCodeOutsideFenceIsStillStyled() {
        val text = "`a`\n```\n`b`\n```"
        val code = parseMarkdownSourceStyles(text).of(MarkdownSourceStyleKind.Code)

        assertEquals(2, code.size)
        val inline = code.first { it.start == 0 }
        assertEquals(3, inline.end)
        val block = code.first { it.start == text.indexOf("`b`") }
        assertEquals(text.length - "```".length - 1, block.end)
    }
}
