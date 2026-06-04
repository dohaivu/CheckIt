import UIKit
import SwiftUI
import NudgeCMP

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .toolbarColorScheme(.dark, for: .navigationBar)
    }
}



