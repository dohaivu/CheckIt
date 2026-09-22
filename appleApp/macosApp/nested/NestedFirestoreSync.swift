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

    private static let lastSyncedDocsKey = "nested.lastSyncedDocsMillis"
    private static let snapshotKeep = 3

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

        await writePreSyncSnapshot(documentId: documentId)
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
            pushed += try await pushAllDirtyItems(db: db, userId: userId)

            // Pull: full document plus full items. Manual sync converges:
            // LWW applies remote wins, delete detection tombstones clean
            // rows missing remotely.
            var applied = 0
            var reconciled = 0
            let pullStart = Self.nowMillis()
            let docSnapshot = try await docRef.getDocument()
            if docSnapshot.exists {
                if let data = docSnapshot.data(),
                   let json = Self.jsonString(for: data, documentId: docSnapshot.documentID),
                   try await bridgeApplyRemoteDocument(json: json) {
                    applied += 1
                }
            } else if try await bridgeReconcileDocumentMissing(documentId: documentId, now: pullStart) {
                reconciled += 1
            }
            let snapshot = try await itemsRef.getDocuments()
            var itemJsons: [String] = []
            for doc in snapshot.documents {
                guard let json = Self.jsonString(for: doc.data(), documentId: doc.documentID) else { continue }
                itemJsons.append(json)
            }
            // Batch apply orders parents before children so the
            // self-FK can never fail on first pulls.
            applied += try await bridgeApplyRemoteItems(jsons: itemJsons)
            let itemIds = itemJsons.compactMap { Self.documentDict(json: $0)?["id"] as? String }
            reconciled += try await bridgeReconcileMissingItems(documentId: documentId, remoteIds: itemIds, now: pullStart)

            // Purge: uploaded item tombstones, then the document tombstone.
            let purgeCutoff = pullStart - QuickNoteRules.shared.PURGE_AFTER_MILLIS
            let purgedItems = await purgeItems(itemsRef: itemsRef, documentId: documentId, cutoff: purgeCutoff)
            let purgedDoc = await purgeDocument(docRef: docRef, documentId: documentId, cutoff: purgeCutoff)
            let purged = purgedItems + purgedDoc

            UserDefaults.standard.set(Double(pullStart), forKey: Self.lastSyncedAtKey(documentId))
            uiState = NestedSyncUiState(status: .synced, documentId: documentId, lastSyncedAt: Date(timeIntervalSince1970: Double(pullStart) / 1000.0))
            print("[NestedSync] doc=\(documentId) pushed=\(pushed) pulled=\(itemJsons.count) applied=\(applied) reconciled=\(reconciled) purged=\(purged)")
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

        await writePreSyncSnapshot(documentId: nil)
        guard let prepared = await prepareSync(documentId: nil) else { return }
        let (userId, db) = prepared
        do {
            print("[NestedSync] docs started uid=\(userId)")
            let docsRef = db.collection(Self.config.USERS_COLLECTION).document(userId)
                .collection(Self.config.DOCUMENTS_COLLECTION)

            // Push: all dirty document rows (batched), plus all dirty
            // items anywhere so list sync never strands edits.
            let docMaps = try await bridgeDirtyDocumentJsons().compactMap { Self.documentDict(json: $0) }
            let pushedDocs = try await pushJsons(
                docMaps,
                into: docsRef,
                db: db
            ) { ids, maxUpdated in
                try await bridgeMarkDocumentsClean(ids: ids, maxUpdatedAt: maxUpdated)
            }
            let pushed = pushedDocs + (try await pushAllDirtyItems(db: db, userId: userId))

            // Pull: full documents. Manual sync converges: LWW applies
            // remote wins, delete detection tombstones clean rows
            // missing remotely.
            var applied = 0
            let pullStart = Self.nowMillis()
            let snapshot = try await docsRef.getDocuments()
            var docJsons: [String] = []
            for doc in snapshot.documents {
                guard let json = Self.jsonString(for: doc.data(), documentId: doc.documentID) else { continue }
                docJsons.append(json)
            }
            for json in docJsons {
                if try await bridgeApplyRemoteDocument(json: json) {
                    applied += 1
                }
            }
            let docIds = docJsons.compactMap { Self.documentDict(json: $0)?["id"] as? String }
            let reconciled = try await bridgeReconcileMissingDocuments(remoteIds: docIds, now: pullStart)

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
            print("[NestedSync] docs pushed=\(pushed) pulled=\(docJsons.count) applied=\(applied) reconciled=\(reconciled) purged=\(purged)")
        } catch {
            recordSyncFailure(documentId: nil, error: error)
        }
    }

    // MARK: - Steps

    /// Pre-sync safety snapshot (full nested JSON, last 3 kept). Never
    /// throws: a snapshot must never break a sync.
    private func writePreSyncSnapshot(documentId: String?) async {
        do {
            guard let json = try await bridgeExportSnapshot() else { return }
            let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
                .appendingPathComponent("NestedSyncSnapshots", isDirectory: true)
            try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
            let name = "nested_\(documentId ?? "docs")_\(Self.nowMillis()).json"
            try json.write(to: dir.appendingPathComponent(name), atomically: true, encoding: .utf8)
            let files = try FileManager.default.contentsOfDirectory(
                at: dir,
                includingPropertiesForKeys: [.contentModificationDateKey],
                options: .skipsHiddenFiles
            )
            let dated = try files.map { url -> (URL, Date) in
                let values = try url.resourceValues(forKeys: [.contentModificationDateKey])
                return (url, values.contentModificationDate ?? .distantPast)
            }.sorted { $0.1 < $1.1 }
            for stale in dated.map(\.0).dropLast(Self.snapshotKeep) {
                try? FileManager.default.removeItem(at: stale)
            }
        } catch {
            print("[NestedSync] Pre-sync snapshot failed; continuing sync: \(error)")
        }
    }

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

    /// Pushes every dirty item across all documents, grouped by parent
    /// collection. Both buttons use this so no edit is ever stranded by
    /// tapping the "wrong" one; pull scopes stay per-button.
    private func pushAllDirtyItems(db: Firestore, userId: String) async throws -> Int {
        let maps = try await bridgeDirtyAllItemJsons().compactMap { Self.documentDict(json: $0) }
        var grouped: [String: [[String: Any]]] = [:]
        for map in maps {
            guard let docId = map["documentId"] as? String, !docId.isEmpty else { continue }
            grouped[docId, default: []].append(map)
        }
        var pushed = 0
        for (docId, group) in grouped {
            let itemsRef = db.collection(Self.config.USERS_COLLECTION).document(userId)
                .collection(Self.config.DOCUMENTS_COLLECTION).document(docId)
                .collection(Self.config.ITEMS_COLLECTION)
            pushed += try await pushJsons(group, into: itemsRef, db: db) { ids, maxUpdated in
                try await bridgeMarkItemsClean(ids: ids, maxUpdatedAt: maxUpdated)
            }
        }
        return pushed
    }

    /// Pushes maps into a collection in batches, clears dirty flags
    /// guarded by the pushed watermark, and returns the pushed count. Rows
    /// without an id are skipped before batching so counts stay accurate.
    private func pushJsons(
        _ maps: [[String: Any]],
        into collection: CollectionReference,
        db: Firestore,
        markClean: ([String], Int64) async throws -> Void
    ) async throws -> Int {
        let rows = maps.compactMap { map -> (String, Int64, [String: Any])? in
            guard let id = map["id"] as? String else { return nil }
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

    private func bridgeDirtyAllItemJsons() async throws -> [String] {
        try await withCheckedThrowingContinuation { cont in
            bridge().dirtyAllItemJsons { jsons, error in
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

    private func bridgeExportSnapshot() async throws -> String? {
        try await withCheckedThrowingContinuation { cont in
            bridge().exportNestedSnapshotJson { json, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: json) }
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

    private func bridgeReconcileDocumentMissing(documentId: String, now: Int64) async throws -> Bool {
        try await withCheckedThrowingContinuation { cont in
            bridge().reconcileDocumentMissing(documentId: documentId, nowMillis: now) { reconciled, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: reconciled?.boolValue ?? false) }
            }
        }
    }

    private func bridgeReconcileMissingDocuments(remoteIds: [String], now: Int64) async throws -> Int {
        try await withCheckedThrowingContinuation { cont in
            bridge().reconcileMissingDocuments(remoteIds: remoteIds, nowMillis: now) { reconciled, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: Int(truncating: reconciled ?? 0)) }
            }
        }
    }

    private func bridgeReconcileMissingItems(documentId: String, remoteIds: [String], now: Int64) async throws -> Int {
        try await withCheckedThrowingContinuation { cont in
            bridge().reconcileMissingItems(documentId: documentId, remoteIds: remoteIds, nowMillis: now) { reconciled, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: Int(truncating: reconciled ?? 0)) }
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
