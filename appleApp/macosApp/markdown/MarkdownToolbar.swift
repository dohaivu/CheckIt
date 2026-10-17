//
//  MarkdownToolbar.swift
//  macosApp — formatting button row for the markdown editor.
//

import SwiftUI
import Shared

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
                .help(spec.help)
                .accessibilityLabel(spec.help)
            }
        }
    }
}
