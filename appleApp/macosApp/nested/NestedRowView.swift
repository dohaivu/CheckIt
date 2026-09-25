//
//  NestedRowView.swift
//  macosApp — one outline row + shared display helpers.
//

import SwiftUI
import Shared
import UniformTypeIdentifiers

// MARK: - Display mappings (mirror NestedListScreen.kt)

func nestedTokenColor(_ name: String) -> Color {
    switch name {
    case "Red": Color(red: 0xE5 / 255.0, green: 0x73 / 255.0, blue: 0x73 / 255.0)
    case "Orange": Color(red: 1.0, green: 0xB7 / 255.0, blue: 0x4D / 255.0)
    case "Yellow": Color(red: 1.0, green: 0xD5 / 255.0, blue: 0x4F / 255.0)
    case "Green": Color(red: 0x66 / 255.0, green: 0xBB / 255.0, blue: 0x6A / 255.0)
    case "Blue": Color(red: 0x42 / 255.0, green: 0xA5 / 255.0, blue: 0xF5 / 255.0)
    case "Purple": Color(red: 0xAB / 255.0, green: 0x47 / 255.0, blue: 0xBC / 255.0)
    case "Pink": Color(red: 0xEC / 255.0, green: 0x40 / 255.0, blue: 0x7A / 255.0)
    default: Color.secondary
    }
}

func nestedPriorityMarker(_ name: String) -> String {
    switch name {
    case "Low": "!"
    case "Medium": "!!"
    case "High": "!!!"
    default: ""
    }
}

func nestedPriorityColor(_ name: String) -> Color {
    switch name {
    case "Low": .green
    case "Medium": .orange
    case "High": .red
    default: .secondary
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
// so the eye can follow nesting by hue.

let nestedDepthSolids: [Color] = [
    Color(red: 0xD9 / 255.0, green: 0x7F / 255.0, blue: 0x5F / 255.0), // 0 terracotta
    Color(red: 0x7E / 255.0, green: 0xA3 / 255.0, blue: 0x8A / 255.0), // 1 sage
    Color(red: 0x7B / 255.0, green: 0x9E / 255.0, blue: 0xBD / 255.0), // 2 dusty blue
    Color(red: 0xC0 / 255.0, green: 0x9F / 255.0, blue: 0x5E / 255.0), // 3 warm sand
]
let nestedDotColor = nestedDepthSolids[0]
let nestedCanvasColor = Color(red: 0xF7 / 255.0, green: 0xF3 / 255.0, blue: 0xEA / 255.0)
let nestedSelectedColor = Color(red: 0xF9 / 255.0, green: 0xEC / 255.0, blue: 0xC8 / 255.0)
let nestedInkColor = Color(red: 0x2E / 255.0, green: 0x2A / 255.0, blue: 0x26 / 255.0)

func nestedGuideColor(_ level: Int) -> Color {
    nestedDepthSolids[level % nestedDepthSolids.count].opacity(0.5)
}

func nestedDotForDepth(_ depth: Int) -> Color {
    nestedDepthSolids[depth % nestedDepthSolids.count]
}

func nestedDateLabel(startDays: Int64?, endDays: Int64?) -> String? {
    guard startDays != nil || endDays != nil else { return nil }
    let fmt = DateFormatter()
    fmt.dateStyle = .medium
    fmt.timeStyle = .none
    func s(_ d: Int64) -> String { fmt.string(from: nestedDate(fromEpochDays: d)) }
    switch (startDays, endDays) {
    case let (.some(a), .some(b)): return a == b ? s(a) : "\(s(a)) → \(s(b))"
    case let (.some(a), nil): return "from \(s(a))"
    case let (nil, .some(b)): return "until \(s(b))"
    default: return nil
    }
}

// MARK: - Row

struct NestedRowView: View {
    @ObservedObject var state: NestedEditorState
    let row: NestedRow
    let isEditing: Bool

    @State private var editText = ""
    @FocusState private var fieldFocused: Bool

    private var item: NestedListItem { row.node.item }

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            // Indent guides, sharing one x per depth (slightly right of
            // cell center) — the toggle column below uses the same grid.
            ForEach(0..<row.depth, id: \.self) { level in
                Rectangle()
                    .fill(nestedGuideColor(level))
                    .frame(width: 1)
                    .padding(.leading, 10)
                    .frame(width: 16, alignment: .leading)
            }
            // Collapse toggle: chevron wrapped in an outside circle (parents)
            // or a small filled dot (leaves), matching the attachment where
            // a collapsed parent shows a dot inside an outer ring.
            // 2pt top offset puts the 16pt ring/dot on the text's visual
            // center (content starts 5pt down), whatever the row height.
            Button {
                state.toggleCollapse(id: item.id)
            } label: {
                Group {
                    if row.node.hasChildren {
                        ZStack {
                            Circle()
                                .stroke(nestedDotForDepth(row.depth), lineWidth: 1.5)
                                .frame(width: 16, height: 16)
                            Image(systemName: item.collapsed ? "chevron.right" : "chevron.down")
                                .font(.system(size: 8, weight: .semibold))
                                .foregroundStyle(nestedDotForDepth(row.depth))
                        }
                    } else {
                        Circle()
                            .fill(nestedDotForDepth(row.depth))
                            .frame(width: 7, height: 7)
                    }
                }
                .frame(width: 16, height: 24)
                .contentShape(Rectangle())
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
                        }
                        .buttonStyle(.plain)
                        .help(item.checked ? "Uncheck" : "Check off")
                    }
                    let marker = nestedPriorityMarker(item.priority.name)
                    if !marker.isEmpty {
                        Text(marker)
                            .font(.headline).bold()
                            .foregroundStyle(nestedPriorityColor(item.priority.name))
                            .padding(.top, 1)
                    }
                    if isEditing {
                        TextField("", text: $editText)
                            .textFieldStyle(.plain)
                            .font(nestedRowFont(item.textStyle.name))
                            .focused($fieldFocused)
                            .onAppear {
                                editText = item.text
                                fieldFocused = true
                            }
                            .onSubmit { state.commitEdit(id: item.id, text: editText) }
                            .onKeyPress(keys: [.return]) { press in
                                guard press.modifiers.contains(.shift) else { return .ignored }
                                state.commitEdit(id: item.id, text: editText)
                                state.startAddSibling(of: item.id)
                                return .handled
                            }
                            .onKeyPress(.escape) {
                                state.cancelEdit()
                                return .handled
                            }
                            .onKeyPress(keys: [.tab]) { press in
                                state.commitEdit(id: item.id, text: editText)
                                if press.modifiers.contains(.shift) {
                                    state.outdentSelected()
                                } else {
                                    state.indentSelected()
                                }
                                state.startEdit(id: item.id)
                                return .handled
                            }
                    } else {
                        Text(item.text.isEmpty ? "Untitled item" : item.text)
                            .font(nestedRowFont(item.textStyle.name))
                            .foregroundStyle(nestedTextColor)
                            .strikethrough(item.checked)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(Rectangle())
                            .onTapGesture(count: 2) { state.startEdit(id: item.id) }
                    }
                }
                metadata
            }
            .padding(.vertical, 5)
            .padding(.horizontal, 6)
            .background(rowBackground)
            .clipShape(RoundedRectangle(cornerRadius: 6))
            .contentShape(Rectangle())
            .onTapGesture { state.select(id: item.id) }
        }
        .background(alignment: .topLeading) {
            // Guide continuation from the toggle down through the children.
            // Same 16pt grid x as the guides. The toggle is a 16pt circle
            // in a 24pt slot with 2pt top padding, so its bottom edge sits
            // at 22pt — start the line there so it touches the ring.
            if row.node.hasChildren && !item.collapsed {
                Rectangle()
                    .fill(nestedGuideColor(row.depth))
                    .frame(width: 1)
                    .padding(.top, 22)
                    .padding(.leading, CGFloat(row.depth * 16) + 10)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            }
        }
        .background(nestedCanvasColor)
        .onDrag {
            NSItemProvider(object: "nested:\(item.id)" as NSString)
        }
        .onDrop(of: [.text], isTargeted: nil) { providers in
            guard let provider = providers.first else { return false }
            _ = provider.loadObject(ofClass: NSString.self) { payload, _ in
                guard let s = payload as? String, s.hasPrefix("nested:")
                else { return }
                let dragged = String(s.dropFirst("nested:".count))
                DispatchQueue.main.async {
                    // Option held while dropping = nest as child; else sibling gap.
                    let opts = NSEvent.modifierFlags.contains(.option)
                    if opts {
                        state.dropAsChild(draggedId: dragged, onto: item.id)
                    } else {
                        state.drop(draggedId: dragged, onto: item.id)
                    }
                }
            }
            return true
        }
    }

    private var nestedTextColor: Color {
        // Explicit light-theme ink: .primary would turn white in dark mode
        // and vanish on the paper canvas. Warm charcoal is softer than pure
        // black for long sessions.
        item.textColor.name == "Default" ? nestedInkColor : nestedTokenColor(item.textColor.name)
    }

    private var isSelected: Bool { state.selectedId == item.id }

    private var rowBackground: Color {
        if isEditing { return nestedSelectedColor }
        if isSelected { return nestedSelectedColor }
        if item.backgroundColor.name != "Default" {
            return nestedTokenColor(item.backgroundColor.name).opacity(0.20)
        }
        return .clear
    }

    @ViewBuilder
    private var metadata: some View {
        let summary = state.summary(for: item.id)
        let progress = item.progressPercent?.int32Value
        let note = item.note
        let dateText = nestedDateLabel(
            startDays: item.startDate?.toEpochDays(),
            endDays: item.endDate?.toEpochDays()
        )
        // Android parity (NestedItemMetadataPreview): leaf items always show
        // their own tracked minutes; parents only when showTrackedMinutes.
        let isLeaf = !row.node.hasChildren
        let showTracked = isLeaf || item.showTrackedMinutes
        let visibleMetrics = item.manualMetrics.filter { $0.enabled && (!$0.value.isEmpty || $0.isCompleted) }
        if progress != nil || (summary?.doneItemCount ?? 0) > 0
            || (showTracked && (summary?.trackedMinutes ?? 0) > 0)
            || (note != nil && !(note!.isEmpty)) || !item.tags.isEmpty || dateText != nil
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
                                    .fill(nestedDotForDepth(row.depth).opacity(0.65))
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
                    if let s = summary, s.doneItemCount > 0 {
                        Text("\(s.doneItemCount) done")
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
                        HStack(spacing: 2) {
                            if m.isCompleted {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.caption2).foregroundStyle(.green)
                            }
                            Text(metricLabel(m)).font(.caption)
                        }
                        .padding(.horizontal, 6).padding(.vertical, 1)
                        .background(Color.accentColor.opacity(0.12), in: RoundedRectangle(cornerRadius: 5))
                    }
                }
                if let n = note, !n.isEmpty {
                    Text(n)
                        .font(.callout).foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                if dateText != nil || !item.tags.isEmpty {
                    HStack(spacing: 6) {
                        if let d = dateText {
                            Label(d, systemImage: "calendar")
                                .font(.caption).foregroundStyle(.secondary)
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

    private func metricLabel(_ m: MetricItem) -> String {
        var s = ""
        if !m.name.isEmpty { s += m.name + " " }
        if !m.value.isEmpty { s += m.value }
        if let t = m.targetValue, !t.isEmpty { s += "/\(t)" }
        if let u = m.displayUnit() { s += " \(u)" }
        return s.trimmingCharacters(in: .whitespaces)
    }
}
