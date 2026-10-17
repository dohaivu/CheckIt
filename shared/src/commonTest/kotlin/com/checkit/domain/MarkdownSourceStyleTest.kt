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
        assertEquals("#DC2626", span.colorHex)
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
}
