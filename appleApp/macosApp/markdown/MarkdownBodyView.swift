//
//  MarkdownBodyView.swift
//  macosApp — SwiftUI counterpart of the shared `MarkdownBody` composable.
//

import SwiftUI
import Shared

/// Multi-block markdown renderer for prose surfaces, mirroring the shared
/// `MarkdownBody` in MarkdownHelper.kt / MarkdownBody.kt:
/// - text blocks go through `renderFullMarkdown`, so headers, quotes, bullets
///   and inline spans render exactly as before;
/// - GFM pipe tables get real layout: weighted columns, a header band, row
///   rules, per-column alignment and capped cell wrapping.
///
/// Blocks come from the shared parser (`MarkdownAppleBridge`), so Android and
/// macOS agree on what a table is. Use `renderInlineMarkdown` for single-line
/// rows: those intentionally keep raw pipes.
struct MarkdownBodyView: View {
    let markdown: String
    var font: Font = .body
    var highlight: Color = Color.orange
    var cellLineLimit: Int = 6

    var body: some View {
        // Parsed once per distinct string, like Android's remember(markdown).
        let blocks = MarkdownBlocksCache.shared.blocks(for: markdown)
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                if block.kind == "table" {
                    MarkdownTableView(
                        block: block,
                        font: font,
                        highlight: highlight,
                        cellLineLimit: cellLineLimit
                    )
                } else if block.kind == "code" {
                    MarkdownCodeBlockView(code: block.text, language: block.language)
                } else {
                    Text(renderFullMarkdown(block.text, highlight: highlight))
                        .font(font)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }
}

/// Bounded cache kept on the Swift side: SwiftUI rebuilds view values on every
/// parent evaluation, and parsing a long note per pass would be wasteful.
private final class MarkdownBlocksCache: @unchecked Sendable {
    static let shared = MarkdownBlocksCache()

    private var cache = [String: [MarkdownBlockDto]]()
    private let lock = NSLock()
    private let maxEntries = 200

    func blocks(for markdown: String) -> [MarkdownBlockDto] {
        lock.lock()
        if let cached = cache[markdown] {
            lock.unlock()
            return cached
        }
        lock.unlock()

        let parsed = MarkdownAppleBridge.shared.parseBlocks(markdown: markdown)

        lock.lock()
        if cache.count >= maxEntries {
            cache.removeAll(keepingCapacity: true)
        }
        cache[markdown] = parsed
        lock.unlock()
        return parsed
    }
}

private struct MarkdownTableView: View {
    let block: MarkdownBlockDto
    var font: Font
    var highlight: Color
    var cellLineLimit: Int

    private var weights: [CGFloat] {
        MarkdownTableWeights.weights(header: block.header, rows: block.rows)
    }

    var body: some View {
        VStack(spacing: 0) {
            row(cells: block.header, isHeader: true)
            tableDivider
            ForEach(Array(block.rows.enumerated()), id: \.offset) { index, cells in
                row(cells: cells, isHeader: false)
                if index < block.rows.count - 1 {
                    tableDivider
                }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .overlay(
            RoundedRectangle(cornerRadius: 6)
                .stroke(Color.secondary.opacity(0.5), lineWidth: 1)
        )
        .padding(.vertical, 4)
    }

    private var tableDivider: some View {
        Rectangle()
            .fill(Color.secondary.opacity(0.3))
            .frame(height: 1)
    }

    private func row(cells: [String], isHeader: Bool) -> some View {
        WeightedRowLayout(weights: weights, spacing: 0) {
            ForEach(Array(cells.enumerated()), id: \.offset) { index, cell in
                Text(renderInlineMarkdown(cell, highlight: highlight))
                    .font(font)
                    .fontWeight(isHeader ? .semibold : nil)
                    .multilineTextAlignment(textAlignment(at: index))
                    .lineLimit(cellLineLimit)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: alignment(at: index))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 6)
            }
        }
        .background(isHeader ? Color.secondary.opacity(0.12) : Color.clear)
    }

    private func alignmentName(at index: Int) -> String {
        index < block.alignments.count ? block.alignments[index] : "start"
    }

    private func alignment(at index: Int) -> Alignment {
        switch alignmentName(at: index) {
        case "center": return .center
        case "end": return .trailing
        default: return .leading
        }
    }

    private func textAlignment(at index: Int) -> TextAlignment {
        switch alignmentName(at: index) {
        case "center": return .center
        case "end": return .trailing
        default: return .leading
        }
    }
}

/// Distributes the row width by column weight, matching the Compose table's
/// `Modifier.weight` behaviour, and reports the tallest cell as the row height.
private struct WeightedRowLayout: Layout {
    var weights: [CGFloat]
    var spacing: CGFloat = 0

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let totalWidth = proposal.width
            ?? subviews.map { $0.sizeThatFits(.unspecified).width }.reduce(0, +)
        let widths = columnWidths(totalWidth: totalWidth, count: subviews.count)
        var height: CGFloat = 0
        for (index, subview) in subviews.enumerated() {
            let size = subview.sizeThatFits(ProposedViewSize(width: widths[index], height: nil))
            height = max(height, size.height)
        }
        return CGSize(width: totalWidth, height: height)
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        let widths = columnWidths(totalWidth: bounds.width, count: subviews.count)
        var x = bounds.minX
        for (index, subview) in subviews.enumerated() {
            subview.place(
                at: CGPoint(x: x, y: bounds.minY),
                anchor: .topLeading,
                proposal: ProposedViewSize(width: widths[index], height: bounds.height)
            )
            x += widths[index] + spacing
        }
    }

    private func columnWidths(totalWidth: CGFloat, count: Int) -> [CGFloat] {
        guard count > 0 else { return [] }
        let available = max(0, totalWidth - spacing * CGFloat(count - 1))
        let active = Array(weights.prefix(count))
        let total = active.reduce(0, +)
        guard total > 0 else {
            return Array(repeating: available / CGFloat(count), count: count)
        }
        return active.map { available * ($0 / total) }
    }
}

/// Full-width code card: monospace, preserved whitespace, optional language
/// caption. No syntax highlighting; long lines wrap (v1 choice) rather than
/// scrolling, so the block stays safe inside intrinsic-measured parents.
private struct MarkdownCodeBlockView: View {
    let code: String
    let language: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if !language.trimmingCharacters(in: .whitespaces).isEmpty {
                Text(language)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Text(code)
                .font(.system(.body, design: .monospaced))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(Color.secondary.opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
        .padding(.vertical, 4)
    }
}

/// Same sizing rule as the Compose table: weight by the longest cell text,
/// clamped to 4...24 so narrow columns get less room but stay readable.
private enum MarkdownTableWeights {
    static func weights(header: [String], rows: [[String]]) -> [CGFloat] {
        guard !header.isEmpty else { return [] }
        var widths = header.map(\.count)
        for row in rows {
            for (index, cell) in row.enumerated() where index < widths.count {
                widths[index] = max(widths[index], cell.count)
            }
        }
        return widths.map { CGFloat(min(max($0, 4), 24)) }
    }
}
