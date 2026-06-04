import SwiftUI
import NudgeCMP
import UIKit
import WidgetKit

@main
struct iOSApp: App {
    init(){
        UIApplication.shared.isIdleTimerDisabled = true
        KoinModuleKt.doInitKoin()

    }
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {

                }
        }
    }
}
