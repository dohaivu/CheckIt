//
//  QuickNoteFirestoreSync.swift
//  appleApp
//
//  Firestore sync for QuickNote on macOS, mirroring
//  FirestoreQuickNoteSyncManager.android.kt.
//
//  Offline-first: every mutation is already in Room before requestSync() is
//  called. Sync is incremental and never periodic:
//  - push uploads only rows flagged dirty (batched), then clears the flag;
//  - pull queries only documents newer than the last pull watermark;
//  - purge permanently deletes old uploaded tombstones (blob, doc, row);
//  - triggers are local edits (debounced), menu open, and network reconnect.
//
//  Room access goes through QuickNoteSyncBridge so the document mapping
//  (QuickNoteSyncDocument), sync topology (QuickNoteSyncConfig), and merge
//  rules stay single-sourced in shared Kotlin.
//

import Foundation
import Combine
import Network
import Shared
import FirebaseAuth
import FirebaseCore
import FirebaseFirestore
import FirebaseStorage

/// Compact sync state for the menu footer, mirroring QuickNoteSyncState.
struct QuickNoteSyncUiState {
    enum Status { case idle, syncing, synced, offline, error }
    var status: Status = .idle
    var lastSyncedAt: Date?
    var message: String?
}

@MainActor
final class QuickNoteFirestoreSync: ObservableObject {
    static let shared = QuickNoteFirestoreSync()

    // MARK: - Constants (topology from QuickNoteSyncConfig; timing stays local)

    private static let config = QuickNoteSyncConfig.shared
    private static let rules = QuickNoteRules.shared
    private static let attachmentsSubdir = "QuickNoteImages"
    private static let syncDebounceNanos: UInt64 = 30_000_000_000 // 30s: no realtime sync, just cross-device availability
    private static let lastPullKey = "quicknote.lastPullMillis"
    private static let lastSyncedAtKey = "quicknote.lastSyncedAt"

    @Published private(set) var uiState = QuickNoteSyncUiState()

    private let monitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "checkit.sync.network")
    private var online = true
    private var debounceTask: Task<Void, Never>?
    private var isSyncing = false
    private var rerunRequested = false
    private var consecutiveFailures = 0
    private var nextRetryAtMillis: Int64 = 0
    private var started = false

    private func bridge() -> QuickNoteSyncBridge {
        QuickNoteAppleBridge.shared.ensureKoin()
        return QuickNoteAppleBridge.shared.syncBridge()
    }

    // MARK: - Lifecycle

    func start() {
        guard !started else { return }
        started = true
        if let last = UserDefaults.standard.object(forKey: Self.lastSyncedAtKey) as? Double {
            uiState = QuickNoteSyncUiState(status: .synced, lastSyncedAt: Date(timeIntervalSince1970: last))
        }
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor [weak self] in
                let wasOffline = self?.online == false
                self?.online = path.status == .satisfied
                if wasOffline, path.status == .satisfied {
                    self?.requestSync()
                } else if path.status != .satisfied {
                    self?.uiState.status = .offline
                }
            }
        }
        monitor.start(queue: monitorQueue)
        syncNow()
    }

    func requestSync() {
        debounceTask?.cancel()
        debounceTask = Task { @MainActor [weak self] in
            try? await Task.sleep(nanoseconds: Self.syncDebounceNanos)
            guard !Task.isCancelled else { return }
            await self?.sync()
        }
    }

    /// Immediate sync, bypassing the debounce — for explicit user refresh.
    func syncNow() {
        debounceTask?.cancel()
        Task { await sync() }
    }

    // MARK: - Sync

    func sync() async {
        if isSyncing {
            rerunRequested = true
            return
        }
        isSyncing = true
        defer {
            isSyncing = false
            if rerunRequested {
                // Edits landed mid-sync: converge immediately instead of
                // waiting out the debounce (sync itself leaves no dirt).
                rerunRequested = false
                Task { await sync() }
            }
        }

        let nowMillis = Self.nowMillis()
        if nowMillis < nextRetryAtMillis { return }
        guard isFirebaseConfigured() else {
            uiState = QuickNoteSyncUiState(
                status: .error, lastSyncedAt: uiState.lastSyncedAt,
                message: "Firebase not configured. Add GoogleService-Info.plist."
            )
            return
        }
        guard online else {
            uiState = QuickNoteSyncUiState(status: .offline, lastSyncedAt: uiState.lastSyncedAt)
            return
        }
        uiState = QuickNoteSyncUiState(status: .syncing, lastSyncedAt: uiState.lastSyncedAt)

        do {
            guard let userId = try await ensureUserId() else {
                recordFailure("Sign-in failed. Sync will retry automatically.")
                return
            }
            print("Sync started uid=\(userId)")
            let db = Firestore.firestore(database: Self.config.DATABASE_ID)
            let storage = Storage.storage(url: Self.config.STORAGE_BUCKET)
            let notesRef = db.collection(Self.config.USERS_COLLECTION).document(userId)
                .collection(Self.config.NOTES_COLLECTION)

            // Upload pending attachments first so their URLs are in the doc push.
            var dirty = try await bridgeDirtyNotes()
            for note in dirty where needsAttachmentUpload(note) {
                if let url = await uploadAttachment(storage: storage, userId: userId, note: note) {
                    try await bridgeSetAttachmentUrl(id: note.id, url: url)
                }
            }

            // Push: only locally changed rows, including tombstones.
            // Documents come JSON-encoded from the bridge so field names
            // stay single-sourced in QuickNoteSyncDocument.
            // Re-read after uploads: attachment URLs bumped updatedAt.
            dirty = try await bridgeDirtyNotes()
            let documents = try await bridgeDirtyDocuments()
            for chunk in documents.chunked(into: Int(Self.config.PUSH_BATCH_SIZE)) {
                let batch = db.batch()
                for json in chunk {
                    guard let doc = Self.documentDict(json: json),
                          let id = doc["id"] as? String
                    else { continue }
                    batch.setData(doc, forDocument: notesRef.document(id))
                }
                try await batch.commit()
            }
            // Rows whose attachment upload failed stay dirty for next time.
            let uploaded = dirty.filter { !needsAttachmentUpload($0) }
            if !uploaded.isEmpty {
                let ids = uploaded.map(\.id)
                let maxUpdated = uploaded.map(\.updatedAt).max() ?? 0
                try await bridgeMarkClean(ids: ids, maxUpdatedAt: maxUpdated)
            }

            // Pull: only documents newer than the last pull (with overlap
            // margin for clock skew; LWW merge keeps re-pulls idempotent).
            let lastPull = Int64(UserDefaults.standard.double(forKey: Self.lastPullKey))
            let pullStart = Self.nowMillis()
            let snapshot = try await notesRef
                .whereField("updatedAt", isGreaterThan: lastPull - Self.config.PULL_OVERLAP_MILLIS)
                .getDocuments()
            var applied = 0
            var maxRemoteUpdatedAt = lastPull
            for doc in snapshot.documents {
                let data = doc.data()
                if let updated = (data["updatedAt"] as? NSNumber)?.int64Value {
                    maxRemoteUpdatedAt = max(maxRemoteUpdatedAt, updated)
                }
                guard let json = Self.jsonString(for: data, documentId: doc.documentID) else { continue }
                // Download first so a failed fetch keeps the existing cached
                // file as fallback (Android parity); the path rides into
                // the merge below.
                let localPath = await downloadAttachment(storage: storage, documentId: doc.documentID, data: data)
                if try await bridgeApplyRemote(json: json, localPath: localPath) {
                    applied += 1
                    if (data["deleted"] as? Bool) == true {
                        QuickNoteNotificationScheduler.cancel(noteId: doc.documentID)
                    }
                }
            }
            UserDefaults.standard.set(Double(max(max(lastPull, pullStart), maxRemoteUpdatedAt)), forKey: Self.lastPullKey)
            UserDefaults.standard.set(Double(pullStart) / 1000.0, forKey: Self.lastSyncedAtKey)

            let purged = try await purgeTombstones(storage: storage, notesRef: notesRef, now: pullStart)
            consecutiveFailures = 0
            nextRetryAtMillis = 0
            uiState = QuickNoteSyncUiState(status: .synced, lastSyncedAt: Date(timeIntervalSince1970: Double(pullStart) / 1000.0))
            print("[QuickNoteSync] pushed=\(dirty.count) pulled=\(snapshot.count) applied=\(applied) purged=\(purged)")
        } catch {
            recordFailure("Sync failed (\(error.localizedDescription)). Will retry automatically.")
        }
    }

    // MARK: - Steps

    private func ensureUserId() async throws -> String? {
        if let uid = Auth.auth().currentUser?.uid { return uid }
        let result = try await Auth.auth().signInAnonymously()
        return result.user.uid
    }

    private func needsAttachmentUpload(_ note: QuickNote) -> Bool {
        note.type != QuickNoteType.text
            && note.attachmentUrl == nil
            && note.attachmentLocalPath != nil
    }

    private func uploadAttachment(storage: Storage, userId: String, note: QuickNote) async -> String? {
        guard let localPath = note.attachmentLocalPath else { return nil }
        let fileURL = URL(fileURLWithPath: localPath)
        guard FileManager.default.fileExists(atPath: fileURL.path) else { return nil }
        do {
            let ref = storage.reference().child("\(Self.config.ATTACHMENTS_DIR)/\(userId)/\(note.id).webp")
            _ = try await ref.putFileAsync(from: fileURL)
            return try await ref.downloadURL().absoluteString
        } catch {
            print("[QuickNoteSync] Attachment upload failed for \(note.id): \(error)")
            return nil
        }
    }

    /// Device-local path to record for this document's attachment: the cached
    /// file when the URL is unchanged, a fresh download otherwise, nil when
    /// there is nothing to show or the fetch failed (the merge then keeps
    /// the previous path as fallback, like Android).
    private func downloadAttachment(storage: Storage, documentId: String, data: [String: Any]) async -> String? {
        guard let urlString = data["attachmentUrl"] as? String,
              (data["type"] as? String) != "TEXT"
        else { return nil }
        if let existing = try? await bridgeNote(id: documentId),
           let localPath = existing.attachmentLocalPath,
           existing.attachmentUrl == urlString,
           FileManager.default.fileExists(atPath: localPath) {
            return localPath
        }
        do {
            let ref = storage.reference(forURL: urlString)
            let bytes = try await ref.data(maxSize: Self.config.MAX_DOWNLOAD_BYTES)
            let dir = Self.attachmentDir()
            try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
            let fileURL = dir.appendingPathComponent("\(documentId).webp")
            try bytes.write(to: fileURL)
            return fileURL.path
        } catch {
            print("[QuickNoteSync] Attachment download failed for \(documentId): \(error)")
            return nil
        }
    }

    /// Permanent deletion for old tombstones: Storage blob, Firestore
    /// document, then the local row — in that order.
    private func purgeTombstones(storage: Storage, notesRef: CollectionReference, now: Int64) async throws -> Int {
        let tombstones = try await bridgePurgeable(cutoff: now - Self.rules.PURGE_AFTER_MILLIS)
        var purged = 0
        var hardDeleteIds: [String] = []
        for note in tombstones {
            do {
                if let urlString = note.attachmentUrl {
                    do {
                        try await storage.reference(forURL: urlString).delete()
                    } catch {
                        print("[QuickNoteSync] Blob delete failed for \(note.id), continuing purge: \(error)")
                    }
                }
                try await notesRef.document(note.id).delete()
                hardDeleteIds.append(note.id)
                purged += 1
            } catch {
                print("[QuickNoteSync] Purge failed for \(note.id); will retry next sync: \(error)")
            }
        }
        if !hardDeleteIds.isEmpty {
            try await bridgeHardDelete(ids: hardDeleteIds)
        }
        return purged
    }

    private func recordFailure(_ message: String) {
        consecutiveFailures += 1
        let shift = min(consecutiveFailures - 1, 4)
        let backoff = min(Self.config.BASE_BACKOFF_MILLIS * (Int64(1) << shift), Self.config.MAX_BACKOFF_MILLIS)
        nextRetryAtMillis = Self.nowMillis() + backoff
        uiState = QuickNoteSyncUiState(status: .error, lastSyncedAt: uiState.lastSyncedAt, message: message)
        print("[QuickNoteSync] \(message) retry in \(backoff)ms")
    }

    // MARK: - Document mapping (decoded with QuickNoteSyncDocument)

    /// Parses a bridge-encoded push document back to a Firestore dictionary.
    private static func documentDict(json: String) -> [String: Any]? {
        guard let data = json.data(using: .utf8),
              let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return nil }
        return dict
    }

    private static func jsonString(for data: [String: Any], documentId: String) -> String? {
        var mutable = data
        mutable["id"] = documentId
        guard JSONSerialization.isValidJSONObject(mutable),
              let jsonData = try? JSONSerialization.data(withJSONObject: mutable)
        else { return nil }
        return String(data: jsonData, encoding: .utf8)
    }

    private static func attachmentDir() -> URL {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(attachmentsSubdir, isDirectory: true)
    }

    private static func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }

    private func isFirebaseConfigured() -> Bool {
        // FirebaseApp.app() is non-nil after FirebaseApp.configure().
        NSClassFromString("FIRApp") != nil && FirebaseApp.app() != nil
    }

    // MARK: - Bridge continuations

    private func bridgeDirtyNotes() async throws -> [QuickNote] {
        try await withCheckedThrowingContinuation { cont in
            bridge().dirtyNotes { notes, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: notes ?? []) }
            }
        }
    }

    private func bridgeSetAttachmentUrl(id: String, url: String) async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            bridge().setAttachmentUrl(id: id, url: url) { error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume() }
            }
        }
    }

    private func bridgeMarkClean(ids: [String], maxUpdatedAt: Int64) async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            bridge().markClean(ids: ids, maxUpdatedAt: maxUpdatedAt) { error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume() }
            }
        }
    }

    private func bridgeNote(id: String) async throws -> QuickNote? {
        try await withCheckedThrowingContinuation { cont in
            bridge().noteById(id: id) { note, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: note) }
            }
        }
    }

    private func bridgeDirtyDocuments() async throws -> [String] {
        try await withCheckedThrowingContinuation { cont in
            bridge().dirtyNoteDocuments { documents, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: documents ?? []) }
            }
        }
    }

    private func bridgeApplyRemote(json: String, localPath: String?) async throws -> Bool {
        try await withCheckedThrowingContinuation { cont in
            bridge().applyRemoteJson(json: json, downloadedAttachmentPath: localPath) { applied, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: applied?.boolValue ?? false) }
            }
        }
    }

    private func bridgePurgeable(cutoff: Int64) async throws -> [QuickNote] {
        try await withCheckedThrowingContinuation { cont in
            bridge().purgeableTombstones(cutoffMillis: cutoff) { notes, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: notes ?? []) }
            }
        }
    }

    private func bridgeHardDelete(ids: [String]) async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            bridge().hardDeleteNotes(ids: ids) { error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume() }
            }
        }
    }
}

private extension Array {
    func chunked(into size: Int) -> [[Element]] {
        guard size > 0 else { return [self] }
        return stride(from: 0, to: count, by: size).map {
            Array(self[$0 ..< Swift.min($0 + size, count)])
        }
    }
}
