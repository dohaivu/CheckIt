package com.checkit.ui.components

import com.checkit.domain.MarkdownBlock
import com.checkit.domain.MarkdownTableAlignment
import com.checkit.domain.parseMarkdownBlocks

/**
 * Flat, Swift-friendly projection of [parseMarkdownBlocks].
 *
 * The Kotlin sealed hierarchy exports to Objective-C as a protocol plus
 * subclasses, which is awkward to consume from SwiftUI. A single data class with
 * a [kind] discriminator keeps the macOS bridge a plain array. [alignments] uses
 * `"start"` / `"center"` / `"end"` for the same reason.
 */
data class MarkdownBlockDto(
    /** `"text"` or `"table"`. */
    val kind: String,
    /** Verbatim markdown for [kind] = `"text"`; empty for tables. */
    val text: String,
    val header: List<String>,
    val alignments: List<String>,
    val rows: List<List<String>>,
)

/** Block-level markdown access for the macOS renderer (parity with `MarkdownBody`). */
object MarkdownAppleBridge {
    fun parseBlocks(markdown: String): List<MarkdownBlockDto> =
        parseMarkdownBlocks(markdown).map { block ->
            when (block) {
                is MarkdownBlock.Text -> MarkdownBlockDto(
                    kind = "text",
                    text = block.markdown,
                    header = emptyList(),
                    alignments = emptyList(),
                    rows = emptyList(),
                )
                is MarkdownBlock.Table -> MarkdownBlockDto(
                    kind = "table",
                    text = "",
                    header = block.header,
                    alignments = block.alignments.map { it.toBridgeValue() },
                    rows = block.rows,
                )
            }
        }

    private fun MarkdownTableAlignment.toBridgeValue(): String = when (this) {
        MarkdownTableAlignment.Start -> "start"
        MarkdownTableAlignment.Center -> "center"
        MarkdownTableAlignment.End -> "end"
    }
}
