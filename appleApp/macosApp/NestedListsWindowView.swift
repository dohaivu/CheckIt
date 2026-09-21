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

struct NestedListsWindowView: View {
    @StateObject private var state = NestedEditorState()
    @FocusState private var focusedRow: Int64?
    @FocusState private var draftFocused: Bool

    @State private var showNewDoc = false
    @State private var newDocTitle = ""
    @State private var renameDoc: NestedDocument? = nil
    @State private var showRename = false
    @State private var renameTitle = ""
    @State private var deleteDoc: NestedDocument? = nil
    @State private var draftText = ""

    var body: some View {
        HSplitView {
            sidebar
                .frame(minWidth: 200, idealWidth: 240, maxWidth: 360, maxHeight: .infinity)
            detail
                .frame(minWidth: 560, maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(minWidth: 760, minHeight: 480)
        .background(WindowAccessor(onWindow: { win in
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
    }

    // MARK: - Sidebar

    private var docSelection: Binding<Int64?> {
        Binding(
            get: { state.selectedDocId >= 0 ? state.selectedDocId : nil },
            set: { if let id = $0 { state.openDocument(id: id) } }
        )
    }

    private var sidebar: some View {
        VStack(spacing: 0) {
            List(state.documents, id: \.id, selection: docSelection) { doc in
                Label(doc.title.isEmpty ? "Untitled document" : doc.title, systemImage: "list.bullet")
                    .contextMenu {
                        Button("Rename") { renameDoc = doc; showRename = true }
                        Button("Delete", role: .destructive) { deleteDoc = doc }
                    }
            }
            .listStyle(.sidebar)
            Divider()
            Button {
                newDocTitle = ""
                showNewDoc = true
            } label: {
                Label("New document", systemImage: "plus")
            }
            .buttonStyle(.plain)
            .padding(8)
        }
    }

    private func createDoc() {
        let t = newDocTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty else { return }
        state.createDocument(title: t)
        showNewDoc = false
    }

    // MARK: - Detail

    @ViewBuilder
    private var detail: some View {
        if state.selectedDocId < 0 {
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
                    .padding(.top, 6)
                Divider()
                bottomBar
            }
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
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
    }

    // MARK: - Editor bar

    private var editorBar: some View {
        let sel = state.selectedId
        let node = sel.flatMap { state.indexById[$0] }
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                editorBtn("Zoom In", system: "plus.magnifyingglass", help: "Zoom in") {
                    state.zoomInSelected()
                }
                .disabled(!(node?.hasChildren ?? false))
                editorBtn("Zoom Out", system: "minus.magnifyingglass", help: "Zoom out") {
                    state.zoomOut()
                }
                .disabled(state.zoomPath.isEmpty)
                barSeparator
                editorBtn("Outdent", system: "arrow.left.to.line", help: "Outdent (Shift+Tab)") {
                    state.outdentSelected()
                }
                .disabled(sel == nil)
                editorBtn("Indent", system: "arrow.right.to.line", help: "Indent (Tab)") {
                    state.indentSelected()
                }
                .disabled(sel == nil)
                editorBtn("Up", system: "chevron.up", help: "Move up (Cmd+Up)") {
                    state.moveSelectedUp()
                }
                .disabled(sel == nil)
                editorBtn("Down", system: "chevron.down", help: "Move down (Cmd+Down)") {
                    state.moveSelectedDown()
                }
                .disabled(sel == nil)
                barSeparator
                editorBtn("Child", system: "plus", help: "Add child (Cmd+Return)") {
                    if let id = sel { state.startAddChild(of: id) }
                }
                .disabled(sel == nil)
                editorBtn("Sibling", system: "text.badge.plus", help: "Add sibling below (Shift+Return)") {
                    if let id = sel { state.startAddSibling(of: id) }
                }
                .disabled(sel == nil)
                editorBtn("Root", system: "plus.circle", help: "Add root item") {
                    state.startAddRoot()
                }
                editorBtn("Delete", system: "trash", help: "Delete (Del)") {
                    if sel != nil { state.showDeleteConfirm = true }
                }
                .disabled(sel == nil)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
        }
    }

    private func editorBtn(
        _ title: String,
        system: String,
        help: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: system)
                .imageScale(.medium)
                .frame(width: 28, height: 28)
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

    // MARK: - Outline list + keyboard

    private var outlineList: some View {
        let rows = state.visibleRows
        return ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    if state.draft?.anchorId == -1 {
                        draftRow(depth: 0)
                    }
                    if rows.isEmpty, state.draft == nil {
                        VStack(spacing: 8) {
                            Text("Nothing here yet").font(.headline)
                            Text("Start with a root item, then indent items to build your outline.")
                                .foregroundStyle(.secondary)
                            Button("Add item") { state.startAddRoot() }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 48)
                    }
                    ForEach(rows) { row in
                        VStack(alignment: .leading, spacing: 0) {
                            NestedRowView(
                                state: state,
                                row: row,
                                isSelected: state.selectedId == row.id,
                                isEditing: state.editingId == row.id
                            )
                            if state.draft?.anchorId == row.id, let d = state.draft {
                                draftRow(depth: d.depth)
                            }
                        }
                        .padding(.vertical, 1)
                        .focusable()
                        .focused($focusedRow, equals: row.id)
                        .id(row.id)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            state.select(id: row.id)
                            focusedRow = row.id
                        }
                    }
                }
                .padding(.horizontal, 8)
            }
            .background(Color(nsColor: .windowBackgroundColor))
            .scrollEdgeEffectHidden(true, for: .all)
            .onChange(of: state.selectedId) { _, id in
                focusedRow = id
                // Minimal scroll: only moves if the row is out of view.
                if let id { withAnimation { proxy.scrollTo(id) } }
            }
            .onChange(of: state.draft?.anchorId) { _, _ in
                draftText = state.draft?.text ?? ""
                draftFocused = state.draft != nil
            }
            .onKeyPress(keys: [.upArrow, .downArrow]) { press in
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
                    guard let id = state.selectedId else { return .ignored }
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
                if press.modifiers.contains(.command) {
                    if press.key == .leftArrow { state.outdentSelected() } else { state.indentSelected() }
                    return .handled
                }
                guard state.editingId == nil, let id = state.selectedId,
                      let node = state.indexById[id], node.hasChildren
                else { return .ignored }
                let collapsed = node.item.collapsed
                if press.key == .leftArrow, !collapsed {
                    state.toggleCollapse(id: id)
                    return .handled
                }
                if press.key == .rightArrow, collapsed {
                    state.toggleCollapse(id: id)
                    return .handled
                }
                return .ignored
            }
            .onKeyPress(keys: [.delete, .deleteForward]) { _ in
                guard state.editingId == nil, state.selectedId != nil else { return .ignored }
                state.showDeleteConfirm = true
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
        }
    }

    private func draftRow(depth: Int) -> some View {
        HStack(spacing: 0) {
            ForEach(0..<depth, id: \.self) { _ in
                Rectangle()
                    .fill(Color.accentColor.opacity(0.3))
                    .frame(width: 1)
                    .padding(.leading, 15)
            }
            Image(systemName: "circle.fill").font(.system(size: 6))
                .foregroundStyle(Color.accentColor.opacity(0.7))
                .frame(width: 22, height: 22)
            TextField("New item…", text: $draftText)
                .textFieldStyle(.roundedBorder)
                .focused($draftFocused)
                .onChange(of: draftText) { _, t in
                    if state.draft != nil { state.draft?.text = t }
                }
                .onSubmit { state.commitDraft(thenContinue: true) }
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
            Button {
                state.cancelDraft()
            } label: {
                Image(systemName: "xmark.circle")
            }
            .buttonStyle(.plain)
            .help("Cancel")
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 1)
        .onAppear {
            draftText = state.draft?.text ?? ""
            draftFocused = true
        }
    }

    @ViewBuilder
    private var bottomBar: some View {
        if let id = state.selectedId, let node = state.indexById[id] {
            NestedFormattingBar(state: state, item: node.item)
                .id(id)
        }
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
