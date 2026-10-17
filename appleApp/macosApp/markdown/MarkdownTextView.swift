//
//  MarkdownTextView.swift
//  macosApp — AppKit-backed editor surface with live markdown styling.
//

import SwiftUI
import AppKit
import Combine
import Shared

/// Bridges the SwiftUI toolbar to the underlying `NSTextView`. Holds a weak
/// reference to the live text view so a toolbar button can read the current
/// selection, apply a shared `ToolbarAction`, and write the result back through
/// the component's text binding (in a single undo group).
final class MarkdownEditorController: ObservableObject {
    weak var textView: NSTextView?

    func apply(_ action: ToolbarAction, to text: Binding<String>) {
        guard let textView else { return }
        let selection = textView.selectedRange()
        let edit = MarkdownTextFieldKt.applyToolbarAction(
            text: textView.string,
            selectionStart: Int32(selection.location),
            selectionEnd: Int32(selection.location + selection.length),
            action: action
        )
        replace(textView, with: edit.text, selectionStart: edit.selectionStart, selectionEnd: edit.selectionEnd)
        text.wrappedValue = edit.text
        textView.window?.makeFirstResponder(textView)
    }

    /// Replaces the whole buffer through the change-notification path so the
    /// edit is undoable as one step.
    private func replace(_ textView: NSTextView, with newText: String, selectionStart: Int32, selectionEnd: Int32) {
        let fullRange = NSRange(location: 0, length: (textView.string as NSString).length)
        guard textView.shouldChangeText(in: fullRange, replacementString: newText) else { return }
        textView.textStorage?.replaceCharacters(in: fullRange, with: newText)
        textView.didChangeText()
        let length = (newText as NSString).length
        let location = min(Int(selectionStart), length)
        let end = min(Int(selectionEnd), length)
        textView.setSelectedRange(NSRange(location: location, length: max(0, end - location)))
    }
}

/// Multi-line editor over `NSTextView`. Keep the raw markdown source visible
/// but style it live (headers, lists, quotes, inline spans), mirroring Android's
/// `MarkdownVisualTransformation`.
struct MarkdownTextView: NSViewRepresentable {
    @Binding var text: String
    @Binding var isFocused: Bool
    var isEditable: Bool = true
    let controller: MarkdownEditorController

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeNSView(context: Context) -> NSScrollView {
        let scrollView = NSTextView.scrollableTextView()
        scrollView.drawsBackground = false
        scrollView.borderType = .noBorder
        scrollView.hasVerticalScroller = true
        guard let textView = scrollView.documentView as? NSTextView else { return scrollView }

        context.coordinator.textView = textView
        textView.delegate = context.coordinator
        textView.textStorage?.delegate = context.coordinator
        controller.textView = textView

        textView.isRichText = true
        textView.allowsUndo = true
        textView.isEditable = isEditable
        textView.drawsBackground = false
        textView.textContainerInset = NSSize(width: 4, height: 6)
        textView.isVerticallyResizable = true
        textView.isHorizontallyResizable = false
        textView.autoresizingMask = [.width]
        textView.textContainer?.widthTracksTextView = true
        // Markdown markers must stay literal — no smart quotes/dashes/replacements.
        textView.isAutomaticQuoteSubstitutionEnabled = false
        textView.isAutomaticDashSubstitutionEnabled = false
        textView.isAutomaticTextReplacementEnabled = false
        textView.isAutomaticSpellingCorrectionEnabled = false
        textView.isAutomaticLinkDetectionEnabled = false
        textView.usesFindBar = true
        textView.font = MarkdownSourceStyling.baseFont
        textView.textColor = MarkdownSourceStyling.baseColor
        textView.typingAttributes = MarkdownSourceStyling.baseAttributes

        textView.string = text
        context.coordinator.highlight(textView)
        return scrollView
    }

    func updateNSView(_ scrollView: NSScrollView, context: Context) {
        guard let textView = scrollView.documentView as? NSTextView else { return }
        context.coordinator.parent = self
        controller.textView = textView
        if textView.isEditable != isEditable { textView.isEditable = isEditable }

        if textView.string != text {
            // External update (item switch, load): replace and park the caret at the end.
            textView.string = text
            let length = (text as NSString).length
            textView.setSelectedRange(NSRange(location: length, length: 0))
            context.coordinator.highlight(textView)
        }

        if isFocused, let window = textView.window, window.firstResponder !== textView {
            window.makeFirstResponder(textView)
        }
    }

    @MainActor
    final class Coordinator: NSObject, NSTextViewDelegate, NSTextStorageDelegate {
        var parent: MarkdownTextView
        weak var textView: NSTextView?
        private var isHighlighting = false

        init(_ parent: MarkdownTextView) { self.parent = parent }

        /// Rebuilds the display attributes from the shared source spans. Runs on
        /// every text edit; the `isHighlighting` guard keeps the resulting
        /// attribute-only storage change from recursing.
        func highlight(_ textView: NSTextView) {
            guard let storage = textView.textStorage else { return }
            // Never fight an active input-method composition.
            guard !textView.hasMarkedText() else { return }
            isHighlighting = true
            defer { isHighlighting = false }

            let fullRange = NSRange(location: 0, length: storage.length)
            storage.beginEditing()
            storage.setAttributes(MarkdownSourceStyling.baseAttributes, range: fullRange)
            for span in MarkdownSourceStyleKt.parseMarkdownSourceStyles(text: storage.string) {
                let range = NSRange(location: Int(span.start), length: Int(span.end - span.start))
                guard range.location >= 0, range.length >= 0, NSMaxRange(range) <= storage.length else { continue }
                storage.addAttributes(MarkdownSourceStyling.attributes(for: span), range: range)
            }
            storage.endEditing()
            textView.typingAttributes = MarkdownSourceStyling.baseAttributes
        }

        func textStorage(
            _ textStorage: NSTextStorage,
            didProcessEditing editedMask: NSTextStorageEditActions,
            range editedRange: NSRange,
            changeInLength delta: Int
        ) {
            guard editedMask.contains(.editedCharacters), !isHighlighting, let textView else { return }
            highlight(textView)
        }

        func textDidChange(_ notification: Notification) {
            guard let textView = notification.object as? NSTextView else { return }
            if parent.text != textView.string {
                parent.text = textView.string
            }
        }

        func textDidBeginEditing(_ notification: Notification) {
            guard !parent.isFocused else { return }
            parent.isFocused = true
        }

        func textDidEndEditing(_ notification: Notification) {
            guard parent.isFocused else { return }
            parent.isFocused = false
        }
    }
}
