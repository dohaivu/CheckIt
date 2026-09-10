//
//  QuickNoteMenuView.swift
//  appleApp
//
//  Created by DO HAI VU on 10/9/26.
//
import SwiftUI
import Combine
import Shared

/// Preset durations mirror QuickNoteRules in shared (15/30/60 min).
enum QuickNoteReminderPreset: CaseIterable {
    case min15, min30, hour1

    var durationMillis: Int64 {
        switch self {
        case .min15: 15 * 60 * 1000
        case .min30: 30 * 60 * 1000
        case .hour1: 60 * 60 * 1000
        }
    }

    var label: String {
        switch self {
        case .min15: "In 15 minutes"
        case .min30: "In 30 minutes"
        case .hour1: "In 1 hour"
        }
    }
}

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
                // Shared scheduler is a no-op on Apple targets: reconcile
                // macOS system notifications from the observed remindAt values.
                QuickNoteNotificationScheduler.sync(with: notes)
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
        QuickNoteNotificationScheduler.cancel(noteId: note.id)
        helper.moveToTrash(id: note.id)
    }

    func remind(_ note: QuickNote, preset: QuickNoteReminderPreset) {
        helper.setReminderIn(id: note.id, durationMillis: preset.durationMillis)
    }

    func remind(_ note: QuickNote, at date: Date) {
        let millis = Int64(date.timeIntervalSince1970 * 1000)
        helper.setReminderAt(id: note.id, epochMillis: millis)
    }

    func clearReminder(_ note: QuickNote) {
        QuickNoteNotificationScheduler.cancel(noteId: note.id)
        helper.clearReminder(id: note.id)
    }

    deinit {
        subscription?.cancel()
    }
}

struct QuickNoteRow: View {
    @ObservedObject var state: QuickNoteMenuState
    let note: QuickNote

    @State private var showPicker = false
    @State private var customDate = Date().addingTimeInterval(3600)

    private var remindAt: Date? {
        guard let millis = note.remindAt?.int64Value else { return nil }
        return Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(alignment: .top, spacing: 8) {
                Text(note.content)
                    .lineLimit(3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button {
                    customDate = (remindAt ?? Date()).addingTimeInterval(60)
                    showPicker = true
                } label: {
                    Image(systemName: remindAt == nil ? "alarm" : "alarm.fill")
                }
                .buttonStyle(.plain)
                .foregroundStyle(remindAt == nil ? .secondary : Color.accentColor)
                .help(remindAt == nil ? "Set reminder" : "Change reminder")
                .popover(isPresented: $showPicker, arrowEdge: .trailing) {
                    reminderPicker
                }
                Button {
                    state.trash(note)
                } label: {
                    Image(systemName: "trash")
                }
                .buttonStyle(.plain)
                .foregroundStyle(.secondary)
                .help("Move to trash")
            }
            if let fireDate = remindAt {
                Text("Reminds \(relativeString(for: fireDate))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 2)
    }

    private var reminderPicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Remind me").font(.headline)
            if let fireDate = remindAt {
                Text("Set for \(absoluteString(for: fireDate))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            ForEach(QuickNoteReminderPreset.allCases, id: \.self) { preset in
                Button(preset.label) {
                    state.remind(note, preset: preset)
                    showPicker = false
                }
                .buttonStyle(.link)
            }
            Divider()
            DatePicker("Custom time", selection: $customDate, in: Date()..., displayedComponents: [.date, .hourAndMinute])
                .datePickerStyle(.compact)
            HStack {
                Spacer()
                Button("Set custom time") {
                    state.remind(note, at: customDate)
                    showPicker = false
                }
                .disabled(customDate <= Date())
            }
            if remindAt != nil {
                Divider()
                Button("Clear reminder") {
                    state.clearReminder(note)
                    showPicker = false
                }
                .foregroundStyle(.red)
            }
        }
        .padding()
        .frame(width: 240)
    }

    private func relativeString(for date: Date) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .short
        return formatter.localizedString(for: date, relativeTo: Date())
    }

    private func absoluteString(for date: Date) -> String {
        date.formatted(date: .abbreviated, time: .shortened)
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
                        QuickNoteRow(state: state, note: note)
                    }
                }
                .listStyle(.plain)
                .frame(height: min(CGFloat(state.notes.count * 60), 340))
            }

            Divider()

            Button("Quit") {
                NSApplication.shared.terminate(nil)
            }
        }
        .padding()
        .frame(width: 360)
        .onAppear { state.start() }
        .onDisappear { state.stop() }
    }
}
