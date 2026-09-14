//
//  SettingsView.swift
//  appleApp
//
//  SwiftUI settings window (opened from the right-click menu).
//
import SwiftUI
import KeyboardShortcuts
import GoogleSignInSwift

struct SettingsView: View {
    @ObservedObject private var account = QuickNoteGoogleSignIn.shared

    var body: some View {
        Form {
            Section("Keyboard Shortcut") {
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

            Section("Account") {
                if !account.isAnonymous, let email = account.email {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Signed in as \(email)")
                            Text("Notes sync across your devices.")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button("Sign Out") {
                            account.signOut()
                        }
                    }
                } else {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Not signed in")
                            Text("Notes stay on this Mac until you sign in.")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        GoogleSignInButton {
                            Task { await account.signIn() }
                        }
                        .disabled(account.busy)
                    }
                    if account.busy {
                        Text("Signing in…").font(.caption).foregroundStyle(.secondary)
                    }
                }
                if let error = account.errorMessage {
                    Text(error).font(.caption).foregroundStyle(.red)
                }
            }
        }
        .padding()
        .frame(width: 380)
    }
}
