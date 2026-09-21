//
//  NestedFirestoreSync.swift
//  appleApp
//
//  Manual Firestore sync for nested lists on macOS, mirroring
//  FirestoreNestedSyncManager.android.kt: one open document (syncNow)
//  plus the document list (syncDocumentsNow).
//
//  Offline-first: every mutation is already in Room. Sync pushes dirty
//  rows, pulls watermarked remote changes with last-write-wins on
//  updatedAtMillis, then purges uploaded tombstones after the shared
//  retention window.
//
//  Unlike QuickNote there are no automatic triggers (no debounce, no
//  reconnect or edit hooks): the UI calls syncNow from a sync button.
//  Failures surface through uiState; the app stays fully usable offline.
//
//  Room access goes through NestedSyncBridge so the document mapping
//  (NestedSyncDocument), sync topology (NestedSyncConfig), and merge rules
//  stay single-sourced in shared Kotlin.
//

import Foundation
import Combine
import Network
import Shared
import FirebaseAuth
import FirebaseCore
import FirebaseFirestore

/// Compact sync state for the nested editor, mirroring NestedSyncState.
struct NestedSyncUiState {
    enum Status { case idle, syncing, synced, offline, error }
    var status: Status = .idle
    var documentId: String?
    var lastSyncedAt: Date?
    var message: String?
}

@MainActor
final class NestedFirestoreSync: ObservableObject {
    static let shared = NestedFirestoreSync()

    private static let config = NestedSyncConfig.shared
    // Firestore field carrying the LWW clock (NestedSyncDocument).
    private static let updatedAtField = "updatedAtMillis"

    private static let lastPullDocsKey = "nested.lastPullDocsMillis"
    private static let lastSyncedDocsKey = "nested.lastSyncedDocsMillis"

    private static func lastPullKey(_ documentId: String) -> String {
        "nested.lastPullMillis.\(documentId)"
    }

    private static func lastSyncedAtKey(_ documentId: String) -> String {
        "nested.lastSyncedAtMillis.\(documentId)"
    }

    @Published private(set) var uiState = NestedSyncUiState()

    private let monitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "checkit.nested-sync.network")
    private var online = true
    private var isSyncing = false
    private var started = false

    private func bridge() -> NestedSyncBridge {
        NestedAppleBridge.shared.ensureKoin()
        return NestedAppleBridge.shared.syncBridge()
    }

    // MARK: - Lifecycle

    func start() {
        guard !started else { return }
        started = true
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor [weak self] in
                self?.online = path.status == .satisfied
                if path.status != .satisfied {
                    self?.uiState.status = .offline
                }
            }
        }
        monitor.start(queue: monitorQueue)
    }

    /// Immediate sync of the open document — the sync button entry point.
    /// Extra taps while syncing are ignored.
    func syncNow(documentId: String) {
        guard !documentId.isEmpty else { return }
        Task { await sync(documentId: documentId) }
    }

    // MARK: - Sync

    func sync(documentId: String) async {
        guard !isSyncing else { return }
        isSyncing = true
        defer { isSyncing = false }

        guard let prepared = await prepareSync(documentId: documentId) else { return }
        let (userId, db) = prepared
        do {
            print("[NestedSync] started uid=\(userId) doc=\(documentId)")
            let docRef = db.collection(Self.config.USERS_COLLECTION).document(userId)
                .collection(Self.config.DOCUMENTS_COLLECTION).document(documentId)
            let itemsRef = docRef.collection(Self.config.ITEMS_COLLECTION)

            // Push: the document row, then dirty items (batched).
            var pushed = 0
            if let json = try await bridgeDirtyDocumentJson(documentId: documentId),
               let map = Self.documentDict(json: json) {
                try await docRef.setData(map)
                pushed += 1
                let maxUpdated = (map[Self.updatedAtField] as? NSNumber)?.int64Value ?? 0
                try await bridgeMarkDocumentClean(id: documentId, maxUpdatedAt: maxUpdated)
            }
            pushed += try await pushJsons(
                bridgeDirtyItemJsons(documentId: documentId),
                into: itemsRef,
                db: db
            ) { ids, maxUpdated in
                try await bridgeMarkItemsClean(ids: ids, maxUpdatedAt: maxUpdated)
            }

            // Pull: the document row by id, then watermarked items.
            var applied = 0
            let docSnapshot = try await docRef.getDocument()
            if let data = docSnapshot.data(),
               let json = Self.jsonString(for: data, documentId: docSnapshot.documentID),
               try await bridgeApplyRemoteDocument(json: json) {
                applied += 1
            }
            let lastPull = Int64(UserDefaults.standard.double(forKey: Self.lastPullKey(documentId)))
            let pullStart = Self.nowMillis()
            let pulled = try await pullJsons(from: itemsRef, lastPull: lastPull)
            // Batch apply orders parents before children so the
            // self-FK can never fail on first pulls.
            applied += try await bridgeApplyRemoteItems(jsons: pulled.jsons)
            UserDefaults.standard.set(Double(max(max(lastPull, pullStart), pulled.maxRemote)), forKey: Self.lastPullKey(documentId))

            // Purge: uploaded item tombstones, then the document tombstone.
            let purgeCutoff = pullStart - QuickNoteRules.shared.PURGE_AFTER_MILLIS
            let purgedItems = await purgeItems(itemsRef: itemsRef, documentId: documentId, cutoff: purgeCutoff)
            let purgedDoc = await purgeDocument(docRef: docRef, documentId: documentId, cutoff: purgeCutoff)
            let purged = purgedItems + purgedDoc

            UserDefaults.standard.set(Double(pullStart), forKey: Self.lastSyncedAtKey(documentId))
            uiState = NestedSyncUiState(status: .synced, documentId: documentId, lastSyncedAt: Date(timeIntervalSince1970: Double(pullStart) / 1000.0))
            print("[NestedSync] doc=\(documentId) pushed=\(pushed) pulled=\(pulled.jsons.count) applied=\(applied) purged=\(purged)")
        } catch {
            recordSyncFailure(documentId: documentId, error: error)
        }
    }

    /// Immediate sync of the document list — the drawer entry point.
    /// Extra taps while syncing are ignored.
    func syncDocumentsNow() {
        Task { await syncAllDocuments() }
    }

    /// Manual sync of the document list (all documents, no items): pushes
    /// dirty document rows, pulls watermarked remote documents, and purges
    /// uploaded document tombstones. Open-document sync still owns items.
    func syncAllDocuments() async {
        guard !isSyncing else { return }
        isSyncing = true
        defer { isSyncing = false }

        guard let prepared = await prepareSync(documentId: nil) else { return }
        let (userId, db) = prepared
        do {
            print("[NestedSync] docs started uid=\(userId)")
            let docsRef = db.collection(Self.config.USERS_COLLECTION).document(userId)
                .collection(Self.config.DOCUMENTS_COLLECTION)

            // Push: all dirty document rows (batched).
            let pushed = try await pushJsons(
                bridgeDirtyDocumentJsons(),
                into: docsRef,
                db: db
            ) { ids, maxUpdated in
                try await bridgeMarkDocumentsClean(ids: ids, maxUpdatedAt: maxUpdated)
            }

            // Pull: watermarked documents (overlap margin for clock skew).
            var applied = 0
            let lastPull = Int64(UserDefaults.standard.double(forKey: Self.lastPullDocsKey))
            let pullStart = Self.nowMillis()
            let pulled = try await pullJsons(from: docsRef, lastPull: lastPull)
            for json in pulled.jsons {
                if try await bridgeApplyRemoteDocument(json: json) {
                    applied += 1
                }
            }
            UserDefaults.standard.set(Double(max(max(lastPull, pullStart), pulled.maxRemote)), forKey: Self.lastPullDocsKey)

            // Purge uploaded document tombstones (items first, guarded).
            var purged = 0
            let purgeCutoff = pullStart - QuickNoteRules.shared.PURGE_AFTER_MILLIS
            let purgeableDocs = try await bridgePurgeableDocumentIds(cutoff: purgeCutoff)
            for id in purgeableDocs {
                purged += await purgeItems(
                    itemsRef: docsRef.document(id).collection(Self.config.ITEMS_COLLECTION),
                    documentId: id,
                    cutoff: purgeCutoff
                )
                purged += await purgeDocument(docRef: docsRef.document(id), documentId: id, cutoff: purgeCutoff)
            }

            UserDefaults.standard.set(Double(pullStart), forKey: Self.lastSyncedDocsKey)
            uiState = NestedSyncUiState(status: .synced, documentId: nil, lastSyncedAt: Date(timeIntervalSince1970: Double(pullStart) / 1000.0))
            print("[NestedSync] docs pushed=\(pushed) pulled=\(pulled.jsons.count) applied=\(applied) purged=\(purged)")
        } catch {
            recordSyncFailure(documentId: nil, error: error)
        }
    }

    // MARK: - Steps

    /// Shared online/config/sign-in prologue. Sets uiState and returns nil
    /// when a terminal state was already set (offline or sign-in fail).
    private func prepareSync(documentId: String?) async -> (String, Firestore)? {
        let last = documentId.map { lastSyncedAt(documentId: $0) } ?? lastSyncedDocsAt()
        online = monitor.currentPath.status == .satisfied
        guard isFirebaseConfigured() else {
            uiState = NestedSyncUiState(
                status: .error, documentId: documentId, lastSyncedAt: last,
                message: "Firebase not configured. Add GoogleService-Info.plist."
            )
            return nil
        }
        guard online else {
            uiState = NestedSyncUiState(status: .offline, documentId: documentId, lastSyncedAt: last)
            return nil
        }
        uiState = NestedSyncUiState(status: .syncing, documentId: documentId, lastSyncedAt: last)
        guard let userId = try? await ensureUserId() else {
            recordFailure(documentId: documentId, message: "Sign-in failed. Try again.")
            return nil
        }
        return (userId, Firestore.firestore(database: Self.config.DATABASE_ID))
    }

    /// Pushes JSON rows into a collection in batches, clears dirty flags
    /// guarded by the pushed watermark, and returns the pushed count. Rows
    /// without an id are skipped before batching so counts stay accurate.
    private func pushJsons(
        _ jsons: [String],
        into collection: CollectionReference,
        db: Firestore,
        markClean: ([String], Int64) async throws -> Void
    ) async throws -> Int {
        let rows = jsons.compactMap { json -> (String, Int64, [String: Any])? in
            guard let map = Self.documentDict(json: json),
                  let id = map["id"] as? String
            else { return nil }
            return (id, (map[Self.updatedAtField] as? NSNumber)?.int64Value ?? 0, map)
        }
        for chunk in rows.chunked(into: Int(Self.config.PUSH_BATCH_SIZE)) {
            let batch = db.batch()
            for (id, _, map) in chunk {
                batch.setData(map, forDocument: collection.document(id))
            }
            try await batch.commit()
        }
        if !rows.isEmpty {
            try await markClean(rows.map(\.0), rows.map(\.1).max() ?? 0)
        }
        return rows.count
    }

    /// Watermarked pull of one collection (overlap margin for clock skew).
    /// Returns JSON rows plus the max remote clock for the watermark.
    private func pullJsons(from collection: CollectionReference, lastPull: Int64) async throws -> (jsons: [String], maxRemote: Int64) {
        let snapshot = try await collection
            .whereField(Self.updatedAtField, isGreaterThan: lastPull - Self.config.PULL_OVERLAP_MILLIS)
            .getDocuments()
        var maxRemote = lastPull
        var jsons: [String] = []
        for doc in snapshot.documents {
            let data = doc.data()
            if let updated = (data[Self.updatedAtField] as? NSNumber)?.int64Value {
                maxRemote = max(maxRemote, updated)
            }
            guard let json = Self.jsonString(for: data, documentId: doc.documentID) else { continue }
            jsons.append(json)
        }
        return (jsons, maxRemote)
    }

    /// Purges uploaded item tombstones of one document. Returns the count.
    private func purgeItems(itemsRef: CollectionReference, documentId: String, cutoff: Int64) async -> Int {
        var purged = 0
        do {
            let ids = try await bridgePurgeableItemIds(documentId: documentId, cutoff: cutoff)
            for id in ids {
                do {
                    try await itemsRef.document(id).delete()
                    try await bridgeHardDeleteItems(ids: [id])
                    purged += 1
                } catch {
                    print("[NestedSync] Purge failed for item \(id); will retry next sync: \(error)")
                }
            }
        } catch {
            print("[NestedSync] Purge lookup failed for doc \(documentId); will retry next sync: \(error)")
        }
        return purged
    }

    /// Purges one uploaded document tombstone. Skipped while the document
    /// still has dirty items, so list sync can never orphan unsynced item
    /// rows through the cascading hard delete. Returns 0 or 1.
    private func purgeDocument(docRef: DocumentReference, documentId: String, cutoff: Int64) async -> Int {
        do {
            guard try await bridgePurgeableDocumentId(documentId: documentId, cutoff: cutoff) != nil else { return 0 }
            guard try await !bridgeHasDirtyItems(documentId: documentId) else { return 0 }
            try await docRef.delete()
            try await bridgeHardDeleteDocument(id: documentId)
            return 1
        } catch {
            print("[NestedSync] Purge failed for document \(documentId); will retry next sync: \(error)")
            return 0
        }
    }

    private func ensureUserId() async throws -> String? {
        if let uid = Auth.auth().currentUser?.uid { return uid }
        let result = try await Auth.auth().signInAnonymously()
        return result.user.uid
    }

    private func recordFailure(documentId: String?, message: String) {
        let last: Date?
        if let documentId {
            last = lastSyncedAt(documentId: documentId)
        } else {
            last = lastSyncedDocsAt()
        }
        uiState = NestedSyncUiState(status: .error, documentId: documentId, lastSyncedAt: last, message: message)
        print("[NestedSync] \(message)")
    }

    /// Shared catch-block mapping: offline vs failed, surfaced via state.
    private func recordSyncFailure(documentId: String?, error: Error) {
        let message: String
        if !online {
            message = "You're offline. Changes are saved on this device."
        } else {
            message = "Sync failed (\(error.localizedDescription)). Try again."
        }
        recordFailure(documentId: documentId, message: message)
    }

    private func lastSyncedDate(key: String) -> Date? {
        let millis = UserDefaults.standard.double(forKey: key)
        guard millis > 0 else { return nil }
        return Date(timeIntervalSince1970: millis / 1000.0)
    }

    private func lastSyncedAt(documentId: String) -> Date? {
        lastSyncedDate(key: Self.lastSyncedAtKey(documentId))
    }

    private func lastSyncedDocsAt() -> Date? {
        lastSyncedDate(key: Self.lastSyncedDocsKey)
    }

    private func isFirebaseConfigured() -> Bool {
        NSClassFromString("FIRApp") != nil && FirebaseApp.app() != nil
    }

    // MARK: - Document mapping

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

    private static func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }

    // MARK: - Bridge continuations

    private func bridgeDirtyDocumentJson(documentId: String) async throws -> String? {
        try await withCheckedThrowingContinuation { cont in
            bridge().dirtyDocumentJson(documentId: documentId) { json, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: json) }
            }
        }
    }

    private func bridgeDirtyItemJsons(documentId: String) async throws -> [String] {
        try await withCheckedThrowingContinuation { cont in
            bridge().dirtyItemJsons(documentId: documentId) { jsons, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: jsons ?? []) }
            }
        }
    }

    private func bridgeDirtyDocumentJsons() async throws -> [String] {
        try await withCheckedThrowingContinuation { cont in
            bridge().dirtyDocumentJsons { jsons, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: jsons ?? []) }
            }
        }
    }

    private func bridgeMarkDocumentClean(id: String, maxUpdatedAt: Int64) async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            bridge().markDocumentClean(id: id, maxUpdatedAt: maxUpdatedAt) { error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume() }
            }
        }
    }

    private func bridgeMarkItemsClean(ids: [String], maxUpdatedAt: Int64) async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            bridge().markItemsClean(ids: ids, maxUpdatedAt: maxUpdatedAt) { error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume() }
            }
        }
    }

    private func bridgeMarkDocumentsClean(ids: [String], maxUpdatedAt: Int64) async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            bridge().markDocumentsClean(ids: ids, maxUpdatedAt: maxUpdatedAt) { error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume() }
            }
        }
    }

    private func bridgeApplyRemoteDocument(json: String) async throws -> Bool {
        try await withCheckedThrowingContinuation { cont in
            bridge().applyRemoteDocumentJson(json: json) { applied, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: applied?.boolValue ?? false) }
            }
        }
    }

    private func bridgeHasDirtyItems(documentId: String) async throws -> Bool {
        try await withCheckedThrowingContinuation { cont in
            bridge().hasDirtyItems(documentId: documentId) { dirty, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: dirty?.boolValue ?? false) }
            }
        }
    }

    private func bridgeApplyRemoteItems(jsons: [String]) async throws -> Int {
        try await withCheckedThrowingContinuation { cont in
            bridge().applyRemoteItemJsons(jsons: jsons) { applied, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: Int(truncating: applied ?? 0)) }
            }
        }
    }

    private func bridgePurgeableItemIds(documentId: String, cutoff: Int64) async throws -> [String] {
        try await withCheckedThrowingContinuation { cont in
            bridge().purgeableItemIds(documentId: documentId, cutoffMillis: cutoff) { ids, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: ids ?? []) }
            }
        }
    }

    private func bridgePurgeableDocumentId(documentId: String, cutoff: Int64) async throws -> String? {
        try await withCheckedThrowingContinuation { cont in
            bridge().purgeableDocumentId(documentId: documentId, cutoffMillis: cutoff) { id, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: id) }
            }
        }
    }

    private func bridgePurgeableDocumentIds(cutoff: Int64) async throws -> [String] {
        try await withCheckedThrowingContinuation { cont in
            bridge().purgeableDocumentIds(cutoffMillis: cutoff) { ids, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: ids ?? []) }
            }
        }
    }

    private func bridgeHardDeleteItems(ids: [String]) async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            bridge().hardDeleteItems(ids: ids) { error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume() }
            }
        }
    }

    private func bridgeHardDeleteDocument(id: String) async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            bridge().hardDeleteDocument(id: id) { error in
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
