//
//  CheckItApp.swift
//  CheckIt
//
//  Created by DO HAI VU on 10/9/26.
//

import SwiftUI
import Shared
import FirebaseCore

@main
struct CheckItApp: App {
    init() {
        QuickNoteAppleBridge.shared.ensureKoin()
        QuickNoteNotificationScheduler.configure()
        configureFirebase()
        QuickNoteGoogleSignIn.shared.start()
        QuickNoteFirestoreSync.shared.start()
    }

    var body: some Scene {
        MenuBarExtra("QuickNote", systemImage: "note.text") {
            QuickNoteMenuView()
        }
        .menuBarExtraStyle(.window)
    }

    /// Requires GoogleService-Info.plist in the macosApp target (Firebase
    /// console → add a macOS app with this target's bundle ID). Without it
    /// the app keeps working offline and sync reports "not configured".
    private func configureFirebase() {
        if FirebaseApp.app() != nil {
            print("[QuickNoteSync] Firebase already configured.")
            return
        }
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") else {
            print("[QuickNoteSync] GoogleService-Info.plist missing from bundle; sync disabled until added.")
            return
        }
        FirebaseApp.configure()
        print("[QuickNoteSync] Firebase configured (plist: \(path), app: \(FirebaseApp.app() != nil ? "ready" : "nil")).")
    }
}
