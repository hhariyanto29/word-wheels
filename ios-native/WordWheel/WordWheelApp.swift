import SwiftUI

@main
struct WordWheelApp: App {
    @StateObject private var soundManager = SoundManager()

    var body: some Scene {
        WindowGroup {
            GameScreen()
                .environmentObject(soundManager)
                .preferredColorScheme(.dark)
                .statusBarHidden(true)
        }
    }
}
