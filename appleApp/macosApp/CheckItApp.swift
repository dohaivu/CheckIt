//
//  CheckItApp.swift
//  CheckIt
//
//  Created by DO HAI VU on 10/9/26.
//

import SwiftUI
import Shared

@main
struct CheckItApp: App {
    init() {
        QuickNoteAppleBridge.shared.ensureKoin()
    }

    var body: some Scene {
        MenuBarExtra("QuickNote", systemImage: "note.text") {
            QuickNoteMenuView()
        }
        .menuBarExtraStyle(.window)
    }
}
