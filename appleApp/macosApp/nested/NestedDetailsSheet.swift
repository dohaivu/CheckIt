//
//  NestedDetailsSheet.swift
//  macosApp — note editor, item details (metrics/progress), formatting bar.
//

import SwiftUI
import AppKit
import Shared

// MARK: - Note sheet

struct NestedNoteSheet: View {
    @ObservedObject var state: NestedEditorState
    let item: NestedListItem
    @Environment(\.dismiss) private var dismiss
    @State private var text: String = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Note").font(.headline)
            TextEditor(text: $text)
                .font(.body)
                .frame(minHeight: 140)
                .scrollContentBackground(.hidden)
                .padding(6)
                .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.secondary.opacity(0.3)))
            Text("\(text.count)/2,000")
                .font(.caption).foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .trailing)
            HStack {
                Spacer()
                Button("Cancel") { dismiss() }.keyboardShortcut(.cancelAction)
                Button("Save") {
                    state.saveNote(id: item.id, text: text)
                    dismiss()
                }.keyboardShortcut(.defaultAction)
            }
        }
        .padding()
        .frame(width: 420)
        .onAppear { text = item.note ?? "" }
    }
}

// MARK: - Details sheet (time, rollup, progress, custom metrics)

struct NestedMetricDraft: Identifiable {
    let id = UUID()
    var name = ""
    var value = ""
    var target = ""
    var unit: MetricUnit = .none
    var customUnit = ""
    var completed = false
    var dueDate: Date? = nil
    var dueTimeMinutes: Int? = nil
}

/// Mirrors shared `applyUnitChange`: date-based units gain a due date and
/// drop value/target; other units clear the date fields.
private func applyMetricUnitDefault(draft: inout NestedMetricDraft, unit: MetricUnit) {
    if unit == .countdown || unit == .duedate {
        if draft.dueDate == nil { draft.dueDate = Calendar.current.startOfDay(for: Date()) }
        draft.value = ""
        draft.target = ""
        if unit == .countdown { draft.dueTimeMinutes = nil }
    } else {
        draft.dueDate = nil
        draft.dueTimeMinutes = nil
    }
}

/// Pure-calendar day difference (no UTC truncation): negative means overdue.
private func daysFromToday(to date: Date) -> Int {
    let cal = Calendar.current
    let comps = cal.dateComponents([.day], from: cal.startOfDay(for: Date()), to: cal.startOfDay(for: date))
    return comps.day ?? 0
}

private func countdownPreview(dueDate: Date?) -> String {
    guard let due = dueDate else { return "No date" }
    let r = daysFromToday(to: due)
    if r > 1 { return "\(r) days left" }
    if r == 1 { return "1 day left" }
    if r == 0 { return "Due today" }
    if r == -1 { return "Overdue by 1 day" }
    return "Overdue by \(-r) days"
}

private func countdownOverdue(dueDate: Date?, completed: Bool) -> Bool {
    guard !completed, let due = dueDate else { return false }
    return daysFromToday(to: due) < 0
}

private func combineDateAndMinutes(date: Date, minutes: Int) -> Date {
    let cal = Calendar.current
    let start = cal.startOfDay(for: date)
    return cal.date(byAdding: .minute, value: minutes, to: start) ?? start
}

private func minutesSinceMidnight(of date: Date) -> Int {
    let comps = Calendar.current.dateComponents([.hour, .minute], from: date)
    return (comps.hour ?? 0) * 60 + (comps.minute ?? 0)
}

/// Display order for the unit picker comes from shared `MetricUnit.entries`;
/// labels and names stay in shared Kotlin.

struct NestedDetailsSheet: View {
    @ObservedObject var state: NestedEditorState
    let item: NestedListItem
    @Environment(\.dismiss) private var dismiss

    @State private var minutes = ""
    @State private var policy = "IncludeChildren"
    @State private var showTracked = false
    @State private var showProgress = false
    @State private var progress = 0.0
    @State private var metrics: [NestedMetricDraft] = []

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(item.text.isEmpty ? "Untitled item" : item.text)
                .font(.headline).lineLimit(2)
            if let ms = item.completedAtMillis?.int64Value {
                Text("Completed · \(Date(timeIntervalSince1970: TimeInterval(ms) / 1000).formatted(date: .abbreviated, time: .shortened))")
                    .font(.caption).foregroundStyle(.secondary)
            }
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    // Time & rollup
                    GroupBox("Time & rollup") {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("Actual")
                                TextField("0", text: $minutes)
                                    .textFieldStyle(.roundedBorder)
                                    .frame(width: 70)
                                Text("min").foregroundStyle(.secondary)
                            }
                            Picker("Rollup", selection: $policy) {
                                Text("Include children").tag("IncludeChildren")
                                Text("Own item only").tag("OwnOnly")
                                Text("Exclude from parent").tag("ExcludeFromParent")
                            }
                            .pickerStyle(.menu)
                            Toggle("Show tracked minutes on row", isOn: $showTracked)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    // Progress
                    GroupBox("Progress") {
                        VStack(alignment: .leading, spacing: 8) {
                            Toggle("Show progress", isOn: $showProgress)
                            if showProgress {
                                HStack {
                                    Slider(value: $progress, in: 0...100, step: 1)
                                    Text("\(Int(progress))%").frame(width: 44, alignment: .trailing)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    // Custom metrics
                    GroupBox("Custom metrics") {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach($metrics) { $m in
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack {
                                        Toggle("", isOn: $m.completed).labelsHidden()
                                        TextField("Metric name", text: $m.name)
                                            .textFieldStyle(.roundedBorder)
                                        Button {
                                            metrics.removeAll { $0.id == m.id }
                                        } label: {
                                            Image(systemName: "trash").foregroundStyle(.red)
                                        }
                                        .buttonStyle(.plain)
                                    }
                                    if $m.wrappedValue.unit == .countdown || $m.wrappedValue.unit == .duedate {
                                        HStack {
                                            DatePicker(
                                                "Due",
                                                selection: Binding(
                                                    get: { $m.wrappedValue.dueDate ?? Calendar.current.startOfDay(for: Date()) },
                                                    set: { $m.wrappedValue.dueDate = $0 }
                                                ),
                                                displayedComponents: .date
                                            )
                                            .labelsHidden()
                                            .frame(width: 120)
                                            if $m.wrappedValue.unit == .countdown {
                                                Text(countdownPreview(dueDate: $m.wrappedValue.dueDate))
                                                    .font(.caption)
                                                    .foregroundStyle(countdownOverdue(dueDate: $m.wrappedValue.dueDate, completed: $m.wrappedValue.completed) ? .red : .secondary)
                                            } else {
                                                Toggle("Time", isOn: Binding(
                                                    get: { $m.wrappedValue.dueTimeMinutes != nil },
                                                    set: { $m.wrappedValue.dueTimeMinutes = $0 ? ($m.wrappedValue.dueTimeMinutes ?? 540) : nil }
                                                ))
                                                if $m.wrappedValue.dueTimeMinutes != nil {
                                                    DatePicker(
                                                        "",
                                                        selection: Binding(
                                                            get: {
                                                                combineDateAndMinutes(
                                                                    date: $m.wrappedValue.dueDate ?? Calendar.current.startOfDay(for: Date()),
                                                                    minutes: $m.wrappedValue.dueTimeMinutes ?? 540
                                                                )
                                                            },
                                                            set: { $m.wrappedValue.dueTimeMinutes = minutesSinceMidnight(of: $0) }
                                                        ),
                                                        displayedComponents: .hourAndMinute
                                                    )
                                                    .labelsHidden()
                                                }
                                            }
                                            Spacer()
                                            unitPicker(for: $m)
                                        }
                                    } else {
                                        HStack {
                                            TextField("Value", text: $m.value)
                                                .textFieldStyle(.roundedBorder)
                                            TextField("Target", text: $m.target)
                                                .textFieldStyle(.roundedBorder)
                                            Spacer()
                                            unitPicker(for: $m)
                                        }
                                        if m.unit == .custom {
                                            TextField("Custom unit (e.g. kg, pts)", text: $m.customUnit)
                                                .textFieldStyle(.roundedBorder)
                                        }
                                    }
                                }
                                .padding(6)
                                .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 6))
                            }
                            Button {
                                metrics.append(NestedMetricDraft())
                            } label: {
                                Label("Add metric", systemImage: "plus")
                            }
                            .buttonStyle(.link)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
            HStack {
                Spacer()
                Button("Cancel") { dismiss() }.keyboardShortcut(.cancelAction)
                Button("Save") {
                    save()
                    dismiss()
                }
                .keyboardShortcut(.defaultAction)
            }
        }
        .padding()
        .frame(width: 460, height: 560)
        .onAppear {
            minutes = item.actualMinutes > 0 ? String(item.actualMinutes) : ""
            policy = item.metricRollupPolicy.name
            showTracked = item.showTrackedMinutes
            if let p = item.progressPercent?.int32Value {
                showProgress = true
                progress = Double(max(0, min(100, Int(p))))
            }
            metrics = item.manualMetrics.map {
                NestedMetricDraft(
                    name: $0.name,
                    value: $0.value,
                    target: $0.targetValue ?? "",
                    unit: $0.unit,
                    customUnit: $0.customUnit ?? "",
                    completed: $0.isCompleted,
                    dueDate: $0.dueDateEpochDays.map { nestedDate(fromEpochDays: Int64($0.int32Value)) },
                    dueTimeMinutes: $0.dueTimeMinutes.map { Int($0.int32Value) }
                )
            }
        }
    }

    @ViewBuilder
    private func unitPicker(for metric: Binding<NestedMetricDraft>) -> some View {
        Picker("", selection: Binding(
            get: { metric.wrappedValue.unit },
            set: { u in
                var d = metric.wrappedValue
                d.unit = u
                applyMetricUnitDefault(draft: &d, unit: u)
                metric.wrappedValue = d
            }
        )) {
            ForEach(MetricUnit.entries, id: \.self) { u in
                Text(u.displayName(customUnit: nil)).tag(u)
            }
        }
        .pickerStyle(.menu)
        .frame(width: 120)
    }

    private func save() {
        let mins = Int32(minutes.trimmingCharacters(in: .whitespaces)) ?? 0
        state.updateMetricsSettings(id: item.id, minutes: mins, policy: policy, show: showTracked)
        state.updateProgress(id: item.id, value: showProgress ? Int32(progress) : nil)
        // Encode as MetricItem JSON for the shared decoder.
        // Date-based units carry no value/target; the shared bridge
        // (isValidForSave) keeps them when a due date is set.
        let arr: [[String: Any]] = metrics.map { m in
            let isDate = (m.unit == .countdown || m.unit == .duedate)
            var d: [String: Any] = [
                "name": m.name,
                "value": isDate ? "" : m.value,
                "sortOrder": 0,
                "enabled": true,
                "unit": m.unit.name,
                "isCompleted": m.completed,
            ]
            d["targetValue"] = (isDate || m.target.isEmpty) ? NSNull() : m.target
            d["customUnit"] = m.customUnit.isEmpty ? NSNull() : m.customUnit
            d["dueDateEpochDays"] = m.dueDate.map { Int(nestedEpochDays(from: $0)) } ?? NSNull()
            d["dueTimeMinutes"] = m.dueTimeMinutes ?? NSNull()
            return d
        }
        if let data = try? JSONSerialization.data(withJSONObject: arr),
           let json = String(data: data, encoding: .utf8)
        {
            state.replaceMetrics(id: item.id, json: json)
        }
    }
}

// MARK: - Formatting bar (selected row)

struct NestedFormattingBar: View {
    @ObservedObject var state: NestedEditorState
    let item: NestedListItem

    @State private var showNote = false
    @State private var showDetails = false
    @State private var showDates = false
    @State private var startDate = Date()
    @State private var endDate = Date()
    @State private var hasStart = false
    @State private var hasEnd = false

    private let tokens = ["Default", "Red", "Orange", "Yellow", "Green", "Blue", "Purple", "Pink"]

    /// Filled-flag tint from the shared TaskPriority palette (None → secondary).
    private func priorityTint(_ name: String) -> Color {
        let entry: TaskPriority?
        switch name {
        case "Low": entry = TaskPriority.low
        case "Medium": entry = TaskPriority.medium
        case "High": entry = TaskPriority.high
        default: entry = nil
        }
        if let hex = entry?.priorityHex(), let color = Color(nestedHex: hex) {
            return color
        }
        return .secondary
    }

    private func barBtn(
        _ system: String,
        tint: Color = .secondary,
        help: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: system)
                .imageScale(.medium)
                .foregroundStyle(tint)
                .frame(width: 28, height: 28)
        }
        .buttonStyle(.borderless)
        .accessibilityLabel(help)
        .help(help)
    }

    var body: some View {
        HStack(spacing: 6) {
            // Text style (flat rows; Toggle renders a native checkmark on
            // the current value — custom row views don't paint in Menus).
            Menu {
                ForEach(["Header", "Subheader", "Body"], id: \.self) { s in
                    Toggle(s, isOn: Binding(
                        get: { s == item.textStyle.name },
                        set: { if $0 { state.updateFormatting(id: item.id, style: s, textColor: item.textColor.name, background: item.backgroundColor.name) } }
                    ))
                }
                Divider()
                Button(item.checkboxEnabled ? "Hide checkbox" : "Show checkbox") {
                    state.setCheckboxEnabled(id: item.id, enabled: !item.checkboxEnabled)
                }
            } label: {
                Image(systemName: "textformat.size")
            }
            .menuStyle(.borderlessButton)
            .frame(width: 28, height: 28)
            .accessibilityLabel("Text style")
            .help("Text style")
            // Text color
            Menu {
                ForEach(tokens, id: \.self) { t in
                    Toggle(t, isOn: Binding(
                        get: { t == item.textColor.name },
                        set: { if $0 { state.updateFormatting(id: item.id, style: item.textStyle.name, textColor: t, background: item.backgroundColor.name) } }
                    ))
                }
            } label: {
                Image(systemName: "paintbrush")
                    .tint(item.textColor.name == "Default" ? .secondary : Color.accentColor)
            }
            .menuStyle(.borderlessButton)
            .frame(width: 28, height: 28)
            .accessibilityLabel("Text color")
            .help("Text color")
            // Background
            Menu {
                ForEach(tokens, id: \.self) { t in
                    Toggle(t, isOn: Binding(
                        get: { t == item.backgroundColor.name },
                        set: { if $0 { state.updateFormatting(id: item.id, style: item.textStyle.name, textColor: item.textColor.name, background: t) } }
                    ))
                }
            } label: {
                Image(systemName: "paintpalette")
                    .tint(item.backgroundColor.name == "Default" ? .secondary : Color.accentColor)
            }
            .menuStyle(.borderlessButton)
            .frame(width: 28, height: 28)
            .accessibilityLabel("Background color")
            .help("Background color")
            // Priority
            Menu {
                ForEach(["None", "Low", "Medium", "High"], id: \.self) { p in
                    Toggle(isOn: Binding(
                        get: { p == item.priority.name },
                        set: { if $0 { state.updatePriority(id: item.id, name: p) } }
                    )) {
                        Label {
                            Text(p)
                        } icon: {
                            Image(systemName: "flag.fill")
                                .foregroundStyle(priorityTint(p))
                        }
                    }
                }
            } label: {
                Image(systemName: item.priority.name == "None" ? "flag" : "flag.fill")
                    .tint(priorityTint(item.priority.name))
            }
            .menuStyle(.borderlessButton)
            .frame(width: 28, height: 28)
            .accessibilityLabel("Priority")
            .help("Priority")
            // Dates
            barBtn(
                "calendar",
                tint: (item.startDate != nil || item.endDate != nil) ? Color.accentColor : .secondary,
                help: "Date range"
            ) {
                hasStart = item.startDate != nil
                hasEnd = item.endDate != nil
                if let s = item.startDate?.toEpochDays() { startDate = nestedDate(fromEpochDays: s) }
                if let e = item.endDate?.toEpochDays() { endDate = nestedDate(fromEpochDays: e) }
                showDates = true
            }
            .popover(isPresented: $showDates, arrowEdge: .bottom) {
                VStack(alignment: .leading, spacing: 8) {
                    Toggle("Start", isOn: $hasStart)
                    DatePicker("", selection: $startDate, displayedComponents: .date)
                        .labelsHidden().disabled(!hasStart)
                    Toggle("End", isOn: $hasEnd)
                    DatePicker("", selection: $endDate, displayedComponents: .date)
                        .labelsHidden().disabled(!hasEnd)
                    HStack {
                        Button("Clear") {
                            state.updateDates(id: item.id, start: nil, end: nil)
                            showDates = false
                        }
                        .foregroundStyle(.red)
                        Spacer()
                        Button("Done") {
                            state.updateDates(
                                id: item.id,
                                start: hasStart ? nestedEpochDays(from: startDate) : nil,
                                end: hasEnd ? nestedEpochDays(from: endDate) : nil
                            )
                            showDates = false
                        }
                    }
                }
                .padding()
                .frame(width: 240)
            }
            // Note
            barBtn(
                "note.text",
                tint: (item.note?.isEmpty ?? true) ? .secondary : Color.accentColor,
                help: "Edit note"
            ) {
                showNote = true
            }
            // Check
            barBtn(
                item.checked ? "checkmark.circle.fill" : "checkmark.circle",
                tint: item.checked ? Color.accentColor : .secondary,
                help: item.checked ? "Uncheck" : "Check off"
            ) {
                state.setCheckedExact(id: item.id, checked: !item.checked)
            }
            // Details
            barBtn("info.circle", help: "Item details") {
                showDetails = true
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(Color(nsColor: .windowBackgroundColor))
        .sheet(isPresented: $showNote) { NestedNoteSheet(state: state, item: item) }
        .sheet(isPresented: $showDetails) { NestedDetailsSheet(state: state, item: item) }
    }
}
