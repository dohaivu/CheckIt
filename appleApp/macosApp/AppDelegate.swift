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

extension Notification.Name {
    /// Posted (main thread) with an Int object: the current NEXT-notes count.
    static let quickNoteHasItems = Notification.Name("checkit.quickNoteHasItems")
}

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {
    private var statusItem: NSStatusItem?
    private let popover = NSPopover()

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
        let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        if let button = item.button {
            button.image = NSImage(systemSymbolName: "note.text", accessibilityDescription: "Quick Note")
            button.target = self
            button.action = #selector(statusClicked(_:))
            button.sendAction(on: [.leftMouseUp, .rightMouseUp])
        }
        statusItem = item

        popover.contentSize = NSSize(width: 380, height: 600)
        popover.behavior = .transient
        popover.animates = true
        popover.contentViewController = NSHostingController(rootView: QuickNoteMenuView())
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

    private func togglePopover() {
        guard let button = statusItem?.button else { return }
        if popover.isShown {
            popover.performClose(nil)
        } else {
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
            popover.contentViewController?.view.window?.makeKey()
        }
    }

    /// Menu-bar count next to the icon, in the system accent color.
    /// Hidden when there is nothing to look at.
    func setItemCount(_ count: Int) {
        guard let button = statusItem?.button else { return }
        if count > 0 {
            button.attributedTitle = NSAttributedString(
                string: "\(count)",
                attributes: [.foregroundColor: NSColor.controlAccentColor]
            )
        } else {
            button.attributedTitle = NSAttributedString(string: "")
        }
    }

    @objc private func itemsChanged(_ notification: Notification) {
        setItemCount((notification.object as? Int) ?? 0)
    }

    private func showRightClickMenu() {
        let account = QuickNoteGoogleSignIn.shared
        let menu = NSMenu()
        if !account.isAnonymous, let email = account.email {
            let header = NSMenuItem(title: "Signed in as \(email)", action: nil, keyEquivalent: "")
            header.isEnabled = false
            menu.addItem(header)
            let signOut = NSMenuItem(title: "Sign Out", action: #selector(signOutClicked), keyEquivalent: "")
            signOut.target = self
            menu.addItem(signOut)
        } else {
            let header = NSMenuItem(title: "Not signed in — notes stay on this Mac", action: nil, keyEquivalent: "")
            header.isEnabled = false
            menu.addItem(header)
            let signIn = NSMenuItem(title: "Sign in with Google…", action: #selector(signInClicked), keyEquivalent: "")
            signIn.target = self
            menu.addItem(signIn)
        }
        menu.addItem(.separator())
        let quit = NSMenuItem(title: "Quit CheckIt", action: #selector(quitClicked), keyEquivalent: "q")
        quit.target = self
        menu.addItem(quit)

        // Showing via performClick displays the menu instead of firing action.
        statusItem?.menu = menu
        statusItem?.button?.performClick(nil)
        statusItem?.menu = nil
    }

    @objc private func signInClicked() {
        if !popover.isShown {
            togglePopover()
        }
        Task { await QuickNoteGoogleSignIn.shared.signIn() }
    }

    @objc private func signOutClicked() {
        QuickNoteGoogleSignIn.shared.signOut()
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
