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
final class QuickNoteNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}

enum QuickNoteNotificationScheduler {
    private static let delegate = QuickNoteNotificationDelegate()

    static func configure() {
        let center = UNUserNotificationCenter.current()
        center.delegate = delegate
        center.requestAuthorization(options: [.alert, .sound]) { _, _ in }
    }

    static func identifier(for noteId: String) -> String {
        "quicknote-\(noteId)"
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
            // Also drop delivered banners for reminders that no longer exist.
            center.getDeliveredNotifications { delivered in
                let staleDelivered = delivered.map(\.request.identifier).filter { !wantedIds.contains($0) }
                if !staleDelivered.isEmpty {
                    center.removeDeliveredNotifications(withIdentifiers: staleDelivered)
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
