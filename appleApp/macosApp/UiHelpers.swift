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

/// Builds display text from the shared markdown spans (bold/italic/strike/
/// highlight/code). Presentation intents keep the caller's base font; only
/// highlight carries an explicit color (bold + accent, or the span's `{cN}`
/// palette color). Shared offsets are UTF-16 code units, so ranges convert
/// via `String.Index(utf16Offset:in:)` — never integer subscripting, which
/// breaks on emoji.
func basicMarkdown(_ raw: String, highlight: Color = Color.orange) -> AttributedString {
    let parsed = RichTextKt.parseRichText(input: raw)
    let clean = parsed.text
    var out = AttributedString(clean)
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
func basicMarkdownNS(_ raw: String, highlight: NSColor = .orange) -> NSAttributedString {
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

private extension NSColor {
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
