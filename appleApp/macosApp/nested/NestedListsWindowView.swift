//
//  NestedListsWindowView.swift
//  macosApp — full nested-list editor window (sidebar + outline).
//
//  No filter bar (per spec). Keyboard-first: Up/Down navigate, Return edit,
//  Return-in-edit commits, Shift+Return commits and adds a sibling below,
//  Return-in-draft commits and opens the next sibling draft, Tab/Shift+Tab
//  indent/outdent, Cmd+Up/Down move, Cmd+Left/Right outdent/indent,
//  Cmd+Return adds a child, Shift+Return adds a sibling, Delete confirms
//  deletion, Esc cancels. Editor actions also live in the native window
//  toolbar.
//

import SwiftUI
import AppKit
import Shared
import UniformTypeIdentifiers

/// Force-sync direction chosen from the sidebar context menu.
private enum NestedSidebarForceAction {
    case push
    case pull
}

private struct NestedSidebarForceRequest {
    let action: NestedSidebarForceAction
    let document: NestedDocument
}

struct NestedListsWindowView: View {
    @StateObject private var state = NestedEditorState()
    @ObservedObject private var sync = NestedFirestoreSync.shared
    @ObservedObject private var account = QuickNoteGoogleSignIn.shared
    @FocusState private var focusedRow: String?
    @FocusState private var draftFocused: Bool
    @FocusState private var searchFocused: Bool

    @State private var showNewDoc = false
    @State private var newDocTitle = ""
    @State private var renameDoc: NestedDocument? = nil
    @State private var showRename = false
    @State private var renameTitle = ""
    @State private var deleteDoc: NestedDocument? = nil
    @State private var forceRequest: NestedSidebarForceRequest? = nil
    /// Row whose checked children are pending deletion (drives the alert;
    /// nil = no alert) plus the count captured when the alert opened, so the
    /// message survives the action resetting the id.
    @State private var deleteCheckedParentId: String? = nil
    @State private var deleteCheckedCount = 0

    @State private var showMoveSheet = false
    @State private var moveDestinationId: String? = nil
    @State private var showSortMenu = false
    @State private var showCheatsheet = false
    @State private var draftText = ""
    /// Ctrl+N opens the note sheet read-only; reset whenever the sheet closes.
    @State private var noteStartsInPreview = false

    // MARK: - Window sizing

    /// Smallest content size that shows all three panes without clipping:
    /// sidebar (240) + detail (760) + inspector (350) plus the two dividers.
    static let minContentWidth: CGFloat = 1360
    static let minContentHeight: CGFloat = 680
    /// Default content size — wide enough to open with every pane visible.
    static let defaultContentSize = NSSize(width: 1440, height: 820)
    private static let sidebarWidth: CGFloat = 240
    
    var body: some View {
        // Layout notes (this shape is deliberate; a constraint pass used to
        // loop forever and abort the app):
        //  • HSplitView must always keep the same number of children. Hiding
        //    the sidebar by removing it leaves a single-child split view,
        //    which thrashes Auto Layout — so the leading child is stable and
        //    the sidebar collapses to zero width instead.
        //  • The inspector is a plain fixed-width trailing sibling rather
        //    than `.inspector(...)`, whose column-size negotiation also
        //    fought the split view's constraints.
        HStack(spacing: 0) {
            HSplitView {
                sidebarColumn
                detail
                    .frame(minWidth: 760, idealWidth: 760, maxWidth: .infinity, maxHeight: .infinity)
            }
            if state.showInspector {
                Divider()
                NestedInspectorPane(state: state)
                    .frame(width: 350)
            }
        }
        .frame(minWidth: Self.minContentWidth, minHeight: Self.minContentHeight)
        // Pin the outline to the light theme: the bright canvas/cream
        // selection assume dark ink, so every component inside resolves
        // its semantic colors (.primary/.secondary/...) to light values.
        .preferredColorScheme(.light)
        .background(WindowAccessor(onWindow: { win in
            win?.appearance = NSAppearance(named: .aqua)
            Task { @MainActor in state.attachWindow(win) }
        }))
        .onAppear { state.start() }
        .onDisappear {
            state.detachWindow()
            state.stop()
        }
        .sheet(isPresented: $showNewDoc) {
            VStack(alignment: .leading, spacing: 12) {
                Text("New document").font(.headline)
                TextField("Title", text: $newDocTitle)
                    .textFieldStyle(.roundedBorder)
                    .onSubmit { createDoc() }
                HStack {
                    Spacer()
                    Button("Cancel") { showNewDoc = false }.keyboardShortcut(.cancelAction)
                    Button("Create") { createDoc() }
                        .disabled(newDocTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        .keyboardShortcut(.defaultAction)
                }
            }
            .padding()
            .frame(width: 320)
        }
        .sheet(isPresented: $showRename) {
            VStack(alignment: .leading, spacing: 12) {
                Text("Rename document").font(.headline)
                TextField("Title", text: $renameTitle)
                    .textFieldStyle(.roundedBorder)
                HStack {
                    Spacer()
                    Button("Cancel") { showRename = false }.keyboardShortcut(.cancelAction)
                    Button("Save") {
                        if let doc = renameDoc {
                            state.renameDocument(id: doc.id, title: renameTitle)
                        }
                        showRename = false
                    }.keyboardShortcut(.defaultAction)
                }
            }
            .padding()
            .frame(width: 320)
            .onAppear { renameTitle = renameDoc?.title ?? "" }
        }
        .alert(
            "Delete document?",
            isPresented: Binding(
                get: { deleteDoc != nil },
                set: { if !$0 { deleteDoc = nil } }
            )
        ) {
            Button("Delete", role: .destructive) {
                if let doc = deleteDoc { state.deleteDocument(id: doc.id) }
                deleteDoc = nil
            }
            Button("Cancel", role: .cancel) { deleteDoc = nil }
        } message: {
            Text(deleteDoc?.title ?? "")
        }
        // Legacy sheet paths for note/details. The formatting bar and the
        // N/D keys reveal the inspector instead; these stay reachable via
        // ⌥N/⌥D and the row context menu.
        .sheet(isPresented: $state.showNoteSheet) {
            if let item = state.selectedItem {
                NestedNoteSheet(state: state, item: item, startsInPreview: noteStartsInPreview)
            }
        }
        .onChange(of: state.showNoteSheet) { _, showing in
            if !showing { noteStartsInPreview = false }
        }
        .sheet(isPresented: $state.showDetailsSheet) {
            if let item = state.selectedItem {
                NestedDetailsSheet(state: state, item: item)
            }
        }
    }

    // MARK: - Sidebar

    private var docSelection: Binding<String?> {
        Binding(
            get: { state.selectedDocId.isEmpty ? nil : state.selectedDocId },
            set: { if let id = $0 { state.openDocument(id: id) } }
        )
    }

    /// Stable leading child for the `HSplitView`. Wrapped in a concrete
    /// container so the split view always sees exactly two children; when
    /// hidden, the sidebar collapses to zero width instead of disappearing.
    ///
    /// The width is pinned (not `minWidth`/`idealWidth`/`maxWidth`) so the
    /// split view can't re-derive it from the sidebar's content — otherwise
    /// selecting a different document shifts the divider as the fitting width
    /// of the document list / sync row changes.
    private var sidebarColumn: some View {
        VStack(spacing: 0) {
            if state.showSidebar {
                sidebar
            }
        }
        .frame(width: state.showSidebar ? Self.sidebarWidth : 0)
        .frame(maxHeight: .infinity)
    }

    private var sidebar: some View {
        VStack(spacing: 0) {
            List(state.documents, id: \.id, selection: docSelection) { doc in
                Label(doc.title.isEmpty ? "Untitled document" : doc.title, systemImage: "list.bullet")
                    .onDrag { NSItemProvider(object: "nested-doc:\(doc.id)" as NSString) }
                    // Top/bottom halves are separate drop zones (insert above /
                    // below), so no geometry measuring is needed and the list
                    // end stays reachable via the last row's bottom half.
                    .background {
                        VStack(spacing: 0) {
                            Color.clear
                                .contentShape(Rectangle())
                                .onDrop(of: [.text], delegate: NestedDocDropDelegate(state: state, targetId: doc.id, below: false))
                            Color.clear
                                .contentShape(Rectangle())
                                .onDrop(of: [.text], delegate: NestedDocDropDelegate(state: state, targetId: doc.id, below: true))
                        }
                    }
                    .listRowBackground(
                        state.docDropTargetId == doc.id ? Color.accentColor.opacity(0.15) : Color.clear
                    )
                    .overlay(alignment: state.docDropBelow ? .bottom : .top) {
                        if state.docDropTargetId == doc.id {
                            Rectangle()
                                .fill(Color.accentColor)
                                .frame(height: 2)
                        }
                    }
                    .contextMenu {
                        Button {
                            state.copyDocumentAsMarkdown(id: doc.id)
                        } label: {
                            Label("Copy as Markdown", systemImage: "doc.on.doc")
                        }
                        Button {
                            state.exportDocumentToJson(document: doc)
                        } label: {
                            Label("Export to JSON…", systemImage: "square.and.arrow.up")
                        }
                        Divider()
                        Button {
                            forceRequest = NestedSidebarForceRequest(action: .push, document: doc)
                        } label: {
                            Label("Force Push to Cloud", systemImage: "icloud.and.arrow.up")
                        }
                        .disabled(sync.uiState.status == .syncing)
                        Button {
                            forceRequest = NestedSidebarForceRequest(action: .pull, document: doc)
                        } label: {
                            Label("Force Pull from Cloud", systemImage: "icloud.and.arrow.down")
                        }
                        .disabled(sync.uiState.status == .syncing)
                        Divider()
                        Button("Rename") { renameDoc = doc; showRename = true }
                        Button("Delete", role: .destructive) { deleteDoc = doc }
                    }
            }
            .listStyle(.sidebar)
            .alert(
                forceRequest?.action == .pull ? "Force pull from cloud?" : "Force push to cloud?",
                isPresented: Binding(
                    get: { forceRequest != nil },
                    set: { if !$0 { forceRequest = nil } }
                )
            ) {
                if let request = forceRequest {
                    Button(request.action == .pull ? "Force Pull" : "Force Push", role: .destructive) {
                        forceRequest = nil
                        // Commit any open inline edit first: a force pull can
                        // remove the row being edited.
                        state.commitEditing()
                        switch request.action {
                        case .push:
                            NestedFirestoreSync.shared.forcePush(documentId: request.document.id)
                        case .pull:
                            NestedFirestoreSync.shared.forcePull(documentId: request.document.id)
                        }
                    }
                }
                Button("Cancel", role: .cancel) { forceRequest = nil }
            } message: {
                if let request = forceRequest {
                    Text(forceMessage(for: request))
                }
            }
            Divider()
            Divider()
            Button {
                newDocTitle = ""
                showNewDoc = true
            } label: {
                Label("New document", systemImage: "plus")
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 8)
            .padding(.top, 8)

            Button {
                state.importDocumentFromJson()
            } label: {
                Label("Import from JSON…", systemImage: "square.and.arrow.down")
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
            Divider()
            nestedDocsSyncRow
                .padding(12)
        }
    }

    /// Sidebar document reorder: each row's top/bottom half is a drop zone
    /// (insert above / below the row). Hover highlights the row with an
    /// insertion edge, so the landing spot is always visible.
    struct NestedDocDropDelegate: DropDelegate {
        let state: NestedEditorState
        let targetId: String
        let below: Bool

        func validateDrop(info: DropInfo) -> Bool {
            info.hasItemsConforming(to: [.text])
        }

        func dropUpdated(info: DropInfo) -> DropProposal? {
            DropProposal(operation: .move)
        }

        func dropEntered(info: DropInfo) {
            state.showDocDropTarget(id: targetId, below: below)
        }

        func dropExited(info: DropInfo) {
            if state.docDropTargetId == targetId {
                state.clearDocDropTarget()
            }
        }

        func performDrop(info: DropInfo) -> Bool {
            // Clear first on every path: foreign payloads and torn-down
            // sessions otherwise strand the highlight.
            state.clearDocDropTarget()
            guard let provider = info.itemProviders(for: [.text]).first else { return false }
            _ = provider.loadObject(ofClass: NSString.self) { payload, _ in
                guard let s = payload as? String, s.hasPrefix("nested-doc:") else { return }
                let dragged = String(s.dropFirst("nested-doc:".count))
                DispatchQueue.main.async {
                    state.moveDocument(draggedId: dragged, onto: targetId, below: below)
                }
            }
            return true
        }
    }

    private func createDoc() {
        let t = newDocTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty else { return }
        state.createDocument(title: t)
        showNewDoc = false
    }

    /// Confirmation copy for the sidebar force-sync actions. Both directions
    /// are destructive on the side that loses.
    private func forceMessage(for request: NestedSidebarForceRequest) -> String {
        let title = request.document.title.isEmpty ? "Untitled document" : request.document.title
        switch request.action {
        case .push:
            return "Replace the cloud copy of “\(title)” with this Mac's version? Changes made on other devices will be overwritten."
        case .pull:
            return "Delete the copy of “\(title)” on this Mac and download the cloud version? Local changes that have not been synced will be lost."
        }
    }

    /// Manual sync row for the document list (no auto-sync).
    /// List sync reports `documentId == nil`, so open-document status
    /// never leaks into this row.
    @ViewBuilder
    private var nestedDocsSyncRow: some View {
        let isListSync = sync.uiState.documentId == nil
        let syncing = isListSync && sync.uiState.status == .syncing
        HStack(spacing: 6) {
            Button {
                NestedFirestoreSync.shared.syncDocumentsNow()
            } label: {
                Label("Sync", systemImage: "arrow.clockwise")
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
            .help("Sync the document list")
            .disabled(syncing)
            Spacer()
            VStack(alignment: .trailing, spacing: 0) {
                Group {
                    switch sync.uiState.status {
                    case .syncing where isListSync:
                        Text("Syncing…")
                    case .synced where isListSync:
                        if let at = sync.uiState.lastSyncedAt {
                            Text("Synced \(at.formatted(date: .omitted, time: .shortened))")
                        } else {
                            Text("Synced")
                        }
                    case .offline where isListSync:
                        Text("Offline")
                    case .error where isListSync:
                        Text(sync.uiState.message ?? "Failed")
                    default:
                        EmptyView()
                    }
                }
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                if account.isAnonymous {
                    Text("Sign in Settings to share between devices")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                        .lineLimit(1)
                } else if let email = account.email {
                    Text(email)
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
        }
    }

    // MARK: - Detail

    /// Confirmation copy for the "Delete checked children" alert. Uses the
    /// count captured when the alert opened, so the message doesn't blank out
    /// as the action clears the pending row id.
    private var deleteCheckedMessage: String {
        let items = deleteCheckedCount == 1 ? "1 checked item" : "\(deleteCheckedCount) checked items"
        return "This permanently deletes \(items) and everything nested under them."
    }

    @ViewBuilder
    private var detail: some View {
        if state.selectedDocId.isEmpty {
            VStack(spacing: 8) {
                Text("Select a document").font(.headline)
                Text("Pick one from the sidebar or create a new document.")
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if state.tree == nil {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            VStack(spacing: 0) {
                detailHeader
                outlineList
                Divider()
                bottomBar
            }
            .alert(
                "Delete checked children?",
                isPresented: Binding(
                    get: { deleteCheckedParentId != nil },
                    set: { if !$0 { deleteCheckedParentId = nil } }
                )
            ) {
                Button("Delete", role: .destructive) {
                    if let id = deleteCheckedParentId { state.deleteCheckedChildren(of: id) }
                    deleteCheckedParentId = nil
                }
                Button("Cancel", role: .cancel) { deleteCheckedParentId = nil }
            } message: {
                Text(deleteCheckedMessage)
            }
            .overlay(alignment: .bottom) {
                if let toast = state.toastMessage {
                    HStack(spacing: 6) {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(.green)
                        Text(toast)
                            .font(.subheadline).bold()
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(.regularMaterial, in: Capsule())
                    .shadow(color: .black.opacity(0.15), radius: 8, y: 3)
                    .padding(.bottom, 48)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.spring(response: 0.3, dampingFraction: 0.8), value: state.toastMessage)
        }
    }

    private var detailHeader: some View {
        VStack(spacing: 0) {
            breadcrumbBar
            Divider()
            editorBar
            Divider()
        }
        .background(Color(nsColor: .windowBackgroundColor))
    }

    private var breadcrumbBar: some View {
        HStack(spacing: 4) {
            paneToggle(
                system: "sidebar.left",
                label: state.showSidebar ? "Hide sidebar" : "Show sidebar",
                help: "Toggle sidebar (⌘⌃S)",
                tint: state.showSidebar ? Color.accentColor : .secondary 
            ) { state.toggleSidebar() }
            barSeparator
            Button("Root") { state.zoomToRoot() }
                .buttonStyle(.link)
                .fontWeight(state.zoomPath.isEmpty ? .bold : .regular)
            if let zid = state.zoomPath.last, let crumbs = state.chain(to: zid) {
                ForEach(Array(crumbs.enumerated()), id: \.element.item.id) { _, node in
                    Image(systemName: "chevron.right").font(.caption).foregroundStyle(.secondary)
                    let isCurrent = node.item.id == zid
                    Button(node.item.text.isEmpty ? "Untitled" : String(node.item.text.prefix(24))) {
                        state.zoomTo(id: node.item.id)
                    }
                    .buttonStyle(.link)
                    .fontWeight(isCurrent ? .bold : .regular)
                    .disabled(isCurrent)
                }
            }
            Spacer()
            nestedSyncStatus
            barSeparator
            paneToggle(
                system: "sidebar.right",
                label: state.showInspector ? "Hide inspector" : "Show inspector",
                help: "Toggle inspector (⌘⌥I)",
                tint: state.showInspector ? Color.accentColor : .secondary
            ) { state.toggleInspector() }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
    }

    /// Compact icon button used for the sidebar/inspector pane toggles.
    private func paneToggle(
        system: String,
        label: String,
        help: String,
        tint: Color,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: system)
                .imageScale(.medium)
                .foregroundStyle(tint)
                .frame(width: 22, height: 22)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .help(help)
    }

    // MARK: - Editor bar

    /// Manual sync button + status for the open document (no auto-sync).
    @ViewBuilder
    private var nestedSyncStatus: some View {
        let docId = state.selectedDocId
        let isSyncing = sync.uiState.status == .syncing && sync.uiState.documentId == docId
        Group {
            switch sync.uiState.status {
            case .syncing where sync.uiState.documentId == docId:
                Text(sync.uiState.operation == .forcePush ? "Force pushing…"
                     : sync.uiState.operation == .forcePull ? "Force pulling…"
                     : "Syncing…")
                    .font(.caption).foregroundStyle(.secondary)
            case .synced where sync.uiState.documentId == docId:
                if let message = sync.uiState.message {
                    Text(message).font(.caption).foregroundStyle(.secondary)
                } else if let at = sync.uiState.lastSyncedAt {
                    Text("Synced \(at.formatted(date: .omitted, time: .shortened))")
                        .font(.caption).foregroundStyle(.secondary)
                } else {
                    Text("Synced").font(.caption).foregroundStyle(.secondary)
                }
            case .offline where sync.uiState.documentId == docId:
                Text("Offline — saved on this device").font(.caption).foregroundStyle(.secondary)
            case .error where sync.uiState.documentId == docId:
                Text(sync.uiState.message ?? "Sync failed.")
                    .font(.caption).foregroundStyle(.secondary)
            default:
                EmptyView()
            }
            Button {
                NestedFirestoreSync.shared.syncNow(documentId: docId)
            } label: {
                Image(systemName: "arrow.clockwise")
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
            .help("Sync this document")
            .disabled(docId.isEmpty || isSyncing)
        }
    }

    private var editorBar: some View {
        let sel = state.selectedId
        let node = sel.flatMap { state.indexById[$0] }
        let hasCollapsible = state.indexById.values.contains(where: { $0.hasChildren })
        // Plain centered row (no ScrollView): the ~12 buttons fit the 760pt
        // detail minimum, and a scroll view would pin content left.
        return HStack(spacing: 6) {
                HStack(spacing: 4) {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(.secondary)
                        .imageScale(.small)
                    TextField("Search items", text: $state.searchQuery)
                        .textFieldStyle(.plain)
                        .frame(width: 130)
                        .focused($searchFocused)
                        .onKeyPress(.escape) {
                            // Resign first; claiming row focus must hop a tick
                            // or the two writes race and focus lands nowhere
                            // (dead keyboard), same pattern as the row handoffs.
                            searchFocused = false
                            DispatchQueue.main.async {
                                if let id = state.selectedId {
                                    focusedRow = id
                                }
                            }
                            return .handled
                        }
                    if !state.searchQuery.isEmpty {
                        Button {
                            state.searchQuery = ""
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .imageScale(.small)
                                .foregroundStyle(.secondary)
                        }
                        .buttonStyle(.plain)
                        .help("Clear search")
                    }
                }
                .padding(.horizontal, 7)
                .padding(.vertical, 4)
                .background(RoundedRectangle(cornerRadius: 7).fill(Color.secondary.opacity(0.12)))
                .accessibilityLabel("Search items")
                .help("Search items (⌘F)")
                barSeparator
                editorBtn("Zoom In", system: "plus.magnifyingglass", help: "Zoom in (⌘=)") {
                    state.zoomInSelected()
                }
                .disabled(!(node?.hasChildren ?? false))
                editorBtn("Zoom Out", system: "minus.magnifyingglass", help: "Zoom out (⌘-)") {
                    state.zoomOut()
                }
                .disabled(state.zoomPath.isEmpty)
                editorBtn(
                    "Focus Selection",
                    system: "scope",
                    help: "Focus the selected branch (dim the rest)",
                    active: state.focusSelectionEnabled
                ) {
                    state.focusSelectionEnabled.toggle()
                }
                .disabled(sel == nil && !state.focusSelectionEnabled)
                .accessibilityValue(state.focusSelectionEnabled ? "On" : "Off")
                editorBtn("Expand All", system: "chevron.down.2", help: "Expand all (ShiftCtrl→)") {
                    state.expandAll()
                }
                .disabled(!hasCollapsible)
                editorBtn("Collapse All", system: "chevron.up.2", help: "Collapse all (ShiftCtrl←)") {
                    state.collapseAll()
                }
                .disabled(!hasCollapsible)
                barSeparator
                editorBtn("Outdent", system: "arrow.left.to.line", help: "Outdent (Shift+Tab)") {
                    state.outdentSelected()
                }
                .disabled(sel == nil)
                editorBtn("Indent", system: "arrow.right.to.line", help: "Indent (Tab)") {
                    state.indentSelected()
                }
                .disabled(sel == nil)
                editorBtn("Up", system: "chevron.up", help: "Move up (Cmd+Up)", shortcut: "⌘↑") {
                    state.moveSelectedUp()
                }
                .disabled(sel == nil)
                editorBtn("Down", system: "chevron.down", help: "Move down (Cmd+Down)", shortcut: "⌘↓") {
                    state.moveSelectedDown()
                }
                .disabled(sel == nil)
                barSeparator
                editorBtn("Child", system: "plus", help: "Add child (Cmd+Return)", shortcut: "⌘↵") {
                    if let id = sel { state.startAddChild(of: id) }
                }
                .disabled(sel == nil)
                editorBtn("Sibling", system: "text.badge.plus", help: "Add sibling below (Shift+Return)", shortcut: "⇧↵") {
                    if let id = sel { state.startAddSibling(of: id) }
                }
                .disabled(sel == nil)
                editorBtn("Root", system: "plus.circle", help: "Add root item") {
                    state.startAddRoot()
                }
                editorBtn("Delete", system: "trash", help: "Delete (Del)") {
                    if sel != nil { state.showDeleteConfirm = true }
                }
                barSeparator
                editorBtn("Copy Markdown", system: "doc.on.doc", help: "Copy selected as Markdown (⌘C)") {
                    state.copySelectedAsMarkdown()
                }
                .disabled(sel == nil)
                barSeparator
                Menu {
                    Toggle(isOn: Binding(
                        get: { state.displayType == .all },
                        set: { if $0 { state.displayType = .all } }
                    )) {
                        Label("All", systemImage: "list.bullet")
                    }
                    Toggle(isOn: Binding(
                        get: { state.displayType == .working },
                        set: { if $0 { state.displayType = .working } }
                    )) {
                        Label("Working", systemImage: "briefcase")
                    }
                } label: {
                    Image(systemName: state.displayType == .all ? "list.bullet" : "briefcase")
                        .imageScale(.medium)
                        .tint(state.displayType == .working ? Color.accentColor : Color.secondary)
                        .frame(width: 28, height: 28)
                }
                .menuStyle(.borderlessButton)
                .accessibilityLabel("Display")
                .help(state.displayType == .all ? "Show all items" : "Show working items (priority, due date, open tasks, progress)")
                barSeparator
                noteLinesMenu
                barSeparator
                Button {
                    showSortMenu = true
                } label: {
                    Image(systemName: "arrow.up.arrow.down")
                        .imageScale(.medium)
                        .frame(width: 28, height: 28)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel("Sort children")
                .help("Sort children of selected item (S)")
                .disabled(!(node?.hasChildren ?? false))
                .popover(isPresented: $showSortMenu, arrowEdge: .bottom) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Sort children").font(.headline)
                        ForEach(sortOrders, id: \.name) { order in
                            Button(order.label) {
                                state.sortChildrenOfSelected(order: order.name)
                                showSortMenu = false
                            }
                            .buttonStyle(.plain)
                            .padding(.vertical, 3)
                        }
                    }
                    .padding(12)
                    .frame(width: 220)
                }
                barSeparator
                Button {
                    moveDestinationId = defaultMoveDestinationId()
                    showMoveSheet = true
                } label: {
                    Image(systemName: "tray.and.arrow.down")
                        .imageScale(.medium)
                        .frame(width: 28, height: 28)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel("Move to")
                .help("Move selected item under a sibling or uncle (M)")
                .disabled(!hasMoveDestinations)
            }
            .frame(maxWidth: .infinity, alignment: .center)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
    }

    private func editorBtn(
        _ title: String,
        system: String,
        help: String,
        shortcut: String? = nil,
        active: Bool = false,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            EditorBarButtonLabel(system: system, shortcut: shortcut, active: active)
        }
        // Match macOS toolbar conventions: template icons, a predictable
        // hit target, and text exposed through help and accessibility rather
        // than permanently consuming horizontal space.
        .buttonStyle(.borderless)
        .accessibilityLabel(title)
        .help(help)
    }

    private var barSeparator: some View {
        Divider().frame(height: 18)
    }

    /// Row note-preview line counts offered in the editor bar; 0 = unlimited.
    private let noteLineOptions = [1, 2, 3, 4, 5, 6, 8, 10, 15, 20, 0]

    /// Editor-bar control for how many lines of a row's note preview are
    /// drawn (`NestedRowView` applies it as `lineLimit`). Persisted on state.
    private var noteLinesMenu: some View {
        Menu {
            ForEach(noteLineOptions, id: \.self) { n in
                Toggle(isOn: Binding(
                    get: { state.noteLineLimit == n },
                    set: { if $0 { state.noteLineLimit = n } }
                )) {
                    Text(n == 0 ? "Unlimited" : "\(n) line\(n == 1 ? "" : "s")")
                }
            }
        } label: {
            HStack(spacing: 3) {
                Image(systemName: "text.alignleft")
                    .imageScale(.medium)
                Text(state.noteLineLimit == 0 ? "∞" : "\(state.noteLineLimit)")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .frame(minWidth: 28, minHeight: 28)
        }
        .menuStyle(.borderlessButton)
        .fixedSize()
        .accessibilityLabel("Note preview lines")
        .help("Lines of each row's note preview")
    }

    // MARK: - Outline list + keyboard

    private var sortOrders: [(name: String, label: String)] {
        [
            ("NameAsc", "Name (A–Z)"),
            ("AddedDesc", "Newest first"),
            ("CompletedDesc", "Recently completed"),
            ("UncheckedFirst", "Unchecked first"),
            ("PriorityDesc", "Priority (high first)"),
        ]
    }

    private var hasSearchText: Bool {
        !state.searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var emptyTitle: String {
        if hasSearchText { return "No matches" }
        return state.displayType == .working ? "No working items" : "Nothing here yet"
    }

    private var emptySubtitle: String {
        if hasSearchText { return "No items match the current search." }
        return state.displayType == .working
            ? "Nothing needs attention — no priority, due dates, open tasks, or tracked progress."
            : "Start with a root item, then indent items to build your outline."
    }

    private var outlineList: some View {
        let rows = state.visibleRows
        return ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    if state.draft?.anchorId.isEmpty == true {
                        draftRow(depth: 0)
                    }
                    if rows.isEmpty, state.draft == nil {
                        VStack(spacing: 8) {
                            Text(emptyTitle).font(.headline)
                            Text(emptySubtitle)
                                .foregroundStyle(.secondary)
                            Button("Add item") { state.startAddRoot() }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 48)
                    }
                    ForEach(rows) { row in
                        VStack(alignment: .leading, spacing: 0) {
                            if state.dropTarget?.displayRowId == row.id && !state.dropTarget!.below {
                                DropIndicatorLine(depth: state.dropTarget!.depth)
                                    .transition(.opacity)
                            }
                            NestedRowView(
                                state: state,
                                row: row,
                                isEditing: state.editingId == row.id,
                                focusTier: state.focusSnapshot.tier(for: row),
                                selectionBranch: state.selectionBranch
                            )
                            if state.dropTarget?.displayRowId == row.id && state.dropTarget!.below {
                                DropIndicatorLine(depth: state.dropTarget!.depth)
                                    .transition(.opacity)
                            }
                            if state.draft?.anchorId == row.id, let d = state.draft {
                                draftRow(depth: d.depth)
                            }
                        }
                        // No vertical padding: the indent guides must run
                        // unbroken across rows. Spacing lives inside the row.
                        // focusEffectDisabled hides the blue focus rectangle;
                        // selection is already shown by the cream background.
                        .focusable()
                        .focused($focusedRow, equals: row.id)
                        .focusEffectDisabled()
                        .id(row.id)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            // Commit any open edit first (tap-away saves);
                            // select() alone would strand it unsaved.
                            if state.editingId != nil && state.editingId != row.id {
                                state.commitEditing()
                            }
                            state.select(id: row.id)
                            focusedRow = row.id
                        }
                        .contextMenu {
                            Button {
                                state.startEdit(id: row.id)
                            } label: {
                                Label("Edit", systemImage: "pencil")
                            }
                            Button {
                                state.startAddChild(of: row.id)
                            } label: {
                                Label("Add Child", systemImage: "plus")
                            }
                            Button {
                                state.startAddSibling(of: row.id)
                            } label: {
                                Label("Add Sibling Below", systemImage: "text.badge.plus")
                            }
                            Button {
                                state.selectedId = row.id
                                moveDestinationId = defaultMoveDestinationId(for: row.id)
                                showMoveSheet = true
                            } label: {
                                Label("Move to…", systemImage: "tray.and.arrow.down")
                            }
                            Divider()
                            if row.node.hasChildren {
                                Button {
                                    state.toggleCollapse(id: row.id)
                                } label: {
                                    Label(row.node.item.collapsed ? "Expand" : "Collapse", systemImage: row.node.item.collapsed ? "chevron.right" : "chevron.down")
                                }
                                Button {
                                    state.zoomTo(id: row.id)
                                } label: {
                                    Label("Zoom In", systemImage: "plus.magnifyingglass")
                                }
                                Divider()
                                Button(role: .destructive) {
                                    // Commit first: deleting a branch that is
                                    // mid-edit would strand the inline editor.
                                    if let editing = state.editingId,
                                       state.isDescendant(itemId: editing, of: row.id) {
                                        state.commitEditing()
                                    }
                                    deleteCheckedCount = state.checkedChildren(of: row.id).count
                                    deleteCheckedParentId = row.id
                                } label: {
                                    Label("Delete Checked Children…", systemImage: "trash.slash")
                                }
                                .disabled(state.checkedChildren(of: row.id).isEmpty)
                            }
                            Button {
                                state.toggleCheck(id: row.id, checked: !row.node.item.checked)
                            } label: {
                                Label(row.node.item.checked ? "Mark as Incomplete" : "Mark as Complete", systemImage: row.node.item.checked ? "circle" : "checkmark.circle")
                            }
                            Divider()
                            Button {
                                state.copyItemAsMarkdown(id: row.id)
                            } label: {
                                Label("Copy Item as Markdown", systemImage: "doc.on.doc")
                            }
                            if row.node.hasChildren {
                                Button {
                                    state.copySubtreeAsMarkdown(id: row.id)
                                } label: {
                                    Label("Copy Subtree as Markdown", systemImage: "list.bullet.indent")
                                }
                            }
                            Divider()
                            Button {
                                state.selectedId = row.id
                                noteStartsInPreview = false
                                state.showNoteSheet = true
                            } label: {
                                Label("Note", systemImage: "note.text")
                            }
                            Button {
                                state.selectedId = row.id
                                state.showDetailsSheet = true
                            } label: {
                                Label("Item details", systemImage: "info.circle")
                            }
                            Divider()
                            Button(role: .destructive) {
                                // Commit any open edit first (see row tap);
                                // select() alone would strand it unsaved.
                                if state.editingId != nil && state.editingId != row.id {
                                    state.commitEditing()
                                }
                                state.select(id: row.id)
                                state.showDeleteConfirm = true
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                    if !rows.isEmpty {
                        Color.clear
                            .frame(height: 60)
                            .contentShape(Rectangle())
                            .onDrop(
                                of: [.text],
                                delegate: OutlineBottomDropDelegate(state: state)
                            )
                    }
                }
                // Cap the working column and center it: fullscreen would
                // otherwise stretch rows across the whole window.
                .padding(.horizontal, 8).padding(.vertical, 10)
                .frame(maxWidth: 840)
                .frame(maxWidth: .infinity)
            }
            .background(nestedCanvasColor)
            .scrollEdgeEffectHidden(true, for: .all)
            .onChange(of: state.selectedId) { _, id in
                // Deferred: publishing focus/scroll state synchronously here
                // warns ("within view updates") and is undefined behavior.
                // Don't steal focus while a TextField owns it (draft/edit):
                // only one view can hold focus, last writer wins and typing
                // or arrow keys end up in the wrong place.
                DispatchQueue.main.async {
                    if state.draft == nil && state.editingId == nil {
                        focusedRow = id
                    }
                    // Minimal scroll: only moves if the row is out of view.
                    if let id { withAnimation { proxy.scrollTo(id) } }
                }
            }
            .onChange(of: state.draft?.anchorId) { _, _ in
                DispatchQueue.main.async {
                    draftText = state.draft?.text ?? ""
                    draftFocused = state.draft != nil
                    // Focus is lost when the draft field disappears; hand it
                    // back to the selected row so Up/Down keep working.
                    if state.draft == nil, state.editingId == nil, let id = state.selectedId {
                        focusedRow = id
                    }
                }
            }
            .onChange(of: state.editingId) { _, editing in
                // Same focus handoff as draft: after commit/cancel the edit
                // field is gone and selectedId often didn't change, so the
                // selectedId observer above won't fire to restore focus.
                if editing == nil, state.draft == nil, let id = state.selectedId {
                    DispatchQueue.main.async { focusedRow = id }
                }
            }
            .onKeyPress(keys: [.upArrow, .downArrow]) { press in
                // The edit/draft/search fields own their arrows (commit-and-stay);
                // without this guard the outline would also move selection.
                guard state.editingId == nil, state.draft == nil else { return .ignored }
                if press.modifiers.contains(.command) {
                    if press.key == .upArrow { state.moveSelectedUp() } else { state.moveSelectedDown() }
                } else {
                    state.moveSelection(by: press.key == .upArrow ? -1 : 1)
                }
                return .handled
            }
            // Note: nav-mode Tab/Shift+Tab is owned by the NSEvent monitor
            // (see NestedEditorState.attachWindow) because AppKit focus
            // navigation can swallow Shift+Tab before SwiftUI sees it.
            .onKeyPress(keys: [.return]) { press in
                if press.modifiers.contains(.command) {
                    guard state.editingId == nil, state.draft == nil,
                          let id = state.selectedId
                    else { return .ignored }
                    state.startAddChild(of: id)
                    return .handled
                }
                if press.modifiers.contains(.shift) {
                    guard state.editingId == nil, state.draft == nil,
                          let id = state.selectedId
                    else { return .ignored }
                    state.startAddSibling(of: id)
                    return .handled
                }
                guard state.editingId == nil, state.draft == nil,
                      let id = state.selectedId
                else { return .ignored }
                state.startEdit(id: id)
                return .handled
            }
            .onKeyPress(keys: [.leftArrow, .rightArrow]) { press in
                self.handleOutlineArrowKey(press)
            }
            .onKeyPress(keys: [.delete, .deleteForward]) { _ in
                guard state.editingId == nil, state.selectedId != nil else { return .ignored }
                state.showDeleteConfirm = true
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("c"), KeyEquivalent("C")]) { press in
                guard state.editingId == nil, state.draft == nil, state.selectedId != nil else { return .ignored }
                if press.modifiers.contains(.command) {
                    state.copySelectedAsMarkdown()
                    return .handled
                }
                return .ignored
            }
            .onKeyPress(keys: [KeyEquivalent("n"), KeyEquivalent("N")]) { press in
                guard state.editingId == nil, state.draft == nil, state.selectedId != nil else { return .ignored }
                // ⌘N stays free for the standard New command; ⌃N opens the
                // sheet straight into the read-only preview.
                guard !press.modifiers.contains(.command) else { return .ignored }
                noteStartsInPreview = press.modifiers.contains(.control)
                state.showNoteSheet = true
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("d"), KeyEquivalent("D")]) { _ in
                guard state.editingId == nil, state.draft == nil, state.selectedId != nil else { return .ignored }
                state.showDetailsSheet = true
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("j"), KeyEquivalent("J")]) { _ in
                guard state.editingId == nil, state.draft == nil, state.selectedId != nil else { return .ignored }
                state.moveToSibling(by: 1)
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("k"), KeyEquivalent("K")]) { _ in
                guard state.editingId == nil, state.draft == nil, state.selectedId != nil else { return .ignored }
                state.moveToSibling(by: -1)
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("g")]) { _ in
                guard state.editingId == nil, state.draft == nil, state.selectedId != nil else { return .ignored }
                state.moveToSiblingEnd(first: true)
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("G")]) { _ in
                guard state.editingId == nil, state.draft == nil, state.selectedId != nil else { return .ignored }
                state.moveToSiblingEnd(first: false)
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("p"), KeyEquivalent("P")]) { press in
                guard !press.modifiers.contains(.command),
                      state.editingId == nil, state.draft == nil, state.selectedId != nil else { return .ignored }
                state.selectParentOfSelected()
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("m"), KeyEquivalent("M")]) { _ in
                guard state.editingId == nil, state.draft == nil, state.selectedId != nil,
                      hasMoveDestinations else { return .ignored }
                moveDestinationId = defaultMoveDestinationId()
                showMoveSheet = true
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("s"), KeyEquivalent("S")]) { press in
                if press.modifiers.contains(.command), press.modifiers.contains(.control) {
                    state.toggleSidebar()
                    return .handled
                }
                guard state.editingId == nil, state.draft == nil,
                      let sel = state.selectedId,
                      state.indexById[sel]?.hasChildren == true else { return .ignored }
                showSortMenu = true
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("="), KeyEquivalent("+")]) { press in
                guard press.modifiers.contains(.command),
                      state.editingId == nil, state.draft == nil,
                      let sel = state.selectedId,
                      state.indexById[sel]?.hasChildren == true else { return .ignored }
                state.zoomInSelected()
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("-"), KeyEquivalent("_")]) { press in
                guard press.modifiers.contains(.command),
                      state.editingId == nil, state.draft == nil,
                      !state.zoomPath.isEmpty else { return .ignored }
                state.zoomOut()
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("?")]) { _ in
                guard state.editingId == nil, state.draft == nil else { return .ignored }
                showCheatsheet = true
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("f")]) { press in
                guard press.modifiers.contains(.command) else { return .ignored }
                searchFocused = true
                return .handled
            }
            .onKeyPress(keys: [KeyEquivalent("i"), KeyEquivalent("I")]) { press in
                guard press.modifiers.contains(.command), press.modifiers.contains(.option) else { return .ignored }
                state.toggleInspector()
                return .handled
            }
            .onKeyPress(.escape) {
                if state.editingId != nil { state.cancelEdit(); return .handled }
                if state.draft != nil { state.cancelDraft(); return .handled }
                return .ignored
            }
            .alert("Delete item?", isPresented: $state.showDeleteConfirm) {
                Button("Delete", role: .destructive) { state.confirmDeleteSelected() }
                Button("Cancel", role: .cancel) {}
            }
            .sheet(isPresented: $showMoveSheet) {
                moveToSheet
            }
            .sheet(isPresented: $showCheatsheet) {
                cheatsheetView
            }
        }
    }

    private var hasMoveDestinations: Bool {
        guard let groups = state.moveDestinationsForSelected() else { return false }
        return !groups.isEmpty
    }

    private func defaultMoveDestinationId(for id: String? = nil) -> String? {
        let target = id ?? state.selectedId
        guard let target, let groups = state.moveDestinations(for: target) else { return nil }
        return groups.siblings.first?.id ?? groups.parentSiblings.first?.id
    }

    private func cheatKey(_ text: String) -> some View {
        Text(text)
            .font(.caption.monospaced())
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Color.secondary.opacity(0.15), in: RoundedRectangle(cornerRadius: 5))
    }

    private func cheatRow(keys: [String], label: String) -> some View {
        HStack {
            HStack(spacing: 2) {
                ForEach(keys, id: \.self) { cheatKey($0) }
            }
            .frame(width: 110, alignment: .trailing)
            Text(label)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func cheatSection(_ title: String, rows: [(keys: [String], label: String)]) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title.uppercased())
                .font(.caption).bold()
                .foregroundStyle(Color.accentColor)
            ForEach(rows, id: \.label) { row in
                cheatRow(keys: row.keys, label: row.label)
            }
        }
    }

    private var cheatsheetView: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Keyboard shortcuts").font(.headline)
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    cheatSection("Navigate", rows: [
                        (["↑", "↓"], "Previous / next item"),
                        (["J", "K"], "Next / previous sibling"),
                        (["g", "G"], "First / last sibling"),
                        (["P"], "Parent"),
                        (["←"], "Collapse / go to parent"),
                        (["→"], "Expand"),
                    ])
                    cheatSection("Edit", rows: [
                        (["Return"], "Edit selected item"),
                        (["Esc"], "Cancel edit / close draft"),
                        (["Tab", "⇧Tab"], "Indent / outdent"),
                    ])
                    cheatSection("Structure", rows: [
                        (["⌘↵"], "Add child"),
                        (["⇧↵"], "Add sibling below"),
                        (["⌘↑", "⌘↓"], "Move item up / down"),
                        (["⌘←", "⌘→"], "Outdent / indent"),
                        (["⌘⌥←", "⌘⌥→"], "Collapse all / expand all"),
                        (["M"], "Move to…"),
                        (["S"], "Sort children"),
                        (["Del"], "Delete"),
                    ])
                    cheatSection("View & actions", rows: [
                        (["⌘F"], "Search"),
                        (["Esc"], "Blur search"),
                        (["N"], "Note"),
                        (["⌃N"], "Note preview"),
                        (["D"], "Item details"),
                        (["⌘⇧P"], "Preview note (note editor)"),
                        (["⌘⌥I"], "Toggle inspector"),
                        (["⌘⌃S"], "Toggle sidebar"),
                        (["⌘=", "⌘-"], "Zoom in / out"),
                        (["⌘C"], "Copy as Markdown"),
                        (["?"], "This cheat sheet"),
                    ])
                }
            }
            HStack {
                Spacer()
                Button("Close") { showCheatsheet = false }.keyboardShortcut(.cancelAction)
            }
        }
        .padding()
        .frame(width: 380)
    }

    private var moveToSheet: some View {
        let groups = state.moveDestinationsForSelected()
        let title = state.selectedId.flatMap { state.indexById[$0]?.item.text } ?? ""
        return VStack(alignment: .leading, spacing: 12) {
            Text("Move to").font(.headline)
            Text(title.isEmpty ? "Untitled item" : title)
                .font(.caption).foregroundStyle(.secondary).lineLimit(1)
            ScrollView {
                VStack(alignment: .leading, spacing: 2) {
                    moveDestinationGroup(title: "Siblings", items: groups?.siblings ?? [])
                    moveDestinationGroup(title: "Parent's siblings", items: groups?.parentSiblings ?? [])
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(maxHeight: 320)
            HStack {
                Spacer()
                Button("Cancel") { showMoveSheet = false }.keyboardShortcut(.cancelAction)
                Button("Move") {
                    if let dest = moveDestinationId { state.moveSelectedUnder(destinationId: dest) }
                    showMoveSheet = false
                }
                .keyboardShortcut(.defaultAction)
                .disabled(moveDestinationId == nil)
            }
        }
        .padding()
        .frame(width: 320)
    }

    @ViewBuilder
    private func moveDestinationGroup(title: String, items: [NestedListItem]) -> some View {
        if !items.isEmpty {
            Text(title.uppercased())
                .font(.caption).bold()
                .foregroundStyle(Color.accentColor)
                .padding(.top, 6)
            ForEach(items, id: \.id) { dest in
                let childCount = state.indexById[dest.id]?.children.count ?? 0
                Button {
                    moveDestinationId = dest.id
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: moveDestinationId == dest.id ? "largecircle.fill.circle" : "circle")
                            .foregroundStyle(moveDestinationId == dest.id ? Color.accentColor : Color.secondary)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(dest.text.isEmpty ? "Untitled item" : dest.text)
                                .foregroundStyle(.primary)
                                .lineLimit(1)
                            Text(childCount > 0 ? "\(childCount) children" : "No children")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.horizontal, 4).padding(.vertical, 6)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .focusable(false)
            }
        }
    }

    // MARK: - Outline arrow keys

    /// Extracted from outlineList so the giant list expression stays within
    /// the type-checker's limits. Shift+Ctrl+Left/Right = collapse/expand all
    /// under the selection (else current view);
    private func handleOutlineArrowKey(_ press: KeyPress) -> KeyPress.Result {
        // Fields own their arrows (explicit cursor driving); without this
        // the outline would collapse/outdent mid-typing.
        guard state.editingId == nil, state.draft == nil else { return .ignored }
        if press.modifiers.contains(.control) {
            if press.key == .leftArrow { state.collapseAll() } else { state.expandAll() }
            return .handled
        }
        guard let id = state.selectedId,
              let node = state.indexById[id]
        else { return .ignored }
        let collapsed = node.item.collapsed
        if press.key == .leftArrow, node.hasChildren, !collapsed {
            state.toggleCollapse(id: id)
            return .handled
        }
        if press.key == .leftArrow, collapsed || !node.hasChildren {
            state.selectParentOfSelected()
            return .handled
        }
        if press.key == .rightArrow, collapsed {
            state.toggleCollapse(id: id)
            return .handled
        }
        return .ignored
    }

    private func draftRow(depth: Int) -> some View {
        HStack(alignment: .center, spacing: 0) {
            ForEach(0..<depth, id: \.self) { level in
                Rectangle()
                    .fill(nestedGuideColor(level))
                    .frame(width: 1)
                    .padding(.leading, 10)
                    .frame(width: 16, alignment: .leading)
            }
            Circle()
                .fill(nestedDotForDepth(depth))
                .frame(width: 7, height: 7)
                .frame(width: 16, height: 24)
                .padding(.leading, 3)
            TextField("New item…", text: $draftText, axis: .vertical)
                .textFieldStyle(.roundedBorder)
                .focused($draftFocused)
                .onChange(of: draftText) { _, t in
                    // Deferred: writing state.draft synchronously here publishes
                    // NestedEditorState from within view updates (Xcode warns).
                    DispatchQueue.main.async {
                        if state.draft != nil { state.draft?.text = t }
                    }
                }
                .onKeyPress(keys: [.return]) { press in
                    // Plain Return commits (never newline); Shift+Return
                    // inserts via the field editor directly (returning
                    // `.ignored` does not reliably re-deliver). Single
                    // commit path (no onSubmit), and commitDraft consumes
                    // the draft, so no double-fire.
                    if press.modifiers.contains(.shift) {
                        if let editor = nestedFieldEditor() {
                            editor.insertText("\n", replacementRange: editor.selectedRange())
                        }
                        return .handled
                    }
                    state.commitDraft(thenContinue: false)
                    return .handled
                }
                .onKeyPress(.escape) {
                    state.cancelDraft()
                    return .handled
                }
                .onKeyPress(keys: [.tab]) { press in
                    if press.modifiers.contains(.shift) {
                        state.commitDraft(thenContinue: false)
                        state.outdentSelected()
                    } else {
                        state.commitDraft(thenContinue: false)
                        state.indentSelected()
                    }
                    return .handled
                }
                .onKeyPress(keys: [.leftArrow, .rightArrow, .upArrow, .downArrow]) { press in
                    // Bare Cmd+Up/Down keeps its outline meaning below.
                    // Everything else is cursor or selection movement.
                    if press.modifiers.contains(.command), !press.modifiers.contains(.shift),
                       press.key == .upArrow || press.key == .downArrow {
                        let hasText = !draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        if hasText {
                            state.commitDraft(thenContinue: false)
                        } else {
                            state.cancelDraft()
                            if press.key == .upArrow { state.moveSelectedUp() } else { state.moveSelectedDown() }
                        }
                        return .handled
                    }
                    return nestedArrowKeys(press)
                }
            Button {
                state.cancelDraft()
            } label: {
                Image(systemName: "xmark.circle")
            }
            .buttonStyle(.plain)
            .help("Cancel")
        }
        // No vertical padding here either: it would cut the guides
        // around the draft row. (Horizontal is owned by the LazyVStack.)
        .onAppear {
            draftText = state.draft?.text ?? ""
            draftFocused = true
        }
    }

    @ViewBuilder
    private var bottomBar: some View {
        if let id = state.selectedId, let node = state.indexById[id] {
            NestedFormattingBar(state: state, item: node.item)
                .id("\(id)-\(node.item.updatedAtMillis)")
        }
    }
}

// MARK: - Editor bar button

/// Label for the editor-bar icon buttons. It reads `isEnabled` from the
/// environment so the `.disabled(...)` callers apply actually shows: an
/// explicit `foregroundStyle` inside a `Button` label overrides macOS's
/// automatic disabled dimming, which left enabled and disabled buttons
/// looking identical.
private struct EditorBarButtonLabel: View {
    let system: String
    let shortcut: String?
    let active: Bool
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        HStack(spacing: 3) {
            Image(systemName: system)
                .imageScale(.medium)
            if let shortcut {
                Text(shortcut)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(minWidth: 28, minHeight: 28)
        .foregroundStyle(active ? Color.accentColor : Color.primary)
        .opacity(isEnabled ? 1 : 0.35)
        .background(
            RoundedRectangle(cornerRadius: 6)
                .fill(active ? Color.accentColor.opacity(0.14) : Color.clear)
        )
    }
}

// MARK: - Window access for the Tab key monitor

final class WindowReportView: NSView {
    var onWindow: ((NSWindow?) -> Void)?
    override func viewDidMoveToWindow() {
        super.viewDidMoveToWindow()
        onWindow?(window)
    }
}

struct WindowAccessor: NSViewRepresentable {
    let onWindow: (NSWindow?) -> Void
    func makeNSView(context: Context) -> WindowReportView {
        let v = WindowReportView()
        v.onWindow = onWindow
        return v
    }
    func updateNSView(_ nsView: WindowReportView, context: Context) {
        nsView.onWindow = onWindow
    }
}
