//
//  CheckItApp.swift
//  CheckIt
//
//  Created by DO HAI VU on 10/9/26.
//

import SwiftUI

@main
struct CheckItApp: App {
    var body: some Scene {
//        WindowGroup {
//            ContentView()
//        }
        MenuBarExtra("QuickNote", systemImage: "note.text") {
            QuickNoteMenuView()
        }
        .menuBarExtraStyle(.window)
    }
}
