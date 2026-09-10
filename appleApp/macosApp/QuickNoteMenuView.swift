//
//  QuickNoteMenuView.swift
//  appleApp
//
//  Created by DO HAI VU on 10/9/26.
//
import SwiftUI
import Shared

struct QuickNoteMenuView: View {
    // 3. Reuse your existing UseCase
//    private let createNoteUseCase = KoinProxy.shared.getCreateQuickNoteUseCase()
    
    @State private var noteText: String = ""

    var body: some View {
        VStack(spacing: 12) {
            Text("Quick Note \(Platform.companion.getPlatform())").font(.headline)
            
            TextEditor(text: $noteText)
                .frame(height: 100)
                .border(Color.gray.opacity(0.2))
            
            Button("Save Note") {
                // 4. Call KMP suspend function (requires async/await wrapper or SKIE)
                Task {
//                    try? await createNoteUseCase.invoke(content: noteText)
                    noteText = ""
                }
            }
            .keyboardShortcut(.return, modifiers: .command)
            
            Divider()
            
            Button("Quit") {
                NSApplication.shared.terminate(nil)
            }
        }
        .padding()
        .frame(width: 300)
    }
}
