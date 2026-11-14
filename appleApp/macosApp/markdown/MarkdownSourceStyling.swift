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

    private static var monoFont: NSFont {
        NSFont.monospacedSystemFont(ofSize: baseFont.pointSize, weight: .regular)
    }

    private static var monoBoldFont: NSFont {
        NSFont.monospacedSystemFont(ofSize: baseFont.pointSize, weight: .bold)
    }

    /// Attributes for one shared span. Unmapped kinds fall back to the base style.
    ///
    /// Applied span-by-span on top of the buffer's base attributes. Font-based
    /// kinds merge with whatever font is already in effect instead of replacing
    /// it: `addAttributes` would overwrite `.font` wholesale, so a nested span
    /// (`**_both_**`, bold inside a quote, code inside bold) would drop the
    /// outer trait. Compose merges overlapping `SpanStyle`s, so mirror that.
    static func apply(
        _ span: MarkdownSourceStyleSpan,
        to storage: NSTextStorage,
        range: NSRange
    ) {
        switch span.kind {
        case .bold:
            storage.applyFontTraits(.boldFontMask, range: range)
        case .italic, .quoted:
            storage.applyFontTraits(.italicFontMask, range: range)
        case .strikethrough:
            storage.addAttribute(.strikethroughStyle, value: NSUnderlineStyle.single.rawValue, range: range)
        case .highlight:
            storage.addAttribute(.foregroundColor, value: highlightColor(hex: span.colorHex), range: range)
        case .code:
            // Monospace, keeping size plus bold/italic traits from the run it
            // covers, so ``**`x`**`` stays bold and code in a quote stays italic.
            replaceFonts(in: range, of: storage) { monospaced(from: $0) }
            storage.addAttribute(.backgroundColor, value: codeBackground, range: range)
        case .header1:
            storage.addAttribute(.font, value: NSFont.systemFont(ofSize: 24, weight: .bold), range: range)
        case .header2:
            storage.addAttribute(.font, value: NSFont.systemFont(ofSize: 20, weight: .bold), range: range)
        case .header3:
            storage.addAttribute(.font, value: NSFont.systemFont(ofSize: 18, weight: .semibold), range: range)
        case .bullet, .quote:
            storage.addAttribute(.font, value: monoBoldFont, range: range)
        case .numbered:
            storage.addAttribute(.font, value: monoFont, range: range)
        default:
            break
        }
    }

    /// Rewrites every font run in [range], one `addAttribute` per existing run
    /// so traits already present survive the change.
    private static func replaceFonts(
        in range: NSRange,
        of storage: NSTextStorage,
        transform: (NSFont) -> NSFont
    ) {
        var updates: [(font: NSFont, range: NSRange)] = []
        storage.enumerateAttribute(.font, in: range, options: []) { value, subRange, _ in
            updates.append((transform(value as? NSFont ?? baseFont), subRange))
        }
        for update in updates {
            storage.addAttribute(.font, value: update.font, range: update.range)
        }
    }

    /// Same point size, monospaced family, existing bold/italic traits kept.
    private static func monospaced(from font: NSFont) -> NSFont {
        let traits = NSFontManager.shared.traits(of: font)
        let weight: NSFont.Weight = traits.contains(.boldFontMask) ? .bold : .regular
        let mono = NSFont.monospacedSystemFont(ofSize: font.pointSize, weight: weight)
        return traits.contains(.italicFontMask)
            ? NSFontManager.shared.convert(mono, toHaveTrait: .italicFontMask)
            : mono
    }

    private static func highlightColor(hex: String?) -> NSColor {
        if let hex, let color = NSColor(nestedHex: hex) { return color }
        return defaultHighlight
    }
}
