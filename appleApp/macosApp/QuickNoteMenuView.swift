//
//  QuickNoteMenuView.swift
//  appleApp
//
//  Created by DO HAI VU on 10/9/26.
//
import SwiftUI
import Combine
import UniformTypeIdentifiers
import AppKit
import Shared

/// Reminder presets backed by QuickNoteRules durations in shared.
/// Badge text comes from QuickNoteDisplayText so both platforms match.
enum QuickNoteReminderPreset: CaseIterable {
    case min15, min30, hour1

    var durationMillis: Int64 {
        switch self {
        case .min15: QuickNoteRules.shared.REMINDER_15_MIN_MILLIS
        case .min30: QuickNoteRules.shared.REMINDER_30_MIN_MILLIS
        case .hour1: QuickNoteRules.shared.REMINDER_1_HOUR_MILLIS
        }
    }

    var label: String {
        switch self {
        case .min15: "In 15 minutes"
        case .min30: "In 30 minutes"
        case .hour1: "In 1 hour"
        }
    }

    var shortLabel: String {
        switch self {
        case .min15: "15m"
        case .min30: "30m"
        case .hour1: "1h"
        }
    }
}

enum QuickNoteRowText {
    private static let text = QuickNoteDisplayText.shared

    static func reminderText(remindAtMillis: Int64, nowMillis: Int64) -> String {
        text.reminderText(remindAt: remindAtMillis, now: nowMillis)
    }

    static func remainingText(deleteAt: KotlinLong?, nowMillis: Int64) -> String {
        text.remainingText(deleteAt: deleteAt, now: nowMillis)
    }
}

/// Observable holder around the shared KMP QuickNote use cases.
/// SwiftUI cannot consume suspend functions or Kotlin Flow directly,
/// so all KMP interop goes through QuickNoteMenuHelper callbacks.
/// Sync triggering is repository-owned (Android parity): mutations sync
/// via AppleQuickNoteSyncManager, so this layer never requests syncs.
/// Section layout mirrors QuickNoteContent in shared.
@MainActor
final class QuickNoteMenuState: ObservableObject {
    /// Same throttle as QuickNoteViewModel (QUICK_NOTE_REFRESH_MIN_INTERVAL_MILLIS).
    private static let refreshMinIntervalMillis: Int64 = 60 * 60 * 1000

    @Published var notes: [QuickNote] = []
    @Published var deletedNotes: [QuickNote] = []
    @Published var input: String = ""
    @Published var isSaving = false
    /// Ticks forward on menu open (and slowly while open) so time-derived
    /// row text ("in 15m") recomputes even when the notes array is unchanged.
    @Published var nowTickMillis: Int64 = 0

    private let helper: QuickNoteMenuHelper
    private var notesSubscription: QuickNoteSubscription?
    private var deletedSubscription: QuickNoteSubscription?
    private var lastRefreshMillis: Int64 = 0

    init() {
        QuickNoteAppleBridge.shared.ensureKoin()
        helper = QuickNoteAppleBridge.shared.menuHelper()
        nowTickMillis = Self.nowMillis()
    }

    func start() {
        if notesSubscription == nil {
            notesSubscription = helper.observeNotes { [weak self] notes in
                // Callbacks already arrive on Main, but stay safe.
                DispatchQueue.main.async {
                    self?.notes = notes
                    // Menu-bar count next to the icon.
                    NotificationCenter.default.post(name: .quickNoteHasItems, object: notes.count)
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
        
        // Menu opens render local Room data only (pushed live by the flows
        // above). Maintenance stays throttled like QuickNoteViewModel, and
        // no Firestore sync is triggered here — syncs happen on local edits,
        // reconnect, app launch, or the header refresh button.
        refresh()
    }

    /// Runs on every menu open: sweep fired reminders, then throttled
    /// maintenance. Called from .quickNoteMenuOpened, not onAppear.
    func menuOpened() {
        nowTickMillis = Self.nowMillis()
        helper.clearExpiredReminders()
        refresh()
    }

    /// Slow tick while the menu stays open, keeping relative times fresh.
    func tick() {
        nowTickMillis = Self.nowMillis()
    }

    private static func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }

    func stop() {
        notesSubscription?.cancel()
        notesSubscription = nil
        deletedSubscription?.cancel()
        deletedSubscription = nil
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
    }

    func trash(_ note: QuickNote) {
        QuickNoteNotificationScheduler.cancel(noteId: note.id)
        helper.moveToTrash(id: note.id)
    }

    func restore(_ note: QuickNote) {
        helper.restore(id: note.id)
    }

    func deleteForever(_ note: QuickNote) {
        QuickNoteNotificationScheduler.cancel(noteId: note.id)
        helper.deletePermanently(id: note.id)
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

    /// Persist a drag-and-drop position change within NEXT. The list is
    /// reordered optimistically (like Android's dragOrder) and the shared
    /// flow corrects it once Room echoes the new sortOrder.
    /// `from` is the dragged row's index at drag start, `ontoId` the drop
    /// target row. A single write, like Android's commitDrag.
    func drop(draggedId: String, from: Int, ontoId: String) {
        guard draggedId != ontoId,
              notes.indices.contains(from),
              notes[from].id == draggedId,
              let target = notes.firstIndex(where: { $0.id == ontoId })
        else { return }
        // Post-removal insertion offset (same convention as Array.move and
        // RoomQuickNoteRepository.reorder): dropping onto a row inserts
        // the dragged row before it.
        let to = from < target ? target - 1 : target
        guard from != to else { return }
        notes.move(fromOffsets: IndexSet(integer: from), toOffset: to)
        helper.reorder(fromIndex: Int32(from), toIndex: Int32(to))
    }

    deinit {
        notesSubscription?.cancel()
        deletedSubscription?.cancel()
    }
}

/// Image attachment thumbnail, mirroring Android's QuickNoteThumbnail.
/// Prefers the device-local file (populated by Firestore sync downloads);
/// falls back to the remote URL. Nothing renders without either.
struct QuickNoteThumbnail: View {
    let localPath: String?
    let remoteURL: URL?

    @State private var localImage: NSImage?

    var body: some View {
        Group {
            if let image = localImage {
                Image(nsImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } else if let url = remoteURL {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().aspectRatio(contentMode: .fill)
                    case .failure, .empty:
                        Color.secondary.opacity(0.15)
                    @unknown default:
                        Color.secondary.opacity(0.15)
                    }
                }
            } else {
                Color.secondary.opacity(0.15)
            }
        }
        .frame(width: 40, height: 40)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .task(id: localPath) { loadLocal() }
    }

    private func loadLocal() {
        guard let path = localPath, !path.isEmpty,
              FileManager.default.fileExists(atPath: path),
              let image = NSImage(contentsOfFile: path)
        else {
            localImage = nil
            return
        }
        localImage = image
    }
}

/// NEXT row: content plus an alarm badge showing the shared
/// "in Xm"/"in Xh" format. Tapping the badge opens the time picker.
struct QuickNoteRow: View {
    @ObservedObject var state: QuickNoteMenuState
    let note: QuickNote
    let nowMillis: Int64

    @State private var showPicker = false
    @State private var customDate = Date().addingTimeInterval(3600)

    private var remindAtMillis: Int64? { note.remindAt?.int64Value }

    var body: some View {
        HStack(alignment: .center, spacing: 8) {            
            Text(note.content)
                .lineLimit(3)
                .frame(maxWidth: .infinity, alignment: .leading)
            if note.type != QuickNoteType.text,
               note.attachmentLocalPath != nil || note.attachmentUrl != nil {
                QuickNoteThumbnail(
                    localPath: note.attachmentLocalPath,
                    remoteURL: note.attachmentUrl.flatMap(URL.init(string:))
                )
            }
            Button {
                let base = remindAtMillis
                    .map { Date(timeIntervalSince1970: TimeInterval($0) / 1000.0) } ?? Date()
                customDate = base.addingTimeInterval(60)
                showPicker = true
            } label: {
                if let millis = remindAtMillis {
                    HStack(spacing: 4) {
                        Image(systemName: "alarm.fill")
                        Text(QuickNoteRowText.reminderText(remindAtMillis: millis, nowMillis: nowMillis))
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
            HStack(spacing: 8) {
                ForEach(QuickNoteReminderPreset.allCases, id: \.self) { preset in
                    Button(preset.shortLabel) {
                        state.remind(note, preset: preset)
                        showPicker = false
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
            }
            HStack {
                DatePicker("", selection: $customDate, in: Date()..., displayedComponents: [.date, .hourAndMinute])
                    .labelsHidden()
                    .datePickerStyle(.compact)
                Button("Set") {
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
    let nowMillis: Int64

    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            Text(note.content)
                .lineLimit(3)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            if note.type != QuickNoteType.text,
               note.attachmentLocalPath != nil || note.attachmentUrl != nil {
                QuickNoteThumbnail(
                    localPath: note.attachmentLocalPath,
                    remoteURL: note.attachmentUrl.flatMap(URL.init(string:))
                )
            }
            if note.deleteAt != nil {
                Text(QuickNoteRowText.remainingText(deleteAt: note.deleteAt, nowMillis: nowMillis))
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
    @State private var tickTimer: Timer?

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
                    QuickNoteFirestoreSync.shared.syncNow()
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .buttonStyle(.plain)
                .foregroundStyle(.secondary)
                .help("Refresh")
            }

            ScrollView {
                VStack(alignment: .leading, spacing: 6) {
                    Text("NEXT")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(.secondary)
                    if state.notes.isEmpty {
                        Text("Nothing here. Capture a thought below.")
                            .foregroundStyle(.secondary)
                            .font(.callout)
                    } else {
                        LazyVStack(spacing: 0) {
                            ForEach(Array(state.notes.enumerated()), id: \.element.id) { index, note in
                                QuickNoteRow(state: state, note: note, nowMillis: state.nowTickMillis)
                                    .padding(.vertical, 4)
                                    .contentShape(Rectangle())
                                    .onDrag {
                                        // Pure payload, no state mutation: mutating
                                        // view state here tears down the rows and
                                        // kills the drag session on macOS.
                                        NSItemProvider(object: "\(index):\(note.id)" as NSString)
                                    }
                                    .onDrop(of: [.text], isTargeted: nil) { (providers: [NSItemProvider]) -> Bool in
                                        guard let provider = providers.first else { return false }
                                        _ = provider.loadObject(ofClass: NSString.self) { payload, _ in
                                            guard let payload = payload as? String else { return }
                                            let parts = payload.split(separator: ":", maxSplits: 1).map(String.init)
                                            guard parts.count == 2, let from = Int(parts[0]) else { return }
                                            DispatchQueue.main.async {
                                                state.drop(draggedId: parts[1], from: from, ontoId: note.id)
                                            }
                                        }
                                        return true
                                    }
                                Divider()
                            }
                        }
                    }
                    Text("TO BE DELETED")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(.secondary)
                        .padding(.top, 6)
                    if state.deletedNotes.isEmpty {
                        Text("Deleted notes disappear after 24 hours.")
                            .foregroundStyle(.secondary)
                            .font(.callout)
                    } else {
                        LazyVStack(spacing: 0) {
                            ForEach(state.deletedNotes, id: \.id) { note in
                                QuickNoteDeletedRow(state: state, note: note, nowMillis: state.nowTickMillis)
                                    .padding(.vertical, 4)
                                Divider()
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
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

            syncStatusLine
        }
        .padding()
        .frame(width: 380)
        .onAppear {
            state.start()
        }
        .onDisappear { state.stop() }
        .onReceive(NotificationCenter.default.publisher(for: .quickNoteMenuOpened)) { _ in
            state.menuOpened()
            // Slow tick while open so relative times stay fresh; stopped on close.
            tickTimer?.invalidate()
            tickTimer = Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { _ in
                state.tick()
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .quickNoteMenuClosed)) { _ in
            tickTimer?.invalidate()
            tickTimer = nil
        }
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
