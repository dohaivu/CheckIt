//
//  QuickNoteCountdown.swift
//  appleApp
//
//  Simple in-memory countdown for one note: pick a duration, watch it tick
//  live in the menu bar next to the note text, get a banner at zero.
//  No storage, no sync, no pause — starting again replaces the running one,
//  quitting the app drops it.
//
import AppKit
import Combine
import UserNotifications

extension Notification.Name {
    /// Posted (main thread) on start, every tick, and stop/finish.
    static let quickNoteCountdownChanged = Notification.Name("checkit.quickNoteCountdownChanged")
}

@MainActor
final class QuickNoteCountdown: ObservableObject {
    static let shared = QuickNoteCountdown()

    @Published private(set) var isRunning = false
    @Published private(set) var remainingSeconds = 0
    @Published private(set) var noteText = ""

    /// "12:34 Buy milk" for the popup banner.
    var displayText: String {
        "\(timeLabel) \(noteText)"
    }

    /// "12:34" alone, for the accent-colored run in the menu bar.
    var timeLabel: String {
        Self.clockLabel(remainingSeconds)
    }

    private var timer: Timer?
    private var endsAt = Date()

    func start(content: String, durationSeconds: Int) {
        guard durationSeconds > 0 else { return }
        stop(silent: true)
        let firstLine = content.split(separator: "\n", maxSplits: 1).first.map(String.init) ?? ""
        noteText = String(firstLine.prefix(20))
        endsAt = Date().addingTimeInterval(TimeInterval(durationSeconds))
        isRunning = true
        tick()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor [weak self] in self?.tick() }
        }
        notifyChanged()
    }

    func stop() {
        stop(silent: false)
    }

    private func stop(silent: Bool) {
        timer?.invalidate()
        timer = nil
        let wasRunning = isRunning
        isRunning = false
        remainingSeconds = 0
        if wasRunning, !silent {
            notifyChanged()
        }
    }

    private func tick() {
        guard isRunning else { return }
        remainingSeconds = max(0, Int(endsAt.timeIntervalSinceNow.rounded()))
        notifyChanged()
        if remainingSeconds == 0 {
            finish()
        }
    }

    private func finish() {
        let text = noteText
        stop(silent: true)
        notifyChanged()
        let content = UNMutableNotificationContent()
        content.title = "Time's up"
        content.body = text.isEmpty ? "Countdown finished." : text
        content.sound = .default
        let request = UNNotificationRequest(
            identifier: "quicknote-countdown",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }

    private func notifyChanged() {
        NotificationCenter.default.post(name: .quickNoteCountdownChanged, object: nil)
    }

    static func clockLabel(_ totalSeconds: Int) -> String {
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%02d:%02d", minutes, seconds)
        }
    }
}
