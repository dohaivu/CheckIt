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
import AppKit
import Shared

/// One visible outline row: node + indent depth.
struct NestedRow: Identifiable, Equatable {
    let id: String
    let depth: Int
    let node: NestedItemNode

    static func == (lhs: NestedRow, rhs: NestedRow) -> Bool {
        lhs.id == rhs.id
            && lhs.depth == rhs.depth
            && lhs.node.item.updatedAtMillis == rhs.node.item.updatedAtMillis
    }
}

/// Draft for the inline "new item" row.
struct NestedDraft: Equatable {
    /// Anchor row id below which the draft sits; blank inserts at root head.
    var anchorId: String = ""
    /// Parent id; blank means document root.
    var parentId: String = ""
    var depth: Int = 0
    var text: String = ""
}

/// Active drop target resolution for outline drag & drop.
struct NestedDropTarget: Equatable {
    let parentId: String?
    let index: Int
    let displayRowId: String
    let below: Bool
    let depth: Int
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
    @Published var selectedDocId: String = ""
    @Published var zoomPath: [String] = []
    @Published var selectedId: String? = nil
    @Published var editingId: String? = nil
    @Published var draft: NestedDraft? = nil
    @Published var showDeleteConfirm = false
    @Published var lastError: String? = nil
    @Published var draggedId: String? = nil
    @Published var dropTarget: NestedDropTarget? = nil

    private let helper: NestedAppleHelper
    private var docsSub: NestedAppleSubscription?
    private var tagsSub: NestedAppleSubscription?
    private var treeSub: NestedAppleSubscription?
    private var hostWindow: NSWindow?
    private var tabMonitor: Any?
    /// Rebuilt once per tree emission instead of for every render and action.
    private var nodeIndex: [String: NestedItemNode] = [:]

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
                    if let self, self.selectedDocId.isEmpty, let first = docs.first {
                        let saved = UserDefaults.standard.string(forKey: "nested.lastDoc")
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
        detachWindow()
    }

    // MARK: - Tab key monitor (Shift+Tab reliability)

    /// AppKit focus navigation can swallow Shift+Tab before SwiftUI's
    /// onKeyPress sees it, so nav-mode Tab is intercepted here at the
    /// NSEvent level. Edit/draft Tabs keep flowing to the SwiftUI field
    /// handlers, which own the in-progress text.
    func attachWindow(_ window: NSWindow?) {
        if hostWindow === window { return }
        detachWindow()
        hostWindow = window
        guard window != nil else { return }
        tabMonitor = NSEvent.addLocalMonitorForEvents(matching: .keyDown) { [weak self] event in
            guard let self,
                  let window = self.hostWindow,
                  event.window == window,
                  window.attachedSheet == nil,
                  event.keyCode == 48, // Tab
                  !(window.firstResponder is NSText)
            else { return event }
            let handled = MainActor.assumeIsolated { self.handleTabKey(shift: event.modifierFlags.contains(.shift)) }
            return handled ? nil : event
        }
    }

    func detachWindow() {
        if let m = tabMonitor { NSEvent.removeMonitor(m); tabMonitor = nil }
        hostWindow = nil
    }

    private func handleTabKey(shift: Bool) -> Bool {
        guard editingId == nil, draft == nil, selectedId != nil else { return false }
        if shift { outdentSelected() } else { indentSelected() }
        return true
    }

    // MARK: - Documents

    var selectedDocument: NestedDocument? {
        documents.first(where: { $0.id == selectedDocId })
    }

    func openDocument(id: String) {
        if selectedDocId == id, tree != nil { return }
        treeSub?.cancel()
        clearTree()
        zoomPath = []
        selectedId = nil
        editingId = nil
        draft = nil
        selectedDocId = id
        UserDefaults.standard.set(id, forKey: "nested.lastDoc")
        treeSub = helper.observeDocumentTree(documentId: id) { [weak self] tree in
            DispatchQueue.main.async { self?.replaceTree(tree) }
        }
    }

    func closeDocument() {
        treeSub?.cancel(); treeSub = nil
        clearTree()
        selectedDocId = ""
        zoomPath = []
        selectedId = nil
        editingId = nil
        draft = nil
    }

    func createDocument(title: String) {
        helper.createDocument(title: title) { [weak self] newId in
            DispatchQueue.main.async {
                if !newId.isEmpty { self?.openDocument(id: newId) }
            }
        }
    }

    func renameDocument(id: String, title: String) {
        helper.renameDocument(documentId: id, title: title)
    }

    func deleteDocument(id: String) {
        helper.removeDocument(documentId: id)
    }

    // MARK: - Tree traversal (local; mirrors flattenVisibleNodes)

    private func findNode(_ id: String, in nodes: [NestedItemNode]) -> NestedItemNode? {
        for node in nodes {
            if node.item.id == id { return node }
            if let found = findNode(id, in: node.children) { return found }
        }
        return nil
    }

    /// Chain from a root down to id (for breadcrumbs), or nil.
    func chain(to id: String) -> [NestedItemNode]? {
        guard let tree else { return nil }
        return chain(to: id, in: tree.rootNodes)
    }

    private func chain(to id: String, in nodes: [NestedItemNode]) -> [NestedItemNode]? {
        for node in nodes {
            if node.item.id == id { return [node] }
            if let sub = chain(to: id, in: node.children) { return [node] + sub }
        }
        return nil
    }

    var visibleRows: [NestedRow] {
        guard let tree else { return [] }
        let roots: [NestedItemNode]
        if let zid = zoomPath.last, let focused = nodeIndex[zid] {
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

    var indexById: [String: NestedItemNode] {
        nodeIndex
    }

    private func replaceTree(_ newTree: NestedDocumentTree) {
        var map: [String: NestedItemNode] = [:]
        var stack: [NestedItemNode] = newTree.rootNodes
        while let node = stack.popLast() {
            map[node.item.id] = node
            stack.append(contentsOf: node.children)
        }
        nodeIndex = map
        tree = newTree
    }

    private func clearTree() {
        nodeIndex = [:]
        tree = nil
    }

    func summary(for id: String) -> NestedMetricSummary? {
        guard let tree else { return nil }
        return helper.summaryFor(tree: tree, itemId: id)
    }

    // MARK: - Selection / editing

    func select(id: String) {
        if draft != nil { draft = nil }
        selectedId = (selectedId == id) ? nil : id
        editingId = nil
    }

    func startEdit(id: String) {
        selectedId = id
        editingId = id
        draft = nil
    }

    func commitEdit(id: String, text: String) {
        editingId = nil
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        helper.saveItemText(itemId: id, text: trimmed)
    }

    func cancelEdit() { editingId = nil }

    // MARK: - Draft (add)

    func startAddRoot() {
        editingId = nil
        draft = NestedDraft(anchorId: "", parentId: "", depth: 0, text: "")
    }

    func startAddChild(of id: String) {
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

    func startAddSibling(of id: String) {
        guard let node = indexById[id] else { return }
        editingId = nil
        let depth = visibleRows.first(where: { $0.id == id })?.depth ?? 0
        draft = NestedDraft(
            anchorId: id,
            parentId: node.item.parentId ?? "",
            depth: depth,
            text: ""
        )
    }

    func commitDraft(thenContinue: Bool) {
        guard let d = draft, !selectedDocId.isEmpty else { return }
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
                if newId.isEmpty { self.draft = nil; return }
                if thenContinue {
                    self.selectedId = newId
                    self.draft = NestedDraft(anchorId: newId, parentId: d.parentId, depth: d.depth, text: "")
                } else {
                    self.draft = nil
                    self.selectedId = newId
                }
            }
        }
    }

    func cancelDraft() { draft = nil }

    // MARK: - Structure ops on selection

    func indentSelected() {
        guard let id = selectedId, !selectedDocId.isEmpty else { return }
        helper.indent(documentId: selectedDocId, itemId: id)
    }

    func outdentSelected() {
        guard let id = selectedId, !selectedDocId.isEmpty else { return }
        helper.outdent(documentId: selectedDocId, itemId: id)
    }

    func moveSelectedUp() {
        guard let id = selectedId, !selectedDocId.isEmpty else { return }
        helper.moveUp(documentId: selectedDocId, itemId: id)
    }

    func moveSelectedDown() {
        guard let id = selectedId, !selectedDocId.isEmpty else { return }
        helper.moveDown(documentId: selectedDocId, itemId: id)
    }

    func toggleCollapse(id: String) { helper.toggleCollapsed(itemId: id) }

    func toggleCheck(id: String, checked: Bool) { helper.toggleChecked(itemId: id, checked: checked) }

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

    func zoomTo(id: String) {
        if let c = chain(to: id) { zoomPath = c.map { $0.item.id } }
    }

    // MARK: - Drag & drop

    func isDescendant(itemId: String, of ancestorId: String) -> Bool {
        var cursor: String? = itemId
        while let c = cursor, !c.isEmpty {
            if c == ancestorId { return true }
            cursor = indexById[c]?.item.parentId
        }
        return false
    }

    func siblings(of parentId: String?) -> [NestedItemNode] {
        if let pid = parentId, !pid.isEmpty {
            return indexById[pid]?.children ?? []
        } else {
            return tree?.rootNodes ?? []
        }
    }

    func updateDropTarget(hoveredRow: NestedRow, isBelow: Bool, pointerX: CGFloat, draggedItemId: String?) {
        let resolved = resolveDropTarget(
            hoveredRow: hoveredRow,
            isBelow: isBelow,
            pointerX: pointerX,
            draggedItemId: draggedItemId
        )
        if dropTarget != resolved {
            dropTarget = resolved
        }
    }

    func clearDropTarget() {
        if dropTarget != nil {
            dropTarget = nil
        }
    }

    func resolveDropTarget(
        hoveredRow: NestedRow,
        isBelow: Bool,
        pointerX: CGFloat,
        draggedItemId: String?
    ) -> NestedDropTarget? {
        guard let dragged = draggedItemId ?? draggedId, !dragged.isEmpty else { return nil }
        guard let tree else { return nil }

        let rows = visibleRows
        guard let hoveredRowIdx = rows.firstIndex(where: { $0.id == hoveredRow.id }) else { return nil }

        let aboveRow: NestedRow?
        let belowRow: NestedRow?
        if !isBelow {
            aboveRow = (hoveredRowIdx > 0) ? rows[hoveredRowIdx - 1] : nil
            belowRow = hoveredRow
        } else {
            aboveRow = hoveredRow
            belowRow = (hoveredRowIdx < rows.count - 1) ? rows[hoveredRowIdx + 1] : nil
        }

        let minDepth = belowRow?.depth ?? 0
        let maxDepth = (aboveRow != nil) ? (aboveRow!.depth + 1) : 0
        let effectiveMinDepth = min(minDepth, maxDepth)

        // Indentation calculation: 16pt per depth level.
        // Option modifier forces max allowed depth (nest as child).
        let isOptionPressed = NSEvent.modifierFlags.contains(.option)
        let targetDepth: Int
        if isOptionPressed {
            targetDepth = maxDepth
        } else {
            let rawDepth = max(0, Int((pointerX - 4) / 16))
            targetDepth = min(max(rawDepth, effectiveMinDepth), maxDepth)
        }

        let targetParentId: String?
        let targetIndex: Int

        if aboveRow == nil {
            targetParentId = belowRow?.node.item.parentId
            targetIndex = 0
        } else if targetDepth == aboveRow!.depth + 1 {
            targetParentId = aboveRow!.node.item.id
            let s = siblings(of: targetParentId).filter { $0.item.id != dragged }
            targetIndex = s.count
        } else if targetDepth == aboveRow!.depth {
            targetParentId = aboveRow!.node.item.parentId
            let s = siblings(of: targetParentId).filter { $0.item.id != dragged }
            if let aboveIdx = s.firstIndex(where: { $0.item.id == aboveRow!.node.item.id }) {
                targetIndex = aboveIdx + 1
            } else {
                targetIndex = s.count
            }
        } else {
            let levelsUp = aboveRow!.depth - targetDepth
            var ancestorItem: NestedListItem? = aboveRow!.node.item
            for _ in 0..<levelsUp {
                if let pid = ancestorItem?.parentId, !pid.isEmpty {
                    ancestorItem = indexById[pid]?.item
                } else {
                    ancestorItem = nil
                    break
                }
            }
            if let ancestor = ancestorItem {
                targetParentId = ancestor.parentId
                let s = siblings(of: targetParentId).filter { $0.item.id != dragged }
                if let ancIdx = s.firstIndex(where: { $0.item.id == ancestor.id }) {
                    targetIndex = ancIdx + 1
                } else {
                    targetIndex = s.count
                }
            } else {
                targetParentId = nil
                let s = siblings(of: nil).filter { $0.item.id != dragged }
                targetIndex = s.count
            }
        }

        // Prevent dragging into self or own subtree
        if let pid = targetParentId, !pid.isEmpty {
            if pid == dragged || isDescendant(itemId: pid, of: dragged) {
                return nil
            }
        }

        let displayRowId: String
        let displayBelow: Bool
        if let belowRow = belowRow {
            displayRowId = belowRow.node.item.id
            displayBelow = false
        } else if let aboveRow = aboveRow {
            displayRowId = aboveRow.node.item.id
            displayBelow = true
        } else {
            displayRowId = hoveredRow.node.item.id
            displayBelow = isBelow
        }

        return NestedDropTarget(
            parentId: targetParentId,
            index: targetIndex,
            displayRowId: displayRowId,
            below: displayBelow,
            depth: targetDepth
        )
    }

    func executeDrop(target: NestedDropTarget, draggedId: String) {
        guard !selectedDocId.isEmpty, draggedId != target.parentId else {
            self.dropTarget = nil
            self.draggedId = nil
            return
        }
        let pid = target.parentId ?? ""
        if !pid.isEmpty && isDescendant(itemId: pid, of: draggedId) {
            self.dropTarget = nil
            self.draggedId = nil
            return
        }

        // Auto-expand parent if collapsed so the moved item is visible
        if let targetPid = target.parentId, let parentNode = indexById[targetPid], parentNode.item.collapsed {
            helper.toggleCollapsed(itemId: targetPid)
        }

        helper.moveTo(
            documentId: selectedDocId,
            itemId: draggedId,
            targetParentId: pid,
            targetIndex: Int32(target.index)
        )
        self.dropTarget = nil
        self.draggedId = nil
    }

    /// Drop draggedId onto targetId's gap: sibling just above target.
    /// Own-subtree drops are refused by shared moveToPosition (no-op).
    func drop(draggedId: String, onto targetId: String) {
        guard let row = visibleRows.first(where: { $0.id == targetId }) else { return }
        if let dt = resolveDropTarget(hoveredRow: row, isBelow: false, pointerX: 0, draggedItemId: draggedId) {
            executeDrop(target: dt, draggedId: draggedId)
        }
    }

    /// Drop draggedId as last child of targetId.
    func dropAsChild(draggedId: String, onto targetId: String) {
        guard let row = visibleRows.first(where: { $0.id == targetId }) else { return }
        if let dt = resolveDropTarget(hoveredRow: row, isBelow: true, pointerX: 200, draggedItemId: draggedId) {
            executeDrop(target: dt, draggedId: draggedId)
        }
    }

    // MARK: - Metadata mutations (thin wrappers over shared use cases)

    func saveNote(id: String, text: String) {
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        helper.saveItemNote(itemId: id, note: t.isEmpty ? nil : t)
    }

    func updateFormatting(id: String, style: String, textColor: String, background: String) {
        helper.updateFormattingByName(itemId: id, styleName: style, textColorName: textColor, backgroundColorName: background)
    }

    func updatePriority(id: String, name: String) {
        helper.updatePriorityByName(itemId: id, priorityName: name)
    }

    func setTag(id: String, tagId: String, enabled: Bool) {
        helper.setTagEnabled(itemId: id, tagId: tagId, enabled: enabled)
    }

    func setCheckboxEnabled(id: String, enabled: Bool) {
        helper.toggleCheckboxEnabled(itemId: id, enabled: enabled)
    }

    func setCheckedExact(id: String, checked: Bool) {
        helper.toggleChecked(itemId: id, checked: checked)
    }

    func updateDates(id: String, start: Int64?, end: Int64?) {
        helper.updateDateRangeDays(
            itemId: id,
            startEpochDays: start.map { Int32($0) } ?? Int32.min,
            endEpochDays: end.map { Int32($0) } ?? Int32.min
        )
    }

    func updateMetricsSettings(id: String, minutes: Int32, policy: String, show: Bool) {
        helper.updateMetricSettingsByName(itemId: id, actualMinutes: minutes, policyName: policy, showTracked: show)
    }

    func updateProgress(id: String, value: Int32?) {
        helper.updateProgressValue(itemId: id, progress: value ?? -1)
    }

    func replaceMetrics(id: String, json: String) {
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
