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
                                    HStack {
                                        TextField("Value", text: $m.value)
                                            .textFieldStyle(.roundedBorder)
                                        TextField("Target", text: $m.target)
                                            .textFieldStyle(.roundedBorder)
                                        Picker("", selection: $m.unit) {
                                            ForEach(MetricUnit.entries, id: \.self) { u in
                                                Text(u.displayName(customUnit: nil)).tag(u)
                                            }
                                        }
                                        .pickerStyle(.menu)
                                        .frame(width: 110)
                                    }
                                    if m.unit == .custom {
                                        TextField("Custom unit (e.g. kg, pts)", text: $m.customUnit)
                                            .textFieldStyle(.roundedBorder)
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
                    completed: $0.isCompleted
                )
            }
        }
    }

    private func save() {
        let mins = Int32(minutes.trimmingCharacters(in: .whitespaces)) ?? 0
        state.updateMetricsSettings(id: item.id, minutes: mins, policy: policy, show: showTracked)
        state.updateProgress(id: item.id, value: showProgress ? Int32(progress) : nil)
        // Encode as MetricItem JSON for the shared decoder.
        let arr: [[String: Any]] = metrics.map { m in
            var d: [String: Any] = [
                "name": m.name,
                "value": m.value,
                "sortOrder": 0,
                "enabled": true,
                "unit": m.unit.name,
                "isCompleted": m.completed,
            ]
            d["targetValue"] = m.target.isEmpty ? NSNull() : m.target
            d["customUnit"] = m.customUnit.isEmpty ? NSNull() : m.customUnit
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
                    .foregroundStyle(item.textColor.name == "Default" ? .secondary : Color.accentColor)
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
                    .foregroundStyle(item.backgroundColor.name == "Default" ? .secondary : Color.accentColor)
            }
            .menuStyle(.borderlessButton)
            .frame(width: 28, height: 28)
            .accessibilityLabel("Background color")
            .help("Background color")
            // Priority
            Menu {
                ForEach(["None", "Low", "Medium", "High"], id: \.self) { p in
                    Toggle(p, isOn: Binding(
                        get: { p == item.priority.name },
                        set: { if $0 { state.updatePriority(id: item.id, name: p) } }
                    ))
                }
            } label: {
                Image(systemName: "flag")
                    .foregroundStyle(item.priority.name == "None" ? .secondary : Color.accentColor)
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
