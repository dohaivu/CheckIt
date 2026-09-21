//
//  NestedFirestoreSync.swift
//  appleApp
//
//  Manual Firestore sync for one open nested document on macOS, mirroring
//  FirestoreNestedSyncManager.android.kt.
//
//  Offline-first: every mutation is already in Room. syncNow(documentId:)
//  pushes dirty rows of that document, pulls watermarked remote changes with
//  last-write-wins on updatedAtMillis, then purges uploaded tombstones after
//  the shared retention window.
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
        online = monitor.currentPath.status == .satisfied
        guard isFirebaseConfigured() else {
            uiState = NestedSyncUiState(
                status: .error, documentId: documentId, lastSyncedAt: lastSyncedAt(documentId: documentId),
                message: "Firebase not configured. Add GoogleService-Info.plist."
            )
            return
        }
        guard online else {
            uiState = NestedSyncUiState(status: .offline, documentId: documentId, lastSyncedAt: lastSyncedAt(documentId: documentId))
            return
        }
        uiState = NestedSyncUiState(status: .syncing, documentId: documentId, lastSyncedAt: lastSyncedAt(documentId: documentId))

        do {
            guard let userId = try await ensureUserId() else {
                recordFailure(documentId: documentId, message: "Sign-in failed. Try again.")
                return
            }
            print("[NestedSync] started uid=\(userId) doc=\(documentId)")
            let db = Firestore.firestore(database: Self.config.DATABASE_ID)
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
            let itemJsons = try await bridgeDirtyItemJsons(documentId: documentId)
            for chunk in itemJsons.chunked(into: Int(Self.config.PUSH_BATCH_SIZE)) {
                let batch = db.batch()
                for json in chunk {
                    guard let map = Self.documentDict(json: json),
                          let id = map["id"] as? String
                    else { continue }
                    batch.setData(map, forDocument: itemsRef.document(id))
                }
                try await batch.commit()
                pushed += chunk.count
            }
            if !itemJsons.isEmpty {
                let ids = itemJsons.compactMap { Self.documentDict(json: $0)?["id"] as? String }
                let maxUpdated = itemJsons.compactMap {
                    (Self.documentDict(json: $0)?[Self.updatedAtField] as? NSNumber)?.int64Value
                }.max() ?? 0
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
            let snapshot = try await itemsRef
                .whereField(Self.updatedAtField, isGreaterThan: lastPull - Self.config.PULL_OVERLAP_MILLIS)
                .getDocuments()
            var maxRemoteUpdatedAt = lastPull
            var pulledItemJsons: [String] = []
            for doc in snapshot.documents {
                let data = doc.data()
                if let updated = (data[Self.updatedAtField] as? NSNumber)?.int64Value {
                    maxRemoteUpdatedAt = max(maxRemoteUpdatedAt, updated)
                }
                guard let json = Self.jsonString(for: data, documentId: doc.documentID) else { continue }
                pulledItemJsons.append(json)
            }
            // Batch apply orders parents before children so the
            // self-FK can never fail on first pulls.
            applied += try await bridgeApplyRemoteItems(jsons: pulledItemJsons)
            UserDefaults.standard.set(Double(max(max(lastPull, pullStart), maxRemoteUpdatedAt)), forKey: Self.lastPullKey(documentId))

            // Purge: uploaded item tombstones, then the document tombstone.
            let purgeCutoff = pullStart - QuickNoteRules.shared.PURGE_AFTER_MILLIS
            let purgedItems = await purgeItems(itemsRef: itemsRef, documentId: documentId, cutoff: purgeCutoff)
            let purgedDoc = await purgeDocument(docRef: docRef, documentId: documentId, cutoff: purgeCutoff)
            let purged = purgedItems + purgedDoc

            UserDefaults.standard.set(Double(pullStart), forKey: Self.lastSyncedAtKey(documentId))
            uiState = NestedSyncUiState(status: .synced, documentId: documentId, lastSyncedAt: Date(timeIntervalSince1970: Double(pullStart) / 1000.0))
            print("[NestedSync] doc=\(documentId) pushed=\(pushed) pulled=\(snapshot.count) applied=\(applied) purged=\(purged)")
        } catch {
            recordFailure(documentId: documentId, message: "Sync failed (\(error.localizedDescription)). Try again.")
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

        online = monitor.currentPath.status == .satisfied
        guard isFirebaseConfigured() else {
            uiState = NestedSyncUiState(
                status: .error, documentId: nil, lastSyncedAt: lastSyncedDocsAt(),
                message: "Firebase not configured. Add GoogleService-Info.plist."
            )
            return
        }
        guard online else {
            uiState = NestedSyncUiState(status: .offline, documentId: nil, lastSyncedAt: lastSyncedDocsAt())
            return
        }
        uiState = NestedSyncUiState(status: .syncing, documentId: nil, lastSyncedAt: lastSyncedDocsAt())

        do {
            guard let userId = try await ensureUserId() else {
                recordFailure(documentId: nil, message: "Sign-in failed. Try again.")
                return
            }
            print("[NestedSync] docs started uid=\(userId)")
            let db = Firestore.firestore(database: Self.config.DATABASE_ID)
            let docsRef = db.collection(Self.config.USERS_COLLECTION).document(userId)
                .collection(Self.config.DOCUMENTS_COLLECTION)

            // Push: all dirty document rows (batched).
            var pushed = 0
            let docJsons = try await bridgeDirtyDocumentJsons()
            for chunk in docJsons.chunked(into: Int(Self.config.PUSH_BATCH_SIZE)) {
                let batch = db.batch()
                for json in chunk {
                    guard let map = Self.documentDict(json: json),
                          let id = map["id"] as? String
                    else { continue }
                    batch.setData(map, forDocument: docsRef.document(id))
                }
                try await batch.commit()
                pushed += chunk.count
            }
            if !docJsons.isEmpty {
                let ids = docJsons.compactMap { Self.documentDict(json: $0)?["id"] as? String }
                let maxUpdated = docJsons.compactMap {
                    (Self.documentDict(json: $0)?[Self.updatedAtField] as? NSNumber)?.int64Value
                }.max() ?? 0
                try await bridgeMarkDocumentsClean(ids: ids, maxUpdatedAt: maxUpdated)
            }

            // Pull: watermarked documents (overlap margin for clock skew).
            var applied = 0
            let lastPull = Int64(UserDefaults.standard.double(forKey: Self.lastPullDocsKey))
            let pullStart = Self.nowMillis()
            let snapshot = try await docsRef
                .whereField(Self.updatedAtField, isGreaterThan: lastPull - Self.config.PULL_OVERLAP_MILLIS)
                .getDocuments()
            var maxRemoteUpdatedAt = lastPull
            for doc in snapshot.documents {
                let data = doc.data()
                if let updated = (data[Self.updatedAtField] as? NSNumber)?.int64Value {
                    maxRemoteUpdatedAt = max(maxRemoteUpdatedAt, updated)
                }
                guard let json = Self.jsonString(for: data, documentId: doc.documentID) else { continue }
                if try await bridgeApplyRemoteDocument(json: json) {
                    applied += 1
                }
            }
            UserDefaults.standard.set(Double(max(max(lastPull, pullStart), maxRemoteUpdatedAt)), forKey: Self.lastPullDocsKey)

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
            print("[NestedSync] docs pushed=\(pushed) pulled=\(snapshot.count) applied=\(applied) purged=\(purged)")
        } catch {
            recordFailure(documentId: nil, message: "Sync failed (\(error.localizedDescription)). Try again.")
        }
    }

    // MARK: - Steps

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
            if !(try await bridgeDirtyItemJsons(documentId: documentId)).isEmpty { return 0 }
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

    private func lastSyncedAt(documentId: String) -> Date? {
        let millis = UserDefaults.standard.double(forKey: Self.lastSyncedAtKey(documentId))
        guard millis > 0 else { return nil }
        return Date(timeIntervalSince1970: millis / 1000.0)
    }

    private func lastSyncedDocsAt() -> Date? {
        let millis = UserDefaults.standard.double(forKey: Self.lastSyncedDocsKey)
        guard millis > 0 else { return nil }
        return Date(timeIntervalSince1970: millis / 1000.0)
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

    private func bridgeApplyRemoteItem(json: String) async throws -> Bool {
        try await withCheckedThrowingContinuation { cont in
            bridge().applyRemoteItemJson(json: json) { applied, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: applied?.boolValue ?? false) }
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
