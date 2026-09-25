//
//  AppDelegate.swift
//  appleApp
//
//  Menu-bar-only hosting (no dock icon via LSUIElement): an NSStatusItem
//  shows the QuickNote popover on left-click and an account/quit menu on
//  right-click. SwiftUI's MenuBarExtra cannot distinguish click types, so
//  AppKit drives the icon directly.
//
import AppKit
import SwiftUI
import KeyboardShortcuts
import Shared

extension KeyboardShortcuts.Name {
    /// Global hotkey toggling the Quick Note menu. User-customizable via
    /// Settings (KeyboardShortcuts.Recorder).
    static let toggleQuickNote = Self("toggleQuickNote", default: .init(.n, modifiers: [.control, .option, .command]))
}

extension Notification.Name {
    /// Posted (main thread) with the current NEXT-notes array whenever it changes.
    static let quickNoteHasItems = Notification.Name("checkit.quickNoteHasItems")
    /// Posted (main thread) each time the menu popover is opened. The
    /// popover content persists, so SwiftUI onAppear fires only at startup.
    static let quickNoteMenuOpened = Notification.Name("checkit.quickNoteMenuOpened")
    /// Posted (main thread) each time the menu popover is closed.
    static let quickNoteMenuClosed = Notification.Name("checkit.quickNoteMenuClosed")
    /// Posted to request closing the menu (e.g. after starting a countdown).
    static let quickNoteCloseMenu = Notification.Name("checkit.quickNoteCloseMenu")
}

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate, NSWindowDelegate {
    private var statusItem: NSStatusItem?
    private let popover = NSPopover()
    private var lastNotes: [QuickNote] = []

    func applicationDidFinishLaunching(_ notification: Notification) {
        // Menu-bar apps are easy to launch twice (no dock icon); a second
        // process crashes opening Firestore's LevelDB LOCK, so bail early.
        if isAnotherInstanceRunning() {
            print("[CheckIt] Another instance is already running; terminating this one.")
            NSApplication.shared.terminate(nil)
            return
        }
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(itemsChanged(_:)),
            name: .quickNoteHasItems,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(closeMenuRequested(_:)),
            name: .quickNoteCloseMenu,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(countdownChanged(_:)),
            name: .quickNoteCountdownChanged,
            object: nil
        )
        let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        if let button = item.button {
            // App icon in the menu bar, scaled to status-item size.
            // Falls back to the SF Symbol when no icon is bundled.
            let icon = (NSApp.applicationIconImage?.copy() as? NSImage)
                ?? NSImage(systemSymbolName: "note.text", accessibilityDescription: nil)
            icon?.size = NSSize(width: 24, height: 24)
            icon?.accessibilityDescription = "Quick Note"
            button.image = icon
            button.target = self
            button.action = #selector(statusClicked(_:))
            button.sendAction(on: [.leftMouseUp, .rightMouseUp])
        }
        statusItem = item

        popover.contentSize = NSSize(width: 380, height: 600)
        popover.behavior = .transient
        popover.animates = true
        popover.contentViewController = NSHostingController(rootView: QuickNoteMenuView())

        // Global hotkey (works from any app): toggles the menu open/closed.
        // Restore the locally saved shortcut first so a customization
        // survives app restarts.
        QuickNoteShortcutStore.restore()
        KeyboardShortcuts.onKeyUp(for: .toggleQuickNote) { [weak self] in
            Task { @MainActor [weak self] in self?.togglePopover() }
        }
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        // A regular activation policy is used only while an auxiliary window
        // is visible. Closing it should restore menu-bar-only behavior, not
        // quit the status-item app.
        false
    }

    // MARK: - Clicks

    private func isAnotherInstanceRunning() -> Bool {
        let myPid = ProcessInfo.processInfo.processIdentifier
        let bundleId = Bundle.main.bundleIdentifier
        return NSWorkspace.shared.runningApplications.contains {
            $0.bundleIdentifier == bundleId && $0.processIdentifier != myPid
        }
    }

    @objc private func statusClicked(_ sender: NSStatusBarButton) {
        guard let event = NSApp.currentEvent else { return }
        if event.type == .rightMouseUp {
            showRightClickMenu()
        } else {
            togglePopover()
        }
    }

    func togglePopover() {
        guard let button = statusItem?.button else { return }
        if popover.isShown {
            // Note: performClose is an NSWindow API and a no-op on NSPopover.
            popover.close()
            uninstallEscCloser()
            NotificationCenter.default.post(name: .quickNoteMenuClosed, object: nil)
        } else {
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
            popover.contentViewController?.view.window?.makeKey()
            installEscCloser()
            NotificationCenter.default.post(name: .quickNoteMenuOpened, object: nil)
        }
    }

    // MARK: - Esc to close

    private var escMonitor: Any?

    /// Transient popovers don't reliably dismiss on Esc in LSUIElement apps;
    /// watch key-downs locally while open (removed on close).
    private func installEscCloser() {
        uninstallEscCloser()
        escMonitor = NSEvent.addLocalMonitorForEvents(matching: .keyDown) { [weak self] event in
            if event.keyCode == 53, self?.popover.isShown == true {
                self?.togglePopover()
                return nil
            }
            return event
        }
    }

    private func uninstallEscCloser() {
        if let monitor = escMonitor {
            NSEvent.removeMonitor(monitor)
            escMonitor = nil
        }
    }

    /// Menu-bar status: accent-colored count plus the top note's text
    /// (starred first, then sortOrder; first line, max 20 chars).
    /// Hidden when there is nothing to look at.
    func setStatusText(count: Int, text: String) {
        guard let button = statusItem?.button else { return }
        guard count > 0 else {
            button.attributedTitle = NSAttributedString(string: "")
            return
        }
        let status = NSMutableAttributedString(
            string: "\(count) ",
            attributes: [.foregroundColor: NSColor.controlAccentColor]
        )
        status.append(NSAttributedString(string: text))
        button.attributedTitle = status
    }

    @objc private func itemsChanged(_ notification: Notification) {
        lastNotes = notification.object as? [QuickNote] ?? []
        renderStatus()
    }

    @objc private func closeMenuRequested(_ notification: Notification) {
        if popover.isShown {
            togglePopover()
        }
    }

    @objc private func countdownChanged(_ notification: Notification) {
        renderStatus()
    }

    /// Menu-bar status: a running countdown takes over ("12:34 Buy milk"),
    /// otherwise count + top item text. Hidden when idle and empty.
    private func renderStatus() {
        if QuickNoteCountdown.shared.isRunning {
            let countdown = QuickNoteCountdown.shared
            setCountdownText(time: countdown.timeLabel, text: countdown.noteText)
            return
        }
        let notes = lastNotes
        let top = notes.sorted {
            let leftStar = $0.priority == TaskPriority.high
            let rightStar = $1.priority == TaskPriority.high
            if leftStar != rightStar { return leftStar }
            if $0.sortOrder != $1.sortOrder { return $0.sortOrder < $1.sortOrder }
            return $0.id < $1.id
        }.first
        let firstLine = top.map { String($0.content.split(separator: "\n", maxSplits: 1).first ?? "") } ?? ""
        setStatusText(count: notes.count, text: String(firstLine.prefix(30)))
    }

    private func setCountdownText(time: String, text: String) {
        guard let button = statusItem?.button else { return }
        let digits = NSFont.monospacedDigitSystemFont(
            ofSize: button.font?.pointSize ?? NSFont.systemFontSize,
            weight: .regular
        )
        let status = NSMutableAttributedString(
            string: "\(time) ",
            attributes: [
                .foregroundColor: NSColor.controlAccentColor,
                .font: digits,
            ]
        )
        status.append(NSAttributedString(string: text))
        button.attributedTitle = status
    }

    private func showRightClickMenu() {
        let account = QuickNoteGoogleSignIn.shared
        let menu = NSMenu()
        if !account.isAnonymous, let email = account.email {
            let header = NSMenuItem(title: "Signed in as \(email)", action: nil, keyEquivalent: "")
            header.isEnabled = false
            menu.addItem(header)
        } else {
            let header = NSMenuItem(title: "Not signed in — see Settings to sync", action: nil, keyEquivalent: "")
            header.isEnabled = false
            menu.addItem(header)
        }
        menu.addItem(.separator())
        let nested = NSMenuItem(title: "Nested Lists…", action: #selector(nestedListsClicked), keyEquivalent: "l")
        nested.target = self
        menu.addItem(nested)
        let settings = NSMenuItem(title: "Settings…", action: #selector(settingsClicked), keyEquivalent: ",")
        settings.target = self
        menu.addItem(settings)
        menu.addItem(.separator())
        let quit = NSMenuItem(title: "Quit CheckIt", action: #selector(quitClicked), keyEquivalent: "q")
        quit.target = self
        menu.addItem(quit)

        // Showing via performClick displays the menu instead of firing action.
        statusItem?.menu = menu
        statusItem?.button?.performClick(nil)
        statusItem?.menu = nil
    }

    // MARK: - Settings window

    private var settingsWindow: NSWindow?
    private var nestedListsWindow: NSWindow?

    /// Keep the app out of the Dock while it acts only as a menu-bar app,
    /// but promote it to a regular app whenever one of its windows is shown.
    private func updateActivationPolicyForWindows() {
        let hasVisibleWindow = [settingsWindow, nestedListsWindow].contains { $0?.isVisible == true }
        NSApp.setActivationPolicy(hasVisibleWindow ? .regular : .accessory)
    }

    func windowWillClose(_ notification: Notification) {
        // The window remains visible until AppKit finishes the close cycle.
        DispatchQueue.main.async { [weak self] in
            self?.updateActivationPolicyForWindows()
        }
    }

    @objc private func nestedListsClicked() {
        if nestedListsWindow == nil {
            let window = NSWindow(
                contentRect: NSRect(x: 0, y: 0, width: 1000, height: 700),
                styleMask: [.titled, .closable, .resizable, .miniaturizable],
                backing: .buffered,
                defer: false
            )
            window.title = "Nested Lists"
            window.minSize = NSSize(width: 760, height: 480)
            window.contentViewController = NSHostingController(rootView: NestedListsWindowView())
            window.delegate = self
            window.center()
            // Don't die with the popover: closing the window must not quit
            // the menu-bar app, and reopening reuses the same window.
            window.isReleasedWhenClosed = false
            nestedListsWindow = window
        }
        NSApp.setActivationPolicy(.regular)
        NSApp.activate()
        nestedListsWindow?.makeKeyAndOrderFront(nil)
    }

    @objc private func settingsClicked() {
        if settingsWindow == nil {
            let window = NSWindow(
                contentRect: NSRect(x: 0, y: 0, width: 380, height: 200),
                styleMask: [.titled, .closable],
                backing: .buffered,
                defer: false
            )
            window.title = "CheckIt Settings"
            window.contentViewController = NSHostingController(rootView: SettingsView())
            window.delegate = self
            window.center()
            settingsWindow = window
        }
        NSApp.setActivationPolicy(.regular)
        NSApp.activate()
        settingsWindow?.makeKeyAndOrderFront(nil)
    }

    @objc private func quitClicked() {
        NSApplication.shared.terminate(nil)
    }

    // MARK: - OAuth callback

    func application(_ application: NSApplication, open urls: [URL]) {
        for url in urls where QuickNoteGoogleSignIn.shared.handle(url: url) {
            break
        }
    }
}
