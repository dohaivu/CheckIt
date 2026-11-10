package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkdownBlocksTest {

    @Test
    fun emptyInputProducesNoBlocks() {
        assertTrue(parseMarkdownBlocks("").isEmpty())
    }

    @Test
    fun plainTextIsOneVerbatimBlock() {
        val input = "hello\n\nworld\n"
        val blocks = parseMarkdownBlocks(input)
        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlock.Text(input), blocks.single())
    }

    @Test
    fun tableWithOuterPipes() {
        val input = """
            | Name | Qty |
            | --- | --- |
            | A | 2 |
            | B | 3 |
        """.trimIndent()

        val table = parseMarkdownBlocks(input).single()
        assertIs<MarkdownBlock.Table>(table)
        assertEquals(listOf("Name", "Qty"), table.header)
        assertEquals(listOf(MarkdownTableAlignment.Start, MarkdownTableAlignment.Start), table.alignments)
        assertEquals(listOf(listOf("A", "2"), listOf("B", "3")), table.rows)
    }

    @Test
    fun tableWithoutOuterPipes() {
        val input = "Name | Qty\n--- | ---\nA | 2"

        val table = parseMarkdownBlocks(input).single()
        assertIs<MarkdownBlock.Table>(table)
        assertEquals(listOf("Name", "Qty"), table.header)
        assertEquals(listOf(listOf("A", "2")), table.rows)
    }

    @Test
    fun alignmentsComeFromDelimiterRow() {
        val input = """
            | L | C | R | D |
            | :--- | :---: | ---: | --- |
            | a | b | c | d |
        """.trimIndent()

        val table = parseMarkdownBlocks(input).single()
        assertIs<MarkdownBlock.Table>(table)
        assertEquals(
            listOf(
                MarkdownTableAlignment.Start,
                MarkdownTableAlignment.Center,
                MarkdownTableAlignment.End,
                MarkdownTableAlignment.Start,
            ),
            table.alignments
        )
    }

    @Test
    fun escapedPipeStaysInCell() {
        val input = """
            | a \| b | c |
            | --- | --- |
            | 1 | 2 |
        """.trimIndent()

        val table = parseMarkdownBlocks(input).single()
        assertIs<MarkdownBlock.Table>(table)
        assertEquals(listOf("a | b", "c"), table.header)
    }

    @Test
    fun pipeInsideCodeSpanDoesNotSplit() {
        val input = """
            | `a|b` | c |
            | --- | --- |
            | 1 | 2 |
        """.trimIndent()

        val table = parseMarkdownBlocks(input).single()
        assertIs<MarkdownBlock.Table>(table)
        assertEquals(listOf("`a|b`", "c"), table.header)
    }

    @Test
    fun raggedRowsArePaddedAndTruncated() {
        val input = """
            | A | B | C |
            | --- | --- | --- |
            | 1 |
            | 1 | 2 | 3 | 4 |
        """.trimIndent()

        val table = parseMarkdownBlocks(input).single()
        assertIs<MarkdownBlock.Table>(table)
        assertEquals(listOf(listOf("1", "", ""), listOf("1", "2", "3")), table.rows)
    }

    @Test
    fun singleColumnTableNeedsPipesInDelimiter() {
        // `---` alone also means a horizontal rule / setext underline, so it is
        // deliberately not treated as a table delimiter.
        val input = "| Title |\n---"
        val blocks = parseMarkdownBlocks(input)
        assertEquals(listOf(MarkdownBlock.Text(input)), blocks)
    }

    @Test
    fun singleColumnTableWithPipesIsParsed() {
        val input = "| Title |\n| --- |\n| a |"
        val table = parseMarkdownBlocks(input).single()
        assertIs<MarkdownBlock.Table>(table)
        assertEquals(listOf("Title"), table.header)
        assertEquals(listOf(listOf("a")), table.rows)
    }

    @Test
    fun mismatchedDelimiterColumnCountIsNotATable() {
        val input = "| A | B |\n| --- |\n| 1 | 2 |"
        assertEquals(listOf(MarkdownBlock.Text(input)), parseMarkdownBlocks(input))
    }

    @Test
    fun blankLineEndsTable() {
        val input = "| A | B |\n| --- | --- |\n| 1 | 2 |\n\nafter"
        val blocks = parseMarkdownBlocks(input)

        assertEquals(2, blocks.size)
        val table = blocks[0]
        assertIs<MarkdownBlock.Table>(table)
        assertEquals(listOf(listOf("1", "2")), table.rows)
        assertEquals(MarkdownBlock.Text("\nafter"), blocks[1])
    }

    @Test
    fun nonTableLineEndsTable() {
        val input = "| A | B |\n| --- | --- |\n| 1 | 2 |\nplain"
        val blocks = parseMarkdownBlocks(input)

        assertEquals(2, blocks.size)
        assertIs<MarkdownBlock.Table>(blocks[0])
        assertEquals(MarkdownBlock.Text("plain"), blocks[1])
    }

    @Test
    fun textBeforeAndAfterTableIsPreserved() {
        val input = "para\n| A | B |\n| --- | --- |\n| 1 | 2 |\ntail"
        val blocks = parseMarkdownBlocks(input)

        assertEquals(3, blocks.size)
        assertEquals(MarkdownBlock.Text("para"), blocks[0])
        assertIs<MarkdownBlock.Table>(blocks[1])
        assertEquals(MarkdownBlock.Text("tail"), blocks[2])
    }

    @Test
    fun tableAtDocumentStartHasNoEmptyTextBlock() {
        val input = "| A | B |\n| --- | --- |\n| 1 | 2 |"
        val blocks = parseMarkdownBlocks(input)
        assertEquals(1, blocks.size)
        assertIs<MarkdownBlock.Table>(blocks.single())
    }

    @Test
    fun pipeLineFollowedByDelimiterRowIsATable() {
        // Documents GFM behaviour: a pipe-containing prose line is a header row.
        val input = "a | b\n--- | ---\n1 | 2"
        val table = parseMarkdownBlocks(input).single()
        assertIs<MarkdownBlock.Table>(table)
        assertEquals(listOf("a", "b"), table.header)
    }

    @Test
    fun crlfRowsAreParsed() {
        val input = "| a | b |\r\n| --- | --- |\r\n| 1 | 2 |\r\n"
        val table = parseMarkdownBlocks(input).first()
        assertIs<MarkdownBlock.Table>(table)
        assertEquals(listOf("a", "b"), table.header)
        assertEquals(listOf(listOf("1", "2")), table.rows)
    }

    @Test
    fun tooManyColumnsFallsBackToText() {
        val header = (1..13).joinToString("|", prefix = "|", postfix = "|") { "h$it" }
        val delimiter = (1..13).joinToString("|", prefix = "|", postfix = "|") { "---" }
        val input = "$header\n$delimiter\n| 1 | 2 |"

        assertEquals(listOf(MarkdownBlock.Text(input)), parseMarkdownBlocks(input))
    }

    @Test
    fun tooManyRowsFallsBackToText() {
        val header = "| A | B |"
        val delimiter = "| --- | --- |"
        val body = (1..201).joinToString("\n") { "| $it | x |" }
        val input = "$header\n$delimiter\n$body"

        assertEquals(listOf(MarkdownBlock.Text(input)), parseMarkdownBlocks(input))
    }
}
