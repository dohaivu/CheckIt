//
//  QuickNoteNotifications.swift
//  appleApp
//
//  macOS system notifications for QuickNote reminders.
//  The shared KMP QuickNoteReminderScheduler is a no-op on Apple targets,
//  so scheduling lives here: every observed note list is reconciled with
//  UNUserNotificationCenter (one pending request per note id).
//
import AppKit
import UserNotifications
import Shared

/// Presents notifications even while the menu-bar app is frontmost.
/// Mirrors Android's QuickNoteReminderReceiver: once the reminder is
/// displayed, its callback clears the reminder from the shared database.
final class QuickNoteNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        if let noteId = QuickNoteNotificationScheduler.noteId(from: notification.request.identifier) {
            QuickNoteNotificationScheduler.onReminderDelivered?(noteId)
        }
        completionHandler([.banner, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let noteId = QuickNoteNotificationScheduler.noteId(from: response.notification.request.identifier) {
            QuickNoteNotificationScheduler.onReminderDelivered?(noteId)
        }
        completionHandler()
    }
}

enum QuickNoteNotificationScheduler {
    private static let delegate = QuickNoteNotificationDelegate()
    private static let idPrefix = "quicknote-"

    /// Called with the note id after one of its reminders is displayed,
    /// so the reminder can be cleared like Android's receiver does.
    static var onReminderDelivered: ((String) -> Void)?

    static func configure() {
        let center = UNUserNotificationCenter.current()
        center.delegate = delegate
        center.requestAuthorization(options: [.alert, .sound]) { _, _ in }
    }

    static func identifier(for noteId: String) -> String {
        "\(idPrefix)\(noteId)"
    }

    static func noteId(from identifier: String) -> String? {
        guard identifier.hasPrefix(idPrefix) else { return nil }
        return String(identifier.dropFirst(idPrefix.count))
    }

    /// Reconcile pending notifications with the current NEXT notes.
    /// Notes with a future remindAt get (re)scheduled; everything else
    /// is removed so trashed/cleared/past reminders never fire.
    static func sync(with notes: [QuickNote]) {
        let center = UNUserNotificationCenter.current()
        let nowMillis = Int64(Date().timeIntervalSince1970 * 1000)

        var wanted: [String: UNNotificationRequest] = [:]
        for note in notes {
            guard let fireMillis = note.remindAt?.int64Value, fireMillis > nowMillis else { continue }
            let interval = TimeInterval(fireMillis - nowMillis) / 1000.0
            let content = UNMutableNotificationContent()
            content.title = "Quick Note reminder"
            content.body = String(note.content.prefix(200))
            content.sound = .default
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, interval), repeats: false)
            let request = UNNotificationRequest(
                identifier: identifier(for: note.id),
                content: content,
                trigger: trigger
            )
            wanted[request.identifier] = request
        }

        center.getPendingNotificationRequests { pending in
            let pendingIds = Set(pending.map(\.identifier))
            let wantedIds = Set(wanted.keys)
            // Remove stale requests (cleared, trashed, or past reminders).
            let stale = pendingIds.subtracting(wantedIds)
            if !stale.isEmpty {
                center.removePendingNotificationRequests(withIdentifiers: Array(stale))
            }
            // Drop delivered banners only for notes that are gone (trashed).
            // A just-fired reminder keeps its banner like on Android, while
            // its remindAt is cleared from the database via onReminderDelivered.
            let liveIds = Set(notes.map { identifier(for: $0.id) })
            center.getDeliveredNotifications { delivered in
                let orphaned = delivered.map(\.request.identifier).filter { !liveIds.contains($0) }
                if !orphaned.isEmpty {
                    center.removeDeliveredNotifications(withIdentifiers: orphaned)
                }
            }
            // (Re)schedule wanted reminders.
            for request in wanted.values {
                center.add(request)
            }
        }
    }

    static func cancel(noteId: String) {
        let id = identifier(for: noteId)
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [id])
        center.removeDeliveredNotifications(withIdentifiers: [id])
    }
}
