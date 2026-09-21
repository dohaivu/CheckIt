//
//  NestedEditorState.swift
//  macosApp — full nested-list editor window.
//
//  Observable state around the shared KMP nested-list use cases. SwiftUI
//  cannot consume suspend functions or Kotlin Flow directly, so all KMP
//  interop goes through NestedAppleHelper callbacks (same pattern as
//  QuickNoteMenuState). Tree math (flatten, zoom, breadcrumbs) runs locally
//  in Swift over tree.rootNodes; mutations go through the helper, Room is
//  the source of truth, and the tree Flow re-emits — which is also what
//  keeps Android and macOS in sync.
//

import SwiftUI
import Combine
import Shared

/// One visible outline row: node + indent depth.
struct NestedRow: Identifiable, Equatable {
    let id: Int64
    let depth: Int
    let node: NestedItemNode

    static func == (lhs: NestedRow, rhs: NestedRow) -> Bool {
        lhs.id == rhs.id && lhs.depth == rhs.depth
    }
}

/// Draft for the inline "new item" row.
struct NestedDraft: Equatable {
    /// Anchor row id below which the draft sits; -1 inserts at root head.
    var anchorId: Int64 = -1
    /// Parent id; -1 means document root.
    var parentId: Int64 = -1
    var depth: Int = 0
    var text: String = ""
}

private let noDate = Int32.min

/// Epoch days (LocalDate.toEpochDays()) <-> Foundation Date.
func nestedDate(fromEpochDays days: Int64) -> Date {
    Date(timeIntervalSince1970: Double(days) * 86400.0)
}

func nestedEpochDays(from date: Date) -> Int64 {
    Int64((date.timeIntervalSince1970 / 86400.0).rounded(.down))
}

@MainActor
final class NestedEditorState: ObservableObject {
    @Published var documents: [NestedDocument] = []
    @Published var availableTags: [TagItem] = []
    @Published var tree: NestedDocumentTree?
    @Published var selectedDocId: Int64 = -1
    @Published var zoomPath: [Int64] = []
    @Published var selectedId: Int64? = nil
    @Published var editingId: Int64? = nil
    @Published var draft: NestedDraft? = nil
    @Published var showDeleteConfirm = false
    @Published var lastError: String? = nil

    private let helper: NestedAppleHelper
    private var docsSub: NestedAppleSubscription?
    private var tagsSub: NestedAppleSubscription?
    private var treeSub: NestedAppleSubscription?

    init() {
        NestedAppleBridge.shared.ensureKoin()
        helper = NestedAppleBridge.shared.helper()
    }

    func start() {
        if docsSub == nil {
            docsSub = helper.observeDocumentList { [weak self] docs in
                DispatchQueue.main.async {
                    self?.documents = docs
                    // Auto-select last opened doc, else first.
                    if let self, self.selectedDocId < 0, let first = docs.first {
                        let saved = UserDefaults.standard.string(forKey: "nested.lastDoc").flatMap(Int64.init)
                        let target = saved.flatMap { id in docs.first(where: { $0.id == id }) } ?? first
                        self.openDocument(id: target.id)
                    } else if let self, !docs.contains(where: { $0.id == self.selectedDocId }) {
                        self.closeDocument()
                    }
                }
            }
        }
        if tagsSub == nil {
            tagsSub = helper.observeTagList { [weak self] tags in
                DispatchQueue.main.async { self?.availableTags = tags }
            }
        }
    }

    func stop() {
        docsSub?.cancel(); docsSub = nil
        tagsSub?.cancel(); tagsSub = nil
        treeSub?.cancel(); treeSub = nil
    }

    // MARK: - Documents

    var selectedDocument: NestedDocument? {
        documents.first(where: { $0.id == selectedDocId })
    }

    func openDocument(id: Int64) {
        if selectedDocId == id, tree != nil { return }
        treeSub?.cancel()
        tree = nil
        zoomPath = []
        selectedId = nil
        editingId = nil
        draft = nil
        selectedDocId = id
        UserDefaults.standard.set(String(id), forKey: "nested.lastDoc")
        treeSub = helper.observeDocumentTree(documentId: id) { [weak self] tree in
            DispatchQueue.main.async { self?.tree = tree }
        }
    }

    func closeDocument() {
        treeSub?.cancel(); treeSub = nil
        tree = nil
        selectedDocId = -1
        zoomPath = []
        selectedId = nil
        editingId = nil
        draft = nil
    }

    func createDocument(title: String) {
        helper.createDocument(title: title) { [weak self] newId in
            DispatchQueue.main.async {
                let id = newId.int64Value
                if id >= 0 { self?.openDocument(id: id) }
            }
        }
    }

    func renameDocument(id: Int64, title: String) {
        helper.renameDocument(documentId: id, title: title)
    }

    func deleteDocument(id: Int64) {
        helper.removeDocument(documentId: id)
    }

    // MARK: - Tree traversal (local; mirrors flattenVisibleNodes)

    private func findNode(_ id: Int64, in nodes: [NestedItemNode]) -> NestedItemNode? {
        for node in nodes {
            if node.item.id == id { return node }
            if let found = findNode(id, in: node.children) { return found }
        }
        return nil
    }

    /// Chain from a root down to id (for breadcrumbs), or nil.
    func chain(to id: Int64) -> [NestedItemNode]? {
        guard let tree else { return nil }
        return chain(to: id, in: tree.rootNodes)
    }

    private func chain(to id: Int64, in nodes: [NestedItemNode]) -> [NestedItemNode]? {
        for node in nodes {
            if node.item.id == id { return [node] }
            if let sub = chain(to: id, in: node.children) { return [node] + sub }
        }
        return nil
    }

    var visibleRows: [NestedRow] {
        guard let tree else { return [] }
        let roots: [NestedItemNode]
        if let zid = zoomPath.last, let focused = findNode(zid, in: tree.rootNodes) {
            roots = [focused]
        } else {
            roots = tree.rootNodes
        }
        var out: [NestedRow] = []
        var stack = roots.reversed().map { ($0, 0) }
        while let (node, depth) = stack.popLast() {
            out.append(NestedRow(id: node.item.id, depth: depth, node: node))
            if !node.item.collapsed {
                for child in node.children.reversed() {
                    stack.append((child, depth + 1))
                }
            }
        }
        return out
    }

    var indexById: [Int64: NestedItemNode] {
        guard let tree else { return [:] }
        var map: [Int64: NestedItemNode] = [:]
        var stack: [NestedItemNode] = tree.rootNodes
        while let node = stack.popLast() {
            map[node.item.id] = node
            stack.append(contentsOf: node.children)
        }
        return map
    }

    func summary(for id: Int64) -> NestedMetricSummary? {
        guard let tree else { return nil }
        return helper.summaryFor(tree: tree, itemId: id)
    }

    // MARK: - Selection / editing

    func select(id: Int64) {
        if draft != nil { draft = nil }
        selectedId = (selectedId == id) ? nil : id
        editingId = nil
    }

    func startEdit(id: Int64) {
        selectedId = id
        editingId = id
        draft = nil
    }

    func commitEdit(id: Int64, text: String) {
        editingId = nil
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        helper.saveItemText(itemId: id, text: trimmed)
    }

    func cancelEdit() { editingId = nil }

    // MARK: - Draft (add)

    func startAddRoot() {
        editingId = nil
        draft = NestedDraft(anchorId: -1, parentId: -1, depth: 0, text: "")
    }

    func startAddChild(of id: Int64) {
        guard let node = indexById[id] else { return }
        editingId = nil
        // Anchor after the subtree's last visible descendant (ViewModel parity).
        var anchor = node
        while let last = anchor.children.last, !anchor.item.collapsed {
            anchor = last
        }
        let depth = (visibleRows.first(where: { $0.id == id })?.depth ?? 0) + 1
        draft = NestedDraft(anchorId: anchor.item.id, parentId: id, depth: depth, text: "")
    }

    func startAddSibling(of id: Int64) {
        guard let node = indexById[id] else { return }
        editingId = nil
        let depth = visibleRows.first(where: { $0.id == id })?.depth ?? 0
        draft = NestedDraft(
            anchorId: id,
            parentId: node.item.parentId?.int64Value ?? -1,
            depth: depth,
            text: ""
        )
    }

    func commitDraft(thenContinue: Bool) {
        guard let d = draft, selectedDocId >= 0 else { return }
        let text = d.text
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            draft = nil
            return
        }
        helper.commitItem(
            documentId: selectedDocId,
            anchorId: d.anchorId,
            parentId: d.parentId,
            text: text
        ) { [weak self] newId in
            DispatchQueue.main.async {
                guard let self else { return }
                let id = newId.int64Value
                if id < 0 { self.draft = nil; return }
                if thenContinue {
                    self.selectedId = id
                    self.draft = NestedDraft(anchorId: id, parentId: d.parentId, depth: d.depth, text: "")
                } else {
                    self.draft = nil
                    self.selectedId = id
                }
            }
        }
    }

    func cancelDraft() { draft = nil }

    // MARK: - Structure ops on selection

    func indentSelected() {
        guard let id = selectedId, selectedDocId >= 0 else { return }
        helper.indent(documentId: selectedDocId, itemId: id)
    }

    func outdentSelected() {
        guard let id = selectedId, selectedDocId >= 0 else { return }
        helper.outdent(documentId: selectedDocId, itemId: id)
    }

    func moveSelectedUp() {
        guard let id = selectedId, selectedDocId >= 0 else { return }
        helper.moveUp(documentId: selectedDocId, itemId: id)
    }

    func moveSelectedDown() {
        guard let id = selectedId, selectedDocId >= 0 else { return }
        helper.moveDown(documentId: selectedDocId, itemId: id)
    }

    func toggleCollapse(id: Int64) { helper.toggleCollapsed(itemId: id) }

    func toggleCheck(id: Int64, checked: Bool) { helper.toggleChecked(itemId: id, checked: checked) }

    func confirmDeleteSelected() {
        guard let id = selectedId else { return }
        helper.deleteSingleItem(itemId: id)
        selectedId = nil
        editingId = nil
    }

    // MARK: - Zoom

    func zoomInSelected() {
        guard let id = selectedId, let node = indexById[id], node.hasChildren else { return }
        if let c = chain(to: id) {
            zoomPath = c.map { $0.item.id }
        }
    }

    func zoomOut() { _ = zoomPath.popLast() }

    func zoomToRoot() { zoomPath = [] }

    func zoomTo(id: Int64) {
        if let c = chain(to: id) { zoomPath = c.map { $0.item.id } }
    }

    // MARK: - Drag & drop (gap-based sibling drop)

    /// Drop draggedId onto targetId's gap: sibling just above target.
    /// Own-subtree drops are refused by shared moveToPosition (no-op).
    func drop(draggedId: Int64, onto targetId: Int64) {
        guard draggedId != targetId, selectedDocId >= 0 else { return }
        guard let target = indexById[targetId] else { return }
        let parentId = target.item.parentId?.int64Value ?? -1
        let siblings: [NestedItemNode]
        if parentId < 0 {
            guard let tree else { return }
            siblings = tree.rootNodes
        } else {
            guard let parent = indexById[parentId] else { return }
            siblings = parent.children
        }
        let ids = siblings.map { $0.item.id }.filter { $0 != draggedId }
        guard let targetIdx = ids.firstIndex(of: targetId) else { return }
        helper.moveTo(documentId: selectedDocId, itemId: draggedId, targetParentId: parentId, targetIndex: Int32(targetIdx))
    }

    /// Drop draggedId as last child of targetId.
    func dropAsChild(draggedId: Int64, onto targetId: Int64) {
        guard draggedId != targetId, selectedDocId >= 0 else { return }
        guard let target = indexById[targetId] else { return }
        helper.moveTo(
            documentId: selectedDocId,
            itemId: draggedId,
            targetParentId: targetId,
            targetIndex: Int32(target.children.count)
        )
    }

    // MARK: - Metadata mutations (thin wrappers over shared use cases)

    func saveNote(id: Int64, text: String) {
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        helper.saveItemNote(itemId: id, note: t.isEmpty ? nil : t)
    }

    func updateFormatting(id: Int64, style: String, textColor: String, background: String) {
        helper.updateFormattingByName(itemId: id, styleName: style, textColorName: textColor, backgroundColorName: background)
    }

    func updatePriority(id: Int64, name: String) {
        helper.updatePriorityByName(itemId: id, priorityName: name)
    }

    func setTag(id: Int64, tagId: Int64, enabled: Bool) {
        helper.setTagEnabled(itemId: id, tagId: tagId, enabled: enabled)
    }

    func setCheckboxEnabled(id: Int64, enabled: Bool) {
        helper.toggleCheckboxEnabled(itemId: id, enabled: enabled)
    }

    func setCheckedExact(id: Int64, checked: Bool) {
        helper.toggleChecked(itemId: id, checked: checked)
    }

    func updateDates(id: Int64, start: Int64?, end: Int64?) {
        helper.updateDateRangeDays(
            itemId: id,
            startEpochDays: start.map { Int32($0) } ?? Int32.min,
            endEpochDays: end.map { Int32($0) } ?? Int32.min
        )
    }

    func updateMetricsSettings(id: Int64, minutes: Int32, policy: String, show: Bool) {
        helper.updateMetricSettingsByName(itemId: id, actualMinutes: minutes, policyName: policy, showTracked: show)
    }

    func updateProgress(id: Int64, value: Int32?) {
        helper.updateProgressValue(itemId: id, progress: value ?? -1)
    }

    func replaceMetrics(id: Int64, json: String) {
        helper.replaceMetricsJson(itemId: id, json: json)
    }

    // MARK: - Keyboard navigation

    func moveSelection(by delta: Int) {
        let rows = visibleRows
        guard !rows.isEmpty else { return }
        if let id = selectedId, let idx = rows.firstIndex(where: { $0.id == id }) {
            let next = min(max(idx + delta, 0), rows.count - 1)
            selectedId = rows[next].id
        } else {
            selectedId = rows[delta > 0 ? 0 : rows.count - 1].id
        }
        editingId = nil
        if draft != nil { draft = nil }
    }
}
