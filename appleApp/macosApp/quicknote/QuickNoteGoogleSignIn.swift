//
//  QuickNoteGoogleSignIn.swift
//  appleApp
//
//  Google Sign-In for the macOS menu app, implementing Option A:
//  the anonymous Firebase user is *linked* to the Google credential, so the
//  UID (and therefore users/{uid}/quickNotes) is preserved — no migration.
//  A second device signing in with the same Google account lands on the same
//  UID and syncs the same collection.
//
//  Note: FirebaseUI (FirebaseGoogleSwiftUI) is iOS-only, so macOS uses the
//  GoogleSignIn SDK directly, per Firebase's Google sign-in guide.
//
import AppKit
import Combine
import FirebaseAuth
import FirebaseCore
import GoogleSignIn

@MainActor
final class QuickNoteGoogleSignIn: ObservableObject {
    static let shared = QuickNoteGoogleSignIn()

    /// Nil while anonymous (or signed out). Anonymous is the default.
    @Published private(set) var email: String?
    @Published private(set) var isAnonymous = true
    @Published private(set) var busy = false
    @Published var errorMessage: String?

    private var listener: AuthStateDidChangeListenerHandle?

    func start() {
        guard listener == nil else { return }
        listener = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor [weak self] in
                self?.email = user?.email
                self?.isAnonymous = user?.isAnonymous ?? true
            }
        }
    }

    func signIn() async {
        guard !busy else { return }
        busy = true
        errorMessage = nil
        defer { busy = false }
        do {
            guard let app = FirebaseApp.app(), let clientID = app.options.clientID else {
                throw SignInError.notConfigured
            }
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
            // The popover may still be becoming key (e.g. sign-in launched
            // from the right-click menu, which opens the popover first).
            var presenting: NSWindow?
            for _ in 0..<10 {
                if let key = NSApp.keyWindow {
                    presenting = key
                    break
                }
                try await Task.sleep(nanoseconds: 50_000_000)
            }
            guard let presenting else {
                throw SignInError.noWindow
            }
            let gidResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenting)
            guard let idToken = gidResult.user.idToken?.tokenString else {
                throw SignInError.missingToken
            }
            let credential = GoogleAuthProvider.credential(
                withIDToken: idToken,
                accessToken: gidResult.user.accessToken.tokenString
            )
            if let user = Auth.auth().currentUser, user.isAnonymous {
                // Link: anonymous UID is preserved, data stays in place.
                do {
                    _ = try await user.link(with: credential)
                } catch {
                    // Google account already linked elsewhere (e.g. signed in
                    // on another device first): join that account instead.
                    // Local Room rows merge in via the next dirty push.
                    if let updated = (error as NSError).userInfo[AuthErrorUserInfoUpdatedCredentialKey] as? AuthCredential {
                        _ = try await Auth.auth().signIn(with: updated)
                    } else {
                        throw error
                    }
                }
            } else {
                _ = try await Auth.auth().signIn(with: credential)
            }
            // UID may have changed (merge path): sync the new collection.
            QuickNoteFirestoreSync.shared.requestSync()
        } catch {
            if (error as NSError).code == GIDSignInError.canceled.rawValue {
                return // user dismissed the browser; not an error
            }
            errorMessage = error.localizedDescription
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
        try? Auth.auth().signOut()
        // Next sync falls back to a fresh anonymous user (offline-first).
        QuickNoteFirestoreSync.shared.requestSync()
    }

    func handle(url: URL) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    enum SignInError: LocalizedError {
        case notConfigured, noWindow, missingToken
        var errorDescription: String? {
            switch self {
            case .notConfigured: "Firebase is not configured."
            case .noWindow: "Open the Quick Note menu and try again."
            case .missingToken: "Google sign-in returned no token."
            }
        }
    }
}
