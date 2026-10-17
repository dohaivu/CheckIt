//
//  MarkdownToolbar.swift
//  macosApp — formatting button row for the markdown editor.
//

import SwiftUI
import AppKit
import Shared

/// A single formatting shortcut. Command is always required; `requiresShift`
/// adds Shift. Option/Control disqualify the event so richer combinations are
/// never shadowed.
struct MarkdownShortcut: Equatable {
    let key: Character
    let requiresShift: Bool

    /// Tooltip form, e.g. `⌘B` or `⇧⌘8`.
    var display: String { (requiresShift ? "⇧" : "") + "⌘" + String(key).uppercased() }

    /// True when `event` is exactly this combination on the current layout.
    /// Uses the base (unshifted) character so digit-row shortcuts such as
    /// `⇧⌘8` don't depend on the shifted symbol (`*`) that the event reports.
    func matches(_ event: NSEvent) -> Bool {
        let flags = event.modifierFlags.intersection(.deviceIndependentFlagsMask)
        guard flags.contains(.command),
              !flags.contains(.option),
              !flags.contains(.control),
              flags.contains(.shift) == requiresShift,
              let base = event.characters(byApplyingModifiers: []),
              base.count == 1,
              let typed = base.lowercased().first
        else { return false }
        return typed == key
    }
}

/// Canonical formatting shortcuts, shared by the toolbar tooltips and the
/// editor's key handling so the two can never drift apart.
enum MarkdownShortcuts {
    static let all: [(shortcut: MarkdownShortcut, action: ToolbarAction)] = [
        (MarkdownShortcut(key: "b", requiresShift: false), .bold),
        (MarkdownShortcut(key: "i", requiresShift: false), .italic),
        (MarkdownShortcut(key: "e", requiresShift: false), .code),
        (MarkdownShortcut(key: "h", requiresShift: true), .highlight),
        (MarkdownShortcut(key: "x", requiresShift: true), .strikethrough),
        (MarkdownShortcut(key: "1", requiresShift: false), .heading),
        (MarkdownShortcut(key: "8", requiresShift: true), .bullet),
        (MarkdownShortcut(key: "7", requiresShift: true), .numbered),
        (MarkdownShortcut(key: "9", requiresShift: true), .quote),
    ]

    /// Resolves the formatting action bound to a key event, if any.
    static func action(for event: NSEvent) -> ToolbarAction? {
        all.first { $0.shortcut.matches(event) }?.action
    }

    /// Tooltip suffix for an action, e.g. `⌘B`, or nil when it has no shortcut.
    static func display(for action: ToolbarAction) -> String? {
        all.first { $0.action == action }?.shortcut.display
    }
}

/// Formatting toolbar mirroring Android's `MarkdownToolbar` (minus the slash
/// command). Stateless: it only reports the chosen `ToolbarAction`; the editor
/// applies it via the shared `applyToolbarAction` logic.
struct MarkdownToolbar: View {
    var onAction: (ToolbarAction) -> Void

    private struct ButtonSpec: Identifiable {
        let id: String
        let symbol: String
        let help: String
        let action: ToolbarAction
    }

    private var buttons: [ButtonSpec] {
        [
            ButtonSpec(id: "bold", symbol: "bold", help: "Bold  (**text**)", action: .bold),
            ButtonSpec(id: "italic", symbol: "italic", help: "Italic  (_text_)", action: .italic),
            ButtonSpec(id: "code", symbol: "chevron.left.forwardslash.chevron.right", help: "Inline code  (`code`)", action: .code),
            ButtonSpec(id: "highlight", symbol: "highlighter", help: "Highlight  (==text==)", action: .highlight),
            ButtonSpec(id: "strike", symbol: "strikethrough", help: "Strikethrough  (~~text~~)", action: .strikethrough),
            ButtonSpec(id: "heading", symbol: "textformat.size", help: "Heading  (## )", action: .heading),
            ButtonSpec(id: "bullet", symbol: "list.bullet", help: "Bullet list  (- )", action: .bullet),
            ButtonSpec(id: "numbered", symbol: "list.number", help: "Numbered list  (1. )", action: .numbered),
            ButtonSpec(id: "quote", symbol: "text.quote", help: "Quote  (> )", action: .quote),
        ]
    }

    var body: some View {
        HStack(spacing: 2) {
            ForEach(buttons) { spec in
                Button {
                    onAction(spec.action)
                } label: {
                    Image(systemName: spec.symbol)
                        .frame(width: 26, height: 22)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .foregroundStyle(.secondary)
                .help(help(for: spec))
                .accessibilityLabel(spec.help)
            }
        }
    }

    /// Tooltip text with the shortcut appended, e.g. `Bold  (**text**)  ⌘B`.
    private func help(for spec: ButtonSpec) -> String {
        guard let shortcut = MarkdownShortcuts.display(for: spec.action) else { return spec.help }
        return "\(spec.help)  \(shortcut)"
    }
}
