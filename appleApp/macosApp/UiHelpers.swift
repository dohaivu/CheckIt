//
//  UiHelpers.swift
//  macosApp — small shared UI builders.
//

import SwiftUI
import AppKit
import Shared

/// The focused field editor, if any. Used to drive cursor/text operations
/// explicitly (insert newline, move cursor): returning `.ignored` from
/// `.onKeyPress` does not reliably re-deliver the event for insertion, so
/// handlers that need text effects must perform them directly. Prefers the
/// window's first responder (which is the editor while editing) because
/// `fieldEditor(_:for:)` won't resolve with a nil object.
func nestedFieldEditor() -> NSTextView? {
    guard let window = NSApp.keyWindow else { return nil }
    if let editor = window.firstResponder as? NSTextView {
        return editor
    }
    return window.fieldEditor(false, for: nil) as? NSTextView
}

/// Drives arrow keys in the outline fields explicitly: matched keys that
/// return `.ignored` never reach the field editor, so without this the keys
/// would bubble to the outline handler and act on the selection mid-typing.
/// Mirrors NSText granularity (char / word / line, document extent with Cmd),
/// with Shift extending the selection. Bare Cmd is the caller's (outline
/// commands like move-item), so callers pre-handle that arm. Shared by the
/// row edit and draft fields.
func nestedArrowKeys(_ press: KeyPress) -> KeyPress.Result {
    guard let editor = nestedFieldEditor() else { return .handled }
    let shift = press.modifiers.contains(.shift)
    let cmd = press.modifiers.contains(.command)
    let opt = press.modifiers.contains(.option)
    let selector: Selector
    switch press.key {
    case .leftArrow, .rightArrow:
        let left = press.key == .leftArrow
        if cmd {
            selector = left
                ? (shift ? #selector(NSResponder.moveToBeginningOfLineAndModifySelection(_:)) : #selector(NSResponder.moveToBeginningOfLine(_:)))
                : (shift ? #selector(NSResponder.moveToEndOfLineAndModifySelection(_:)) : #selector(NSResponder.moveToEndOfLine(_:)))
        } else if opt {
            selector = left
                ? (shift ? #selector(NSResponder.moveWordLeftAndModifySelection(_:)) : #selector(NSResponder.moveWordLeft(_:)))
                : (shift ? #selector(NSResponder.moveWordRightAndModifySelection(_:)) : #selector(NSResponder.moveWordRight(_:)))
        } else {
            selector = left
                ? (shift ? #selector(NSResponder.moveLeftAndModifySelection(_:)) : #selector(NSResponder.moveLeft(_:)))
                : (shift ? #selector(NSResponder.moveRightAndModifySelection(_:)) : #selector(NSResponder.moveRight(_:)))
        }
    case .upArrow, .downArrow:
        let up = press.key == .upArrow
        if cmd {
            selector = up
                ? (shift ? #selector(NSResponder.moveToBeginningOfDocumentAndModifySelection(_:)) : #selector(NSResponder.moveToBeginningOfDocument(_:)))
                : (shift ? #selector(NSResponder.moveToEndOfDocumentAndModifySelection(_:)) : #selector(NSResponder.moveToEndOfDocument(_:)))
        } else {
            selector = up
                ? (shift ? #selector(NSResponder.moveUpAndModifySelection(_:)) : #selector(NSResponder.moveUp(_:)))
                : (shift ? #selector(NSResponder.moveDownAndModifySelection(_:)) : #selector(NSResponder.moveDown(_:)))
        }
    default:
        return .handled
    }
    editor.doCommand(by: selector)
    return .handled
}

private final class InlineMarkdownCache: @unchecked Sendable {
    static let shared = InlineMarkdownCache()
    private var cache = [String: AttributedString]()
    private let lock = NSLock()
    private let maxEntries = 500

    func get(_ key: String) -> AttributedString? {
        lock.lock()
        defer { lock.unlock() }
        return cache[key]
    }

    func set(_ key: String, value: AttributedString) {
        lock.lock()
        defer { lock.unlock() }
        if cache.count >= maxEntries {
            cache.removeAll(keepingCapacity: true)
        }
        cache[key] = value
    }
}

/// Builds display text from the shared markdown spans (bold/italic/strike/
/// highlight/code). Presentation intents keep the caller's base font; only
/// highlight carries an explicit color (bold + accent, or the span's `{cN}`
/// palette color). Shared offsets are UTF-16 code units, so ranges convert
/// via `String.Index(utf16Offset:in:)` — never integer subscripting, which
/// breaks on emoji.
func renderInlineMarkdown(_ raw: String, highlight: Color = Color.orange) -> AttributedString {
    guard !raw.isEmpty else { return AttributedString("") }
    if highlight == Color.orange, let cached = InlineMarkdownCache.shared.get(raw) {
        return cached
    }
    let parsed = RichTextKt.parseRichText(input: raw)
    let rendered = applyingRichSpans(parsed, to: AttributedString(parsed.text), highlight: highlight)
    if highlight == Color.orange {
        InlineMarkdownCache.shared.set(raw, value: rendered)
    }
    return rendered
}

/// Multi-line counterpart of [renderInlineMarkdown], mirroring the shared
/// `renderFullMarkdown` in MarkdownHelper.kt: line-based constructs are
/// resolved first (headers become larger/bolder fonts, `- `/`* ` bullets become
/// `•  `, `>` quotes become an italic `│  ` block), then the inline spans are
/// applied per line. Use this for prose blocks; keep [renderInlineMarkdown] for
/// single-line rows so `#tag` never blows up into a header.
func renderFullMarkdown(_ raw: String?, highlight: Color = Color.orange) -> AttributedString {
    guard let raw, !raw.isEmpty else { return AttributedString("") }
    var result = AttributedString()
    let lines = raw.components(separatedBy: "\n")

    for (index, line) in lines.enumerated() {
        var content = line
        var prefix: AttributedString?
        var headerFont: Font?
        var isQuote = false

        // 1. Line-based markers (strip the raw markers, style structurally).
        if content.hasPrefix("# ") {
            content.removeFirst(2)
            headerFont = .system(size: 24, weight: .bold)
        } else if content.hasPrefix("## ") {
            content.removeFirst(3)
            headerFont = .system(size: 20, weight: .bold)
        } else if content.hasPrefix("### ") {
            content.removeFirst(4)
            headerFont = .system(size: 18, weight: .semibold)
        } else if content.hasPrefix("- ") || content.hasPrefix("* ") {
            prefix = AttributedString("•  ")
            content.removeFirst(2)
        } else if content.hasPrefix("> ") {
            prefix = quotePrefix()
            content.removeFirst(2)
            isQuote = true
        } else if content.hasPrefix(">") {
            prefix = quotePrefix()
            content = String(content.dropFirst().drop(while: { $0.isWhitespace }))
            isQuote = true
        }

        // 2. Inline spans on the cleaned content. Base styling is applied
        // first so per-span intents (e.g. bold inside a quote) win, matching
        // the ordering in MarkdownHelper.kt.
        let parsed = RichTextKt.parseRichText(input: content)
        var lineText = AttributedString(parsed.text)
        if let headerFont { lineText.font = headerFont }
        if isQuote { lineText.inlinePresentationIntent = .emphasized }
        lineText = applyingRichSpans(parsed, to: lineText, highlight: highlight)

        if let prefix { result.append(prefix) }
        result.append(lineText)
        if index < lines.count - 1 { result.append(AttributedString("\n")) }
    }
    return result
}

/// Bold monospace `│  ` marker for blockquote lines.
private func quotePrefix() -> AttributedString {
    var prefix = AttributedString("│  ")
    prefix.font = .system(.body, design: .monospaced).bold()
    prefix.inlinePresentationIntent = .stronglyEmphasized
    return prefix
}

/// Applies the shared inline spans onto `base`, converting UTF-16 offsets via
/// `String.Index(utf16Offset:in:)` — never integer subscripting, which breaks
/// on emoji.
private func applyingRichSpans(
    _ parsed: RichText,
    to base: AttributedString,
    highlight: Color
) -> AttributedString {
    let clean = parsed.text
    var out = base
    for span in parsed.spans {
        let nsRange = NSRange(location: Int(span.start), length: Int(span.end - span.start))
        guard let swiftRange = Range(nsRange, in: clean),
              let lower = AttributedString.Index(swiftRange.lowerBound, within: out),
              let upper = AttributedString.Index(swiftRange.upperBound, within: out)
        else { continue }
        let range = lower..<upper
        switch span.kind {
        case .bold:
            out[range].inlinePresentationIntent = .stronglyEmphasized
        case .italic:
            out[range].inlinePresentationIntent = .emphasized
        case .strikethrough:
            out[range].strikethroughStyle = .single
        case .highlight:
            out[range].inlinePresentationIntent = .stronglyEmphasized
            out[range].foregroundColor = highlightColor(hex: span.colorHex, default: highlight)
        case .code:
            out[range].inlinePresentationIntent = .code
            out[range].backgroundColor = Color.gray.opacity(0.15)
        default:
            break
        }
    }
    return out
}

/// Resolves a shared `{cN}` hex (or nil) to a display color.
private func highlightColor(hex: String?, default highlight: Color) -> Color {
    if let hex, let color = Color(nestedHex: hex) {
        return color
    }
    return highlight
}

/// AppKit twin of [basicMarkdown] for `NSStatusBarButton.attributedTitle`
/// and other AppKit surfaces: same shared spans, applied as `NSFont` traits
/// and `NSColor` attributes. (Converting the SwiftUI `AttributedString`
/// would drop presentation intents like bold/italic, which only SwiftUI
/// renders.)
func renderInlineMarkdownNS(_ raw: String, highlight: NSColor = .orange) -> NSAttributedString {
    let parsed = RichTextKt.parseRichText(input: raw)
    let out = NSMutableAttributedString(string: parsed.text)
    for span in parsed.spans {
        let range = NSRange(location: Int(span.start), length: Int(span.end - span.start))
        guard range.location >= 0, NSMaxRange(range) <= out.length else { continue }
        switch span.kind {
        case .bold:
            out.applyFontTraits(.boldFontMask, range: range)
        case .italic:
            out.applyFontTraits(.italicFontMask, range: range)
        case .strikethrough:
            out.addAttribute(.strikethroughStyle, value: NSUnderlineStyle.single.rawValue, range: range)
        case .highlight:
            out.applyFontTraits(.boldFontMask, range: range)
            out.addAttribute(.foregroundColor, value: highlightColorNS(hex: span.colorHex, default: highlight), range: range)
        case .code:
            out.addAttribute(.font, value: NSFont.monospacedSystemFont(ofSize: NSFont.systemFontSize, weight: .regular), range: range)
            out.addAttribute(.backgroundColor, value: NSColor.gray.withAlphaComponent(0.15), range: range)
        default:
            break
        }
    }
    return out
}

/// Resolves a shared `{cN}` hex (or nil) to an `NSColor`.
private func highlightColorNS(hex: String?, default highlight: NSColor) -> NSColor {
    if let hex, let color = NSColor(nestedHex: hex) {
        return color
    }
    return highlight
}

extension NSColor {
    convenience init?(nestedHex hex: String) {
        var s = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6, let v = UInt32(s, radix: 16) else { return nil }
        self.init(
            red: CGFloat((v >> 16) & 0xFF) / 255.0,
            green: CGFloat((v >> 8) & 0xFF) / 255.0,
            blue: CGFloat(v & 0xFF) / 255.0,
            alpha: 1.0
        )
    }
}
