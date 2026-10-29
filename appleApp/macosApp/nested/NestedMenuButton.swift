//
//  NestedMenuButton.swift
//  macosApp — toolbar menu with image rows.
//

import AppKit
import Shared
import SwiftUI

/// One row of a `NestedMenuButton` menu.
enum NestedMenuRow {
    case header(String)
    case item(
        title: String,
        icon: NSImage? = nil,
        selected: Bool = false,
        action: () -> Void
    )

    /// Palette row: filled dot for a concrete color, hollow ring for
    /// `Default` (which follows the theme).
    static func color(
        _ token: NestedColorToken,
        selected: Bool,
        action: @escaping () -> Void
    ) -> NestedMenuRow {
        .item(
            title: token.name,
            icon: nestedColorDot(token),
            selected: selected,
            action: action
        )
    }
}

/// Borderless toolbar menu whose rows can carry an image.
///
/// SwiftUI's `Menu` drops row images on macOS 27 (AppKit's
/// `NSMenuItem.preferredImageVisibility` defaults to automatic, which hides
/// them, and SwiftUI exposes no override). Building the `NSMenu` ourselves lets
/// us force `.visible`, so the color dots and priority flags render. The
/// trigger stays a SwiftUI `Button` so it matches the surrounding toolbar.
struct NestedMenuButton<Label: View>: View {
    private let rows: () -> [NestedMenuRow]
    private let label: Label

    @State private var anchor: NSView?

    init(rows: @escaping () -> [NestedMenuRow], @ViewBuilder label: () -> Label) {
        self.rows = rows
        self.label = label()
    }

    var body: some View {
        Button(action: showMenu) {
            label
        }
        .buttonStyle(.borderless)
        .background(NestedMenuAnchor { anchor = $0 })
    }

    private func showMenu() {
        guard let anchor, let window = anchor.window else { return }
        let menu = makeMenu()
        // Menu drops below the button, aligned to its leading edge.
        let rect = window.convertToScreen(anchor.convert(anchor.bounds, to: nil))
        menu.popUp(positioning: nil, at: NSPoint(x: rect.minX, y: rect.minY - 2), in: nil)
    }

    private func makeMenu() -> NSMenu {
        let menu = NestedMenu()
        menu.autoenablesItems = false
        var nextTag = 0

        for row in rows() {
            switch row {
            case .header(let title):
                let item = NSMenuItem(title: title, action: nil, keyEquivalent: "")
                item.attributedTitle = NSAttributedString(
                    string: title,
                    attributes: [
                        .font: NSFont.systemFont(ofSize: NSFont.smallSystemFontSize),
                        .foregroundColor: NSColor.secondaryLabelColor,
                    ]
                )
                item.isEnabled = false
                menu.addItem(item)
            case .item(let title, let icon, let selected, let action):
                let item = NSMenuItem(
                    title: title,
                    action: #selector(NestedMenuTarget.run(_:)),
                    keyEquivalent: ""
                )
                item.target = menu.actionTarget
                item.tag = nextTag
                item.image = icon
                item.state = selected ? .on : .off
                if #available(macOS 27.0, *) {
                    item.preferredImageVisibility = .visible
                }
                menu.actionTarget.actions[nextTag] = action
                nextTag += 1
                menu.addItem(item)
            }
        }
        return menu
    }
}

/// Button label matching a native `Menu` trigger: icon plus disclosure chevron.
struct NestedMenuTrigger: View {
    let symbol: String
    let tint: Color

    var body: some View {
        HStack(spacing: 1) {
            Image(systemName: symbol)
                .foregroundStyle(tint)
            Image(systemName: "chevron.compact.down")
                .font(.system(size: 9, weight: .semibold))
                .foregroundStyle(.secondary)
        }
        .frame(width: 28, height: 28)
    }
}

// MARK: - Images

/// 18 pt image holding a 13 pt palette dot. Declared non-template so AppKit
/// keeps the real color.
private func nestedColorDot(_ token: NestedColorToken) -> NSImage {
    let image = NSImage(size: NSSize(width: 18, height: 18), flipped: false) { rect in
        let dot = rect.insetBy(dx: 2.5, dy: 2.5)
        if let hex = token.hex(), let color = NSColor(nestedHex: hex) {
            color.setFill()
            NSBezierPath(ovalIn: dot).fill()
            // Hairline outline keeps pale colors readable on menu highlights.
            NSColor.separatorColor.setStroke()
            let outline = NSBezierPath(ovalIn: dot.insetBy(dx: 0.5, dy: 0.5))
            outline.lineWidth = 1
            outline.stroke()
        } else {
            NSColor.secondaryLabelColor.setStroke()
            let ring = NSBezierPath(ovalIn: dot.insetBy(dx: 0.5, dy: 0.5))
            ring.lineWidth = 1.5
            ring.stroke()
        }
        return true
    }
    image.isTemplate = false
    return image
}

/// Flag tinted with the shared priority palette, so the menu shows the same
/// colors as the trigger icon.
func nestedPriorityFlag(_ priority: TaskPriority) -> NSImage? {
    let color = priority.priorityHex().flatMap { NSColor(nestedHex: $0) } ?? .secondaryLabelColor
    let configuration = NSImage.SymbolConfiguration(pointSize: 13, weight: .regular)
        .applying(NSImage.SymbolConfiguration(paletteColors: [color]))
    guard let image = NSImage(systemSymbolName: "flag.fill", accessibilityDescription: nil)?
        .withSymbolConfiguration(configuration)
    else { return nil }
    image.isTemplate = false
    return image
}

// MARK: - AppKit plumbing

/// Zero-sized probe that reports its `NSView` so the popup can be positioned
/// under the SwiftUI trigger without guessing coordinate spaces.
private struct NestedMenuAnchor: NSViewRepresentable {
    let onResolve: (NSView) -> Void

    func makeNSView(context: Context) -> NSView {
        let view = NSView(frame: .zero)
        DispatchQueue.main.async { onResolve(view) }
        return view
    }

    func updateNSView(_ view: NSView, context: Context) {
        DispatchQueue.main.async { onResolve(view) }
    }
}

/// Menu kept alive while it tracks; holds the closures the items dispatch to.
private final class NestedMenu: NSMenu {
    let actionTarget = NestedMenuTarget()
}

private final class NestedMenuTarget: NSObject {
    var actions: [Int: () -> Void] = [:]

    @objc func run(_ sender: NSMenuItem) {
        actions[sender.tag]?()
    }
}
