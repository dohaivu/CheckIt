//
//  UiHelpers.swift
//  macosApp — small shared UI builders.
//

import SwiftUI
import AppKit
import Shared

/// Builds display text from the shared markdown spans (bold/italic/strike/
/// highlight). Presentation intents keep the caller's base font; only
/// highlight carries an explicit color (bold + accent). Shared offsets are
/// UTF-16 code units, so ranges convert via `String.Index(utf16Offset:in:)` —
/// never integer subscripting, which breaks on emoji.
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
            out[range].foregroundColor = highlight
        case .code:
            out[range].inlinePresentationIntent = .code
            out[range].backgroundColor = Color.gray.opacity(0.15)
        default:
            break
        }
    }
    return out
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
            out.addAttribute(.foregroundColor, value: highlight, range: range)
        case .code:
            out.addAttribute(.font, value: NSFont.monospacedSystemFont(ofSize: NSFont.systemFontSize, weight: .regular), range: range)
            out.addAttribute(.backgroundColor, value: NSColor.gray.withAlphaComponent(0.15), range: range)
        default:
            break
        }
    }
    return out
}
