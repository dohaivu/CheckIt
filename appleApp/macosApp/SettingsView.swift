//
//  SettingsView.swift
//  appleApp
//
//  SwiftUI settings window (opened from the right-click menu).
//
import SwiftUI
import KeyboardShortcuts

struct SettingsView: View {
    var body: some View {
        Form {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Toggle Quick Note menu")
                    Text("Works from any app. Press again to close.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                KeyboardShortcuts.Recorder(for: .toggleQuickNote)
            }
        }
        .padding()
        .frame(width: 380)
    }
}
