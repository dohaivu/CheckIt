//
//  NestedInspectorPane.swift
//  macosApp — trailing inspector for the selected item's details / note.
//

import SwiftUI
import Combine
import Shared

/// Trailing inspector: Details (metrics/progress) or Note for the selected
/// item. Coexists with the legacy Note/Details sheets; entry points that
/// used to open a sheet reveal this pane instead, and the sheets stay
/// reachable via ⌥N/⌥D and the row context menu.
struct NestedInspectorPane: View {
    @ObservedObject var state: NestedEditorState

    @StateObject private var detailsDraft = NestedDetailsDraft()
    @State private var loadedDetailsId: String? = nil
    @State private var noteText = ""
    @State private var noteBaseline = ""
    @State private var noteFocused = false

    var body: some View {
        VStack(spacing: 0) {
            header
            Divider()
            if let item = state.selectedItem {
                content(for: item)
            } else {
                emptyState
            }
        }
        .frame(minWidth: 260, maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(nsColor: .windowBackgroundColor))
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 8) {
            Picker("", selection: $state.inspectorTab) {
                Text("Details").tag(NestedInspectorTab.details)
                Text("Note").tag(NestedInspectorTab.note)
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .accessibilityLabel("Inspector section")
            Spacer()
            Button {
                state.showInspector = false
            } label: {
                Image(systemName: "xmark")
                    .imageScale(.small)
                    .foregroundStyle(.secondary)
                    .frame(width: 22, height: 22)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Hide inspector")
            .help("Hide inspector (⌘⌥I)")
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 5)
    }

    // MARK: - Content

    @ViewBuilder
    private func content(for item: NestedListItem) -> some View {
        switch state.inspectorTab {
        case .details:
            VStack(spacing: 0) {
                itemHeader(item)
                NestedDetailsForm(draft: detailsDraft, state: state, item: item)
                Divider()
                detailsFooter(item: item)
            }
            .onAppear { loadDetailsIfNeeded(item) }
            .onChange(of: item.id) { _, _ in loadDetailsIfNeeded(item) }
        case .note:
            VStack(spacing: 0) {
                itemHeader(item)
                NestedNoteEditor(
                    text: $noteText,
                    isDirty: trimmedNote != noteBaseline,
                    focused: $noteFocused,
                    startsInPreview: true
                )
                .padding(.horizontal, 10)
                .padding(.bottom, 8)
            }
            .onAppear { loadNote(from: item) }
            .onChange(of: item.id) { _, _ in loadNote(from: item) }
            .onChange(of: noteFocused) { _, focused in
                if !focused { commitNote(item: item) }
            }
            .onDisappear { commitNote(item: item) }
        }
    }

    private func itemHeader(_ item: NestedListItem) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(item.text.isEmpty ? "Untitled item" : item.text)
                .font(.headline)
                .lineLimit(2)
            if let ms = item.completedAtMillis?.int64Value {
                Text("Completed · \(Date(timeIntervalSince1970: TimeInterval(ms) / 1000).formatted(date: .abbreviated, time: .shortened))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
    }

    private func detailsFooter(item: NestedListItem) -> some View {
        HStack {
            Button("Revert") { detailsDraft.load(from: item) }
                .buttonStyle(.link)
                .foregroundStyle(.secondary)
                .help("Discard unsaved changes")
            Spacer()
            Button("Apply") { detailsDraft.save(state: state, itemId: item.id) }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
    }

    private var emptyState: some View {
        VStack(spacing: 6) {
            Image(systemName: "sidebar.right")
                .imageScale(.large)
                .foregroundStyle(.tertiary)
            Text("No item selected")
                .font(.headline)
            Text("Select an item to see its details or note.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    // MARK: - Note helpers

    private var trimmedNote: String {
        noteText.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Load the details draft on first show / selection change, but keep
    /// unsaved edits across inspector tab switches for the same item.
    private func loadDetailsIfNeeded(_ item: NestedListItem) {
        guard loadedDetailsId != item.id else { return }
        loadedDetailsId = item.id
        detailsDraft.load(from: item)
    }

    private func loadNote(from item: NestedListItem) {
        noteText = item.note ?? ""
        noteBaseline = (item.note ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Persist on focus loss / tab change; no-op when unchanged.
    private func commitNote(item: NestedListItem) {
        guard trimmedNote != noteBaseline else { return }
        state.saveNote(id: item.id, text: noteText)
        noteBaseline = trimmedNote
    }
}
