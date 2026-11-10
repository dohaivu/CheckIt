package com.checkit.domain

/** Horizontal alignment of a GFM table column, derived from the delimiter row. */
enum class MarkdownTableAlignment {
    Start,
    Center,
    End,
}

/**
 * A block-level markdown element. [parseMarkdownBlocks] splits a document into
 * blocks so the UI can render tables with real layout while keeping
 * [Text] blocks on the existing [parseRichText]-based renderer.
 *
 * [Text.markdown] is the verbatim source of the block (blank lines included),
 * so line-based constructs and spacing behave exactly as before.
 */
sealed interface MarkdownBlock {
    /** Prose lines, rendered by the existing full-markdown renderer. */
    data class Text(val markdown: String) : MarkdownBlock

    /** A GFM pipe table. Cells keep their inline markdown, markers included. */
    data class Table(
        val header: List<String>,
        val alignments: List<MarkdownTableAlignment>,
        val rows: List<List<String>>,
    ) : MarkdownBlock
}

/**
 * Hostile-input guards: pasted tables can be arbitrarily large, and every cell
 * becomes a laid-out `Text` node. Beyond these limits the table degrades to a
 * plain [MarkdownBlock.Text] block, which is exactly today's rendering.
 */
private const val MaxTableColumns = 12
private const val MaxTableRows = 200

private val DelimiterCell = Regex("^:?-+:?$")

/**
 * Splits [markdown] into text and table blocks.
 *
 * Only the GFM subset the app needs is supported: a header row plus delimiter
 * row (`---`, `:---`, `:---:`, `---:`), followed by non-blank rows. Escaped
 * pipes (`\|`) and pipes inside code spans stay inside the cell. Ragged rows
 * are padded (or truncated) to the header width.
 *
 * Deliberate limitations, kept simple on purpose:
 * - tables inside code fences are not detected (the renderer has no fenced
 *   code support either);
 * - a single-column table still needs a `|` in the delimiter row, so prose
 *   followed by a `---` rule is never mistaken for a table;
 * - `\\|` is treated as `\|` (escaped pipe), not as an escaped backslash.
 *
 * Pure and platform-neutral so the Apple renderer can share it.
 */
fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    if (markdown.isEmpty()) return emptyList()
    // Fast path: no pipe, no table. Keeps the common case at a single scan.
    if (markdown.indexOf('|') < 0) return listOf(MarkdownBlock.Text(markdown))

    val lines = markdown.split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    val textLines = mutableListOf<String>()
    var index = 0

    fun flushText() {
        if (textLines.isEmpty()) return
        blocks.add(MarkdownBlock.Text(textLines.joinToString("\n")))
        textLines.clear()
    }

    while (index < lines.size) {
        val table = parseTableAt(lines, index)
        if (table == null) {
            textLines.add(lines[index])
            index++
            continue
        }
        flushText()
        blocks.add(table.block)
        index = table.endExclusive
    }
    flushText()
    return blocks
}

private class ParsedTable(val block: MarkdownBlock.Table, val endExclusive: Int)

private fun parseTableAt(lines: List<String>, start: Int): ParsedTable? {
    if (start + 1 >= lines.size) return null
    val headerLine = lines[start]
    if (headerLine.indexOf('|') < 0) return null
    if (!isDelimiterRow(lines[start + 1])) return null

    val header = splitTableRow(headerLine)
    if (header.isEmpty() || header.size > MaxTableColumns) return null
    val alignments = parseAlignments(lines[start + 1])
    if (header.size != alignments.size) return null

    val rows = mutableListOf<List<String>>()
    var index = start + 2
    while (index < lines.size) {
        val line = lines[index]
        if (line.isBlank() || line.indexOf('|') < 0) break
        if (rows.size >= MaxTableRows) return null
        rows.add(normalizeCells(splitTableRow(line), header.size))
        index++
    }

    return ParsedTable(
        block = MarkdownBlock.Table(
            header = normalizeCells(header, header.size),
            alignments = alignments,
            rows = rows,
        ),
        endExclusive = index,
    )
}

private fun isDelimiterRow(line: String): Boolean {
    // Requiring a pipe keeps `prose` + `---` (rule / setext) out of table land.
    if (line.indexOf('|') < 0) return false
    val cells = splitTableRow(line)
    return cells.isNotEmpty() && cells.all { DelimiterCell.matches(it) }
}

private fun parseAlignments(line: String): List<MarkdownTableAlignment> =
    splitTableRow(line).map { cell ->
        val leading = cell.startsWith(":")
        val trailing = cell.endsWith(":")
        when {
            leading && trailing -> MarkdownTableAlignment.Center
            trailing -> MarkdownTableAlignment.End
            else -> MarkdownTableAlignment.Start
        }
    }

private fun normalizeCells(cells: List<String>, columnCount: Int): List<String> {
    if (cells.size == columnCount) return cells
    return List(columnCount) { index -> cells.getOrElse(index) { "" } }
}

/** Splits one pipe-table line into trimmed cells, honoring `\|` and code spans. */
internal fun splitTableRow(line: String): List<String> {
    var start = 0
    var end = line.length
    while (start < end && line[start].isWhitespace()) start++
    while (end > start && line[end - 1].isWhitespace()) end--
    if (start < end && line[start] == '|') start++
    if (end > start && line[end - 1] == '|' && !isEscaped(line, end - 1)) end--
    if (start >= end) return emptyList()

    val cells = mutableListOf<String>()
    val cell = StringBuilder()
    var inCode = false
    var i = start
    while (i < end) {
        val char = line[i]
        if (char == '\\' && i + 1 < end && line[i + 1] == '|') {
            cell.append('|')
            i += 2
        } else if (char == '`') {
            inCode = !inCode
            cell.append(char)
            i++
        } else if (char == '|' && !inCode) {
            cells.add(cell.toString().trim())
            cell.clear()
            i++
        } else {
            cell.append(char)
            i++
        }
    }
    cells.add(cell.toString().trim())
    return cells
}

private fun isEscaped(line: String, index: Int): Boolean {
    var backslashes = 0
    var i = index - 1
    while (i >= 0 && line[i] == '\\') {
        backslashes++
        i--
    }
    return backslashes % 2 == 1
}
