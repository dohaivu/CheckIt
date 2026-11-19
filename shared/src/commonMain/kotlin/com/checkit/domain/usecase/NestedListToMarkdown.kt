package com.checkit.domain.usecase

import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.NestedNumberingStyle
import com.checkit.domain.formatNestedNumber

data class NestedMarkdownExportOptions(
    val indent: String = "  ",
    val includeDocumentTitle: Boolean = false,
    val includeNotes: Boolean = true,
    val includeTags: Boolean = true
)

class ExportNestedListToMarkdownUseCase {
    operator fun invoke(
        tree: NestedDocumentTree,
        options: NestedMarkdownExportOptions = NestedMarkdownExportOptions()
    ): String = tree.toMarkdown(options)

    fun exportNodes(
        nodes: List<NestedItemNode>,
        options: NestedMarkdownExportOptions = NestedMarkdownExportOptions(),
        baseDepth: Int = 0,
        numberingStyle: NestedNumberingStyle = NestedNumberingStyle.None
    ): String = nodes.toMarkdown(options, baseDepth, numberingStyle)

    fun exportSubtree(
        node: NestedItemNode,
        options: NestedMarkdownExportOptions = NestedMarkdownExportOptions(),
        baseDepth: Int = 0,
        numberingStyle: NestedNumberingStyle = NestedNumberingStyle.None
    ): String = node.toMarkdown(options, baseDepth, numberingStyle)
}

fun NestedDocumentTree.toMarkdown(
    options: NestedMarkdownExportOptions = NestedMarkdownExportOptions()
): String {
    val builder = StringBuilder()
    if (options.includeDocumentTitle && document.title.isNotBlank()) {
        builder.appendLine("# ${document.title.trim()}")
        builder.appendLine()
    }
    builder.append(rootNodes.toMarkdown(options, numberingStyle = document.numberingStyle))
    return builder.toString().trimEnd()
}

fun List<NestedItemNode>.toMarkdown(
    options: NestedMarkdownExportOptions = NestedMarkdownExportOptions(),
    baseDepth: Int = 0,
    numberingStyle: NestedNumberingStyle = NestedNumberingStyle.None
): String {
    val builder = StringBuilder()
    forEachIndexed { index, node ->
        if (index > 0) builder.appendLine()
        builder.append(node.toMarkdown(options, baseDepth, numberingStyle))
    }
    return builder.toString().trimEnd()
}

/**
 * Renders one node and its subtree.
 */
fun NestedItemNode.toMarkdown(
    options: NestedMarkdownExportOptions = NestedMarkdownExportOptions(),
    depth: Int = 0,
    numberingStyle: NestedNumberingStyle = NestedNumberingStyle.None,
    ancestorOrdinals: List<Int> = emptyList()
): String {
    val builder = StringBuilder()
    val prefix = options.indent.repeat(depth)
    val numberStr = formatNestedNumber(numberingStyle, ordinal, ancestorOrdinals)
    val bullet = when {
        item.checkboxEnabled -> {
            val box = if (item.checked) "[x]" else "[ ]"
            if (numberStr != null) "${numberStr}. $box " else "- $box "
        }
        numberStr != null -> "${numberStr}. "
        else -> "- "
    }

    builder.append(prefix)
    builder.append(bullet)

    val rawText = item.text.trim().ifEmpty { "Untitled" }
    val formattedText = when (item.textStyle) {
        NestedTextStyle.Title -> "***$rawText***"
        NestedTextStyle.Heading -> "**$rawText**"
        NestedTextStyle.Subheading -> "*$rawText*"
        NestedTextStyle.Monostyled -> "`$rawText`"
        NestedTextStyle.Body -> rawText
    }
    builder.append(formattedText)

    if (options.includeTags && item.tags.isNotEmpty()) {
        val tagSuffix = item.tags.joinToString(" ") { tag ->
            val cleanName = tag.name.trim().replace("\\s+".toRegex(), "_")
            if (cleanName.startsWith("#")) cleanName else "#$cleanName"
        }
        if (tagSuffix.isNotBlank()) {
            builder.append(" ")
            builder.append(tagSuffix)
        }
    }

    if (options.includeNotes && !item.note.isNullOrBlank()) {
        val notePrefix = options.indent.repeat(depth + 1)
        item.note.lineSequence().forEach { line ->
            builder.appendLine()
            builder.append(notePrefix)
            builder.append(line)
        }
    }

    val currentPath = ancestorOrdinals + ordinal
    children.forEach { child ->
        builder.appendLine()
        builder.append(child.toMarkdown(options, depth + 1, item.numberingStyle, currentPath))
    }

    return builder.toString()
}
