package com.checkit.domain.usecase

import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedTextStyle

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
        baseDepth: Int = 0
    ): String = nodes.toMarkdown(options, baseDepth)

    fun exportSubtree(
        node: NestedItemNode,
        options: NestedMarkdownExportOptions = NestedMarkdownExportOptions(),
        baseDepth: Int = 0
    ): String = node.toMarkdown(options, baseDepth)
}

fun NestedDocumentTree.toMarkdown(
    options: NestedMarkdownExportOptions = NestedMarkdownExportOptions()
): String {
    val builder = StringBuilder()
    if (options.includeDocumentTitle && document.title.isNotBlank()) {
        builder.appendLine("# ${document.title.trim()}")
        builder.appendLine()
    }
    builder.append(rootNodes.toMarkdown(options))
    return builder.toString().trimEnd()
}

fun List<NestedItemNode>.toMarkdown(
    options: NestedMarkdownExportOptions = NestedMarkdownExportOptions(),
    baseDepth: Int = 0
): String {
    val builder = StringBuilder()
    forEachIndexed { index, node ->
        if (index > 0) builder.appendLine()
        builder.append(node.toMarkdown(options, baseDepth))
    }
    return builder.toString().trimEnd()
}

fun NestedItemNode.toMarkdown(
    options: NestedMarkdownExportOptions = NestedMarkdownExportOptions(),
    depth: Int = 0
): String {
    val builder = StringBuilder()
    val prefix = options.indent.repeat(depth)
    val bullet = when {
        item.checkboxEnabled -> if (item.checked) "- [x] " else "- [ ] "
        else -> "- "
    }

    builder.append(prefix)
    builder.append(bullet)

    val rawText = item.text.trim().ifEmpty { "Untitled" }
    val formattedText = when (item.textStyle) {
        NestedTextStyle.Header -> "**$rawText**"
        NestedTextStyle.Subheader -> "*$rawText*"
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

    children.forEach { child ->
        builder.appendLine()
        builder.append(child.toMarkdown(options, depth + 1))
    }

    return builder.toString()
}
