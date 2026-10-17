package com.checkit.domain

/**
 * Converts pasted HTML (rich clipboard content) into the markdown dialect this
 * app understands: `**bold**`, `_italic_`, `~~strike~~`, `==highlight==`,
 * `` `code` ``, `# ` headings, `- `/`1. ` lists and `> ` quotes.
 *
 * The dialect is deliberately small (see [parseRichText] and
 * [parseMarkdownSourceStyles]), so constructs without a representation are
 * flattened to plain text: links keep their text, images are dropped, code
 * fences/tables become plain lines and nested list levels collapse to one
 * level.
 *
 * This lives in shared code so the macOS editor (pasting rich clipboard
 * content) uses the same conversion Android can, and so the logic is unit
 * testable without a platform UI.
 */
fun htmlToMarkdown(html: String): String {
    if (html.isBlank()) return ""
    val root = HtmlParser(html).parse()
    return normalizeMarkdown(renderHtmlNode(root))
}

// region DOM

private sealed interface HtmlNode {
    class Element(val tag: String, val children: MutableList<HtmlNode> = mutableListOf()) : HtmlNode
    class Text(val value: String) : HtmlNode
}

private val VOID_TAGS = setOf(
    "area", "base", "br", "col", "embed", "hr", "img", "input",
    "link", "meta", "param", "source", "track", "wbr",
)

/** Elements whose text content is not prose and must be discarded. */
private val RAW_TEXT_TAGS = setOf("script", "style")

// endregion

// region Parser

/**
 * Minimal, dependency-free HTML tokenizer. It only needs to be good enough to
 * recover structure from typical clipboard HTML (browsers, editors, rendered
 * markdown); it tolerates unclosed/unmatched tags rather than failing.
 */
private class HtmlParser(private val input: String) {
    private val root = HtmlNode.Element("#root")
    private val stack = mutableListOf(root)
    private val text = StringBuilder()
    private var pos = 0

    fun parse(): HtmlNode.Element {
        while (pos < input.length) {
            val ch = input[pos]
            if (ch != '<') {
                text.append(ch)
                pos++
                continue
            }
            if (input.startsWith("<!--", pos)) {
                skipComment()
                continue
            }
            // Doctype / processing instruction: skip to the closing '>'.
            if (pos + 1 < input.length && (input[pos + 1] == '!' || input[pos + 1] == '?')) {
                skipTo('>')
                continue
            }
            val end = input.indexOf('>', pos)
            if (end == -1) { // stray '<' with no tag end; keep it as text.
                text.append(ch)
                pos++
                continue
            }
            val raw = input.substring(pos + 1, end)
            pos = end + 1
            handleTag(raw)
        }
        flushText()
        return root
    }

    private fun handleTag(rawTag: String) {
        val raw = rawTag.trim()
        if (raw.isEmpty()) return
        if (raw.startsWith("/")) {
            flushText()
            closeTag(tagName(raw.substring(1)))
            return
        }
        val selfClosing = raw.endsWith("/")
        val name = tagName(raw)
        if (name.isEmpty()) return
        flushText()
        val element = HtmlNode.Element(name)
        stack.last().children.add(element)
        if (selfClosing || name in VOID_TAGS) return
        if (name in RAW_TEXT_TAGS) {
            skipRawText(name)
            return
        }
        stack.add(element)
    }

    /** Pops to the nearest open ancestor with [name], ignoring unmatched closers. */
    private fun closeTag(name: String) {
        if (name.isEmpty()) return
        var index = stack.lastIndex
        while (index > 0 && stack[index].tag != name) index--
        if (index <= 0) return
        while (stack.lastIndex >= index) stack.removeAt(stack.lastIndex)
    }

    private fun skipRawText(name: String) {
        val close = input.indexOf("</$name", pos, ignoreCase = true)
        pos = if (close == -1) {
            input.length
        } else {
            val end = input.indexOf('>', close)
            if (end == -1) input.length else end + 1
        }
        // The element was already attached; drop it (its content is not prose).
        stack.last().children.removeAt(stack.last().children.lastIndex)
    }

    private fun flushText() {
        if (text.isEmpty()) return
        stack.last().children.add(HtmlNode.Text(text.toString()))
        text.clear()
    }

    private fun skipComment() {
        val end = input.indexOf("-->", pos + 4)
        pos = if (end == -1) input.length else end + 3
    }

    private fun skipTo(target: Char) {
        val end = input.indexOf(target, pos)
        pos = if (end == -1) input.length else end + 1
    }

    private fun tagName(s: String): String {
        val b = StringBuilder()
        for (c in s) {
            if (c.isLetterOrDigit() || c == '-' || c == '_' || c == ':') b.append(c) else break
        }
        return b.toString().lowercase()
    }
}

// endregion

// region Renderer

/** Tags that own a block of their own (`\n\n`-separated) rather than inline flow. */
private val BLOCK_TAGS = setOf(
    "p", "div", "section", "article", "header", "footer", "main", "aside",
    "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "blockquote", "pre", "hr",
    "table", "thead", "tbody", "tfoot", "tr", "figure", "figcaption",
    "dl", "dt", "dd", "form", "fieldset", "address", "nav", "details", "summary",
)

private fun renderHtmlNode(node: HtmlNode): String = when (node) {
    is HtmlNode.Text -> collapseWhitespace(decodeHtmlEntities(node.value))
    is HtmlNode.Element -> renderElement(node)
}

/**
 * Block containers that hold other blocks, where whitespace-only text (the
 * newlines/indentation of formatted HTML) is insignificant and must be dropped.
 * Leaf blocks like `p`/`li`/`h1` are excluded: there a space between two inline
 * elements is meaningful.
 */
private val CONTAINER_TAGS = setOf(
    "#root", "html", "body", "div", "section", "article", "header", "footer",
    "main", "aside", "ul", "ol", "blockquote", "table", "thead", "tbody",
    "tfoot", "tr", "form", "fieldset", "nav", "details", "figure", "dl",
)

private fun renderChildren(element: HtmlNode.Element): String {
    val dropWhitespaceOnly = element.tag in CONTAINER_TAGS
    return element.children.joinToString("") { child ->
        if (dropWhitespaceOnly && child is HtmlNode.Text && child.value.isBlank()) "" else renderHtmlNode(child)
    }
}

private fun renderElement(element: HtmlNode.Element): String = when (element.tag) {
    "strong", "b" -> wrapMarkdown("**", element)
    "em", "i" -> wrapMarkdown("_", element)
    "del", "s", "strike" -> wrapMarkdown("~~", element)
    "mark" -> wrapMarkdown("==", element)
    "code" -> "`" + collapseWhitespace(decodeHtmlEntities(plainText(element))).trim() + "`"
    "a", "span", "font", "small", "sub", "sup", "u", "abbr", "cite", "q", "kbd", "samp", "var", "time", "label" ->
        renderChildren(element)
    "img", "input", "meta", "link", "source", "track", "wbr", "area", "base", "col", "param", "embed" -> ""
    "br", "hr" -> "\n"
    "h1" -> heading(1, element)
    "h2" -> heading(2, element)
    "h3", "h4", "h5", "h6" -> heading(3, element)
    "p" -> renderChildren(element).trim() + "\n\n"
    "ul" -> renderList(element, ordered = false)
    "ol" -> renderList(element, ordered = true)
    "li" -> renderChildren(element).trim() + "\n"
    "blockquote" -> renderBlockquote(element)
    "pre" -> plainText(element).trim('\n') + "\n\n"
    "table" -> renderTable(element)
    else -> if (element.tag in BLOCK_TAGS) renderChildren(element) + "\n\n" else renderChildren(element)
}

private fun wrapMarkdown(marker: String, element: HtmlNode.Element): String {
    val inner = renderChildren(element)
    return if (inner.isBlank()) "" else "$marker$inner$marker"
}

private fun heading(level: Int, element: HtmlNode.Element): String {
    val text = renderChildren(element).trim()
    return if (text.isEmpty()) "" else "#".repeat(level) + " " + text + "\n\n"
}

private fun renderBlockquote(element: HtmlNode.Element): String {
    val lines = renderChildren(element).split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return ""
    return lines.joinToString("\n") { "> $it" } + "\n\n"
}

private fun renderList(element: HtmlNode.Element, ordered: Boolean): String {
    val sb = StringBuilder()
    var index = 1
    for (child in element.children) {
        if (child !is HtmlNode.Element || child.tag != "li") continue
        val prefix = if (ordered) "${index}. " else "- "
        sb.append(prefix).append(renderListItemInline(child)).append('\n')
        // Nested lists are flattened to top-level markers: the shared parser
        // only matches unindented `- `/`1. ` at the start of a line.
        sb.append(renderNestedLists(child))
        index++
    }
    sb.append('\n')
    return sb.toString()
}

private fun renderListItemInline(li: HtmlNode.Element): String {
    val sb = StringBuilder()
    for (child in li.children) {
        if (child is HtmlNode.Element && (child.tag == "ul" || child.tag == "ol")) continue
        sb.append(renderHtmlNode(child))
    }
    // Flatten any internal line breaks so one `li` stays one markdown line.
    return WHITESPACE.replace(sb.toString(), " ").trim()
}

private fun renderNestedLists(li: HtmlNode.Element): String {
    val sb = StringBuilder()
    for (child in li.children) {
        if (child is HtmlNode.Element && child.tag == "ul") sb.append(renderList(child, ordered = false))
        else if (child is HtmlNode.Element && child.tag == "ol") sb.append(renderList(child, ordered = true))
    }
    return sb.toString()
}

private fun renderTable(element: HtmlNode.Element): String {
    val rows = mutableListOf<List<String>>()
    collectRows(element, rows)
    if (rows.isEmpty()) return ""
    return rows.joinToString("\n") { it.joinToString(" | ") } + "\n\n"
}

private fun collectRows(element: HtmlNode.Element, out: MutableList<List<String>>) {
    for (child in element.children) {
        if (child !is HtmlNode.Element) continue
        if (child.tag == "tr") {
            val cells = child.children
                .filterIsInstance<HtmlNode.Element>()
                .filter { it.tag == "td" || it.tag == "th" }
                .map { WHITESPACE.replace(renderChildren(it), " ").trim() }
            out.add(cells)
        } else {
            collectRows(child, out)
        }
    }
}

private fun plainText(node: HtmlNode): String = when (node) {
    is HtmlNode.Text -> decodeHtmlEntities(node.value)
    is HtmlNode.Element -> node.children.joinToString("") { plainText(it) }
}

// endregion

// region Text utilities

private val WHITESPACE = Regex("\\s+")

private fun collapseWhitespace(value: String): String = WHITESPACE.replace(value, " ")

/** Malformed markup is tolerated, so this stays deliberately small. */
private val NAMED_ENTITIES = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
    "nbsp" to " ", "ndash" to "\u2013", "mdash" to "\u2014", "hellip" to "\u2026",
    "lsquo" to "\u2018", "rsquo" to "\u2019", "ldquo" to "\u201C", "rdquo" to "\u201D",
    "copy" to "\u00A9", "reg" to "\u00AE", "trade" to "\u2122", "deg" to "\u00B0",
    "times" to "\u00D7", "divide" to "\u00F7", "middot" to "\u00B7", "bull" to "\u2022",
    "eacute" to "\u00E9", "egrave" to "\u00E8", "agrave" to "\u00E0", "ccedil" to "\u00E7",
    "uuml" to "\u00FC", "ouml" to "\u00F6", "auml" to "\u00E4", "szlig" to "\u00DF",
)

private val ENTITY_PATTERN = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);")

private fun decodeHtmlEntities(input: String): String {
    if ('&' !in input) return input
    return ENTITY_PATTERN.replace(input) { match ->
        val token = match.groupValues[1]
        when {
            token.startsWith("#x", ignoreCase = true) ->
                token.drop(2).toIntOrNull(16)?.let(::codePointToString) ?: match.value
            token.startsWith("#") ->
                token.drop(1).toIntOrNull()?.let(::codePointToString) ?: match.value
            else -> NAMED_ENTITIES[token.lowercase()] ?: match.value
        }
    }
}

private fun codePointToString(codePoint: Int): String = when {
    codePoint <= 0 -> ""
    codePoint <= 0xFFFF -> codePoint.toChar().toString()
    codePoint <= 0x10FFFF -> {
        val v = codePoint - 0x10000
        val high = (0xD800 + (v shr 10)).toChar()
        val low = (0xDC00 + (v and 0x3FF)).toChar()
        "$high$low"
    }
    else -> ""
}

/** Trims trailing spaces, collapses blank-line runs and drops outer newlines. */
private fun normalizeMarkdown(raw: String): String {
    val result = StringBuilder()
    var blankRun = 0
    for (line in raw.split('\n')) {
        val trimmed = line.trimEnd()
        if (trimmed.isBlank()) {
            blankRun++
            if (blankRun > 1) continue
            result.append('\n')
        } else {
            blankRun = 0
            result.append(trimmed).append('\n')
        }
    }
    return result.toString().trim('\n')
}

// endregion
