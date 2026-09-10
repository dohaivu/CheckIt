//
//  QuickNoteMenuView.swift
//  appleApp
//
//  Created by DO HAI VU on 10/9/26.
//
import SwiftUI
import Combine
import Shared

/// Observable holder around the shared KMP QuickNote use cases.
/// SwiftUI cannot consume suspend functions or Kotlin Flow directly,
/// so all KMP interop goes through QuickNoteMenuHelper callbacks.
@MainActor
final class QuickNoteMenuState: ObservableObject {
    @Published var notes: [QuickNote] = []
    @Published var input: String = ""
    @Published var isSaving = false

    private let helper: QuickNoteMenuHelper
    private var subscription: QuickNoteSubscription?

    init() {
        QuickNoteAppleBridge.shared.ensureKoin()
        helper = QuickNoteAppleBridge.shared.menuHelper()
    }

    func start() {
        guard subscription == nil else { return }
        subscription = helper.observeNotes { [weak self] notes in
            // Callbacks already arrive on Main, but stay safe.
            DispatchQueue.main.async {
                self?.notes = notes
            }
        }
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }

    func add() {
        let text = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !isSaving else { return }
        isSaving = true
        helper.create(content: text) { [weak self] in
            DispatchQueue.main.async {
                self?.input = ""
                self?.isSaving = false
            }
        }
    }

    func trash(_ note: QuickNote) {
        helper.moveToTrash(id: note.id)
    }

    deinit {
        subscription?.cancel()
    }
}

struct QuickNoteMenuView: View {
    @StateObject private var state = QuickNoteMenuState()

    var body: some View {
        VStack(spacing: 12) {
            Text("Quick Note").font(.headline)

            HStack(spacing: 8) {
                TextField("New note...", text: $state.input)
                    .textFieldStyle(.roundedBorder)
                    .onSubmit(state.add)

                Button(state.isSaving ? "Saving..." : "Add") {
                    state.add()
                }
                .disabled(state.input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || state.isSaving)
                .keyboardShortcut(.return, modifiers: .command)
            }

            Divider()

            if state.notes.isEmpty {
                Text("No notes yet")
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 8)
            } else {
                List {
                    ForEach(state.notes, id: \.id) { note in
                        HStack(alignment: .top, spacing: 8) {
                            Text(note.content)
                                .lineLimit(3)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Button {
                                state.trash(note)
                            } label: {
                                Image(systemName: "trash")
                            }
                            .buttonStyle(.plain)
                            .foregroundStyle(.secondary)
                            .help("Move to trash")
                        }
                        .padding(.vertical, 2)
                    }
                }
                .listStyle(.plain)
                .frame(height: min(CGFloat(state.notes.count * 44), 320))
            }

            Divider()

            Button("Quit") {
                NSApplication.shared.terminate(nil)
            }
        }
        .padding()
        .frame(width: 340)
        .onAppear { state.start() }
        .onDisappear { state.stop() }
    }
}
