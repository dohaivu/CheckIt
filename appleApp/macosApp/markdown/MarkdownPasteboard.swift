//
//  MarkdownPasteboard.swift
//  macosApp — converts rich clipboard content into markdown source on paste.
//

import AppKit
import Shared

/// Resolves what to insert for a paste or drop, converting rich clipboard
/// content into the app's markdown dialect. Always returns *plain* markdown
/// source, so AppKit never injects foreign attributes (fonts, colours) that the
/// live highlighter would immediately discard anyway.
enum MarkdownPasteboard {

    /// Returns the markdown source to insert, or `nil` when the pasteboard has
    /// nothing we handle so AppKit's default behaviour should run instead
    /// (file URLs, images, …).
    static func markdown(from pasteboard: NSPasteboard) -> String? {
        guard let types = pasteboard.types else { return nil }
        // File drags/inserts stay with AppKit — it knows about attachments.
        if types.contains(.fileURL) { return nil }

        let plain = pasteboard.string(forType: .string)
        let html = pasteboard.string(forType: .html).flatMap { $0.isEmpty ? nil : $0 }

        if let html {
            // Rendered source (browser, Notion, …). Only skip HTML when the
            // plain text is unambiguously *inline* markdown source; a rendered
            // page legitimately contains "1. "/bullet text, so those markers
            // must not count here or we would drop the page's HTML formatting.
            if let plain, looksLikeInlineMarkdownSource(plain) { return plain }
            let converted = HtmlToMarkdownKt.htmlToMarkdown(html: html)
            if !converted.isEmpty { return converted }
        }

        if let rtf = pasteboard.data(forType: .rtf),
           let attributed = NSAttributedString(rtf: rtf, documentAttributes: nil) {
            // No HTML: if the plain text already reads as our markdown (our own
            // editor, Obsidian, …) use it verbatim. Reverse-engineering the RTF
            // would double-escape the markers, since the editor styles them.
            if let plain, !plain.isEmpty,
               !MarkdownSourceStyleKt.parseMarkdownSourceStyles(text: plain).isEmpty {
                return plain
            }
            let converted = MarkdownFromAttributedString.convert(attributed)
            if !converted.isEmpty { return converted }
        }

        return plain
    }

    /// Inline delimiters and verbatim heading/quote markers are the reliable
    /// signal that a plain string *is* markdown source rather than rendered
    /// text, which only ever contains them as literal characters.
    private static func looksLikeInlineMarkdownSource(_ text: String) -> Bool {
        if text.contains("**") || text.contains("~~") || text.contains("==") || text.contains("`") {
            return true
        }
        for line in text.split(separator: "\n", omittingEmptySubsequences: false) {
            if line.hasPrefix("# ") || line.hasPrefix("## ") || line.hasPrefix("### ") || line.hasPrefix("> ") {
                return true
            }
        }
        return false
    }
}
