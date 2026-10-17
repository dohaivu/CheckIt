package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlToMarkdownTest {

    @Test
    fun blankInputProducesEmptyOutput() {
        assertEquals("", htmlToMarkdown(""))
        assertEquals("", htmlToMarkdown("   \n  "))
    }

    @Test
    fun plainTextPassesThrough() {
        assertEquals("Hello world", htmlToMarkdown("<p>Hello world</p>"))
        assertEquals("no tags here", htmlToMarkdown("no tags here"))
    }

    @Test
    fun inlineEmphasis() {
        assertEquals("Hello **world**!", htmlToMarkdown("<p>Hello <strong>world</strong>!</p>"))
        assertEquals("**bold**", htmlToMarkdown("<b>bold</b>"))
        assertEquals("_italic_", htmlToMarkdown("<em>italic</em>"))
        assertEquals("_italic_", htmlToMarkdown("<i>italic</i>"))
        assertEquals("~~gone~~", htmlToMarkdown("<del>gone</del>"))
        assertEquals("~~gone~~", htmlToMarkdown("<s>gone</s>"))
        assertEquals("==focus==", htmlToMarkdown("<mark>focus</mark>"))
        assertEquals("`val x`", htmlToMarkdown("<code>val x</code>"))
    }

    @Test
    fun nestedEmphasis() {
        assertEquals("**_both_**", htmlToMarkdown("<strong><em>both</em></strong>"))
    }

    @Test
    fun headingsClampToThreeLevels() {
        assertEquals("# Title", htmlToMarkdown("<h1>Title</h1>"))
        assertEquals("## Title", htmlToMarkdown("<h2>Title</h2>"))
        assertEquals("### Title", htmlToMarkdown("<h3>Title</h3>"))
        assertEquals("### Title", htmlToMarkdown("<h4>Title</h4>"))
        assertEquals("### Title", htmlToMarkdown("<h6>Title</h6>"))
    }

    @Test
    fun paragraphsBecomeBlankSeparatedLines() {
        assertEquals("a\n\nb", htmlToMarkdown("<p>a</p><p>b</p>"))
    }

    @Test
    fun lineBreakBecomesNewline() {
        assertEquals("a\nb", htmlToMarkdown("<p>a<br>b</p>"))
    }

    @Test
    fun listsBecomeMarkers() {
        assertEquals("- a\n- b", htmlToMarkdown("<ul><li>a</li><li>b</li></ul>"))
        assertEquals("1. a\n2. b", htmlToMarkdown("<ol><li>a</li><li>b</li></ol>"))
        assertEquals("- a", htmlToMarkdown("<ul><li><p>a</p></li></ul>"))
    }

    @Test
    fun nestedListsAreFlattened() {
        assertEquals(
            "- a\n- b",
            htmlToMarkdown("<ul><li>a<ul><li>b</li></ul></li></ul>"),
        )
    }

    @Test
    fun blockquotePrefixesEveryLine() {
        assertEquals("> hi", htmlToMarkdown("<blockquote><p>hi</p></blockquote>"))
        assertEquals("> a\n> b", htmlToMarkdown("<blockquote><p>a</p><p>b</p></blockquote>"))
    }

    @Test
    fun preKeepsPlainText() {
        assertEquals("a\n  b", htmlToMarkdown("<pre>a\n  b</pre>"))
    }

    @Test
    fun entitiesAreDecoded() {
        assertEquals("a & b <c> d", htmlToMarkdown("<p>a &amp; b &lt;c&gt; &nbsp;d</p>"))
        assertEquals("AB", htmlToMarkdown("<p>&#65;&#x42;</p>"))
        assertEquals("&unknown;", htmlToMarkdown("<p>&unknown;</p>"))
    }

    @Test
    fun scriptsAndStylesAreDiscarded() {
        assertEquals(
            "a\n\nb",
            htmlToMarkdown("<p>a</p><script>alert(1)</script><style>x{}</style><p>b</p>"),
        )
    }

    @Test
    fun linksKeepTextImagesAreDropped() {
        assertEquals("see X", htmlToMarkdown("<p>see <a href=\"https://x\">X</a></p>"))
        assertEquals("ab", htmlToMarkdown("<p>a<img src=\"x\">b</p>"))
    }

    @Test
    fun whitespaceIsCollapsedAndBlankRunsTrimmed() {
        assertEquals("a b", htmlToMarkdown("<p>a\n   b</p>"))
        assertEquals("a\n\nb", htmlToMarkdown("<p>a</p><div></div><p>b</p>"))
    }

    @Test
    fun malformedMarkupIsTolerated() {
        assertEquals("a**b**", htmlToMarkdown("<p>a<strong>b</p>"))
        assertEquals("", htmlToMarkdown("<p></p>"))
    }

    @Test
    fun tableRowsBecomePlainLines() {
        assertEquals(
            "a | b\nc | d",
            htmlToMarkdown(
                "<table><tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></table>",
            ),
        )
    }

    @Test
    fun horizontalRuleSeparatesParagraphs() {
        assertEquals("a\n\nb", htmlToMarkdown("<p>a</p><hr><p>b</p>"))
    }

    @Test
    fun commentsAndDoctypeAreIgnored() {
        assertEquals("a", htmlToMarkdown("<!doctype html><!-- note --><p>a</p>"))
    }

    @Test
    fun realisticDocument() {
        val html = """
            <h1>Install</h1>
            <p>Run <code>gradlew</code> and you are <strong>done</strong>.</p>
            <ul>
              <li>First</li>
              <li>Second</li>
            </ul>
            <blockquote>A note</blockquote>
        """.trimIndent()
        assertEquals(
            "# Install\n\nRun `gradlew` and you are **done**.\n\n- First\n- Second\n\n> A note",
            htmlToMarkdown(html),
        )
    }

    @Test
    fun outputReusesTheSharedDialect() {
        // Round-trip sanity: whatever we emit should be recognised by the live
        // editor's highlighter, so pasted content styles immediately.
        val markdown = htmlToMarkdown("<p><strong>bold</strong> and <mark>hi</mark></p>")
        assertEquals("**bold** and ==hi==", markdown)
        val kinds = parseMarkdownSourceStyles(markdown).map { it.kind }.toSet()
        assertEquals(
            setOf(
                MarkdownSourceStyleKind.Bold,
                MarkdownSourceStyleKind.Highlight,
            ),
            kinds,
        )
    }
}
