package com.checkit.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownParsingTest {

    @Test
    fun parseMarkdownToAnnotatedString_handlesNullAndEmpty() {
        assertEquals("", parseMarkdownToAnnotatedString(null).text)
        assertEquals("", parseMarkdownToAnnotatedString("").text)
    }

    @Test
    fun parseMarkdownToAnnotatedString_parsesStrikethrough() {
        val input = "This is ~~strikethrough~~ text"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("This is strikethrough text", result.text)

        val styles = result.spanStyles
        assertEquals(1, styles.size)
        val styleSpan = styles[0]
        assertEquals(8, styleSpan.start)
        assertEquals(21, styleSpan.end)
        assertEquals(TextDecoration.LineThrough, styleSpan.item.textDecoration)
    }

    @Test
    fun parseMarkdownToAnnotatedString_parsesBoldAndItalicAndStrikethrough() {
        val input = "**Bold**, _italic_, and ~~strikethrough~~"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("Bold, italic, and strikethrough", result.text)

        val styles = result.spanStyles
        assertEquals(3, styles.size)

        val boldSpan = styles.first { it.item.fontWeight == FontWeight.Bold }
        assertEquals(0, boldSpan.start)
        assertEquals(4, boldSpan.end)

        val italicSpan = styles.first { it.item.fontStyle == FontStyle.Italic }
        assertEquals(6, italicSpan.start)
        assertEquals(12, italicSpan.end)

        val strikeSpan = styles.first { it.item.textDecoration == TextDecoration.LineThrough }
        assertEquals(18, strikeSpan.start)
        assertEquals(31, strikeSpan.end)
    }

    @Test
    fun parseMarkdownToAnnotatedString_parsesNestedStrikethroughAndBold() {
        val input = "~~**bold strikethrough**~~"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("bold strikethrough", result.text)

        val styles = result.spanStyles
        assertEquals(2, styles.size)

        val strikeSpan = styles.first { it.item.textDecoration == TextDecoration.LineThrough }
        assertEquals(0, strikeSpan.start)
        assertEquals(18, strikeSpan.end)

        val boldSpan = styles.first { it.item.fontWeight == FontWeight.Bold }
        assertEquals(0, boldSpan.start)
        assertEquals(18, boldSpan.end)
    }

    @Test
    fun markdownVisualTransformation_appliesStrikethroughStyle() {
        val transformation = MarkdownVisualTransformation()
        val input = AnnotatedString("This is ~~strikethrough~~ text")
        val transformed = transformation.filter(input)

        assertEquals("This is ~~strikethrough~~ text", transformed.text.text)

        val strikeSpan = transformed.text.spanStyles.firstOrNull { it.item.textDecoration == TextDecoration.LineThrough }
        assertTrue(strikeSpan != null, "Expected a line-through span style")
        assertEquals(8, strikeSpan.start)
        assertEquals(25, strikeSpan.end) // Includes raw "~~strikethrough~~"
    }

    @Test
    fun parseMarkdownToAnnotatedString_parsesBlockquote() {
        val input = "> This is a quote"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("│  This is a quote", result.text)

        val italicSpan = result.spanStyles.firstOrNull { it.item.fontStyle == FontStyle.Italic }
        assertTrue(italicSpan != null, "Expected italic style for quote")
        assertEquals(0, italicSpan.start)
        assertEquals(18, italicSpan.end)

        val barSpan = result.spanStyles.firstOrNull { it.item.fontWeight == FontWeight.Bold }
        assertTrue(barSpan != null, "Expected bold style for quote prefix bar")
        assertEquals(0, barSpan.start)
        assertEquals(2, barSpan.end)
    }

    @Test
    fun parseMarkdownToAnnotatedString_parsesHighlight() {
        val input = "==If== it's 9am, ==then== I will read book"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("If it's 9am, then I will read book", result.text)

        val highlights = result.spanStyles.filter { it.item.color == MarkdownHighlightColor }
        assertEquals(2, highlights.size)
        assertEquals(0, highlights[0].start)
        assertEquals(2, highlights[0].end)
        assertEquals(FontWeight.Bold, highlights[0].item.fontWeight)
        assertEquals(13, highlights[1].start)
        assertEquals(17, highlights[1].end)
    }

    @Test
    fun parseMarkdownToAnnotatedString_parsesNestedBoldHighlight() {
        val input = "**==both==**"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("both", result.text)

        assertTrue(result.spanStyles.any { it.item.color == MarkdownHighlightColor })
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold && it.item.color == Color.Unspecified })
    }

    @Test
    fun parseMarkdownToAnnotatedString_leavesUnclosedHighlightLiteral() {
        val input = "==oops"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("==oops", result.text)
        assertTrue(result.spanStyles.none { it.item.color == MarkdownHighlightColor })
    }

    @Test
    fun parseMarkdownToAnnotatedString_acceptsCustomHighlightColor() {
        val input = "==hi=="
        val custom = Color.Red
        val result = parseMarkdownToAnnotatedString(input, highlightColor = custom)

        assertEquals("hi", result.text)
        assertEquals(custom, result.spanStyles.single().item.color)
    }

    @Test
    fun markdownVisualTransformation_appliesHighlightStyle() {
        val transformation = MarkdownVisualTransformation()
        val input = AnnotatedString("This is ==highlight== text")
        val transformed = transformation.filter(input)

        assertEquals("This is ==highlight== text", transformed.text.text)

        val highlightSpan = transformed.text.spanStyles.firstOrNull { it.item.color == MarkdownHighlightColor }
        assertTrue(highlightSpan != null, "Expected a highlight span style")
        assertEquals(8, highlightSpan.start)
        assertEquals(21, highlightSpan.end) // Includes raw "==highlight=="
        assertEquals(FontWeight.Bold, highlightSpan.item.fontWeight)
    }

    @Test
    fun parseMarkdownToAnnotatedString_parsesUnderscoreItalic() {
        val input = "This is _italic_ text"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("This is italic text", result.text)

        val italicSpan = result.spanStyles.single()
        assertEquals(FontStyle.Italic, italicSpan.item.fontStyle)
        assertEquals(8, italicSpan.start)
        assertEquals(14, italicSpan.end)
    }

    @Test
    fun parseMarkdownToAnnotatedString_parsesNestedBoldUnderscoreItalic() {
        val input = "**_both_**"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("both", result.text)
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(result.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun parseMarkdownToAnnotatedString_parsesNestedUnderscoreBold() {
        val input = "_**both**_"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("both", result.text)
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(result.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun parseMarkdownToAnnotatedString_leavesSnakeCaseLiteral() {
        val input = "my_var_name"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("my_var_name", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun parseMarkdownToAnnotatedString_italicRespectsBoundaries() {
        val input = "(see _this_)"
        val result = parseMarkdownToAnnotatedString(input)

        assertEquals("(see this)", result.text)

        val italicSpan = result.spanStyles.single()
        assertEquals(FontStyle.Italic, italicSpan.item.fontStyle)
        assertEquals(5, italicSpan.start)
        assertEquals(9, italicSpan.end)
    }

    @Test
    fun markdownVisualTransformation_appliesUnderscoreItalicStyle() {
        val transformation = MarkdownVisualTransformation()
        val input = AnnotatedString("This is _italic_ text")
        val transformed = transformation.filter(input)

        assertEquals("This is _italic_ text", transformed.text.text)

        val italicSpan = transformed.text.spanStyles.firstOrNull { it.item.fontStyle == FontStyle.Italic }
        assertTrue(italicSpan != null, "Expected an italic span style")
        assertEquals(7, italicSpan.start)
        assertEquals(16, italicSpan.end) // Includes leading boundary plus raw "_italic_"
    }

    @Test
    fun markdownVisualTransformation_appliesQuoteStyle() {
        val transformation = MarkdownVisualTransformation()
        val input = AnnotatedString("> A blockquote line")
        val transformed = transformation.filter(input)

        assertEquals("> A blockquote line", transformed.text.text)

        val italicSpan = transformed.text.spanStyles.firstOrNull { it.item.fontStyle == FontStyle.Italic }
        assertTrue(italicSpan != null, "Expected italic style for quote line")
        assertEquals(0, italicSpan.start)
        assertEquals(19, italicSpan.end)
    }
}
