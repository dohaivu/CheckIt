package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.MarkdownBlock
import com.checkit.domain.MarkdownTableAlignment
import com.checkit.domain.parseMarkdownBlocks

/** Cells wrap by default but are capped so one long cell cannot grow a table forever. */
internal const val DefaultTableCellMaxLines = 6

private const val MinColumnWeight = 4f
private const val MaxColumnWeight = 24f

/**
 * Multi-block markdown renderer for prose surfaces.
 *
 * [MarkdownBlock.Text] blocks go through [renderFullMarkdown] unchanged, so
 * everything the app already supports (headers, quotes, bullets, inline spans,
 * `=={cN}==` colors) renders exactly as before. [MarkdownBlock.Table] blocks get
 * real layout: wrapping cells, weighted columns, a header band, and row rules.
 * [MarkdownBlock.Code] blocks get a full-width monospace card (no syntax
 * highlighting) with preserved whitespace.
 *
 * Non-lazy `Column`: safe inside `verticalScroll` content and `LazyColumn`
 * items, and it does not measure children with intrinsics. Do not place a table
 * inside an `IntrinsicSize`-measured parent.
 *
 * Keep using [asMarkdownAnnotatedString] for single-line / `maxLines`-truncated
 * rows: those intentionally stay on the plain text path and show raw pipes.
 */
@Composable
fun MarkdownBody(
    markdown: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    highlightColor: Color = MarkdownHighlightColor,
    cellMaxLines: Int = DefaultTableCellMaxLines,
) {
    val source = markdown.orEmpty()
    if (source.isEmpty()) return
    val blocks = remember(source) { parseMarkdownBlocks(source) }

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Text -> {
                    if (block.markdown.isNotEmpty()) {
                        Text(
                            text = remember(block.markdown, highlightColor) {
                                renderFullMarkdown(block.markdown, highlightColor)
                            },
                            style = style,
                            color = color,
                        )
                    }
                }
                is MarkdownBlock.Table -> MarkdownTableBlock(
                    table = block,
                    style = style,
                    color = color,
                    highlightColor = highlightColor,
                    cellMaxLines = cellMaxLines,
                )
                is MarkdownBlock.Code -> MarkdownCodeBlock(
                    block = block,
                    style = style,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun MarkdownTableBlock(
    table: MarkdownBlock.Table,
    style: TextStyle,
    color: Color,
    highlightColor: Color,
    cellMaxLines: Int,
) {
    val weights = remember(table) { table.columnWeights() }
    val shape = RoundedCornerShape(6.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
    ) {
        MarkdownTableRow(
            cells = table.header,
            alignments = table.alignments,
            weights = weights,
            style = style,
            color = color,
            highlightColor = highlightColor,
            maxLines = cellMaxLines,
            isHeader = true,
        )
        AppHorizontalDivider()
        table.rows.forEachIndexed { index, row ->
            MarkdownTableRow(
                cells = row,
                alignments = table.alignments,
                weights = weights,
                style = style,
                color = color,
                highlightColor = highlightColor,
                maxLines = cellMaxLines,
                isHeader = false,
            )
            if (index < table.rows.lastIndex) AppHorizontalDivider()
        }
    }
}

@Composable
private fun MarkdownTableRow(
    cells: List<String>,
    alignments: List<MarkdownTableAlignment>,
    weights: List<Float>,
    style: TextStyle,
    color: Color,
    highlightColor: Color,
    maxLines: Int,
    isHeader: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHeader) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                } else {
                    Modifier
                }
            )
    ) {
        cells.forEachIndexed { index, cell ->
            Text(
                // Cells are inline-only: a cell starting with `#` or `-` must not
                // become a header or a bullet.
                text = remember(cell, highlightColor) { renderInlineMarkdown(cell, highlightColor) },
                modifier = Modifier
                    .weight(weights.getOrElse(index) { MinColumnWeight })
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                style = style,
                color = color,
                fontWeight = if (isHeader) FontWeight.SemiBold else null,
                textAlign = alignments.getOrElse(index) { MarkdownTableAlignment.Start }.toTextAlign(),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun MarkdownTableAlignment.toTextAlign(): TextAlign = when (this) {
    MarkdownTableAlignment.Start -> TextAlign.Start
    MarkdownTableAlignment.Center -> TextAlign.Center
    MarkdownTableAlignment.End -> TextAlign.End
}

/**
 * Full-width code card: monospace, preserved whitespace, optional language
 * caption. No syntax highlighting; long lines wrap (v1 choice) rather than
 * scrolling, so the block stays safe inside intrinsic-measured parents.
 */
@Composable
private fun MarkdownCodeBlock(
    block: MarkdownBlock.Code,
    style: TextStyle,
    color: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        if (block.language.isNotBlank()) {
            Text(
                text = block.language,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(
            text = block.code,
            style = style.copy(fontFamily = FontFamily.Monospace),
            color = color,
        )
    }
}

/**
 * Cheap column sizing: weight by the longest cell text, clamped. Avoids
 * measuring cells (intrinsic passes) while still giving narrow columns less
 * room than wide ones.
 */
private fun MarkdownBlock.Table.columnWeights(): List<Float> {
    val widths = IntArray(header.size)
    header.forEachIndexed { index, cell -> widths[index] = cell.length }
    rows.forEach { row ->
        row.forEachIndexed { index, cell ->
            if (index < widths.size) widths[index] = maxOf(widths[index], cell.length)
        }
    }
    return widths.map { it.coerceIn(MinColumnWeight.toInt(), MaxColumnWeight.toInt()).toFloat() }
}
