//
//  QuickNoteShortcutStore.swift
//  appleApp
//
//  Local backup of the user-customized global hotkey. KeyboardShortcuts
//  persists the recorder value itself, but the backup below is restored
//  explicitly on launch so a customized shortcut survives app restarts.
//

import Foundation
import KeyboardShortcuts

enum QuickNoteShortcutStore {
    /// Local backup key for the Toggle Quick Note hotkey.
    private static let backupKey = "checkit.toggleQuickNote.shortcut"

    /// Mirror the current recorder value to local storage.
    static func save(_ shortcut: KeyboardShortcuts.Shortcut?) {
        guard let shortcut else {
            UserDefaults.standard.removeObject(forKey: backupKey)
            return
        }
        guard let data = try? JSONEncoder().encode(shortcut) else { return }
        UserDefaults.standard.set(data, forKey: backupKey)
    }

    /// Previously saved recorder value, if any.
    static func load() -> KeyboardShortcuts.Shortcut? {
        guard let data = UserDefaults.standard.data(forKey: backupKey) else { return nil }
        return try? JSONDecoder().decode(KeyboardShortcuts.Shortcut.self, from: data)
    }

    /// Re-apply the locally saved shortcut when the active one is missing,
    /// otherwise seed the backup from the currently active value.
    static func restore() {
        let current = KeyboardShortcuts.getShortcut(for: .toggleQuickNote)
        guard let backup = load() else {
            // No backup yet: remember whatever is active (default or
            // already customized) for future launches.
            save(current)
            return
        }
        if backup != current {
            KeyboardShortcuts.setShortcut(backup, for: .toggleQuickNote)
        }
    }
}
