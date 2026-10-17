//
//  MarkdownSourceStyling.swift
//  macosApp — maps shared raw-source style spans to AppKit text attributes.
//

import AppKit
import Shared

/// AppKit attribute mapping for the live markdown editor. The span computation
/// itself lives in shared Kotlin (`parseMarkdownSourceStyles`, used by Android's
/// `MarkdownVisualTransformation` too); only the presentation is platform-specific
/// here, so the two editors stay in lock-step.
enum MarkdownSourceStyling {
    static let defaultHighlight = NSColor.systemOrange
    static let codeBackground = NSColor.gray.withAlphaComponent(0.15)

    /// Base body font/color for unstyled source text.
    static var baseFont: NSFont { NSFont.preferredFont(forTextStyle: .body) }
    static var baseColor: NSColor { .labelColor }

    private static let paragraphStyle: NSParagraphStyle = {
        let style = NSMutableParagraphStyle()
        style.lineSpacing = 2
        return style
    }()

    /// Attributes applied to the whole buffer before per-span overrides.
    static var baseAttributes: [NSAttributedString.Key: Any] {
        [.font: baseFont, .foregroundColor: baseColor, .paragraphStyle: paragraphStyle]
    }

    private static var boldFont: NSFont { trait(.boldFontMask) }
    private static var italicFont: NSFont { trait(.italicFontMask) }

    private static func trait(_ mask: NSFontTraitMask) -> NSFont {
        NSFontManager.shared.convert(baseFont, toHaveTrait: mask)
    }

    private static var monoFont: NSFont {
        NSFont.monospacedSystemFont(ofSize: baseFont.pointSize, weight: .regular)
    }

    private static var monoBoldFont: NSFont {
        NSFont.monospacedSystemFont(ofSize: baseFont.pointSize, weight: .bold)
    }

    /// Attributes for one shared span. Unmapped kinds fall back to the base style.
    static func attributes(for span: MarkdownSourceStyleSpan) -> [NSAttributedString.Key: Any] {
        let kind = span.kind
        if kind == .bold { return [.font: boldFont] }
        if kind == .italic { return [.font: italicFont] }
        if kind == .strikethrough { return [.strikethroughStyle: NSUnderlineStyle.single.rawValue] }
        if kind == .highlight {
            return [.font: boldFont, .foregroundColor: highlightColor(hex: span.colorHex)]
        }
        if kind == .code { return [.font: monoFont, .backgroundColor: codeBackground] }
        if kind == .header1 { return [.font: NSFont.systemFont(ofSize: 24, weight: .bold)] }
        if kind == .header2 { return [.font: NSFont.systemFont(ofSize: 20, weight: .bold)] }
        if kind == .header3 { return [.font: NSFont.systemFont(ofSize: 18, weight: .semibold)] }
        if kind == .bullet { return [.font: monoBoldFont] }
        if kind == .numbered { return [.font: monoFont] }
        if kind == .quote { return [.font: monoBoldFont] }
        if kind == .quoted { return [.font: italicFont] }
        return [:]
    }

    private static func highlightColor(hex: String?) -> NSColor {
        if let hex, let color = NSColor(nestedHex: hex) { return color }
        return defaultHighlight
    }
}
