//
//  MarkdownFromAttributedString.swift
//  macosApp — converts rich (RTF) clipboard content into markdown source.
//

import AppKit

/// Best-effort conversion of an attributed string (decoded from RTF clipboard
/// data) into the app's markdown dialect. Inline emphasis comes from font
/// traits, strikethrough and background colour; block structure comes from
/// paragraph styles (lists) plus a conservative font-size heading heuristic.
///
/// This is deliberately a *fallback*: HTML clipboard data is richer and more
/// faithful, and is preferred upstream (see `MarkdownPasteboard`).
enum MarkdownFromAttributedString {

    static func convert(_ attributed: NSAttributedString) -> String {
        guard attributed.length > 0 else { return "" }
        let bodySize = baseFontSize(of: attributed)
        var lines: [String] = []

        var location = 0
        let source = attributed.string as NSString
        while location < attributed.length {
            let range = source.paragraphRange(for: NSRange(location: location, length: 0))
            let paragraph = NSMutableAttributedString(
                attributedString: attributed.attributedSubstring(from: range)
            )
            trimParagraphEdges(paragraph)

            let style = paragraph.attribute(.paragraphStyle, at: 0, effectiveRange: nil) as? NSParagraphStyle
            let prefix = prefix(for: paragraph, style: style, at: range.location, in: attributed, bodySize: bodySize)
            let content = inlineMarkdown(paragraph)

            lines.append(content.isEmpty ? "" : prefix + content)
            location = NSMaxRange(range)
        }

        return normalize(lines)
    }

    // MARK: - Block prefix

    private static func prefix(
        for paragraph: NSAttributedString,
        style: NSParagraphStyle?,
        at location: Int,
        in attributed: NSAttributedString,
        bodySize: CGFloat
    ) -> String {
        if let list = style?.textLists.first {
            if list.isOrdered {
                return "\(max(attributed.itemNumber(in: list, at: location), 1)). "
            }
            return "- "
        }
        return headingPrefix(for: paragraph, bodySize: bodySize) ?? ""
    }

    /// Font size carries heading intent in RTF, so treat a uniformly larger
    /// short line as a heading. Kept conservative to avoid promoting body text.
    private static func headingPrefix(for paragraph: NSAttributedString, bodySize: CGFloat) -> String? {
        guard paragraph.length > 0, paragraph.length <= 100, bodySize > 0 else { return nil }
        let size = maxFontSize(of: paragraph)
        if size >= bodySize * 1.5 { return "# " }
        if size >= bodySize * 1.25 { return "## " }
        if size >= bodySize * 1.12 { return "### " }
        return nil
    }

    // MARK: - Inline runs

    /// Groups consecutive runs sharing a style, so `**bold**` is emitted once
    /// around the whole span rather than once per run.
    private static func inlineMarkdown(_ string: NSAttributedString) -> String {
        guard string.length > 0 else { return "" }
        let ns = string.string as NSString
        var result = ""
        var current: StyleSignature?
        var buffer = ""

        func flush() {
            guard !buffer.isEmpty else { return }
            result += apply(current, to: buffer)
            buffer = ""
        }

        string.enumerateAttributes(in: NSRange(location: 0, length: string.length)) { attributes, range, _ in
            let signature = StyleSignature(attributes: attributes)
            if signature != current {
                flush()
                current = signature
            }
            buffer += ns.substring(with: range)
        }
        flush()
        return result
    }

    private static func apply(_ signature: StyleSignature?, to text: String) -> String {
        guard let signature, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return text }
        var result = text
        if signature.code { result = "`\(result)`" }
        if signature.italic { result = "_\(result)_" }
        if signature.bold { result = "**\(result)**" }
        if signature.strike { result = "~~\(result)~~" }
        if signature.highlight { result = "==\(result)==" }
        return result
    }

    // MARK: - Fonts

    private static func baseFontSize(of string: NSAttributedString) -> CGFloat {
        var weights: [CGFloat: Int] = [:]
        string.enumerateAttribute(.font, in: NSRange(location: 0, length: string.length)) { value, range, _ in
            if let font = value as? NSFont { weights[font.pointSize, default: 0] += range.length }
        }
        return weights.max { $0.value < $1.value }?.key ?? NSFont.systemFontSize
    }

    private static func maxFontSize(of string: NSAttributedString) -> CGFloat {
        var size: CGFloat = 0
        string.enumerateAttribute(.font, in: NSRange(location: 0, length: string.length)) { value, _, _ in
            if let font = value as? NSFont { size = max(size, font.pointSize) }
        }
        return size
    }

    // MARK: - Cleanup

    private static func trimParagraphEdges(_ string: NSMutableAttributedString) {
        var end = string.length
        while end > 0, isParagraphTerminator((string.string as NSString).character(at: end - 1)) { end -= 1 }
        if end < string.length {
            string.deleteCharacters(in: NSRange(location: end, length: string.length - end))
        }

        var start = 0
        while start < string.length {
            let character = (string.string as NSString).character(at: start)
            if character == 9 || character == 32 { start += 1 } else { break }
        }
        if start > 0 {
            string.deleteCharacters(in: NSRange(location: 0, length: start))
        }
    }

    private static func isParagraphTerminator(_ character: unichar) -> Bool {
        character == 10 || character == 13 || character == 0x2028 || character == 0x2029
    }

    private static func normalize(_ lines: [String]) -> String {
        var output: [String] = []
        var previousBlank = false
        for line in lines {
            let trimmed = line.replacingOccurrences(of: "\\s+$", with: "", options: .regularExpression)
            if trimmed.isEmpty {
                if previousBlank || output.isEmpty { continue }
                previousBlank = true
                output.append("")
            } else {
                previousBlank = false
                output.append(trimmed)
            }
        }
        while output.last?.isEmpty == true { output.removeLast() }
        return output.joined(separator: "\n")
    }
}

/// The inline markers to apply to one run of text.
private struct StyleSignature: Equatable {
    let bold: Bool
    let italic: Bool
    let strike: Bool
    let code: Bool
    let highlight: Bool

    init(attributes: [NSAttributedString.Key: Any]) {
        let font = attributes[.font] as? NSFont
        bold = font.map { NSFontManager.shared.traits(of: $0).contains(.boldFontMask) } ?? false
        italic = font.map { NSFontManager.shared.traits(of: $0).contains(.italicFontMask) } ?? false
        code = font.map { $0.fontDescriptor.symbolicTraits.contains(.monoSpace) } ?? false
        strike = ((attributes[.strikethroughStyle] as? Int) ?? 0) != 0
        if let color = attributes[.backgroundColor] as? NSColor {
            highlight = color.alphaComponent > 0.05
        } else {
            highlight = false
        }
    }
}
