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
import UniformTypeIdentifiers

/// Outline display filter. Named to avoid clashing with the shared
/// Kotlin `NestedDisplayType` (Android filter state) in the Shared module.
enum NestedDisplayFilter {
    case all
    case working
}

/// Tab shown in the trailing inspector pane. Persisted as its raw value.
enum NestedInspectorTab: String {
    case details
    case note
}

/// One visible outline row: node + indent depth.
struct NestedRow: Identifiable, Equatable {
    let id: String
    /// Position in `visibleRows`; focus selection uses the contiguous
    /// pre-order range of the selection branch.
    let index: Int
    let depth: Int
    let node: NestedItemNode
    /// Self or any ancestor checked (Android `isInCheckedBranch` parity).
    let isInCheckedBranch: Bool
    /// Kept only as ancestor scaffolding for a filtered match.
    var isContext: Bool = false

    static func == (lhs: NestedRow, rhs: NestedRow) -> Bool {
        lhs.id == rhs.id
            && lhs.depth == rhs.depth
            && lhs.isInCheckedBranch == rhs.isInCheckedBranch
            && lhs.isContext == rhs.isContext
            && lhs.node.item.collapsed == rhs.node.item.collapsed
            && lhs.node.item.updatedAtMillis == rhs.node.item.updatedAtMillis
    }
}

/// Render-time emphasis tier for the transient focus-selection mode.
enum NestedFocusTier {
    case none
    case focused
    case path
    case outside
}

/// Snapshot of the selection branch's pre-order range plus its ancestor path.
/// Computed once per selection/filter change; rows only do an O(1) check.
struct NestedFocusSnapshot: Equatable {
    var range: ClosedRange<Int>? = nil
    var ancestorIds: Set<String> = []

    static let inactive = NestedFocusSnapshot()

    func tier(for row: NestedRow) -> NestedFocusTier {
        guard let range else { return .none }
        if range.contains(row.index) { return .focused }
        if ancestorIds.contains(row.id) { return .path }
        return .outside
    }
}

/// Effective selection branch: the root row, its contiguous pre-order range,
/// and the guide column that carries its spine. Resolving this once lets rows
/// check membership in O(1) instead of walking ancestors one by one.
struct NestedSelectionBranch: Equatable {
    let rootId: String
    let range: ClosedRange<Int>
    let depth: Int
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
    @Published var zoomPath: [String] = [] {
        didSet { recomputeVisibleRows() }
    }
    @Published var selectedId: String? = nil {
        didSet { recomputeSelectionFocus() }
    }
    /// Transient view mode: dim everything outside the selection branch.
    /// Not persisted; resets with the editor's lifetime.
    @Published var focusSelectionEnabled: Bool = false {
        didSet { recomputeFocusSnapshot() }
    }
    /// Effective selection branch: selected node, or its visible parent for a
    /// leaf. Drives both focus scope and the selection spine.
    @Published private(set) var selectionBranch: NestedSelectionBranch? = nil
    /// Derived pre-order range + ancestor path for focus selection.
    @Published private(set) var focusSnapshot: NestedFocusSnapshot = .inactive
    @Published var displayType: NestedDisplayFilter = .all {
        didSet {
            UserDefaults.standard.set(displayType == .working ? "working" : "all", forKey: "nested.displayType")
            recomputeVisibleRows()
        }
    }
    /// Max lines of the note preview drawn under an outline row.
    /// 0 means unlimited; persisted like `displayType`.
    @Published var noteLineLimit: Int = 5 {
        didSet { UserDefaults.standard.set(noteLineLimit, forKey: "nested.noteLineLimit") }
    }
    @Published var searchQuery: String = "" {
        didSet {
            if searchQuery.isEmpty {
                // Clearing restores the full outline immediately; typing
                // debounces below so fast input doesn't re-filter per keystroke.
                searchTask?.cancel()
                appliedQuery = ""
            } else {
                debounceSearch()
            }
        }
    }
    /// Debounced mirror of searchQuery; the outline filters on this.
    @Published private(set) var appliedQuery: String = "" {
        didSet { recomputeVisibleRows() }
    }
    private var searchTask: Task<Void, Never>?
    @Published var editingId: String? = nil
    @Published var draft: NestedDraft? = nil
    @Published var showDeleteConfirm = false
    @Published var showNoteSheet = false
    @Published var showDetailsSheet = false
    /// Sidebar visibility; persisted so the window restores its layout.
    @Published var showSidebar = true {
        didSet { UserDefaults.standard.set(showSidebar, forKey: "nested.showSidebar") }
    }
    /// Trailing inspector visibility; persisted. Independent of the
    /// note/details sheets, which remain available as a legacy path.
    @Published var showInspector = true {
        didSet { UserDefaults.standard.set(showInspector, forKey: "nested.showInspector") }
    }
    /// Active inspector tab (details/metrics vs note); persisted.
    @Published var inspectorTab: NestedInspectorTab = .details {
        didSet { UserDefaults.standard.set(inspectorTab.rawValue, forKey: "nested.inspectorTab") }
    }
    @Published var lastError: String? = nil
    @Published var draggedId: String? = nil
    @Published var dropTarget: NestedDropTarget? = nil
    @Published var docDropTargetId: String? = nil
    @Published var docDropBelow: Bool = false
    @Published private(set) var visibleRows: [NestedRow] = []
    @Published var toastMessage: String? = nil

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
        if UserDefaults.standard.string(forKey: "nested.displayType") == "working" {
            displayType = .working
        }
        if let v = UserDefaults.standard.object(forKey: "nested.noteLineLimit") as? Int {
            noteLineLimit = max(0, min(50, v))
        }
        if let v = UserDefaults.standard.object(forKey: "nested.showSidebar") as? Bool {
            showSidebar = v
        }
        if let v = UserDefaults.standard.object(forKey: "nested.showInspector") as? Bool {
            showInspector = v
        }
        if let raw = UserDefaults.standard.string(forKey: "nested.inspectorTab"),
           let tab = NestedInspectorTab(rawValue: raw) {
            inspectorTab = tab
        }
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
        tabMonitor = NSEvent.addLocalMonitorForEvents(matching: [.keyDown, .leftMouseUp]) { [weak self] event in
            guard let self,
                  let window = self.hostWindow,
                  event.window == window,
                  window.attachedSheet == nil
            else { return event }

            if event.type == .leftMouseUp {
                if self.draggedId != nil || self.dropTarget != nil ||
                    self.docDropTargetId != nil {
                    MainActor.assumeIsolated {
                        self.draggedId = nil
                        self.dropTarget = nil
                        self.clearDocDropTarget()
                    }
                }
                return event
            }

            guard event.keyCode == 48, // Tab
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

    // MARK: - Panes (sidebar / inspector)

    func toggleSidebar() { showSidebar.toggle() }

    func toggleInspector() { showInspector.toggle() }

    /// Selected node's item, or nil when nothing/stale is selected.
    var selectedItem: NestedListItem? {
        guard let id = selectedId else { return nil }
        return indexById[id]?.item
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

    /// Sidebar drag-drop: move the dragged document above (`below` false)
    /// or below the target row, then persist the order. Skips when the
    /// result matches the current order (self-drops, no-op moves), so no
    /// redundant sync churn. Unknown ids are ignored.
    func moveDocument(draggedId: String, onto targetId: String, below: Bool) {
        var ids = documents.map { $0.id }.filter { $0 != draggedId }
        guard ids.count != documents.count,
              targetId != draggedId,
              let idx = ids.firstIndex(of: targetId)
        else { return }
        ids.insert(draggedId, at: below ? idx + 1 : idx)
        guard ids != documents.map({ $0.id }) else { return }
        helper.reorderDocuments(documentIds: ids)
    }

    func clearDocDropTarget() {
        docDropTargetId = nil
        docDropBelow = false
    }

    /// Hover highlight with same-value guard: dropEntered fires on every
    /// pointer movement, and republishing identical values rebuilds the
    /// list mid-drag (flicker, wedged sessions, stuck highlight).
    func showDocDropTarget(id: String, below: Bool) {
        if docDropTargetId == id && docDropBelow == below { return }
        docDropTargetId = id
        docDropBelow = below
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

    private func debounceSearch() {
        searchTask?.cancel()
        searchTask = Task { @MainActor [weak self] in
            try? await Task.sleep(nanoseconds: 150_000_000)
            guard !Task.isCancelled, let self else { return }
            self.appliedQuery = self.searchQuery
        }
    }

    private func recomputeVisibleRows() {
        guard let tree else {
            visibleRows = []
            recomputeSelectionFocus()
            return
        }
        let zoomRoots: [NestedItemNode]
        if let zid = zoomPath.last, let focused = nodeIndex[zid] {
            zoomRoots = [focused]
        } else {
            zoomRoots = tree.rootNodes
        }
        // Reuse the shared outline projection (text query + Working filter:
        // priority / due date / countdown metric + ancestor context) instead
        // of reimplementing it. Runs on the debounced query (see searchQuery).
        let query = appliedQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        let projection: NestedOutlineProjection? = (displayType == .working || !query.isEmpty)
            ? NestedAppleBridge.shared.filterOutlineProjection(
                roots: zoomRoots,
                query: query,
                workingOnly: displayType == .working
            )
            : nil
        let contextIds = Set(projection?.contextIds ?? [])
        let roots: [NestedItemNode] = projection?.roots ?? zoomRoots
        var out: [NestedRow] = []
        var stack = roots.reversed().map { ($0, 0, false) }
        while let (node, depth, ancestorChecked) = stack.popLast() {
            let inCheckedBranch = ancestorChecked || node.item.checked
            out.append(
                NestedRow(
                    id: node.item.id,
                    index: out.count,
                    depth: depth,
                    node: node,
                    isInCheckedBranch: inCheckedBranch,
                    isContext: contextIds.contains(node.item.id)
                )
            )
            if !node.item.collapsed {
                for child in node.children.reversed() {
                    stack.append((child, depth + 1, inCheckedBranch))
                }
            }
        }
        visibleRows = out
        recomputeSelectionFocus()
    }

    /// Keep the selection branch and focus snapshot in sync. The branch also
    /// drives the spine, so it updates even when focus mode is off.
    private func recomputeSelectionFocus() {
        recomputeSelectionBranch()
        recomputeFocusSnapshot()
    }

    /// A selected parent is its own branch root; a leaf promotes to its
    /// visible parent so focus and the selection spine keep the sibling
    /// branch in context. If the parent is filtered out or hidden, the leaf
    /// stays root. A root outside the current projection yields no branch, so
    /// focus suspends until it is visible again.
    private func recomputeSelectionBranch() {
        guard let selectedId, let selected = nodeIndex[selectedId] else {
            setSelectionBranch(nil)
            return
        }
        let rootId: String
        if !selected.hasChildren,
           let parentId = selected.item.parentId,
           !parentId.isEmpty,
           visibleRows.contains(where: { $0.id == parentId }) {
            rootId = parentId
        } else {
            rootId = selectedId
        }
        guard let start = visibleRows.firstIndex(where: { $0.id == rootId }) else {
            setSelectionBranch(nil)
            return
        }
        let depth = visibleRows[start].depth
        var end = start
        while end + 1 < visibleRows.count, visibleRows[end + 1].depth > depth {
            end += 1
        }
        setSelectionBranch(
            NestedSelectionBranch(rootId: rootId, range: start...end, depth: depth)
        )
    }

    private func setSelectionBranch(_ branch: NestedSelectionBranch?) {
        if selectionBranch != branch {
            selectionBranch = branch
        }
    }

    /// Focus selection is a render-time overlay on `visibleRows`: filters,
    /// Working Mode, zoom, and collapse have already shaped the list. A
    /// branch hidden by the projection is nil, so focus suspends until it is
    /// visible again; no filter state is touched.
    private func recomputeFocusSnapshot() {
        guard focusSelectionEnabled, let branch = selectionBranch else {
            if focusSnapshot != .inactive {
                focusSnapshot = .inactive
            }
            return
        }
        var ancestors: Set<String> = []
        var cursor: String? = nodeIndex[branch.rootId]?.item.parentId
        while let id = cursor, !id.isEmpty, ancestors.insert(id).inserted {
            cursor = nodeIndex[id]?.item.parentId
        }
        let snapshot = NestedFocusSnapshot(range: branch.range, ancestorIds: ancestors)
        if focusSnapshot != snapshot {
            focusSnapshot = snapshot
        }
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
        recomputeVisibleRows()
    }

    private func clearTree() {
        // Focus mode is per open document; switching or closing clears it.
        focusSelectionEnabled = false
        nodeIndex = [:]
        tree = nil
        recomputeVisibleRows()
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

    func startEdit(id: String, text: String? = nil) {
        // Same view-update deferral as the draft starters: editingId and
        // friends must not publish synchronously from event handlers.
        Task { @MainActor [weak self] in
            guard let self else { return }
            self.selectedId = id
            self.editingId = id
            self.draft = nil
            // Prefer the caller's text: the tree can lag a just-saved edit.
            self.editingText = text ?? self.indexById[id]?.item.text ?? ""
        }
    }

    func commitEdit(id: String, text: String) {
        Task { @MainActor [weak self] in
            self?.editingId = nil
            self?.editingText = ""
        }
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        helper.saveItemText(itemId: id, text: trimmed)
    }

    /// In-progress edit text, mirrored synchronously from the field (plain
    /// var: no publish, no re-render, no view-update warning).
    var editingText: String = ""

    /**
     * Synchronous commit from trusted contexts (tap gestures): gesture
     * actions run outside view updates, so direct writes are safe — unlike
     * focus-loss observers, which must hop a tick. Uses the mirrored
     * [editingText] because the field's local copy is unreachable there.
     * Same save semantics as [commitEdit].
     */
    func commitEditing() {
        guard let id = editingId else { return }
        let text = editingText
        editingId = nil
        editingText = ""
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        helper.saveItemText(itemId: id, text: trimmed)
    }

    func cancelEdit() {
        Task { @MainActor [weak self] in
            self?.editingId = nil
            self?.editingText = ""
        }
    }

    // MARK: - Draft (add)

    func startAddRoot() {
        editingId = nil
        draft = NestedDraft(anchorId: "", parentId: "", depth: 0, text: "")
    }

    func startAddChild(of id: String) {
        // Key-press/button handlers can run inside SwiftUI's view-update
        // cycle; publishing draft/editingId synchronously warns ("within
        // view updates"), so hop a tick like the navigation methods do.
        Task { @MainActor [weak self] in
            guard let self, let node = self.indexById[id] else { return }
            self.editingId = nil
            // Anchor after the subtree's last visible descendant (ViewModel parity).
            var anchor = node
            while let last = anchor.children.last, !anchor.item.collapsed {
                anchor = last
            }
            let depth = (self.visibleRows.first(where: { $0.id == id })?.depth ?? 0) + 1
            self.draft = NestedDraft(anchorId: anchor.item.id, parentId: id, depth: depth, text: "")
        }
    }

    func startAddSibling(of id: String) {
        // Same view-update deferral as startAddChild above.
        Task { @MainActor [weak self] in
            guard let self, let node = self.indexById[id] else { return }
            self.editingId = nil
            let depth = self.visibleRows.first(where: { $0.id == id })?.depth ?? 0
            self.draft = NestedDraft(
                anchorId: id,
                parentId: node.item.parentId ?? "",
                depth: depth,
                text: ""
            )
        }
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

    func sortChildrenOfSelected(order: String) {
        guard let id = selectedId, !selectedDocId.isEmpty else { return }
        helper.sortChildren(documentId: selectedDocId, parentId: id, orderName: order)
    }

    func moveDestinationsForSelected() -> MoveDestinations? {
        guard let id = selectedId else { return nil }
        return moveDestinations(for: id)
    }

    func moveDestinations(for id: String) -> MoveDestinations? {
        guard !selectedDocId.isEmpty else { return nil }
        return helper.moveDestinationGroups(documentId: selectedDocId, itemId: id)
    }

    func moveSelectedUnder(destinationId: String) {
        guard let id = selectedId, !selectedDocId.isEmpty else { return }
        helper.moveUnder(documentId: selectedDocId, itemId: id, destinationId: destinationId)
    }

    func toggleCollapse(id: String) { helper.toggleCollapsed(itemId: id) }

    /// Scope roots for expand-all / collapse-all: selection (when it has
    /// children), else the zoom focus (current view), else empty = whole doc.
    private func collapseScopeRoots() -> [String] {
        if let id = selectedId, let node = indexById[id], node.hasChildren {
            return [id]
        }
        if let zid = zoomPath.last, indexById[zid] != nil {
            return [zid]
        }
        return []
    }

    func expandAll() {
        guard !selectedDocId.isEmpty else { return }
        helper.expandSubtrees(documentId: selectedDocId, scopeIds: collapseScopeRoots())
    }

    func collapseAll() {
        guard !selectedDocId.isEmpty else { return }
        helper.collapseSubtrees(documentId: selectedDocId, scopeIds: collapseScopeRoots())
    }

    func toggleCheck(id: String, checked: Bool) { helper.toggleChecked(itemId: id, checked: checked) }

    func confirmDeleteSelected() {
        guard let id = selectedId else { return }
        helper.deleteSingleItem(itemId: id)
        selectedId = nil
        editingId = nil
    }

    /// Direct children of `id` that are checked off. Used by the outline's
    /// "Delete checked children" action; each hit takes its own subtree with it.
    func checkedChildren(of id: String) -> [NestedItemNode] {
        (indexById[id]?.children ?? []).filter { $0.item.checkboxEnabled && $0.item.checked }
    }

    /// Deletes every checked direct child of `id` (subtree included). Clears
    /// the selection/editor when they sat inside one of the removed branches,
    /// so focus isn't stranded on an id that no longer exists.
    func deleteCheckedChildren(of id: String) {
        let ids = checkedChildren(of: id).map { $0.item.id }
        guard !ids.isEmpty else { return }
        helper.deleteItemList(itemIds: ids)
        if let sel = selectedId, ids.contains(where: { isDescendant(itemId: sel, of: $0) }) {
            selectedId = nil
        }
        if let editing = editingId, ids.contains(where: { isDescendant(itemId: editing, of: $0) }) {
            editingId = nil
        }
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

    /// Enables checkboxes on every direct child that does not already have one.
    func enableCheckboxForChildren(id: String) {
        helper.enableCheckboxForChildren(itemId: id)
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
        // Key-press handlers can run inside SwiftUI's view-update cycle;
        // publishing synchronously warns ("within view updates") and is
        // undefined behavior, so hop a tick like the view's onChange
        // observers do.
        Task { @MainActor [weak self] in
            guard let self else { return }
            let rows = self.visibleRows
            guard !rows.isEmpty else { return }
            if let id = self.selectedId, let idx = rows.firstIndex(where: { $0.id == id }) {
                let next = min(max(idx + delta, 0), rows.count - 1)
                self.selectedId = rows[next].id
            } else {
                self.selectedId = rows[delta > 0 ? 0 : rows.count - 1].id
            }
            self.editingId = nil
            if self.draft != nil { self.draft = nil }
        }
    }

    /// Plain Left on a collapsed node or leaf: move selection to its parent.
    func selectParentOfSelected() {
        // Same view-update deferral as moveSelection above.
        Task { @MainActor [weak self] in
            guard let self,
                  let id = self.selectedId,
                  let parentId = self.indexById[id]?.item.parentId,
                  !parentId.isEmpty
            else { return }
            self.selectedId = parentId
            self.editingId = nil
        }
    }

    /// Sibling group of the selection (roots when parentless), in position order.
    private func siblingGroup() -> [NestedItemNode]? {
        guard let id = selectedId, let tree else { return nil }
        if let pid = indexById[id]?.item.parentId, !pid.isEmpty {
            return indexById[pid]?.children
        }
        return tree.rootNodes
    }

    /// Move selection within the sibling group, clamped at the ends.
    func moveToSibling(by delta: Int) {
        // Same view-update deferral as moveSelection above.
        Task { @MainActor [weak self] in
            guard let self,
                  let group = self.siblingGroup(),
                  let id = self.selectedId,
                  let idx = group.firstIndex(where: { $0.item.id == id }),
                  !group.isEmpty
            else { return }
            self.selectedId = group[min(max(idx + delta, 0), group.count - 1)].item.id
            self.editingId = nil
        }
    }

    /// Jump selection to the first/last sibling.
    func moveToSiblingEnd(first: Bool) {
        // Same view-update deferral as moveSelection above.
        Task { @MainActor [weak self] in
            guard let self,
                  let group = self.siblingGroup(),
                  !group.isEmpty
            else { return }
            self.selectedId = (first ? group.first : group.last)!.item.id
            self.editingId = nil
        }
    }

    // MARK: - Markdown export

    func markdown(includeTitle: Bool = false) -> String {
        guard let tree else { return "" }
        return helper.exportTreeToMarkdown(tree: tree, includeTitle: includeTitle)
    }

    func markdownForZoom(includeTitle: Bool = false) -> String {
        guard let tree else { return "" }
        if let zid = zoomPath.last, let node = indexById[zid] {
            return helper.exportNodeToMarkdown(node: node, includeNotes: true)
        }
        return helper.exportTreeToMarkdown(tree: tree, includeTitle: includeTitle)
    }

    func markdown(for itemId: String) -> String {
        guard let node = indexById[itemId] else { return "" }
        return helper.exportNodeToMarkdown(node: node, includeNotes: true)
    }

    func copySelectedAsMarkdown() {
        guard let id = selectedId else { return }
        copySubtreeAsMarkdown(id: id)
    }

    func copyDocumentAsMarkdown(id: String? = nil, includeTitle: Bool = true) {
        let text: String
        if let id = id, id != selectedDocId {
            text = helper.exportDocumentToMarkdown(documentId: id, includeTitle: includeTitle)
        } else {
            text = markdownForZoom(includeTitle: includeTitle)
        }
        guard !text.isEmpty else { return }
        copyToClipboard(text, message: "Copied document as Markdown")
    }

    func copyItemAsMarkdown(id: String) {
        guard let node = indexById[id] else { return }
        let singleNode = NestedItemNode(item: node.item, children: [])
        let text = helper.exportNodeToMarkdown(node: singleNode, includeNotes: true)
        guard !text.isEmpty else { return }
        copyToClipboard(text, message: "Copied item as Markdown")
    }

    func copySubtreeAsMarkdown(id: String) {
        guard let node = indexById[id] else { return }
        let text = helper.exportNodeToMarkdown(node: node, includeNotes: true)
        guard !text.isEmpty else { return }
        let msg = node.hasChildren ? "Copied subtree as Markdown" : "Copied item as Markdown"
        copyToClipboard(text, message: msg)
    }

    // MARK: - JSON Export & Import

    func exportDocumentToJson(document: NestedDocument) {
        let panel = NSSavePanel()
        panel.title = "Export Document as JSON"
        panel.allowedContentTypes = [.json]
        let sanitized = document.title
            .replacingOccurrences(of: "/", with: "-")
            .replacingOccurrences(of: ":", with: "-")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        panel.nameFieldStringValue = "\(sanitized.isEmpty ? "document" : sanitized).json"
        panel.canCreateDirectories = true

        if panel.runModal() == .OK, let url = panel.url {
            helper.exportDocumentToJson(documentId: document.id) { [weak self] json, error in
                guard let self else { return }
                if let json {
                    do {
                        try json.write(to: url, atomically: true, encoding: .utf8)
                        self.showToast("Exported to JSON")
                    } catch {
                        self.showToast("Export failed: \(error.localizedDescription)")
                    }
                } else if let error {
                    self.showToast("Export failed: \(error)")
                }
            }
        }
    }

    func importDocumentFromJson() {
        let panel = NSOpenPanel()
        panel.title = "Import Document from JSON"
        panel.allowedContentTypes = [.json]
        panel.allowsMultipleSelection = false
        panel.canChooseDirectories = false
        panel.canChooseFiles = true

        if panel.runModal() == .OK, let url = panel.url {
            do {
                let json = try String(contentsOf: url, encoding: .utf8)
                helper.importDocumentFromJson(json: json) { [weak self] newDocId, error in
                    guard let self else { return }
                    if let newDocId, !newDocId.isEmpty {
                        self.openDocument(id: newDocId)
                        self.showToast("Imported document")
                    } else if let error {
                        self.showToast("Import failed: \(error)")
                    }
                }
            } catch {
                showToast("Failed to read file: \(error.localizedDescription)")
            }
        }
    }

    func showToast(_ message: String) {
        toastMessage = message
        Task { @MainActor [weak self] in
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            if self?.toastMessage == message {
                self?.toastMessage = nil
            }
        }
    }

    private func copyToClipboard(_ text: String, message: String = "Copied as Markdown") {
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(text, forType: .string)
        showToast(message)
    }
}

