package com.checkit.ui.components

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
        val input = "**Bold**, *italic*, and ~~strikethrough~~"
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
}
