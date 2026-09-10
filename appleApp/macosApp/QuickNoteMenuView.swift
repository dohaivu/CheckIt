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

/// Text mirrors shared formatReminder/formatRemaining in QuickNoteScreen.kt.
enum QuickNoteRowText {
    static func reminderText(remindAtMillis: Int64) -> String {
        let remaining = max(0, remindAtMillis - nowMillis())
        let minutes = remaining / 60_000
        if minutes < 60 { return "in \(max(1, minutes))m" }
        return "in \(minutes / 60)h"
    }

    static func remainingText(deleteAtMillis: Int64) -> String {
        let remaining = max(0, deleteAtMillis - nowMillis())
        let hours = remaining / 3_600_000
        if hours >= 1 { return "\(hours)h" }
        return "\(max(1, remaining / 60_000))m"
    }

    private static func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}

/// Observable holder around the shared KMP QuickNote use cases.
/// SwiftUI cannot consume suspend functions or Kotlin Flow directly,
/// so all KMP interop goes through QuickNoteMenuHelper callbacks.
/// Section layout mirrors QuickNoteContent in shared.
@MainActor
final class QuickNoteMenuState: ObservableObject {
    /// Same throttle as QuickNoteViewModel (QUICK_NOTE_REFRESH_MIN_INTERVAL_MILLIS).
    private static let refreshMinIntervalMillis: Int64 = 5 * 60 * 1000

    @Published var notes: [QuickNote] = []
    @Published var deletedNotes: [QuickNote] = []
    @Published var input: String = ""
    @Published var isSaving = false

    private let helper: QuickNoteMenuHelper
    private var notesSubscription: QuickNoteSubscription?
    private var deletedSubscription: QuickNoteSubscription?
    private var lastRefreshMillis: Int64 = 0

    init() {
        QuickNoteAppleBridge.shared.ensureKoin()
        helper = QuickNoteAppleBridge.shared.menuHelper()
    }

    func start() {
        if notesSubscription == nil {
            notesSubscription = helper.observeNotes { [weak self] notes in
                // Callbacks already arrive on Main, but stay safe.
                DispatchQueue.main.async {
                    self?.notes = notes
                    // Shared scheduler is a no-op on Apple targets: reconcile
                    // macOS system notifications from the observed remindAt values.
                    QuickNoteNotificationScheduler.sync(with: notes)
                }
            }
        }
        if deletedSubscription == nil {
            deletedSubscription = helper.observeDeletedNotes { [weak self] notes in
                DispatchQueue.main.async {
                    self?.deletedNotes = notes
                }
            }
        }
        // Fired reminders clear their DB flag like Android's receiver;
        // the banner itself stays in Notification Center.
        QuickNoteNotificationScheduler.onReminderDelivered = { [weak self] id in
            self?.helper.clearReminder(id: id)
        }
        // Every menu open is an explicit user request: force maintenance so
        // expiry, auto-trash, and fired-reminder cleanup apply immediately
        // instead of waiting out the 5-minute throttle.
        refresh(force: true)
    }

    func stop() {
        notesSubscription?.cancel()
        notesSubscription = nil
        deletedSubscription?.cancel()
        deletedSubscription = nil
        QuickNoteNotificationScheduler.onReminderDelivered = nil
    }

    /// Full maintenance (auto-trash, expiry, fired-reminder cleanup, sync),
    /// mirroring QuickNoteViewModel.refresh().
    func refresh(force: Bool = false) {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        if !force && now - lastRefreshMillis < Self.refreshMinIntervalMillis { return }
        lastRefreshMillis = now
        helper.refresh()
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
        QuickNoteFirestoreSync.shared.requestSync()
    }

    func trash(_ note: QuickNote) {
        QuickNoteNotificationScheduler.cancel(noteId: note.id)
        helper.moveToTrash(id: note.id)
        QuickNoteFirestoreSync.shared.requestSync()
    }

    func restore(_ note: QuickNote) {
        helper.restore(id: note.id)
        QuickNoteFirestoreSync.shared.requestSync()
    }

    func deleteForever(_ note: QuickNote) {
        QuickNoteNotificationScheduler.cancel(noteId: note.id)
        helper.deletePermanently(id: note.id)
        QuickNoteFirestoreSync.shared.requestSync()
    }

    func remind(_ note: QuickNote, preset: QuickNoteReminderPreset) {
        helper.setReminderIn(id: note.id, durationMillis: preset.durationMillis)
        QuickNoteFirestoreSync.shared.requestSync()
    }

    func remind(_ note: QuickNote, at date: Date) {
        let millis = Int64(date.timeIntervalSince1970 * 1000)
        helper.setReminderAt(id: note.id, epochMillis: millis)
        QuickNoteFirestoreSync.shared.requestSync()
    }

    func clearReminder(_ note: QuickNote) {
        QuickNoteNotificationScheduler.cancel(noteId: note.id)
        helper.clearReminder(id: note.id)
        QuickNoteFirestoreSync.shared.requestSync()
    }

    deinit {
        notesSubscription?.cancel()
        deletedSubscription?.cancel()
    }
}

/// NEXT row: content plus an alarm badge showing the shared
/// "in Xm"/"in Xh" format. Tapping the badge opens the time picker.
struct QuickNoteRow: View {
    @ObservedObject var state: QuickNoteMenuState
    let note: QuickNote

    @State private var showPicker = false
    @State private var customDate = Date().addingTimeInterval(3600)

    private var remindAtMillis: Int64? { note.remindAt?.int64Value }

    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            Text(note.content)
                .lineLimit(3)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                let base = remindAtMillis
                    .map { Date(timeIntervalSince1970: TimeInterval($0) / 1000.0) } ?? Date()
                customDate = base.addingTimeInterval(60)
                showPicker = true
            } label: {
                if let millis = remindAtMillis {
                    HStack(spacing: 4) {
                        Image(systemName: "alarm.fill")
                        Text(QuickNoteRowText.reminderText(remindAtMillis: millis))
                            .font(.caption)
                    }
                    .foregroundStyle(Color.accentColor)
                } else {
                    Image(systemName: "alarm")
                        .foregroundStyle(.secondary)
                }
            }
            .buttonStyle(.plain)
            .help(remindAtMillis == nil ? "Set reminder" : "Change reminder")
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
        .padding(.vertical, 2)
    }

    private var reminderPicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Remind me").font(.headline)
            if let millis = remindAtMillis {
                Text("Set for \(absoluteString(millis: millis))")
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
            if remindAtMillis != nil {
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

    private func absoluteString(millis: Int64) -> String {
        Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0)
            .formatted(date: .abbreviated, time: .shortened)
    }
}

/// TO BE DELETED row: content plus remaining lifetime ("23h"/"59m"),
/// with restore and permanent-delete actions.
struct QuickNoteDeletedRow: View {
    @ObservedObject var state: QuickNoteMenuState
    let note: QuickNote

    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            Text(note.content)
                .lineLimit(3)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let millis = note.deleteAt?.int64Value {
                Text(QuickNoteRowText.remainingText(deleteAtMillis: millis))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Button {
                state.restore(note)
            } label: {
                Image(systemName: "arrow.uturn.backward")
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
            .help("Restore")
            Button {
                state.deleteForever(note)
            } label: {
                Image(systemName: "trash.fill")
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
            .help("Delete forever")
        }
        .padding(.vertical, 2)
    }
}

struct QuickNoteMenuView: View {
    @StateObject private var state = QuickNoteMenuState()
    @ObservedObject private var sync = QuickNoteFirestoreSync.shared

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text("Quick Note").font(.headline)
                Text("\(state.notes.count)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Spacer()
                Button {
                    state.refresh(force: true)
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .buttonStyle(.plain)
                .foregroundStyle(.secondary)
                .help("Refresh")
            }

            List {
                Section("NEXT") {
                    if state.notes.isEmpty {
                        Text("Nothing here. Capture a thought below.")
                            .foregroundStyle(.secondary)
                            .font(.callout)
                    } else {
                        ForEach(state.notes, id: \.id) { note in
                            QuickNoteRow(state: state, note: note)
                        }
                    }
                }
                Section("TO BE DELETED") {
                    if state.deletedNotes.isEmpty {
                        Text("Deleted notes disappear after 24 hours.")
                            .foregroundStyle(.secondary)
                            .font(.callout)
                    } else {
                        ForEach(state.deletedNotes, id: \.id) { note in
                            QuickNoteDeletedRow(state: state, note: note)
                        }
                    }
                }
            }
            .listStyle(.plain)
            .frame(height: listHeight)

            Divider()

            // Capture bar (bottom, like QuickCaptureBar in QuickNoteContent).
            HStack(spacing: 8) {
                TextField("Capture a thought...", text: $state.input)
                    .textFieldStyle(.roundedBorder)
                    .onSubmit(state.add)

                Button(state.isSaving ? "Saving..." : "Add") {
                    state.add()
                }
                .disabled(state.input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || state.isSaving)
                .keyboardShortcut(.return, modifiers: .command)
            }

            Button("Quit") {
                NSApplication.shared.terminate(nil)
            }

            syncStatusLine
        }
        .padding()
        .frame(width: 380)
        .onAppear {
            state.start()
            QuickNoteFirestoreSync.shared.requestSync()
        }
        .onDisappear { state.stop() }
    }

    @ViewBuilder
    private var syncStatusLine: some View {
        switch sync.uiState.status {
        case .idle:
            EmptyView()
        case .syncing:
            Text("Syncing…").font(.caption).foregroundStyle(.secondary)
        case .synced:
            if let at = sync.uiState.lastSyncedAt {
                Text("Synced \(at.formatted(date: .omitted, time: .shortened))")
                    .font(.caption).foregroundStyle(.secondary)
            } else {
                Text("Synced").font(.caption).foregroundStyle(.secondary)
            }
        case .offline:
            Text("You're offline. Changes are saved on this device.")
                .font(.caption).foregroundStyle(.secondary)
        case .error:
            Text(sync.uiState.message ?? "Sync failed. Will retry automatically.")
                .font(.caption).foregroundStyle(.secondary)
        }
    }

    private var listHeight: CGFloat {
        let rows = state.notes.count + state.deletedNotes.count
        return min(CGFloat(rows * 52 + 130), 430)
    }
}
