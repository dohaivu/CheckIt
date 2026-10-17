//
//  MarkdownTextField.swift
//  macosApp — markdown editor with live styling and a formatting toolbar.
//

import SwiftUI

/// Multi-line markdown editor: raw source with live inline styling plus an
/// optional formatting toolbar. The macOS analogue of the shared Android
/// `MarkdownTextField`, reusing the shared edit semantics
/// (`applyToolbarAction`) and span computation (`parseMarkdownSourceStyles`).
///
/// The caller owns the text and focus bindings, e.g.:
///
///     MarkdownTextField(text: $text, focused: $focused, placeholder: "Write a note…")
struct MarkdownTextField: View {
    @Binding var text: String
    @Binding var focused: Bool
    var placeholder: String? = nil
    var minHeight: CGFloat = 160
    var isEditable: Bool = true
    var showToolbar: Bool = true

    @StateObject private var controller = MarkdownEditorController()

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ZStack(alignment: .topLeading) {
                MarkdownTextView(
                    text: $text,
                    isFocused: $focused,
                    isEditable: isEditable,
                    controller: controller
                )
                if text.isEmpty, let placeholder {
                    Text(placeholder)
                        .foregroundStyle(.tertiary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .allowsHitTesting(false)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if showToolbar && isEditable {
                MarkdownToolbar { action in
                    controller.apply(action, to: $text)
                }
            }
        }
        .frame(minHeight: minHeight, maxHeight: .infinity, alignment: .top)
    }
}
