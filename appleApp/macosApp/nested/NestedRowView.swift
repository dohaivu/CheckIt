//
//  NestedRowView.swift
//  macosApp — one outline row + shared display helpers.
//

import SwiftUI
import Shared
import UniformTypeIdentifiers

// MARK: - Display mappings (mirror NestedListScreen.kt)

func nestedTokenColor(_ token: NestedColorToken) -> Color {
    switch token {
    case NestedColorToken.red: Color(red: 0xE5 / 255.0, green: 0x73 / 255.0, blue: 0x73 / 255.0)
    case NestedColorToken.orange: Color(red: 1.0, green: 0xB7 / 255.0, blue: 0x4D / 255.0)
    case NestedColorToken.yellow: Color(red: 1.0, green: 0xD5 / 255.0, blue: 0x4F / 255.0)
    case NestedColorToken.green: Color(red: 0x66 / 255.0, green: 0xBB / 255.0, blue: 0x6A / 255.0)
    case NestedColorToken.blue: Color(red: 0x42 / 255.0, green: 0xA5 / 255.0, blue: 0xF5 / 255.0)
    case NestedColorToken.purple: Color(red: 0xAB / 255.0, green: 0x47 / 255.0, blue: 0xBC / 255.0)
    case NestedColorToken.pink: Color(red: 0xEC / 255.0, green: 0x40 / 255.0, blue: 0x7A / 255.0)
    default: Color.secondary
    }
}

func nestedRowFont(_ styleName: String) -> Font {
    switch styleName {
    case "Header": .title3.weight(.semibold)
    case "Subheader": .headline
    default: .body
    }
}

extension Color {
    init?(nestedHex hex: String) {
        var s = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6, let v = UInt32(s, radix: 16) else { return nil }
        self.init(
            red: Double((v >> 16) & 0xFF) / 255.0,
            green: Double((v >> 8) & 0xFF) / 255.0,
            blue: Double(v & 0xFF) / 255.0
        )
    }
}

// MARK: - Palette (match NestedListScreen reference, tuned for long sessions)
//
// Warm paper canvas instead of near-white: it cuts blue-light glare while
// keeping dark-ink contrast, which fatigues less over hours. Guides cycle
// one muted color per depth; the toggle ring/dot uses the same depth color
// so the eye can follow nesting by hue. Both stay faint by default and only
// the active row (see `isEmphasized`) draws them at full strength.

let nestedDepthSolids: [Color] = [
    Color(red: 0xD9 / 255.0, green: 0x7F / 255.0, blue: 0x5F / 255.0), // 0 terracotta
    Color(red: 0x7E / 255.0, green: 0xA3 / 255.0, blue: 0x8A / 255.0), // 1 sage
    Color(red: 0x7B / 255.0, green: 0x9E / 255.0, blue: 0xBD / 255.0), // 2 dusty blue
    Color(red: 0xC0 / 255.0, green: 0x9F / 255.0, blue: 0x5E / 255.0), // 3 warm sand
]
let nestedDotColor = nestedDepthSolids[0]
let nestedCanvasColor = Color(red: 0xF7 / 255.0, green: 0xF3 / 255.0, blue: 0xEA / 255.0)
let nestedSelectedColor = Color(red: 0xF9 / 255.0, green: 0xEC / 255.0, blue: 0xC8 / 255.0)
/// Editing tint: cool accent wash, distinct from the warm selection cream.
let nestedEditingColor = Color.accentColor.opacity(0.18)
let nestedInkColor = Color(red: 0x2E / 255.0, green: 0x2A / 255.0, blue: 0x26 / 255.0)

// Guides and dots are quiet by default: the depth hue is a hint, not a
// statement. The active row (selected or editing) promotes its own guides
// and spine to full strength so the eye can follow the branch, without
// repainting the whole outline.
let nestedGuideSubtleOpacity = 0.18
let nestedGuideStrongOpacity = 0.92
let nestedDotSubtleOpacity = 0.5

func nestedDepthColor(_ depth: Int) -> Color {
    nestedDepthSolids[depth % nestedDepthSolids.count]
}

func nestedGuideColor(_ level: Int, emphasized: Bool = false) -> Color {
    nestedDepthColor(level).opacity(emphasized ? nestedGuideStrongOpacity : nestedGuideSubtleOpacity)
}

/// Quiet by default; `emphasized` restores the solid depth color.
func nestedDotForDepth(_ depth: Int, emphasized: Bool = false) -> Color {
    nestedDepthColor(depth).opacity(emphasized ? 1.0 : nestedDotSubtleOpacity)
}

private let nestedDateFormatter: DateFormatter = {
    let fmt = DateFormatter()
    fmt.dateStyle = .medium
    fmt.timeStyle = .none
    return fmt
}()

struct NestedDateLabel {
    let text: String
    let isOverdue: Bool
}

func nestedDateLabel(
    startDays: Int64?,
    endDays: Int64?,
    todayDays: Int64 = DateExtensionsKt.today().toEpochDays()
) -> NestedDateLabel? {
    guard startDays != nil || endDays != nil else { return nil }
    func s(_ d: Int64) -> String { nestedDateFormatter.string(from: nestedDate(fromEpochDays: d)) }
    let text: String
    switch (startDays, endDays) {
    case let (.some(a), .some(b)): text = a == b ? s(a) : "\(s(a)) → \(s(b))"
    case let (.some(a), nil): text = "from \(s(a))"
    case let (nil, .some(b)): text = "until \(s(b))"
    default: return nil
    }
    // The end date is inclusive: only a date before today is overdue.
    return NestedDateLabel(text: text, isOverdue: endDays.map { $0 < todayDays } ?? false)
}

// MARK: - Row

struct NestedRowView: View {
    @ObservedObject var state: NestedEditorState
    let row: NestedRow
    let isEditing: Bool
    let focusTier: NestedFocusTier
    let selectionBranch: NestedSelectionBranch?

    @State private var editText = ""
    @State private var rowHeight: CGFloat = 32
    @FocusState private var fieldFocused: Bool

    private var item: NestedListItem { row.node.item }

    var body: some View {
        // Ancestor guides stay quiet always: the emphasis is the selection
        // branch's dot/ring plus its spine running down through its children.
        // Descendant rows carry that spine at the root's level.
        let spineLevel = selectedSpineLevel
        let dotEmphasis = isEmphasized || isSpineRoot
        HStack(alignment: .top, spacing: 0) {
            // Indent guides, sharing one x per depth (slightly right of
            // cell center) — the toggle column below uses the same grid.
            ForEach(0..<row.depth, id: \.self) { level in
                Rectangle()
                    .fill(nestedGuideColor(level, emphasized: level == spineLevel))
                    .frame(width: 1)
                    .padding(.leading, 10)
                    .frame(width: 16, alignment: .leading)
                    .opacity(focusOpacity)
                    .animation(.easeOut(duration: 0.15), value: level == spineLevel)
            }
            // Collapse toggle: chevron wrapped in an outside circle (parents)
            // or a small filled dot (leaves), matching the attachment where
            // a collapsed parent shows a dot inside an outer ring.
            // 2pt top offset puts the 13pt ring on the text's visual
            // center (content starts 5pt down), whatever the row height.
            Button {
                state.toggleCollapse(id: item.id)
            } label: {
                Group {
                    if row.node.hasChildren {
                        ZStack {
                            Circle()
                                .stroke(nestedDotForDepth(row.depth, emphasized: dotEmphasis), lineWidth: dotEmphasis ? 2 : 1.5)
                                .frame(width: 12, height: 12)
                            Image(systemName: item.collapsed ? "chevron.right" : "chevron.down")
                                .font(.system(size: 8, weight: .semibold))
                                .foregroundStyle(nestedDotForDepth(row.depth, emphasized: dotEmphasis))
                        }
                    } else {
                        Circle()
                            .fill(nestedDotForDepth(row.depth, emphasized: dotEmphasis))
                            .frame(width: 7, height: 7)
                    }
                }
                .frame(width: 16, height: 24)
                .contentShape(Rectangle())
                .opacity(focusOpacity)
                .animation(.easeOut(duration: 0.15), value: dotEmphasis)
            }
            .buttonStyle(.plain)
            .padding(.top, 2).padding(.leading, 3)
            .disabled(!row.node.hasChildren)
            .help(row.node.hasChildren ? (item.collapsed ? "Expand" : "Collapse") : "")

            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .center, spacing: 6) {
                    if item.checkboxEnabled {
                        Button {
                            state.toggleCheck(id: item.id, checked: !item.checked)
                        } label: {
                            Image(systemName: item.checked ? "checkmark.square.fill" : "square")
                                .foregroundStyle(item.checked ? Color.accentColor : .secondary)
                                .opacity(focusOpacity)
                        }
                        .buttonStyle(.plain)
                        .help(item.checked ? "Uncheck" : "Check off")
                    }
                    if let hex = item.priority.priorityHex(),
                       let color = Color(nestedHex: hex) {
                        Image(systemName: "flag.fill")
                            .imageScale(.small)
                            .foregroundStyle(color)
                            .opacity(focusOpacity)
                            .padding(.top, 1)
                            .help("Priority: \(item.priority.name)")
                    }
                    if let label = item.label, !label.isEmpty {
                        Text(label)
                            .font(.caption2.weight(.semibold))
                            .lineLimit(1)
                            .truncationMode(.tail)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 2)
                            .background(Color.accentColor.opacity(0.15), in: Capsule())
                            .frame(maxWidth: 160, alignment: .leading)
                            .fixedSize(horizontal: true, vertical: false)
                            .opacity(focusOpacity)
                            .help("Label: \(label)")
                    }
                    // Direct field (not overlay): multiline growth needs a
                    // layout-defining field; the few-pt inset delta while
                    // editing is accepted. Edit stays raw; display parses.
                    if isEditing {
                        TextField("", text: $editText, axis: .vertical)
                            .textFieldStyle(.plain)
                            .font(nestedRowFont(item.textStyle.name))
                            .focused($fieldFocused)
                            .onAppear {
                                editText = item.text
                                fieldFocused = true
                            }
                            .onChange(of: editText) { state.editingText = $1 }
                            .onChange(of: fieldFocused) { _, focused in
                                // Focus loss (tap-away, toolbar, sheet) commits;
                                // explicit paths already cleared editingId, so
                                // this skips then. Must hop a tick: observers
                                // run inside view updates.
                                if !focused {
                                    Task { @MainActor in state.commitEditing() }
                                }
                            }
                            .onKeyPress(keys: [.return]) { press in
                                // Plain Return commits (never newline).
                                // Shift+Return inserts via the field editor
                                // directly: returning `.ignored` does not
                                // reliably re-deliver for insertion.
                                // Cmd+Return commits without opening a draft.
                                if press.modifiers.contains(.shift) {
                                    if let editor = nestedFieldEditor() {
                                        editor.insertText("\n", replacementRange: editor.selectedRange())
                                    }
                                    return .handled
                                }
                                state.commitEdit(id: item.id, text: editText)
                                fieldFocused = false
                                return .handled
                            }
                            .onKeyPress(.escape) {
                                fieldFocused = false
                                state.cancelEdit()
                                return .handled
                            }
                            .onKeyPress(keys: [.tab]) { press in
                                fieldFocused = false
                                state.commitEdit(id: item.id, text: editText)
                                if press.modifiers.contains(.shift) {
                                    state.outdentSelected()
                                } else {
                                    state.indentSelected()
                                }
                                state.startEdit(id: item.id, text: editText)
                                return .handled
                            }
                            .onKeyPress(keys: [.leftArrow, .rightArrow, .upArrow, .downArrow]) { press in
                                // Bare Cmd+Up/Down keeps its move-item meaning,
                                // committing first. Everything else is cursor or
                                // selection movement in the field.
                                if press.modifiers.contains(.command), !press.modifiers.contains(.shift),
                                   press.key == .upArrow || press.key == .downArrow {
                                    fieldFocused = false
                                    state.commitEdit(id: item.id, text: editText)
                                    if press.key == .upArrow { state.moveSelectedUp() } else { state.moveSelectedDown() }
                                    return .handled
                                }
                                return nestedArrowKeys(press)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .writingToolsBehavior(.complete)
                    } else {
                        Text(item.text.isEmpty ? AttributedString("Untitled item") : renderInlineMarkdown(item.text))
                            .font(nestedRowFont(item.textStyle.name))
                            .foregroundStyle(nestedTextColor)
                            .strikethrough(item.checked)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(Rectangle())
                            .onTapGesture(count: 2) { state.startEdit(id: item.id) }
                    }
                }
                metadata.opacity(focusOpacity)
            }
            .padding(.vertical, 5 + shapeExtraVerticalPadding)
            .padding(.horizontal, 6 + shapeExtraHorizontalPadding)
            .background(rowBackground, in: rowShape)
            .clipShape(rowShape)
            .overlay { rowBorderOverlay }
            .contentShape(Rectangle())
        }
        .background(alignment: .topLeading) {
            // Guide continuation from the toggle down through the children.
            // Same 16pt grid x as the guides. The toggle is a 13pt circle
            // in a 16pt slot with 2pt top padding, so its bottom edge sits
            // at 20pt — start the line there so it touches the ring.
            if row.node.hasChildren && !item.collapsed {
                Rectangle()
                    .fill(nestedGuideColor(row.depth, emphasized: isEmphasized || isSpineRoot))
                    .frame(width: 1)
                    .padding(.top, 20)
                    .padding(.leading, CGFloat(row.depth * 16) + 10)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    .opacity(focusOpacity)
                    .animation(.easeOut(duration: 0.15), value: isEmphasized || isSpineRoot)
            }
        }
        .background(nestedCanvasColor)
        .opacity(state.draggedId == item.id ? 0.35 : 1.0)
        .background(
            GeometryReader { geo in
                Color.clear
                    .onAppear { rowHeight = geo.size.height }
                    .onChange(of: geo.size.height) { _, newH in
                        if abs(rowHeight - newH) > 1 { rowHeight = newH }
                    }
            }
        )
        .onDrag({
            state.draggedId = item.id
            return NSItemProvider(object: "nested:\(item.id)" as NSString)
        }) {
            dragPreview
        }
        .onDrop(
            of: [.text],
            delegate: NestedRowDropDelegate(
                row: row,
                state: state,
                rowHeight: rowHeight
            )
        )
    }

    private var dragPreview: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(nestedDepthColor(row.depth))
                .frame(width: 7, height: 7)
            Text(item.text.isEmpty ? AttributedString("Untitled item") : renderInlineMarkdown(item.text))
                .font(nestedRowFont(item.textStyle.name))
                .foregroundStyle(nestedTextColor)
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(nestedSelectedColor)
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .shadow(color: .black.opacity(0.18), radius: 4, y: 2)
    }

    private var nestedTextColor: Color {
        // Context rows (kept only to hold matches under their ancestors)
        // recede so working rows carry the eye; selecting one restores ink.
        if row.isContext && !isEmphasized {
            return Color.secondary.opacity(min(0.6, focusOpacity))
        }
        // Android parity (NestedListScreen): rows in a checked branch (self
        // or any ancestor checked) fade to onSurfaceVariant at 0.68 alpha.
        if row.isInCheckedBranch {
            return Color.secondary.opacity(min(0.68, focusOpacity))
        }
        // Explicit light-theme ink: .primary would turn white in dark mode
        // and vanish on the paper canvas. Warm charcoal is softer than pure
        // black for long sessions.
        let base = item.textColor == NestedColorToken.default_
            ? nestedInkColor
            : nestedTokenColor(item.textColor)
        return base.opacity(focusOpacity)
    }

    /// Focus selection dims everything outside the selection branch. Path
    /// rows keep a little more ink than unrelated branches so the way back
    /// to the root stays legible. `min()` in `nestedTextColor` avoids
    /// stacking context/checked-branch alpha into unreadable text.
    private var focusOpacity: Double {
        switch focusTier {
        case .none, .focused: return 1
        case .path: return 0.72
        case .outside: return 0.45
        }
    }

    private var isSelected: Bool { state.selectedId == item.id }

    /// The active row gets full-strength ink and selection chrome.
    private var isEmphasized: Bool { isSelected || isEditing }

    /// True for the effective branch root. A selected leaf promotes to its
    /// parent, whose downward guide should still read as the spine.
    private var isSpineRoot: Bool { selectionBranch?.rootId == item.id }

    /// Depth column carrying the selection branch's spine through its
    /// subtree. Nil on the root row itself (its own guides point up at
    /// ancestors, which stay quiet) and on rows outside the branch.
    private var selectedSpineLevel: Int? {
        guard let branch = selectionBranch,
              branch.rootId != item.id,
              branch.range.contains(row.index)
        else { return nil }
        return branch.depth
    }

    private var rowBackground: Color {
        if isEditing { return nestedEditingColor }
        if isSelected { return nestedSelectedColor }
        if item.backgroundColor != NestedColorToken.default_ {
            return nestedTokenColor(item.backgroundColor).opacity(0.20 * focusOpacity)
        }
        return .clear
    }

    private var rowShape: NestedCardShape {
        NestedCardShape(kind: item.borderShape)
    }

    /// Shape-specific breathing room: scallops/notches would otherwise run
    /// into the checkbox, text, and labels.
    private var shapeExtraHorizontalPadding: CGFloat {
        switch item.borderShape {
        case NestedBorderShape.cloud, NestedBorderShape.ticket: return 8
        case NestedBorderShape.hexagon: return 6
        case NestedBorderShape.cutcorner, NestedBorderShape.leaf: return 3
        default: return 0
        }
    }

    private var shapeExtraVerticalPadding: CGFloat {
        switch item.borderShape {
        case NestedBorderShape.cloud: return 5
        case NestedBorderShape.ticket: return 2
        default: return 0
        }
    }

    private var rowBorderColor: Color {
        let base = item.borderColor == NestedColorToken.default_
            ? Color.secondary.opacity(0.55)
            : nestedTokenColor(item.borderColor)
        return base.opacity(focusOpacity)
    }

    private var rowBorderDash: [CGFloat] {
        switch item.borderStyle {
        case NestedBorderStyle.dashed: return [4.5, 3]
        case NestedBorderStyle.dotted: return [0.5, 2.8]
        default: return []
        }
    }

    @ViewBuilder
    private var rowBorderOverlay: some View {
        if item.borderStyle != NestedBorderStyle.none {
            rowShape.strokeBorder(
                rowBorderColor,
                style: StrokeStyle(lineWidth: 1.5, lineCap: .round, dash: rowBorderDash)
            )
        }
    }

    @ViewBuilder
    private var metadata: some View {
        let summary = state.summary(for: item.id)
        let progress = item.progressPercent?.int32Value
        let note = item.note
        let dateLabel = nestedDateLabel(
            startDays: item.startDate?.toEpochDays(),
            endDays: item.endDate?.toEpochDays()
        )
        // Android parity (NestedItemMetadataPreview): leaf items always show
        // their own tracked minutes; parents only when showTrackedMinutes.
        let isLeaf = !row.node.hasChildren
        let showTracked = isLeaf || item.showTrackedMinutes
        let visibleMetrics = item.manualMetrics.filter { $0.enabled && ($0.isCompleted || $0.isValidForSave()) }
        if progress != nil || (summary?.totalTaskCount ?? 0) > 0
            || (showTracked && (summary?.trackedMinutes ?? 0) > 0)
            || (note != nil && !(note!.isEmpty)) || !item.tags.isEmpty || dateLabel != nil
            || !visibleMetrics.isEmpty
        {
            VStack(alignment: .leading, spacing: 3) {
                if let p = progress {
                    HStack(spacing: 6) {
                        // Slim custom bar: native linear ProgressView is tall
                        // and accent-blue, too strong on the paper canvas.
                        GeometryReader { geo in
                            let ratio = Double(max(0, min(100, Int(p)))) / 100.0
                            ZStack(alignment: .leading) {
                                RoundedRectangle(cornerRadius: 2)
                                    .fill(Color.black.opacity(0.08))
                                RoundedRectangle(cornerRadius: 2)
                                    .fill(nestedDepthColor(row.depth).opacity(0.65))
                                    .frame(width: geo.size.width * ratio)
                            }
                        }
                        .frame(height: 4)
                        Text("\(max(0, min(100, Int(p))))%")
                            .font(.caption).bold()
                            .foregroundStyle(.secondary)
                    }
                }
                HStack(spacing: 6) {
                    if let s = summary, s.totalTaskCount > 0 {
                        Text("\(s.doneTaskCount)/\(s.totalTaskCount) done")
                            .font(.caption)
                            .padding(.horizontal, 6).padding(.vertical, 1)
                            .background(Color.secondary.opacity(0.15), in: RoundedRectangle(cornerRadius: 5))
                    }
                    if let s = summary, showTracked && s.trackedMinutes > 0 {
                        Text("\(s.trackedMinutes) min")
                            .font(.caption)
                            .padding(.horizontal, 6).padding(.vertical, 1)
                            .background(Color.secondary.opacity(0.15), in: RoundedRectangle(cornerRadius: 5))
                    }
                    ForEach(visibleMetrics, id: \.name) { m in
                        let overdue = m.isMetricOverdueToday()
                        HStack(spacing: 2) {
                            if m.isCompleted {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.caption2).foregroundStyle(.green)
                            }
                            Text(metricLabel(m))
                        }
                        .padding(.horizontal, 6).padding(.vertical, 1)
                        .background((overdue ? Color.red : Color.accentColor).opacity(0.12), in: RoundedRectangle(cornerRadius: 5))
                    }
                }
                if let n = note, !n.isEmpty {
                    Text(renderInlineMarkdown(n))
                        .font(.callout).foregroundStyle(.secondary)
                        .lineLimit(state.noteLineLimit > 0 ? state.noteLineLimit : nil)
                }
                if dateLabel != nil || !item.tags.isEmpty {
                    HStack(spacing: 6) {
                        if let date = dateLabel {
                            Label(date.text, systemImage: "calendar")
                                .font(.caption)
                                .foregroundStyle(date.isOverdue ? Color.red : Color.secondary)
                        }
                        let tags: [TagItem] = item.tags
                        ForEach(tags, id: \.id) { tag in
                            // Android parity (compact TagPill): tinted label
                            // icon plus name instead of a filled pill.
                            HStack(spacing: 2) {
                                Image(systemName: "tag.fill")
                                    .font(.caption2)
                                    .foregroundStyle(Color(nestedHex: tag.color) ?? .secondary)
                                Text(tag.name)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
        }
    }

    private func metricLabel(_ m: MetricItem) -> AttributedString {
        // Mirrors shared toAnnotatedString: name/unit small and secondary,
        // value and target bigger, bold and highlighted (red when overdue).
        var result = AttributedString("")
        let overdue = m.isMetricOverdueToday()
        let highlight: Color = overdue ? .red : .accentColor
        if !m.name.isEmpty {
            var name = AttributedString(m.name + " ")
            name.font = .caption
            name.foregroundColor = .secondary
            result += name
        }
        if m.isDateBased() {
            var value = AttributedString(m.dateBasedDisplayToday())
            value.font = .callout.bold()
            value.foregroundColor = highlight
            result += value
        } else {
            if !m.value.isEmpty {
                var value = AttributedString(m.value)
                value.font = .callout.bold()
                value.foregroundColor = highlight
                result += value
            }
            if let t = m.targetValue, !t.isEmpty {
                var slash = AttributedString("/")
                slash.font = .caption
                slash.foregroundColor = .secondary
                result += slash
                var target = AttributedString(t)
                target.font = .callout.bold()
                target.foregroundColor = .primary
                result += target
            }
            if let u = m.displayUnit() {
                var unit = AttributedString(" " + u)
                unit.font = .caption
                unit.foregroundColor = .secondary
                result += unit
            }
        }
        return result
    }
}

// MARK: - Drop Indicator

struct DropIndicatorLine: View {
    let depth: Int

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            ForEach(0..<depth, id: \.self) { level in
                Rectangle()
                    .fill(nestedGuideColor(level))
                    .frame(width: 1)
                    .padding(.leading, 10)
                    .frame(width: 16, alignment: .leading)
            }
            Circle()
                .fill(Color.accentColor)
                .frame(width: 7, height: 7)
                .frame(width: 16)
                .padding(.leading, 3)
            Capsule()
                .fill(Color.accentColor)
                .frame(height: 2.5)
                .padding(.trailing, 12)
        }
        .frame(height: 8)
        .padding(.vertical, 1)
        .background(nestedCanvasColor)
        .animation(.spring(response: 0.22, dampingFraction: 0.82), value: depth)
    }
}

// MARK: - Drop Delegates

struct NestedRowDropDelegate: DropDelegate {
    let row: NestedRow
    let state: NestedEditorState
    let rowHeight: CGFloat

    func validateDrop(info: DropInfo) -> Bool {
        info.hasItemsConforming(to: [.text])
    }

    func dropEntered(info: DropInfo) {
        updateTarget(info: info)
    }

    func dropUpdated(info: DropInfo) -> DropProposal? {
        updateTarget(info: info)
        if state.dropTarget != nil {
            return DropProposal(operation: .move)
        } else {
            return DropProposal(operation: .forbidden)
        }
    }

    func dropExited(info: DropInfo) {
        if state.dropTarget?.displayRowId == row.id {
            state.clearDropTarget()
        }
    }

    func performDrop(info: DropInfo) -> Bool {
        guard let provider = info.itemProviders(for: [.text]).first else {
            state.clearDropTarget()
            return false
        }

        let isBelow = info.location.y > (rowHeight / 2)
        let locX = info.location.x
        let currentTarget = state.dropTarget

        _ = provider.loadObject(ofClass: NSString.self) { payload, _ in
            guard let s = payload as? String, s.hasPrefix("nested:") else {
                DispatchQueue.main.async { state.clearDropTarget() }
                return
            }
            let dragged = String(s.dropFirst("nested:".count))
            DispatchQueue.main.async {
                let target = currentTarget ?? state.resolveDropTarget(
                    hoveredRow: row,
                    isBelow: isBelow,
                    pointerX: locX,
                    draggedItemId: dragged
                )
                if let target = target {
                    state.executeDrop(target: target, draggedId: dragged)
                } else {
                    state.clearDropTarget()
                }
            }
        }
        return true
    }

    private func updateTarget(info: DropInfo) {
        let isBelow = info.location.y > (rowHeight / 2)
        state.updateDropTarget(
            hoveredRow: row,
            isBelow: isBelow,
            pointerX: info.location.x,
            draggedItemId: nil
        )
    }
}

struct OutlineBottomDropDelegate: DropDelegate {
    let state: NestedEditorState

    func validateDrop(info: DropInfo) -> Bool {
        info.hasItemsConforming(to: [.text])
    }

    func dropEntered(info: DropInfo) {
        updateTarget(info: info)
    }

    func dropUpdated(info: DropInfo) -> DropProposal? {
        updateTarget(info: info)
        return state.dropTarget != nil ? DropProposal(operation: .move) : DropProposal(operation: .forbidden)
    }

    func dropExited(info: DropInfo) {
        if state.dropTarget?.below == true {
            state.clearDropTarget()
        }
    }

    func performDrop(info: DropInfo) -> Bool {
        guard let provider = info.itemProviders(for: [.text]).first else {
            state.clearDropTarget()
            return false
        }
        let currentTarget = state.dropTarget
        let locX = info.location.x
        let rows = state.visibleRows
        let lastRow = rows.last

        _ = provider.loadObject(ofClass: NSString.self) { payload, _ in
            guard let s = payload as? String, s.hasPrefix("nested:") else {
                DispatchQueue.main.async { state.clearDropTarget() }
                return
            }
            let dragged = String(s.dropFirst("nested:".count))
            DispatchQueue.main.async {
                let target: NestedDropTarget?
                if let current = currentTarget {
                    target = current
                } else if let last = lastRow {
                    target = state.resolveDropTarget(
                        hoveredRow: last,
                        isBelow: true,
                        pointerX: locX,
                        draggedItemId: dragged
                    )
                } else {
                    target = nil
                }

                if let target = target {
                    state.executeDrop(target: target, draggedId: dragged)
                } else {
                    state.clearDropTarget()
                }
            }
        }
        return true
    }

    private func updateTarget(info: DropInfo) {
        guard let last = state.visibleRows.last else { return }
        state.updateDropTarget(
            hoveredRow: last,
            isBelow: true,
            pointerX: info.location.x,
            draggedItemId: nil
        )
    }
}

// MARK: - Row card shapes
//
// Mirrors NestedBorderShape / NestedBorderStyle in the shared domain. The
// geometry is authored against the row card bounds, so the same shape drives
// both the background fill and the inset border.

struct NestedCardShape: InsettableShape {
    let kind: NestedBorderShape
    var insetAmount: CGFloat = 0

    func inset(by amount: CGFloat) -> NestedCardShape {
        var copy = self
        copy.insetAmount += amount
        return copy
    }

    func path(in rect: CGRect) -> Path {
        let r = rect.insetBy(dx: insetAmount, dy: insetAmount)
        guard r.width > 0, r.height > 0 else { return Path() }
        switch kind {
        case NestedBorderShape.rectangle:
            var p = Path()
            p.addRect(r)
            return p
        case NestedBorderShape.pill:
            return Path(roundedRect: r, cornerRadius: min(r.width, r.height) / 2)
        case NestedBorderShape.cloud:
            return cloudCardPath(r)
        case NestedBorderShape.ticket:
            return ticketCardPath(r)
        case NestedBorderShape.cutcorner:
            return cutCornerCardPath(r)
        case NestedBorderShape.hexagon:
            return hexagonCardPath(r)
        case NestedBorderShape.leaf:
            return leafCardPath(r)
        default:
            return Path(roundedRect: r, cornerRadius: min(6, min(r.width, r.height) / 2))
        }
    }
}

private func ticketCardPath(_ r: CGRect) -> Path {
    let radius = min(min(r.width, r.height) * 0.32, 12)
    let notch = min(r.height * 0.18, 9)
    let cy = r.midY
    let c = notch * 0.66
    var p = Path()
    p.move(to: CGPoint(x: r.minX + radius, y: r.minY))
    p.addLine(to: CGPoint(x: r.maxX - radius, y: r.minY))
    p.addQuadCurve(
        to: CGPoint(x: r.maxX, y: r.minY + radius),
        control: CGPoint(x: r.maxX, y: r.minY)
    )
    p.addLine(to: CGPoint(x: r.maxX, y: cy - notch))
    p.addCurve(
        to: CGPoint(x: r.maxX, y: cy + notch),
        control1: CGPoint(x: r.maxX - c, y: cy - c),
        control2: CGPoint(x: r.maxX - c, y: cy + c)
    )
    p.addLine(to: CGPoint(x: r.maxX, y: r.maxY - radius))
    p.addQuadCurve(
        to: CGPoint(x: r.maxX - radius, y: r.maxY),
        control: CGPoint(x: r.maxX, y: r.maxY)
    )
    p.addLine(to: CGPoint(x: r.minX + radius, y: r.maxY))
    p.addQuadCurve(
        to: CGPoint(x: r.minX, y: r.maxY - radius),
        control: CGPoint(x: r.minX, y: r.maxY)
    )
    p.addLine(to: CGPoint(x: r.minX, y: cy + notch))
    p.addCurve(
        to: CGPoint(x: r.minX, y: cy - notch),
        control1: CGPoint(x: r.minX + c, y: cy + c),
        control2: CGPoint(x: r.minX + c, y: cy - c)
    )
    p.addLine(to: CGPoint(x: r.minX, y: r.minY + radius))
    p.addQuadCurve(
        to: CGPoint(x: r.minX + radius, y: r.minY),
        control: CGPoint(x: r.minX, y: r.minY)
    )
    p.closeSubpath()
    return p
}

private func cutCornerCardPath(_ r: CGRect) -> Path {
    let cut = max(min(r.height * 0.36, r.width * 0.10), 2)
    var p = Path()
    p.move(to: CGPoint(x: r.minX, y: r.minY))
    p.addLine(to: CGPoint(x: r.maxX - cut, y: r.minY))
    p.addLine(to: CGPoint(x: r.maxX, y: r.minY + cut))
    p.addLine(to: CGPoint(x: r.maxX, y: r.maxY))
    p.addLine(to: CGPoint(x: r.minX, y: r.maxY))
    p.closeSubpath()
    return p
}

private func hexagonCardPath(_ r: CGRect) -> Path {
    let slant = max(min(r.height * 0.45, r.width * 0.09), 2)
    var p = Path()
    p.move(to: CGPoint(x: r.minX, y: r.midY))
    p.addLine(to: CGPoint(x: r.minX + slant, y: r.minY))
    p.addLine(to: CGPoint(x: r.maxX - slant, y: r.minY))
    p.addLine(to: CGPoint(x: r.maxX, y: r.midY))
    p.addLine(to: CGPoint(x: r.maxX - slant, y: r.maxY))
    p.addLine(to: CGPoint(x: r.minX + slant, y: r.maxY))
    p.closeSubpath()
    return p
}

private func leafCardPath(_ r: CGRect) -> Path {
    let big = min(r.width, r.height) / 2
    let small = min(6, min(r.width, r.height) / 2)
    var p = Path()
    p.move(to: CGPoint(x: r.minX + big, y: r.minY))
    p.addLine(to: CGPoint(x: r.maxX - small, y: r.minY))
    p.addQuadCurve(
        to: CGPoint(x: r.maxX, y: r.minY + small),
        control: CGPoint(x: r.maxX, y: r.minY)
    )
    p.addLine(to: CGPoint(x: r.maxX, y: r.maxY - big))
    p.addQuadCurve(
        to: CGPoint(x: r.maxX - big, y: r.maxY),
        control: CGPoint(x: r.maxX, y: r.maxY)
    )
    p.addLine(to: CGPoint(x: r.minX + small, y: r.maxY))
    p.addQuadCurve(
        to: CGPoint(x: r.minX, y: r.maxY - small),
        control: CGPoint(x: r.minX, y: r.maxY)
    )
    p.addLine(to: CGPoint(x: r.minX, y: r.minY + big))
    p.addQuadCurve(
        to: CGPoint(x: r.minX + big, y: r.minY),
        control: CGPoint(x: r.minX, y: r.minY)
    )
    p.closeSubpath()
    return p
}

private func cloudCardPath(_ r: CGRect) -> Path {
    let depth = max(min(11, r.height * 0.20), 1.5)
    let left = r.minX + depth
    let top = r.minY + depth
    let right = r.maxX - depth
    let bottom = r.maxY - depth
    // Only slivers with no room for a body fall back to a plain card. The
    // scaled threshold that used to live here changed between the fill rect
    // and the half-stroke-inset border rect, so a Cloud fill could pair with
    // a non-cloud stroke on the same row.
    guard right - left > 1, bottom - top > 1 else {
        return Path(roundedRect: r, cornerRadius: min(r.width, r.height) * 0.2)
    }

    let corner = min(14, min(right - left, bottom - top) * 0.28)
    let horizontal = right - left - corner * 2
    let vertical = bottom - top - corner * 2
    let horizontalBumps = max(2, min(14, Int((horizontal / (depth * 3)).rounded())))
    let verticalBumps = max(1, min(5, Int((vertical / (depth * 3)).rounded())))
    let bumpW = horizontal / CGFloat(horizontalBumps)
    let bumpH = vertical / CGFloat(verticalBumps)
    let bulge = depth / 3

    var p = Path()
    p.move(to: CGPoint(x: left + corner, y: top))

    var x = left + corner
    for _ in 0..<horizontalBumps {
        p.addCurve(
            to: CGPoint(x: x + bumpW, y: top),
            control1: CGPoint(x: x + bumpW * 0.25, y: top - bulge),
            control2: CGPoint(x: x + bumpW * 0.75, y: top - bulge)
        )
        x += bumpW
    }
    p.addQuadCurve(
        to: CGPoint(x: right, y: top + corner),
        control: CGPoint(x: right, y: top)
    )

    var y = top + corner
    for _ in 0..<verticalBumps {
        p.addCurve(
            to: CGPoint(x: right, y: y + bumpH),
            control1: CGPoint(x: right + bulge, y: y + bumpH * 0.25),
            control2: CGPoint(x: right + bulge, y: y + bumpH * 0.75)
        )
        y += bumpH
    }
    p.addQuadCurve(
        to: CGPoint(x: right - corner, y: bottom),
        control: CGPoint(x: right, y: bottom)
    )

    x = right - corner
    for _ in 0..<horizontalBumps {
        p.addCurve(
            to: CGPoint(x: x - bumpW, y: bottom),
            control1: CGPoint(x: x - bumpW * 0.25, y: bottom + bulge),
            control2: CGPoint(x: x - bumpW * 0.75, y: bottom + bulge)
        )
        x -= bumpW
    }
    p.addQuadCurve(
        to: CGPoint(x: left, y: bottom - corner),
        control: CGPoint(x: left, y: bottom)
    )

    y = bottom - corner
    for _ in 0..<verticalBumps {
        p.addCurve(
            to: CGPoint(x: left, y: y - bumpH),
            control1: CGPoint(x: left - bulge, y: y - bumpH * 0.25),
            control2: CGPoint(x: left - bulge, y: y - bumpH * 0.75)
        )
        y -= bumpH
    }
    p.addQuadCurve(
        to: CGPoint(x: left + corner, y: top),
        control: CGPoint(x: left, y: top)
    )
    p.closeSubpath()
    return p
}
